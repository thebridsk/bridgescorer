package com.github.thebridsk.bridge.fullserver.test.pages.individual

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.fullserver.test.pages.BaseHandPage
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ScoreboardPage.PlaceEntry
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HandPicture
import com.github.thebridsk.bridge.data.BoardInSet
import com.github.thebridsk.browserpages.Element
import scala.util.matching.Regex
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ScoreboardPage
import com.github.thebridsk.bridge.data.IndividualMovement

object HandPage {

  val log: Logger = Logger[HandPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): HandPage = {
    val (dupid, viewtype, board, hand) = parseUrl
    new HandPage(dupid, viewtype, board, hand)
  }

  def goto(
      dupid: String,
      tableId: Int,
      roundId: Int,
      board: String,
      hand: String
  )(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): HandPage = {
    go to urlFor(dupid, tableId, roundId, board, hand)
    new HandPage(dupid, ScoreboardPage.TableViewType(tableId.toString(), roundId.toString()), board, hand)
  }

  // duplicate URI: #duplicate/M19/table/1/round/1/boards/B1/hands/T1
  def urlFor(
      dupid: String,
      tableId: Int,
      roundId: Int,
      board: String,
      hand: String
  ): String =
    TestServer.getAppPageUrl(
      s"individual/match/$dupid/table/$tableId/round/$roundId/boards/$board/hands/$hand"
    )

  val patternForIds: Regex =
    """(I\d+)/table/(\d+)/round/(\d+)/boards/(B\d+)/hands/(p\d+)""".r

  val patternDirectorForIds: Regex =
    """(I\d+)/director/boards/(B\d+)/hands/(p\d+)""".r

  val patternCompletedForIds: Regex =
    """(I\d+)/boards/(B\d+)/hands/(p\d+)""".r

  def parseUrl(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): (String, ScoreboardPage.ViewType, String, String) = {
    val url = currentUrl
    val prefix = TestServer.getAppPageUrl("individual/match/")
    withClue(s"Unable to determine duplicate id from URL: ${url}") {
      url must startWith(prefix)
      url.drop(prefix.length()) match {
        case patternForIds(did, tid, rid, bid, hid) =>
          (
            did,
            ScoreboardPage.TableViewType(
              tid,
              rid
            ),
            bid,
            hid
          )
        case patternDirectorForIds(did,bid,hid) =>
          (
            did,
            ScoreboardPage.DirectorViewType,
            bid,
            hid
          )
        case patternCompletedForIds(did,bid,hid) =>
          (
            did,
            ScoreboardPage.CompletedViewType,
            bid,
            hid
          )
        case _                      => fail("Could not determine table")
      }
    }
  }
}

