package com.github.thebridsk.bridge.server.util

import com.github.thebridsk.utilities.logging.Logger
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.io.Writer
import java.io.BufferedWriter
import java.util.Date
import com.github.thebridsk.utilities.logging.FileHandler
import java.io.File
import java.util.regex.Pattern
import java.io.FilenameFilter
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.time.Instant
import java.time.ZoneId

object MemoryMonitor {
  val log = Logger[MemoryMonitor]

  var activeMonitor: Option[MemoryMonitor] = None

  def start(outfile: String = "MemoryMonitor.csv") = synchronized {
    log.info(s"Starting Memory Monitor with $outfile")
    activeMonitor match {
      case Some(am) =>
        log.warning("Memory monitor is already running")
      case None =>
        val m = new MemoryMonitor(outfile)
        activeMonitor = Some(m)
        m.start()
    }
  }

  def stop() = synchronized {
    activeMonitor match {
      case Some(am) =>
        log.info("Stopping Memory Monitor")
        am.stop()
        activeMonitor = None
      case None =>
//        log.warning( "Memory monitor is not running" )
    }
  }

  private var fCount = 10

  private val fSDF = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss.SSS").withZone( ZoneId.systemDefault() );
  private val dateRegex = """\d\d\d\d\.\d\d\.\d\d\.\d\d\.\d\d\.\d\d\.\d\d\d"""

  private def getDate() = {
    fSDF.format(Instant.now())
  }

  /**
    * @param unique
    * @param date
    * @param regex generate regex to search for log files
    */
  private def getFileName(
      pattern: String,
      date: String,
      regex: Boolean = false
  ): String = {
    val b = new StringBuilder();

    val len = pattern.length();
    var i = 0;
    import scala.util.control.Breaks._
    breakable {
      while (i < len) {
        var c = pattern.charAt(i);
        if (c == '%') {
          i += 1;
          if (i >= len) {
            // ignore an isolated % at end of string
            break;
          }
          c = pattern.charAt(i);
          c match {
            case '%' =>
              b.append(c);
            case 't' => {
              var tmpDir = System.getProperty("java.io.tmpdir");
              if (tmpDir == null) {
                tmpDir = System.getProperty("user.home");
              }
              b.append(tmpDir);
            }
            case 'h' =>
              b.append(System.getProperty("user.home"));
            case 'd' =>
              if (regex) {
                b.append(dateRegex)
              } else {
                b.append(date);
              }
            case _ =>
              if (regex) {
                if (FileHandler.special.indexOf(c) >= 0) {
                  b.append('\\')
                }
              }
              b.append(c)
          }
        } else {
          if (regex) {
            if (FileHandler.special.indexOf(c) >= 0) {
              b.append('\\')
            }
          }
          b.append(c)
        }
        i += 1;
      }
    }
    return b.toString();
  }

  private def cleanupExistingFiles(pattern: String) = {
    // This is called when starting or rotating file.
    // need to check existing files to see if they match the pattern.
    // If they do, need to add them to fOldFiles in cron order, with oldest at index 0.

    val outpat = pattern.replace('\\', '/')

    val filename = getFileName(outpat, "", false)
    val parent = new File(filename).getParent
    val dir =
      (if (parent == null) "." else parent + File.separator).replace('\\', '/');

    val reg = if (dir.length() > 0 && outpat.startsWith(dir)) {
      outpat.substring(dir.length())
    } else {
      outpat
    }

    val regex = getFileName(reg, "", true)

    val pat = Pattern.compile(regex)

    val dirf = new File(dir)
    val files = dirf.list(new FilenameFilter() {
      def accept(dir1: File, name: String) = {
        pat.matcher(name).matches()
      }
    })

    import scala.jdk.CollectionConverters._
    val sortedfiles = files.toList.sorted
//    println(s"Found ${sortedfiles.length} log files with pattern ${pat}")
    if (sortedfiles.length > fCount) {
      val del = sortedfiles.length - fCount
      sortedfiles.take(del).foreach { f =>
        val todelete = new File(dirf, f)
//        println( s"  Deleting ${todelete}" )
        todelete.delete
      }
      sortedfiles.drop(del)
    }
  }

}

import MemoryMonitor._

class MemoryMonitor(pattern: String) {

  val outfile = getFileName(pattern, getDate())

  private class Monitor(name: String) extends Thread(name) {

    setDaemon(true)

    private var active = false
    private val sleepLock = new Object

    private var startTime = 0L

    private var interval = 15000

    override def run() = {
      val out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outfile))
      )
      try {
        header(out)
        active = true
        startTime = System.currentTimeMillis()
        while (active) {
          try {
            sleepLock.synchronized {
              sleepLock.wait(interval)
            }
          } catch {
            case x: Exception =>
          }

          writeRow(out)
        }
      } finally {
        out.flush()
        out.close()
        log.info(s"Memory monitor closed: $outfile")
      }
    }

    val rt = Runtime.getRuntime

    def writeRow(out: Writer) = {
      val free = rt.freeMemory()
      val total = rt.totalMemory()
      val max = rt.maxMemory()
      row(out, free, total, max)
    }

    def header(out: Writer) = {
      val head = "time,used,free,total,max"
      log.fine(head)
      out.write(head)
      out.write("\n")
    }

    def row(out: Writer, free: Long, total: Long, max: Long) = {
      val dt = System.currentTimeMillis() - startTime
      val r = s"""$dt,${total - free},$free,$total,$max"""
      log.fine(r)
      out.write(r)
      out.write("\n")
      out.flush()
    }

    def stopMonitor() = sleepLock.synchronized {
      active = false
      sleepLock.notify()
    }
  }

  private var activeMonitor: Option[Monitor] = None

  def start() = synchronized {
    activeMonitor match {
      case Some(am) =>
        log.warning("Memory monitor is already running")
      case None =>
        log.info(
          s"MemoryMonitor out file pattern is: $pattern, outfile: $outfile"
        )
        cleanupExistingFiles(pattern)
        val m = new Monitor(outfile)
        activeMonitor = Some(m)
        m.start()
    }
  }

  def stop() = synchronized {
    activeMonitor match {
      case Some(am) =>
        am.stopMonitor()
        activeMonitor = None
      case None =>
        log.warning("Memory monitor is not running")
    }
  }

}
