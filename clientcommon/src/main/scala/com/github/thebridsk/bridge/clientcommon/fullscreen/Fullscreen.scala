package com.github.thebridsk.bridge.clientcommon.fullscreen

import scala.scalajs.js
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Document
import org.scalajs.dom.document



@js.native
trait DocumentFullscreen extends js.Object {
  val fullscreenEnabled: js.UndefOr[Boolean] = js.native
  val fullscreenElement: Element = js.native
  def exitFullscreen(): js.Promise[Unit] = js.native

  val webkitFullscreenEnabled: js.UndefOr[Boolean] = js.native
  val webkitFullscreenElement: Element = js.native
  def webkitExitFullscreen(): js.Promise[Unit] = js.native
}

@js.native
trait DocumentElementFullscreen extends js.Object {
  def requestFullscreen(): js.Promise[Unit] = js.native
  def webkitRequestFullscreen(): js.Promise[Unit] = js.native
}

object Values {
  val isIpad = {
    val p = js.Dynamic.global.window.navigator.platform.asInstanceOf[String]
    p == "iPad"
  }
}

object Implicits {

  implicit class DocumentFullscreenWrapper( val document: Document ) extends AnyVal {
    import Values._

    def doc = document.asInstanceOf[DocumentFullscreen]

    def fullscreenEnabled: Boolean = (if (isIpad) doc.webkitFullscreenEnabled else doc.fullscreenEnabled).getOrElse(false)
    def fullscreenElement: Element = {
      if (isFullscreenEnabled) if (isIpad) doc.webkitFullscreenElement else doc.fullscreenElement
      else null
    }
    def exitFullscreen(): js.Promise[Unit] = {
      if (isFullscreenEnabled) if (isIpad) doc.webkitExitFullscreen else doc.exitFullscreen
      else js.Promise.reject("fullscreen not enabled")
    }

    def isFullscreen = fullscreenElement != null
  }

  implicit class ElementFullscreenWrapper( val element: Element ) extends AnyVal {
    import Values._

    def elem = element.asInstanceOf[DocumentElementFullscreen]

    def requestFullscreen(): js.Promise[Unit] = {
      if (isFullscreenEnabled) if (isIpad) elem.webkitRequestFullscreen() else elem.requestFullscreen()
      else js.Promise.reject("fullscreen not enabled")
    }

  }

  val isFullscreenEnabled = document.fullscreenEnabled
}
