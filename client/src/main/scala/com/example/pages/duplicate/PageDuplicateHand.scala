package com.example.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.routes.BridgeRouter
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.data.bridge.DuplicateViewPerspective
import com.example.bridge.store.DuplicateStore
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.Contract
import com.example.data.bridge.NoTrump
import com.example.data.bridge.NotDoubled
import com.example.data.bridge.North
import com.example.data.bridge.Duplicate
import com.example.data.Hand
import com.example.pages.hand.PageHand
import com.example.data.MatchDuplicate
import com.example.data.DuplicateHand
import com.example.data.Board
import com.example.data.bridge.PlayerPosition
import com.example.pages.duplicate.DuplicateRouter.BaseHandView
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageDuplicateHand( routerCtl: BridgeRouter[DuplicatePage], page: BaseHandView )
 * </code></pre>
 *
 * @author werewolf
 */
object PageDuplicateHand {
  import PageDuplicateHandInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: BaseHandView )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: BaseHandView ) = component(Props(routerCtl,page))

}

object PageDuplicateHandInternal {
  import PageDuplicateHand._

  val logger = Logger("bridge.PageDuplicateHand")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( vals: Option[(MatchDuplicate,Board,DuplicateHand,Hand)], newhand: Boolean, errormsg: Option[String] )

  object State {
    def create( props: Props ) = {
      DuplicateStore.getMatch() match {
        case Some(md) =>
          md.getBoard(props.page.boardid) match {
            case Some(board) =>
              board.getHand(props.page.handid) match {
                case Some(hand) =>
                  logger.fine("PageDuplicateHand.State.create: "+hand)
                  val (res, newhand) = hand.hand match {
                    case Some(h) => (h,false)
                    case None => (Hand.create(hand.id,
                                              0,
                                              NoTrump.suit,
                                              NotDoubled.doubled,
                                              North.pos,
                                              board.nsVul,
                                              board.ewVul,
                                              true,
                                              0 ), true)
                  }
                  State(Some((md,board,hand,res)),newhand,None)
                case None => State( None,false,Some("Did not find hand"))
              }
              case None => State( None,false,Some("Did not find board"))
            }
          case None => State( None,false,Some("Waiting"))
        }
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      logger.fine(s"""PageDuplicateHand.render( ${props.page} )""")
      <.div(
        DuplicatePageBridgeAppBar(
          id = Some(props.page.dupid),
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Enter Hand",
                    )
                )),
          helpurl = "/help/duplicate/enterhand.html",
          routeCtl = props.routerCtl
        )(

        ),
        state.errormsg match {
          case Some(msg) => <.p(msg)
          case None =>
            val (md,board,hand,res) = state.vals.get
            val newhand = state.newhand
            val (north,south,east,west) = {
              var n = "Unknown"
              var s = n
              var e = n
              var w = n
              md.getTeam(hand.nsTeam) match {
                case Some(team) =>
                  if (hand.nIsPlayer1) {
                    n = team.player1
                    s = team.player2
                  } else {
                    s = team.player1
                    n = team.player2
                  }
                case None =>
              }
              md.getTeam(hand.ewTeam) match {
                case Some(team) =>
                  if (hand.eIsPlayer1) {
                    e = team.player1
                    w = team.player2
                  } else {
                    w = team.player1
                    e = team.player2
                  }
                case None =>
              }
              (n,s,e,w)
            }
            val contract = Contract.create(res,
                                           Duplicate,
                                           None,
                                           hand.table.toInt,
                                           Id.boardIdToBoardNumber(hand.board).toInt,
                                           north,south,east,west,
                                           PlayerPosition(board.dealer))

            PageHand(contract,
                     viewHandCallbackOk(md,hand) _,
                     viewHandCallbackCancel,
                     Some(hand.nsTeam),
                     Some(hand.ewTeam),
                     newhand=newhand,
                     allowPassedOut=board.timesPlayed()>0,
                     helppage= None) // Some("/help/duplicate/enterhand.html"))
        }
      )
    }

    def viewHandCallbackOk(dup: MatchDuplicate, oldhand: DuplicateHand)( contract: Contract ) = CallbackTo {
      logger.info("PageDuplicateHand: new contract "+contract)
    } >> scope.props >>= { p =>
      val newhand: Hand = contract.toHand()
      Controller.updateHand(dup, oldhand.updateHand(newhand))
      p.routerCtl.set(p.page.toBoardView())
    }

    val viewHandCallbackCancel = scope.props >>= { p =>
      p.routerCtl.set(p.page.toBoardView())
    }

    val storeCallback = scope.modStateOption { ( s, p ) =>
      val newstate = State.create(p)
      if (newstate.vals.isDefined == s.vals.isDefined) {
        newstate.vals match {
          case Some((nmd,nboard,nhand,nres)) =>
            s.vals match {
              case Some((md,board,hand,res)) =>
                if (nres.equalsIgnoreModifyTime(res)) None
                else Some(newstate)
              case None =>
                Some(newstate)
            }
            Some(newstate)
          case None =>
            Some(newstate)
        }
      } else {
        Some(newstate)
      }
   }

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("PageDuplicateHand.didMount")
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitorMatchDuplicate(p.page.dupid)
    }}

    val willUnmount = CallbackTo {
      logger.info("PageDuplicateHand.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageDuplicateHand")
                            .initialStateFromProps { props => State.create(props) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

