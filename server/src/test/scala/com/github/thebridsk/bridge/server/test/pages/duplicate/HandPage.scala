package com.github.thebridsk.bridge.server.test.pages.duplicate

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.bridge.server.test.pages.BaseHandPage
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.server.test.pages.duplicate.ScoreboardPage.PlaceEntry
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement

object HandPage {

  val log = Logger[HandPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new HandPage
  }

  def goto(dupid: String, tableId: Int, roundId: Int, board: String, hand: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(dupid, tableId, roundId, board, hand )
    new HandPage
  }

  // duplicate URI: #duplicate/M19/table/1/round/1/boards/B1/hands/T1
  def urlFor(dupid: String, tableId: Int, roundId: Int, board: String, hand: String ) = TestServer.getAppPageUrl("duplicate/M19/table/1/round/1/boards/B1/hands/T1")

  val patternForIds = """(M\d+)/table/(\d+)/round/(\d+)/boards/(B\d+)/hands/(T\d+)""".r
}

class HandPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends BaseHandPage {
  import HandPage._

  override
  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate, patienceConfig=${patienceConfig}") {
    eventually {
      findIds
    }
    super.validate
    this
  }

  override
  def clickOk(implicit patienceConfig: PatienceConfig, pos: Position) = {
    super.clickOk
    new BoardPage
  }

  override
  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    super.clickCancel
    new BoardPage
  }

  def findIds(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val prefix = TestServer.getAppPageUrl("duplicate/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate id: ${cur}") {
      cur must startWith (prefix)
      cur.drop( prefix.length() ) match {
        case patternForIds(did,tableid,roundid,boardid,handid) => (did,tableid,roundid,boardid,handid)
        case _ => fail(s"did not match pattern")
      }
    }
  }

  def checkDealerPos( pos: PlayerPosition )(implicit patienceConfig: PatienceConfig, spos: Position) = {
    val t = getElemByXPath(s"""//div[contains(concat(' ', @class, ' '), ' handViewTableBoard ')]/table/tbody/tr[3]/td[1]""").text
    t must fullyMatch regex s"""Dealer ${pos.name}\n.*"""
    this
  }

  def enterHand(
      nsTeam: Int,
      nsScore: Int,
      nsMP: Double,
      ewTeam: Int,
      ewMP: Double,
      contractTricks: Int,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      madeOrDown: MadeOrDown,
      tricks: Int,
      vul: Vulnerability,
      validate: Boolean = true
    )(implicit
        patienceConfig: PatienceConfig,
        pos: Position
    ): BoardPage = {
    if (validate) {
      getTeamNumber(North) mustBe nsTeam.toString()
      getTeamNumber(South) mustBe nsTeam.toString()
      getTeamNumber(East) mustBe ewTeam.toString()
      getTeamNumber(West) mustBe ewTeam.toString()
    }
    val board = onlyEnterHand( contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks )
    if (validate) board.checkTeamScores(nsTeam, nsScore, nsMP, ewTeam, ewMP, contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, vul)
    else board
  }

  def enterHand( eh: EnterHand )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    import eh._
    enterHand(nsTeam, nsScore, nsMP, ewTeam, ewMP, contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, vul)
  }

  def enterHand(  table: Int, round: Int, board: Int, allhands: AllHandsInMatch, nsTeam: Team, ewTeam: Team )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    withClue( s"""${pos.line} HandPage.enterHand table ${table} round ${round} board ${board} """ ) {
      val b = allhands.getBoard(table, round, board)
      onlyEnterHand(table, round, board, allhands, nsTeam, ewTeam).checkOthers(b, allhands)
    }
  }

  def onlyEnterHand(
      contractTricks: Int,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      madeOrDown: MadeOrDown,
      tricks: Int,
      validate: Boolean = true
    )(implicit
        patienceConfig: PatienceConfig,
        pos: Position
    ): BoardPage = {
    enterContract(contractTricks,contractSuit,contractDoubled,declarer,madeOrDown,tricks)
    if (validate) validateContract(Some(contractTricks),Some(contractSuit),Some(contractDoubled),Some(declarer),Some(madeOrDown),Some(tricks))
    isOkEnabled mustBe true
    clickOk.validate
  }

  def onlyEnterHand( eh: EnterHand )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    import eh._
    onlyEnterHand( contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks)
  }

  def onlyEnterHand(  table: Int, round: Int, board: Int, allhands: AllHandsInMatch, nsTeam: Team, ewTeam: Team )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    withClue( s"""${pos.line} HandPage.onlyEnterHand table ${table} round ${round} board ${board} """ ) {
      val b = allhands.getBoard(table, round, board)

      val dealerPos: Option[PlayerPosition] = {
        allhands.boardsets match {
          case Some(bs) =>
            bs.boards.find(bis => bis.id==board).map(bis=>PlayerPosition(bis.dealer))
          case None => None
        }
      }

      val dealer: Option[String] = dealerPos match {
        case Some(dp) =>
          dp match {
            case North => Some(nsTeam.one)
            case South => Some(nsTeam.two)
            case East => Some(ewTeam.one)
            case West => Some(ewTeam.two)
          }
        case None => None
      }

      allhands.getBoardFromBoardSet(board) match {
        case Some(bis) =>
          b.hand.declarer match {
            case North | South =>
              b.hand.vul.vul mustBe bis.nsVul
            case East | West =>
              b.hand.vul.vul mustBe bis.ewVul
          }

          def buttonText( player: String, team: Int, vul: Boolean, pos: String ) = {
            val svul = if (vul) "Vul" else "vul"
            s"""${player.trim} ($team) $svul $pos"""
          }

          getButton("DecN").text mustBe buttonText(nsTeam.one,nsTeam.teamid,bis.nsVul,"North")
          getButton("DecS").text mustBe buttonText(nsTeam.two,nsTeam.teamid,bis.nsVul,"South")
          getButton("DecE").text mustBe buttonText(ewTeam.one,ewTeam.teamid,bis.ewVul,"East")
          getButton("DecW").text mustBe buttonText(ewTeam.two,ewTeam.teamid,bis.ewVul,"West")

        case None =>
      }

      dealerPos.foreach(dp => checkDealerPos(dp))
      dealer.foreach(d => checkDealer(d))

      onlyEnterHand(b.hand)
    }

  }

  def findTeamNumberElement(
                    loc: PlayerPosition
                  )(implicit
                     patienceConfig: PatienceConfig,
                     pos: Position
                  ) = {
    val e = find(xpath(s"""//span[@id = '${loc.name}']/span"""))
    e
  }

  val patternTeamNumber = """ \((\d+)\)""".r

  def getTeamNumber(
                    loc: PlayerPosition
                  )(implicit
                     patienceConfig: PatienceConfig,
                     pos: Position
                  ) = {
    eventually {
      findTeamNumberElement(loc).text match {
        case patternTeamNumber( team ) => team
        case x =>
          fail( s"Did not find a team number in declarer button for ${loc.name}: ${x}" )
      }
    }
  }

}

