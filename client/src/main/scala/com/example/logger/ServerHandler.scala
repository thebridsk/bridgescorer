package com.example.logger

import utils.logging.TraceMsg
import com.example.data.websocket.DuplexProtocol.LogEntryV2
import org.scalactic.source.Position
import com.example.data.SystemTime
import com.example.routes.AppRouter
import java.io.StringWriter
import java.io.PrintWriter

trait ServerHandler {

  def traceMsgToLogEntryV2( tm: TraceMsg ): LogEntryV2 = {
    // LogEntryV2( position: String,
    //             timestamp: Long,
    //              level: String,
    //              url: String,
    //              message: String,
    //              cause: String,
    //              args: String*)
    // TraceMsg( val msgtype: TraceType,
    //               val level: utils.logging.Level,
    //               val message: String=null,
    //               val cause: Throwable = null
    //             )( val args: Any* )(implicit val pos: Position)

    LogEntryV2( posToString(tm.pos), tm.logger, tm.time, tm.level.short, url(), tm.message, exceptionToString(tm.cause), tm.args.map{o=>o.toString}.toList )
  }

  def posToString( pos: Position ) = pos.fileName+":"+pos.lineNumber

  def timestamp() = {
    SystemTime.currentTimeMillis().toLong
  }

  def url() = {
    try {
      AppRouter.baseUrl.toString()
    } catch {
      case _: Throwable => ""
    }
  }

  def exceptionToString( x: Throwable ) = {
    if (x==null) ""
    else {
      val b = new StringWriter
      val pw = new PrintWriter(b)
      x.printStackTrace(pw)
      pw.flush
      b.toString()
    }
  }

}
