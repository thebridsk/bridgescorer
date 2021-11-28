package com.github.thebridsk.bridge.client.pages

import org.scalajs.dom
import dom.document
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.Bridge
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.Event
import org.scalajs.dom.{CSSStyleDeclaration, HTMLElement}
import scala.util.matching.Regex

/**
  * utility functions for various css properties.
  *
  * All sizes MUST be in px.
  */
object Pixels {

  val log: Logger = Logger("bridge.Pixels")

  lazy val defaultFont: String = Pixels.getFont("DefaultHandButton")

  /**
    * @param id
    * @return the element with the specified **id**
    */
  def getComputedProperties(id: String): CSSStyleDeclaration = {
    getElementAndComputedProperties(id)._2
  }

  /**
    * @param id
    * @return the element with the specified **id** and its computed styles.
    */
  def getElementAndComputedProperties(
      id: String
  ): (HTMLElement, CSSStyleDeclaration) = {
    val elem = Bridge.getElement(id)
    val window = document.defaultView
    (elem, window.getComputedStyle(elem))
  }

  /**
    * Returns the computed font for the element with the specified Id.
    * @param id
    * @return a string, contents are the font-size and font-family, space separated
    */
  def getFont(id: String): String = {
    val computed = getComputedProperties(id)
    val r = computed.fontSize + " " + computed.fontFamily
    log.fine(s"""On element with id ${id} found font of ${r}""")
    r
  }

  val patternRadius: Regex = """(\d+(?:\.\d+)?)px""".r
  /**
    * Convert the specified **value** to number of px.
    *
    * @param name an identifier of the **value**, used only for logging.
    * @param value the value to convert
    * @param id the id of the element, used only for logging.
    * @param default the default value to return if the **value** string is not valid, or an error occurred.
    * @return the number of px
    */
  def getPixels(
      name: String,
      value: String,
      id: String,
      default: Int = 0
  ): Int = {
    value match {
      case patternRadius(r) =>
        log.fine(s"""On element with id ${id} found ${name} of ${r}px""")
        val f = r.toDouble
        f.ceil.toInt
      case x =>
        log.fine(
          s"""On element with id ${id} found ${name} of ${x}, using default of ${default}"""
        )
        default
    }
  }

  /**
    * Returns the border radius property.
    *
    * Note: firefox does not return the borderRadius property.
    * The borderTopLeftRadius property is used instead.
    *
    * @param id the id of the element
    * @return the number of px of the borderRadius
    */
  def getBorderRadius(id: String): Int = {
    val computed = getComputedProperties(id)
    val r = getPixels("borderRadius", computed.borderRadius, id, -1)
    if (r == -1)
      getPixels(
        "borderRadius",
        computed.borderTopLeftRadius,
        id
      ) // firefox doesn't return borderRadius property
    else r
  }

  /**
    * Calculate the width of the border and padding on the specified element.
    *
    * @param id the id of the element
    * @return the sum of the padding left and right and border left and right.
    */
  def getPaddingBorder(id: String): Int = {
    val computed = getComputedProperties(id)
    val pl = getPixels("paddingLeft", computed.paddingLeft, id)
    val pr = getPixels("paddingRight", computed.paddingRight, id)
    val ml = getPixels("borderLeft", computed.borderLeft, id)
    val mr = getPixels("borderRight", computed.borderRight, id)
    pl + pr + ml + mr
  }

  /**
    * Return the width of the client of the element, including the border.
    *
    * @param id the id of the element.
    * @return
    */
  def getWidthWithBorder(id: String): Int = {
    val (elem, computed) = getElementAndComputedProperties(id)
    val ml = getPixels("borderLeft", computed.borderLeft, id)
    val mr = getPixels("borderRight", computed.borderRight, id)
    elem.clientWidth + ml + mr
  }

  /**
    * Max length, in pixels, of the names.  Uses the default font.
    */
  def maxLength(names: String*): Int = {
    val font = defaultFont
    names.map(n => length(n, font)).reduce((l, r) => Math.max(l, r))
  }

  /**
    * Max length, in pixels, of the **names** in the specified **font**
    */
  def maxLengthWithFont(font: String, names: String*): Int = {
    names.map(n => length(n, font)).reduce((l, r) => Math.max(l, r))
  }

  import org.scalajs.dom.html.Canvas
  private lazy val canvas =
    document.createElement("canvas").asInstanceOf[Canvas]
  private lazy val context =
    canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  /**
    * Length, in pixels, of the **name** in the specified **font**
    * @param name
    * @param font
    * @return
    *
    * @see https://stackoverflow.com/questions/118241/calculate-text-width-with-javascript/21015393#21015393
    */
  def length(name: String, font: String): Int = {
    context.font = font
    val metrics = context.measureText(name)
    val l = metrics.width
    val i = l.toInt
    val w =
      if (l == i) i
      else i + 1
    log.fine(s"""Width of "${name}" using ${font} is ${w}""")
    w
  }

  /**
    * Initialize
    *
    * Loads the `defaults.html`, source from `/fullserver/src/main/public/defaults.html`,
    * and adds the content as a child to the element with an *id* of `ForDefaultStyles`.
    *
    * @param callback called when initialization is complete.
    */
  def init(callback: () => Unit): Unit = {
    val xhr = new XMLHttpRequest()

    def initCallback(event: Event): Unit = {
      val someElem = document.querySelector("#ForDefaultStyles")
      val someOtherElem = xhr.responseXML.querySelector(":root > body > div")
      someElem.innerHTML = someOtherElem.innerHTML
      callback()
    }

    xhr.onload = initCallback _
    xhr.open("GET", "defaults.html")
    xhr.responseType = "document"
    xhr.send()

  }
}