case class EnterHand(
      nsTeam: Int,
      nsScore: Int,
      nsMP: Double,
      ewTeam: Int,
      ewMP: Double,
      nsIMP: Double,
      contractTricks: Int,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      madeOrDown: MadeOrDown,
      tricks: Int,
      vul: Vulnerability
    )(implicit patienceConfig: PatienceConfig, pos: Position) {
  def ewScore = -nsScore

  def didTeamPlay( team: Int ) = team==nsTeam||team==ewTeam

  def ewIMP = if (nsIMP == 0.0) 0.0 else -nsIMP    // I don't want -0.0
}

sealed trait HandViewType
case object HandDirectorView extends HandViewType
case object HandCompletedView extends HandViewType
case class HandTableView( table: Int, round: Int, team1: Int, team2: Int ) extends HandViewType

sealed trait OtherHand
case class OtherHandPlayed( table: Int, round: Int, board: Int, nsMP: Double, ewMP: Double, nsIMP: Double, ewIMP: Double ) extends OtherHand
case class OtherHandNotPlayed( table: Int, round: Int, board: Int ) extends OtherHand
case class TeamNotPlayingHand( board: Int, team: Team ) extends OtherHand

case class HandsOnBoard( table: Int, round: Int, board: Int, hand: EnterHand, other: OtherHand* ) {

  def addTeamNotPlayed( tnp: TeamNotPlayingHand* ) = {
    HandsOnBoard( table, round, board, hand, (other.toList ::: (tnp.toList)): _* )
  }

  def doesTeamPlay( team: Int ) = {
    other.find(oh => oh match {
      case TeamNotPlayingHand( b, t ) if (team==t.teamid) => true
      case _ => false
    }).isEmpty
  }

  def didTeamPlay( team: Int, toRound: Int, allHands: AllHandsInMatch ): Boolean = {
    if (round>toRound) false
    else if (hand.didTeamPlay(team)) true
    else {
      other.find(oh => oh match {
        case ohp: OtherHandPlayed if ohp.round <= toRound &&
                          allHands.getBoard(ohp.table, ohp.round, ohp.board).hand.didTeamPlay(team) =>
          true
        case _ =>
          false
      })
      false
    }
  }

  /**
   * Get the team score assuming this is the latest hand played
   */
  def getTeamScore( team: Int, viewtype: HandViewType, allHands: AllHandsInMatch, imp: Boolean = false ): (Double,String) = {
    val boardCompleted = other.find { oh => oh.isInstanceOf[OtherHandNotPlayed] }.isEmpty
    val showScores = boardCompleted || (viewtype match {
      case HandDirectorView => true
      case HandCompletedView => false
      case tv: HandTableView =>
        if (boardCompleted) true
        else {
          other.filter(oh => oh.isInstanceOf[OtherHandNotPlayed]).size > 1 && didTeamPlay(tv.team1, round, allHands ) && didTeamPlay(tv.team2, round, allHands )
        }
    })

    def points( p: Double ) = {
      if (showScores) (p, (if (imp) f"$p%.1f" else BoardPage.toPointsString(p)))
      else (0.0,Strings.checkmark)
    }

    if (team == hand.nsTeam) {
      points(if (imp) hand.nsIMP else hand.nsMP)
    } else if (team == hand.ewTeam) {
      points(if (imp) hand.ewIMP else hand.ewMP)
    } else {
      HandPage.log.fine( s"""Board ${board} checking team ${team} viewtype ${viewtype} this ${this}""" )
      other.find( oh => oh match {
        case tnp: TeamNotPlayingHand if (tnp.team.teamid == team) =>
          true
        case _ => false
      }).map { oh =>
        HandPage.log.fine( s"""Board ${board} team ${team} did not play, viewtype ${viewtype} this ${this}""" )
        (0.0,Strings.xmark)
      } match {
        case Some(r) => r
        case None =>
          val teamOHPs = other.flatMap(oh => oh match {
            case ohp: OtherHandPlayed =>
              val h = allHands.getBoard(ohp.table, ohp.round, ohp.board).hand
              if (team == h.nsTeam) points(if (imp) ohp.nsIMP else ohp.nsMP)::Nil
              else if (team == h.ewTeam) points(if (imp) ohp.ewIMP else ohp.ewMP)::Nil
              else Nil
            case _ => Nil
          })
          teamOHPs.headOption match {
            case Some(r) => r
            case None => (0,"")
          }
      }
    }
  }

  def numberUnplayed = other.filter(oh => oh.isInstanceOf[OtherHandNotPlayed]).size

  /**
   * @param hands all the hands that have been played.
   * @return Tuple2( ns, ew )  The match points for the NS and EW team that played this hand.
   */
  def getMatchPoints( hands: List[HandsOnBoard] ): (Double, Double) = {
    def pts( other: Int, me: Int ) = {
      if (other < me) 2
      else if (other == me) 1
      else 0.0
    }
    hands.filterNot(h => h.table==table && h.round==round && h.board==board).foldLeft((0.0,0.0)) { (ac, h) =>
      ( ac._1 + pts( h.hand.nsScore, hand.nsScore ), ac._2 + pts( h.hand.ewScore, hand.ewScore ) )
    }
  }

  /**
   * @param hands all the hands that have been played.
   * @return Tuple2( ns, ew )  The international match points for the NS and EW team that played this hand.
   */
  def getInternationalMatchPoints( hands: List[HandsOnBoard] ): (Double, Double) = {
    def pts( other: Int, me: Int ) = {
      if (other <= me) BoardScore.getIMPs(me-other)
      else -BoardScore.getIMPs(other-me)
    }
    val (ns,ew) = hands.filterNot(h => h.table==table && h.round==round && h.board==board).foldLeft((0.0,0.0)) { (ac, h) =>
      ( ac._1 + pts( h.hand.nsScore, hand.nsScore ), ac._2 + pts( h.hand.ewScore, hand.ewScore ) )
    }
    val n = hands.length -1
    (ns/n,ew/n)
  }

}

