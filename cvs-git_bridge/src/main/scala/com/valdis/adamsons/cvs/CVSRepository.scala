package com.valdis.adamsons.cvs

import scala.sys.process._
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.ByteArrayInputStream
import org.omg.CORBA.portable.InputStream
import java.io.InputStream
import java.io.File
import scala.collection.immutable.SortedSet
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.utils.SerialFileSeq
import com.valdis.adamsons.utils.EmptyFileSeq
import com.valdis.adamsons.utils.SerialFileSeqLike

case class CVSRepository(val cvsroot: Option[String], val module: Option[String]) extends SweetLogger{
  def logger = Logger
  def this(cvsroot: Option[String]) = this(cvsroot, None)
  def this() = this(None, None)
  def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
  
  def root = cvsroot.getOrElse("echo $CVSROOT"!!)
  
  def module(module: String) = CVSRepository(this.cvsroot, Some(module))

  private def cvsString = "cvs " + cvsroot.map("-d " + _ + " ").getOrElse("");
  
  def cleanRCSpath(absolutePath:String)= absolutePath.drop(cvsroot.getOrElse("").size + 1 + module.getOrElse("").size + 1).trim.dropRight(2).replace("Attic/", "")
  /**
   * Should only be used for text files.
   * Binary files get corrupted because Java tries to convert them to UTF8.
   */
  def getFileContents(name: String, version: CVSFileVersion) = cvsString+"co -p -r "+version +module.map(" "+ _ + "/").getOrElse("")+name!!
  def getFile(name: String, version: CVSFileVersion) = {
    val process = cvsString+"co -p -r "+version +module.map(" "+ _ + "/").getOrElse("")+name
    log("running command:\n" + process)
    val file = File.createTempFile("tmp", ".bin")
    //forces the to wait until process finishes.
    val exitvalue=process.#>(file).run.exitValue;
    //TODO handle errors
    file
  }
  def fileNameList = {
    val responseLines = stringToProcess(cvsString+ "rlog -R " + module.getOrElse("")).lines;
    responseLines.toList.map(cleanRCSpath)
  }

  private def getTagLines = {
    val command = cvsString + "rlog -h" + module.map(" " + _ + "/").getOrElse("")
    log("running command:\n" + command)
    val lines = stringToProcess(command).lines;
    lines.filter((str) => str.size > 0 && str(0) == '\t').map(_.trim)
  }

  def getBranchNameSet: Set[String] = {
    val pairs = getTagLines.map(_.split(':')).map((pair) => (pair(0), CVSFileVersion(pair(1).trim)))
    pairs.filter(_._2.isBranch).map(_._1).toSet
  }

  def getTagNameSet: Set[String] = {
    val pairs = getTagLines.map(_.split(':')).map((pair) => (pair(0), CVSFileVersion(pair(1).trim)))
    pairs.filter(!_._2.isBranch).map(_._1).toSet
  }

  private case class RlogTagParseState(val isInHeader: Boolean, val fileName: String, val tag: CVSTag) {
    def this(name: String) = this(true, "", CVSTag(name, Map()))

    def extractFileName(line: String): Option[String] = {
      val arr = line.split(':')
      if (arr.length == 2 && arr(0).trim == "RCS file") {
        Some(cleanRCSpath(arr(1).trim))
      } else {
        None
      }
    }

    def withLine(line: String): RlogTagParseState = {
      line match {
        case CVSRepository.FILES_SPLITTER => {
          val isInHeader = true;
          if (this.isInHeader == isInHeader) {
            this
          } else {
            new RlogTagParseState(isInHeader, this.fileName, tag)
          }
        }
        case CVSRepository.COMMITS_SPLITTER => {
          val isInHeader = false;
          if (this.isInHeader == isInHeader) {
            this
          } else {
            new RlogTagParseState(isInHeader, fileName, tag)
          }
        }
        case _ => {
          if (isInHeader) {
            val fileNameUpdated = extractFileName(line).map(new RlogTagParseState(isInHeader, _, tag))
            fileNameUpdated.getOrElse({
              lazy val pair = {
                val arr = line.split(':')
                (arr(0).trim(), CVSFileVersion(arr(1).trim))
              }
              def isTagLine = line.size > 1 && line(0) == '\t'
              def isTheRightTag = tag.name == pair._1
              if (isTagLine && isTheRightTag) {
                new RlogTagParseState(isInHeader, this.fileName, tag.withFile(this.fileName, pair._2))
              } else {
                this
              }
            })
          } else {
            this
          }
        }
      }
    }
  }

  def resolveTag(tagName: String): CVSTag = {
    val command = cvsString + "rlog -h" + module.map(" " + _ + "/").getOrElse("")
    log("running command:\n" + command)
    val lines = stringToProcess(command).lines;
    lines.foldLeft(new RlogTagParseState(tagName))(_ withLine _).tag
  }

  def getCommitList: Seq[CVSCommit] = getCommitList(None, None)
  def getCommitList(start: Option[Date], end: Option[Date]): Seq[CVSCommit] = getCommitList(None, start, end)
  def getCommitList(branch:String, start:Option[Date], end:Option[Date]):Seq[CVSCommit] = getCommitList(Some(branch),start, end)
  def getCommitList(branch:Option[String], start:Option[Date], end:Option[Date]):Seq[CVSCommit]={
    val startString = start.map(CVSRepository.CVS_SHORT_DATE_FORMAT.format(_))
    val endString = end.map(CVSRepository.CVS_SHORT_DATE_FORMAT.format(_))
    val dateString = if (start.isDefined || end.isDefined) {
      "-d \"" + startString.getOrElse("") + "<" + endString.getOrElse("") + "\" "
     } else {
      ""
     }
    val command = cvsString+ "rlog "+branch.map(" -r"+_+" ").getOrElse(" -b ") + dateString + module.getOrElse("")
    log("running command:\n" + command)
    val responseLines = stringToProcess(command).lines;
    // TODO do not convert
    parseRlogLines(responseLines)
  }
  
  private def missing(field:String) = throw new IllegalStateException("cvs rlog malformed. Mandatory field '"+field+"' missing")

  private case class RlogCommitParseState(val isInHeader: Boolean,
		  							val commits: SerialFileSeqLike[CVSCommit],
		  							val headerBuffer: Vector[String],
		  							val commitBuffer: Vector[String]) {
    def this() = this(true, new EmptyFileSeq(), Vector(), Vector())

    private def updatedCommits = if (isInHeader) {
      commits
    } else {
      commits :+ commitFromRLog(headerBuffer, commitBuffer)
    }

    def withLine(line: String): RlogCommitParseState = {
      line match {
        case CVSRepository.FILES_SPLITTER => {
          val isInHeader = true;
          new RlogCommitParseState(isInHeader, updatedCommits, Vector(), Vector())
        }
        case CVSRepository.COMMITS_SPLITTER => {
          val isInHeader = false;
          new RlogCommitParseState(isInHeader, updatedCommits, headerBuffer, Vector())
        }
        case _ => {
          val headerBuffer =  (if (isInHeader) {this.headerBuffer :+ line } else { this.headerBuffer})
          val commitBuffer =  (if (!isInHeader) {this.commitBuffer :+ line } else { this.commitBuffer })
          new RlogCommitParseState(isInHeader, commits, headerBuffer, commitBuffer)
        }
      }
    }
  }

  private def commitFromRLog(header: IndexedSeq[String], commit: IndexedSeq[String]): CVSCommit = {
    val headerPairs = header.toList.map(_.split(": ")).toList.filter(_.length > 1).map((x) => x(0).trim -> x(1))
    val headerMap = headerPairs.toMap
    val fileName = cleanRCSpath(headerMap.get("RCS file").getOrElse(missing("file name(RCS file)")))

    val revisionStr = commit(0).trim.split(' ')(1)
    val revision = CVSFileVersion(revisionStr)
    val params = commit(1).trim.dropRight(1).split(';').map(_.split(": ")).map((x) => x(0).trim -> x(1).trim).toMap
    val date = CVSRepository.CVS_DATE_FORMAT.parse(params.get("date").getOrElse(missing("date")))
    val author = params.get("author").getOrElse(missing("author"))
    val commitId = params.get("commitid");
    val isDead = params.get("state").exists(_ == "dead")
    //need a good way to determine where commit message starts
    val linesToDrop = if (commit(2).contains(": ")) { 3 } else { 2 }
    val comment = commit.drop(linesToDrop).mkString("\n").trim

    val cvsCommit = CVSCommit(fileName, revision, isDead, date, author, comment, commitId)
    cvsCommit
  }

  def parseRlogLines(lines: Iterable[String]): Seq[CVSCommit] = {
	lines.foldLeft(new RlogCommitParseState())(_.withLine(_)).commits
  }
}

object CVSRepository {
  def apply() = new CVSRepository();
  def apply(cvsroot: String, module: String) = new CVSRepository(cvsroot, module);
  private val FILES_SPLITTER="=============================================================================";
  private val COMMITS_SPLITTER="----------------------------";
  private val CVS_DATE_FORMAT= new SimpleDateFormat("yyyy-MM-dd kk:mm:ss Z",Locale.UK)
  private val CVS_SHORT_DATE_FORMAT= new SimpleDateFormat("yyyy/MM/dd",Locale.UK)
}