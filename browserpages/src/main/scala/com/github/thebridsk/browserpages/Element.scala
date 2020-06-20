package com.github.thebridsk.browserpages

import org.openqa.selenium.WebElement
import org.scalactic.source.Position
import org.openqa.selenium.By
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import org.scalatest.concurrent.Eventually.PatienceConfig
import com.github.thebridsk.utilities.logging.Logger
import org.openqa.selenium.OutputType
import com.github.thebridsk.utilities.file.FileIO
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.{ NoSuchElementException => SelNoSuchElementException }

class Element( val underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) {

    def location = underlying.getLocation

    def size = underlying.getSize

    def isDisplayed: Boolean = underlying.isDisplayed

    def isEnabled: Boolean = underlying.isEnabled

    def isSelected: Boolean = underlying.isSelected

    def tagName: String = underlying.getTagName

    def attribute(name: String): Option[String] = Option(underlying.getAttribute(name))

    /**
     * Get the value of a given CSS property. Color values should be returned as rgba strings,
     * so, for example if the "background-color" property is set as "green" in the HTML source,
     * the returned value will be "rgba(0, 255, 0, 1)". Note that shorthand CSS properties
     * (e.g. background, font, border, border-top, margin, margin-top, padding, padding-top,
     * list-style, outline, pause, cue) are not returned, in accordance with the DOM CSS2
     * specification - you should directly access the longhand properties (e.g. background-color)
     *  to access the desired values.
     *
     * @param propertyName - the css property name of the element
     * @return The current, computed value of the property.
     */
    def cssValue( name: String ) = underlying.getCssValue(name)

    def id = attribute("id")

    def containsClass( cls: String ) = attribute("class").map{ clses =>
      clses.split(" ").contains(cls)
    }.getOrElse(false)

    def text: String = {
      Option( underlying.getText ).getOrElse("")
    }

    def scrollToElement = {
      PageBrowser.scrollToElement(underlying)
    }

    def click = {
      PageBrowser.scrollToElement(underlying)
      underlying.click()
//      Thread.sleep( 100L )
    }

    def isClickable = {
      PageBrowser.scrollToElement(underlying)
      isDisplayed && isEnabled
    }

    def checkClickable = {
      PageBrowser.scrollToElement(underlying)
      if (!isDisplayed) {
        val disp = underlying.getCssValue("display")
        val width = underlying.getCssValue("width")
        val height = underlying.getCssValue("height")
        throw new Exception(s"Not clickable, not displayed, $disp $height x $width")
      }
      if (!isEnabled) {
        throw new Exception("Not clickable, disabled")
      }
    }

