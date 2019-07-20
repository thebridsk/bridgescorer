package com.github.thebridsk.bridge.server.test.util

import scala.concurrent.duration._
import scala.util.Failure
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import org.scalactic.source.Position
import java.util.concurrent.TimeUnit
import java.io.StringWriter
import java.io.PrintWriter
import java.io.PrintStream
import com.github.thebridsk.bridge.source.SourcePosition
import scala.util.Success

object ParallelUtilsInternals {
  val log = Logger[ParallelUtils]

  private[util] def toExceptionMsg( msg: String, causes: (Position,Throwable)* ) = {
    msg+causes.map { e =>
      val ex = e._2
      s"""CodeBlock ${e._1.line}: ${causeToString(ex)}"""
    }.mkString("\nSuppressed:\n","\nSuppressed\n","\nDoneSuppressed")
  }

  private[util] def causeToString( e: Throwable ) = {
    val sw = new StringWriter
    val pw = new PrintWriter( sw )
    e.printStackTrace(pw)
    pw.flush()
    e.toString()+"\n"+sw.toString()
  }

}

class FutureException( msg: String, cause: Throwable ) extends Exception(msg,cause)

class ParallelException( msg: String, causes: (Position,Throwable)* ) extends Exception( ParallelUtilsInternals.toExceptionMsg(msg, causes:_*)) {
  causes.foreach(e=>addSuppressed( new FutureException(s"From ${e._1.line}", e._2)))

  ParallelUtilsInternals.log.warning("Oops",this)

}

trait ParallelUtils {
  import ParallelUtilsInternals._
  import ParallelUtils._


  val useSerial: Boolean

  def waitForFuturesIgnoreTimeouts( name: String, funs: CodeBlock[_]* )( implicit timeoutduration: Duration ): Unit = {
    try {
      waitForFutures( name, funs:_* )
    } catch {
      case t: Throwable =>
        ignoreTimeoutExceptions(name, t)
    }
  }

  def waitForFutures( name: String, cbs: CodeBlock[_]* )( implicit timeoutduration: Duration ) = {
    try {
      if (useSerial) executeSerial(name, cbs:_*)
      else waitForFuturesImpl(name, cbs:_*)
    } catch {
      case e: Throwable =>
        log.warning(s"waitForFutures: Exception on ${name}", e )
        throw e
    }
  }

  def waitForFuturesImpl( name: String, cbs: CodeBlock[_]* )( implicit timeoutduration: Duration ) = {
    val futurefailures = Future.foldLeft(
                         cbs.map { cb =>
                           cb.toFuture.transform { t =>
                             t match {
                               case Success(v) =>
                                 log.fine(s"CodeBlock ${cb.pos.line} ended in success")
                                 Success( cb.pos, None )
                               case Failure(ex) =>
                                 log.severe(s"CodeBlock ${cb.pos.line} ended in failure", ex)
                                 Success( cb.pos, Some(ex) )
                             }
                           }
                         }.toList
                       )( List[(Position,Throwable)]())( (ac,v) => v._2.map { ex => (v._1,ex)::ac }.getOrElse(ac))
    val failures = Await.result(futurefailures, timeoutduration)
    if (!failures.isEmpty) throw new ParallelException(name+failures.map(s=>s._2).mkString("\n  ","\n  ",""), failures:_*)
  }

  def executeSerial( name: String, cbs: CodeBlock[_]* )( implicit timeoutduration: Duration ): Unit = {
    val failures = cbs.flatMap { cb =>
      try {
        cb.execute
        Nil
      } catch {
        case x: Exception =>
          log.severe(s"""Error executing "${name}" code block ${cb.pos.line}""", x)
          (cb.pos,x)::Nil
      }
    }
    if (!failures.isEmpty) throw new ParallelException(name+failures.map(s=>s._2).mkString("\n  ","\n  ",""), failures:_*)
  }

}

