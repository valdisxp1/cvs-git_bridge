package com.valdis.adamsons.cvs

import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Date, Locale}

import com.valdis.adamsons.cvs.rlog.parse.{RlogParseState, RlogTagNameLookupState}
import com.valdis.adamsons.logger.{Logger, SweetLogger}
import com.valdis.adamsons.utils.{FileUtils, ProcessAsTraversable}

import scala.sys.process._

case class CVSRepository(
                          cvsroot: Option[String],
                          module: Option[String],
                          serverDateFormat: DateFormat = CVSRepository.DEFAULT_CVS_DATE_FORMAT) extends SweetLogger {
  protected def logger = Logger
  def this(cvsroot: Option[String]) = this(cvsroot, None)
  def this() = this(None, None)
  def this(cvsroot: String, module: String) = this(Some(cvsroot), Some(module))
  
  /**
   * The path to repository root.
   */
  def root = cvsroot.getOrElse(System.getenv("CVSROOT"))
  
  /**
   * Creates a new repository object with the given module.
   */
  def module(module: String) = CVSRepository(this.cvsroot, Some(module))

  private def cvsRepoCommandPrefix = "cvs" +:  cvsroot.toSeq.flatMap(root => Seq("-d" , root))

  /**
   * @return relative path against chosen module.
   */
  def cleanRCSpath(absolutePath: String) = {
    val pathOnServer = cvsroot.map {
      root =>
        if (root.startsWith(":pserver:")) {
          val cutpos = root.lastIndexOf(':')
          root.drop(cutpos + 1)
        } else {
          root
        }
    }
    absolutePath.drop(pathOnServer.getOrElse("").length + 1 + module.getOrElse("").length + 1).trim.dropRight(2).replace("Attic/", "")
  }

  private def fileContentsProcess(name: String, version: CVSFileVersion) = {
    val fullFileName = module.map(_ + "/").getOrElse("") + name
    val args = cvsRepoCommandPrefix ++ Seq("co", "-p", "-r", version.toString, fullFileName)
    Process(args)
  }

  /**
    * Should only be used for text files.
   * Binary files get corrupted because Java tries to convert them to UTF8.
   */
  def getFileContents(name: String, version: CVSFileVersion) = {
    val process = fileContentsProcess(name, version)
    log("running command:\n" + process)
    process.!!
  }

  def getFile(name: String, version: CVSFileVersion) = {
    val process = fileContentsProcess(name, version)
    log("running command:\n" + process)
    val file = FileUtils.createTempFile("tmp", ".bin")
    //forces the to wait until process finishes.
    process.#>(file).!
    file.deleteOnExit()
    file
  }

  def fileNameList = {
    val command = cvsRepoCommandPrefix ++ Seq("rlog", "-R") ++ module
    log("running command:\n" + command)
    val responseLines = command.lines_!
    responseLines.toList.map(cleanRCSpath)
  }

  private def getTagProcess = {
    val command = cvsRepoCommandPrefix ++ Seq("rlog", "-h") ++ module
    log("running command:\n" + command)
    Process(command)
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
  
  private trait TagParseState[This] extends RlogParseState[This] {
    val fileName: String
    
    protected def withTagEntry(tagName: String, version: CVSFileVersion): This
    protected def withFileName(fileName:String): This
    
    override protected def withHeaderLine(line: String): This = {
      val fileNameUpdated = extractFileName(line).map(withFileName)
      fileNameUpdated.getOrElse {
        lazy val (tagName, version) = {
          val arr = line.split(':')
          (arr(0).trim(), CVSFileVersion(arr(1).trim))
        }
        
        def isTagLine = line.length > 1 && line(0) == '\t'
        if (isTagLine) {
        	withTagEntry(tagName, version)
        } else {
          self
        }
      }
    }
  }
  
  private trait RlogAllTagParseStateLike[This] extends TagParseState[This] {
    val tags: Map[String, CVSTag]
    
    protected def withFileName(fileName:String) = create(isInHeader, fileName, tags)
    protected def create(isInHeader: Boolean,fileName: String, tags: Map[String,CVSTag]): This
    
    protected def withTagEntry(tagName: String, version: CVSFileVersion): This = {
      val previousTag = tags.getOrElse(tagName, CVSTag(tagName))
      val updatedTags = tags + (tagName -> previousTag.withFile(this.fileName, version))
      create(isInHeader, this.fileName, updatedTags)
    }
    
  }

  private case class RlogAllTagParseState(
    override val isInHeader: Boolean,
    override val fileName: String,
    override val tags: Map[String, CVSTag]) extends RlogAllTagParseStateLike[RlogAllTagParseState] {
     
    def this() = this(RlogParseState.isFirstLineHeader, "", Map())
    
    override protected def create(isInHeader: Boolean): RlogAllTagParseState = new RlogAllTagParseState(isInHeader, fileName, tags)
    override protected def self = this
    protected def create(isInHeader: Boolean,fileName: String, tags: Map[String,CVSTag]) = new RlogAllTagParseState(isInHeader, fileName, tags)
  }

  private case class RlogAllBranchParseState(
    override val isInHeader: Boolean,
    override val fileName: String,
    override val tags: Map[String, CVSTag]) extends RlogAllTagParseStateLike[RlogAllBranchParseState] {
    
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

  /**
   * This passes multiple tags and keeps identical tags (identical file versions) using the same immutable map
   * by grouping them accordingly to the tag. Even if differences appear at some point the common ancestry makes
   * at least some recycled parts the same.
   */
  private case class SmartRlogMultiTagParseState(
    override val isInHeader: Boolean,
    override val fileName: String,
    tags: Set[Set[CVSTag]],
    currentChanges: Map[String, CVSFileVersion]) extends TagParseState[SmartRlogMultiTagParseState] {
    
    def this(names: Iterable[String]) = this(RlogParseState.isFirstLineHeader, "", Set(names.map(CVSTag(_)).toSet),Map())
    protected def create(isInHeader: Boolean) = new SmartRlogMultiTagParseState(isInHeader, fileName, tags, currentChanges)
    override protected def self = this

    private def processTags = {
      tags.flatMap {
        group =>
          val versionMap = group.head.fileVersions
          group.map(tag => tag -> currentChanges.get(tag.name)).groupBy(_._2).map {
            entry =>
              val updatedVersionMap = entry._1.map(version => versionMap + (fileName -> version)).getOrElse(versionMap)
              entry._2.map{pair => CVSTag(pair._1.name, updatedVersionMap)}
          }
      }
    }
    
    protected def withFileName(fileName: String) = new SmartRlogMultiTagParseState(isInHeader, fileName, processTags, currentChanges)
    protected def withTagEntry(tagName: String, version: CVSFileVersion) = new SmartRlogMultiTagParseState(isInHeader, fileName, tags, currentChanges + (tagName -> version))
    
  }

  private case class RlogSingleTagParseState(override val isInHeader: Boolean, fileName: String, tag: CVSTag) extends RlogParseState[RlogSingleTagParseState] {
    def this(name: String) = this(RlogParseState.isFirstLineHeader, "", CVSTag(name))
    
    override protected def create(isInHeader: Boolean): RlogSingleTagParseState = new RlogSingleTagParseState(isInHeader, fileName, tag)
    override protected def self = this
    private def create(isInHeader: Boolean,fileName: String, tag: CVSTag) = new RlogSingleTagParseState(isInHeader, fileName, tag)

    override protected def withHeaderLine(line: String): RlogSingleTagParseState = {
      val fileNameUpdated = extractFileName(line).map(create(isInHeader, _, tag))
      fileNameUpdated.getOrElse({
        lazy val (tagName, version) = {
          val arr = line.split(':')
          (arr(0).trim(), CVSFileVersion(arr(1).trim))
        }
        def isTagLine = line.length > 1 && line(0) == '\t'
        def isTheRightTag = tag.name == tagName
        if (isTagLine && isTheRightTag) {
          create(isInHeader, this.fileName, tag.withFile(this.fileName, version))
        } else {
          this
        }
      })
    }
    
  }

  private def rlogHeader: ProcessBuilder = {
    val command = cvsRepoCommandPrefix ++ Seq("rlog", "-h") ++ module
    log("running command:\n" + command)
    Process(command)
  }

  def resolveTag(tagName: String): CVSTag = {
    val process: ProcessBuilder = rlogHeader
    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new RlogSingleTagParseState(tagName))(_ withLine _).tag
  }

  def resolveTags(tagNames: Iterable[String]): Iterable[CVSTag] = {
    val process: ProcessBuilder = rlogHeader

    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new SmartRlogMultiTagParseState(tagNames))(_ withLine _).tags.flatten
  }
  
  def resolveAllTagsAndBranches: Set[CVSTag] = {
    val process: ProcessBuilder = rlogHeader

    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new RlogAllTagParseState())(_ withLine _).tags.values.toSet
  }
  
  def resolveAllBranches: Set[CVSTag] = {
    val process: ProcessBuilder = rlogHeader

    new ProcessAsTraversable(process, line => log(line))
    	.foldLeft(new RlogAllBranchParseState())(_ withLine _).tags.values.toSet
  }

  def getCommitsForTag(tag: CVSTag) = {
    val commandStrings = tag.fileVersions.map {
      case (path, version) =>
        val fullFileName = module.map(_ + "/").getOrElse("") + path
        cvsRepoCommandPrefix ++ Seq ("rlog", "-r" + version.toString, fullFileName)
    }
    log("running command batch")
    commandStrings.foreach(log(_))
    log("end of batch")
    val combinedProcess = commandStrings.map(Process.apply).reduce(_ ### _)
    parseRlogLines(combinedProcess)
  }
  
  def getCommit(filename: String, version: CVSFileVersion): Option[CVSCommit] = {
    val fullFileName = module.map(_ + "/").getOrElse("") + filename
    val command = cvsRepoCommandPrefix ++ Seq ("rlog", "-r" + version.toString, fullFileName)
    log("running command:\n" + command)
    parseRlogLines(Process(command)).headOption
  }


  /**
   * A branch of None means trunk. A empty (None) date means the search is not limited in that direction.
   */
  def getCommitList(branch: Option[String]=None, start: Option[Date]=None, end: Option[Date]=None): Seq[CVSCommit] = {
    val startString = start.map(CVSRepository.CVS_SHORT_DATE_FORMAT.format)
    val endString = end.map(CVSRepository.CVS_SHORT_DATE_FORMAT.format)
    val dateString = if (start.isDefined || end.isDefined) {
      Some("-d \"" + startString.getOrElse("") + "<" + endString.getOrElse("") + "\"")
     } else {
      None
     }
    val revisonSeq = branch.map(id => ("-r" + id) :: Nil) getOrElse Seq("-b")
    val command = cvsRepoCommandPrefix ++ Seq("rlog") ++ revisonSeq ++ dateString ++ module
    log("running command:\n" + command)
    parseRlogLines(Process(command))
  }
  
  private def missing(field:String) = throw new IllegalStateException("cvs rlog malformed. Mandatory field '"+field+"' missing")

  private case class RlogCommitParseState(
                                           isInHeader: Boolean,
                                           commits: Seq[CVSCommit],
                                           headerBuffer: Vector[String],
                                           commitBuffer: Vector[String]) extends RlogParseState[RlogCommitParseState]{
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

  private def parseDate(dateString: String) = {
    CVSRepository.ACCEPTED_DATE_FORMATS.view.flatMap {
      format =>
        try {
          Some(format.parse(dateString))
        } catch {
          case _: java.text.ParseException => None
        }
    }.head
  }
  
  private def commitFromRLog(header: IndexedSeq[String], commit: IndexedSeq[String]): CVSCommit = {
    val headerPairs = header.toList.map(_.split(": ")).filter(_.length > 1).map((x) => x(0).trim -> x(1))
    val headerMap = headerPairs.toMap
    val fileName = cleanRCSpath(headerMap.getOrElse("RCS file", missing("file name(RCS file)")))

    val revisionStr = commit.head.trim.split(' ')(1)
    val revision = CVSFileVersion(revisionStr)
    val params = commit(1).trim.dropRight(1).split(';').map(_.split(": ")).map((x) => x(0).trim -> x(1).trim).toMap
    val date = parseDate(params.getOrElse("date",missing("date")))
    val author = params.getOrElse("author",missing("author"))
    val commitId = params.get("commitid")
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
  def apply() = new CVSRepository()
  def apply(cvsroot: String, module: String) = new CVSRepository(cvsroot, module)
  private val DEFAULT_CVS_DATE_FORMAT= new SimpleDateFormat("yyyy-MM-dd kk:mm:ss Z",Locale.UK)
  private val ACCEPTED_DATE_FORMATS = {
	val formatStrings = Seq("yyyy-MM-dd kk:mm:ss Z",
							"yyyy/MM/dd kk:mm:ss")
	formatStrings.map(new SimpleDateFormat(_,Locale.UK))
  }
  private val CVS_SHORT_DATE_FORMAT= new SimpleDateFormat("yyyy/MM/dd",Locale.UK)
}