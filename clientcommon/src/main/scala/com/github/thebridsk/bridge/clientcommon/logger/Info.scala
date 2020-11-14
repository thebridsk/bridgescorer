package com.github.thebridsk.bridge.clientcommon.logger

import org.scalajs.dom.document
import scala.scalajs.js
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.raw.Node
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import japgolly.scalajs.react.extra.router.BaseUrl
import japgolly.scalajs.react.vdom.TagMod

/**
  * @author werewolf
  */
object Info {
  private val log = Logger("bridge.InfoPage")

  def info: List[(String, String)] = {
    val window = document.defaultView
    val nav = window.navigator
    val geoloc = nav.geolocation
    val screen = window.screen
    val styleMediaDefined = js.typeOf(window.styleMedia) != "undefined"
    val styleMedia = if (styleMediaDefined) window.styleMedia; else null
//    log.info( "InfoPage" )
    val i = List(
      ("Navigator.appName", nav.appName),
      ("Navigator.appVersion", nav.appVersion),
      ("Navigator.userAgent", nav.userAgent),
      ("Navigator.platform", nav.platform),
      ("Navigator.onLine", nav.onLine),
      ("Navigator.standalone", js.Dynamic.global.window.navigator.standalone),
      ("window.innerWidth", window.innerWidth),
      ("window.innerHeight", window.innerHeight),
      ("window.orientation", getOrientation),
      ("isPortrait", isPortrait),
      ("Screen.width", screen.width),
      ("Screen.height", screen.height),
      ("Screen.availHeight", screen.availHeight),
      ("Screen.availWidth", screen.availWidth),
      ("Screen.colorDepth", screen.colorDepth),
      ("Screen.pixelDepth", screen.pixelDepth),
      ("typeOf(StyleMedia)", js.typeOf(window.styleMedia)),
      (
        "StyleMedia.type",
        (if (styleMediaDefined) styleMedia.`type`; else "???")
      ),
      ("isTouchEnabled", isTouchEnabled),
      ("closed", window.asInstanceOf[js.Dynamic].closed)
//      ("", "")
    ).map {
      case (key, value) =>
        val v = value.toString()
//      log.info(s"""  ${key} = ${v}""")
        (key, v)
    }
    log.info(
      s"InfoPage\n  ${i.map { e => s"${e._1} = ${e._2}" }.mkString("\n  ")}"
    )
    i
  }

  /**
    * window.orientation: (from http://www.williammalone.com/articles/html5-javascript-ios-orientation/)
    *   0 - portrait
    * 180 - portrait, upsidedown
    *  90 - landscape, counterclockwise
    * -90 - landscape, clockwise
    */
  def getOrientation: Option[Int] = {
    js.Dynamic.global.window.orientation.toString match {
      case "undefined" => None
      case s           => Some(s.toInt)
    }
  }

  def isPortrait: Boolean = {
    val window = document.defaultView // js.Dynamic.global.window
    window.innerHeight / window.innerWidth > 1
  };

  def isLandscape: Boolean = !isPortrait

  def isWindowsAsusTablet: Boolean = {
    // HACK Alert
    // screen is 1368x768, platform is Win32
    val s = js.Dynamic.global.window.screen
    val h = s.height.asInstanceOf[Int]
    val w = s.width.asInstanceOf[Int]
    val p = js.Dynamic.global.window.navigator.platform.asInstanceOf[String]
    (p == "Win32") && ((w == 1368 && h == 768) || (w == 768 && h == 1368))
  }

  def isTouchEnabled: Boolean = {
    val g = js.Dynamic.global.window
    !js.isUndefined(g.ontouchstart) || isWindowsAsusTablet
  }

  val touchEnabled = isTouchEnabled

  def showOnlyInLandscapeOnTouch: TagMod = {
    if (touchEnabled) baseStyles.hideInPortrait
    else baseStyles.alwaysHide
  }

  val location = document.location
  val hostUrl: String = location.protocol + "//" + location.host
  val baseUrl = new BaseUrl(hostUrl + location.pathname)

  /**
    * Get the element with the specified ID
    * @param id
    * @return the HTMLElement object
    * @throws IllegalStateException if the element was not found.
    */
  def getElement(id: String = "BridgeApp"): HTMLElement = {

    val div = document.getElementById(id)
    if (div == null) {
      log.warning("Did not find element with id " + id)
      throw new IllegalStateException("Did not find a element with id " + id)
    } else {
      div.asInstanceOf[HTMLElement]
    }

  }

  def findDiv(root: HTMLElement, id: String): Node = {
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

}
