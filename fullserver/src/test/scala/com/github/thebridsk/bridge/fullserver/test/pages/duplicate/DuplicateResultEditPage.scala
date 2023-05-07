package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.DateTimePicker
import scala.util.matching.Regex

object DuplicateResultEditPage {

  val log: Logger = Logger[DuplicateResultEditPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): DuplicateResultEditPage = {
    val did = findIds
    new DuplicateResultEditPage(Option(did))
  }

  def waitFor(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): DuplicateResultEditPage = {
    val did = eventually { findIds }
    new DuplicateResultEditPage(Option(did))
  }

  def goto(id: String)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): DuplicateResultEditPage = {
    go to getUrl(id)
    new DuplicateResultEditPage(Option(id))
  }

  def getUrl(id: String): String = {
    TestServer.getAppPageUrl(s"duplicate/results/${id}/edit")
  }

  val buttonIds: List[(String, String)] =
    ("OK", "OK") ::
      ("Cancel", "Cancel") ::
      Nil

  private val patternResult = """(E\d+)/edit""".r

  /**
    * Get the duplicate ID, the View type, subresource name and id.
    * currentUrl needs to be one of the following:
    *   duplicateresults/dupid
    * @return Tuple4(
    *            dupid
    *            ViewType   - CompletedViewType, DirectorViewType, TableViewType(tableid,roundid)
    *            subresource    - "game" or subresource
    *            Option(subid)
    *          )
    */
  def findIds(implicit webDriver: WebDriver, pos: Position): String = {
    val prefix = TestServer.getAppPageUrl("duplicate/results/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate result ID: ${cur}") {
      cur must startWith(prefix)
      cur.drop(prefix.length()) match {
        case patternResult(did) => did
        case _                  => fail("Could not determine duplicate result ID")
      }
    }
  }

  val patternName: Regex = """P(\d+)T(T\d+)P(.)""".r

  /**
    * @param name the name attribute of an input field.
    * @return determine the winner set and team id and whether it is player 1 or 2 or the points.
    * winner set is zero based.
    */
  def getInputIds(name: String): Option[(Int, String, String)] = {
    name match {
      case patternName(ws, teamid, what) => Some((ws.toInt, teamid, what))
      case _ =>
        None
    }
  }

  /**
    * @param ws the winning set, zero based.
    * @param teamid
    * @param what "1","2", "P" for player 1, 2, or points
    */
  def getInputName(ws: Int, teamid: Int, what: String): String = {
    s"""P${ws}TT${teamid}P${what}"""
  }
}

class DuplicateResultEditPage(
    val dupid: Option[String] = None
)(implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[DuplicateResultEditPage] {
  import DuplicateResultEditPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): DuplicateResultEditPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        val did = eventually { findIds }

        log.info(s"DuplicateResultEditPage.validate: on edit of result ${did}")
        dupid match {
          case Some(d) =>
            withClue(s"Expecting duplicate result for ${dupid}") {
              did mustBe d
            }
          case None =>
          // any page here
        }

        findButtons(buttonIds.toMap)

        new DuplicateResultEditPage(Some(did))
      }
    }

  def findPlayed(implicit pos: Position): DateTimePicker =
    findDateTimePicker("played")

  def getWinnerSets(implicit pos: Position): List[Int] = {
    findAllInputs(Some("text")).keys
      .flatMap { name => getInputIds(name).map(e => e._1) }
      .toList
      .distinct
  }

  def getAllTeams(implicit pos: Position): List[String] = {
    findAllInputs(Some("text")).keys
      .flatMap { name => getInputIds(name).map(e => e._2) }
      .toList
      .distinct
  }

  def getTeams(ws: Int)(implicit pos: Position): List[String] = {
    findAllInputs(Some("text")).keys
      .flatMap { name =>
        getInputIds(name).filter(e => e._1 == ws).map(e => e._2)
      }
      .toList
      .distinct
  }

  /**
    * @param ws the winner set, zero based
    * @param teamid
    * @param player the player, one based
    */
  def getName(ws: Int, teamid: Int, player: Int)(implicit
      pos: Position
  ): String = {
    getTextInputById(getInputName(ws, teamid, player.toString())).value
  }

  /**
    * @param ws the winner set, zero based
    * @param teamid
    * @param player the player, one based
    * @param name
    */
  def setName(ws: Int, teamid: Int, player: Int, name: String)(implicit
      pos: Position
  ): DuplicateResultEditPage = {
    getTextInputById(getInputName(ws, teamid, player.toString())).value = name
    this
  }

  /**
    * @param ws the winner set, zero based
    * @param teamid
    */
  def getPoints(ws: Int, teamid: Int)(implicit pos: Position): Double = {
    val pts = getNumberInput(getInputName(ws, teamid, "P")).value
    try {
      pts.toDouble
    } catch {
      case x: NumberFormatException =>
        log.warning(s"Points for ${ws}/${teamid} was '${pts}'", x)
        throw x
    }
  }

  /**
    * @param ws the winner set, zero based
    * @param teamid
    * @param points
    */
  def setPoints(ws: Int, teamid: Int, points: Double)(implicit
      pos: Position
  ): Unit = {
    getNumberInput(getInputName(ws, teamid, "P")).value = points.toString()
  }

  def isOKEnabled(implicit pos: Position): Boolean = {
    findButton("OK").isEnabled
  }

  def clickOK(implicit pos: Position): DuplicateResultPage = {
    clickButton("OK")
    DuplicateResultPage.current
  }

  def clickCancel(implicit pos: Position): DuplicateResultPage = {
    clickButton("Cancel")
    DuplicateResultPage.current
  }

}
