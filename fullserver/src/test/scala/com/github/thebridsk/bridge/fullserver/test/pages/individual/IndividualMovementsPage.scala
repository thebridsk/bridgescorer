package com.github.thebridsk.bridge.fullserver.test.pages.individual

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import java.net.URL
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ListDuplicatePage
import com.github.thebridsk.browserpages.GenericPage

object IndividualMovementsPage {

  val log: Logger = Logger[IndividualMovementsPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): IndividualMovementsPage = {
    val curMov = getCurrentMovement
    val gp = GenericPage.current
    val playI = gp.findButton(buttonPlayIndividual).containsClass("baseButtonSelected")
    new IndividualMovementsPage(curMov, playI)
  }

  def urlFor(movement: Option[String] = None): String =
    TestServer.getAppPageUrl(
      "individual/movements" + movement.map { m => s"/${m}" }.getOrElse("")
    )
  def restUrlFor(movement: String): URL =
    TestServer.getUrl(s"/v1/rest/individualmovements/${movement}")

  def goto(movement: String = null)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): IndividualMovementsPage = {
    val mov = Option(movement)
    go to urlFor(Option(movement))
    new IndividualMovementsPage(mov, false)
  }

  val movementsIndividual: List[String] =
    "Individual2Tables21Boards" :: Nil
  val movementsTeams: List[String] =
    "2TablesArmonk" :: Nil

  def getMovement(movement: String): Option[IndividualMovement] = {
    import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
    val ResponseFromHttp(status, loc, ce, mov, cd) =
      HttpUtils.getHttpObject[IndividualMovement](restUrlFor(movement))
    mov
  }

  def getBoardsFromMovement(movement: String): List[Int] = {
    getMovement(movement) match {
      case Some(mov) =>
        mov.hands.flatMap(hit => hit.boards).distinct.sorted
      case None =>
        fail(s"Unable to get movement $movement")
    }
  }

  def getRoundsFromMovement(movement: String): List[Int] = {
    getMovement(movement) match {
      case Some(mov) =>
        mov.hands.map(hit => hit.round).distinct.sorted
      case None =>
        fail(s"Unable to get movement $movement")
    }
  }

  def getCurrentMovement(implicit
      webDriver: WebDriver,
      pos: Position
  ): Option[String] = {
    val prefix = urlFor()
    eventually {
      val cur = currentUrl
      cur must startWith(prefix)
      if (cur == prefix) None
      else {
        val s = prefix.drop(cur.length())
        if (s.charAt(0) == '/') Some(s.drop(1))
        else fail(s"${pos.line} URL is not valid for IndividualMovementsPage: $cur")
      }
    }
  }

  val buttonOK = "OK"
  val buttonPlayIndividual = "PlayIndividual"
  val buttonPlayTeams = "PlayTeams"

  val buttons = buttonOK::buttonPlayIndividual::buttonPlayTeams::Nil
}

class IndividualMovementsPage(
    movement: Option[String],
    playIndividual: Boolean
)(implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[IndividualMovementsPage] {
  import IndividualMovementsPage._

  private def getMovements = if (playIndividual) movementsIndividual else movementsTeams

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): IndividualMovementsPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {
        currentUrl mustBe urlFor(movement)

        findButton(buttonPlayIndividual).containsClass("baseButtonSelected") mustBe playIndividual
        findButton(buttonPlayTeams).containsClass("baseButtonSelected") mustBe !playIndividual

        findButtons("OK" :: getMovements: _*)
        this
      }
    }

  def clickPlayIndividual(implicit patienceConfig: PatienceConfig, pos: Position): IndividualMovementsPage = {
    if (playIndividual) throw new Exception("Already in play individual")
    clickButton(buttonPlayIndividual)
    new IndividualMovementsPage(None, true)
  }

  def clickPlayTeams(implicit patienceConfig: PatienceConfig, pos: Position): IndividualMovementsPage = {
    if (!playIndividual) throw new Exception("Already in play teams")
    clickButton(buttonPlayTeams)
    new IndividualMovementsPage(None, false)
  }

  def click(
      mov: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): IndividualMovementsPage = {
    if (!getMovements.contains(mov)) log.warning(s"Unknown movement mov")

    clickButton(mov)
    new IndividualMovementsPage(Option(mov), playIndividual)
  }

  def clickOK(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    clickButton("OK")
    new ListDuplicatePage(None)
  }

}
