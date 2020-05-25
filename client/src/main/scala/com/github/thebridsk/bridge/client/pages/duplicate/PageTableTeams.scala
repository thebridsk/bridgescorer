package com.github.thebridsk.bridge.client.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.data.bridge.DuplicateViewPerspective
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore.Round
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.bridge.client.pages.info.InfoPage
import com.github.thebridsk.bridge.clientcommon.react.ComboboxOrInput
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableTeamView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableTeamByBoardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableTeamByRoundView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseBoardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.data.MatchPlayerPosition
import com.github.thebridsk.bridge.data.maneuvers.TableManeuvers
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge._
import scala.scalajs.js.JSConverters._
import com.github.thebridsk.bridge.clientcommon.react.Button
import com.github.thebridsk.bridge.client.pages.hand.PageHand
import com.github.thebridsk.bridge.client.pages.hand.{ Properties => HProperties}
import com.github.thebridsk.bridge.client.pages.Pixels
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageTableTeams( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageTableTeams {
  import PageTableTeamsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: TableTeamView ) {
  }

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: TableTeamView ) = component(Props(routerCtl,page))

}

object PageTableTeamsInternal {
  import PageTableTeams._
  import DuplicateStyles._

  val logger = Logger("bridge.PageTableTeams")

