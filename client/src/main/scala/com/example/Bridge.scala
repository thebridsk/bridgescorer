package com.example

import com.example.data.Table
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.data.bridge._
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import com.example.pages.hand._
import com.example.routes.AppRouter
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.raw.Node
import com.example.data.SystemTime
import scala.scalajs.js.Date
import com.example.data.js.SystemTimeJs
import com.example.bridge.store.NamesStore
import com.example.pages.chicagos.ChicagoModule
import com.example.pages.duplicate.DuplicateModule
import com.example.pages.rubber.RubberModule
import utils.logging.Logger
import com.example.modules.Loader
import com.example.logger.Alerter
import com.example.pages.BaseStyles
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.raw.Event
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import com.example.rest2.AjaxResult

/**
 * @author werewolf
 */
@JSExportTopLevel("Bridge")
object Bridge {   // need to figure out how to use new way to call main

  sealed trait MyPages
  case object Home extends MyPages
  case object ScoreHand extends MyPages

  import com.example.logger.Init

  SystemTimeJs()

  def logger = Logger("bridge.Bridge")

  Loader.init

  def isDemo: Boolean = {
    val g = js.Dynamic.global.asInstanceOf[js.Dictionary[Boolean]]
    if ( g.contains("demo") ) {
      g("demo")
    } else {
      false
    }
  }

//  def main(args: Array[String]): Unit = main()

//  @JSExportTopLevel("com.example.Bridge")
//  protected def getInstance(): this.type = this

  @JSExport
  def main(): Unit = Alerter.tryitWithUnit {

    if (isDemo) {
      AjaxResult.setEnabled(false)
    }

    Alerter.setupError()

    Init( startClient _)

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
        if (child.hasAttributes) {
          val unknown = child.attributes.getNamedItem("id")
          if (unknown.value == id) return child
        }
      }
    }
    root
  }
}
