package com.github.thebridsk.bridge.server

import com.github.thebridsk.utilities.logging.Logger
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
import scala.reflect.io.File

object Browser {

  val log = Logger(Browser.getClass.getName)

  def logStream(
      level: Level,
      name: String,
      prefix: String,
      i: Int,
      is: InputStream
  ) = {
    import resource._

    for (in <- managed(new BufferedReader(new InputStreamReader(is)))) {
      var line: String = null
      while ({ line = in.readLine(); line } != null) {
        log.log(level, s"$prefix($i,$name): $line")
      }
      log.log(level, s"Done with output for $prefix($i,$name)")
    }

  }

  def logOutput(name: String, i: Int) = new ProcessIO(
    in => { in.close },
    out => { logStream(Level.INFO, name, "out", i, out) },
    err => { logStream(Level.WARNING, name, "err", i, err) },
    false
  )

  def logExitCode(name: String, i: Int, p: Process) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val rc = p.exitValue()
      log.info(s"Exit code for ($i,$name): $rc")
    }
    p
  }

  val counter = new AtomicInteger()

  def exec(cmd: String) = {
    val i = counter.incrementAndGet()
    log.info(s"Executing OS command($i): $cmd")
    logExitCode(cmd, i, Process(cmd).run(logOutput(cmd, i)))
  }

  def exec(cmd: List[String]) = {
    val i = counter.incrementAndGet()
    log.info(s"""Executing OS command($i): ${cmd.mkString(" ")}""")
    logExitCode(cmd(0), i, Process(cmd).run(logOutput(cmd(0), i)))
  }

  val chromeBrowsers = List(
    """C:\Program Files\Google\Chrome\Application\chrome.exe""",
    """C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"""
  )

  def startOnWindows(url: String, fullscreen: Boolean): Option[Process] = {
    val chrome = chromeBrowsers.find(f => File(f).isFile)
    val cmd =
      (if (fullscreen) {
         chrome
           .map(c => List(c, "--start-fullscreen"))
           .getOrElse(List("cmd", "/c", "start "))
       } else {
         List("cmd", "/c", "start ")
       }) ::: List(url)
    Some(exec(cmd))
  }

  def startOnMac(url: String, fullscreen: Boolean) = {
    exec(List("open", url));
  }

  def startOnLinux(url: String, fullscreen: Boolean) = {
    val browsers = "epiphany" :: "firefox" :: "mozilla" :: "konqueror" ::
      "netscape" :: "opera" :: "links" :: "lynx" :: Nil
    val cmd = browsers
      .map { b =>
        b + " \"" + url + "\""
      }
      .mkString(" || ")
    val c = List("sh", "-c", cmd)

    exec(c);
  }

  def start(url: String, fullscreen: Boolean = false): Option[Process] = {
    try {
      sys.props.getOrElse("os.name", "oops").toLowerCase() match {
        case os: String if (os.contains("win")) =>
          startOnWindows(url, fullscreen)
        case os: String if (os.contains("mac")) =>
          Some(startOnMac(url, fullscreen))
        case os: String if (os.contains("nix") || os.contains("nux")) =>
          Some(startOnLinux(url, fullscreen))
        case os =>
          log.severe("Unknown operating system: " + os)
          None
      }
    } catch {
      case x: IOException =>
        log.warning("Unable to start browser:", x)
        None
    }

  }
}
