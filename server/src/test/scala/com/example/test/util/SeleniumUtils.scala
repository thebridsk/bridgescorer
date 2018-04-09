package com.example.test.util

import org.openqa.selenium.WebDriver
import org.scalatest.MustMatchers
import utils.logging.Logger
import java.util.logging.Level
import org.scalactic.source.Position
import org.openqa.selenium.WebElement
import org.openqa.selenium.By.ByName
import scala.collection.convert.ImplicitConversionsToScala._
import org.openqa.selenium.By.ByTagName
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Eventually.PatienceConfig
import com.example.test.pages.PageBrowser

object SeleniumUtilsLogger {

  private[util] val testlog = Logger[SeleniumUtils]

}

trait SeleniumUtils {
  import org.scalatest.selenium.WebBrowser._
  import org.scalatest.MustMatchers._
  import EventuallyUtils._

  import SeleniumUtilsLogger._

  /**
   * Find a button
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text optionally the text that must appear on the button
   * @param buttontype optionally the value of the <code>type</code> attribute of the button.
   *                   if not specified, the value "button" is used.
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def findButton( eid: String,
                  text: Option[String] = None,
                  buttontype: Option[String] = None
                )(implicit webDriver: WebDriver, pos: Position): Element = {
    val bType = buttontype match {
      case Some(s) => s
      case None => "button"
    }
    try {
        find(id(eid)) match {
        case Some(button) =>
          withClue(s"${pos.fileName}:${pos.lineNumber} And has a button with id $eid") { button.tagName mustBe "button" }
          withClue(s"${pos.fileName}:${pos.lineNumber} And has a button with id $eid and type $bType") { button.attribute("type").get mustBe bType }
          text match {
            case Some(t) =>
              withClue(s"${pos.fileName}:${pos.lineNumber} And has a button with id $eid and text of $t") { button.text mustBe t }
              testlog.fine(s"${pos.fileName}:${pos.lineNumber} findButton: found button with id $eid type $bType and text $t")
              button
            case None =>
              testlog.fine(s"${pos.fileName}:${pos.lineNumber} findButton: found button with id $eid type $bType")
              button
          }
        case None =>
          fail(s"${pos.fileName}:${pos.lineNumber} Element with id $eid was not found")
      }
    } catch {
      case x: Throwable =>
        testlog.fine(s"${pos.fileName}:${pos.lineNumber} findButton: exception "+x.toString(), x)
        throw x
    }
  }

  /**
   * Find a button with a type of button
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text the text that must appear on the button
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def findButton( eid: String, text: String )(implicit webDriver: WebDriver, pos: Position): Element = {
    findButton(eid, Some(text))
  }

  /**
   * Get all the buttons that match.
   * @param ids the ids of the buttons to return
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @return a map of id -> Element
   * @throws TestFailedException if all the buttons are not found
   */
  def findButtons( ids: String* )(implicit webDriver: WebDriver, pos: Position): Map[String,Element] = {
    val buttons = findAll(tagName("button")).flatMap { b =>
      b.attribute("id") match {
        case Some(s) if ids.contains(s) => (s,b)::Nil
        case _ => Nil
      }
    }.toMap
    withClue(s"${pos.fileName}:${pos.lineNumber} findButtons($ids.toList)") {
      buttons.size mustBe ids.size
    }
    buttons
  }

  /**
   * Get all the buttons that match.  The text must match.
   * @param ids Map[String,String]  id->text
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @throws TestFailedException if all the buttons are not found or any of the texts don't match
   */
  def findButtons( map: Map[String,String] )(implicit webDriver: WebDriver, pos: Position): Map[String,Element] = {
    val buttons = findButtons( map.keySet.toList :_* )

    buttons.foreach{ case (id,e) => map.get(id) match {
      case Some(t) => e.text mustBe t
      case None =>
    }}
    buttons
  }

  /**
   * Get all the buttons that have an id
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   */
  def findAllButtons()(implicit webDriver: WebDriver): Map[String,Element] = {
    findAll(tagName("button")).flatMap { b =>
      b.attribute("id") match {
        case Some(s) => (s,b)::Nil
        case _ => Nil
      }
    }.toMap
  }

  /**
   * Eventually find a button
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text optionally the text that must appear on the button
   * @param buttontype optionally the value of the <code>type</code> attribute of the button.
   *                   if not specified, the value "button" is used.
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def eventuallyFindButton( eid: String,
                  text: Option[String] = None,
                  buttontype: Option[String] = None
                )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Element = {
    eventually { findButton(eid,text,buttontype) }
  }

  /**
   * Eventually find a button with a type of button
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text the text that must appear on the button
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def eventuallyFindButton( eid: String, text: String )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Element = {
    eventuallyFindButton(eid, Some(text))
  }

  /**
   * Eventually get all the buttons that match.
   * @param ids the ids of the buttons to return
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @return a map of id -> Element
   * @throws TestFailedException if all the buttons are not found
   */
  def eventuallyFindButtons( ids: String* )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Map[String,Element] = {
    eventually( findButtons(ids:_*) )
  }

