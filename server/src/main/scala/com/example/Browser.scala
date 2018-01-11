package com.example

import utils.logging.Logger
import java.io.IOException
import scala.sys.process.ProcessIO
import java.util.logging.Level
import java.io.OutputStream
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.sys.process.Process
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future

object Browser {

  val log = Logger(Browser.getClass.getName )

  def logStream( level: Level, name: String, prefix: String, i: Int, is: InputStream ) = {
    import resource._

    for (
      in <- managed( new BufferedReader( new InputStreamReader( is ) ) )
    ) {
      var line: String = null
      while ({line = in.readLine(); line} != null) {
        log.log(level, s"$prefix($i,$name): $line")
      }
      log.log(level, s"Done with output for $prefix($i,$name)")
    }

  }

  def logOutput( name: String, i: Int ) = new ProcessIO(
      in => { in.close },
      out => { logStream( Level.INFO, name, "out", i, out ) },
      err => { logStream( Level.WARNING, name, "err", i, err )},
      false
      )

  def logExitCode( name: String, i: Int, p: Process ) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val rc = p.exitValue()
      log.info( s"Exit code for ($i,$name): $rc")
    }
    p
  }

  val counter = new AtomicInteger()

  def exec( cmd: String ) = {
    val i = counter.incrementAndGet()
    log.info(s"Executing OS command($i): $cmd")
    logExitCode( cmd, i, Process(cmd).run(logOutput(cmd, i)))
  }

  def exec( cmd: List[String] ) = {
    val i = counter.incrementAndGet()
    log.info(s"""Executing OS command($i): ${cmd.mkString(" ")}""")
    logExitCode( cmd(0), i, Process(cmd).run(logOutput(cmd(0), i)))
  }
  def startOnWindows( url: String ) = {
    exec( List("cmd", "/c", "start " + url));
  }

  def startOnMac( url: String ) = {
    exec( List("open",url));
  }

  def startOnLinux( url: String ) = {
    val browsers = "epiphany"::"firefox"::"mozilla"::"konqueror"::
                   "netscape"::"opera"::"links"::"lynx"::Nil
    val cmd = browsers.map{b => b+" \""+url+"\"" }.mkString(" || ")
    val c = List("sh","-c",cmd)

    exec(c);
  }

  def start( url: String ): Option[Process] = {
    try {
      sys.props.getOrElse("os.name", "oops").toLowerCase() match {
        case os: String if (os.contains("win")) => Some(startOnWindows(url))
        case os: String if (os.contains("mac")) => Some(startOnMac(url))
        case os: String if (os.contains("nix")||os.contains("nux")) => Some(startOnLinux(url))
        case os =>
          log.severe("Unknown operating system: "+os)
          None
      }
    } catch {
      case x: IOException =>
        log.warning("Unable to start browser:",x )
        None
    }

  }
}
