package com.github.thebridsk.bridge

import com.github.thebridsk.bridge.clientapi.routes.AppRouter
import com.github.thebridsk.utilities.time.js.SystemTimeJs
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.logger.Info
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import com.github.thebridsk.bridge.clientapi.pages.Pixels

/**
  * @author werewolf
  */
@JSExportTopLevel("BridgeApi")
object BridgeApi { // need to figure out how to use new way to call main

  sealed trait MyPages
  case object Home extends MyPages
  case object ScoreHand extends MyPages

  import com.github.thebridsk.bridge.clientcommon.logger.Init

  SystemTimeJs()

  def logger: Logger = Logger("bridge.BridgeApi")

  //  def main(args: Array[String]): Unit = main()

//  @JSExportTopLevel("com.github.thebridsk.bridge.clientapi.Bridge")
//  protected def getInstance(): this.type = this

  private var loggerInitDone = false
  private var defaultInitDone = false

  def initDone(): Unit = {
    if (loggerInitDone && defaultInitDone) startClient()
    else
      logger.info(
        s"loggerInitDone=${loggerInitDone}, defaultInitDone=${defaultInitDone}"
      )
  }

  def main(args: Array[String]): Unit = main()

  @JSExport
  def main(): Unit =
    Alerter.tryitWithUnit {

      if (BridgeDemo.isDemo) {
        AjaxResult.setEnabled(false)
      }

      Alerter.setupError()
      Tooltip.init()

      Init { () => loggerInitDone = true; initDone() }
      Pixels.init { () => defaultInitDone = true; initDone() }

    }

  def startClient(): Unit =
    Alerter.tryitWithUnit {
      logger.info("Bridge Scorer Starting")

      try {
        val div = Info.getElement()
        val sdiv = div.nodeName
        logger.info("Bridge Scorer Starting, rendering into " + sdiv)
        AppRouter().router().renderIntoDOM(div)
      } catch {
        case t: Throwable =>
          logger.info("Uncaught exception starting up: " + t.toString(), t)
          t.printStackTrace()
      }
    }
}
