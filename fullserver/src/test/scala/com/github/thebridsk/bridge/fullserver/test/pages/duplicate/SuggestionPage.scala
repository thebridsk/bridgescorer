package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.browserpages.TextField
import com.github.thebridsk.browserpages.Checkbox

object SuggestionPage {

  val log: Logger = Logger[SuggestionPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    val pt: PageType =
      if (findElem[Element](id("Clear")).isDisplayed) {
        if (findElem[Element](id("NeverPair")).isDisplayed) {
          if (findElem[Element](id("ToggleDetails")).isDisplayed) {
            ResultWithoutNeverPair
          } else {
            AllPlayers
          }
        } else {
          NotAllPlayers
        }
      } else {
        if (findElem[Element](id("ToggleDetails")).isDisplayed) {
          ResultWithNeverPair
        } else {
          NeverPair
        }
      }

    new SuggestionPage(pt)
  }

  def urlFor(): String = TestServer.getAppPageUrl("duplicate/suggestion")

  def goto()(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    go to urlFor()
    new SuggestionPage(NotAllPlayers)
  }

  sealed trait PageType
  case object NotAllPlayers extends PageType
  case object AllPlayers extends PageType
  case object NeverPair extends PageType
  case object ResultWithoutNeverPair extends PageType
  case object ResultWithNeverPair extends PageType

  val buttonsNotAllPlayers: List[String] = "Clear" ::
    "Cancel" ::
    Nil

  val buttonsAllPlayers: List[String] = "Calculate" ::
    "CalculateLocal" ::
    "Clear" ::
    "NeverPair" ::
    "Cancel" ::
    Nil

  val buttonsNeverPair: List[String] = "Calculate" ::
    "CalculateLocal" ::
    "NeverPair" ::
    "ClearNeverPair" ::
    "CancelNeverPair" ::
    "Cancel" ::
    Nil

  val buttonsResults = "ToggleDetails"

  def buttonsResultWithNeverPair: List[String] =
    buttonsResults :: buttonsNeverPair
  def buttonsResultWithoutNeverPair: List[String] =
    buttonsResults :: buttonsAllPlayers
}

class SuggestionPage(
    val pageType: SuggestionPage.PageType = SuggestionPage.NotAllPlayers
)(implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[SuggestionPage] {
  import SuggestionPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {
        currentUrl mustBe urlFor()

        val buttons = pageType match {
          case NotAllPlayers          => buttonsNotAllPlayers
          case AllPlayers             => buttonsAllPlayers
          case NeverPair              => buttonsNeverPair
          case ResultWithoutNeverPair => buttonsResultWithoutNeverPair
          case ResultWithNeverPair    => buttonsResultWithNeverPair
        }

        findButtons(buttons: _*)
        this
      }
    }

  def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    clickButton("Cancel")
    new ListDuplicatePage(None)
  }

  def clickCalculate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    val npt = pageType match {
      case AllPlayers => ResultWithoutNeverPair
      case NeverPair  => ResultWithNeverPair
      case _ =>
        fail(s"Clicking calculation when pagetype is $pageType")
    }
    clickButton("Calculate")
    new SuggestionPage(npt)
  }

  def isCalculateEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    findButton("Calculate").isEnabled
  }

  def clickCalculateLocal(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    val npt = pageType match {
      case AllPlayers => ResultWithoutNeverPair
      case NeverPair  => ResultWithNeverPair
      case _ =>
        fail(s"Clicking calculation when pagetype is $pageType")
    }
    clickButton("CalculateLocal")
    new SuggestionPage(npt)
  }

  def clickClear(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    clickButton("Clear")
    new SuggestionPage(NotAllPlayers)
  }

  def clickNeverPair(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    pageType mustBe AllPlayers
    clickButton("NeverPair")
    new SuggestionPage(NeverPair)
  }

  def clickClearNeverPair(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    NeverPair :: ResultWithNeverPair :: Nil must contain(pageType)
    clickButton("ClearNeverPair")
    new SuggestionPage(pageType)
  }

  def clickCancelNeverPair(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    NeverPair :: ResultWithNeverPair :: Nil must contain(pageType)
    clickButton("ClearNeverPair")
    if (pageType == NeverPair) {
      new SuggestionPage(AllPlayers)
    } else {
      new SuggestionPage(ResultWithoutNeverPair)
    }
  }

  def toggleDetails(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    ResultWithoutNeverPair :: ResultWithNeverPair :: Nil must contain(pageType)
    clickButton("ToggleDetails")
    this
  }

  def isDetail(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    ResultWithoutNeverPair :: ResultWithNeverPair :: Nil must contain(pageType)
    findButton("ToggleDetails").text == "Hide Details"
  }

  def getNumberChecked(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Int = {
    Checkbox.findAllChecked().length
  }

  private def getCheckboxes(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ) = {
    Checkbox.findAll()
  }

  def getNumberKnownNames(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Int = {
    getCheckboxes.length
  }

  def getKnownNames(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[String] = {
    getCheckboxes.map(e => e.label.text.trim)
  }

  def isKnownNameChecked(
      name: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    Checkbox.find(name).isSelected
  }

  private def namesChanged(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ) = {
    Thread.sleep(5)
    if (isCalculateEnabled) {
      new SuggestionPage(AllPlayers)
    } else {
      new SuggestionPage(NotAllPlayers)
    }

  }

  def toggleKnownName(
      n: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): SuggestionPage = {
    findElem(id(s"KP$n")).click
    namesChanged
  }

  private def getNameFieldsElements(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ) = {
    findAllElems[Element](
      xpath(
        """//div[contains(concat(' ', @class, ' '), ' dupDivSuggestionPage ')]/div[3]/ul/li/input"""
      )
    )
  }

  def getNumberNameFields(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Int = {
    getNameFieldsElements.length
  }

  def getNameFields(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[Option[String]] = {
    getNameFieldsElements.map(e => e.attribute("value"))
  }

  def setNameField(n: Int, name: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SuggestionPage = {
    new TextField(findInput(s"NP$n", "text")).value = name
    namesChanged
  }

  def getNeverPairTableNames(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[String] = {
    findAllElems[Element](
      xpath(
        """//div[contains(concat(' ', @class, ' '), ' dupDivSuggestionPage ')]/div[4]/table/thead/tr[1]/th"""
      )
    ).drop(1).map(e => e.text)
  }
}