    /**
     * Send enter to the element.
     * Most of the time this is functionally equivalent to clicking the element.
     */
    def enter = {
      PageBrowser.scrollToElement(underlying)
      sendKeys( Keys.ENTER.toString() )
//      Thread.sleep( 100L )
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

class InputElement( underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) extends Element(underlying) {

  def this( el: Element )(implicit pos1: Position, webdriver1: WebDriver, patienceConfig1: PatienceConfig ) = this(el.underlying)(pos1,webdriver1,patienceConfig1)

  def `type` = attribute("type")

  def name = attribute("name")

}

class TextField( underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig1: PatienceConfig ) extends InputElement(underlying) {

  def this( el: Element )(implicit pos1: Position, webdriver1: WebDriver, patienceConfig1: PatienceConfig ) = this(el.underlying)(pos1,webdriver1,patienceConfig1)

  def value: String = underlying.getAttribute("value")

  def value_=(value: String): Unit = {
//    underlying.clear()
    PageBrowser.scrollToElement(underlying)
    underlying.sendKeys(Keys.chord(Keys.CONTROL, "a"))
    underlying.sendKeys(value)
  }

  def clear(): Unit = { underlying.clear() }

}

class NumberField( underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) extends TextField(underlying) {
  def this( el: Element )(implicit pos1: Position, webdriver1: WebDriver, patienceConfig1: PatienceConfig ) = this(el.underlying)(pos1,webdriver1,patienceConfig1)
}

class RadioButton( underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) extends InputElement(underlying) {

  def this( el: Element )(implicit pos1: Position, webdriver1: WebDriver, patienceConfig1: PatienceConfig ) = this(el.underlying)(pos1,webdriver1,patienceConfig1)

  private lazy val input = find( PageBrowser.xpath( """.//input""" ))

  override
  def click = input.click

  override
  def isSelected = {
    try {
      find( PageBrowser.xpath( s"""./span[1][contains(concat(' ', @class, ' '), ' Mui-checked ')]"""))
      true
    } catch {
      case x: SelNoSuchElementException =>
        false
    }

  }

  override
  def name = input.attribute("name")
  override
  def id = input.attribute("id")

  def label = find( PageBrowser.xpath( """./*[2]""" ))
}

object RadioButton {

  def findAll()(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.findAll( PageBrowser.xpath(s"""//label[contains(concat(' ', @class, ' '), ' baseRadioButton ')]""") )
    el.map( e => new RadioButton(e.underlying) )
  }

  def find( name: String )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.find( PageBrowser.xpath(s"""//label[contains(concat(' ', @class, ' '), ' baseRadioButton ') and .//input[@id='${name}']]""") )
    new RadioButton(el.underlying)
  }

}

class Checkbox( underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) extends InputElement(underlying) {

  def this( el: Element )(implicit pos1: Position, webdriver1: WebDriver, patienceConfig1: PatienceConfig ) = this(el.underlying)(pos1,webdriver1,patienceConfig1)

  private lazy val input = find( PageBrowser.xpath( """.//input""" ))

  override
  def click = input.click

  override
  def isSelected = {
    try {
      find( PageBrowser.xpath( """./span[1][contains(concat(' ', @class, ' '), ' Mui-checked ')]""" ))
      true
    } catch {
      case x: SelNoSuchElementException =>
        false
    }
  }
  // def value: String = underlying.getAttribute("value")

  override
  def name = input.attribute("name")
  override
  def id = input.attribute("id")

  def label = find( PageBrowser.xpath( """./*[2]""" ))

}

object Checkbox {

  def findAll()(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.findAll( PageBrowser.xpath(s"""//label[contains(concat(' ', @class, ' '), ' baseCheckbox ')]""") )
    el.map( e => new Checkbox(e.underlying) )
  }

  def find( name: String )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.find( PageBrowser.xpath(s"""//label[contains(concat(' ', @class, ' '), ' baseCheckbox ') and .//input[@id='${name}']]""") )
    new Checkbox(el.underlying)
  }

  def findAllChecked()(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.findAll( PageBrowser.xpath(s"""//label[contains(concat(' ', @class, ' '), ' baseCheckbox ') and ./span[1][contains(concat(' ', @class, ' '), ' Mui-checked ')]]""") )
    el.map( e => new Checkbox(e.underlying) )
  }

}

/**
 * @constructor
 * @param underlying - the input element of the combobox
 */
class Combobox( underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) extends TextField(underlying) {

  def this( el: Element )(implicit pos1: Position, webdriver1: WebDriver, patienceConfig1: PatienceConfig ) = this(el.underlying)(pos1,webdriver1,patienceConfig1)

  def suggestions: List[Element] = {
    underlying.findElements(By.xpath( """./parent::div/following-sibling::div/div/div/ul/li""" ) ).asScala.map(e => new Element(e)).toList
  }

  def isSuggestionVisible = {
    underlying.findElement(By.xpath( """./parent::div/following-sibling::div/div""" ) ).isDisplayed()
  }

  def clickCaret = {
    underlying.findElement(By.xpath("./following-sibling::span/button")).click()
  }

  def esc = {
    underlying.sendKeys(Keys.ESCAPE)
  }
}

