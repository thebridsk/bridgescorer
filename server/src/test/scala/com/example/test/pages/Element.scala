package com.example.test.pages

import org.openqa.selenium.WebElement
import org.scalactic.source.Position
import org.openqa.selenium.By
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import org.scalatest.concurrent.Eventually.PatienceConfig
import utils.logging.Logger
import org.openqa.selenium.OutputType
import com.example.backend.resource.FileIO
import com.example.source.SourcePosition
import org.openqa.selenium.Keys

class Element( val underlying: WebElement )(implicit pos: Position ) {

    def location = underlying.getLocation

    def size = underlying.getSize

    def isDisplayed: Boolean = underlying.isDisplayed

    def isEnabled: Boolean = underlying.isEnabled

    def isSelected: Boolean = underlying.isSelected

    def tagName: String = underlying.getTagName

    def attribute(name: String): Option[String] = Option(underlying.getAttribute(name))

    def id = attribute("id")

    def containsClass( cls: String ) = attribute("class").map{ clses =>
      clses.split(" ").contains(cls)
    }.getOrElse(false)

    def text: String = {
      val txt = underlying.getText
      if (txt != null) txt else ""
    }

    def click = {
      underlying.click()
      Thread.sleep( 100L )
    }

    override def equals(other: Any): Boolean = underlying == other

    override def hashCode: Int = underlying.hashCode

    override def toString: String = underlying.toString

    def find(
        by: QueryBy
      )(implicit patienceConfig: PatienceConfig,
                 pos: Position) = {
      by.queryElement(this)
    }

    def findAll(
        by: QueryBy
      )(implicit patienceConfig: PatienceConfig,
                 pos: Position) = {
      by.queryElements(this)
    }

    def findElem[T <: Element](
        by: QueryBy
      )(implicit patienceConfig: PatienceConfig,
                 pos: Position,
                 classtag: ClassTag[T]
      ): T = {
      val el = find(by)
      PageBrowser.getElement(el)
    }

    def findAllElem[T <: Element](
        by: QueryBy
      )(implicit patienceConfig: PatienceConfig,
                 pos: Position,
                 classtag: ClassTag[T]
      ): List[T] = {
      val el = findAll(by)
      PageBrowser.getAllElements(el)
    }

    def getElement[T <: Element](implicit pos: Position,
                                          classtag: ClassTag[T]
    ): T = {
    PageBrowser.getElement(this)
  }

  def takeScreenshot( directory: String, filename: String )( implicit pos: Position ) = {
    try {
      val scrFile = underlying.getScreenshotAs(OutputType.FILE);
      val destFile = PageBrowser.getPath(directory,filename)
      FileIO.copyFile( PageBrowser.getPath(scrFile), destFile )
    } catch {
      case x: Exception =>
        Element.log.warning(s"Unable to take screenshot, called from ${pos.line}", x)
        throw x
    }
    this
  }

  def sendKeys( keys: String ) = {
    underlying.sendKeys(keys)
  }
}

object Element {
  val log = Logger[Element]
}

class InputElement( underlying: WebElement )(implicit pos: Position ) extends Element(underlying) {

  def this( el: Element )(implicit pos1: Position ) = this(el.underlying)(pos1)

  def `type` = attribute("type")

  def name = attribute("name")

}

class TextField( underlying: WebElement )(implicit pos: Position ) extends InputElement(underlying) {

  def this( el: Element )(implicit pos1: Position ) = this(el.underlying)(pos1)

  def value: String = underlying.getAttribute("value")

  def value_=(value: String): Unit = {
//    underlying.clear()
    underlying.sendKeys(Keys.chord(Keys.CONTROL, "a"))
    underlying.sendKeys(value)
  }

  def clear(): Unit = { underlying.clear() }

}

class NumberField( underlying: WebElement )(implicit pos: Position ) extends TextField(underlying) {
  def this( el: Element )(implicit pos1: Position ) = this(el.underlying)(pos1)
}

