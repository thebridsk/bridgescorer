package com.example.pages

import org.scalajs.dom
import dom.document
import utils.logging.Logger
import com.example.Bridge

object Pixels {

  val log = Logger("bridge.Pixels")

  lazy val defaultHandButtonFont = getFont("DefaultHandButton")

  lazy val defaultHandButtonBorderRadius = getBorderRadius("DefaultHandButton")

  /**
   * Returns the computed font for the element with the specified Id.
   * @param id
   * @return the font-size and font-family
   */
  def getFont( id: String ) = {
    val elem = Bridge.getElement(id)
    val window = document.defaultView
    val computed = window.getComputedStyle(elem)
    computed.fontSize+" "+computed.fontFamily
  }

  val patternRadius = """(\d+)px""".r
  def getBorderRadius( id: String ) = {
    val elem = Bridge.getElement(id)
    val window = document.defaultView
    val computed = window.getComputedStyle(elem)
    computed.borderRadius match {
      case patternRadius( r ) =>
        log.fine( s"""On element with id ${id} found radius of ${r}px""" )
        r.toInt
      case x =>
        log.fine( s"""On element with id ${id} found border-radius of ${x}, using 0""" )
        0
    }
  }

  /**
   * Max length, in pixels, of the names.  Uses the default hand button font.
   */
  def maxLength( names: String* ): Int = {
    val font = defaultHandButtonFont
    names.map(n => length(n, font)).reduce((l,r) => Math.max(l,r))
  }

  /**
   * Max length, in pixels, of the names
   */
  def maxLengthWithFont( font: String, names: String* ): Int = {
    names.map(n => length(n, font)).reduce((l,r) => Math.max(l,r))
  }

  import org.scalajs.dom.html.Canvas
  private lazy val canvas = document.createElement("canvas").asInstanceOf[Canvas]
  private lazy val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  /**
   * Length, in pixels, of the name
   * @see https://stackoverflow.com/questions/118241/calculate-text-width-with-javascript/21015393#21015393
   */
  def length( name: String, font: String ): Int = {
    context.font = font
    val metrics = context.measureText(name)
    val l = metrics.width
    val i = l.toInt
    val w = if (l == i) i
    else i+1
    log.fine(s"""Width of "${name}" using ${font} is ${w}""")
    w
  }

}
