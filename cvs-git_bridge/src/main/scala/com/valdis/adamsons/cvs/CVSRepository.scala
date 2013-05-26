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
import com.valdis.adamsons.utils.FileUtils
import com.valdis.adamsons.utils.ProcessAsTraversable
import com.valdis.adamsons.cvs.rlog.parse.{RlogParseState, RlogTagNameLookupState}

case class CVSRepository(val cvsroot: Option[String], val module: Option[String]) extends SweetLogger {
  protected def logger = Logger
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

  private def getTagProcess = {
    val command = cvsString + "rlog -h" + module.map(" " + _ + "/").getOrElse("")
    log("running command:\n" + command)
    stringToProcess(command);
  }
  

  def getBranchNameSet: Set[String] = {
    new ProcessAsTraversable(getTagProcess, line => log(line))
    	.foldLeft(new RlogTagNameLookupState((name, version) => version.isBranch))(_ withLine _).nameSet
  }

  def getTagNameSet: Set[String] = {
    new ProcessAsTraversable(getTagProcess, line => log(line))
    	.foldLeft(new RlogTagNameLookupState((name, version) => !version.isBranch))(_ withLine _).nameSet
  }

  private def extractFileName(line: String): Option[String] = {
      val arr = line.split(':')
      if (arr.length == 2 && arr(0).trim == "RCS file") {
        Some(cleanRCSpath(arr(1).trim))
      } else {
        None
      }
    }
  
  private trait RlogAllTagParseStateLike[This] extends RlogParseState[This] {
    
    val tags: Map[String,CVSTag]
    val fileName: String
    
    protected def create(isInHeader: Boolean,fileName: String, tags: Map[String,CVSTag]): This
    
    protected def withTagEntry(tagName: String, version: CVSFileVersion): This = {
      val previousTag = tags.getOrElse(tagName, CVSTag(tagName))
      val updatedTags = tags + (tagName -> previousTag.withFile(this.fileName, version))
      create(isInHeader, this.fileName, updatedTags)
    }
    
    override protected def withHeaderLine(line: String): This = {
      val fileNameUpdated = extractFileName(line).map(create(isInHeader, _, tags))
      fileNameUpdated.getOrElse({
        lazy val pair = {
          val arr = line.split(':')
          (arr(0).trim(), CVSFileVersion(arr(1).trim))
        }
        def isTagLine = line.size > 1 && line(0) == '\t'
        if (isTagLine) {
        	withTagEntry(pair._1, pair._2)
        } else {
          self
        }
      })
    }
  }
  
   private case class RlogAllTagParseState(override val isInHeader: Boolean,
		  									 override val fileName: String,
		  									 override val tags: Map[String,CVSTag]) extends RlogAllTagParseStateLike[RlogAllTagParseState]{
    def this() = this(RlogParseState.isFirstLineHeader, "", Map())
    
    override protected def create(isInHeader: Boolean): RlogAllTagParseState = new RlogAllTagParseState(isInHeader, fileName, tags)
    override protected def self = this
    protected def create(isInHeader: Boolean,fileName: String, tags: Map[String,CVSTag]) = new RlogAllTagParseState(isInHeader, fileName, tags)
  }
  
  private case class RlogAllBranchParseState(override val isInHeader: Boolean,
		  									 override val fileName: String,
		  									 override val tags: Map[String,CVSTag]) extends RlogAllTagParseStateLike[RlogAllBranchParseState]{
    def this() = this(RlogParseState.isFirstLineHeader, "", Map())
    
    override protected def create(isInHeader: Boolean): RlogAllBranchParseState = new RlogAllBranchParseState(isInHeader, fileName, tags)
    override protected def self = this
    protected def create(isInHeader: Boolean,fileName: String, tags: Map[String,CVSTag]) = new RlogAllBranchParseState(isInHeader, fileName, tags)
    
    override protected def withTagEntry(tagName: String, version: CVSFileVersion) = if(version.isBranch) super.withTagEntry(tagName, version) else self
  }
  
  private case class RlogMultiTagParseState(override val isInHeader: Boolean,
		  									 override val fileName: String,
		  									 override val tags: Map[String,CVSTag]) extends RlogAllTagParseStateLike[RlogMultiTagParseState]{
    //initializes all interesting tags
    def this(names: Iterable[String]) = this(RlogParseState.isFirstLineHeader, "", names.map(name => name -> CVSTag(name)).toMap)
    
    override protected def create(isInHeader: Boolean): RlogMultiTagParseState = new RlogMultiTagParseState(isInHeader, fileName, tags)
    override protected def self = this
    protected def create(isInHeader: Boolean,fileName: String, tags: Map[String,CVSTag]) = new RlogMultiTagParseState(isInHeader, fileName, tags)
    
    override protected def withTagEntry(tagName: String, version: CVSFileVersion) = {
      //ignores tags that are not initialized
      tags.get(tagName).map(tag=>{
      val updatedTags = tags + (tagName -> tag.withFile(this.fileName, version))
      create(isInHeader, this.fileName, updatedTags)
      }
    ).getOrElse(self)
    }
  }

  private case class RlogSingleTagParseState(override val isInHeader: Boolean, val fileName: String, val tag: CVSTag) extends RlogParseState[RlogSingleTagParseState] {
    def this(name: String) = this(RlogParseState.isFirstLineHeader, "", CVSTag(name))
    
    override protected def create(isInHeader: Boolean): RlogSingleTagParseState = new RlogSingleTagParseState(isInHeader, fileName, tag)
    override protected def self = this
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
    val process = stringToProcess(command);
    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new RlogSingleTagParseState(tagName))(_ withLine _).tag
  }
  
  def resolveTags(tagNames: Iterable[String]): Iterable[CVSTag] = {
    val command = cvsString + "rlog -h" + module.map(" " + _ + "/").getOrElse("")
    log("running command:\n" + command)
    val process = stringToProcess(command);
    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new RlogMultiTagParseState(tagNames))(_ withLine _).tags.values
  }
  
  def resolveAllTagsAndBranches: Set[CVSTag] = {
    val command = cvsString + "rlog -h" + module.map(" " + _ + "/").getOrElse("")
    log("running command:\n" + command)
    val process = stringToProcess(command);
    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new RlogAllTagParseState())(_ withLine _).tags.values.toSet
  }
  
  def resolveAllBranches: Set[CVSTag] = {
    val command = cvsString + "rlog -h" + module.map(" " + _ + "/").getOrElse("")
    log("running command:\n" + command)
    val process = stringToProcess(command);
    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new RlogAllBranchParseState())(_ withLine _).tags.values.toSet
  }

  def getCommit(filename: String, version: CVSFileVersion): Option[CVSCommit] = {
    val command = cvsString + "rlog " + " -r" + version.toString +" "+ module.map( _ + "/").getOrElse("") + filename
    log("running command:\n" + command)
    parseRlogLines(stringToProcess(command)).headOption
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
    def this() = this(RlogParseState.isFirstLineHeader, Vector(), Vector(), Vector())
    def this(emptyCollection: Seq[CVSCommit]) = this(RlogParseState.isFirstLineHeader, emptyCollection, Vector(), Vector())

    private def updatedCommits = if (isInHeader) {
      commits
    } else {
      commits :+ commitFromRLog(headerBuffer, commitBuffer)
    }
    
    override protected def self = this
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

  private def parseRlogLines(lines: Iterable[String]): Seq[CVSCommit] = {
	lines.foldLeft(new RlogCommitParseState())(_.withLine(_)).commits
  }
  private def parseRlogLines(process: ProcessBuilder): Seq[CVSCommit] = {
    val state = new ProcessAsTraversable(process,line=>log(line))
    	.foldLeft(new RlogCommitParseState(Vector[CVSCommit]()))(_ withLine _)
    state.commits
  }
}

object CVSRepository {
  def apply() = new CVSRepository();
  def apply(cvsroot: String, module: String) = new CVSRepository(cvsroot, module);
  private val CVS_DATE_FORMAT= new SimpleDateFormat("yyyy-MM-dd kk:mm:ss Z",Locale.UK)
  private val CVS_SHORT_DATE_FORMAT= new SimpleDateFormat("yyyy/MM/dd",Locale.UK)
}