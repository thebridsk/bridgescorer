package com.github.thebridsk.browserpages

import com.github.thebridsk.utilities.logging.Logger
import org.openqa.selenium.WebElement
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Eventually.PatienceConfig
import org.openqa.selenium.WebDriver
import org.openqa.selenium.Keys
import org.openqa.selenium.JavascriptExecutor
import com.github.thebridsk.source.SourcePosition
import org.scalatestplus.selenium.WebBrowser
import org.openqa.selenium.By
import scala.jdk.CollectionConverters._
import java.net.URL
import scala.reflect.ClassTag
import java.lang.reflect.Constructor
import java.io.File
import java.nio.file.Path
import java.nio.file.FileSystems
import com.github.thebridsk.utilities.file.FileIO
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.OutputType
import scala.io.Codec

object PageBrowsersImplicits {
  import scala.language.implicitConversions

  implicit def convertWebElementToElement( webElement: WebElement )(implicit pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig) = new Element(webElement)

}

import PageBrowsersImplicits._
import org.openqa.selenium.interactions.Action
import org.openqa.selenium.interactions.Actions

abstract class QueryBy(implicit webDriver: WebDriver) {
  /**
   * The query to search with
   */
  def query: By

  /**
   * Query the page with the query
   * @return the element
   * @throws org.openqa.selenium.NoSuchElement if not found
   */
  def queryElement(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Element = {
    webDriver.findElement(query)
  }

  /**
   * Query the page with the query
   * @return the elements, empty list is returned if nothing matches
   */
  def queryElements(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    webDriver.findElements(query).asScala.map(e => new Element(e)).toList
  }

  /**
   * Query the page with the query
   * @param searchFrom the starting element for the search
   * @return the element
   * @throws org.openqa.selenium.NoSuchElement if not found
   */
  def queryElement( searchFrom: Element )(implicit patienceConfig: PatienceConfig, pos: Position): Element = {
    searchFrom.underlying.findElement(query)
  }


  /**
   * Query the page with the query
   * @param searchFrom the starting element for the search
   * @return the elements, empty list is returned if nothing matches
   */
  def queryElements( searchFrom: Element )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    searchFrom.underlying.findElements(query).asScala.map(e => new Element(e)).toList
  }
}

class GoTo(implicit createdpos: SourcePosition) {
  import PageBrowser.log
  def to( url: String )(implicit webDriver: WebDriver, pos: Position) = {
    log.fine( s"Going to ${url} from ${pos.line}" )
    webDriver.get(url)
  }
  def to( url: URL )(implicit webDriver: WebDriver, pos: Position) = {
    log.fine( s"Going to ${url} from ${pos.line}" )
    webDriver.get(url.toString())
  }
}

class ClickOn(implicit createdpos: SourcePosition) {
  def on( e: WebBrowser.Element )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = on(e.underlying)
  def on( e: Element )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = on(e.underlying)
  def on( e: WebElement )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = eventually {
    PageBrowser.log.fine( s"Clicking on ${e}: patienceConfig=${patienceConfig}, pos=${pos.line}" )
//    moveToElement(e)
    scrollToElement(e)
//    PageBrowser.log.fine( s"""Clicking on ${e}: text = ${e.text}""" )
    e.click()
  }
  def on( query: QueryBy )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    PageBrowser.log.fine( s"Finding element ${query} to click: patienceConfig=${patienceConfig}, pos=${pos.line}" )
    val e = query.queryElement
    PageBrowser.log.fine( s"Clicking on element ${query} ${e}: patienceConfig=${patienceConfig}, pos=${pos.line}" )
    on(e)
  }

  def moveToElement( e: WebElement )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    new Actions(webDriver).moveToElement(e).perform()
  }

  def scrollToElement( e: WebElement )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    PageBrowser.executeScript( """arguments[0].scrollIntoView({behavior: "auto", block: "center", inline: "center"});""", e);
//    Thread.sleep(200)
  }
}

trait PageBrowser {

  def esc(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): this.type = {
    pressKeys(Keys.ESCAPE)
    this
  }

  def refresh(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): this.type = {
    webDriver.navigate().refresh()
    this
  }

  def enter(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): this.type = {
    pressKeys(Keys.ENTER)
    this
  }

