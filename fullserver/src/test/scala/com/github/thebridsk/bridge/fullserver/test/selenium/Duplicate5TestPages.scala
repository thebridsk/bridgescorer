package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.openqa.selenium.WebDriver
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.server.backend.BridgeService
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.data.bridge._
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import java.util.logging.Level
import org.scalactic.source.Position
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.server.test.util.NoResultYet
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.NewDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.MovementsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardSetsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterScorekeeperPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterMissingNamesPage
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandPage
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.SelectNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.Hands
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableSelectScorekeeperPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.Team
import com.github.thebridsk.browserpages.Page.AnyPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.EnterHand
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.AllHandsInMatch
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandsOnBoard
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.OtherHandNotPlayed
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.OtherHandPlayed
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TeamScoreboard
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandDirectorView
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandCompletedView
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandTableView
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.PlaceEntry
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.PlaceEntry
import java.net.URL
import com.github.thebridsk.bridge.data.MatchDuplicate
import scala.io.Source
import scala.io.Codec
import com.github.thebridsk.utilities.file.FileIO
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PeopleRow
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.MissingNames
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PeopleRowMP
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PeopleRowIMP
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.StatisticsPage
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.fullserver.test.pages.FullscreenAddOn
import com.github.thebridsk.bridge.server.test.util.TestServer

object Duplicate5TestPages {

  val testlog = Logger[Duplicate5TestPages]

  val screenshotDir = "target/screenshots/Duplicate5TestPages"

  val cm = Strings.checkmark
  val bl = ""
  val zr = "0"
  val xx = Strings.xmark
  val half = Strings.half

  val team1 = Team( 1, "Nick", "Sam")
  val team2 = Team( 2, "Ethan", "Wayne")
  val team3 = Team( 3, "Ellen", "Wilma")
  val team4 = Team( 4, "Nora", "Sally")
  val team5 = Team( 5, "Alice", "Andy" )

  val peopleResult2 = List(
                        PeopleRow(team5.one,"50.00%","25.00%","56.25%","100.00","14.0","0","0.00","1","0.50","2","1","1","0","56.25%","18","32"),
                        PeopleRow(team5.two,"0.00%","0.00%","56.25%","16.67","-11.0","0","0.00","0","0.00","2","1","1","0","56.25%","18","32"),
                        PeopleRow(team3.one,"0.00%","0.00%","46.88%","50.00","-1.0","0","0.00","0","0.00","2","1","1","0","46.88%","15","32"),
                        PeopleRow(team2.one,"0.00%","0.00%","50.00%","0.00","-16.0","0","0.00","0","0.00","2","1","1","0","50.00%","16","32"),
                        PeopleRow(team1.one,"50.00%","25.00%","37.50%","100.00","14.0","0","0.00","1","0.50","2","1","1","0","37.50%","12","32"),
                        PeopleRow(team4.one,"50.00%","50.00%","59.38%","16.67","-11.0","1","1.00","0","0.00","2","1","1","0","59.38%","19","32"),
                        PeopleRow(team4.two,"50.00%","50.00%","59.38%","50.00","-1.0","1","1.00","0","0.00","2","1","1","0","59.38%","19","32"),
                        PeopleRow(team1.two,"50.00%","25.00%","37.50%","100.00","14.0","0","0.00","1","0.50","2","1","1","0","37.50%","12","32"),
                        PeopleRow(team2.two,"50.00%","25.00%","50.00%","100.00","14.0","0","0.00","1","0.50","2","1","1","0","50.00%","16","32"),
                        PeopleRow(team3.two,"0.00%","0.00%","46.88%","0.00","-16.0","0","0.00","0","0.00","2","1","1","0","46.88%","15","32")
      )

  val peopleResultMP2 = List(
                        PeopleRowMP(team5.one,"50.00%","50.00%","57.81%","1","1.00","2","0","59.38%","37","64"),
                        PeopleRowMP(team5.two,"0.00%","0.00%","51.56%","0","0.00","2","0","56.25%","33","64"),
                        PeopleRowMP(team3.one,"0.00%","0.00%","48.44%","0","0.00","2","0","50.00%","31","64"),
                        PeopleRowMP(team2.one,"0.00%","0.00%","43.75%","0","0.00","2","0","50.00%","28","64"),
                        PeopleRowMP(team1.one,"0.00%","0.00%","46.88%","0","0.00","2","0","56.25%","30","64"),
                        PeopleRowMP(team4.one,"50.00%","50.00%","53.13%","1","1.00","2","0","59.38%","34","64"),
                        PeopleRowMP(team4.two,"50.00%","50.00%","54.69%","1","1.00","2","0","59.38%","35","64"),
                        PeopleRowMP(team1.two,"50.00%","50.00%","48.44%","1","1.00","2","0","59.38%","31","64"),
                        PeopleRowMP(team2.two,"0.00%","0.00%","53.13%","0","0.00","2","0","56.25%","34","64"),
                        PeopleRowMP(team3.two,"0.00%","0.00%","42.19%","0","0.00","2","0","46.88%","27","64")
      )
  val peopleResultIMP2 = List(
                        PeopleRowIMP(team5.one,"100.00%","50.00%","100.00","14.0","2","1.00","2","0"),
                        PeopleRowIMP(team5.two,"50.00%","25.00%","54.55","1.5","1","0.50","2","0"),
                        PeopleRowIMP(team3.one,"0.00%","0.00%","27.27","-6.0","0","0.00","2","0"),
                        PeopleRowIMP(team2.one,"0.00%","0.00%","18.18","-8.5","0","0.00","2","0"),
                        PeopleRowIMP(team1.one,"50.00%","25.00%","45.45","-1.0","1","0.50","2","0"),
                        PeopleRowIMP(team4.one,"50.00%","25.00%","54.55","1.5","1","0.50","2","0"),
                        PeopleRowIMP(team4.two,"50.00%","25.00%","72.73","6.5","1","0.50","2","0"),
                        PeopleRowIMP(team1.two,"50.00%","25.00%","45.45","-1.0","1","0.50","2","0"),
                        PeopleRowIMP(team2.two,"50.00%","25.00%","72.73","6.5","1","0.50","2","0"),
                        PeopleRowIMP(team3.two,"0.00%","0.00%","0.00","-13.5","0","0.00","2","0")
      )