case class TeamScoreboard( team: Team, points: Double, total: String, boards: List[String] )

class AllHandsInMatch( val hands: List[HandsOnBoard],
                       val teams: List[Team],
                       val boardsets: Option[BoardSet],
                       val movement: Option[Movement]
                     ) {

  val boards = hands.map(h => h.board).distinct.sorted

  val rounds = hands.map(h => h.round).distinct.sorted

  def getBoardsInRound( round: Int ) = {
    hands.filter(h=>h.round==round).map(h=>h.board).distinct.sorted
  }

  def getBoardsInTableRound( table: Int, round: Int ) = {
    hands.filter(h=>h.round==round&&h.table==table).map(h=>h.board).distinct.sorted
  }

  def getBoard( table: Int, round: Int, board: Int ) = hands.find(hob=> hob.board==board && hob.table==table && hob.round==round) match {
    case Some(b) => b
    case None =>
      import org.scalatest.Assertions._
      fail(s"""Did not find a hand for table ${table} round ${round} board ${board}""")
  }

  /**
   * Get the latest HandsOnBoard upto and including the specified round
   * @param toRound
   * @param board
   * @return None if board has not been played by anyone, Some(hob) the latest played.
   */
  def getBoardToRound( toRound: Int, board: Int ): Option[HandsOnBoard] = {
    hands.filter{ h=> h.board==board&&h.round<=toRound }.foldRight(None: Option[HandsOnBoard]) { (hob: HandsOnBoard, res: Option[HandsOnBoard]) =>
      res match {
        case Some(other) =>
          if (hob.round == other.round) {
            if (hob.numberUnplayed < other.numberUnplayed) Some(hob)
            else res
          } else if (hob.round < other.round) res
          else Some(hob)
        case None => Some(hob)
      }
    }
  }

  def getTeamScoreToRound( team: Team, toRound: Int, viewtype: HandViewType, imp: Boolean = false ) = {
    var total = 0.0
    val bs = boards.map{b =>
      getBoardToRound(toRound, b) match {
        case Some(hob) =>
          val (mp,s) = hob.getTeamScore(team.teamid, viewtype, this, imp)
          total+=mp
          s
        case None =>
          // nothing has been played yet, must find one in later round to see if I'm not playing it
          getBoardToRound(rounds.last, b) match {
            case Some(hob) =>
              if (hob.doesTeamPlay(team.teamid)) ""
              else Strings.xmark
            case None =>
              ""
          }
      }
    }
    TeamScoreboard(team,total,if (imp) f"$total%.1f" else BoardPage.toPointsString(total),bs)
  }

  def getScoreToRound( toRound: Int, viewtype: HandViewType, imp: Boolean = false ): (List[TeamScoreboard],List[PlaceEntry]) = {
    val ts = teams.map(t => getTeamScoreToRound(t, toRound, viewtype, imp))
    val scores = ts.sortWith( (l,r) => l.points>r.points ).map(t => t.total ).distinct
    HandPage.log.fine("Scores are "+scores)
    val pes = scores.map {sc => (sc, ts.filter(t=>t.total==sc).map(t=>t.team)) }.foldLeft((1,List[PlaceEntry]())){(p,v) =>
      val (sc, teams) = v
      val (nextPlace, places) = p
      ( nextPlace+teams.size, PlaceEntry( nextPlace, sc, teams )::places )
    }
    (ts, pes._2)
  }

  def getBoardFromBoardSet( board: Int ) = {
    boardsets match {
      case Some(bs) =>
        bs.boards.find(b=>b.id == board)
      case None => None
    }
  }

  def getHandsOnBoards( board: Int ) = {
    hands.filter( h => h.board == board )
  }

  def getTeamsThatPlayBoard( board: Int ) = {
    getHandsOnBoards(board).flatMap(hob => hob.hand.nsTeam::hob.hand.ewTeam::Nil)
  }

  val teamNumber = teams.map( t => t.teamid)

  def getTeamsThatDontPlayBoard( board: Int ) = {
    val played = getTeamsThatPlayBoard(board)
    teamNumber.filter( t => !played.contains(t) )
  }

  val teamsById = teams.map( t => (t.teamid, t)).toMap


  /**
   * Add in all the TeamNotPlayingHand for all hands where a team does not play the board.
   */
  def addTeamsNotPlayingBoards = {
    val h = hands.map { hob =>
      hob.addTeamNotPlayed( getTeamsThatDontPlayBoard(hob.board).map(t => TeamNotPlayingHand(hob.board, teamsById(t))): _* )
    }
    new AllHandsInMatch( h, teams, boardsets, movement )
  }

  /**
   *  Fix the hands.  All the HandsOnBoard object don't have any others.
   *  This calculates the others for all boards.
   *  It will add either a OtherHandPlayed or OtherHandNotPlayed object depending on whether the hand has been played yet.
   *  Also calculates the match points in the OtherHandPlayed objects.
   *  It will also add TeamNotPlayingHand object if any team does not play the board.
   */
  def fixHands = {
    val nh = hands.map { hob =>
      val (played,notplayed) = getHandsOnBoards( hob.board ).filterNot( h => h.table == hob.table && h.round == hob.round).
                                  foldLeft((List[HandsOnBoard](),List[HandsOnBoard]())) { (ac,h) =>
                                    if (h.round < hob.round) ( h::ac._1, ac._2 )
                                    else ( ac._1, h::ac._2 )
                                  }
      val allplayed = hob::played

      val (hobnsmp, hobewmp) = hob.getMatchPoints(allplayed)
      if ( hob.hand.nsMP != hobnsmp || hob.hand.ewMP != hobewmp ) {
        // error
        HandPage.log.severe( s"""nsMP=${hob.hand.nsMP} expect ${hobnsmp} ewMP=${hob.hand.ewMP} expect ${hobewmp} Match Points for ${hob} don't match what should be for played hands ${allplayed} """)
        fail( s"""nsMP=${hob.hand.nsMP} expect ${hobnsmp} ewMP=${hob.hand.ewMP} expect ${hobewmp} Match Points for ${hob} don't match what should be for played hands ${allplayed} """)
      }

      val otherplayed = played.map { oh =>
        val (ohnsmp, ohewmp) = oh.getMatchPoints(allplayed)
        val (ohnsimp, ohewimp) = oh.getInternationalMatchPoints(allplayed)
        OtherHandPlayed(oh.table,oh.round,oh.board,ohnsmp,ohewmp,ohnsimp, ohewimp)
      }
      val othernotplayed = notplayed.map { oh =>
        OtherHandNotPlayed(oh.table,oh.round,oh.board)
      }
      HandsOnBoard( hob.table, hob.round, hob.board, hob.hand, (otherplayed ::: othernotplayed): _* )
    }

    new AllHandsInMatch( nh, teams, boardsets, movement ).addTeamsNotPlayingBoards
  }

  def checkFixHands = {
    val calc = fixHands

    hands.foreach { hob =>
      val calcboard = calc.getBoard(hob.table, hob.round, hob.board)
      if (hob != calcboard) {
        HandPage.log.severe(s"Oops fixHands has a problem at ${hob}, calculated ${calcboard}")
        fail(s"Oops fixHands has a problem at ${hob}, calculated ${calcboard}")
      }
    }

    this
  }

  /**
   * @return Tuple2( ns, ew )
   */
  def getNSEW( table: Int, round: Int ) = {
    val boards = getBoardsInTableRound(table, round)
    val b = getBoard(table, round, boards.head)
    ( teamsById(b.hand.nsTeam), teamsById(b.hand.ewTeam) )
  }

  def getTeamThatDidNotPlay( table: Int, round: Int ) = {
    val boards = getBoardsInTableRound(table, round)
    val played = boards.flatMap { b =>
      val bb = getBoard(table, round, b)
      bb.hand.nsTeam::bb.hand.ewTeam::Nil
    }.distinct
    teamNumber.find( t => !played.contains(t))
  }

  def getTeamThatDidNotPlay( round: Int ) = {
    val boards = getBoardsInRound(round)
    val played = boards.flatMap { b =>
      val bb = getBoardToRound(round, b).get
      bb.hand.nsTeam::bb.hand.ewTeam::Nil
    }.distinct
    teamNumber.find( t => !played.contains(t))
  }
}

