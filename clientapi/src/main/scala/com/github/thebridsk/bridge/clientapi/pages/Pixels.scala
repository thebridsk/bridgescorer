package com.github.thebridsk.bridge.clientapi.pages

import org.scalajs.dom
import dom.document
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Info

object Pixels {

  val log = Logger("bridge.Pixels")

  lazy val defaultFont = Pixels.getFont("DefaultHandButton")

  def getComputedProperties( id: String ) = {
    val elem = Info.getElement(id)
    val window = document.defaultView
    window.getComputedStyle(elem)
  }

  def getElementAndComputedProperties( id: String ) = {
    val elem = Info.getElement(id)
    val window = document.defaultView
    (elem,window.getComputedStyle(elem))
  }

  /**
   * Returns the computed font for the element with the specified Id.
   * @param id
   * @return the font-size and font-family
   */
  def getFont( id: String ) = {
    val computed = getComputedProperties(id)
    val r = computed.fontSize+" "+computed.fontFamily
    log.fine( s"""On element with id ${id} found font of ${r}""" )
    r
  }

  val patternRadius = """(\d+)px""".r
  def getPixels( name: String, value: String, id: String, default: Int = 0 ) = {
    value match {
      case patternRadius( r ) =>
        log.fine( s"""On element with id ${id} found ${name} of ${r}px""" )
        r.toInt
      case x =>
        log.fine( s"""On element with id ${id} found ${name} of ${x}, using 0""" )
        default
    }
  }

  def getBorderRadius( id: String ) = {
    val computed = getComputedProperties(id)
    val r = getPixels( "borderRadius", computed.borderRadius, id, -1 )
    if (r == -1) getPixels( "borderRadius", computed.borderTopLeftRadius, id )   // firefox doesn't return borderRadius property
    else r
  }

  def getPaddingBorder( id: String ) = {
    val computed = getComputedProperties(id)
    val pl = getPixels( "paddingLeft", computed.paddingLeft, id )
    val pr = getPixels( "paddingRight", computed.paddingRight, id )
    val ml = getPixels( "borderLeft", computed.borderLeft, id )
    val mr = getPixels( "borderRight", computed.borderRight, id )
    pl+pr+ml+mr
  }

  def getWidthWithBoarder( id: String ) = {
    val (elem,computed) = getElementAndComputedProperties(id)
    val ml = getPixels( "borderLeft", computed.borderLeft, id )
    val mr = getPixels( "borderRight", computed.borderRight, id )
    elem.clientWidth+ml+mr
  }

  /**
   * Max length, in pixels, of the names.  Uses the default font.
   */
  def maxLength( names: String* ): Int = {
    val font = defaultFont
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
