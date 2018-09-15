package com.example.pages.rubber

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import utils.logging.Logger
import com.example.bridge.store.RubberStore
import com.example.controller.RubberController
import com.example.data.Hand
import com.example.data.rubber.RubberScoring
import com.example.data.bridge.RubberBridge.ScoreHand
import com.example.data.bridge.Rubber
import com.example.pages.hand.PageHand
import com.example.data.bridge.Contract
import com.example.data.bridge.NotVul
import com.example.data.bridge.Vul
import com.example.data.bridge.PassedOut
import com.example.data.bridge.NoTrump
import com.example.data.bridge.NotDoubled
import com.example.data.bridge.North
import com.example.data.bridge.Made
import com.example.controller.RubberController
import com.example.data.RubberHand
import com.example.data.bridge.PlayerPosition
import com.example.pages.rubber.RubberRouter.RubberMatchHandView
import com.example.data.SystemTime


/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageRubberMatchHand( PageRubberMatchHand.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageRubberMatchHand {
  import PageRubberMatchHandInternal._

  case class Props( page: RubberMatchHandView, routerCtl: RouterCtl[RubberPage] )

  def apply( page: RubberMatchHandView, routerCtl: RouterCtl[RubberPage] ) =
    component( Props( page, routerCtl ) )

}

object PageRubberMatchHandInternal {
  import PageRubberMatchHand._

  val logger = Logger("bridge.PageRubberMatchHand")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def viewHandCallbackOk( handid: String )( contract: Contract ) =
      scope.props >>= { props => {
        val time = SystemTime.currentTimeMillis()
        RubberController.updateRubberHand(props.page.rid, handid, RubberHand(contract.id,contract.toHand(),0,"",time,time))
        props.routerCtl.set(props.page.toRubber())
      }}

    def viewHandCallbackWithHonors( handid: String )( contract: Contract, honors: Int, honorsPlayer: PlayerPosition ) =
      scope.props >>= { props => {
        val time = SystemTime.currentTimeMillis()
        RubberController.updateRubberHand(props.page.rid, handid, RubberHand(contract.id,contract.toHand(),honors,honorsPlayer.pos,time,time))
        props.routerCtl.set(props.page.toRubber())
      }}

    val viewHandCallbackCancel = scope.props >>= { props => props.routerCtl.set(props.page.toRubber()) }

    def getPlayerPosition( pos: String ) = try {
      Some(PlayerPosition(pos))
    } catch {
      case _ : Exception => None
    }

    def render( props: Props, state: State ) = {
      RubberStore.getRubber match {
        case Some(rub) if (rub.id == props.page.rid) =>
          val rubberScoring = RubberScoring(rub)
          <.div(
            rub.getHand(props.page.handid) match {
              case Some(h) =>
                val scorehand = ScoreHand(h)
                PageHand.create(scorehand, Rubber, 0, 0,
                                rub.north, rub.south, rub.east, rub.west,
                                rubberScoring.getDealerForHand(props.page.handid),
                                viewHandCallbackOk(props.page.handid), viewHandCallbackCancel, allowPassedOut=false,
                                callbackWithHonors = Some(viewHandCallbackWithHonors(h.id) _),
                                honors = Some(h.honors),
                                honorsPlayer = getPlayerPosition(h.honorsPlayer) )
              case None =>
                val score = RubberScoring(rub)
                val (nsVul, ewVul) = {
                  def getVul( vul: Boolean ) = if (vul) Vul else NotVul
                  ( getVul(score.nsVul), getVul(score.ewVul) )
                }
                val contract = Contract( "0",
                                         PassedOut,
                                         NoTrump,
                                         NotDoubled,
                                         North,
                                         nsVul,
                                         ewVul,
                                         Made,
                                         0,
                                         None,
                                         None,
                                         Rubber,
                                         None,
                                         0,
                                         0,
                                         rub.north,
                                         rub.south,
                                         rub.east,
                                         rub.west,
                                         rubberScoring.getDealerForHand(props.page.handid))
                PageHand( contract , viewHandCallbackOk(""), viewHandCallbackCancel, newhand=true, allowPassedOut=false,
                          callbackWithHonors = Some(viewHandCallbackWithHonors("") _),
                          honors = None,
                          honorsPlayer = None,
                          helppage = Some("/help/rubber/hand.html"))
            }
          )
        case _ =>
          <.div(<.h1("Loading rubber match..."))
      }

    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    val didMount = CallbackTo {
      logger.info("PageRubberNames.didMount")
      RubberStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      RubberController.ensureMatch(p.page.rid))
    }

    val willUnmount = CallbackTo {
      logger.info("PageRubberNames.willUnmount")
      RubberStore.removeChangeListener(storeCallback)
    }

  }

  val component = ScalaComponent.builder[Props]("PageRubberMatchHand")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

