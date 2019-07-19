package com.github.thebridsk.bridge.clientcommon.rest2

import org.scalactic.source.Position
import com.github.thebridsk.bridge.data.RestMessage

class AjaxFailureException( val exmsg: String, val result: AjaxResult[_] )(implicit val pos: Position) extends Exception(exmsg)

class AjaxFailure( val msg: RestMessage, result: AjaxResult[_] )(implicit pos: Position) extends AjaxFailureException(msg.msg, result) {
  override
  def toString() = {
    getClass.getName+" "+msg.msg
  }
}

class RequestCancelled( msg: RestMessage, result: AjaxResult[_] )(implicit pos: Position) extends AjaxFailure(msg,result)

class AjaxDisabled( msg: RestMessage, result: AjaxResult[_] )(implicit pos: Position) extends AjaxFailure(msg,result)

class AjaxErrorReturn( val statusCode: Int, val body: String, result: AjaxResult[_] )(implicit pos: Position) extends AjaxFailureException(s"""Error return on ${result.url}: ${statusCode} ${body}""", result)
