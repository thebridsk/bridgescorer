package com.example.util

import utils.logging.Logger
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.io.Writer
import java.io.BufferedWriter
import java.text.SimpleDateFormat
import java.util.Date

object MemoryMonitor {
  val log = Logger[MemoryMonitor]

  var activeMonitor: Option[MemoryMonitor] = None


  def start( outfile: String = "MemoryMonitor.csv" ) = synchronized {
    log.info( s"Starting Memory Monitor with $outfile")
    activeMonitor match {
      case Some(am) =>
        log.warning( "Memory monitor is already running" )
      case None =>
        val m = new MemoryMonitor( getFileName(outfile, getDate()) )
        activeMonitor = Some(m)
        m.start()
    }
  }

  def stop() = synchronized {
    activeMonitor match {
      case Some(am) =>
        log.info( "Stopping Memory Monitor")
        am.stop()
        activeMonitor = None
      case None =>
//        log.warning( "Memory monitor is not running" )
    }
  }

  private val fSDF = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");

  private def getDate() = {
    var d = new Date();
    fSDF.format(d)
  }


  /**
   * @param unique
   * @param date
   */
  private def getFileName( pattern: String, date: String ): String =
  {
      val b = new StringBuilder();

      val len = pattern.length();
      var i = 0;
      import scala.util.control.Breaks._
      breakable {
        while (i < len)
        {
            var c = pattern.charAt(i);
            if (c == '%')
            {
                i+=1;
                if (i >= len)
                {
                    // ignore an isolated % at end of string
                    break;
                }
                c = pattern.charAt(i);
                c match {
                  case '%' =>
                      b.append(c);
                  case 't' =>
                  {
                      var tmpDir = System.getProperty("java.io.tmpdir");
                      if (tmpDir == null) {
                          tmpDir = System.getProperty("user.home");
                      }
                      b.append(tmpDir);
                  }
                  case 'h' =>
                      b.append(System.getProperty("user.home"));
                  case 'd' =>
                      b.append(date);
                  case _ =>
                    b.append(c)
                }
            } else {
              b.append(c)
            }
            i+=1;
        }
      }
      return b.toString();
  }

}

import MemoryMonitor._

class MemoryMonitor( outfile: String ) {

  private class Monitor( name: String ) extends Thread(name) {

    setDaemon(true)

    private var active = false
    private val sleepLock = new Object

    private var startTime = 0L

    private var interval = 15000

    override
    def run() = {
      val out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( outfile ) ))
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
        log.info( s"Memory monitor closed: $outfile" )
      }
    }

    val rt = Runtime.getRuntime

    def writeRow( out: Writer ) = {
      val free = rt.freeMemory()
      val total = rt.totalMemory()
      val max = rt.maxMemory()
      row( out, free, total, max )
    }

    def header( out: Writer ) = {
      val head = "time,used,free,total,max"
      log.fine(head)
      out.write( head )
      out.write( "\n" )
    }

    def row( out: Writer, free: Long, total: Long, max: Long ) = {
      val dt = System.currentTimeMillis() - startTime
      val r = s"""$dt,${total-free},$free,$total,$max"""
      log.fine(r)
      out.write(r)
      out.write("\n")
      out.flush()
    }

    def stopMonitor() = sleepLock.synchronized  {
      active = false
      sleepLock.notify()
    }
  }

  private var activeMonitor: Option[Monitor] = None

  def start( outfile: String = "MemoryMonitor.csv" ) = synchronized {
    activeMonitor match {
      case Some(am) =>
        log.warning( "Memory monitor is already running" )
      case None =>
        val m = new Monitor( outfile )
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
        log.warning( "Memory monitor is not running" )
    }
  }


}