object Combobox {

  def findAll()(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.findAll( PageBrowser.xpath(s"""//div[contains(concat(' ', @class, ' '), ' rw-combobox ')]/div/input""") )
    el.map( e => new Combobox(e.underlying) )
  }

  def find( name: String )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.find( PageBrowser.xpath(s"""//div[contains(concat(' ', @class, ' '), ' rw-combobox ')]/div/input[@name='${name}']""") )
    new Combobox(el.underlying)
  }
}

/**
 * @constructor
 * @param underlying - the input element of the combobox
 */
class DateTimePicker( underlying: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) extends TextField(underlying) {

  def this( el: Element )(implicit pos1: Position, webdriver1: WebDriver, patienceConfig1: PatienceConfig ) = this(el.underlying)(pos1,webdriver1,patienceConfig1)

  def clickSelectDate(implicit pos1: Position ) = {
    val b = underlying.findElement(By.xpath( """./following-sibling::span/button[1]""" ) )
    b.click
  }

  def isSelectDatePopupVisible(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[2]""" ) ) )(pos1,webdriver,patienceConfig)
    !b.containsClass("rw-popup-transition-exited")
  }

  def clickPreviousMonth(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[2]/div/div/div/button[1]""" ) ) )(pos1,webdriver,patienceConfig)
    b.click
  }

  def clickNextMonth(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[2]/div/div/div/button[2]""" ) ) )(pos1,webdriver,patienceConfig)
    b.click
  }

  /**
   * @return get all the days that are visible in calendar popup.
   * if the popup is not visible, then the empty list is returned.
   */
  def getDays(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ): List[String] = {
    val b = underlying.findElements(By.xpath( """./parent::div/parent::div/div[3]/div/div/div[2]/table/tbody/tr/td""" ) ).asScala
    b.flatMap { cell =>
      new Element(cell)(pos1,webdriver,patienceConfig).attribute("aria-label")
    }.toList
  }

  def clickDay( d: String)(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val b = new Element( underlying.findElement(By.xpath( s"""./parent::div/parent::div/div[3]/div/div/div[2]/table/tbody/tr/td[@aria-label='${d}']""" ) ) )(pos1,webdriver,patienceConfig)
    b.click
  }

  def clickSelectTime(implicit pos1: Position ) = {
    val b = underlying.findElement(By.xpath( """./following-sibling::span/button[2]""" ) )
    b.clear()
  }

  def isSelectTimePopupVisible(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val b = new Element( underlying.findElement(By.xpath( """./parent::div/parent::div/div[3]""" ) ) )(pos1,webdriver,patienceConfig)
    !b.containsClass("rw-popup-transition-exited")
  }

  /**
   * @return get all the days that are visible in calendar popup.
   * if the popup is not visible, then the empty list is returned.
   */
  def getTimes(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ): List[String] = {
    val b = underlying.findElements(By.xpath( """./parent::div/parent::div/div[2]/div/div/div/ul/li""" ) ).asScala
    b.map { cell =>
      new Element(cell)(pos1,webdriver,patienceConfig).text
    }.toList
  }

  def clickTime( d: String)(implicit pos1: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val b = new Element( underlying.findElement(By.xpath( s"""./parent::div/parent::div/div[2]/div/div/div/ul/li[text()='${d}']""" ) ) )(pos1,webdriver,patienceConfig)
    b.click
  }

}

object DateTimePicker {

  def findAll()(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.findAll( PageBrowser.xpath(s"""//div[contains(concat(' ', @class, ' '), ' rw-datetime-picker ')]/div/input""") )
    el.map( e => new DateTimePicker(e.underlying) )
  }

  def find( name: String )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig ) = {
    val el = PageBrowser.find( PageBrowser.xpath(s"""//div[contains(concat(' ', @class, ' '), ' rw-datetime-picker ')]/div/input[@name='${name}']""") )
    new DateTimePicker(el.underlying)
  }
}
