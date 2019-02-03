package com.example.test.pages.duplicate

import com.example.test.pages.Page
import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.test.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.test.util.HttpUtils
import com.example.data.BoardSet
import com.example.data.Movement
import java.net.URL
import com.example.test.util.HttpUtils.ResponseFromHttp
import com.example.test.pages.Element
import com.example.test.pages.TextField

object SuggestionPage {

  val log = Logger[SuggestionPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val pt: PageType =
    if (  findElem[Element]( id("Clear") ).isDisplayed ) {
      if (  findElem[Element]( id("NeverPair") ).isDisplayed ) {
        if (  findElem[Element]( id("ToggleDetails") ).isDisplayed ) {
          ResultWithoutNeverPair
        } else {
          AllPlayers
        }
      } else {
        NotAllPlayers
      }
    } else {
      if (  findElem[Element]( id("ToggleDetails") ).isDisplayed ) {
        ResultWithNeverPair
      } else {
        NeverPair
      }
    }

    new SuggestionPage( pt  )
  }

  def urlFor() = TestServer.getAppPageUrl("duplicate/suggestion" )

  def goto()(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor()
    new SuggestionPage(NotAllPlayers)
  }

  sealed trait PageType
  case object NotAllPlayers extends PageType
  case object AllPlayers extends PageType
  case object NeverPair extends PageType
  case object ResultWithoutNeverPair extends PageType
  case object ResultWithNeverPair extends PageType

  val buttonsNotAllPlayers = "Clear"::
                             "Cancel"::
                             Nil

  val buttonsAllPlayers = "Calculate"::
                          "CalculateLocal"::
                          "Clear"::
                          "NeverPair"::
                          "Cancel"::
                          Nil

  val buttonsNeverPair = "Calculate"::
                         "CalculateLocal"::
                         "NeverPair"::
                         "ClearNeverPair"::
                         "CancelNeverPair"::
                         "Cancel"::
                         Nil

   val buttonsResults = "ToggleDetails"

   def buttonsResultWithNeverPair = buttonsResults::buttonsNeverPair
   def buttonsResultWithoutNeverPair = buttonsResults::buttonsAllPlayers
}

class SuggestionPage(
                      val pageType: SuggestionPage.PageType = SuggestionPage.NotAllPlayers
                   )( implicit
                       webDriver: WebDriver,
                       pageCreated: SourcePosition
                   ) extends Page[SuggestionPage] {
  import SuggestionPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {
    currentUrl mustBe urlFor()

    val buttons = pageType match {
      case NotAllPlayers => buttonsNotAllPlayers
      case AllPlayers => buttonsAllPlayers
      case NeverPair => buttonsNeverPair
      case ResultWithoutNeverPair => buttonsResultWithoutNeverPair
      case ResultWithNeverPair => buttonsResultWithNeverPair
    }

    findButtons( buttons: _* )
    this
  }}

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Cancel")
    new ListDuplicatePage(None)
  }

  def clickCalculate(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val npt = pageType match {
      case AllPlayers => ResultWithoutNeverPair
      case NeverPair => ResultWithNeverPair
      case _ =>
        fail( s"Clicking calculation when pagetype is $pageType" )
    }
    clickButton("Calculate")
    new SuggestionPage(npt)
  }

  def isCalculateEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findButton("Calculate").isEnabled
  }

  def clickCalculateLocal(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val npt = pageType match {
      case AllPlayers => ResultWithoutNeverPair
      case NeverPair => ResultWithNeverPair
      case _ =>
        fail( s"Clicking calculation when pagetype is $pageType" )
    }
    clickButton("CalculateLocal")
    new SuggestionPage(npt)
  }

  def clickClear(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Clear")
    new SuggestionPage(NotAllPlayers)
  }

  def clickNeverPair(implicit patienceConfig: PatienceConfig, pos: Position) = {
    pageType mustBe AllPlayers
    clickButton("NeverPair")
    new SuggestionPage(NeverPair)
  }

  def clickClearNeverPair(implicit patienceConfig: PatienceConfig, pos: Position) = {
    NeverPair::ResultWithNeverPair::Nil must contain( pageType )
    clickButton("ClearNeverPair")
    new SuggestionPage(pageType)
  }

  def clickCancelNeverPair(implicit patienceConfig: PatienceConfig, pos: Position) = {
    NeverPair::ResultWithNeverPair::Nil must contain( pageType )
    clickButton("ClearNeverPair")
    if (pageType == NeverPair) {
      new SuggestionPage(AllPlayers)
    } else {
      new SuggestionPage(ResultWithoutNeverPair)
    }
  }

  def toggleDetails(implicit patienceConfig: PatienceConfig, pos: Position) = {
    ResultWithoutNeverPair::ResultWithNeverPair::Nil must contain( pageType )
    clickButton("ToggleDetails")
    this
  }

  def isDetail(implicit patienceConfig: PatienceConfig, pos: Position) = {
    ResultWithoutNeverPair::ResultWithNeverPair::Nil must contain( pageType )
    findButton("ToggleDetails").text == "Hide Details"
  }

  def getNumberChecked(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findAllElems[Element]( xpath( """//div[contains(concat(' ', @class, ' '), ' dupDivSuggestionPage ')]/div[4]/ul/li/label/input""") ).
      map( e => e.attribute("checked").map( s => s.toBoolean ).getOrElse(false) ).filter( b => b ).length
  }

  private def getCheckboxLabels(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findAllElems[Element]( xpath( """//div[contains(concat(' ', @class, ' '), ' dupDivSuggestionPage ')]/div[4]/ul/li/label""") )
  }

  def getNumberKnownNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getCheckboxLabels.length
  }

  def getKnownNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getCheckboxLabels.map( e => e.text.trim )
  }

  def isKnownNameChecked( n: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findElem[Element]( id( s"KP$n" ) ).attribute("checked").map( s => s.toBoolean ).getOrElse(false)
  }


  private def namesChanged(implicit patienceConfig: PatienceConfig, pos: Position) = {
    Thread.sleep(5)
    if (isCalculateEnabled) {
      new SuggestionPage(AllPlayers)
    } else {
      new SuggestionPage(NotAllPlayers)
    }

  }

  def toggleKnownName( n: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findElem[Element]( id( s"KP$n" ) ).click
    namesChanged
  }

  private def getNameFieldsElements(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findAllElems[Element]( xpath( """//div[contains(concat(' ', @class, ' '), ' dupDivSuggestionPage ')]/div[5]/ul/li/input""") )
  }

  def getNumberNameFields(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getNameFieldsElements.length
  }

  def getNameFields(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getNameFieldsElements.map( e => e.attribute("value") )
  }

  def setNameField( n: Int, name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    new TextField( findInput(s"NP$n", "text") ).value = name
    namesChanged
  }

  def getNeverPairTableNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findAllElems[Element]( xpath( """//div[contains(concat(' ', @class, ' '), ' dupDivSuggestionPage ')]/div[6]/table/thead/tr[1]/th""") ).
      drop(1).map( e => e.text )
  }
}