  /**
   * The names as they are stored in the Team class.
   */
  case class Names( ns1: String, ns2: String, ew1: String, ew2: String ) {
    def playerValid( p: String ) = p!=null && p.length()>0
    def isNSMissing = !playerValid(ns1) || !playerValid(ns2)
    def isEWMissing = !playerValid(ew1) || !playerValid(ew2)

    def isAllValid = !isNSMissing && !isEWMissing && areAllPlayersUnique()
    def isMissingOneTeam = isNSMissing != isEWMissing
    def isMissingAll = isNSMissing && isEWMissing

    /**
     * Set a player
     */
    def setPlayer( pos: PlayerPosition, name: String ) =
      pos match {
        case North => copy( ns1 = name )
        case South => copy( ns2 = name )
        case East => copy( ew1 = name )
        case West => copy( ew2 = name )
      }

    def areAllPlayersUnique() = {
      val p =
        ns1.trim ::
        ns2.trim ::
        ew1.trim ::
        ew2.trim ::
        Nil
      val before = p.length
      val after = p.distinct.length
      before == after
    }


    def tableManeuvers = TableManeuvers(ns1,ns2,ew1,ew2)
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   * This class has three major states:
   *   1) Enter missing names (only one team can be missing)
   *       enteringMissingNames && (players.areNSPlayersValid != players.areEWPlayersValid)
   *
   *       call setPlayer(pos,name) for two missing names
   *
   *   2) Select/Enter scorekeeper
   *       !scorekeeperSet || scorekeeperPosition.isEmpty || scorekeeperName.isEmpty
   *
   *       setScorekeeperName(name)
   *       setScorekeeperPosition(pos)
   *       okScorekeeper
   *
   *   3) Select/Enter remaining names
   *       rest
   *
   *       substates:
   *
   *        a) enter names
   *
   *          setPlayer(posL,name)
   *          setPlayer(posR,name)
   *          setPlayer(posP,name)
   *          getPositions
   *
   *        b) select names
   *
   *          swapLeftRight
   *          getPositions
   *
   */
  case class State( originalNames: Names,           // the original names when this was created.
                    names: Names,                   // current set of names (different when entering one team only)
                    nsTeam: Id.Team,
                    ewTeam: Id.Team,
                    enteringMissingNames: Boolean,  // entering names of missing team
                    inputNames: Boolean,            // input fields when renderNames
                    page: TableTeamView,            //
                    players: TableManeuvers,        // the current positions of the players

                    scorekeeperPosition: Option[PlayerPosition],
                    scorekeeperName: Option[String],
                    scorekeeperSet: Boolean,

                    nameSuggestions: Option[List[String]] = None         // known names from server
  ) {

    def getSuggestions = nameSuggestions.getOrElse(List()).toJSArray
    def gettingNames = nameSuggestions.isEmpty

    def isEnteringMissingNames = enteringMissingNames // ( players.areNSPlayersValid != players.areEWPlayersValid )
    def isSelectingScorekeeper = !scorekeeperSet || scorekeeperName.isEmpty || scorekeeperPosition.isEmpty

    def setPlayer( pos: PlayerPosition, name: String ) = {
      copy( players = players.setPlayer(pos, name), names=names.setPlayer(pos,name))
    }

    def setMissingNames = copy( enteringMissingNames = false, inputNames = false )

    def setScorekeeperPosition( pos: PlayerPosition ) = copy( scorekeeperPosition=Some(pos) )
    def setScorekeeperName( name: String ) = copy( scorekeeperName=Some(name) )
    def okScorekeeper = {
      if ( scorekeeperPosition.isDefined && scorekeeperName.isDefined ) {
        if ( players.find(scorekeeperPosition.get) != scorekeeperName.get ) {
          if (players.isPlayerValid(scorekeeperPosition.get)) {
            copy( players = players.rotate180(), scorekeeperSet = true )
          } else {
            copy( players = players.setPlayer(scorekeeperPosition.get, scorekeeperName.get),
                  names = names.setPlayer(scorekeeperPosition.get, scorekeeperName.get),
                  scorekeeperSet = true)
          }
        } else {
          // scorekeeper already in correct position
          copy( scorekeeperSet = true)
        }
      } else {
        // not valid to hit okScorekeeper
        this
      }
    }

    def isAllValid = players.areAllPlayersValid()

    def isCurrentValid( pos: PlayerPosition ) = pos match {
      case North | South => players.areNSPlayersValid()
      case East | West => players.areEWPlayersValid()
    }

    def getTeam( pos: PlayerPosition ) = pos match {
      case North | South => Id.teamIdToTeamNumber(nsTeam)
      case East | West => Id.teamIdToTeamNumber(ewTeam)
    }

    def swapLeftRight = copy( players = players.swapRightAndLeftOf(scorekeeperPosition.get))

    def swapWithPartner(pos: PlayerPosition) = copy( players = players.swapWithPartner(pos) )

    /**
     * @return true if original is not set, or if original and name are equal
     */
    def compare( original: String, name: String ) = {
      if (playerValid(original)) {
        original == name
      } else {
        true
      }
    }

    /**
     * @return tuple2( northIs1, eastIs1 )
     * True indicates name not set in original, or the north/east player is the first player in team.
     */
    def getPositions: (Boolean, Boolean) = {
      ( compare( originalNames.ns1, players.north ), compare( originalNames.ew1, players.east) )
    }

    def updateNames( list: List[String] ) = copy( nameSuggestions = Some(list) )

    def logState( comment: String ) = {
      logger.fine( s"""${comment}: ${this}""")
      this
    }
  }

  object State {

    private
    def state( originalNames: Names,
               page: TableTeamView,
               nsTeam: Id.Team,
               ewTeam: Id.Team,
               players: Option[TableManeuvers] = None,
               suggestions: Option[List[String]] = None
             ): State = {

      new State( originalNames,
                 originalNames,
                 nsTeam,
                 ewTeam,
                 originalNames.isMissingOneTeam,
                 !originalNames.isAllValid,
                 page,
                 players.getOrElse( originalNames.tableManeuvers ),
                 None,
                 None,
                 false,
                 suggestions
               )
    }

    private
    def state( page: TableTeamView ): State = state( Names("","","",""), page, "","" )

    def invalid( props: Props ) = {
      logger.info("PageTableTeams: Invalid props "+props)
      state( props.page )
    }

    def findBoardsInRound( md: MatchDuplicate, tableid: Id.Table, round: Int ) = {
      var hands: List[DuplicateHand] = Nil
      for (bb <- md.boards) {
        for (hh <- bb.hands) {
          if (hh.round==round && hh.table==tableid) {
            hands = hh::hands
          }
        }
      }
      hands
    }

    /**
     * Determine the teams are the same as the previous round, and in the same NS, EW,
     * If they are the same, then return the nIsPlayer1 and eIsPlayer1 from the previous round,
     * otherwise return the specified values for nIsPlayer1 and eIsPlayer1.
     * @param md
     * @param tableid
     * @param currentRound
     * @param nsTeam
     * @param ewTeam
     * @param nIsPlayer1
     * @param eIsPlayer1
     * @return A tuple, (nIsPlayer1, eIsPlayer1)
     */
    def determinePosition( md: MatchDuplicate, tableid: Id.Table, currentRound: Int, nsTeam: Id.Team, ewTeam: Id.Team, nIsPlayer1: Boolean, eIsPlayer1: Boolean ) = {
      if (currentRound == 1) {
        logger.fine(s"PageTableTeams.determinePosition: In round 1 table ${tableid}, returning ($nIsPlayer1, $eIsPlayer1)")
        (nIsPlayer1, eIsPlayer1)  // no previous round
      }
      else {
        val hands = findBoardsInRound(md, tableid, currentRound-1)
        if (hands.isEmpty) {
          logger.fine(s"PageTableTeams.determinePosition: CurrentRound is ($currentRound) table ${tableid}, did not find hands in previous round, returning ($nIsPlayer1, $eIsPlayer1)")
          (nIsPlayer1, eIsPlayer1)   // could not find previous round (should never happen)
        }
        else {
          val hand = hands.head
          if (nsTeam != hand.nsTeam || ewTeam != hand.ewTeam) {
            logger.fine(s"PageTableTeams.determinePosition: CurrentRound is ($currentRound) table ${tableid}, Not the same teams ${nsTeam}!=${hand.nsTeam} || ${ewTeam}!=${hand.ewTeam}, returning ($nIsPlayer1, $eIsPlayer1)")
            (nIsPlayer1, eIsPlayer1)   // not the same teams
          }
          else {
            logger.fine(s"PageTableTeams.determinePosition: CurrentRound is ($currentRound) table ${tableid}, same teams $nsTeam $ewTeam returning same as previous round (${hand.nIsPlayer1}, ${hand.eIsPlayer1}) not ($nIsPlayer1, $eIsPlayer1)")
            (hand.nIsPlayer1, hand.eIsPlayer1)
          }
        }
      }
    }

    def createOld( props: Props ): State = {
      DuplicateStore.getMatch() match {
        case Some(md) =>
          props.page match {
            case TableTeamByBoardView( dupid, tableid, round, boardid ) =>
              md.getBoard(boardid) match {
                case Some(b) =>
                  val h = b.hands.find { h => h.table==tableid && h.round==round }
                  h match {
                    case Some(hand) =>
                      val hands = findBoardsInRound(md, tableid, round)
                      val (nIsPlayer1, eIsPlayer1) = determinePosition(md, tableid, round, hand.nsTeam, hand.ewTeam, hand.nIsPlayer1, hand.eIsPlayer1)
                      val MatchPlayerPosition(north,south,east,west,allspecified,_,_) = md.determinePlayerPositionFromCaller(hand, nIsPlayer1, eIsPlayer1)
                      val MatchPlayerPosition(origNS1, origNS2, origEW1, origEW2, xxxx,_,_) = md.determinePlayerPositionFromCaller(hand, true, true)
                      logger.fine(s"PageTableTeams.create: NS: $nIsPlayer1 $north $south, EW: $eIsPlayer1 $east $west")
                      logger.info("PageTableTeams: hand is "+hand)
                      val originalNames = Names(origNS1, origNS2, origEW1, origEW2)
                      val players = TableManeuvers(north,south,east,west)
                      state(originalNames,props.page,hand.nsTeam,hand.ewTeam,Some(players)).logState("PageTableTeams.State.create with boardid")
                    case None => State.invalid(props)
                  }
                case None => State.invalid(props)
              }
            case TableTeamByRoundView( dupid, tableid, round ) =>
              val hands = findBoardsInRound(md, tableid, round)
              if (hands.isEmpty) {
                State.invalid(props)
              } else {
                val hand = hands.head
                val (nIsPlayer1, eIsPlayer1) = determinePosition(md, tableid, round, hand.nsTeam, hand.ewTeam, hand.nIsPlayer1, hand.eIsPlayer1)
                val MatchPlayerPosition(north,south,east,west,allspecified,_,_) = md.determinePlayerPositionFromCaller(hand, nIsPlayer1, eIsPlayer1)
                val MatchPlayerPosition(origNS1, origNS2, origEW1, origEW2, xxxx,_,_) = md.determinePlayerPositionFromCaller(hand, true, true)
                logger.fine(s"PageTableTeams.create: NS: $nIsPlayer1 $north $south, EW: $eIsPlayer1 $east $west")
                logger.info("PageTableTeams: hand is "+hand)
                val originalNames = Names(origNS1, origNS2, origEW1, origEW2)
                val players = TableManeuvers(north,south,east,west)
                state(originalNames,props.page,hand.nsTeam,hand.ewTeam,Some(players)).logState("PageTableTeams.State.create from round")
              }
          }
        case None =>
          State.invalid(props)
      }
    }

    def getTableManeuvers( md: MatchDuplicate, tableid: String, currentround: Int ): Option[(Team,Team,TableManeuvers)] = {

      val hands = findBoardsInRound(md, tableid, currentround)
      hands.headOption.map { hand =>
        val ns = md.getTeam(hand.nsTeam).get
        val ew = md.getTeam(hand.ewTeam).get
        val (n,s) = if (hand.nIsPlayer1) (ns.player1,ns.player2) else (ns.player2,ns.player1)
        val (e,w) = if (hand.eIsPlayer1) (ew.player1,ew.player2) else (ew.player2,ew.player1)
        val rtm = if (currentround == 1) {
          Some(TableManeuvers(n,s,e,w))
        } else {
          if (hands.find( dh => !dh.played.isEmpty ).isEmpty) {
            // never played
            val prevhands = findBoardsInRound(md, tableid, currentround-1)
            prevhands.headOption.map { prevhand =>
              if ( (hand.nsTeam == prevhand.nsTeam && hand.ewTeam == prevhand.ewTeam)
                   || (hand.nsTeam == prevhand.ewTeam && hand.ewTeam == prevhand.nsTeam)
              ) {
                // same teams as previous round
                val pns = md.getTeam(prevhand.nsTeam).get
                val pew = md.getTeam(prevhand.ewTeam).get
                val (pn,ps) = if (prevhand.nIsPlayer1) (pns.player1,pns.player2) else (pns.player2,pns.player1)
                val (pe,pw) = if (prevhand.eIsPlayer1) (pew.player1,pew.player2) else (pew.player2,pew.player1)
                val tm = TableManeuvers(pn,ps,pe,pw)
                val tm2 = if (hand.nsTeam == prevhand.nsTeam) tm else tm.rotateClockwise
                Some(tm2)
              } else {
                // different teams
                Some(TableManeuvers(n,s,e,w))
              }
            }.getOrElse {
              // could not find previous round on table.  This should not happen
              Some(TableManeuvers(n,s,e,w))
            }
          } else {
            // this has been played already
            Some(TableManeuvers(n,s,e,w))
          }
        }
        rtm.map( tm => (ns,ew,tm) )
      }.getOrElse( None )  // no hands in round
    }

    def create( props: Props ): State = {
      DuplicateStore.getMatch().map { md =>
        (props.page match {
          case TableTeamByBoardView( dupid, tableid, round, boardid ) => Some( dupid, tableid, round )
          case TableTeamByRoundView( dupid, tableid, round ) => Some( dupid, tableid, round )
          case _ => None
        }).map { vls =>
          val (dupid, tableid, round) = vls
          val tm = getTableManeuvers(md,tableid,round)
          tm match {
            case Some((nsteam,ewteam,tm)) =>
              val TableManeuvers(n,s,e,w) = tm
              val names = Names(nsteam.player1,nsteam.player2,ewteam.player1,ewteam.player2)
              state(names,props.page,nsteam.id,ewteam.id,Some(tm)).logState("PageTableTeams.State.create from round")
            case _ =>
              State.invalid(props)
          }
        }.getOrElse( State.invalid(props))
      }.getOrElse( State.invalid(props))
    }
  }


                                 //            nsew           swapid arrow  swap             setPlayer(l)(p)  tabindex readonly showpos
  val Position = ScalaComponent.builder[(State,PlayerPosition,String,String,Option[Callback],String=>Callback,Int,     Boolean, Boolean)]("Position")
                      .render_P( args => {
                        val (state,nsew,swapid,arrow,swap,setPlayer,tabindex,readonly,showpos) = args

                        val busy = state.gettingNames
                        val names = state.getSuggestions
                        val team = state.getTeam(nsew)
                        val playername = state.players.find(nsew)

                        <.span(
                          ^.textAlign := "left",
                          readonly ?= <.br,
                          if (showpos) {
                            TagMod(
                              <.b(nsew.name),
                              s" (Team ${team})"
                            )
                          } else {
                            EmptyVdom    // s"Team ${team}"
                          },
                          <.br(),
                          if (readonly) {
                            Seq[TagMod](
                              <.b( ^.id:="Player_"+nsew.pos, playername),
                              swap.toList.flatMap( sw => Seq[TagMod](
                                  <.br,
                                  AppButton( swapid, "Swap "+ arrow, ^.onClick-->sw )
                                  )).toTagMod
                            ).toTagMod
                          } else {
                            <.div(
                              dupStyles.inputTableNames,
                              ComboboxOrInput( setPlayer, noNull(playername), names, "startsWith", tabindex, "I_"+nsew.pos,
                                               msgEmptyList="No suggested names", msgEmptyFilter="No names matched", busy=busy ),
                              BaseStyles.highlight(required = !playerValid(playername))
                            )
                          }
                        )
                      }).build

  private def noNull( s: String ) = Option(s).getOrElse("")
  private def playerValid( s: String ) = s!=null && s.length!=0