object ParallelUtils extends ParallelUtils {
  import scala.language.implicitConversions
  import ParallelUtilsInternals._

  /**
   * Get the specified property as either a java property or an environment variable.
   * If both are set, the java property wins.
   * @param name the property name
   * @return the property value wrapped in a Some object.  None property not set.
   */
  def getPropOrEnv( name: String ): Option[String] = sys.props.get(name) match {
    case v: Some[String] =>
      log.fine("getPropOrEnv: found system property: "+name+"="+v.get)
      v
    case None =>
      sys.env.get(name) match {
        case v: Some[String] =>
          log.fine("getPropOrEnv: found env var: "+name+"="+v.get)
          v
        case None =>
          log.fine("getPropOrEnv: did not find system property or env var: "+name)
          None
      }
  }

  /**
   * flag to determine if serial or parallel processing is used.
   * Configure by setting the System Property ParallelUtils.useSerial or environment variable ParallelUtils.useSerial
   * to "true" or "false".
   * If this property is not set, then the os.name system property is used, on windows or unknown parallel, otherwise serial.
   */
  val useSerial = {
    getPropOrEnv("ParallelUtilsUseSerial") match {
      case Some(v) =>
        v.toBoolean
      case None =>
        sys.props.getOrElse("os.name", "oops").toLowerCase() match {
          case os: String if (os.contains("win")) => false
          case os: String if (os.contains("mac")) => true
          case os: String if (os.contains("nix")||os.contains("nux")) => true
          case os =>
            log.severe("Unknown operating system: "+os)
            false
        }
    }
  }

  log.fine( s"""useSerial=${useSerial}""")

  class CodeBlock[T]( body: => T )(implicit val pos: Position) {

    def execute = body

    def toFuture = {
      Future {
        try {
          log.fine(s"CodeBlock ${pos.line} starting")
          body
        } catch {
          case x: Throwable =>
            log.fine("Uncaught exception in CodeBlock", x )
            throw x
        } finally {
          log.fine(s"CodeBlock ${pos.line} finished")
        }
      }
    }
  }

  object CodeBlock {
    def apply[T]( body: => T )(implicit pos: Position) = {
      new CodeBlock(body)
    }
  }

  def ignoreTimeoutExceptions( name: String, t: Throwable )( implicit pos: Position ) = {
    t match {
      case x: ParallelException =>
        if (x.getSuppressed.find{ s =>
              s match {
                case f: FutureException =>
                  f.getCause match {
                    case _: TimeoutException => false
                  }
                case _ => true
              }
            }.isDefined
        ) {
          log.warning(s"Exception, ${name} called from ${pos.line}", t )
          throw t
        }
        log.warning(s"Ignoring timeout, ${name} called from ${pos.line}", t )
      case x: TimeoutException =>
        log.warning(s"Ignoring timeout, ${name} called from ${pos.line}", t )
      case _ =>
        log.warning(s"Exception, ${name} called from ${pos.line}", t )
        throw t
    }
  }
}

object Test {
  import ParallelUtils._
  import ParallelUtilsInternals.log

  implicit val timeoutduration = Duration( 60, TimeUnit.SECONDS )

  def main( args: Array[String] ): Unit = {
    waitForFutures( "hello",
        CodeBlock {
          log.warning("parallel 0")
        },
        CodeBlock { log.warning("parallel 1") }
        )

    object xx extends ParallelUtils {
      val useSerial = true
    }
    xx.waitForFutures( "hello",
        CodeBlock {
          log.warning("serial 0")
        },
        CodeBlock { log.warning("serial 1") }
        )

    waitForFutures( "hello",
        CodeBlock {
          if (true) throw new Exception( "parallel 0e" )
          0
        },
        CodeBlock { throw new Exception("parallel 1e" ) }
        )

    xx.waitForFutures( "hello",
        CodeBlock {
          if (true) throw new Exception("serial 0e" )
          0
        },
        CodeBlock { throw new Exception("serial 1e" ) }
        )


  }
}