  val peopleResult = List(
                        PeopleRow(team4.one,"100.00%","100.00%","59.38%","0.00","0.0","1","1.00","0","0.00","1","1","0","0","59.38%","19","32"),
                        PeopleRow(team4.two,"100.00%","100.00%","59.38%","0.00","0.0","1","1.00","0","0.00","1","1","0","0","59.38%","19","32"),
                        PeopleRow(team5.one,"0.00%","0.00%","56.25%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","56.25%","18","32"),
                        PeopleRow(team5.two,"0.00%","0.00%","56.25%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","56.25%","18","32"),
                        PeopleRow(team2.one,"0.00%","0.00%","50.00%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","50.00%","16","32"),
                        PeopleRow(team2.two,"0.00%","0.00%","50.00%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","50.00%","16","32"),
                        PeopleRow(team3.one,"0.00%","0.00%","46.88%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","46.88%","15","32"),
                        PeopleRow(team3.two,"0.00%","0.00%","46.88%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","46.88%","15","32"),
                        PeopleRow(team1.one,"0.00%","0.00%","37.50%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","37.50%","12","32"),
                        PeopleRow(team1.two,"0.00%","0.00%","37.50%","0.00","0.0","0","0.00","0","0.00","1","1","0","0","37.50%","12","32")
      )

  val peopleResultMP = List(
                        PeopleRowMP(team4.one,"100.00%","100.00%","59.38%","1","1.00","1","0","59.38%","19","32"),
                        PeopleRowMP(team4.two,"100.00%","100.00%","59.38%","1","1.00","1","0","59.38%","19","32"),
                        PeopleRowMP(team5.one,"0.00%","0.00%","56.25%","0","0.00","1","0","56.25%","18","32"),
                        PeopleRowMP(team5.two,"0.00%","0.00%","56.25%","0","0.00","1","0","56.25%","18","32"),
                        PeopleRowMP(team2.one,"0.00%","0.00%","50.00%","0","0.00","1","0","50.00%","16","32"),
                        PeopleRowMP(team2.two,"0.00%","0.00%","50.00%","0","0.00","1","0","50.00%","16","32"),
                        PeopleRowMP(team3.one,"0.00%","0.00%","46.88%","0","0.00","1","0","46.88%","15","32"),
                        PeopleRowMP(team3.two,"0.00%","0.00%","46.88%","0","0.00","1","0","46.88%","15","32"),
                        PeopleRowMP(team1.one,"0.00%","0.00%","37.50%","0","0.00","1","0","37.50%","12","32"),
                        PeopleRowMP(team1.two,"0.00%","0.00%","37.50%","0","0.00","1","0","37.50%","12","32")
      )
  val peopleResultIMP = List(
                        PeopleRowIMP(team4.one,"100.00%","50.00%","100.00","14.0","1","0.50","1","0"),
                        PeopleRowIMP(team4.two,"100.00%","50.00%","100.00","14.0","1","0.50","1","0"),
                        PeopleRowIMP(team5.one,"100.00%","50.00%","100.00","14.0","1","0.50","1","0"),
                        PeopleRowIMP(team5.two,"100.00%","50.00%","100.00","14.0","1","0.50","1","0"),
                        PeopleRowIMP(team2.one,"0.00%","0.00%","50.00","-1.0","0","0.00","1","0"),
                        PeopleRowIMP(team2.two,"0.00%","0.00%","50.00","-1.0","0","0.00","1","0"),
                        PeopleRowIMP(team3.one,"0.00%","0.00%","16.67","-11.0","0","0.00","1","0"),
                        PeopleRowIMP(team3.two,"0.00%","0.00%","16.67","-11.0","0","0.00","1","0"),
                        PeopleRowIMP(team1.one,"0.00%","0.00%","0.00","-16.0","0","0.00","1","0"),
                        PeopleRowIMP(team1.two,"0.00%","0.00%","0.00","-16.0","0","0.00","1","0")
      )

  val listDuplicateResult = List(
        team4.one+"\n1\n19",
        team4.two+"\n1\n19",
        team5.one+"\n2\n18",
        team5.two+"\n2\n18",
        team2.one+"\n3\n16",
        team2.two+"\n3\n16",
        team3.one+"\n4\n15",
        team3.two+"\n4\n15",
        team1.one+"\n5\n12",
        team1.two+"\n5\n12",
      )

  val prefixThatMatchesSomeNames = "e"
  lazy val matchedNames = allHands.teams.flatMap{ t => List(t.one,t.two).filter(n=> n.toLowerCase().startsWith(prefixThatMatchesSomeNames))}
  val prefixThatMatchesNoOne = "asdf"

  val movement = "Howell2Table5Teams"
  val boardset = "StandardBoards"

