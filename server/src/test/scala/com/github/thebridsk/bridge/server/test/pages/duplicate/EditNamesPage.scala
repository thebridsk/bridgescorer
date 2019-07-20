package com.github.thebridsk.bridge.server.test.pages.duplicate

import com.github.thebridsk.bridge.server.test.pages.Page
import com.github.thebridsk.bridge.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.bridge.server.test.pages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import java.net.URL
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import com.github.thebridsk.bridge.data.{ Team => PTeam }

object EditNamesPage {

  val log = Logger[EditNamesPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new EditNamesPage( getCurrentDupId )
  }

  def urlFor( dupid: String ) = TestServer.getAppPageUrl(s"duplicate/${dupid}/names")
  def restUrlFor( dupid: String, teamId: String ) = TestServer.getUrl(s"/v1/rest/duplicate/${dupid}/teams/${teamId}")

  def goto( dupid: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(dupid)
    new EditNamesPage(dupid)
  }

  def getTeam( dupid: String, teamid: String ): Option[PTeam] = {
    import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
    val ResponseFromHttp(status,loc,ce,mov,cd) = HttpUtils.getHttpObject[PTeam](restUrlFor(dupid,teamid))
    mov
  }

  val patternURL = """(M\d+)/names""".r

  def getCurrentDupId( implicit webDriver: WebDriver, pos: Position ): String = {
    val prefix = TestServer.getAppPageUrl("duplicate/")
    eventually {
      val cur = currentUrl
      cur must startWith( prefix )
      if (cur == prefix) fail(s"${pos.line} URL is not valid for EditNamesPage: $cur")
      else {
        val s = prefix.drop(cur.length())
        s match {
          case patternURL(dupid) => dupid
          case _ => fail(s"${pos.line} URL is not valid for EditNamesPage: $cur")
        }
      }
    }
  }

  val buttonOK = "OK"
  val buttons = buttonOK::"Reset"::"Cancel"::Nil
}

class EditNamesPage(
                     val dupid: String
                   )( implicit
                       webDriver: WebDriver,
                       pageCreated: SourcePosition
                   ) extends Page[EditNamesPage] {
  import EditNamesPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {
    currentUrl mustBe urlFor( dupid )

    findButtons( buttons: _* )
    this
  }}

  def isOKEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonOK).isEnabled
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonOK)
    new ScoreboardPage(Some(dupid))
  }

  def clickReset(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Reset")
    this
  }

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Cancel")
    new ScoreboardPage(Some(dupid))
  }

  /**
   * Returns a table of names,
   * The first index is the row is the team number -1.
   * The columns (second index) are the players on the team.
   */
  def getNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' dupDivNamesPage ')]/table/tbody/tr/td/div/div/input""").map { e =>
      e.attribute("value").getOrElse("")
    }.grouped(2).toList
  }

  /**
   *  @param players must specify an even number of players.  The number of players is the number of teams times two
   */
  def checkNames( players: String * )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val p = players.grouped(2).toList
    p mustBe getNames
    this
  }

  /**
   * @param teamId - 1, 2, 3, 4
   * @param player - 1, 2
   * @param name
   */
  def checkName( teamid: Int, player: Int, name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val names = getNames
    names(teamid-1)(player-1) mustBe name
    this
  }

  /**
   * @param teamId - 1, 2, 3, 4
   * @param player - 1, 2
   * @param name
   */
  def setName( teamId: Int, player: Int, name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val text = eventually {
      getTextInput(s"I_T${teamId}_${player}")
    }
    text.value = name
    this
  }
}
