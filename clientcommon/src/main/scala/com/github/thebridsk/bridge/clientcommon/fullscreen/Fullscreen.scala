package com.github.thebridsk.bridge.clientcommon.fullscreen

import scala.scalajs.js
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Document


@js.native
trait DocumentFullscreen extends js.Object {
  val fullscreenEnabled: Boolean = js.native
  val fullscreenElement: Element = js.native
  def exitFullscreen(): js.Promise[js.UndefOr[js.Any]] = js.native

  val webkitFullscreenEnabled: Boolean = js.native
  val webkitFullscreenElement: Element = js.native
  def webkitExitFullscreen(): js.Promise[js.UndefOr[js.Any]] = js.native
}

@js.native
trait DocumentElementFullscreen extends js.Object {
  def requestFullscreen(): js.Promise[js.UndefOr[js.Any]] = js.native
  def webkitRequestFullscreen(): js.Promise[js.UndefOr[js.Any]] = js.native
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

    def fullscreenEnabled: Boolean = if (isIpad) doc.webkitFullscreenEnabled else doc.fullscreenEnabled
    def fullscreenElement: Element = if (isIpad) doc.webkitFullscreenElement else doc.fullscreenElement
    def exitFullscreen(): js.Promise[js.UndefOr[js.Any]] = {
      if (isIpad) doc.webkitExitFullscreen else doc.exitFullscreen
    }

    def isFullscreen = fullscreenElement != null
  }

  implicit class ElementFullscreenWrapper( val element: Element ) extends AnyVal {
    import Values._

    def elem = element.asInstanceOf[DocumentElementFullscreen]

    def requestFullscreen(): js.Promise[js.UndefOr[js.Any]] = if (isIpad) elem.webkitRequestFullscreen() else elem.requestFullscreen()

  }

}