  lazy val allHands = AllHandsInMatch(
      List(team1,team2,team3,team4,team5),
      BoardSetsPage.getBoardSet(boardset).get,
      MovementsPage.getMovement(movement).get,

      // board 1    NS      EW
      EnterHand( 2, 110,0,  4,0,  0,  1,Spades,NotDoubled,North,Made,2,NotVul)::
      EnterHand( 3,  80,0,  5,2, -1,  1,Spades,NotDoubled,North,Made,1,NotVul)::
      Nil,

      // board 2    NS      EW
      EnterHand( 2, 110,0,  4,0,  0,  2,Spades,NotDoubled,North,Made,2,Vul)::
      EnterHand( 3, 140,2,  5,0,  1,  2,Spades,NotDoubled,North,Made,3,Vul)::
      Nil,

      // board 3    NS      EW
      EnterHand( 5, 140,0,  4,0,  0,  3,Spades,NotDoubled,North,Made,3,NotVul)::
      EnterHand( 3, 140,1,  2,1,  0,  3,Spades,NotDoubled,North,Made,3,NotVul)::
      Nil,

      // board 4    NS      EW
      EnterHand( 5, 620,0,  4,0,  0,  4,Spades,NotDoubled,North,Made,4,Vul)::
      EnterHand( 3, 620,1,  2,1,  0,  4,Spades,NotDoubled,North,Made,4,Vul)::
      Nil,

      // board 5    NS      EW
      EnterHand( 4, 650,0,   3,0,  0,  5,Spades,NotDoubled,North,Made,5,Vul)::
      EnterHand( 1,   0,0,   5,2, -12,  0,Spades,NotDoubled,North,Made,5,Vul)::
      Nil,

      // board 6    NS      EW
      EnterHand( 4,1010,0,   3,0,  0,  6,Spades,NotDoubled,North,Made,7,NotVul)::
      EnterHand( 1, 980,0,   5,2,  -1,  6,Spades,NotDoubled,North,Made,6,NotVul)::
      Nil,

      // board 7    NS      EW
      EnterHand( 3,720,0,  5,0,  0,  1,Hearts,Redoubled,North,Made,1,Vul)::
      EnterHand( 4,720,1,  1,1,  0,  1,Hearts,Redoubled,North,Made,1,Vul)::
      Nil,

      // board 8    NS      EW
      EnterHand( 3,470,0,  5,0,  0,  2,Hearts,Doubled,North,Made,2,NotVul)::
      EnterHand( 4,470,1,  1,1,  0,  2,Hearts,Doubled,North,Made,2,NotVul)::
      Nil,

      // board 9    NS      EW
      EnterHand( 5,140,0,  4,0,  0,  3,Hearts,NotDoubled,North,Made,3,NotVul)::
      EnterHand( 2,140,1,  1,1,  0,  3,Hearts,NotDoubled,North,Made,3,NotVul)::
      Nil,

      // board 10   NS      EW
      EnterHand( 5,630,0,    4,0,  0,  4,NoTrump,NotDoubled,North,Made,4,Vul)::
      EnterHand( 2,660,2,    1,0,  1,  4,NoTrump,NotDoubled,North,Made,5,Vul)::
      Nil,

      // board 11   NS      EW
      EnterHand( 5,460,0,  2,0,  0,  5,NoTrump,NotDoubled,North,Made,5,NotVul)::
      EnterHand( 4,460,1,  1,1,  0,  5,NoTrump,NotDoubled,North,Made,5,NotVul)::
      Nil,

      // board 12   NS      EW
      EnterHand( 5,2220,0,  2,0,  0,  7,NoTrump,NotDoubled,North,Made,7,Vul)::
      EnterHand( 4,2220,1,  1,1,  0,  7,NoTrump,NotDoubled,North,Made,7,Vul)::
      Nil,

      // board 13   NS      EW
      EnterHand( 5, 70,0,  2,0,  0,  1,Diamonds,NotDoubled,North,Made,1,Vul)::
      EnterHand( 1, 70,1,  3,1,  0,  1,Diamonds,NotDoubled,North,Made,1,Vul)::
      Nil,

      // board 14   NS      EW
      EnterHand( 5, 90,0,  2,0,  0,  2,Diamonds,NotDoubled,North,Made,2,NotVul)::
      EnterHand( 1, 90,1,  3,1,  0,  2,Diamonds,NotDoubled,North,Made,2,NotVul)::
      Nil,

      // board 15   NS      EW
      EnterHand( 1,110,0,   5,0,  0,  3,Diamonds,NotDoubled,North,Made,3,Vul)::
      EnterHand( 3,110,1,   2,1,  0,  3,Diamonds,NotDoubled,North,Made,3,Vul)::
      Nil,

      // board 16   NS      EW
      EnterHand( 1,-100,0,  5,0,  0,  4,Clubs,NotDoubled,North,Down,2,NotVul)::
      EnterHand( 3, -50,2,  2,0,  2,  4,Clubs,NotDoubled,North,Down,1,NotVul)::
      Nil,

      // board 17   NS      EW
      EnterHand( 4,-150,0,  3,0,  0,  5,Clubs,NotDoubled,North,Down,3,NotVul)::
      EnterHand( 2,-150,1,  1,1,  0,  5,Clubs,NotDoubled,North,Down,3,NotVul)::
      Nil,

      // board 18   NS      EW
      EnterHand( 4,-100,0,  3,0,  0,  6,Clubs,NotDoubled,North,Down,1,Vul)::
      EnterHand( 2,-100,1,  1,1,  0,  6,Clubs,NotDoubled,North,Down,1,Vul)::
      Nil,

      // board 19   NS      EW
      EnterHand( 1,-150,0,  3,0,  0,  5,Clubs,NotDoubled,North,Down,3,NotVul)::
      EnterHand( 2,-150,1,  4,1,  0,  5,Clubs,NotDoubled,North,Down,3,NotVul)::
      Nil,

      // board 20   NS      EW
      EnterHand( 1,-100,0,  3,0,  0,  6,Clubs,NotDoubled,North,Down,1,Vul)::
      EnterHand( 2,-100,1,  4,1,  0,  6,Clubs,NotDoubled,North,Down,1,Vul)::
      Nil
  )

