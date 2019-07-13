package com.github.thebridsk.bridge.pages.chicagos

import com.github.thebridsk.bridge.bridge.store.ChicagoStore
import com.github.thebridsk.bridge.controller.ChicagoController
import com.github.thebridsk.bridge.data.bridge.Chicago
import com.github.thebridsk.bridge.data.bridge.Contract
import com.github.thebridsk.bridge.data.bridge.Made
import com.github.thebridsk.bridge.data.bridge.NoTrump
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.NotDoubled
import com.github.thebridsk.bridge.data.bridge.NotVul
import com.github.thebridsk.bridge.data.bridge.PassedOut
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.Vul
import com.github.thebridsk.bridge.data.bridge.Vulnerability
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.bridge.pages.hand.PageHand

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.pages.chicagos.ChicagoRouter.HandView
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.bridge.routes.BridgeRouter
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.pages.BaseStyles.baseStyles

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * PageChicagoSkeleton( PageChicagoSkeleton.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object PageChicagoHand {
  import PageChicagoHandInternal._

  case class Props(page: HandView, routerCtl: BridgeRouter[ChicagoPage])

  def apply(page: HandView, routerCtl: BridgeRouter[ChicagoPage]) =
    component(Props(page, routerCtl))

}

object PageChicagoHandInternal {
  import PageChicagoHand._

  val log = Logger("bridge.PageChicagoHandInternal")

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
    def render(props: Props, state: State) = {
      <.div(
        baseStyles.divHandPage,
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span("Enter Hand")
            )
          ),
          helpurl = "../help/chicago/hand.html",
          routeCtl = props.routerCtl
        )(),
        ChicagoStore.getChicago match {
          case Some(mc) if (mc.id == props.page.chiid) =>
            log.info("Got: " + mc)
            val scoring = ChicagoScoring(mc)
            val iround = props.page.round
            val ihand = props.page.hand
            val round = scoring.rounds(iround)

            if (ihand < round.hands.length) {
              val scorehand = round.hands(ihand)
              PageHand.create(
                scorehand,
                Chicago,
                0,
                0,
                round.round.north,
                round.round.south,
                round.round.east,
                round.round.west,
                round.dealerOrder(ihand),
                viewHandCallbackOk(iround, ihand, mc.isQuintet()),
                viewHandCallbackCancel(mc.isQuintet()),
                allowPassedOut = false
              )
            } else {
              val (nsVul, ewVul, dealer) = {
                if (ihand == 5 && mc.gamesPerRound == 6) {
                  (Vul, Vul, round.dealerOrder(1))
                } else {
                  val dealerInFirstGame =
                    scoring.rounds(iround).dealerFirstRound
                  val nsDealer = dealerInFirstGame == North || dealerInFirstGame == South
                  def vulIfNSDealer(nsdealer: Boolean): Vulnerability = {
                    if (nsdealer) Vul; else NotVul
                  }
                  ihand % 4 match {
                    case 0 => (NotVul, NotVul, round.dealerOrder(0))
                    case 1 =>
                      (
                        vulIfNSDealer(!nsDealer),
                        vulIfNSDealer(nsDealer),
                        round.dealerOrder(1)
                      )
                    case 2 =>
                      (
                        vulIfNSDealer(nsDealer),
                        vulIfNSDealer(!nsDealer),
                        round.dealerOrder(2)
                      )
                    case 3 => (Vul, Vul, round.dealerOrder(3))
                  }
                }

              }
              val contract = Contract(
                ihand.toString(),
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
                Chicago,
                None,
                0,
                0,
                round.round.north,
                round.round.south,
                round.round.east,
                round.round.west,
                dealer
              )
              PageHand(
                contract,
                viewHandCallbackOk(iround, ihand, mc.isQuintet()),
                viewHandCallbackCancel(mc.isQuintet()),
                newhand = true,
                allowPassedOut = false
//                        helppage = Some("../help/chicago/hand.html")
              )
            }

          case _ =>
            <.div("Oops")
        }
      )
    }

    def viewHandCallbackOk(iround: Int, ihand: Int, quintet: Boolean)(
        contract: Contract
    ) =
      scope.props >>= { props =>
        {
          ChicagoController.updateChicagoHand(
            props.page.chiid,
            iround,
            ihand,
            contract.toHand()
          )
          props.routerCtl.set(
            if (quintet) props.page.toSummaryView()
            else props.page.toRoundView()
          )
        }
      }

    def viewHandCallbackCancel(quintet: Boolean) =
      scope.props >>= { props =>
        props.routerCtl.set(
          if (quintet) props.page.toSummaryView() else props.page.toRoundView()
        )
      }

  }

  val component = ScalaComponent
    .builder[Props]("PageChicagoHand")
    .initialStateFromProps { props =>
      State()
    }
    .backend(new Backend(_))
    .renderBackend
    .build
}