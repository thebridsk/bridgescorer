package com.example.pages.chicagos

import org.scalajs.dom
import dom.document
import org.scalajs.dom.raw.HTMLElement
import utils.logging.Logger
import com.example.Bridge

object Pixels {

  val log = Logger("bridge.Pixels")

  /**
   * Max length, in pixels, of the names
   */
  def maxLength( names: String* ): Int = {
    names.map(n => length(n)).reduce((l,r) => Math.max(l,r))
  }

  /**
   * Length, in pixels, of the name
   * Doesn't work
   */
  def length( name: String ): Int = {
    var e = document.createElement("span").asInstanceOf[HTMLElement];
    e.style.fontFamily = "Arial"
    e.style.fontSize = "x-large"

    e.innerHTML = name

    try {
      val hidden = Bridge.getElement("Hidden")
      hidden.appendChild(e)
      val l = e.clientWidth
      hidden.removeChild(e)
      log.info("The width of \""+name+"\" is "+l)
      l
    } catch {
      case _:IllegalStateException =>
        log.info("The width of \""+name+"\" can't be determined ")
        0
    }
  }

}
