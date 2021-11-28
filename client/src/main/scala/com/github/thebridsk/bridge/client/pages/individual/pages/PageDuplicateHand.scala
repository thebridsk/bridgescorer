package com.github.thebridsk.bridge.client.pages.individual.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.IndividualController
import com.github.thebridsk.bridge.client.bridge.store.IndividualDuplicateStore
import com.github.thebridsk.bridge.data.bridge.Contract
import com.github.thebridsk.bridge.data.bridge.NoTrump
import com.github.thebridsk.bridge.data.bridge.NotDoubled
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.Duplicate
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.client.pages.hand.PageHand
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import org.scalajs.dom.File
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientIndividualDuplicate
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseHandView
import com.github.thebridsk.bridge.client.pages.individual.components.DuplicateBridgeAppBar
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel

/**
  * A component page that shows and allows entering of the results of playing a hand.
  *
  * The hand is identified by the duplicate match, board being played, and the team playing north-south.
  *
  * To use, just code the following:
  *
  * {{{
  * val page: BaseHandView = ...
  *
  * PageDuplicateHand(
  *   routerCtl = router,
  *   page: page
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageDuplicateHand {
  import Internal._

  case class Props(routerCtl: BridgeRouter[IndividualDuplicatePage], page: BaseHandView)

  /**
    * Instantiate the component
    *
    * @param routerCtl
    * @param page the BaseHandView object that identifies the hand to display.
    *
    * @return the unmounted react component.
    *
    * @see See [[PageDuplicateHand]] for usage information.
    */
  def apply(routerCtl: BridgeRouter[IndividualDuplicatePage], page: BaseHandView) =
    component(
      Props(routerCtl, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.PageDuplicateHand")

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(
        vals: Option[(IndividualDuplicate, IndividualBoard, IndividualDuplicateHand, Hand)],
        newhand: Boolean,
        errormsg: Option[String] = None
    ) {
      def clearMsg() = copy(errormsg = None)
      def setMsg(m: String) = copy(errormsg = Some(m))
    }

    object State {
      def create(props: Props): State = {
        IndividualDuplicateStore.getMatch() match {
          case Some(md) if md.id == props.page.dupid =>
            logger.fine(s"PageDuplicateHand.State.create: md=${md}")
            md.getBoard(props.page.boardid) match {
              case Some(board) =>
                board.getHand(props.page.handid) match {
                  case Some(hand) =>
                    logger.fine("PageDuplicateHand.State.create: " + hand)
                    val (res, newhand) = hand.played match {
                      case Some(h) => (h, false)
                      case None =>
                        (
                          Hand.create(
                            hand.id.id,
                            0,
                            NoTrump.suit,
                            NotDoubled.doubled,
                            North.pos,
                            board.nsVul,
                            board.ewVul,
                            true,
                            0
                          ),
                          true
                        )
                    }
                    State(Some((md, board, hand, res)), newhand, None)
                  case None => State(None, false, Some("Did not find hand"))
                }
              case None => State(None, false, Some("Did not find board"))
            }
          case _ => State(None, false, None)
        }
      }
    }

    def getName(p: Int, players: List[String]) = {
      if (0 < p && p <= players.length) {
        s"${p}: ${players(p-1)}"
      } else {
        s"${p}"
      }
    }

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      val cancel: Callback = scope.modState(s => s.clearMsg())

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        logger.fine(s"""PageDuplicateHand.render( ${props.page} )""")
        <.div(
          PopupOkCancel(state.errormsg.map(s => s), None, Some(cancel)),
          DuplicateBridgeAppBar(
            id = Some(props.page.dupid),
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  "Enter Hand"
                )
              )
            ),
            helpurl = "../help/duplicate/enterhand.html",
            routeCtl = props.routerCtl
          )(
          ),
          state.vals match {
            case Some((md, board, hand, res)) if md.id == props.page.dupid =>
              logger.fine(s"""PageDuplicateHand.render md=${md}""")
              val newhand = state.newhand
              val players = md.players
              val north = getName(hand.north, players)
              val south = getName(hand.south, players)
              val east = getName(hand.east, players)
              val west = getName(hand.west, players)
              logger.fine(s"""PageDuplicateHand.render n=${north} s=${south} e=${east} w=${west}""")
              val contract = Contract.create(
                res,
                Duplicate,
                None,
                hand.table.toInt,
                hand.board.toInt,
                north,
                south,
                east,
                west,
                PlayerPosition(board.dealer)
              )

              PageHand(
                contract,
                viewHandCallbackOk(md, hand) _,
                viewHandCallbackCancel,
                None,
                None,
                newhand = newhand,
                allowPassedOut = board.timesPlayed > 0,
                helppage = None, // Some("../help/duplicate/enterhand.html"))
                picture = IndividualDuplicateStore
                  .getPicture(md.id, hand.board, hand.id)
                  .map(_.url),
                supportPicture = true,
                playingDuplicate = true
              )
            case _ =>
              HomePage.loading
          }
        )
      }

      def viewHandCallbackOk(
          dup: IndividualDuplicate,
          oldhand: IndividualDuplicateHand
      )(
          contract: Contract,
          picture: Option[File],
          removePicture: Boolean
      ): Callback =
        scope.stateProps { (s, p) =>
          logger.fine(
            s"PageDuplicateHand: new contract $contract for (${oldhand.board},${oldhand.id})"
          )
          val newhand: Hand = contract.toHand
          IndividualController.updateHand(dup, oldhand.updateHand(newhand))
          if (removePicture) {
            RestClientIndividualDuplicate
              .pictureResource(dup.id)
              .delete(oldhand.board)
              .foreach { (x) =>
                logger.fine(
                  s"PageDuplicateHand: deleted picture for (${oldhand.board},${oldhand.id})"
                )
              }
          } else {
            picture.foreach { f =>
              RestClientIndividualDuplicate
                .pictureResource(dup.id)
                .handResource(oldhand.board)
                .putPicture(oldhand.id, f)
                .foreach { (x) =>
                  logger.fine(
                    s"PageDuplicateHand: updated picture for (${oldhand.board},${oldhand.id})"
                  )
                }
            }
          }
          p.routerCtl.set(p.page.toBoardView)
        }

      val viewHandCallbackCancel: Callback = scope.props >>= { p =>
        p.routerCtl.set(p.page.toBoardView)
      }

      val storeCallback: Callback = scope.modStateOption { (s, p) =>
        val newstate = State.create(p)
        logger.fine(s"""PageDuplicateHand.storeCallback setting up new state from old=${s}\nand new=${newstate}""")
        val ns = if (newstate.vals.isDefined == s.vals.isDefined) {
          newstate.vals match {
            case Some((nmd, nboard, nhand, nres)) =>
              s.vals match {
                case Some((md, board, hand, res)) =>
                  if (md.equalsIgnoreModifyTime(nmd)) {
                    logger.fine(s"""PageDuplicateHand.storeCallback nothing has changed""")
                    None
                  }
                  else {
                    logger.fine(s"""PageDuplicateHand.storeCallback generating new state""")
                    Some(newstate.copy(vals = Some((nmd, nboard, nhand, res))))
                  }
                case None =>
                  Some(newstate)
              }
            case None =>
              Some(newstate)
          }
        } else {
          Some(newstate)
        }
        logger.fine(s"""PageDuplicateHand.storeCallback returning ${ns}""")
        ns
      }

      val didMount: Callback = scope.props >>= { (p) =>
        Callback {
          logger.info("PageDuplicateHand.didMount")
          IndividualDuplicateStore.addChangeListener(storeCallback)

          IndividualController.monitor(p.page.dupid)

        }
      }

      val willUnmount: Callback = Callback {
        logger.info("PageDuplicateHand.willUnmount")
        IndividualDuplicateStore.removeChangeListener(storeCallback)
        IndividualController.delayStop()
      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps

        if (prevProps.page != props.page) {
          IndividualController.monitor(props.page.dupid)
          cdu.modState { s =>
            State.create(props)
          }.runNow()
        }
      }

    val component = ScalaComponent
      .builder[Props]("PageDuplicateHand")
      .initialStateFromProps { props => State.create(props) }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