class HandPage(
  val dupid: String,
  val viewtype: ScoreboardPage.ViewType,
  val board: String,
  val hand: String
)(
  implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends BaseHandPage[HandPage]
    with HandPicture[HandPage] {
  import HandPage._

  override def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): HandPage =
    logMethod(
      s"${pos.line} ${getClass.getSimpleName}.validate, patienceConfig=${patienceConfig}"
    ) {
      super.validate
      eventually {
        val (did, tableid, roundid, boardid, handid) = findIds
        did mustBe dupid
        viewtype match {
          case vt: ScoreboardPage.TableViewType =>
            tableid mustBe vt.table
            roundid mustBe vt.round
          case _ =>
            fail(s"Expecting a TableViewType, got ${viewtype}")
        }
        boardid mustBe board
        if (hand != "") handid mustBe handid
        new HandPage(dupid,viewtype,board,handid)
      }
    }

  override def clickOk(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardPage = {
    super.clickOk
    new BoardPage(dupid, viewtype, board)
  }

  override def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardPage = {
    super.clickCancel
    new BoardPage(dupid, viewtype, board)
  }

  def findIds(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): (String, String, String, String, String) = {
    val prefix = TestServer.getAppPageUrl("individual/match/")
    val cur = currentUrl
    withClue(s"Unable to determine individual duplicate id: ${cur}") {
      cur must startWith(prefix)
      cur.drop(prefix.length()) match {
        case patternForIds(did, tableid, roundid, boardid, handid) =>
          (did, tableid, roundid, boardid, handid)
        case _ => fail(s"did not match pattern")
      }
    }
  }

  def checkDealerPos(
      pos: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, spos: Position): HandPage = {
    val t = getElemByXPath(
      s"""//div[contains(concat(' ', @class, ' '), ' handViewTableBoard ')]/table/tbody/tr[3]/td[1]"""
    ).text
    t must fullyMatch regex s"""Dealer ${pos.name}(\n.*)?"""
    this
  }

  def enterHand(
      north: Int,
      south: Int,
      east: Int,
      west: Int,
      nsScore: Int,
      nsMP: Double,
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
      getPlayer(North) mustBe north
      getPlayer(South) mustBe south
      getPlayer(East) mustBe east
      getPlayer(West) mustBe west
    }
    val board = onlyEnterHand(
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      madeOrDown,
      tricks
    )
    if (validate)
      board.checkHandPlayedContract(
        north, south, east, west,
        nsScore,
        nsMP,
        ewMP,
        contractTricks,
        contractSuit,
        contractDoubled,
        declarer,
        madeOrDown,
        tricks,
        vul
      )
    else board
  }

  def enterHand(
      eh: EnterHand
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    import eh._
    enterHand(
      north, south, east, west,
      nsScore,
      nsMP,
      ewMP,
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      madeOrDown,
      tricks,
      vul
    )
  }

  def enterHand(
      table: Int,
      round: Int,
      board: Int,
      allhands: AllHandsInMatch,
      north: Int,
      south: Int,
      east: Int,
      west: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    withClue(
      s"""${pos.line} HandPage.enterHand table ${table} round ${round} board ${board} """
    ) {
      val b = allhands.getBoard(table, round, board)
      onlyEnterHand(
        table,
        round,
        board,
        allhands,
        allhands.playerMap(north),
        allhands.playerMap(south),
        allhands.playerMap(east),
        allhands.playerMap(west)
      ).checkOthers(
        b,
        allhands
      )
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
    enterContract(
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      madeOrDown,
      tricks
    )
    if (validate)
      validateContract(
        Some(contractTricks),
        Some(contractSuit),
        Some(contractDoubled),
        Some(declarer),
        Some(madeOrDown),
        Some(tricks)
      )
    isOkEnabled mustBe true
    clickOk.validate
  }

  def onlyEnterHand(
      eh: EnterHand
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    import eh._
    onlyEnterHand(
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      madeOrDown,
      tricks
    )
  }

  def onlyEnterHand(
      hob: HandsOnBoard,
      bis: BoardInSet,
      north: ScoreboardPage.Player,
      south: ScoreboardPage.Player,
      east: ScoreboardPage.Player,
      west: ScoreboardPage.Player,
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    withClue(
      s"""${pos.line} HandPage.onlyEnterHand table ${hob} """
    ) {
      val b = hob

      val dealerPos: PlayerPosition = PlayerPosition(bis.dealer)

      val dealer: ScoreboardPage.Player =
          dealerPos match {
            case North => north
            case South => south
            case East  => east
            case West  => west
          }

      eventually {
        b.hand.declarer match {
          case North | South =>
            b.hand.vul.vul mustBe bis.nsVul
          case East | West =>
            b.hand.vul.vul mustBe bis.ewVul
        }

        def buttonText(
            player: ScoreboardPage.Player,
            vul: Boolean,
            pos: String
        ) = {
          val svul = if (vul) "Vul" else "vul"
          s"""${player.index}: ${player.name.trim} $svul"""
        }

        getButton("DecN").text mustBe buttonText(
          north,
          bis.nsVul,
          "North"
        )
        getButton("DecS").text mustBe buttonText(
          south,
          bis.nsVul,
          "South"
        )
        getButton("DecE").text mustBe buttonText(
          east,
          bis.ewVul,
          "East"
        )
        getButton("DecW").text mustBe buttonText(
          west,
          bis.ewVul,
          "West"
        )

        checkDealerPos(dealerPos)
        checkDealer(dealer.toLabelString())
      }

      onlyEnterHand(b.hand)
    }
  }

  def onlyEnterHand(
      table: Int,
      round: Int,
      board: Int,
      allhands: AllHandsInMatch,
      north: ScoreboardPage.Player,
      south: ScoreboardPage.Player,
      east: ScoreboardPage.Player,
      west: ScoreboardPage.Player,
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    withClue(
      s"""${pos.line} HandPage.onlyEnterHand table ${table} round ${round} board ${board} """
    ) {
      val b = allhands.getBoard(table, round, board)

      val bis: BoardInSet = allhands.getBoardFromBoardSet(board).get
      val dp: PlayerPosition = PlayerPosition(bis.dealer)

      val dealer: ScoreboardPage.Player =
          dp match {
            case North => north
            case South => south
            case East  => east
            case West  => west
          }
      onlyEnterHand(b, bis, north, south, east, west)
    }

  }

  def findPlayerNameElement(
      loc: PlayerPosition
  )(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Element = {
    val e = find(xpath(s"""//span[@id = '${loc.name}']"""))
    e
  }

  val patternPlayerNumber: Regex = """(\d+): (.*) ([vV]ul)""".r

  def getPlayer(
      loc: PlayerPosition
  )(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ScoreboardPage.Player = {
    eventually {
      findPlayerNameElement(loc).text match {
        case patternPlayerNumber(index, name, vul) => ScoreboardPage.Player(index.toInt, name)
        case x =>
          fail(
            s"Did not find a player number in declarer button for ${loc.name}: ${x}"
          )
      }
    }
  }

}

case class EnterHand(
    north: Int,
    south: Int,
    east: Int,
    west: Int,
    nsScore: Int,
    nsMP: Double,
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
  def ewScore: Int = -nsScore

  def didPlayerPlay(player: Int): Boolean =
    player == north || player == south || player == east || player == west

  def ewIMP: Double = if (nsIMP == 0.0) 0.0 else -nsIMP // I don't want -0.0
}

sealed trait HandViewType
case object HandDirectorView extends HandViewType
case object HandCompletedView extends HandViewType
case class HandTableView(table: Int, round: Int, north: Int, south: Int, east: Int, west: Int)
    extends HandViewType

sealed trait OtherHand
case class OtherHandPlayed(
    table: Int,
    round: Int,
    board: Int,
    nsMP: Double,
    ewMP: Double,
    nsIMP: Double,
    ewIMP: Double
) extends OtherHand
case class OtherHandNotPlayed(
    table: Int,
    round: Int,
    board: Int
) extends OtherHand
case class PlayerNotPlayingHand(
    board: Int,
    player: Int
) extends OtherHand

case class HandsOnBoard(
    table: Int,
    round: Int,
    board: Int,
    hand: EnterHand,
    other: OtherHand*
) {

  def setOther(o: OtherHand*) = HandsOnBoard(table, round, board, hand, o:_*)

  def addTeamNotPlayed(tnp: PlayerNotPlayingHand*): HandsOnBoard = {
    HandsOnBoard(table, round, board, hand, (other.toList ::: (tnp.toList)): _*)
  }

  def doesPlayerPlay(player: Int): Boolean = {
    other
      .find(oh =>
        oh match {
          case PlayerNotPlayingHand(b, p) => p == player
          case _                          => false
        }
      )
      .isEmpty
  }

  /**
    *
    *
    * @param player
    * @param toRound
    * @param allHands
    * @return true if the player plays the hand on or before teh specified round, toRound.
    * false if the player plays the hand after the round or does not play the hand.
    */
  def didPlayerPlay(
      player: Int,
      toRound: Int,
      allHands: AllHandsInMatch
  ): Boolean = {
    if (round > toRound) false
    else if (hand.didPlayerPlay(player)) true
    else {
      other.find(oh =>
        oh match {
          case ohp: OtherHandPlayed
              if ohp.round <= toRound &&
                allHands
                  .getBoard(ohp.table, ohp.round, ohp.board)
                  .hand
                  .didPlayerPlay(player) =>
            true
          case _ =>
            false
        }
      ).isDefined
    }
  }

  /**
    * Get the team score assuming this is the latest hand played
    */
  def getPlayerScore(
      player: Int,
      viewtype: HandViewType,
      allHands: AllHandsInMatch,
      imp: Boolean = false
  ): (Double, String) = {
    val boardCompleted = other.find { oh =>
      oh.isInstanceOf[OtherHandNotPlayed]
    }.isEmpty
    val showScores = boardCompleted || (viewtype match {
      case HandDirectorView  => true
      case HandCompletedView => false
      case tv: HandTableView =>
        if (boardCompleted) true
        else {
          other
            .filter(oh => oh.isInstanceOf[OtherHandNotPlayed])
            .size > 1 &&
              didPlayerPlay(tv.north, round, allHands) &&
              didPlayerPlay(tv.south, round, allHands) &&
              didPlayerPlay(tv.east, round, allHands) &&
              didPlayerPlay(tv.west, round, allHands)
        }
    })

    def points(p: Double) = {
      if (showScores) (p, (if (imp) f"$p%.1f" else BoardPage.toPointsString(p)))
      else (0.0, Strings.checkmark)
    }

    if (player == hand.north || player == hand.south) {
      HandPage.log.fine(
        s"""Player ${player} played NS in ${hand}"""
      )
      points(if (imp) hand.nsIMP else hand.nsMP)
    } else if (player == hand.east || player == hand.west) {
      HandPage.log.fine(
        s"""Player ${player} played EW in ${hand}"""
      )
      points(if (imp) hand.ewIMP else hand.ewMP)
    } else {
      HandPage.log.fine(
        s"""Board ${board} checking player ${player} viewtype ${viewtype} this ${this}"""
      )
      other
        .find(oh =>
          oh match {
            case tnp: PlayerNotPlayingHand if (tnp.player == player) =>
              true
            case _ => false
          }
        )
        .map { oh =>
          HandPage.log.fine(
            s"""Board ${board} player ${player} did not play, viewtype ${viewtype} this ${this}"""
          )
          (0.0, Strings.xmark)
        } match {
          case Some(r) => r
          case None =>
            val teamOHPs = other.flatMap(oh =>
              oh match {
                case ohp: OtherHandPlayed =>
                  val h = allHands.getBoard(ohp.table, ohp.round, ohp.board).hand
                  if (player == h.north || player == h.south) {
                    HandPage.log.fine(
                      s"""Player ${player} played NS in ${ohp}"""
                    )
                    points(if (imp) ohp.nsIMP else ohp.nsMP) :: Nil
                  } else if (player == h.east || player == h.west) {
                    HandPage.log.fine(
                      s"""Player ${player} played NS in ${ohp}"""
                    )
                    points(if (imp) ohp.ewIMP else ohp.ewMP) :: Nil
                  }
                  else Nil
                case _ => Nil
              }
            )
            teamOHPs.headOption match {
              case Some(r) => r
              case None    => (0, "")
            }
        }
    }
  }

  def numberUnplayed: Int =
    other.filter(oh => oh.isInstanceOf[OtherHandNotPlayed]).size

  /**
    * @param hands all the hands that have been played.
    * @return Tuple2( ns, ew )  The match points for the NS and EW team that played this hand.
    */
  def getMatchPoints(hands: List[HandsOnBoard]): (Double, Double) = {
    def pts(other: Int, me: Int) = {
      if (other < me) 2
      else if (other == me) 1
      else 0.0
    }
    hands
      .filterNot(h => h.table == table && h.round == round && h.board == board)
      .foldLeft((0.0, 0.0)) { (ac, h) =>
        (
          ac._1 + pts(h.hand.nsScore, hand.nsScore),
          ac._2 + pts(h.hand.ewScore, hand.ewScore)
        )
      }
  }

  /**
    * @param hands all the hands that have been played.
    * @return Tuple2( ns, ew )  The international match points for the NS and EW team that played this hand.
    */
  def getInternationalMatchPoints(
      hands: List[HandsOnBoard]
  ): (Double, Double) = {
    def pts(other: Int, me: Int) = {
      if (other <= me) BoardScore.getIMPs(me - other)
      else -BoardScore.getIMPs(other - me)
    }
    val (ns, ew) = hands
      .filterNot(h => h.table == table && h.round == round && h.board == board)
      .foldLeft((0.0, 0.0)) { (ac, h) =>
        (
          ac._1 + pts(h.hand.nsScore, hand.nsScore),
          ac._2 + pts(h.hand.ewScore, hand.ewScore)
        )
      }
    val n = hands.length - 1
    (ns / n, ew / n)
  }

}

case class PlayerScore(
    player: ScoreboardPage.Player,
    points: Double,
    total: String,
    boards: List[String]
)

class AllHandsInMatch(
    val hands: List[HandsOnBoard],
    val players: List[String],
    val boardsets: Option[BoardSet],
    val movement: Option[IndividualMovement]
) {

  val playerMap = players.zipWithIndex.map( e =>
      (e._2+1, ScoreboardPage.Player(e._2 + 1, e._1))
  ).toMap

  val boards: List[Int] = hands.map(h => h.board).distinct.sorted

  val rounds: List[Int] = hands.map(h => h.round).distinct.sorted

  def getBoardsInRound(round: Int): List[Int] = {
    hands.filter(h => h.round == round).map(h => h.board).distinct.sorted
  }

  def getBoardsInTableRound(table: Int, round: Int): List[Int] = {
    hands
      .filter(h => h.round == round && h.table == table)
      .map(h => h.board)
      .distinct
      .sorted
  }

  def getHandsInTableRound(table: Int, round: Int): List[HandsOnBoard] = {
    hands
      .filter(h => h.round == round && h.table == table)
      .sortWith { (l,r) =>
        l.board < r.board
      }
  }

  def getBoard(table: Int, round: Int, board: Int): HandsOnBoard =
    hands.find(hob =>
      hob.board == board && hob.table == table && hob.round == round
    ) match {
      case Some(b) => b
      case None =>
        import org.scalatest.Assertions._
        fail(
          s"""Did not find a hand for table ${table} round ${round} board ${board}"""
        )
    }

  /**
    * Get the latest HandsOnBoard upto and including the specified round
    * @param toRound
    * @param board
    * @return None if board has not been played by anyone, Some(hob) the latest played.
    */
  def getBoardToRound(toRound: Int, board: Int): Option[HandsOnBoard] = {
    hands
      .filter { h => h.board == board && h.round <= toRound }
      .foldRight(None: Option[HandsOnBoard]) {
        (hob: HandsOnBoard, res: Option[HandsOnBoard]) =>
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

  /**
    * Get the latest HandsOnBoard upto and including the specified round
    * @param toRound
    * @param board
    * @return the boards that are played in the round
    */
  def getHandsInRound(round: Int): List[HandsOnBoard] = {
    hands
      .filter { h => h.round == round }
  }

  def getPlayerScoreToRound(
      player: Int,
      toRound: Int,
      viewtype: HandViewType,
      imp: Boolean = false
  ): PlayerScore = {
    var total = 0.0
    val bs = boards.map { b =>
      getBoardToRound(toRound, b) match {
        case Some(hob) =>
          val (mp, s) = hob.getPlayerScore(player, viewtype, this, imp)
          total += mp
          s
        case None =>
          // nothing has been played yet, must find one in later round to see if I'm not playing it
          getBoardToRound(rounds.last, b) match {
            case Some(hob) =>
              if (hob.doesPlayerPlay(player)) ""
              else Strings.xmark
            case None =>
              ""
          }
      }
    }
    PlayerScore(
      playerMap.get(player).get,
      total,
      if (imp) f"$total%.1f" else BoardPage.toPointsString(total),
      bs
    )
  }

  def getScoreToRound(
      toRound: Int,
      viewtype: HandViewType,
      imp: Boolean = false
  ): (List[PlayerScore], List[PlaceEntry]) = {
    val ts = playerMap.map(e => getPlayerScoreToRound(e._1, toRound, viewtype, imp)).toList
    val scores =
      ts.sortWith((l, r) => l.points > r.points).map(t => t.total).distinct
    HandPage.log.fine("Scores are " + scores)
    val pes = scores
      .map { sc => (sc, ts.filter(t => t.total == sc).map(t => t.player)) }
      .foldLeft((1, List[PlaceEntry]())) { (p, v) =>
        val (sc, players) = v
        val (nextPlace, places) = p
        (nextPlace + players.size, PlaceEntry(nextPlace, sc, players) :: places)
      }
    (ts, pes._2)
  }

  def getBoardFromBoardSet(board: Int): Option[BoardInSet] = {
    boardsets match {
      case Some(bs) =>
        bs.boards.find(b => b.id == board)
      case None => None
    }
  }

  def getHandsOnBoards(board: Int): List[HandsOnBoard] = {
    val h = hands.filter(h => h.board == board)
    HandPage.log.info(s"Board ${board}: hands ${h}")
    h
  }

  def getPlayersThatPlayBoard(board: Int): List[Int] = {
    getHandsOnBoards(board).flatMap(hob =>
      hob.hand.north :: hob.hand.south :: hob.hand.east :: hob.hand.west :: Nil
    )
  }

  def getPlayersThatDontPlayBoard(board: Int): List[Int] = {
    val played = getPlayersThatPlayBoard(board)
    (1 to players.size).filter(p => !played.contains(p)).toList
  }

  /**
    * Add in all the TeamNotPlayingHand for all hands where a team does not play the board.
    */
  def addPlayersNotPlayingBoards: AllHandsInMatch = {
    val h = hands.map { hob =>
      hob.addTeamNotPlayed(
        getPlayersThatDontPlayBoard(hob.board).map(t =>
          PlayerNotPlayingHand(hob.board, t)
        ): _*
      )
    }
    new AllHandsInMatch(h, players, boardsets, movement)
  }

  /**
    *  Fix the hands.  All the HandsOnBoard object don't have any others.
    *  This calculates the others for all boards.
    *  It will add either a OtherHandPlayed or OtherHandNotPlayed object depending on whether the hand has been played yet.
    *  Also calculates the match points in the OtherHandPlayed objects.
    *  It will also add TeamNotPlayingHand object if any team does not play the board.
    */
  def fixHands: AllHandsInMatch = {
    val nh = hands.map { hob =>
      val (played, notplayed) = getHandsOnBoards(hob.board)
        .filterNot(h => h.table == hob.table && h.round == hob.round)
        .foldLeft((List[HandsOnBoard](), List[HandsOnBoard]())) { (ac, h) =>
          if (h.round <= hob.round) (h :: ac._1, ac._2)
          else (ac._1, h :: ac._2)
        }
      val allplayed = hob :: played
      HandPage.log.info(s"allplayed is ${allplayed}")
      val (hobnsmp, hobewmp) = hob.getMatchPoints(allplayed)
      if (hob.hand.nsMP != hobnsmp || hob.hand.ewMP != hobewmp) {
        // error
        HandPage.log.severe(
          s"""nsMP=${hob.hand.nsMP} expect ${hobnsmp} ewMP=${hob.hand.ewMP} expect ${hobewmp} Match Points for ${hob} don't match what should be for played hands ${allplayed} """
        )
        fail(
          s"""nsMP=${hob.hand.nsMP} expect ${hobnsmp} ewMP=${hob.hand.ewMP} expect ${hobewmp} Match Points for ${hob} don't match what should be for played hands ${allplayed} """
        )
      }

      val otherplayed = played.map { oh =>
        val (ohnsmp, ohewmp) = oh.getMatchPoints(allplayed)
        val (ohnsimp, ohewimp) = oh.getInternationalMatchPoints(allplayed)
        OtherHandPlayed(
          oh.table,
          oh.round,
          oh.board,
          ohnsmp,
          ohewmp,
          ohnsimp,
          ohewimp
        )
      }
      val othernotplayed = notplayed.map { oh =>
        OtherHandNotPlayed(oh.table, oh.round, oh.board)
      }
      HandsOnBoard(
        hob.table,
        hob.round,
        hob.board,
        hob.hand,
        (otherplayed ::: othernotplayed): _*
      )
    }

    new AllHandsInMatch(nh, players, boardsets, movement).addPlayersNotPlayingBoards
  }

  def checkFixHands: AllHandsInMatch = {
    val calc = fixHands

    hands.foreach { hob =>
      val calcboard = calc.getBoard(hob.table, hob.round, hob.board)
      if (hob != calcboard) {
        HandPage.log.severe(
          s"Oops fixHands has a problem at ${hob}, calculated ${calcboard}"
        )
        fail(s"Oops fixHands has a problem at ${hob}, calculated ${calcboard}")
      }
    }
    calc.logit
    calc
  }

  def logit: Unit = {
    val hnds = hands.sortWith { (l, r) =>
      if (l.round < r.round) true
      else if (l.round > r.round) false
      else {
        if (l.board < r.board) true
        else if (l.board > r.board) false
        else {
          l.table < r.table
        }
      }
    }

    def toStrHob(hob: HandsOnBoard): String = {
      def toStrOther(other: Seq[OtherHand]): String = {
        other.mkString("\n    ", "\n    ", "")
      }
      s"""HandsOnBoard(table=${hob.table}, round=${hob.round}, board=${hob.board}, hand=${hob.hand})${toStrOther(hob.other)}"""
    }

    def toStr(hs: List[HandsOnBoard] ): String = {
      hs.map { hob =>
        toStrHob(hob)
      }.mkString("  ", "\n  ", "")
    }

    HandPage.log.fine(
      s"AllHandsInMatch\n${toStr(hnds)}"
    )
  }

  /**
    * @return Tuple2( ns, ew )
    */
  def getNSEW(table: Int, round: Int): (Int, Int, Int, Int) = {
    val boards = getBoardsInTableRound(table, round)
    val b = getBoard(table, round, boards.head)
    (b.hand.north, b.hand.south, b.hand.east, b.hand.west)
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
  def apply(
      players: List[String],
      boardsets: BoardSet,
      movement: IndividualMovement,
      handsOnBoard: List[EnterHand]*
  ): AllHandsInMatch = {
    val boards = movement.getBoards
    if (boards.size != handsOnBoard.size)
      throw new IllegalArgumentException(
        s"The number of handsOnBoard (${handsOnBoard.size}) must be equal number of boards (${boards.size})"
      )
    val hands: List[HandsOnBoard] =
      movement.getBoards.zip(handsOnBoard).flatMap {
        case (board, hob) =>
          val wp = movement.wherePlayed(board)
          if (wp.size != hob.size)
            throw new IllegalArgumentException(
              s"Board ${board}: The number of hands (${hob.size}) must be equal number of times played (${wp.size})"
            )
          wp.zip(hob).map {
            case (wherePlayed, hand) =>
              HandsOnBoard(wherePlayed.table, wherePlayed.round, board, hand)
          }
      }
    new AllHandsInMatch(hands, players, Some(boardsets), Some(movement)).fixHands
  }

}
