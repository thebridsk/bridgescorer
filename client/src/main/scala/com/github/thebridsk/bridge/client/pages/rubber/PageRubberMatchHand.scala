package com.github.thebridsk.bridge.client.pages.rubber

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.RubberStore
import com.github.thebridsk.bridge.data.rubber.RubberScoring
import com.github.thebridsk.bridge.data.bridge.RubberBridge.ScoreHand
import com.github.thebridsk.bridge.data.bridge.Rubber
import com.github.thebridsk.bridge.client.pages.hand.PageHand
import com.github.thebridsk.bridge.data.bridge.Contract
import com.github.thebridsk.bridge.data.bridge.NotVul
import com.github.thebridsk.bridge.data.bridge.Vul
import com.github.thebridsk.bridge.data.bridge.PassedOut
import com.github.thebridsk.bridge.data.bridge.NoTrump
import com.github.thebridsk.bridge.data.bridge.NotDoubled
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.Made
import com.github.thebridsk.bridge.client.controller.RubberController
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.RubberMatchHandView
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import org.scalajs.dom.raw.File

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

  case class Props( page: RubberMatchHandView, routerCtl: BridgeRouter[RubberPage] )

  def apply( page: RubberMatchHandView, routerCtl: BridgeRouter[RubberPage] ) =
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

    def viewHandCallbackOk( handid: String )( contract: Contract, picture: Option[File], removePicture: Boolean ) =
      scope.props >>= { props => {
        val time = SystemTime.currentTimeMillis()
        RubberController.updateRubberHand(props.page.rid, handid, RubberHand(contract.id,contract.toHand,0,None,time,time))
        props.routerCtl.set(props.page.toRubber)
      }}

    def viewHandCallbackWithHonors( handid: String )( contract: Contract, picture: Option[File], removePicture: Boolean, honors: Int, honorsPlayer: Option[PlayerPosition] ) =
      scope.props >>= { props => {
        val time = SystemTime.currentTimeMillis()
        RubberController.updateRubberHand(props.page.rid, handid, RubberHand(contract.id,contract.toHand,honors,honorsPlayer.map(_.pos),time,time))
        props.routerCtl.set(props.page.toRubber)
      }}

    val viewHandCallbackCancel = scope.props >>= { props => props.routerCtl.set(props.page.toRubber) }

    def getPlayerPosition( pos: Option[String] ): Option[PlayerPosition] = {
      pos.flatMap { p =>
        try {
          Some(PlayerPosition(p))
        } catch {
          case _ : Exception => None
        }
      }
    }

    def render( props: Props, state: State ) = {
      <.div(
        RubberPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit,
            )(
                <.span( "Enter Hand" )
            )),
          helpurl = "../help/rubber/hand.html",
          routeCtl = props.routerCtl
        )(),
        RubberStore.getRubber match {
          case Some(rub) if (rub.id == props.page.rid) =>
            val rubberScoring = RubberScoring(rub)
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
//                          helppage = Some("../help/rubber/hand.html")
                        )
            }

          case _ =>
            HomePage.loading
        }
      )
    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    val didMount = CallbackTo {
      logger.info("PageRubberMatchHand.didMount")
      RubberStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      RubberController.ensureMatch(p.page.rid))
    }

    val willUnmount = CallbackTo {
      logger.info("PageRubberMatchHand.willUnmount")
      RubberStore.removeChangeListener(storeCallback)
    }

  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = Callback {
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (prevProps.page != props.page) {
      RubberController.monitor(props.page.rid)
    }
  }

  val component = ScalaComponent.builder[Props]("PageRubberMatchHand")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .componentDidUpdate( didUpdate )
                            .build
}

