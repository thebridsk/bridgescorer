package com.github.thebridsk.bridge.client.pages.duplicate


import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.bridge.Contract
import com.github.thebridsk.bridge.data.bridge.NoTrump
import com.github.thebridsk.bridge.data.bridge.NotDoubled
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.Duplicate
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.client.pages.hand.PageHand
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseHandView
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import org.scalajs.dom.raw.File
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicate
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.clientcommon.react.Utils._


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

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: BaseHandView ) = component(Props(routerCtl,page))  // scalafix:ok ExplicitResultTypes; ReactComponent

}

object PageDuplicateHandInternal {
  import PageDuplicateHand._

  val logger: Logger = Logger("bridge.PageDuplicateHand")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
    vals: Option[(MatchDuplicate,Board,DuplicateHand,Hand)],
    newhand: Boolean,
    errormsg: Option[String] = None
  )

  object State {
    def create( props: Props ): State = {
      DuplicateStore.getMatch() match {
        case Some(md) =>
          md.getBoard(props.page.boardid) match {
            case Some(board) =>
              board.getHand(props.page.handid) match {
                case Some(hand) =>
                  logger.fine("PageDuplicateHand.State.create: "+hand)
                  val (res, newhand) = hand.hand match {
                    case Some(h) => (h,false)
                    case None => (Hand.create(hand.id.id,
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
    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React
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
          helpurl = "../help/duplicate/enterhand.html",
          routeCtl = props.routerCtl
        )(

        ),
        state.errormsg match {
          case Some(msg) => <.p(msg)
          case None =>
            state.vals match {
              case Some((md,board,hand,res)) if md.id == props.page.dupid =>
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
                                              hand.board.toInt,
                                              north,south,east,west,
                                              PlayerPosition(board.dealer))

                PageHand(contract,
                        viewHandCallbackOk(md,hand) _,
                        viewHandCallbackCancel,
                        Some(hand.nsTeam),
                        Some(hand.ewTeam),
                        newhand=newhand,
                        allowPassedOut=board.timesPlayed>0,
                        helppage= None,   // Some("../help/duplicate/enterhand.html"))
                        picture=DuplicateStore.getPicture(md.id,hand.board,hand.id).map(_.url),
                        supportPicture = true,
                )
              case _ =>
                HomePage.loading
            }
        }
      )
    }

    def viewHandCallbackOk(
        dup: MatchDuplicate,
        oldhand: DuplicateHand
    )(
        contract: Contract,
        picture: Option[File],
        removePicture: Boolean
    ): Callback = scope.stateProps { (s,p) =>
      logger.fine(s"PageDuplicateHand: new contract $contract for (${oldhand.board},${oldhand.id})" )
      val newhand: Hand = contract.toHand
      Controller.updateHand(dup, oldhand.updateHand(newhand))
      if (removePicture) {
        RestClientDuplicate.pictureResource(dup.id).delete(oldhand.board).foreach { (x) =>
          logger.fine(s"PageDuplicateHand: deleted picture for (${oldhand.board},${oldhand.id})")
        }
      } else {
        picture.foreach{ f =>
          RestClientDuplicate.pictureResource(dup.id).handResource(oldhand.board).putPicture(oldhand.id,f).foreach { (x) =>
            logger.fine(s"PageDuplicateHand: updated picture for (${oldhand.board},${oldhand.id})")
          }
        }
      }
      p.routerCtl.set(p.page.toBoardView)
    }

    val viewHandCallbackCancel: Callback = scope.props >>= { p =>
      p.routerCtl.set(p.page.toBoardView)
    }

    val storeCallback: Callback = scope.modStateOption { ( s, p ) =>
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

    val didMount: Callback = scope.props >>= { (p) => Callback {
      logger.info("PageDuplicateHand.didMount")
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitor(p.page.dupid)

    }}

    val willUnmount: Callback = Callback {
      logger.info("PageDuplicateHand.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
    }
  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ): Callback = Callback {
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (prevProps.page != props.page) {
      Controller.monitor(props.page.dupid)
    }
  }

  private[duplicate]
  val component = ScalaComponent.builder[Props]("PageDuplicateHand")
                            .initialStateFromProps { props => State.create(props) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .componentDidUpdate( didUpdate )
                            .build
}