class RadioButton( underlying: WebElement )(implicit pos: Position ) extends InputElement(underlying) {

  def this( el: Element )(implicit pos1: Position ) = this(el.underlying)(pos1)

}

class Checkbox( underlying: WebElement )(implicit pos: Position ) extends InputElement(underlying) {

  def this( el: Element )(implicit pos1: Position ) = this(el.underlying)(pos1)

  def value: String = underlying.getAttribute("value")

}

/**
 * @constructor
 * @param underlying - the input element of the combobox
 */
class Combobox( underlying: WebElement )(implicit pos: Position ) extends TextField(underlying) {

  def this( el: Element )(implicit pos1: Position ) = this(el.underlying)(pos1)

  def suggestions: List[Element] = {
    underlying.findElements(By.xpath( """./parent::div/following-sibling::div/div/div/ul/li""" ) ).asScala.map(e => new Element(e)).toList
  }

  def isSuggestionVisible = {
    underlying.findElement(By.xpath( """./parent::div/following-sibling::div/div""" ) ).isDisplayed()
  }

  def esc = {
    underlying.sendKeys(Keys.ESCAPE)
  }
}

/**
 * @constructor
 * @param underlying - the input element of the combobox
 */
class DateTimePicker( underlying: WebElement )(implicit pos: Position ) extends TextField(underlying) {

  def this( el: Element )(implicit pos1: Position ) = this(el.underlying)(pos1)

  def clickSelectDate(implicit pos1: Position ) = {
    val b = underlying.findElement(By.xpath( """./following-sibling::span/button[1]""" ) )
    b.click
  }

  def isSelectDatePopupVisible(implicit pos1: Position ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[2]""" ) ) )(pos1)
    !b.containsClass("rw-popup-transition-exited")
  }

  def clickPreviousMonth(implicit pos1: Position ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[2]/div/div/div/button[1]""" ) ) )(pos1)
    b.click
  }

  def clickNextMonth(implicit pos1: Position ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[2]/div/div/div/button[2]""" ) ) )(pos1)
    b.click
  }

  /**
   * @return get all the days that are visible in calendar popup.
   * if the popup is not visible, then the empty list is returned.
   */
  def getDays(implicit pos1: Position ): List[String] = {
    val b = underlying.findElements(By.xpath( """./parent::div/parent::div/div[3]/div/div/div[2]/table/tbody/tr/td""" ) ).asScala
    b.flatMap { cell =>
      new Element(cell)(pos1).attribute("aria-label")
    }.toList
  }

  def clickDay( d: String)(implicit pos1: Position ) = {
    val b = new Element( underlying.findElement(By.xpath( s"""./parent::div/parent::div/div[3]/div/div/div[2]/table/tbody/tr/td[@aria-label='${d}']""" ) ) )(pos1)
    b.click
  }

  def clickSelectTime(implicit pos1: Position ) = {
    val b = underlying.findElement(By.xpath( """./following-sibling::span/button[2]""" ) )
    b.clear()
  }

  def isSelectTimePopupVisible(implicit pos1: Position ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[3]""" ) ) )(pos1)
    !b.containsClass("rw-popup-transition-exited")
  }

  /**
   * @return get all the days that are visible in calendar popup.
   * if the popup is not visible, then the empty list is returned.
   */
  def getTimes(implicit pos1: Position ): List[String] = {
    val b = underlying.findElements(By.xpath( """./parent::div/parent::div/div[2]/div/div/div/ul/li""" ) ).asScala
    b.map { cell =>
      new Element(cell)(pos1).text
    }.toList
  }

  def clickTime( d: String)(implicit pos1: Position ) = {
    val b = new Element( underlying.findElement(By.xpath( s"""./parent::div/parent::div/div[2]/div/div/div/ul/li[text()='${d}']""" ) ) )(pos1)
    b.click
  }

}