  def pressKeys(value: CharSequence )(implicit webDriver: WebDriver, pos: Position) = {
    val ae: WebElement = webDriver.switchTo.activeElement
    ae.sendKeys(value)
//    Thread.sleep( 100L )
    this
  }

  def currentUrl(implicit webDriver: WebDriver, pos: Position) = {
    webDriver.getCurrentUrl
  }

  def pageTitle(implicit webDriver: WebDriver, pos: Position) = {
    webDriver.getTitle
  }

  /**
   * Executes JavaScript in the context of the currently selected frame or window.
   * The script fragment provided will be executed as the body of an anonymous function.
   *
   * <p>
   * Within the script, you can use <code>document</code> to refer to the current document.
   * Local variables will not be available once the script has finished executing, but global variables will.
   * </p>
   *
   * <p>
   * To return a value (e.g. if the script contains a return statement), then the following steps will be taken:
   * </p>
   *
   * <ol>
   *   <li>For an HTML element, this method returns a WebElement</li>
   *   <li>For a decimal, a Double is returned</li>
   *   <li>For a non-decimal number, a Long is returned</li>
   *   <li>For a boolean, a Boolean is returned</li>
   *   <li>For all other cases, a String is returned</li>
   *   <li>For an array, return a List<Object> with each object following the rules above. We support nested lists</li>
   *   <li>Unless the value is null or there is no return value, in which null is returned</li>
   * </ol>
   *
   * <p>
   * Script arguments must be a number, boolean, String, WebElement, or a List of any combination of these. An exception will
   * be thrown if the arguments do not meet these criteria. The arguments will be made available to the JavaScript via the "arguments" variable.
   * (Note that although this behavior is specified by <a href="http://selenium.googlecode.com/git/docs/api/java/org/openqa/selenium/JavascriptExecutor.html">Selenium's JavascriptExecutor Javadoc</a>,
   * it may still be possible for the underlying <code>JavascriptExecutor</code> implementation to return an objects of other types.
   * For example, <code>HtmlUnit</code> has been observed to return a <code>java.util.Map</code> for a Javascript object.)
   * </p>
   *
   * @param script the JavaScript to execute
   * @param args the arguments to the script, may be empty
   * @return One of Boolean, Long, String, List or WebElement. Or null (following <a href="http://selenium.googlecode.com/git/docs/api/java/org/openqa/selenium/JavascriptExecutor.html">Selenium's JavascriptExecutor Javadoc</a>)
   */
  def executeScript[T](script: String, args: AnyRef*)(implicit webDriver: WebDriver): AnyRef =
    webDriver match {
      case executor: JavascriptExecutor => executor.executeScript(script, args: _*)
      case _ => throw new UnsupportedOperationException("Web driver " + webDriver.getClass.getName + " does not support javascript execution.")
    }

  def saveDom( tofile: String )(implicit webDriver: WebDriver): Unit = {
    try {
      reflect.io.File(tofile)(Codec.UTF8).writeAll( executeScript("return document.documentElement.outerHTML")(webDriver).toString() )
    } catch {
      case e: Exception =>
        PageBrowser.log.warning("Exception trying to execute a script in browser", e)
    }
  }

  def go(implicit webDriver: WebDriver, pos: Position) = new GoTo

