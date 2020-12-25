package com.github.thebridsk.bridge.clientcommon.fullscreen

import scala.scalajs.js
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Document
import org.scalajs.dom.document
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.dispatcher.Listenable
import scala.scalajs.js.annotation.JSName

/**
  * Extended Document object with fullscreen functions.
  *
  * There are 3 sets of functions:
  * - standard names
  * - Chrome 15-70 names
  * - iPad names
  */
@js.native
trait DocumentFullscreen extends js.Object {
  // standard names
  val fullscreenEnabled: js.UndefOr[Boolean] = js.native
  val fullscreenElement: js.UndefOr[Element] = js.native
  def exitFullscreen(): js.Promise[Unit] = js.native
  @JSName("exitFullscreen")
  def exitFullscreenVal: js.Any = js.native

  // for Chrome 15-70
  val webkitFullScreenEnabled: js.UndefOr[Boolean] = js.native
  val webkitFullScreenElement: js.UndefOr[Element] = js.native
  def webkitExitFullScreen(): js.Promise[Unit] = js.native
  @JSName("webkitExitFullScreen")
  def webkitExitFullScreenVal: js.Any = js.native

  // for iPad
  val webkitFullscreenEnabled: js.UndefOr[Boolean] = js.native
  val webkitFullscreenElement: js.UndefOr[Element] = js.native
  def webkitExitFullscreen(): js.Promise[Unit] = js.native
  @JSName("webkitExitFullscreen")
  def webkitExitFullscreenVal: js.Any = js.native
}

/**
  * Extended Element object with fullscreen functions.
  *
  * There are 3 sets of functions:
  * - standard names
  * - Chrome 15-70 names
  * - iPad names
  */

@js.native
trait DocumentElementFullscreen extends js.Object {
  // standard names
  def requestFullscreen(): js.Promise[Unit] = js.native
  @JSName("requestFullscreen")
  def requestFullscreenVal: js.Any = js.native
  // for Chrome 15-70
  def webkitRequestFullScreen(): js.Promise[Unit] = js.native
  @JSName("webkitRequestFullScreen")
  def webkitRequestFullScreenVal: js.Any = js.native
  // for iPad
  def webkitRequestFullscreen(): js.Promise[Unit] = js.native
  @JSName("webkitRequestFullscreen")
  def webkitRequestFullscreenVal: js.Any = js.native
}

object Values {
  val isIpad: Boolean = {
    val p = js.Dynamic.global.window.navigator.platform.asInstanceOf[String]
    val tp = if (js.isUndefined(js.Dynamic.global.window.navigator.maxTouchPoints)) {
      0
    } else {
      js.Dynamic.global.window.navigator.maxTouchPoints.asInstanceOf[Int]
    }

    p == "MacIntel" && tp > 1
  }
}

object Implicits {

  val log: Logger = Logger("bridge.Fullscreen")

  private lazy val (webkitFullscreenEnabled, fullscreenEnabled, ipadFullscreenEnabled) = {
    xxxyyyzzz()
    val doc = document.asInstanceOf[DocumentFullscreen]
    (doc.webkitFullScreenEnabled.getOrElse(false),
     doc.fullscreenEnabled.getOrElse(false),
     doc.webkitFullscreenEnabled.getOrElse(false))
  }

