package com.example.pages.duplicate

import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.data.util.Strings
import com.example.pages.Page

object DuplicateResultPage {

  val log = Logger[DuplicateResultPage]

  case class PlaceEntry( place: Int, points: Double, teams: List[Team] ) {
    def pointsAsString = {
      BoardPage.toPointsString(points)
    }
  }

  val patternPoints = s"""(\\d+)(${Strings.half})?""".r

  def parsePoints( s: String ) = {
    s match {
      case patternPoints( n, f ) =>
        n.toDouble + (if (f==Strings.half) 0.5 else 0.0)
      case _ =>
        fail( s"Unable to convert points to a double: ${s}" )
    }
  }

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val did = findIds
    new DuplicateResultPage( Option( did ) )
  }

  def waitFor(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val did = eventually { findIds }
    new DuplicateResultPage( Option( did ) )
  }

  def goto( id: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to getUrl(id)
    new DuplicateResultPage( Option(id) )
  }

  def getUrl( id: String ) = {
    TestServer.getAppPageUrl(s"duplicate/results/${id}")
  }

  val buttonIds =
                 ("Summary", "Summary") ::
                 ("Edit", "Edit") ::
                 Nil

  private val patternResult = """(E\d+)""".r

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
      cur must startWith (prefix)
      cur.drop( prefix.length() ) match {
        case patternResult(did) => did
        case _ => fail("Could not determine duplicate result ID")
      }
    }
  }

  val patternPlaceTablePlayersColumn = """(\d+) (.*?) (.*)""".r
}

class DuplicateResultPage(
                           val dupid: Option[String] = None
                         )( implicit
                             webDriver: WebDriver,
                             pageCreated: SourcePosition
                         ) extends Page[DuplicateResultPage] {
  import DuplicateResultPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    val did = eventually { findIds }

    log.info(s"DuplicateResultEditPage.validate: on edit of result ${did}")
    dupid match {
      case Some(d) =>
        withClue( s"Expecting duplicate result for ${dupid}" ) {
          did mustBe d
        }
      case None =>
        // any page here
    }

    findButtons( buttonIds.toMap )

    new DuplicateResultPage( Some(did) )

  }}

  def findPlayed( implicit pos: Position ) = findDateTimePicker("played")

  def clickSummary( implicit pos: Position ) = {
    clickButton("Summary")
    ListDuplicatePage.current
  }

  def clickEdit( implicit pos: Position ) = {
    clickButton("Edit")
    DuplicateResultEditPage.current
  }

  private def getWinnerSetTableElements(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getElemsByXPath("""//table[@id='scoreboardplayers']""")
  }

  /**
   * @return a List of List of Lists.  The outer list is the winner set, the middle list is the rows (place) in the table, the inner list are the columns.
   * The columns are: place, points, players
   */
  def getPlaceTable(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getWinnerSetTableElements.map { ws =>
      ws.findAll( xpath("""./tbody/tr/td""")).map(c=>c.text).grouped(3).toList
    }
  }

  /**
   * @param ws the winner set, zero based
   * @return a List of Lists.  The outer list is the rows (place) in the table, the inner list are the columns.
   * The columns are: place, points, players
   */
  def getPlaceTable(ws: Int)(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val all = getWinnerSetTableElements
    all(ws).findAll( xpath("""./tbody/tr/td""")).map(c=>c.text).grouped(3).toList

  }

  /**
   * @return a map that maps teamid -> winnerset
   */
  private def getWinnerSetMappings( table: List[List[List[String]]] ) = {
    log.info("DuplicateResultPage.getWinnerSetMappings: "+
        table.map { ws =>
          ws.mkString("\n    ", "\n    ", "")
        }.mkString("\n  WinnerSet","\n  WinnerSet","")
        )
    table.zipWithIndex.flatMap { e =>
      val (ws,i) = e
      ws.flatMap { row =>
        patternPlaceTablePlayersColumn.findAllIn(row(2)).matchData.map { m =>
          m.group(1).toInt -> i
        }
      }
    }.toMap
  }

  /**
   * @param ws the winner set, zero based
   * @param team a Tuple4( teamnumber, players, points, boardscores )
   */
  def checkPlaceTable( places: PlaceEntry* )(implicit pos: Position) = {
    val table = getPlaceTable
    val mapping = getWinnerSetMappings(table)
    places.foreach{ pe =>
      val plc = pe.place.toString()
      log.info(s"""looking for place ${pe.place} points ${pe.points} teams ${pe.teams}, plc=${plc}""")
      withClue( s"""working with place ${pe.place} points ${pe.points} teams ${pe.teams}""" ) {
        val ws = mapping( pe.teams.head.teamid )
        table(ws).find { row =>
          val rc = row(0)==plc
            log.info(s"""  rc=${rc} looking for ${plc} matching ${row(0)} trying to match ${row}""")
          rc
        } match {
          case Some(row) =>
            log.info(s"""  checking other fields ${row}""")
            row(1) mustBe pe.pointsAsString
            val players = row(2)
            pe.teams.foreach(team => players must include (team.toStringForPlayers) )
          case None =>
            fail(s"""${pos.line}: Did not find place ${pe.place}""" )
        }
      }
    }
  }

}
