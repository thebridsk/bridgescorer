package com.github.thebridsk.bridge.server.test.util

import scala.concurrent.duration._
import scala.language.postfixOps
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Logging
import java.io.PrintWriter
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

object MonitorTCP extends Logging {

  val log: Logger = Logger[MonitorTCP]()

  val toMonitorFile = "ToMonitorFile"
  val monitorFileDefault = "logs/unittestTcpMonitorTimeWait.csv"

  /**
    * flag to determine if waiting for connections is used.
    * Configure by setting the System Property DisableMonitorTCP or environment variable DisableMonitorTCP
    * to "true" or "false".
    * If this property is not set, then the os.name system property is used, disabled on all systems.
    * Setting this to false implies disableMonitorTCP is false.
    */
  val disableTCPSleep: Boolean = {
    ParallelUtils.getPropOrEnv("DisableTCPSleep") match {
      case Some(v) =>
        v.toBoolean
      case None =>
        sys.props.getOrElse("os.name", "oops").toLowerCase() match {
          case os: String if (os.contains("win"))                       => true
          case os: String if (os.contains("mac"))                       => true
          case os: String if (os.contains("nix") || os.contains("nux")) => true
          case os =>
            log.severe("Unknown operating system: " + os)
            true
        }
    }
  }

  /**
    * flag to determine if TCP Time Wait monitoring is used.
    * Configure by setting the System Property DisableMonitorTCP or environment variable DisableMonitorTCP
    * to "true" or "false".
    * If this property is not set, then the os.name system property is used, disabled on all systems.
    */
  val disableMonitorTCP: Boolean = disableTCPSleep && {
    ParallelUtils.getPropOrEnv("DisableMonitorTCP") match {
      case Some(v) =>
        v.toBoolean
      case None =>
        sys.props.getOrElse("os.name", "oops").toLowerCase() match {
          case os: String if (os.contains("win"))                       => true
          case os: String if (os.contains("mac"))                       => true
          case os: String if (os.contains("nix") || os.contains("nux")) => true
          case os =>
            log.severe("Unknown operating system: " + os)
            true
        }
    }
  }

  if (disableMonitorTCP) {
    log.fine(
      s"""disableTCPSleep=${disableTCPSleep} disableMonitorTCP=${disableMonitorTCP}"""
    )
  }

  def getNumberTimeWaitConnections(): Int = {

    import sys.process._
    import scala.language.postfixOps

    val r = "netstat -an" !!

    "TIME_WAIT".r.findAllMatchIn(r).length

  }

  val starttime: Long = System.currentTimeMillis()

  def timeSinceStart(): String = {
    val cur = System.currentTimeMillis()
    val delta = (cur - starttime) / 1000.0
    f"$delta%10.3f"
  }

  val format: DateTimeFormatter =
    DateTimeFormatter.ofPattern("hh:mm:ss").withZone(ZoneId.systemDefault())
  def showTime(): String = {
    format.format(Instant.now())
  }

  def getPrefix(): String = {
    timeSinceStart()
  }

  def showNumberConnectionsInTimeWait(): Unit = {

    logger.fine(getPrefix() + " " + getNumberTimeWaitConnections())
  }

  val seconds = 5

  val runForMinutes = 25

  def main(args: Array[String]): Unit = {
    showNumberConnectionsInTimeWait()
    for (i <- 0 to runForMinutes * 60 / seconds) {
      Thread.sleep(seconds * 1000L)
      showNumberConnectionsInTimeWait()
    }
  }

  var connectionsThreshold = 7000

  val period: FiniteDuration = 5 seconds

  val defaultMaxWait: FiniteDuration = 30 seconds

  class TimeoutException extends Exception

  /**
    * The lock object.
    * All fields and methods can only be used when holding the lock (synchronized)
    */
  object lock {
    @volatile var numberWaiting: Int = 0
    @volatile var pollingThread: Option[Thread] = None
    @volatile var connectionsWhileActive = 0
    @volatile var startedWaiting: Long = 0
    @volatile var totalWait: Long = 0