  // this is here to validate the AllHandsInMatch.getScoreToRound call
  val resultAfterOneRoundCheckMark = List(
        TeamScoreboard(team1.blankNames, 0, "0", List(xx,xx,xx,xx,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team2, 0, "0", List(bl,bl,bl,bl,xx,xx,xx,xx,bl,bl,cm,cm,cm,cm,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team3, 0, "0", List(bl,bl,bl,bl,cm,cm,bl,bl,xx,xx,xx,xx,bl,bl,bl,bl,cm,cm,bl,bl)),
        TeamScoreboard(team4, 0, "0", List(bl,bl,bl,bl,cm,cm,bl,bl,bl,bl,bl,bl,xx,xx,xx,xx,cm,cm,bl,bl)),
        TeamScoreboard(team5, 0, "0", List(bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,cm,cm,cm,cm,bl,bl,xx,xx,xx,xx))
      )

  val resultAfterOneRoundZero = List(
        TeamScoreboard(team1.blankNames, 0, "0", List(xx,xx,xx,xx,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team2, 0, "0", List(bl,bl,bl,bl,xx,xx,xx,xx,bl,bl,zr,zr,zr,zr,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team3, 0, "0", List(bl,bl,bl,bl,zr,zr,bl,bl,xx,xx,xx,xx,bl,bl,bl,bl,zr,zr,bl,bl)),
        TeamScoreboard(team4, 0, "0", List(bl,bl,bl,bl,zr,zr,bl,bl,bl,bl,bl,bl,xx,xx,xx,xx,zr,zr,bl,bl)),
        TeamScoreboard(team5, 0, "0", List(bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,zr,zr,zr,zr,bl,bl,xx,xx,xx,xx))
      )
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class Duplicate5TestPages
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import Eventually.{ patienceConfig => _, _ }
  import ParallelUtils._

  import Duplicate5TestPages._
  import PageBrowser._

  import scala.concurrent.duration._

  val screenshotDir = "target/Duplicate5TestPages"
  val docsScreenshotDir = "target/docs/Duplicate"

  val SessionDirector = new DirectorSession()
  val SessionComplete = new CompleteSession()
  val SessionTable1 = new TableSession("1")
  val SessionTable2 = new TableSession("2")

//  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

//  case class MyDuration( timeout: Long, units: TimeUnit )
  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration = MyDuration( 60, TimeUnit.SECONDS )

  val defaultPatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig = defaultPatienceConfig

  override
  def beforeAll() = {

    MonitorTCP.nextTest()
    TestStartLogging.startLogging()
    try {
      import Session._
      // The sessions for the tables and complete is defered to the test that gets the home page url.
      waitForFutures( "Starting browser or server",
                      CodeBlock { SessionDirector.sessionStart(getPropOrEnv("SessionDirector")).setQuadrant(1) },
                      CodeBlock { TestServer.start() }
                      )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }
  }

  override
  def afterAll() = {
    waitForFuturesIgnoreTimeouts( "Stopping browsers and server",
                    CodeBlock { SessionTable1.sessionStop() },
                    CodeBlock { SessionTable2.sessionStop() },
                    CodeBlock { SessionComplete.sessionStop() },
                    CodeBlock { SessionDirector.sessionStop() },
                    CodeBlock { TestServer.stop() }
                    )
  }

  var dupid: Option[String] = None
  var boardSet: Option[BoardSet] = None

  behavior of "Duplicate test pages of Bridge Server"

  it should "go to the home page" in {
    import Session._
    waitForFutures(
      "Starting browsers",
      CodeBlock { SessionTable1.sessionStart(getPropOrEnv("SessionTable1")).setQuadrant(4) },
      CodeBlock { SessionTable2.sessionStart(getPropOrEnv("SessionTable2")).setQuadrant(3) },
      CodeBlock { SessionComplete.sessionStart(getPropOrEnv("SessionComplete")).setQuadrant(2) },
      CodeBlock {
        import SessionDirector._
        HomePage.goto.validate
      }
    )

  }

  it should "go to duplicate list page" in {
    import SessionDirector._

    tcpSleep(15)
    HomePage.current.clickListDuplicateButton.validate
  }

//  it should "go to boardsets page" in {
//    import SessionDirector._
//
//    val lp = ListDuplicatePage.current.validate.clickMainMenu.validate
//    eventually {
//      lp.findElemById("BoardSets")
//    }
//    lp.clickBoardSets.validate.click(BoardSetsPage.boardsets.head).validate.clickOK.validate
//  }
//
//  it should "go to movements page" in {
//    import SessionDirector._
//
//    val lp = ListDuplicatePage.current.validate.clickMainMenu.validate
//    eventually {
//      lp.findElemById("Movements")
//    }
//    lp.clickMovements.validate.click(MovementsPage.movements.head).validate.clickOK.validate
//  }

  it should "allow creating a new duplicate match" in {
    import SessionDirector._

    val dp = ListDuplicatePage.current
    dp.withClueAndScreenShot(screenshotDir, "NewDuplicate", "clicking NewDuplicate button") {
      dp.clickNewDuplicateButton.validate
    }
  }

  it should "create a new duplicate match" in {
    import SessionDirector._

    try {

      val curPage = NewDuplicatePage.current

      val boards = MovementsPage.getBoardsFromMovement(movement)

      testlog.info(s"Boards are $boards")

      dupid = curPage.click(boardset, movement).validate(boards).dupid
      dupid mustBe Symbol("defined")

      testlog.info(s"Duplicate id is ${dupid.get}")

      allHands.boardsets mustBe Symbol("defined")
    } catch {
      case x: Exception =>
        testlog.severe("Error creating new duplicate match", x)

        throw x
    }
  }

  var rounds: List[Int] = Nil

  it should "go to duplicate match game in complete, table 1, and table 2 browsers" in {
    tcpSleep(60)

    rounds = MovementsPage.getRoundsFromMovement(movement)

    waitForFutures(
      "Starting browsers",
      CodeBlock {
        import SessionDirector._
        val main = ScoreboardPage.current.clickMainMenu.validate
        eventually {
          main.findElemById("Director")
        }
        main.clickDirectorButton.validate
      },
      CodeBlock {
        import SessionTable1._
        ScoreboardPage.goto(dupid.get).validate.clickTableButton(1).validate(rounds)
      },
      CodeBlock {
        import SessionTable2._
        TablePage.goto(dupid.get,"2", EnterNames).validate(rounds)
      },
      CodeBlock {
        import SessionComplete._
        ScoreboardPage.goto(dupid.get).validate
      }
    )
  }

  it should "allow players names to be entered at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Entering Names",
      CodeBlock {
        import SessionTable1._
        val (nsTeam,ewTeam) = allHands.getNSEW(1, 1)
        var sk = TablePage.current(EnterNames).validate(rounds).clickRound(1).asInstanceOf[TableEnterScorekeeperPage].validate
        sk.isOKEnabled mustBe false
        sk = sk.enterScorekeeper(nsTeam.one).esc.clickPos(North)
        sk.isOKEnabled mustBe true
        sk.findSelectedPos mustBe Some(North)
        var en = sk.clickOK.validate
        en.isOKEnabled mustBe false
        en = en.enterPlayer(South, nsTeam.two).enterPlayer(East, ewTeam.one)
        en.isOKEnabled mustBe false
        en = en.enterPlayer(West, ewTeam.two).esc
        en.isOKEnabled mustBe true
        val scoreboard = en.clickOK.asInstanceOf[ScoreboardPage].validate( allHands.getBoardsInTableRound(1, 1) )
      },
      CodeBlock {
        import SessionTable2._
        val (nsTeam,ewTeam) = allHands.getNSEW(2, 1)
        var sk = TablePage.current(EnterNames).validate(rounds).clickRound(1).asInstanceOf[TableEnterScorekeeperPage].validate
        sk.isOKEnabled mustBe false
        sk = sk.enterScorekeeper(nsTeam.one).esc.clickPos(North)
        sk.isOKEnabled mustBe true
        sk.findSelectedPos mustBe Some(North)
        var en = sk.clickOK.validate
        en.isOKEnabled mustBe false
        en = en.enterPlayers(nsTeam.two, ewTeam.one, ewTeam.two).esc
        en.isOKEnabled mustBe true
        val scoreboard = en.clickOK.asInstanceOf[ScoreboardPage].validate( allHands.getBoardsInTableRound(2, 1) )
      }
    )
  }

  it should "allow first round to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing first round",
      CodeBlock {
        import SessionTable1._
        val (nsTeam,ewTeam) = allHands.getNSEW(1, 1)
        playRound(ScoreboardPage.current, 1, 1, North, false)
      },
      CodeBlock {
        import SessionTable2._
        val (nsTeam,ewTeam) = allHands.getNSEW(2, 1)
        playRound(ScoreboardPage.current, 2, 1, North, false)
      }
    )
  }