  private def xxxyyyzzz() = {

    def checkValueF(n: String, r: => ()=>js.Promise[Unit]) = {
      try {
        val v = r
        if (js.isUndefined(v)) {
          log.info(s"xxxyyyzzz.checkValueF: $n is undefined")
        }
        else {
          val t = js.typeOf(v)
          t match {
            case "object" =>
              log.info(s"xxxyyyzzz.checkValueF: $n type is $t: $v")
            case "boolean" =>
              log.info(s"xxxyyyzzz.checkValueF: $n type is $t: $v")
            case _ =>
              log.info(s"xxxyyyzzz.checkValueF: $n type is $t")
          }
        }

      } catch {
        case x: Exception =>
          log.info(s"xxxyyyzzz.checkValueF: $n exception $x")
      }
    }

    def checkValue(n: String, r: => js.Any) = {
      try {
        val v = r
        if (js.isUndefined(v)) {
          log.info(s"xxxyyyzzz.checkValue: $n is undefined")
        }
        else {
          val t = js.typeOf(v)
          t match {
            case "object" =>
              log.info(s"xxxyyyzzz.checkValue: $n type is $t: $v")
            case "boolean" =>
              log.info(s"xxxyyyzzz.checkValue: $n type is $t: $v")
            case _ =>
              log.info(s"xxxyyyzzz.checkValue: $n type is $t")
          }
        }
      } catch {
        case x: Exception =>
          log.info(s"xxxyyyzzz.checkValue: $n exception $x")
      }
    }

    val doc = document.asInstanceOf[DocumentFullscreen]
    val body = document.body.asInstanceOf[DocumentElementFullscreen]

    checkValue("exitFullscreen", doc.exitFullscreen _)
    checkValueF("exitFullscreen", doc.exitFullscreen _)
    checkValue("exitFullscreenVal", doc.exitFullscreenVal)
    checkValue("fullscreenElement", doc.fullscreenElement)
    checkValue("fullscreenEnabled", doc.fullscreenEnabled)
    checkValue("requestFullscreen", body.requestFullscreen _)
    checkValueF("requestFullscreen", body.requestFullscreen _)
    checkValue("requestFullscreenVal", body.requestFullscreenVal)

    checkValue("webkitExitFullscreen", doc.webkitExitFullscreen _)
    checkValueF("webkitExitFullscreen", doc.webkitExitFullscreen _)
    checkValue("webkitExitFullscreenVal", doc.webkitExitFullscreenVal)
    checkValue("webkitFullscreenElement", doc.webkitFullscreenElement)
    checkValue("webkitFullscreenEnabled", doc.webkitFullscreenEnabled)
    checkValue("webkitRequestFullscreen", body.webkitRequestFullscreen _)
    checkValueF("webkitRequestFullscreen", body.webkitRequestFullscreen _)
    checkValue("webkitRequestFullscreenVal", body.webkitRequestFullscreenVal)

    checkValue("webkitExitFullScreen", doc.webkitExitFullScreen _)
    checkValueF("webkitExitFullScreen", doc.webkitExitFullScreen)
    checkValue("webkitExitFullScreenVal", doc.webkitExitFullScreenVal)
    checkValue("webkitFullScreenElement", doc.webkitFullScreenElement)
    checkValue("webkitFullScreenEnabled", doc.webkitFullScreenEnabled)
    checkValue("webkitRequestFullScreen", body.webkitRequestFullScreen _)
    checkValueF("webkitRequestFullScreen", body.webkitRequestFullScreen _)
    checkValue("webkitRequestFullScreenVal", body.webkitRequestFullScreenVal)
  }

  lazy val isFullscreenEnabled = fullscreenEnabled || webkitFullscreenEnabled || ipadFullscreenEnabled

  /**
    * Wraps a Document class, and adds "my" versions of the fullscreen
    *
    * @param document
    */
  implicit class DocumentFullscreenWrapper(private val document: Document)
      extends AnyVal {

    def doc: DocumentFullscreen = document.asInstanceOf[DocumentFullscreen]

    def myFullscreenEnabled: Boolean = Implicits.isFullscreenEnabled

    def myFullscreenElement: js.UndefOr[Element] = {
      if (!js.isUndefined(doc.fullscreenEnabled)) doc.fullscreenElement
      else if (!js.isUndefined(doc.webkitFullscreenElement)) doc.webkitFullscreenElement
      else if (!js.isUndefined(doc.webkitFullScreenElement)) doc.webkitFullScreenElement
      else js.undefined
    }
    def myExitFullscreen(): js.Promise[Unit] = {
      if (!js.isUndefined(doc.exitFullscreenVal)) doc.exitFullscreen()
      else if (!js.isUndefined(doc.webkitExitFullscreenVal)) doc.webkitExitFullscreen()
      else if (!js.isUndefined(doc.webkitExitFullScreenVal)) doc.webkitExitFullScreen()
      else js.Promise.reject("fullscreen not supported")
    }

    def myIsFullscreen: Boolean = {
      val fe = myFullscreenElement
      !js.isUndefined(fe) && fe != null
    }
  }


  /**
    * Wraps a Element class, and adds "my" versions of the fullscreen
    *
    * @param element
    */
  implicit class ElementFullscreenWrapper(private val element: Element)
      extends AnyVal {

    def elem: DocumentElementFullscreen =
      element.asInstanceOf[DocumentElementFullscreen]

    def myRequestFullscreen(): js.Promise[Unit] = {
      xxxyyyzzz()
      if (!js.isUndefined(elem.requestFullscreenVal)) elem.requestFullscreen()
      else if (!js.isUndefined(elem.webkitRequestFullscreenVal)) elem.webkitRequestFullscreen()
      else if (!js.isUndefined(elem.webkitRequestFullScreenVal)) elem.webkitRequestFullScreen()
      else js.Promise.reject("fullscreen not supported")
    }

  }

}

object Fullscreen extends Listenable {
  import Implicits._
  var fullscreenElement: js.UndefOr[Element] = js.undefined

  def init(): Unit = {
    setFullscreenElement(
      if (document.myFullscreenEnabled) document.myFullscreenElement
      else js.undefined
    )
  }

  init()

  def setFullscreenElement(e: js.UndefOr[Element]): Unit = {
    fullscreenElement = e
  }

  val onFullScreenChange: js.Function1[js.Any, Unit] = { (x) =>
    notify("fullscreenchange")
  }

  document.addEventListener("fullscreenchange", onFullScreenChange, false);
  document.addEventListener(
    "webkitfullscreenchange",
    onFullScreenChange,
    false
  );
  document.addEventListener("mozfullscreenchange", onFullScreenChange, false);
}