    /**
      * Get the number of connections and test against the threshold.
      * Must be called while synchronized on <code>lock</code>.
      * @return true if the number of connections is less than threshold
      */
    def checkThreshold(): Boolean = {
      connectionsWhileActive = getNumberTimeWaitConnections()
      logger.info(
        "checkThreshold: connectionsInTimeWait " + lock.connectionsWhileActive
      )
      testThreshold()
    }

    /**
      * Test against the threshold.
      * @return true if the number of connections is less than threshold
      */
    def testThreshold(): Boolean =
      synchronized {
        connectionsWhileActive <= connectionsThreshold
      }

    /**
      * waits for period, or stop-cur, whichever is smaller
      * Must be called synchronized on <code>lock</code>
      * @param stop the stop time in milliseconds since 1/1/1970
      * @return true if past stop time.
      */
    def sleepFor(stop: Long): Option[Boolean] = {
      try {
        val cur = System.currentTimeMillis()
        val delta = stop - cur
        if (delta <= 0) throw new TimeoutException
        val sl = scala.math.min(delta, period.toMillis)
        lock.wait(sl)
      } catch {
        case x: TimeoutException =>
      } finally {
        val cur = System.currentTimeMillis()
        totalWait = totalWait - startedWaiting + cur
        startedWaiting = cur
      }
      testForDoneWait(stop)
    }

    /**
      * @return Some(false) if done waiting, None if continue to wait
      */
    def testForDoneWait(stop: Long): Option[Boolean] = {
      if (stop < System.currentTimeMillis()) Some(false)
      else None
    }

    /**
      * waits for the number of connections to be below the threshold.
      * Waits until connections are below threshold or stop time passed.
      * Must be called synchronized on <code>lock</code>
      * @param stop the stop time in milliseconds since 1/1/1970
      * @return if Some(true) - done, below threshold
      *            Some(false) - done, above threshold
      *            None - continue polling
      */
    def waitUntil(stop: Long): Option[Boolean] =
      synchronized {
        val currentThread = Thread.currentThread()
        numberWaiting = numberWaiting + 1
        if (numberWaiting == 1) startedWaiting = System.currentTimeMillis()
        try {
          var done: Option[Boolean] = None
          while (done.isEmpty) {
            done = if (numberWaiting == 1) {
              pollingThread = Some(currentThread)
              if (checkThreshold()) Some(true)
              else {
                sleepFor(stop)
              }
            } else {
              // More than one connection
              pollingThread match {
                case Some(pt) =>
                  if (pt == currentThread) {
                    if (checkThreshold()) Some(true)
                    else {
                      sleepFor(stop)
                    }
                  } else {
                    if (testThreshold()) Some(true)
                    else {
                      sleepFor(stop)
                    }
                  }
                case None =>
                  pollingThread = Some(currentThread)
                  if (checkThreshold()) Some(true)
                  else {
                    sleepFor(stop)
                  }
              }
              None
            }
          }
          done
        } finally {
          numberWaiting = numberWaiting - 1
          pollingThread match {
            case Some(pt) if (pt == currentThread) => pollingThread = None
            case _                                 =>
          }
          if (numberWaiting == 0) {
            val cur = System.currentTimeMillis()
            totalWait = totalWait - startedWaiting + cur
            startedWaiting = 0
          }
        }
      }

  }

  /**
    * Wait until the number of connections in TIME_WAIT is below the threshold
    * @param maxWait the maximum time to wait
    * @return true - number below threshold
    *         false - number above threshold
    */
  def waitForConnections(maxWait: Duration): Boolean = {
    if (!disableTCPSleep) {
      val start = System.currentTimeMillis()
      val stop = start + maxWait.toMillis
      val rc = lock.waitUntil(stop) match {
        case Some(rc) => rc
        case None     => false
      }

      val end = System.currentTimeMillis()
      val waited = (end - start).milliseconds

      logger.info(
        "waitForConnections waited for " + waited + ", maxWait " + maxWait + ", returning " + rc
      )
      rc
    } else {
      val waitfor = Math.min(maxWait.toMillis, 500)
      Thread.sleep(waitfor)
      true
    }
  }

