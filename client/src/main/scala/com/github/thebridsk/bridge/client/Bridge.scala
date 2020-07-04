package com.github.thebridsk.bridge.client

import com.github.thebridsk.bridge.data.Table
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge._
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import com.github.thebridsk.bridge.client.pages.hand._
import com.github.thebridsk.bridge.client.routes.AppRouter
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.raw.Node
import com.github.thebridsk.bridge.data.SystemTime
import scala.scalajs.js.Date
import com.github.thebridsk.bridge.data.js.SystemTimeJs
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoModule
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule
import com.github.thebridsk.bridge.client.pages.rubber.RubberModule
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.modules.Loader
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.raw.Event
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.pages.ColorThemeStorage
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import com.github.thebridsk.bridge.client.pages.Pixels

/**
 * @author werewolf
 */
@JSExportTopLevel("Bridge")
object Bridge {   // need to figure out how to use new way to call main

  sealed trait MyPages
  case object Home extends MyPages
  case object ScoreHand extends MyPages

  import com.github.thebridsk.bridge.clientcommon.logger.Init

  SystemTimeJs()

  def logger = Logger("bridge.Bridge")

  Loader.init

//  def main(args: Array[String]): Unit = main()

//  @JSExportTopLevel("com.github.thebridsk.bridge.client.Bridge")
//  protected def getInstance(): this.type = this

  private var loggerInitDone = false
  private var defaultInitDone = false

  def initDone() = {
    if (loggerInitDone && defaultInitDone) startClient()
    else logger.info(s"loggerInitDone=${loggerInitDone}, defaultInitDone=${defaultInitDone}")
  }

  def main(args: Array[String]): Unit = main()

  @JSExport
  def main(): Unit = Alerter.tryitWithUnit {

    if (BridgeDemo.isDemo) {
      AjaxResult.setEnabled(false)
    }

    Alerter.setupError()
    Tooltip.init()

    Init { () => loggerInitDone = true; initDone() }
    Pixels.init { () => defaultInitDone = true; initDone() }

    ColorThemeStorage.initTheme()

  }

  def startClient() = Alerter.tryitWithUnit {
    logger.info("Bridge Scorer Starting" )

    try {
      val div = getElement()
      val sdiv = div.nodeName
      logger.info("Bridge Scorer Starting, rendering into "+sdiv )
      NamesStore.init()
      AppRouter(ChicagoModule, DuplicateModule, RubberModule).router().renderIntoDOM(div)
    } catch {
      case t: Throwable =>
        logger.info("Uncaught exception starting up: "+t.toString(),t)
        t.printStackTrace()
    }
  }

  /**
   * Get the element with the specified ID
   * @param id
   * @return the HTMLElement object
   * @throws IllegalStateException if the element was not found.
   */
  def getElement( id: String = "BridgeApp" ) = {

    val div = document.getElementById(id)
    if (div == null) {
      logger.warning("Did not find element with id "+id)
      throw new IllegalStateException("Did not find a element with id "+id)
    } else {
      div.asInstanceOf[HTMLElement]
    }

  }

  def findDiv( root: HTMLElement, id: String ) : Node = {
    val children = root.childNodes
    val len = children.length

    if (len > 0) {
      for (i <- 0 until len) {
        val child = children.item(i)
        if (child.hasAttributes()) {
          val unknown = child.attributes.getNamedItem("id")
          if (unknown.value == id) return child
        }
      }
    }
    root
  }

  def getBody() = {
    document.body
  }

}
