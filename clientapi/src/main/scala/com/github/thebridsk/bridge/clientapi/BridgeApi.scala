package com.github.thebridsk.bridge

import com.github.thebridsk.bridge.data.Table
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge._
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import com.github.thebridsk.bridge.clientapi.routes.AppRouter
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.raw.Node
import com.github.thebridsk.bridge.data.SystemTime
import scala.scalajs.js.Date
import com.github.thebridsk.bridge.data.js.SystemTimeJs
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.raw.Event
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.logger.Info

/**
 * @author werewolf
 */
@JSExportTopLevel("BridgeApi")
object BridgeApi {   // need to figure out how to use new way to call main

  sealed trait MyPages
  case object Home extends MyPages
  case object ScoreHand extends MyPages

  import com.github.thebridsk.bridge.clientcommon.logger.Init

  SystemTimeJs()

  def logger = Logger("bridge.BridgeApi")

  //  def main(args: Array[String]): Unit = main()

//  @JSExportTopLevel("com.github.thebridsk.bridge.clientapi.Bridge")
//  protected def getInstance(): this.type = this

  @JSExport
  def main(): Unit = Alerter.tryitWithUnit {

    if (BridgeDemo.isDemo) {
      AjaxResult.setEnabled(false)
    }

    Alerter.setupError()

    Init( startClient _)

  }

  def startClient() = Alerter.tryitWithUnit {
    logger.info("Bridge Scorer Starting" )

    try {
      val div = Info.getElement()
      val sdiv = div.nodeName
      logger.info("Bridge Scorer Starting, rendering into "+sdiv )
      AppRouter().router().renderIntoDOM(div)
    } catch {
      case t: Throwable =>
        logger.info("Uncaught exception starting up: "+t.toString(),t)
        t.printStackTrace()
    }
  }
}
