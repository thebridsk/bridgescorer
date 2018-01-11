package com.example.test.util

import scala.concurrent.duration._
import scala.util.Failure
import scala.concurrent._
import ExecutionContext.Implicits.global
import utils.logging.Logger
import org.scalactic.source.Position
import java.util.concurrent.TimeUnit
import java.io.StringWriter
import java.io.PrintWriter
import java.io.PrintStream
import com.example.source.SourcePosition

object ParallelUtilsInternals {
  val log = Logger[ParallelUtils]
}

class FutureException( msg: String, cause: Throwable ) extends Exception(msg,cause)

class ParallelException( msg: String, causes: (ParallelUtils.MyFuture[_],Throwable)* ) extends Exception(msg) {
  causes.foreach(e=>addSuppressed( new FutureException("From "+e._1.pos.fileName+":"+e._1.pos.lineNumber,e._2)))

  ParallelUtilsInternals.log.warning("Oops",this)

  override
  def toString(): String = {
    super.toString()+getSuppressed().map( e => e.toString() ).mkString(" Suppressed: ",", ","")
  }

//  override
//  def printStackTrace( ps: PrintStream ): Unit = {
//    super.printStackTrace(ps)
//    getSuppressed.foreach( e => {
//      ps.println()
//      ps.println("Suppressed Exception:")
//      e.printStackTrace(ps)
//    } )
//  }
//
//  override
//  def printStackTrace( pw: PrintWriter ): Unit = {
//    super.printStackTrace(pw)
//    getSuppressed.foreach( e => {
//      pw.println()
//      pw.println("Suppressed Exception:")
//      e.printStackTrace(pw)
//    } )
//  }
}

trait ParallelUtils {
  import ParallelUtilsInternals._
  import ParallelUtils._

  /**
   *  Run several functions in parallel
   *
   *  @param funs the functions
   *  @param timeoutduration the timeout for waiting for all functions to finish.
   *         this is an implicit parameter.
   *  @throws Exception if any of the functions throws an exception, it will be rethrown by this function
   */
  def runInParallel( funs: ()=>Unit* )( implicit timeoutduration: Duration ): Unit = {
    waitForFutures( "<unknown>", funs.map{ff=>functionToMyFuture(ff)}: _* )
  }

  /**
   *  Run several functions in parallel
   *
   *  @param name an identifying comment for logs
   *  @param funs the functions
   *  @param timeoutduration the timeout for waiting for all functions to finish.
   *         this is an implicit parameter.
   *  @throws Exception if any of the functions throws an exception, it will be rethrown by this function
   */
  def runInParallel( name: String, funs: ()=>Unit* )( implicit timeoutduration: Duration ): Unit = {
    waitForFutures( name, funs.map{ff=>functionToMyFuture(ff)}: _* )
  }

//  /**
//   *  Wait for several futures to complete
//   *
//   *  @param funs the futures
//   *  @param timeoutduration the timeout for waiting for all functions to finish.
//   *         this is an implicit parameter.
//   *  @throws Exception if any of the functions throws an exception, it will be rethrown by this function
//   */
//  def waitForFutures( funs: Future[_]* )( implicit timeoutduration: Duration ): Unit = {
//    waitForFutures( "<unknown>", funs:_* )
//  }


//  /**
//   *  Wait for several futures to complete
//   *
//   *  @param name an identifying comment for logs
//   *  @param funs the futures
//   *  @param timeoutduration the timeout for waiting for all functions to finish.
//   *         this is an implicit parameter.
//   *  @throws Exception if any of the functions throws an exception, it will be rethrown by this function
//   */
//  def waitForFutures( name: String, funs: Future[_]* )( implicit timeoutduration: Duration ): Unit = {
//    funs.map { f => {
//      try {
//        Await.ready(f, timeoutduration)
//        f.value match {
//          case None =>
//            val x = new TimeoutException
//            log.warning("Timed out waiting for future: "+name, x)
//            Some(Failure(x))
//          case x => x
//        }
//      } catch {
//        case x: TimeoutException =>
//          log.warning("Timed out waiting for future: "+name, x)
//          Some(Failure(x))
//      }
//    }}.find(r => r.map(t => t.isFailure).getOrElse(true)) match {
//      case Some( Some( Failure(x)) ) => throw x
//      case _ =>
//    }
//  }

  def waitForFutures( funs: Future[_]* )( implicit timeoutduration: Duration, pos: Position ): Unit = {
    waitForFutures( "<Unknown>", funs.map(new MyFuture(_)):_* )
  }

  def waitForFutures( funs: ParallelUtils.MyFuture[_]* )( implicit timeoutduration: Duration ): Unit = {
    waitForFutures( "<Unknown>", funs:_* )
  }

  def waitForFutures( name: String, funs: ParallelUtils.MyFuture[_]* )( implicit timeoutduration: Duration ): Unit = {
    val x = funs.map { f => {
      try {
        Await.ready(f.f, timeoutduration)
        f.f.value match {
          case None =>
            val x = new TimeoutException
            log.warning("Timed out waiting for future: "+name+", from "+f.pos.fileName+":"+f.pos.lineNumber, x)
            (f,Some(Failure(x)))
          case Some(Failure(x)) =>
            log.warning("Exception in future: "+name+", from "+f.pos.fileName+":"+f.pos.lineNumber, x)
            (f,Some(Failure(x)))
          case x => (f,x)
        }
      } catch {
        case x: TimeoutException =>
          log.warning("Timed out waiting for future: "+name+", from "+f.pos.fileName+":"+f.pos.lineNumber, x)
          (f,Some(Failure(x)))
      }
    }}.flatMap{ r=>r match {
      case (fut,Some(Failure(x))) => Seq((fut,x))
      case _ => Seq[(ParallelUtils.MyFuture[_],Throwable)]()
    }}
    if (!x.isEmpty) throw new ParallelException(name+x.map(s=>s._2).mkString("\n  ","\n  ",""), x:_*)
  }

  def waitForFuturesIgnoreTimeouts( name: String, funs: ParallelUtils.MyFuture[_]* )( implicit timeoutduration: Duration ): Unit = {
    try {
      waitForFutures( "<Unknown>", funs:_* )
    } catch {
      case t: Throwable => ignoreTimeoutExceptions(name, t)
    }
  }
}

object ParallelUtils extends ParallelUtils {
  import scala.language.implicitConversions
  import ParallelUtilsInternals._

  implicit class MyFuture[T]( val f: Future[T] )(implicit val pos: Position) {
  }

  implicit def functionToMyFuture( fun: ()=>Unit )(implicit pos: Position) = new MyFuture( Future{fun()} )

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
          throw t
        }
        log.warning(s"Ignoring timeout, ${name} called from ${pos.line}", t )
      case x: TimeoutException =>
        log.warning(s"Ignoring timeout, ${name} called from ${pos.line}", t )
      case _ =>
        throw t
    }
  }
}

object Test {
  import ParallelUtils._
  implicit val timeoutduration = Duration( 60, TimeUnit.SECONDS )

  def main( args: Array[String] ): Unit = {
    waitForFutures( "hello",
        Future {
          if (true) throw new Exception
          0
          },
        Future { throw new Exception },
        ()=>{
          throw new Exception
        }
        )

  }
}