  def id( s: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = new QueryBy {
    def query = By.id(s)
    override
    def toString() = {
      s"""QueryBy id ${s}"""
    }
  }

  def name( s: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = new QueryBy {
    def query = By.name(s)
    override
    def toString() = {
      s"""QueryBy name ${s}"""
    }
  }

  def xpath( s: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = new QueryBy {
    def query = By.xpath(s)
    override
    def toString() = {
      s"""QueryBy xpath ${s}"""
    }
  }

  def cssSelector( s: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = new QueryBy {
    def query = By.cssSelector(s)
    override
    def toString() = {
      s"""QueryBy cssSelector ${s}"""
    }
  }

  def className( s: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = new QueryBy {
    def query = By.className(s)
    override
    def toString() = {
      s"""QueryBy className ${s}"""
    }
  }

  def tagName( s: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = new QueryBy {
    def query = By.tagName(s)
    override
    def toString() = {
      s"""QueryBy tagName ${s}"""
    }
  }

  def findOption( by: QueryBy )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Option[Element] = {
    try {
      Some( by.queryElement )
    } catch {
      case x: org.openqa.selenium.NoSuchElementException =>
        None
    }
  }

  def find( by: QueryBy )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    by.queryElement
  }

  def findAll( by: QueryBy )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    by.queryElements
  }

  def findAllTextInputs(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    GenericPage.current.findAllTextInputs()
  }

  def findAllInputs(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    GenericPage.current.findAllInputs()
  }

  def click(implicit pos: Position) = new ClickOn

  def textField( tid: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): TextField =
    // new TextField( find(name(tid)) )
    findElem[TextField]( name(tid) )

  def findElem[T <: Element](
      by: QueryBy
    )(implicit webDriver: WebDriver,
               patienceConfig: PatienceConfig,
               pos: Position,
               classtag: ClassTag[T]
    ): T = {
    val el = find(by)
    getElement(el)
  }

  def getElement[T <: Element](
      elem: Element
    )(implicit webDriver: WebDriver,
                patienceConfig: PatienceConfig,
                pos: Position,
                classtag: ClassTag[T]
    ): T = {
    if (classtag.runtimeClass == classOf[Element]) elem.asInstanceOf[T]
    else {
      implicit val con = PageBrowser.getConstructor[T]
      newInstance(elem.underlying,pos,webDriver,patienceConfig)
    }
  }

  private def newInstance[T <: Element]( e: WebElement, pos: Position, webdriver: WebDriver, patienceConfig: PatienceConfig )(implicit con: Constructor[T]): T = {
    con.newInstance(e,pos,webdriver,patienceConfig).asInstanceOf[T]
  }

  def findAllElems[T <: Element](
      by: QueryBy
    )(implicit webDriver: WebDriver,
               patienceConfig: PatienceConfig,
               pos: Position,
               classtag: ClassTag[T]
    ): List[T] = {
    val el = findAll(by)
    implicit val con = PageBrowser.getConstructor[T]
    el.map( e => newInstance(e.underlying,pos,webDriver,patienceConfig))
  }

  def getAllElements[T <: Element](
      el: List[Element]
    )(implicit patienceConfig: PatienceConfig,
                webdriver: WebDriver,
               pos: Position,
               classtag: ClassTag[T]
    ): List[T] = {
    implicit val con = PageBrowser.getConstructor[T]
    el.map( e => newInstance(e.underlying,pos,webdriver,patienceConfig))
  }

  def takeScreenshot( directory: String, filename: String )( implicit webDriver: WebDriver, pos: Position ): Unit = {
    val f = if (filename.endsWith(".png")) filename else filename+".png"
    if (webDriver.isInstanceOf[TakesScreenshot]) {
      try {
        val scrFile = webDriver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE);
        val destFile = PageBrowser.getPath(directory,f)
        FileIO.mktree(PageBrowser.getPath(directory))
        FileIO.copyFile( PageBrowser.getPath(scrFile), destFile )
        FileIO.writeFileSafe(destFile.toString()+".url.txt", webDriver.getCurrentUrl)
      } catch {
        case x: Exception =>
          PageBrowser.log.warning(s"Unable to take screenshot, called from ${pos.line}", x)
          throw x
      }
    } else {
      PageBrowser.log.warning(s"Unable to take screenshot, called from ${pos.line}")
    }
  }

  /**
   * Take a screenshot.
   * The filename of the screenshot file will be generated from the position object.  filename-linenumber.png
   * @param directory The directory where the screenshot is written to
   */
  def takeScreenshot( directory: String )( implicit webDriver: WebDriver, pos: Position ): Unit = {
    val filename = pos.lineForFilename
    takeScreenshot(directory, filename)
  }

  /**
   * Take a screenshot if an exception is thrown by fun.
   * @param directory The directory where the screenshot is written to
   * @param filename The name of the file where the screenshot is written to.  It it doesn't end in ".png", then ".png" will be appended.
   */
  def takeScreenshotOnError[T]( directory: String, filename: String, savedom: Boolean = false )(fun: => T)( implicit webDriver: WebDriver, pos: Position ): T = {
    try {
      fun
    } catch {
      case x: Throwable =>
        takeScreenshot(directory, filename)
        if (savedom) {
          val f = if (filename.endsWith(".dom.html")) filename else filename+".dom.html"
          val destFile = PageBrowser.getPath(directory,f)
          saveDom(destFile.toString())
        }
        PageBrowser.log.severe("Error with screenshot: ", x)
        throw x
    }
  }

  /**
   * Take a screenshot if an exception is thrown by fun.
   * The filename of the screenshot file will be generated from the position object.  filename-linenumber.png
   * @param directory The directory where the screenshot is written to
   */
  def takeScreenshotOnError[T]( directory: String )(fun: => T)( implicit webDriver: WebDriver, pos: Position ): T = {
    val filename = pos.lineForFilename
    takeScreenshotOnError(directory, filename)(fun)
  }

  /**
   * Executes the specified function, fun, with the specified clue.
   * <p>
   * On an execption being thrown, a screen shot is also taken, the screen shot file is put into the specified directory with the
   * given filename.
   *
   * <p>
   * This method allows you to add more information about what went wrong that will be
   * reported when a test fails. Here's an example:
   * </p>
   *
   * <pre class="stHighlight">
   * withClueAndScreenShot( dir, "xxx", "(Employee's name was: " + employee.name + ")") {
   *   intercept[IllegalArgumentException] {
   *     employee.getTask(-1)
   *   }
   * }
   * </pre>
   *
   * <p>
   * If an invocation of <code>intercept</code> completed abruptly with an exception, the resulting message would be something like:
   * </p>
   *
   * <pre>
   * (Employee's name was Bob Jones) Expected IllegalArgumentException to be thrown, but no exception was thrown
   * </pre>
   *
   * @param directory the directory that will get the screenshot file
   * @param filenamePrefix the prefix of the screenshot filename.
   * @param fun the function to execute
   * @throws NullArgumentException if the passed <code>clue</code> is <code>null</code>
  */
  def withClueAndScreenShot[T]( directory: String, filenamePrefix: String, clue: Any, savedom: Boolean = false)(fun: => T)(implicit webDriver: WebDriver, pos: Position): T = {
    takeScreenshotOnError(directory, s"${filenamePrefix}_${pos.lineForFilename}", savedom) {
      import org.scalatest.Assertions._
      withClue(clue)(fun)
    }
  }

  def moveToElement( e: Element )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    new Actions(webDriver).moveToElement(e.underlying).perform()
  }

  def moveToElement( e: WebElement )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    new Actions(webDriver).moveToElement(e).perform()
  }

  def scrollToElement( e: Element )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    PageBrowser.executeScript( """arguments[0].scrollIntoView({behavior: "auto", block: "center", inline: "center"});""", e.underlying);
  }

