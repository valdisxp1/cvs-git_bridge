package com.valdis.adamsons.cvs

import scala.sys.process._
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.File
import scala.collection.immutable.SortedSet
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.utils.SerialFileSeq
import com.valdis.adamsons.utils.EmptyFileSeq
import com.valdis.adamsons.utils.SerialFileSeqLike
import com.valdis.adamsons.utils.FileUtils

case class CVSRepository(val cvsroot: Option[String], val module: Option[String]) extends SweetLogger {
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
    val file = FileUtils.createTempFile("tmp", ".bin")
    //forces the to wait until process finishes.
    val exitvalue=process.#>(file).run.exitValue;
    //TODO handle errors
    file.deleteOnExit()
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

  private trait RlogParseState[This]{
    val isInHeader: Boolean

    private val FILES_SPLITTER = "=============================================================================";
    private val COMMITS_SPLITTER = "----------------------------";
    
    def extractFileName(line: String): Option[String] = {
      val arr = line.split(':')
      if (arr.length == 2 && arr(0).trim == "RCS file") {
        Some(cleanRCSpath(arr(1).trim))
      } else {
        None
      }
    }
    
    protected def create(isInHeader: Boolean): This
    protected def create: This

    protected def withHeaderLine(line: String): This = create
    protected def withCommitLine(line: String): This = create

    def setIsInHeader(isInHeader: Boolean): This = if (this.isInHeader == isInHeader) {
      create
    } else {
      create(isInHeader)
    }
    
    protected def withFileSpliter = setIsInHeader(true)
    protected def withCommitSpliter = setIsInHeader(false)

    def withLine(line: String): This = {
      line match {
        case FILES_SPLITTER => {
          withFileSpliter
        }
        case COMMITS_SPLITTER => {
          withCommitSpliter
        }
        case _ => {
          if (isInHeader) {
            withHeaderLine(line)
          } else {
            withCommitLine(line)
          }
        }
      }
    }
  }

  private case class RlogSingleTagParseState(override val isInHeader: Boolean, val fileName: String, val tag: CVSTag) extends RlogParseState[RlogSingleTagParseState] {
    def this(name: String) = this(true, "", CVSTag(name, Map()))
    
    type This = RlogSingleTagParseState
    
    override protected def create(isInHeader: Boolean): RlogSingleTagParseState = new RlogSingleTagParseState(isInHeader, fileName, tag)
    override protected def create = this
    private def create(isInHeader: Boolean,fileName: String, tag: CVSTag) = new RlogSingleTagParseState(isInHeader, fileName, tag)

    override protected def withHeaderLine(line: String): RlogSingleTagParseState = {
      val fileNameUpdated = extractFileName(line).map(create(isInHeader, _, tag))
      fileNameUpdated.getOrElse({
        lazy val pair = {
          val arr = line.split(':')
          (arr(0).trim(), CVSFileVersion(arr(1).trim))
        }
        def isTagLine = line.size > 1 && line(0) == '\t'
        def isTheRightTag = tag.name == pair._1
        if (isTagLine && isTheRightTag) {
          create(isInHeader, this.fileName, tag.withFile(this.fileName, pair._2))
        } else {
          this
        }
      })
    }
    
  }

  def resolveTag(tagName: String): CVSTag = {
    val command = cvsString + "rlog -h" + module.map(" " + _ + "/").getOrElse("")
    log("running command:\n" + command)
    val lines = stringToProcess(command).lines;
    lines.foldLeft(new RlogSingleTagParseState(tagName))(_ withLine _).tag
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
    parseRlogLines(stringToProcess(command))
  }
  
  private def missing(field:String) = throw new IllegalStateException("cvs rlog malformed. Mandatory field '"+field+"' missing")

  private case class RlogCommitParseState(val isInHeader: Boolean,
		  							val commits: Seq[CVSCommit],
		  							val headerBuffer: Vector[String],
		  							val commitBuffer: Vector[String]) extends RlogParseState[RlogCommitParseState]{
    def this() = this(true, Vector(), Vector(), Vector())
    def this(emptyCollection: Seq[CVSCommit]) = this(true, emptyCollection, Vector(), Vector())

    private def updatedCommits = if (isInHeader) {
      commits
    } else {
      commits :+ commitFromRLog(headerBuffer, commitBuffer)
    }
    
    override protected def create = this
    override protected def create(isInHeader: Boolean) = new RlogCommitParseState(isInHeader, commits, headerBuffer, commitBuffer)

    override protected def withHeaderLine(line: String) = new RlogCommitParseState(isInHeader, commits, headerBuffer :+ line, commitBuffer)
    override protected def withCommitLine(line: String) = new RlogCommitParseState(isInHeader, commits, headerBuffer, commitBuffer :+ line)
    
    override protected def withFileSpliter = new RlogCommitParseState(true, updatedCommits, Vector(), Vector())
    override protected def withCommitSpliter = new RlogCommitParseState(false, updatedCommits, headerBuffer, Vector())
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

  def parseRlogLines(process: ProcessBuilder): Seq[CVSCommit] = {
    val state = processFold(new RlogCommitParseState(Vector[CVSCommit]()), process)(_ withLine _)
    state.commits
  }
  /**
   * This method does folds over all lines without generating permanent data structure.
   * @see ProcessBuilder.lines generates a immutable Stream
   */
  private def processFold[A](initial: A, process: ProcessBuilder)(f: (A, String) => A): A = {
    var state = initial
    val processLogger = ProcessLogger(line => state = f(state, line), line => log(line))
    process.run(processLogger).exitValue
    state
  }
}

object CVSRepository {
  def apply() = new CVSRepository();
  def apply(cvsroot: String, module: String) = new CVSRepository(cvsroot, module);
  private val CVS_DATE_FORMAT= new SimpleDateFormat("yyyy-MM-dd kk:mm:ss Z",Locale.UK)
  private val CVS_SHORT_DATE_FORMAT= new SimpleDateFormat("yyyy/MM/dd",Locale.UK)
}