//  private val southArrow = Strings.doubleArrowDown
//  private val northArrow = Strings.doubleArrowUp
//  private val eastArrow = Strings.doubleArrowRight
//  private val westArrow = Strings.doubleArrowLeft

  private val southArrow = Strings.arrowUpDown
  private val northArrow = Strings.arrowUpDown
  private val eastArrow = Strings.arrowRightLeft
  private val westArrow = Strings.arrowLeftRight

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setPlayer( pos: PlayerPosition )( name: String ) = scope.modState { s => s.setPlayer(pos, name) }

    def swapWithPartner( pos: PlayerPosition ) = scope.modState { s => s.swapWithPartner(pos) }

    val setMissingNames = scope.modState{ s => s.setMissingNames }

    def setScorekeeperPosition( pos: PlayerPosition ) = scope.modState{ s => s.setScorekeeperPosition(pos) }

    def setScorekeeperName( name: String ) = scope.modState{ s => s.setScorekeeperName(name) }

    val setScorekeeper = scope.modState{ s => s.okScorekeeper }

    def getHand( md: MatchDuplicate, page: TableTeamView ) = page match {
      case TableTeamByBoardView( dupid, tableid, round, boardId ) =>
        md.getHand(tableid, round, boardId)
      case TableTeamByRoundView( dupid, tableid, round ) =>
        md.getHandsInRound(tableid, round).headOption
    }

    def findBoardsInRound( md: MatchDuplicate, tableid: Id.Table, round: Int ) = {
      var hands: List[DuplicateHand] = Nil
      for (bb <- md.boards) {
        for (hh <- bb.hands) {
          if (hh.round==round && hh.table==tableid) {
            hands = hh::hands
          }
        }
      }
      hands
    }

    def getHandsInRound( page: TableTeamView ) =
      DuplicateStore.getMatch() match {
        case Some(md) => page match {
          case TableTeamByBoardView( dupid, tableid, round, boardId ) =>
            md.getHandsInRound(tableid, round)
          case TableTeamByRoundView( dupid, tableid, round ) =>
            md.getHandsInRound(tableid, round)
        }
        case None => List()
      }

    val ok =
      // update the team players if they were entered, and/or update position of players.
      scope.stateProps { (s,props) =>
        s.scorekeeperPosition match {
          case Some(sk) => PageHand.scorekeeper = sk
          case None =>
        }
        val nsid = DuplicateStore.getMatch() match {
          case Some(dup) =>
            logger.fine(s"OK: state=$s, dup=$dup")
            getHand(dup,s.page) match {
              case Some(hand) =>
                if (s.originalNames.isNSMissing) setPlayersOnTeam(dup, hand.nsTeam, s.players.north, s.players.south)
                if (s.originalNames.isEWMissing) setPlayersOnTeam(dup, hand.ewTeam, s.players.east, s.players.west)

                val (nIsPlayer1, eIsPlayer1) = s.getPositions

                getHandsInRound(s.page).foreach { h => {
                  logger.fine("PageTableTeams.Backend.setAllPlayers.Callback: "+h)
                  if (nIsPlayer1 != h.nIsPlayer1 || eIsPlayer1 != h.eIsPlayer1) {
                    val nh = h.setPlayer1North(nIsPlayer1).setPlayer1East(eIsPlayer1)
                    logger.fine("PageTableTeams.Backend.setAllPlayers.Callback: Setting players "+nh)
                    Controller.updateHand(dup, nh)
                  }
                }}
                hand.nsTeam
              case None =>
                logger.fine(s"OK: state=$s, hand not found")
                ""
            }
          case None =>
            logger.fine(s"OK: state=$s, no duplicate match")
            ""
        }
        props.routerCtl.set(props.page.toNextView() match {
          case p: BaseBoardView =>
            if (nsid.length()>0) p.toHandView(nsid)
            else p
          case p => p
        })
      }

    def setPlayersOnTeam( dup: MatchDuplicate, teamid: Id.Team, player1: String, player2: String ) = {
      val newteam = dup.getTeam(teamid) match {
        case Some(oldteam) => oldteam.setPlayers(player1.trim, player2.trim)
        case None => Team.create(teamid, player1.trim, player2.trim)
      }
      Controller.updateTeam(dup, newteam)
    }

    val reset = scope.modState((s,props)=> State.create(props).copy( nameSuggestions = s.nameSuggestions).logState("PageTableTeams.Backend.reset"))

    def header( props: Props, helpurl: String ) = {
        DuplicatePageBridgeAppBar(
          id = Some(props.page.dupid),
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      s"Players for Table ${props.page.tableid} Round ${props.page.round}",
                    )
                )),
          helpurl = helpurl,
          routeCtl = props.routerCtl
        )(

        )
    }

    def render( props: Props, state: State ) = {
      logger.fine("PageTableTeams.Backend.render state="+state )
      <.div(
        DuplicateStore.getCompleteView() match {
          case Some(score) if score.id == props.page.dupid =>
            score.tables.get(props.page.tableid) match {
              case Some(rounds) =>
                val readonly = state.originalNames.isAllValid && !props.page.editPlayers
                rounds.find { r => r.round==props.page.round } match {
                  case Some(round) =>
                    renderInput(props, state, score, round )
                  case None =>
                    TagMod(
                      header(props, "../help/duplicate/enterscorekeepername.html"),
                      <.div(
                        dupStyles.divTableNamesPage,
                        <.p("Round "+props.page.round+" not found on Table "+props.page.tableid),
                        <.p(
                          Button( baseStyles.footerButton, "Game", "Scoreboard", props.routerCtl.setOnClick(CompleteScoreboardView(props.page.dupid)) )
                        )
                      )
                    )
                }
              case None =>
                TagMod(
                  header(props, "../help/duplicate/enterscorekeepername.html"),
                  <.div(
                    dupStyles.divTableNamesPage,
                    <.p("Table "+props.page.tableid+" not found"),
                    <.p(
                      Button( baseStyles.footerButton, "Game", "Scoreboard", props.routerCtl.setOnClick(CompleteScoreboardView(props.page.dupid)) )
                    ) )
                )
            }
          case _ =>
            TagMod(
              header(props, "../help/duplicate/enterscorekeepername.html"),
              <.div(
                dupStyles.divTableNamesPage,
                HomePage.loading
              )
            )
        }
      )

    }

    def renderInput( props: Props, state: State, score: MatchDuplicateScore, round: MatchDuplicateScore.Round ) = {
      val ( okCB, valid, div, helppage, errormsg ) = {
        if (state.isEnteringMissingNames) renderEnterMissing(props, state)
        else if (state.isSelectingScorekeeper) renderScorekeeper(props, state)
        else renderNames(props, state, score, round)
      }

      TagMod(
        header(props, helppage.getOrElse("../help/duplicate/enterscorekeepername.html")),
        <.div(
            dupStyles.divTableNamesPage,
            <.div(
              <.div(
                div,
                <.div(
                  ^.id:="ErrorMsg",
                  <.p(
                    errormsg.whenDefined
                  )
                ),
                <.div(
                    baseStyles.divFooter,
                    <.div(
                        baseStyles.divFooterLeft,
                        Button( baseStyles.footerButton,
                                "OK",
                                "OK",
                                ^.onClick --> okCB,
                                ^.disabled := !valid,
                                BaseStyles.highlight(required = valid)
                              )
                    ),
                    <.div(
                        baseStyles.divFooterCenter,
                        Button( baseStyles.footerButton, "Reset", "Reset", ^.onClick --> reset )
                    ),
                    <.div(
                        baseStyles.divFooterRight,
                        Button( baseStyles.footerButton, "Cancel", "Cancel", props.routerCtl.setOnClick( props.page.toTableView() ) ),
    //                    helppage.whenDefined( p => HelpButton(p) )
                    )
                )
              ),
            )
        )
      )

    }

    def renderEnterMissing( props: Props, state: State ): (Callback, Boolean, TagMod, Option[String], Option[String]) = {
      val ( ns, ew ) = (
                         (North,South,state.names.ns1,state.names.ns2,state.nsTeam),
                         (East,West,state.names.ew1,state.names.ew2,state.ewTeam)
                       )
      val ((mpos1,mpos2,mname1,mname2,mteamid),(vpos1,vpos2,vname1,vname2,vteamid)) = {
        if (state.players.areEWPlayersValid()) {
          (ns,ew)
        } else {
          (ew,ns)
        }
      }
      val valid = playerValid(mname1) && playerValid(mname2) && state.names.areAllPlayersUnique()

      val errormsg = if (valid) None
                     else if (!playerValid(mname1) || !playerValid(mname2)) Some("Please enter missing player name(s)")
                     else if (!state.names.areAllPlayersUnique()) Some("Please fix duplicate player names")
                     else Some("Unknown error")

      val div =
          <.div(
              <.h1(s"Enter player names for team ${Id.teamIdToTeamNumber(mteamid)}"),
              <.span(
                  s"Playing ${mpos1.name} ${mpos2.name}",
                  <.p(),
                  Position(state,mpos1,"","",None,setPlayer(mpos1),1,false,false),
                  <.p(),
                  Position(state,mpos2,"","",None,setPlayer(mpos2),2,false,false),
                  <.p(),
                  <.p,
                  s"Playing against team ${Id.teamIdToTeamNumber(vteamid)}, ${vname1} ${vname2}"
              )
          )
      ( setMissingNames, valid, div, None, errormsg )
    }

    def renderScorekeeper( props: Props, state: State ): (Callback, Boolean, TagMod, Option[String], Option[String]) = {
      val valid = state.scorekeeperName.isDefined && state.scorekeeperPosition.isDefined
      val (div, helppage, errormsg) = {
        if (state.names.isAllValid) renderSelectScorekeeper(props, state, valid)
        else renderEnterScorekeeper(props, state, valid)
      }

      ( setScorekeeper, valid, div, helppage, errormsg)
    }

    def renderEnterScorekeeper( props: Props, state: State, valid: Boolean ): (TagMod, Option[String], Option[String]) = {
      val names = state.getSuggestions
      val busy = state.gettingNames
      val playername = state.scorekeeperName.getOrElse("")
      val np = noNull(playername)
      val errormsg = if (valid) None
                     else if (state.scorekeeperName.isEmpty) Some("Please enter scorekeeper's name")
                     else if (state.scorekeeperPosition.isEmpty) Some("Please select scorekeeper's position")
                     else Some("Unknown error")
      ( <.div(
          <.h1( "Enter scorekeeper:" ),
          <.div(
            dupStyles.inputTableNames,
            ComboboxOrInput( setScorekeeperName, np, names, "startsWith", 1, "Scorekeeper",
                             msgEmptyList="No suggested names", msgEmptyFilter="No names matched", busy=busy ),
            BaseStyles.highlight(required = state.scorekeeperName.isEmpty)
          ),
          <.h1( "Enter scorekeeper's position:" ),
          (North::South::East::West::Nil).map( pos => {
            val selected = state.scorekeeperPosition match {
              case Some(sk ) => sk == pos
              case None => false
            }
            Button( baseStyles.footerButton,
                    "SK_"+pos.pos,
                    pos.name,
                    ^.onClick --> setScorekeeperPosition(pos),
                    BaseStyles.highlight(
                        selected = selected,
                        required = state.scorekeeperPosition.isEmpty
                    )
                  )
          }).toTagMod
        ),
        Some("../help/duplicate/enterscorekeepername.html"),
        errormsg
      )

    }

    def renderSelectScorekeeper( props: Props, state: State, valid: Boolean ): (TagMod, Option[String], Option[String]) = {
      val extraWidth = HProperties.defaultHandButtonBorderRadius+
                       HProperties.defaultHandButtonPaddingBorder
      val width = s"${Pixels.maxLength( state.players.players(): _* )+extraWidth}px"
      val bwidth: TagMod = ^.width := width
      val errormsg = if (valid) None
                     else if (state.scorekeeperName.isEmpty) Some("Please select scorekeeper")
                     else if (state.scorekeeperPosition.isEmpty) Some("Please select scorekeeper's position")
                     else Some("Unknown error")
      ( <.div(
          <.h1( "Enter scorekeeper:" ),
          state.players.sortedPlayers().map( p => {
            val selected = state.scorekeeperName match {
              case Some(sk ) => sk == p
              case None => false
            }
            AppButton( "P_"+p,
                       p,
                       bwidth,
                       ^.onClick --> setScorekeeperName(p),
                       BaseStyles.highlight(
                           selected = selected,
                           required = state.scorekeeperName.isEmpty
                       )
                     )
          }).toTagMod,
          <.h1( "Enter scorekeeper's position:" ),
          state.scorekeeperName match {
            case Some(sk) =>
              val skpos = state.players.find(sk).get
              val partner = state.players.partnerOfPosition(skpos)
              (skpos::partner::Nil).map( pos => {
                val selected = state.scorekeeperPosition match {
                  case Some(sk ) => sk == pos
                  case None => false
                }
                Button( baseStyles.footerButton,
                        "SK_"+pos.pos,
                        pos.name,
                        ^.onClick --> setScorekeeperPosition(pos),
                        BaseStyles.highlight(
                            selected = selected,
                            required = state.scorekeeperPosition.isEmpty
                        )
                      )
              }).toTagMod
            case None =>
              EmptyVdom
          }
        ),
        Some("../help/duplicate/selectscorekeepername.html"),
        errormsg
      )
    }

    def renderNames( props: Props, state: State, score: MatchDuplicateScore, round: MatchDuplicateScore.Round ): (Callback, Boolean, TagMod, Option[String], Option[String]) = {
      val scorekeeper = state.scorekeeperPosition.getOrElse(North)
      val partner = state.players.partnerOfPosition(scorekeeper)
      val left = state.players.leftOfPosition(scorekeeper)
      val right = state.players.rightOfPosition(scorekeeper)

      val readonly = !state.inputNames
      val allvalid = readonly || state.names.isAllValid
      val errormsg = if (allvalid) None
                     else if (state.names.isNSMissing || state.names.isEWMissing) Some("Please enter missing player name(s)")
                     else if (!state.names.areAllPlayersUnique()) Some("Please fix duplicate player names")
                     else Some("Unknown error")

      logger.fine(s"""renderNames: allvalid=${allvalid}, readonly=${readonly} state.names=${state.names}""")

      /*
       * Returns the html for the player position.  This could be an input field, or just names with swap buttons.
       * @param pos the player position being rendered
       * @param tabindex the tab index
       * @param swapArrow if None no swap button is created.  If some string, then a swap button is created with
       *                  the string as the "arrow"
       */
      def getPlayerButton( pos: PlayerPosition, tabindex: Int, swapArrow: Option[String], swapid: String, ro: Boolean = readonly ) = {

        val (arrow,swapfun) = swapArrow match {
          case Some(a) => (a, Some( swapWithPartner(pos) ))
          case None => ("", None)
        }

        Position( (state,pos,s"Swap_$swapid",arrow,swapfun,setPlayer(pos),tabindex,ro,true) )
      }

      def getSwapArrow( pos: PlayerPosition, arrow: String ) = {
        if (state.isCurrentValid(pos)) Some(arrow)
        else None
      }

      val helpurl = if (readonly) "../help/duplicate/selectothernames.html"
                     else "../help/duplicate/enterothernames.html"

      val div =
          <.div(
            !readonly ?= <.h1(InfoPage.showOnlyInLandscapeOnTouch(), "Rotate to portrait for a better view"),
            <.table( tableStyles.tableWidthPage,
              <.tbody(
                <.tr(
                  <.td(tableStyles.tableFloatLeft, <.b("Enter players")),
                  <.td(tableStyles.tableFloatRight, "NS is team "+state.nsTeam+", EW is team "+state.ewTeam)
                )
              )
            ),
            <.table( tableStyles.tableWidthPage,
              <.tbody(
                <.tr(
                  <.td( ^.colSpan:=2, tableStyles.tableCellWidth2Of7),
                  <.td( ^.colSpan:=3, tableStyles.tableCellWidth3Of7,
                        getPlayerButton( partner, 2, None, "top" )
                      ),
                  <.td( ^.colSpan:=2, tableStyles.tableCellWidth2Of7 )
                ),
                <.tr(
                  <.td(
                    ^.colSpan:=3,
                    tableStyles.tableCellWidth3Of7,
                    getPlayerButton( left, 1, getSwapArrow(left, eastArrow), "left" )
                  ),
                  <.td(
                    ^.colSpan:=1,
                    tableStyles.tableCellWidth1Of7
                  ),
                  <.td(
                    ^.colSpan:=3,
                    tableStyles.tableCellWidth3Of7,
                    getPlayerButton( right, 3, getSwapArrow(right, westArrow), "right" )
                  )
                ),
                <.tr(
                  <.td( ^.colSpan:=2, tableStyles.tableCellWidth2Of7 ),
                  <.td( ^.colSpan:=3, tableStyles.tableCellWidth3Of7,
                        getPlayerButton( scorekeeper, 4, None, "bottom", true )
                  ),
                  <.td( ^.colSpan:=2, tableStyles.tableCellWidth2Of7 )
                )
              )
            )
          )

      (
        ok,
        allvalid,
        div,
        Some(helpurl),
        errormsg
      )
    }

    val storeCallback =
      scope.modState( (s, props) => {
        val newState = State.create(props)
        if (newState.originalNames == s.originalNames) {
          s
        } else {
          newState
        }
      })

    val namesCallback = scope.modState(s => {
      val sug = NamesStore.getNames
      s.copy( nameSuggestions=Some(sug))
    })

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("PageTableTeams.didMount")
      NamesStore.ensureNamesAreCached(Some(namesCallback))
      DuplicateStore.addChangeListener(storeCallback)
      Controller.monitor(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageTableTeams.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
    }
  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = Callback {
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (prevProps.page != props.page) {
      Controller.monitor(props.page.dupid)
    }
  }

  val component = ScalaComponent.builder[Props]("PageTableTeams")
                            .initialStateFromProps { props => State.create(props) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .componentDidUpdate( didUpdate )
                            .build
}