  def startMonitor(
      pw: PrintWriter,
      period: Duration = 5 seconds
  ): MonitorTCP = {
    val mon = new MonitorTCP(pw, period)
    mon.start
    mon
  }

  var currentMonitor: Option[MonitorTCP] = None
  var currentMonitorUsage = 0

  def startMonitoring(tofilename: Option[String] = None): Unit =
    synchronized {
      if (!disableMonitorTCP) {
        currentMonitor match {
          case Some(m) =>
            currentMonitorUsage += 1
          case None =>
            currentMonitorUsage = 1
            val fn = tofilename.getOrElse(
              getProp(toMonitorFile).getOrElse(monitorFileDefault)
            )
            val pw = new PrintWriter(
              new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fn), "UTF8")
              ),
              true
            )
            currentMonitor = Some(startMonitor(pw))
        }
      }
    }

  def stopMonitoring(): Unit =
    synchronized {
      if (!disableMonitorTCP) {
        currentMonitorUsage -= 1
        if (currentMonitorUsage == 0) {
          currentMonitor match {
            case Some(m) =>
              currentMonitor = None
              m.stopMonitor()
            case None => // do nothing
          }
        }
      }
    }

  def nextTest(): Unit = {
    if (!disableMonitorTCP) {
      currentMonitor match {
        case Some(m) => m.nextTest()
        case None    =>
      }
    }
  }

  def getProp(name: String): Option[String] = {
    sys.props.get(name) match {
      case Some(s) => Some(s)
      case None    => sys.env.get(name)
    }
  }
}

/**
  * Monitor the TCP connections in time_wait.
  * @param pw a PrintWrite that gets the output.  This is closed when the monitor stops
  * @param period
  */
class MonitorTCP(pw: PrintWriter, period: Duration) extends Thread {
  val starttime: Long = System.currentTimeMillis()

  def formatToSec(millis: Long): String = {
    f"${millis / 1000.0}%5.3f"
  }

  def getPrefix(): String = {
    val cur = System.currentTimeMillis()
    formatToSec(cur - starttime)
  }

  def nextTest(): Unit =
    synchronized {
      testNumber = testNumber + 1
    }

  @volatile var lastConnections = 0
  @volatile var lastTotalTime: Long = 0
  @volatile var testNumber = 0

  def getNextTest(): Int =
    synchronized {
      testNumber
    }

  def writeNumberConnectionsInTimeWait(): Unit = {
    val curCon = MonitorTCP.getNumberTimeWaitConnections()
    val totWait = MonitorTCP.lock.totalWait
    val tn = getNextTest()
    val pre = getPrefix()
    val s =
      pre + "," + curCon + "," + formatToSec(totWait) + "," + tn + "," + "," +
        pre + "," + (curCon - lastConnections) + "," + formatToSec(
        totWait - lastTotalTime
      ) + "," + tn
    lastConnections = curCon
    lastTotalTime = totWait
    pw.println(s)
  }

  def writeHeader(): Unit = {
    pw.println(
      "time ms,numberTimeWaitConnections,totalWaitInMillis,testNumber,,time ms,diffConnections,DifftotalWait,testNumber"
    )
  }

  override def run(): Unit =
    synchronized {
      try {
        writeHeader()
        writeNumberConnectionsInTimeWait()
        try {
          while (true) {
            wait(period.toMillis)
            writeNumberConnectionsInTimeWait()
          }
        } catch {
          case x: InterruptedException =>
        }
      } finally {
        pw.close()
      }
    }

  def stopMonitor(): Unit = {
    interrupt()
  }

}