object AllHandsInMatch {

  /**
   * Construct an AllHandsInMatch object.
   * This will construct the hands field from the movement and handsOnBoard argument.
   * The number of items in handsOnBoard must be equal to the number of boards in movement.
   * The number of items in each handsOnBoard must be equal to the number of times that board is played.
   * The order of the boards will be sorted.
   * @param teams all the teams that are playing
   * @param boardsets
   * @param movement
   * @param handsOnBoard each list represents the hands that play on the same board
   */
  def apply( teams: List[Team],
             boardsets: BoardSet,
             movement: Movement,
             handsOnBoard: List[EnterHand]*
           ): AllHandsInMatch = {
    val boards = movement.getBoards
    if (boards.size != handsOnBoard.size)
      throw new IllegalArgumentException(s"The number of handsOnBoard (${handsOnBoard.size}) must be equal number of boards (${boards.size})" )
    val hands: List[HandsOnBoard] = movement.getBoards.zip(handsOnBoard).flatMap{ case (board,hob) =>
      val wp = movement.wherePlayed(board)
      if (wp.size != hob.size)
        throw new IllegalArgumentException(s"Board ${board}: The number of hands (${hob.size}) must be equal number of times played (${wp.size})" )
      wp.zip(hob).map{ case (wherePlayed,hand) =>
        HandsOnBoard( wherePlayed.table, wherePlayed.round, board, hand)
      }
    }
    new AllHandsInMatch( hands, teams, Some(boardsets), Some(movement) ).fixHands
  }

}
