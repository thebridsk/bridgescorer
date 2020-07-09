package com.github.thebridsk.bridgedemo.utils

import java.io.IOException
import java.util.logging.Level
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.FileVisitor
import java.nio.file.FileVisitResult
import java.util.EnumSet
import java.nio.file.FileVisitOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.SimpleFileVisitor
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.StandardCopyOption
import scala.jdk.CollectionConverters._
import java.lang.ProcessBuilder.Redirect
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import scala.io.Source
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.github.thebridsk.utilities.logging.Logger

object ProcessPrivate {
  implicit val ec = ExecutionContext.global

  val logger: Option[Logger] = Some( Logger[MyProcess]() )

}

class MyProcess {
  import ProcessPrivate._

  val counter = new AtomicInteger()

  def logStream( log: String => Unit, in: InputStream ) = {
    Future {
      val buf = Source.fromInputStream( in, "UTF8" )
      try {
        for ( line <- buf.getLines()) {
          log(line)
        }
      } finally {
        buf.close
      }
    }
  }

  def exec( cmd: List[String], cwd: File  ): Process = {
    val i = counter.incrementAndGet()
    logger.foreach( l => l.info(s"""Executing OS command($i): ${cmd.mkString(" ")}""") )
    val pb = new ProcessBuilder().command(cmd.asJava).directory(cwd).inheritIO()
    val proc = pb.start()
    proc
  }

  def exec( cmd: List[String], addEnvp: Map[String,String], cwd: File  ): Process = {
    val i = counter.incrementAndGet()
    logger.foreach( l => l.info(s"""Executing OS command($i): ${cmd.mkString(" ")}""") )
    val pb = new ProcessBuilder().command(cmd.asJava).directory(cwd).inheritIO()
    val env = pb.environment()
    addEnvp.foreach( e => env.put(e._1, e._2) )
    val proc = pb.start()
    proc
  }

  /**
    * execute a program, stdin of program gets string in stdin argument.
    *
    * @param cmd
    * @param addEnvp
    * @param cwd
    * @param stdin the data in UTF8
    * @return
    */
  def exec( cmd: List[String], addEnvp: Map[String,String], cwd: File, stdin: Option[String], pcmd: Option[List[String]] = None ): Process = {
    val i = counter.incrementAndGet()
    logger.foreach( l => l.info(s"""Executing OS command($i): ${pcmd.getOrElse(cmd).mkString(" ")}""") )
    val redirect = logger.map( l => Redirect.PIPE).getOrElse(Redirect.INHERIT)
    val pb = new ProcessBuilder().command(cmd.asJava).directory(cwd).redirectOutput(redirect).redirectError(redirect).redirectInput(Redirect.PIPE)
    val env = pb.environment()
    addEnvp.foreach( e => env.put(e._1, e._2) )
    val proc = pb.start()
    val outfuture = logger.map { l =>
      val f1 = logStream( s => l.info(s), proc.getInputStream())
      val f2 = logStream( s => l.warning(s), proc.getErrorStream())
      Future.foldLeft(List(f1,f2))(0) { (ac,v) => ac }
    }.getOrElse( Future(0) )
    stdin.foreach { data =>
      val pipeIn = proc.getOutputStream()
      val in = new OutputStreamWriter( pipeIn, "UTF8" )
      try {
        in.write(data)
        in.flush
      } finally {
        in.close
      }
    }
    Await.ready( outfuture, Duration.Inf )
    proc
  }

  /**
    * Execute a command using bash shell on windows.  This requires WSL on windows.
    * On linux, the command will be executed as is.
    * @param cmd
    * @param addEnvp
    * @param cwd
    * @param stdin the data in UTF8
    * @param printcmd the command to print, with pw removed
    * @return Process object
    */
  def bash( cmd: List[String], addEnvp: Map[String,String], workingDirectory: Option[File], stdin: Option[String] = None, printcmd: Option[List[String]] = None ) = {
    val wd = workingDirectory.getOrElse( new File(".") ).getAbsoluteFile()
    val c = sys.props.getOrElse("os.name", "oops").toLowerCase() match {
      case os: String if (os.contains("win")) => List("bash", "-c", cmd.mkString(" "))
      case os: String if (os.contains("mac")) => cmd
      case os: String if (os.contains("nix")||os.contains("nux")) => cmd
      case os =>
        logger.foreach( _.severe("Unknown operating system: "+os) )
        throw new Exception("Unknown operating system: "+os)
    }
    exec( c, addEnvp, wd, stdin, printcmd );
  }