  /**
   * Eventually get all the buttons that match.  The text must match.
   * @param ids Map[String,String]  id->text
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @throws TestFailedException if all the buttons are not found or any of the texts don't match
   */
  def eventuallyFindButtons( map: Map[String,String] )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Map[String,Element] = {
    eventually( findButtons(map) )
  }

  /**
   * Find a button
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text optionally the text that must appear on the button
   * @param buttontype optionally the value of the <code>type</code> attribute of the button.
   *                   if not specified, the value "button" is used.
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def eventuallyFindAndClickButton( id: String, text: Option[String] = None )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position ): Unit = {
    eventually( click on findButton( id, text ) )
  }

  def eventuallyFindAndClickButton( id: String, text: String )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position ): Unit =
    eventuallyFindAndClickButton(id, Some(text))

  /**
   * Find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def findInput( iname: String, itype: String )(implicit webDriver: WebDriver, pos: Position): TextField = {
    try {
      val input = webDriver.findElement(new ByName(iname))
      withClue("And has an input field with name "+iname) { input.getTagName mustBe "input" }
      withClue("And has an input field with name "+iname+" and with type "+itype) { input.getAttribute("type") mustBe itype }
      new TextField(input)(pos)
    } catch {
      case x: Throwable =>
        testlog.fine ("findInput: exception "+x.toString(), x)
//        x.printStackTrace(System.out)
        throw x
    }
  }

  /**
   * Find all input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def findAllInput( itype: String, iname: String* )(implicit webDriver: WebDriver, pos: Position): Map[String,TextField] = {
    try {
      val input = webDriver.findElements(new ByTagName("input")).
                    filter{ we => we.getAttribute("type") == itype && iname.contains(we.getAttribute("name")) }.
                    map { we => (we.getAttribute("name"), new TextField(we)(pos)) }.
                    toMap
      withClue(s"""${pos.fileName}:${pos.lineNumber} findAllInput(${itype},${iname.toList})""") {
        input.size mustBe iname.size
      }
      input
    } catch {
      case x: Throwable =>
        testlog.fine (s"${pos.fileName}:${pos.lineNumber} findAllInput: exception "+x.toString(), x)
//        x.printStackTrace(System.out)
        throw x
    }
  }

  /**
   * eventually find all input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def eventuallyFindAllInput( itype: String, iname: String* )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Map[String,TextField] = {
    eventually( findAllInput(itype,iname:_*)(webDriver,pos) )
  }

  /**
   * Eventually find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def eventuallyFindInput( iname: String, itype: String )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position ): TextField = {
    eventually( findInput(iname,itype)(webDriver,pos) )
  }

  /**
   * Eventually find an input field
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
   *          <code>interval</code> parameters
   * @param pos the filename and line number of where it is called from.
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the input was not found or if the specified <code>itype</code> don't match or if the element tag is not <code>input</code>
   */
  def eventuallyFindElem( id: String )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position ): Element = {
    eventually( findElem(id)(webDriver,pos) )
  }

  /**
   * Eventually find an input field and send some data
   * @param iname the value of the <code>name</code> attribute of the button
   * @param itype the value of the <code>type</code> attribute of the input
   * @param data the keys to send
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if any of the input fields were not found
   */
  def findInputAndSendKeys( iname: String, itype: String, data: String )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {
    eventuallyFindInput(iname,itype).value = data
  }


  /**
   * Find a button and click it
   * @param eid the value of the <code>id</code> attribute of the button
   * @param text optionally the text that must appear on the button
   * @param buttontype optionally the value of the <code>type</code> attribute of the button.
   *                   if not specified, the value "button" is used.
   * @param webDriver the <code>WebDriver</code> with which to drive the browser
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the button was not found or if the specified <code>text</code> or <code>buttontype</code> don't match or if the element tag is not <code>button</code>
   */
  def findButtonAndClick( eid: String,
                          text: Option[String] = None,
                          buttontype: Option[String] = None
                        )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {
    click on eventually {
      val b = findButton(eid,text,buttontype)
      PageBrowser.scrollToElement(b)
      if (!b.isEnabled) throw new NoResultYet
      b
    }
  }

  def findButtonAndClick( eid: String,
                          text: String
                        )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit =
    findButtonAndClick(eid,Some(text),None)

  /**
   * Find an element by id
   * @param id
   * @return the <code>Element</code> selected by this query
   * @throws TestFailedException if the element was not found
   */
  def findElem( eid: String )(implicit webDriver: WebDriver, pos: Position): Element = {
    find(id(eid)) match {
      case Some(e) => e
      case None => fail(s"${pos.fileName}:${pos.lineNumber} Did not find Element with id $eid")
    }
  }

  def getElemByXPath( xp: String )(implicit webDriver: WebDriver, pos: Position): Option[Element] = {
    find(xpath(xp))
  }

  def findElemByXPath( xp: String )(implicit webDriver: WebDriver, pos: Position): Element = {
    find(xpath(xp)) match {
      case Some(e) => e
      case None => fail(s"""${pos.fileName}:${pos.lineNumber} Did not find Element with xpath query "${xp}"""")
    }
  }

  def findElemsByXPath( xp: String )(implicit webDriver: WebDriver): List[Element] = {
    findAll(xpath(xp)).toList

  }
}

object SeleniumUtils extends SeleniumUtils

