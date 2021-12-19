package com.github.thebridsk.bridge.clientcommon.logger

import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2
import org.scalactic.source.Position
import java.io.StringWriter
import java.io.PrintWriter
import com.github.thebridsk.bridge.data.SystemTime

trait ServerHandler {

  def traceMsgToLogEntryV2(tm: TraceMsg): LogEntryV2 = {
    // LogEntryV2( position: String,
    //             timestamp: Long,
    //              level: String,
    //              url: String,
    //              message: String,
    //              cause: String,
    //              args: String*)
    // TraceMsg( val msgtype: TraceType,
    //               val level: com.github.thebridsk.utilities.logging.Level,
    //               val message: String=null,
    //               val cause: Throwable = null
    //             )( val args: Any* )(implicit val pos: Position)

    val id = Init.clientid.getOrElse("?")

    LogEntryV2(
      posToString(tm.pos),
      tm.logger,
      tm.time,
      tm.level.short,
      url(),
      s"""(${id}): ${Option(tm.message).getOrElse("")}""",
      exceptionToString(tm.cause),
      tm.args.map { o => o.toString }.toList,
      Init.clientid
    )
  }

  def posToString(pos: Position): String = pos.fileName + ":" + pos.lineNumber

  def timestamp(): Long = {
    SystemTime.currentTimeMillis().toLong
  }

  def url(): String = {
    try {
      Info.baseUrl.toString()
    } catch {
      case _: Throwable => ""
    }
  }

  def exceptionToString(x: Throwable): String = {
    Option(x)
      .map { e =>
        val b = new StringWriter
        val pw = new PrintWriter(b)
        x.printStackTrace(pw)
        pw.flush
        b.toString()
      }
      .getOrElse("")
  }

}