  def startOnWindows( cwd: File, addEnvp: Option[Map[String,String]], cmd: String* ) = {
    val env = addEnvp.getOrElse(Map.empty)
    exec( List("cmd", "/c", cmd.mkString(" ")), env, cwd);
  }

  def startOnMac( cwd: File, addEnvp: Option[Map[String,String]], cmd: String* ) = {
    exec( "sh"::"-c"::cmd.mkString(" ")::Nil, cwd );
  }

  def startOnLinux( cwd: File, addEnvp: Option[Map[String,String]], cmd: String* ) = {
    val c = "sh"::"-c"::cmd.mkString(" ")::Nil
    exec(c, cwd);
  }

  /**
   * @param cwd the current directory for the started process
   * @param cmd the command with arguments.  The first element is the command, the rest are arguments.
   */
  def start( cwd: File, cmd: String* ): Process = {
    startoe(cwd,None,cmd:_*)
  }


  /**
   * @param cwd the current directory for the started process
   * @param cmd the command with arguments.  The first element is the command, the rest are arguments.
   */
  def start( cwd: File, addEnvp: Map[String,String], cmd: String* ): Process = {
    startoe(cwd,Some(addEnvp),cmd:_*)
  }

  /**
   * @param cwd the current directory for the started process
   * @param cmd the command with arguments.  The first element is the command, the rest are arguments.
   */
  def startoe( cwd: File, addEnvp: Option[Map[String,String]], cmd: String* ): Process = {
    try {
      sys.props.getOrElse("os.name", "oops").toLowerCase() match {
        case os: String if (os.contains("win")) => startOnWindows(cwd,addEnvp,cmd: _*)
        case os: String if (os.contains("mac")) => startOnMac(cwd,addEnvp,cmd: _*)
        case os: String if (os.contains("nix")||os.contains("nux")) => startOnLinux(cwd,addEnvp,cmd: _*)
        case os =>
          logger.foreach( l => l.severe("Unknown operating system: "+os) )
          throw new Exception("Unknown operating system: "+os)
      }
    } catch {
      case x: IOException =>
        logger.foreach( l => l.severe("Unable to run command: "+cmd.mkString(" ")+", error="+x ) )
        throw new Exception("Unable to run command: "+cmd.mkString(" ")+", error="+x )
    }

  }

  def keytool(
      cmd: List[String],
      workingDirectory: Option[File] = None,
      env: Option[Map[String,String]] = None,
      printcmd: Option[List[String]] = None,
      stdin: Option[String] = None
  ) = {
    runProgram("keytool",cmd,workingDirectory,env,printcmd,stdin)
  }

  def runProgram(
      pgm: String,
      cmd: List[String],
      workingDirectory: Option[File] = None,
      env: Option[Map[String,String]] = None,
      printcmd: Option[List[String]] = None,
      stdin: Option[String] = None
  ): Unit = {
    val pcmd = pgm::printcmd.getOrElse(cmd)
    val wd = workingDirectory.getOrElse( new File(".") ).getAbsolutePath()
    logger.foreach { l =>
      l.info( s"Working Directory is $wd")
      l.info( s"Running: ${pcmd.mkString(" ")}" )
    }
    val ecmd = pgm::cmd
    val proc = exec(ecmd, env.getOrElse(Map()), workingDirectory.getOrElse(new File(".")), stdin, Some(pcmd) )
    val rc = proc.waitFor()
    if (rc != 0) throw new Error(s"Failed, with rc=${rc} running keytool ${pcmd.mkString(" ")}")
  }

  def runJava(
      cmd: List[String],
      workingDirectory: Option[File] = None,
      env: Option[Map[String,String]] = None,
      printcmd: Option[List[String]] = None,
      stdin: Option[String] = None
  ) = {
    runProgram("java",cmd,workingDirectory,env,printcmd,stdin)
  }

}

object MyProcess {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println( "MyProcess cwd cmd..." )
      System.exit(99)
    }
    val c = args.toList
    val cwd = new File(c.head).getCanonicalFile
    val cmd = c.tail

    val proc = new MyProcess().start(cwd,cmd: _*)
    val rc = proc.waitFor()
    if (rc == 0) {
      println( "Success: "+cmd.mkString(" ") )
    } else {
      println( s"Error ${rc}: "+cmd.mkString(" ") )
    }

    System.exit(rc)
  }

}

class CopyFileVisitor( destDir: Path, srcDir: Path, onlyExt: String ) extends FileVisitor[Path] {