  it should "validate first round at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Validating first round",
      CodeBlock {
        import SessionTable1._
        val (nsTeam,ewTeam) = allHands.getNSEW(1, 1)
        validateRound(ScoreboardPage.current, 1, 1 )
      },
      CodeBlock {
        import SessionTable2._
        val (nsTeam,ewTeam) = allHands.getNSEW(2, 1)
        validateRound(ScoreboardPage.current, 2, 1 )
      }
    )
  }

  def checkPlayedBoards(
        sb: ScoreboardPage,
        checkmarks: Boolean,
        table: Option[Int],
        round: Int
      )( implicit
         webDriver: WebDriver
      ): ScoreboardPage = {
    val boards = table match {
      case Some(t) => allHands.getBoardsInTableRound(t, round)
      case None => allHands.getBoardsInRound(round)
    }

    sb.withClueAndScreenShot(screenshotDir, s"screenshotTable${table.getOrElse(0)}Round${round}", s"Table ${table}, round ${round} looking at boards ${boards}") {
      sb.validate
      val pr = boards.foldLeft( None: Option[BoardPage] ) { (progress,board) =>
        val bb = progress match {
          case Some(bp) => bp.clickPlayedBoard(board).validate
          case None => sb.clickBoardToBoard(board).validate
        }

        Some(bb.checkHand(round, board, allHands, checkmarks))
      }

      pr.map(bp=>bp.clickScoreboard).getOrElse(sb)
    }
  }

  it should "show the director's scoreboard and complete scoreboard shows checkmarks for the played games" in {
    tcpSleep(10)
    waitForFutures(
      "Checking scoreboards",
      CodeBlock{
        import SessionDirector._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundZero:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandDirectorView)
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, false, None, 1 )
      },
      CodeBlock{
        import SessionComplete._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandCompletedView)
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, true, None, 1 )
      },
      CodeBlock{
        import SessionTable1._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandTableView( 1, 1, team1.teamid, team2.teamid ))
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, false, Some(1), 1 )
      },
      CodeBlock{
        import SessionTable2._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandTableView( 2, 1, team4.teamid, team3.teamid ))
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, false, Some(2), 1 )
      }
    )
  }

  def selectScorekeeper( currentPage: ScoreboardPage,
                         table: Int, round: Int,
                         scorekeeper: PlayerPosition,
                         mustswap: Boolean
                       )( implicit
                           webDriver: WebDriver
                       ) = {
    val (ns,ew) = allHands.getNSEW(table, round)
    val tp = currentPage.clickTableButton(table).validate.setTarget(SelectNames)
    val ss = tp.clickRound(round).asInstanceOf[TableSelectScorekeeperPage].validate

    val sn = ss.verifyAndSelectScorekeeper(ns.one, ns.two, ew.one, ew.two, scorekeeper)
    sn.verifyNamesAndSelect(ns.teamid, ew.teamid, ns.one, ns.two, ew.one, ew.two, scorekeeper, mustswap).asInstanceOf[ScoreboardPage]
  }

  it should "allow selecting players for round 2" in {
    tcpSleep(10)
    waitForFutures(
      "Selecting players for round 2",
      CodeBlock{
        import SessionTable1._
        val sb = selectScorekeeper(ScoreboardPage.current,1,2,North,false )
      },
      CodeBlock{
        import SessionTable2._
        val (ns,ew) = allHands.getNSEW(2, 2)

        val em = ScoreboardPage.current.
                                clickTableButton(2).validate.
                                setTarget(MissingNames).
                                clickRound(2).asInstanceOf[TableEnterMissingNamesPage].validate
        em.getInputFieldNames.foreach( p => p match {
          case North => em.enterPlayer(North, ns.one)
          case South => em.enterPlayer(South, ns.two)
          case East => em.enterPlayer(East, ew.one)
          case West => em.enterPlayer(West, ew.two)
        })
        val ss = em.esc.validate.clickOK
        val sn = ss.verifyAndSelectScorekeeper(ns.one, ns.two, ew.one, ew.two, North)
        sn.verifyNamesAndSelect(ns.teamid, ew.teamid, ns.one, ns.two, ew.one, ew.two, North, false).asInstanceOf[ScoreboardPage]
      }
    )
  }

  it should "allow second round to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing second round",
      CodeBlock {
        import SessionTable1._
        playRound(ScoreboardPage.current, 1, 2, North, false)
      },
      CodeBlock {
        import SessionTable2._
        playRound(ScoreboardPage.current, 2, 2, North, false)
      }
    )
  }

  it should "validate second round at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Validating second round",
      CodeBlock {
        import SessionTable1._
        validateRound(ScoreboardPage.current, 1, 2 )
      },
      CodeBlock {
        import SessionTable2._
        validateRound(ScoreboardPage.current, 2, 2 )
      }
    )
  }

  it should "show the results on the scoreboards after round 2" in {
    tcpSleep(10)
    waitForFutures(
      "Checking scoreboards",
      CodeBlock{
        import SessionDirector._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandDirectorView)
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, None, 2 )
      },
      CodeBlock{
        import SessionComplete._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandCompletedView)
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, true, None, 2 )
      },
      CodeBlock{
        import SessionTable1._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandTableView( 1, 2, team1.teamid, team2.teamid ))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, Some(1), 2 )
      },
      CodeBlock{
        import SessionTable2._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandTableView( 2, 2, team3.teamid, team4.teamid ))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, Some(2), 2 )
      }
    )
  }

  /**
   * Fix the tables to accound for not having all the names in the first round
   */
  def fixTables( ts: List[TeamScoreboard],
                 pes: List[ScoreboardPage.PlaceEntry],
                 round: Int
               ): (List[TeamScoreboard],List[ScoreboardPage.PlaceEntry]) = {
    if (round == 1) {
      val notplaying = allHands.getTeamThatDidNotPlay(round)
      val ts1 = {
        ts.map { t =>
          if (notplaying.contains(t.team.teamid)) {
            t.copy(team = t.team.blankNames )
          } else {
            t
          }
        }
      }
      val pes1 = {
        pes.map { p =>
          val nteams = p.teams.map{ t =>
            if (notplaying.contains(t.teamid)) {
              t.blankNames
            } else {
              t
            }
          }
          p.copy( teams = nteams )
        }
      }
      (ts1,pes1)
    } else {
      (ts,pes)
    }
  }

  /**
   * @param currentPage
   * @param table
   * @param round
   * @param scorekeeper
   * @param mustswap
   * @param webDriver
   * @return A ScoreboardPage object that represents the page at the end.
   */
  def playRound(
      currentPage: ScoreboardPage,
      table: Int,
      round: Int,
      scorekeeper: PlayerPosition,
      mustswap: Boolean
    )( implicit
         webDriver: WebDriver
    ) = {

    val (nsTeam,ewTeam) = allHands.getNSEW(table, round)
    val boards = allHands.getBoardsInTableRound(table, round)

    try {
      withClueAndScreenShot( screenshotDir, s"PlayRoundT${table}R${round}", s"On table ${table} round ${round}", true) {

        val board = withClue( s"""board ${boards.head}""" ) {
          val hand = currentPage.clickBoardToHand(boards.head).validate
          hand.setInputStyle("Prompt")
          val brd = hand.enterHand( table, round, boards.head, allHands, nsTeam, ewTeam)
          brd.checkBoardButtons(boards.head,true,boards.head).checkBoardButtons(boards.head,false, boards.tail:_*).checkBoardButtonSelected(boards.head)
        }

        var playedBoards = boards.head::Nil
        var unplayedBoards = boards.tail

        var currentBoard = board
        while (!unplayedBoards.isEmpty) {
          val b = unplayedBoards.head
          unplayedBoards = unplayedBoards.tail
          playedBoards = b::playedBoards

          val board = withClue( s"""board ${b}""" ) {
            val hand2 = currentBoard.clickUnplayedBoard(b).validate
            currentBoard = hand2.enterHand( table, round, b, allHands, nsTeam, ewTeam)
            currentBoard = currentBoard.checkBoardButtons(b,true,playedBoards:_*).checkBoardButtons(b,false, unplayedBoards:_*).checkBoardButtonSelected(b)
          }
        }

        val sbr = currentBoard.clickScoreboard.validate

        sbr
      }
    } catch {
      case x: Exception =>
        // Thread.sleep(10*60*1000L)
        throw x
    }
  }

  /**
   * @param currentPage
   * @param table
   * @param round
   * @param scorekeeper
   * @param mustswap
   * @param webDriver
   * @return A ScoreboardPage object that represents the page at the end.
   */
  def validateRound(
      currentPage: ScoreboardPage,
      table: Int,
      round: Int,
      imp: Boolean = false
    )( implicit
         webDriver: WebDriver
    ) = {

    val (nsTeam,ewTeam) = allHands.getNSEW(table, round)

    val sbr = currentPage.validate
    val (ts,pes) = allHands.getScoreToRound(round, HandTableView( table, round, nsTeam.teamid, ewTeam.teamid ), imp)
    val (ts1,pes1) = fixTables(ts, pes, round)
    sbr.checkTable( ts1: _*)
    sbr.checkPlaceTable( pes1: _*)

    sbr
  }

  it should "allow round 3 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 3",
      CodeBlock {
        import SessionTable1._
        val sb = selectScorekeeper(ScoreboardPage.current,1,3,North,false)
        playRound(sb,1,3,North,false )
      },
      CodeBlock {
        import SessionTable2._
        val sb = selectScorekeeper(ScoreboardPage.current,2,3,North,false )
        playRound(sb,2,3,North,false )
      }
    )
  }

  it should "validate round 3 at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Validating round 3",
      CodeBlock {
        import SessionTable1._
        val sb = ScoreboardPage.current
        validateRound(sb,1,3 )
      },
      CodeBlock {
        import SessionTable2._
        val sb = ScoreboardPage.current
        validateRound(sb,2,3 )
      }
    )
  }

  it should "show all boards" in {
    tcpSleep(60)
    waitForFutures(
      "Checking all boards",
      CodeBlock {
        import SessionDirector._
        withClue( """On session Director""" ) {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1).head, allHands, false)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3).head, allHands, false)
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3).head, allHands, false)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5).head, allHands, false)

          page.clickScoreboard
        }
      },
      CodeBlock {
        import SessionComplete._
        withClue( """On session Complete""" ) {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5).head, allHands, true)

          page.clickScoreboard
        }
      },
      CodeBlock {
        import SessionTable1._
        PageBrowser.withClueAndScreenShot(screenshotDir, "VerifyBoards", """On session Table 1""") {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3).head, allHands, false)  // played on this table
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5).head, allHands, true)

          page.clickScoreboard
        }
      },
      CodeBlock {
        import SessionTable2._
        PageBrowser.withClueAndScreenShot(screenshotDir, "VerifyBoards", """On session Table 2""") {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1)(2), allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3)(2), allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3)(2), allHands, false)  // played on this table
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5)(2), allHands, true)

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.clickScoreboard
        }
      }
    )
  }

  it should "allow round 4 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 4",
      CodeBlock {
        import SessionTable1._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,1,4,North,false)
        playRound(sb1,1,4,North,false )
      },
      CodeBlock {
        import SessionTable2._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,2,4,North,false)
        playRound(sb1,2,4,North,false )
      }
    )
  }

  it should "validate round 4 at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Validating round 4",
      CodeBlock {
        import SessionTable1._
        val sb1 = ScoreboardPage.current
        validateRound(sb1,1,4 )
      },
      CodeBlock {
        import SessionTable2._
        val sb1 = ScoreboardPage.current
        validateRound(sb1,2,4 )
      }
    )
  }

  it should "allow round 5 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 5",
      CodeBlock {
        import SessionTable1._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,1,5,North,false)
        playRound(sb1,1,5,North,false)
      },
      CodeBlock {
        import SessionTable2._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,2,5,North,false)
        playRound(sb1,2,5,North,false )
      }
    )
  }

  it should "validate round 5 at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Validating round 5",
      CodeBlock {
        import SessionTable1._
        val sb1 = ScoreboardPage.current
        validateRound(sb1,1,5 )
      },
      CodeBlock {
        import SessionTable2._
        val sb1 = ScoreboardPage.current.setScoreStyle(ScoreboardPage.ScoreStyleIMP).validate
//        Thread.sleep(500L)
        validateRound(sb1,2,5, true )
      }
    )
  }

  it should "show the result on the summary page" in {
    import SessionComplete._

    dupid match {
      case Some(id) =>
        val page = ScoreboardPage.current.clickMainMenu.validate
        eventually {
          page.findElemById("Summary")
        }
        val sum = page.clickSummary.validate( id )
        sum.checkResults(id, listDuplicateResult:_*)
      case None =>
        ScoreboardPage.current.clickSummary.validate
    }
  }

  it should "show the people page" in {
    import SessionComplete._

    val sb = ListDuplicatePage.current.validate
    val ids = sb.getMatchIds
    val lp = sb.clickMainMenu.validate
    eventually {
      lp.findElemById("Statistics")
    }

    val peoplePage = lp.clickStatistics.validate.clickPeopleResults

//    maximize
    val hasFullscreen = peoplePage.hasFullscreenButton
    if (hasFullscreen) peoplePage.clickFullscreen

    peoplePage.withClueAndScreenShot(screenshotDir, "ShowPeoplePage", "Checking people") {
      if (ids.size == 1) {
        peoplePage.getPlayerTableScoringStyle mustBe Some("CalcMP")
        peoplePage.checkPeopleMP( peopleResultMP:_*)
        val pp = peoplePage.clickPlayerTableScoringStyle("CalcIMP")
        // Thread.sleep(100L)
        pp.checkPeopleIMP( peopleResultIMP:_*)
        val pp1 = pp.clickPlayerTableScoringStyle("CalcPlayed")
        // Thread.sleep(100L)
        pp1.checkPeople( peopleResult:_*)

      } else {
        testlog.info(s"Not testing the people page with results, number of matchs played was not 1: ${ids.size}")
      }
    }
    if (hasFullscreen) peoplePage.clickFullscreen
  }

  def getDuplicate( dupid: String ): MatchDuplicate = {
    import com.github.thebridsk.bridge.data.rest.JsonSupport._
    val url = TestServer.getUrl(s"/v1/rest/duplicates/${dupid}")
    val o = HttpUtils.getHttpObject[MatchDuplicate](url)
    o.data match {
      case Some(r) => r
      case None =>
        testlog.warning(s"Unable to get MatchDuplicate from rest API for ${dupid}: ${o}")
        fail(s"Unable to get MatchDuplicate from rest API for ${dupid}")
    }
  }

  def postDuplicate( dup: MatchDuplicate ): String = {
    import com.github.thebridsk.bridge.data.rest.JsonSupport._
    val url = TestServer.getUrl(s"/v1/rest/duplicates")
    val o = HttpUtils.postHttpObject(url, dup)
    o.data match {
      case Some(r) => r.id
      case None =>
        testlog.warning(s"Unable to post MatchDuplicate to rest API for ${dup.id}: ${o}")
        fail(s"Unable to post MatchDuplicate to rest API for ${dup.id}")
    }
  }

  def getTeam( dup: MatchDuplicate, teamid: Id.Team, name1: String, name2: String ) = {
    val t = dup.getTeam(teamid) match {
      case Some(team) =>
        team.copy(player1 = name1, player2 = name2 )
      case None =>
        testlog.warning(s"Did not find team ${teamid} in ${dup}")
        fail(s"Did not find team ${teamid}")
    }
    t
  }

  def getTeams( dup: MatchDuplicate ) = {
    getTeam( dup, "T1", team2.one, team3.two ) ::
    getTeam( dup, "T2", team3.one, team4.two ) ::
    getTeam( dup, "T3", team4.one, team5.two ) ::
    getTeam( dup, "T4", team5.one, team1.two ) ::
    getTeam( dup, "T5", team1.one, team2.two ) ::
    Nil
  }

  it should "Create another duplicate match from the one just played" in {
    import SessionComplete._

    val dup = getDuplicate( dupid.get )

    val teams = getTeams(dup)
    val tdup = dup.setTeams(teams)
    val ndup = tdup.copy( scoringmethod = Some(MatchDuplicate.InternationalMatchPoints) )

    val newid = postDuplicate(ndup)

    val menu = StatisticsPage.current.clickMainMenu.validateMainMenu
    menu.clickSummary.validate( dupid.get, newid )
  }

  it should "show the people page 2" in {
    import SessionComplete._

    val sb = ListDuplicatePage.current
    val ids = sb.getMatchIds
    val lp = sb.clickMainMenu.validate
    eventually {
      lp.findElemById("Statistics")
    }
    val peoplePage = lp.clickStatistics.validate.clickPeopleResults

    peoplePage.withClueAndScreenShot(screenshotDir, "ShowPeoplePage", "Checking people") {
      if (ids.size == 2) {
        peoplePage.getPlayerTableScoringStyle mustBe Some("CalcPlayed")
        peoplePage.checkPeople( peopleResult2:_*)
        val pp = peoplePage.clickPlayerTableScoringStyle("CalcIMP")
        // Thread.sleep(100L)
        pp.checkPeopleIMP( peopleResultIMP2:_*)
        val pp1 = pp.clickPlayerTableScoringStyle("CalcMP")
        // Thread.sleep(100L)
        pp1.checkPeopleMP( peopleResultMP2:_*)

      } else {
        testlog.info(s"Not testing the people page with results, number of matches played was not 2: ${ids.size}")
      }
    }
  }
}
