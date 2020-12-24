package com.github.thebridsk.bridge.clientcommon.fullscreen

import scala.scalajs.js
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Document
import org.scalajs.dom.document
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.dispatcher.Listenable

@js.native
trait DocumentFullscreen extends js.Object {
  val fullscreenEnabled: js.UndefOr[Boolean] = js.native
  val fullscreenElement: js.UndefOr[Element] = js.native
  def exitFullscreen(): js.Promise[Unit] = js.native

  val webkitFullScreenEnabled: js.UndefOr[Boolean] = js.native
  val webkitFullScreenElement: js.UndefOr[Element] = js.native
  def webkitExitFullscreen(): js.Promise[Unit] = js.native
}

@js.native
trait DocumentElementFullscreen extends js.Object {
  def requestFullscreen(): js.Promise[Unit] = js.native
  def webkitRequestFullScreen(): js.Promise[Unit] = js.native
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

  private lazy val (webkitFullscreenEnabled, fullscreenEnabled) = {
    val doc = document.asInstanceOf[DocumentFullscreen]
    (doc.webkitFullScreenEnabled.getOrElse(false),
     doc.fullscreenEnabled.getOrElse(false))
  }

  lazy val isFullscreenEnabled = fullscreenEnabled || webkitFullscreenEnabled

  implicit class DocumentFullscreenWrapper(private val document: Document)
      extends AnyVal {

    def doc: DocumentFullscreen = document.asInstanceOf[DocumentFullscreen]

    def myFullscreenEnabled: Boolean = Implicits.isFullscreenEnabled

    def myFullscreenElement: js.UndefOr[Element] = {
      if (webkitFullscreenEnabled) doc.webkitFullScreenElement
      else if (fullscreenEnabled) doc.fullscreenElement
      else js.undefined
    }
    def myExitFullscreen(): js.Promise[Unit] = {
      if (webkitFullscreenEnabled) {
        doc.webkitExitFullscreen()
      } else if (fullscreenEnabled) {
        doc.exitFullscreen()
      } else {
        js.Promise.reject("fullscreen not supported")
      }
    }

    def myIsFullscreen: Boolean = {
      val fe = myFullscreenElement
      !js.isUndefined(fe) && fe != null
    }
  }

  implicit class ElementFullscreenWrapper(private val element: Element)
      extends AnyVal {

    def elem: DocumentElementFullscreen =
      element.asInstanceOf[DocumentElementFullscreen]

    def myRequestFullscreen(): js.Promise[Unit] = {
      if (webkitFullscreenEnabled) {
        elem.webkitRequestFullScreen()
      } else if (fullscreenEnabled) {
        elem.requestFullscreen()
      } else {
        js.Promise.reject("fullscreen not supported")
      }
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