  def scrollToElement( e: WebElement )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    PageBrowser.executeScript( """arguments[0].scrollIntoView({behavior: "auto", block: "center", inline: "center"});""", e);
  }

  def scrollToElement( e: org.scalatestplus.selenium.WebBrowser.Element )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    PageBrowser.executeScript( """arguments[0].scrollIntoView({behavior: "auto", block: "center", inline: "center"});""", e.underlying)
  }

  def scrollToTop(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = {
    PageBrowser.executeScript("window.scrollTo(0, 0)")
  }
  /**
   * @param e the element
   */
  def isSoundPlaying( e: WebElement )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Boolean = {
    PageBrowser.executeScript("""!arguments[0].paused || arguments[0].currentTime""", e).asInstanceOf[Boolean]
  }

  /**
   * @param e the element
   */
  def isSoundPlaying( e: Element )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Boolean = {
    isSoundPlaying(e.underlying)
  }

  /**
   * @param id the id of the element
   * @throws org.openqa.selenium.NoSuchElement if element with specified id is not found
   */
  def isSoundPlaying( ida: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Boolean = {
    val e = find( id(ida) )
    isSoundPlaying(e)
  }

}

object PageBrowser extends PageBrowser {

  private[browserpages] val log = Logger[PageBrowser]

  private[PageBrowser] def getConstructor[T <: Element](implicit classtag: ClassTag[T]) =
    classtag.runtimeClass.getDeclaredConstructor(classOf[WebElement],classOf[Position],classOf[WebDriver],classOf[PatienceConfig] ).asInstanceOf[Constructor[T]]

  private[browserpages] def getPath( filename: File ): Path = FileSystems.getDefault.getPath(filename.toString())

  private[browserpages] def getPath( filename: String ): Path = FileSystems.getDefault.getPath(filename)

  private[browserpages] def getPath( directory: String, filename: String ): Path = FileSystems.getDefault.getPath(directory,filename)

}
