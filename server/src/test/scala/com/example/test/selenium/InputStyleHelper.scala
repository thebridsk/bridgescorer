package com.example.test.selenium

import org.scalatest.MustMatchers
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Firefox
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import selenium._
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually._
import org.scalactic.source.Position
import scala.concurrent._
import scala.concurrent.duration._
import utils.logging.Logger
import java.util.logging.Level
import com.example.test.util.NoResultYet
import com.example.test.util.MonitorTCP
import com.example.pages.Element
import org.scalatest.time.Millis
import org.scalatest.time.Span


class InputStyleHelper extends MustMatchers {
    import InputStyleHelper._
    import com.example.pages.PageBrowser._
    import com.example.test.util.EventuallyUtils._

  def getButton( id: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig) = {
     eventuallySome{ findButton(id) }
  }

  def isButton( button: String, text: String ) = {
    "Input Style: "+text == button
  }

  /**
   * Hit the "InputStyle" button until <i>wantInputStyleText</i> appears in the button text
   */
  def hitInputStyleButton( wantInputStyleText: String )( implicit webDriver: WebDriver, patienceConfig: PatienceConfig ): Unit = {
    val rid = "InputStyle"

    var found = false
    var i = 2     // ComponentInputStyleButton.InputMethod.values.size
    while (!found && i > 0) {
      i=i-1
      val button = getButton(rid)
      val buttontext = button.text
      testlog.fine(s"HitInputStyleButton ${i}: Want ${wantInputStyleText}, have ${buttontext}")
      if (isButton(buttontext, wantInputStyleText)) {
        found = true
      } else {

        click on button

        eventuallyTrue {
          findButton(rid) match {
            case Some(b) =>
              val t = b.text
              if (t != buttontext) {
                found = isButton(t,wantInputStyleText)
                true
              }
              else false
            case None =>
              false
          }
        }
      }
    }

    if (!found) {
      fail("did not find button: "+rid)
    }

  }

  def findButton( eid: String, text: Option[String] = None, buttontype: Option[String] = None )(implicit webDriver: WebDriver): Option[Element] = {
    val bType = buttontype match {
      case Some(s) => s
      case None => "button"
    }
    try {
      val button = find(id(eid))
      withClue("And has a "+eid+" input field") { button.tagName mustBe "button" }
      withClue("And has a "+eid+" button") { button.attribute("type").get mustBe bType }
      text match {
        case Some(t) =>
          withClue("And has a "+eid+" button with text of Duplicate") { button.text mustBe t }
          testlog.fine ("findButton: found "+eid+" with text "+text+" "+button)
          Some(button)
        case None =>
          testlog.fine ("findButton: found "+eid+" "+button)
          Some(button)
      }
    } catch {
      case x: Throwable =>
        testlog.fine ("findbutton: exception "+x.toString(), x)
//        x.printStackTrace(System.out)
        throw x
    }
  }

  /**
   * Get all the buttons that match.
   * @param ids the ids of the buttons to return
   */
  def findButtons( ids: String* )(implicit webDriver: WebDriver): Map[String,Element] = {
    val buttons = findAll(tagName("button")).flatMap { b =>
      b.attribute("id") match {
        case Some(s) if ids.contains(s) => (s,b)::Nil
        case _ => Nil
      }
    }.toMap
    buttons.size mustBe ids.size
    buttons
  }

  def findInput( nam: String, itype: String )(implicit webDriver: WebDriver): Option[Element] = {
    try {
      val input = find(name(nam))
      withClue("And has an input field with name "+nam) { input.tagName mustBe "input" }
      withClue("And has an input field "+nam+" is of type "+itype) { input.attribute("type").get mustBe itype }
      Some(input)
    } catch {
      case x: Throwable =>
        testlog.fine ("findInput: exception "+x.toString(), x)
//        x.printStackTrace(System.out)
        throw x
    }
  }

  def findButton( eid: String, text: String )(implicit webDriver: WebDriver): Option[Element] = {
    findButton(eid, Some(text))
  }

}

object InputStyleHelper extends InputStyleHelper {

  val testlog = Logger[InputStyleHelper]

}
