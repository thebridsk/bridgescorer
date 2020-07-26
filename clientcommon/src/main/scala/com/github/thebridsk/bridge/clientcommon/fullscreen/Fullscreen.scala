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
  val isIpad: Boolean = {
    val p = js.Dynamic.global.window.navigator.platform.asInstanceOf[String]
    p == "MacIntel"
  }
}

object Implicits {
  import Values._

  val log: Logger = Logger("bridge.Fullscreen")

  val isFullscreenEnabled = fullscreenEnabled

  def fullscreenEnabled: Boolean = {

    val doc = document.asInstanceOf[DocumentFullscreen]
    if (isIpad) {
      doc.webkitFullscreenEnabled.getOrElse {
        val body = document.body
        val f = js.typeOf(body.asInstanceOf[js.Dynamic].requestFullscreen) == "function"
        log.fine(s"On iPad, found requestFullscreen function on body object: $f")
        f
      }
    } else {
      doc.fullscreenEnabled.getOrElse(false)
    }
  }


  implicit class DocumentFullscreenWrapper( private val document: Document ) extends AnyVal {

    def doc: DocumentFullscreen = document.asInstanceOf[DocumentFullscreen]

    def fullscreenEnabled: Boolean = Implicits.isFullscreenEnabled

    def fullscreenElement: Element = {
      if (isFullscreenEnabled) if (isIpad) doc.webkitFullscreenElement else doc.fullscreenElement
      else null
    }
    def exitFullscreen(): js.Promise[Unit] = {
      if (isFullscreenEnabled) if (isIpad) doc.webkitExitFullscreen() else doc.exitFullscreen()
      else js.Promise.reject("fullscreen not enabled")
    }

    def isFullscreen: Boolean = {
      val fe = fullscreenElement
      !js.isUndefined(fe) && fe != null
    }
  }

  implicit class ElementFullscreenWrapper( private val element: Element ) extends AnyVal {
    import Values._

    def elem: DocumentElementFullscreen = element.asInstanceOf[DocumentElementFullscreen]

    def requestFullscreen(): js.Promise[Unit] = {
      if (isFullscreenEnabled) if (isIpad) elem.webkitRequestFullscreen() else elem.requestFullscreen()
      else js.Promise.reject("fullscreen not enabled")
    }

  }

}

object Fullscreen extends Listenable {
  import Implicits._
  var fullscreenElement: js.UndefOr[Element] = js.undefined

  def init(): Unit = {
    setFullscreenElement(
      if (document.fullscreenEnabled) document.fullscreenElement
      else js.undefined
    )
  }

  init()

  def setFullscreenElement( e: js.UndefOr[Element] ): Unit = {
    fullscreenElement = e
  }

  val onFullScreenChange: js.Function1[js.Any,Unit] = { (x) =>
    notify("fullscreenchange")
  }

  document.addEventListener("fullscreenchange", onFullScreenChange, false);
  document.addEventListener("webkitfullscreenchange", onFullScreenChange, false);
  document.addEventListener("mozfullscreenchange", onFullScreenChange, false);
}