    Files.createDirectories(destDir)

    val ext = "."+onlyExt

    /**
     * Invoked for a directory before entries in the directory are visited.
     *
     * <p> If this method returns {@link FileVisitResult#CONTINUE CONTINUE},
     * then entries in the directory are visited. If this method returns {@link
     * FileVisitResult#SKIP_SUBTREE SKIP_SUBTREE} or {@link
     * FileVisitResult#SKIP_SIBLINGS SKIP_SIBLINGS} then entries in the
     * directory (and any descendants) will not be visited.
     *
     * @param   dir
     *          a reference to the directory
     * @param   attrs
     *          the directory's basic attributes
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    def preVisitDirectory( dir: Path, attrs: BasicFileAttributes ): FileVisitResult = FileVisitResult.CONTINUE

    /**
     * Invoked for a file in a directory.
     *
     * @param   file
     *          a reference to the file
     * @param   attrs
     *          the file's basic attributes
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    def visitFile( file: Path, attrs: BasicFileAttributes ): FileVisitResult = {
      if (attrs.isRegularFile()) {
        if (file.toString().toLowerCase().endsWith(ext)) {
          val tar = destDir.resolve(srcDir.relativize(file))
          Files.copy(file, destDir.resolve(tar), StandardCopyOption.REPLACE_EXISTING)
        }
      }
      FileVisitResult.CONTINUE
    }

    /**
     * Invoked for a file that could not be visited. This method is invoked
     * if the file's attributes could not be read, the file is a directory
     * that could not be opened, and other reasons.
     *
     * @param   file
     *          a reference to the file
     * @param   exc
     *          the I/O exception that prevented the file from being visited
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    def visitFileFailed( file: Path, exc: IOException ): FileVisitResult = FileVisitResult.CONTINUE

    /**
     * Invoked for a directory after entries in the directory, and all of their
     * descendants, have been visited. This method is also invoked when iteration
     * of the directory completes prematurely (by a {@link #visitFile visitFile}
     * method returning {@link FileVisitResult#SKIP_SIBLINGS SKIP_SIBLINGS},
     * or an I/O error when iterating over the directory).
     *
     * @param   dir
     *          a reference to the directory
     * @param   exc
     *          {@code null} if the iteration of the directory completes without
     *          an error; otherwise the I/O exception that caused the iteration
     *          of the directory to complete prematurely
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    def postVisitDirectory( dir: Path, exc: IOException ): FileVisitResult = FileVisitResult.CONTINUE

}

object MyFileUtils {
  import ProcessPrivate._

  /**
   * Copies all files from source directory to target directory.
   * Does NOT recurse into subdirectories.
   *
   * @param src the source directory
   * @param dest the destination directory
   * @param onlyExt only files with this extension
   * @param maxDepth max directory depth, default is 1, copy only files in src
   *
   */
  def copyDirectory( src: File, dest: File, onlyExt: String, maxDepth: Int = 1 ) = {
    val srcPath = src.toPath()
    val destPath = dest.toPath()

    val visitor = new CopyFileVisitor(destPath, srcPath, onlyExt)
    val options = EnumSet.noneOf(classOf[FileVisitOption])
    Files.walkFileTree(srcPath, options, maxDepth, visitor)

  }

  /**
   * Recursivily deletes all files
   * @param directory the base directory
   * @param ext an optional extension of files to delete.  MUST NOT start with ".".
   */
  def deleteDirectory( directory: Path, ext: Option[String] ) = {
    val extension = ext.map( e => "."+e )
    if (directory.toFile().exists()) {
      if (directory.toFile().isDirectory()) {
        Files.walkFileTree(directory, new SimpleFileVisitor[Path]() {
           override
           def visitFile( file: Path, attrs: BasicFileAttributes ): FileVisitResult = {
             val del = extension.map { e =>
               file.toString().toLowerCase().endsWith(e)
             }.getOrElse(true)
             if (del) {
               logger.foreach(_.fine(s"Deleting ${file}"))
               Files.delete(file);
             }
             FileVisitResult.CONTINUE;
           }

           override
           def postVisitDirectory( dir: Path, exc: IOException ): FileVisitResult = {
               logger.foreach(_.fine(s"Deleting ${dir}"))
             try {
               Files.delete(dir);
             } catch {
               case x: DirectoryNotEmptyException =>
             }
             FileVisitResult.CONTINUE;
           }
        })
      } else {
        directory.toFile().delete()
      }
    }
  }

}
