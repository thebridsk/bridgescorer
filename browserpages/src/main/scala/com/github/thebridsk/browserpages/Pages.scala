
package com.github.thebridsk.browserpages

import scala.concurrent.Future
import com.github.thebridsk.utilities.main.Main
import scala.concurrent.duration.Duration
import org.openqa.selenium.WebDriver
import com.github.thebridsk.source.SourcePosition
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Eventually.PatienceConfig
import org.scalatest.MustMatchers._
import com.github.thebridsk.utilities.logging.Logger
import org.openqa.selenium.By.ByName
import org.openqa.selenium.By.ByTagName
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import org.openqa.selenium.Keys
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor
import org.scalatest.Failed
import org.scalatest.Canceled

object Page {
  val testlog = Logger( getClass.getName )

  type AnyPage = Page[_]
}

abstract class Page[ +T <: Page[T] ]()( implicit webDriver: WebDriver, pageCreated: SourcePosition ) {
  self: T =>
  import Page._
  import webDriver._
  import PageBrowser._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position): T

  def logMethod[T]( name: String )( f: => T )(implicit pos: Position): T = {
    testlog.fine(s"${pos.line} Starting ${name}")
    try {
      val r = f
      testlog.fine(s"${pos.line} Return ${name}: ${r}")
      r
    } catch {
      case x: Exception =>
        testlog.fine(s"${pos.line} Exception return ${name}: ${x}", x)
        throw x
    }

  }

  def logMethod0[T]( name: String )( f: => T ): T = {
    testlog.fine(s"Starting ${name}")
    try {
      val r = f
      testlog.fine(s"Return ${name}")
      r
    } catch {
      case x: Exception =>
        testlog.fine(s"Exception return ${name}: ${x}",x)
        throw x
    }

  }

  /**
   * Find a button
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text optionally the text that must appear on the button
   * @param buttontype optionally the value of the <code>type</code> attribute of the button.
   *                   if not specified, the value "button" is used.
   * @param pos the source position of the caller
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def findButton( eid: String,
                  text: Option[String] = None,
                  buttontype: Option[String] = None
                )(implicit pos: Position): Element = {
    val bType = buttontype match {
      case Some(s) => s
      case None => "button"
    }
    val t = text.map( t => s""" and text()='$t'""" ).getOrElse("")
    val bt = buttontype.map( t => s""" and @type='$t'""" ).getOrElse("")
    val x = s"""//button[@id='${eid}'${t}${bt}]"""
    findElemByXPath(x)
  }

  /**
   * Find a button
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text optionally the text that must appear on the button
   * @param buttontype optionally the value of the <code>type</code> attribute of the button.
   *                   if not specified, the value "button" is used.
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the source position of the caller
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def getButton( eid: String,
                 text: Option[String] = None,
                 buttontype: Option[String] = None
               )(implicit patienceConfig: PatienceConfig, pos: Position): Element = {
    eventually { findButton(eid, text, buttontype) }
  }

  /**
   * Find all the buttons that match.
   * @param ids the ids of the buttons to return
   * @param pos the source position of the caller
   * @return a map of id -> Element
   * @throws TestFailedException if all the buttons are not found
   */
  def findButtons( ids: String* )(implicit pos: Position): Map[String,Element] = {
    // This is using WebDriver.findElements because WebBrowser.findAll is very expensive to call
    val buttons = webDriver.findElements(By.tagName("button")).asScala.flatMap { b =>
      Option(b.getAttribute("id")) match {
        case Some(s) if ids.contains(s) => (s, new Element(b))::Nil
        case _ => Nil
      }
    }.toMap
    withClue(s"""${pos.line} findButtons${ids.mkString("[", ",", "]")} only found ${buttons.keySet.mkString("[", ",", "]")}, page created ${pageCreated.line}""") {
      buttons.size mustBe ids.size
    }
    buttons
  }

  /**
   * Get all the buttons that match.  The text must match.
   * @param map Map[String,String]  id->text
   * @param pos the source position of the caller
   * @throws TestFailedException if all the buttons are not found or any of the texts don't match
   */
  def findButtons( map: Map[String,String] )(implicit pos: Position): Map[String,Element] = {
    val buttons = findButtons( map.keySet.toList :_* )

    withClue(s"""finding buttons ${map}""") {
      buttons.foreach{ case (id,e) => map.get(id) match {
        case Some(t) =>
          withClue(s"""checking text on $id to be $t""") {
            val txt = e.text
            txt mustBe t
          }
        case None =>
      }}
      buttons
    }
  }

  /**
   * Find all the buttons that match.
   * @param ids the ids of the buttons to return
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the source position of the caller
   * @return a map of id -> Element
   * @throws TestFailedException if all the buttons are not found
   */
  def getButtons( ids: String* )(implicit patienceConfig: PatienceConfig, pos: Position): Map[String,Element] = {
    eventually { findButtons(ids: _*) }
  }

  /**
   *
   */
  def findAllButtons(implicit pos: Position): Map[String,Element] = {
    findElements(By.cssSelector("button")).asScala.map(b => (b.getAttribute("id"),new Element(b))).toMap
  }

  /**
   * Get all the buttons, at least one button is returned.
   */
  def getAllButtons(implicit patienceConfig: PatienceConfig, pos: Position): Map[String,Element] = eventually {
    val bs = findAllButtons
    bs.size must not be (0)
    bs
  }

  def clickButton( bid: String )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = {
    try {
      eventually {
        val we = findElemById(bid)
//        PageBrowser.scrollToElement(we.underlying)
//        if (!we.underlying.isDisplayed()) Thread.sleep(100)
        we.click
      }
    } catch {
      case x: Exception =>
        log.warning(s"""Got exception clicking button, patienceConfig = ${patienceConfig}""",x)
        throw x
    }
    self
  }

  def enterOnButton( bid: String )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = {
    try {
      eventually {
        val we = findElemById(bid)
//        PageBrowser.scrollToElement(we.underlying)
        if (!we.underlying.isDisplayed()) Thread.sleep(100)
        we.enter
      }
    } catch {
      case x: Exception =>
        log.warning(s"""Got exception clicking button, patienceConfig = ${patienceConfig}""",x)
        throw x
    }
    self
  }

  /**
   * Find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>TextField</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def findTextInput( iname: String )(implicit pos: Position, patienceConfig: PatienceConfig ): TextField = {
    new TextField(findInput(iname,"text"))(pos,webDriver,patienceConfig)
  }

  /**
   * Find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>TextField</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def findNumberInput( iname: String )(implicit pos: Position, patienceConfig: PatienceConfig ): NumberField = {
    new NumberField( findInput(iname,"number") )(pos,webDriver,patienceConfig)
  }

  /**
   * Find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def findInput( iname: String, itype: String )(implicit pos: Position): Element = {
    try {
      val input = webDriver.findElement(new ByName(iname))
      withClue(s"And has an input field with name ${iname}, page created ${pageCreated.line}") { input.getTagName mustBe "input" }
      withClue(s"And has an input field with name ${iname} and with type ${itype}, page created ${pageCreated.line}") { input.getAttribute("type") mustBe itype }
      new Element(input)
    } catch {
      case x: Throwable =>
        testlog.fine (s"findInput: exception ${x.toString()}, page created ${pageCreated.line}", x)
//        x.printStackTrace(System.out)
        throw x
    }
  }

  /**
   * Find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def getTextInput( iname: String )(implicit patienceConfig: PatienceConfig, pos: Position): TextField = {
    eventually { findTextInput( iname ) }
  }

  /**
   * Find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def getNumberInput( iname: String )(implicit patienceConfig: PatienceConfig, pos: Position): TextField = {
    eventually { findNumberInput( iname ) }
  }

  /**
   * Find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def getInput( iname: String, itype: String )(implicit patienceConfig: PatienceConfig, pos: Position): Element = {
    eventually { findInput( iname, itype ) }
  }

  /**
   * Find all input field
   * @param itype the value of the <code>type</code> attribute of the input
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def findAllTextInputs()(implicit pos: Position): Map[String,TextField] = {
    val x = findAllInputs( Some("text") ).map(e => e._1->new TextField(e._2))
    x
  }

  /**
   * Find all input field
   * @param itype the value of the <code>type</code> attribute of the input
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def findAllInputs( itype: Option[String] = None )(implicit pos: Position): Map[String,Element] = {
    try {
      val search = itype.map(t => s"""[@type='${t}']""").getOrElse("")

      def getEntry( tf: Element ) = {
        tf.attribute("name") match {
          case Some(n) => (n, tf)::Nil
          case None => Nil
        }
      }

      val input = findElemsByXPath(s"//input${search}").
                    flatMap{ tf =>
                      itype match {
                        case Some(st) =>
                          if (tf.attribute("type").getOrElse("") == st) {
                            getEntry(tf)
                          } else {
                            Nil
                          }
                        case None =>
                          getEntry(tf)
                      }
                    }.
                    toMap
      input
    } catch {
      case x: Throwable =>
        testlog.fine (s"${pos.line} findAllInput(${itype}): exception ${x.toString()}, page created ${pageCreated.line}", x)
//        x.printStackTrace(System.out)
        throw x
    }
  }

  /**
   * Get all the input fields of the specified type
   */
  def getAllTextInputs()(implicit patienceConfig: PatienceConfig, pos: Position): Map[String,TextField] = eventually {
    val bs = findAllTextInputs()
    bs
  }

  /**
   * Get all the input fields of the specified type
   */
  def getAllInputs( itype: Option[String] = None )(implicit patienceConfig: PatienceConfig, pos: Position): Map[String,Element] = eventually {
    val bs = findAllInputs(itype)
    bs
  }

  /**
   * Find all input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def findTextInputs( iname: String* )(implicit pos: Position): Map[String,TextField] = {
    findInputs("text",iname:_*).map(e => e._1->new TextField(e._2))
  }

  /**
   * Find all input field
   * @param itype the value of the <code>type</code> attribute of the input
   * @param iname the value of the <code>name</code> attribute of the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def findInputs( itype: String, iname: String* )(implicit pos: Position, patienceConfig: PatienceConfig): Map[String,Element] = {
    try {
      val input = findElements(By.tagName("input")).asScala.
                    flatMap{ we =>
                      if (we.getAttribute("type") == itype) {
                        val n = we.getAttribute("name")
                        if (iname.contains(n)) {
                          (n, new Element(we)(pos,webDriver,patienceConfig))::Nil
                        } else {
                          Nil
                        }
                      } else {
                        Nil
                      }
                    }.
                    toMap
      withClue(s"""${pos.line} findInputs(${itype},${iname.toList}), page created ${pageCreated.line}""") {
        input.size mustBe iname.size
      }
      input
    } catch {
      case x: Throwable =>
        testlog.fine (s"${pos.line} findInputs(${itype},${iname.toList}): exception ${x.toString()}, page created ${pageCreated.line}", x)
//        x.printStackTrace(System.out)
        throw x
    }
  }

  /**
   * Find all input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def getTextInputs( iname: String* )(implicit patienceConfig: PatienceConfig, pos: Position): Map[String,TextField] = {
    eventually { findTextInputs(iname: _*) }
  }

  /**
   * Find all input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def getInputs( itype: String, iname: String* )(implicit patienceConfig: PatienceConfig, pos: Position): Map[String,Element] = {
    eventually { findInputs(itype, iname: _*) }
  }

  /**
   * Find element by xpath
   * @param xp the xpath
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def findElemByXPath( xp: String )(implicit pos: Position): Element = {
    try {
      new Element(findElement(By.xpath(xp)))
    } catch {
      case x: org.openqa.selenium.NoSuchElementException =>
        fail(s"""${pos.line} Did not find Element with xpath query "${xp}", page created ${pageCreated.line}""")
        throw x
    }
  }

  /**
   * Find element by xpath
   * @param xp the xpath
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the fields were not found
   */
  def getElemByXPath( xp: String )(implicit patienceConfig: PatienceConfig, pos: Position): Element = {
    eventually { findElemByXPath(xp) }
  }

  /**
   * Find element by ID
   * @param id
   * @return the <code>Element</code>
   * @throws TestFailedException if any of the fields were not found
   */
  def findElemById( id: String )(implicit patienceConfig: PatienceConfig, pos: Position): Element = {
    try {
      new Element(webDriver.findElement(By.id(id)))
    } catch {
      case x: org.openqa.selenium.NoSuchElementException =>
        fail(s"""Unable to find element with id ${id}""")
    }
  }

  def getElemById( id: String )(implicit patienceConfig: PatienceConfig, pos: Position): Element = eventually {
    findElemById(id)
  }

  /**
   * Find all elements by xpath
   * @param xp the xpath
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code>s selected by this query
   */
  def findElemsByXPath( xp: String )(implicit pos: Position): List[Element] = {
    findElements(By.xpath(xp)).asScala.map{ e => new Element(e) }.toList
  }

  /**
   * Find all elements by xpath
   * @param xp the xpath
   * @param patienceConfig configuration for eventually to get the button
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def getElemsByXPath( xp: String )(implicit patienceConfig: PatienceConfig, pos: Position): List[Element] = {
    eventually { findElemsByXPath(xp) }
  }

  def findCombobox( name: String )(implicit pos: Position) = {
    val el = find( xpath(s"""//input[@name='${name}']""") )
    new Combobox(el.underlying)
  }

  def getCombobox( name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually { findCombobox(name) }
  }

  def findCheckbox( name: String )(implicit pos: Position) = {
    val el = find( xpath(s"""//input[@name='${name}']""") )
    new Checkbox(el.underlying)
  }

  def getCheckbox( name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually { findCheckbox(name) }
  }

  def findDateTimePicker( name: String )(implicit pos: Position) = {
    val el = find( xpath(s"""//input[@name='${name}']""") )
    new DateTimePicker(el.underlying)
  }

  def getDateTimePicker( name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually { findDateTimePicker(name) }
  }

  def findRadioButton( name: String )(implicit pos: Position) = {
    val el = find( xpath(s"""//input[@name='${name}']""") )
    new RadioButton(el.underlying)
  }

  def getRadioButton( name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually { findRadioButton(name) }
  }

  def esc(implicit patienceConfig: PatienceConfig, pos: Position): this.type = {
    PageBrowser.esc
    this
  }

  def refresh(implicit patienceConfig: PatienceConfig, pos: Position): this.type = {
    PageBrowser.refresh
    this
  }

  def enter(implicit patienceConfig: PatienceConfig, pos: Position): this.type = {
    PageBrowser.enter
    this
  }

  def currentUrl(implicit patienceConfig: PatienceConfig, pos: Position) = PageBrowser.currentUrl

  def pageTitle(implicit patienceConfig: PatienceConfig, pos: Position) = PageBrowser.pageTitle

  def go(implicit pos: Position) = PageBrowser.go

  def pressKeys(value: String)(implicit pos: Position) = PageBrowser.pressKeys(value)

  def executeScript[T](script: String, args: AnyRef*): AnyRef = PageBrowser.executeScript(script, args:_*)

  /**
   * Take a screenshot
   * @param directory The directory where the screenshot is written to
   * @param filename The name of the file where the screenshot is written to.  It it doesn't end in ".png", then ".png" will be appended.
   */
  def takeScreenshot( directory: String, filename: String )( implicit pos: Position ) = {
    PageBrowser.takeScreenshot(directory, filename)
    this
  }

  /**
   * Take a screenshot.
   * The filename of the screenshot file will be generated from the position object.  filename-linenumber.png
   * @param directory The directory where the screenshot is written to
   */
  def takeScreenshot( directory: String )( implicit pos: Position ) = {
    val filename = pos.lineForFilename
    PageBrowser.takeScreenshot(directory, filename)
    this
  }

  /**
   * Take a screenshot if an exception is thrown by fun.
   * @param directory The directory where the screenshot is written to
   * @param filename The name of the file where the screenshot is written to.  It it doesn't end in ".png", then ".png" will be appended.
   */
  def takeScreenshotOnError[T]( directory: String, filename: String )(fun: => T)( implicit pos: Position ): T = {
    try {
      fun
    } catch {
      case x: Throwable =>
        try {
          PageBrowser.takeScreenshot(directory, filename)
          throw x
        } catch {
          case x2: Exception =>
            if (x != x2) x.addSuppressed(x2)
            throw x
        }
    }
  }

  /**
   * Take a screenshot if an exception is thrown by fun.
   * The filename of the screenshot file will be generated from the position object.  filename-linenumber.png
   * @param directory The directory where the screenshot is written to
   */
  def takeScreenshotOnError[T]( directory: String )(fun: => T)( implicit pos: Position ): T = {
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
  def withClueAndScreenShot[T]( directory: String, filenamePrefix: String, clue: Any)(fun: => T)(implicit pos: Position): T = {
    takeScreenshotOnError(directory, s"${filenamePrefix}_${pos.lineForFilename}") {
      withClue(clue)(fun)
    }
  }

  def getTextNode( e: Element, i: Int ) = {
    val script = """
var parent = arguments[0];
var child = parent.childNodes.item(arguments[1]);
var ret = child.textContent;
return ret;
"""
    val r = executeScript(script, e.underlying, i.asInstanceOf[AnyRef])
    r.asInstanceOf[String]
  }

  def getAllTextNodes( e: Element ) = {
    val script = """
var parent = arguments[0];
var child = parent.firstChild;
var ret = "";
while(child) {
    if (child.nodeType === Node.TEXT_NODE)
        ret += "<"+child.textContent+">";
    child = child.nextSibling;
}
return ret;
"""
    val r = executeScript(script, e.underlying )
    val s = r.asInstanceOf[String]

    val p = """\<([^>]*)\>""".r

    val x = for (m <- p.findAllMatchIn(s)) yield m.group(1)
    x.toList
  }
}
