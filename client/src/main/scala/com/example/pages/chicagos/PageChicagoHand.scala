package com.example.pages.chicagos

import com.example.bridge.store.ChicagoStore
import com.example.controller.ChicagoController
import com.example.data.bridge.Chicago
import com.example.data.bridge.Contract
import com.example.data.bridge.Made
import com.example.data.bridge.NoTrump
import com.example.data.bridge.North
import com.example.data.bridge.NotDoubled
import com.example.data.bridge.NotVul
import com.example.data.bridge.PassedOut
import com.example.data.bridge.South
import com.example.data.bridge.Vul
import com.example.data.bridge.Vulnerability
import com.example.data.chicago.ChicagoScoring
import com.example.pages.hand.PageHand

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import utils.logging.Logger
import com.example.pages.chicagos.ChicagoRouter.HandView

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

  case class Props( page: HandView, routerCtl: RouterCtl[ChicagoPage] )

  def apply( page: HandView, routerCtl: RouterCtl[ChicagoPage] ) =
    component( Props( page, routerCtl ) )

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
    def render( props: Props, state: State ) = {
      ChicagoStore.getChicago match {
        case Some(mc) if (mc.id == props.page.chiid) =>
          log.info("Got: "+mc)
          val scoring = ChicagoScoring(mc)
          val iround = props.page.round
          val ihand = props.page.hand
          val round = scoring.rounds(iround)

          <.div(
            if (ihand < round.hands.length) {
              val scorehand = round.hands(ihand)
              PageHand.create(scorehand,
                              Chicago,
                              0,
                              0,
                              round.round.north,
                              round.round.south,
                              round.round.east,
                              round.round.west,
                              round.dealerOrder(ihand),
                              viewHandCallbackOk(iround,ihand,mc.isQuintet()),
                              viewHandCallbackCancel(mc.isQuintet()),
                              allowPassedOut=false )
            } else {
              val (nsVul, ewVul, dealer) = {
                if (ihand == 5 && mc.gamesPerRound == 6) {
                  (Vul,Vul, round.dealerOrder(1))
                } else {
                  val dealerInFirstGame = scoring.rounds(iround).dealerFirstRound
                  val nsDealer = dealerInFirstGame==North || dealerInFirstGame==South
                  def vulIfNSDealer(nsdealer: Boolean): Vulnerability = { if (nsdealer) Vul; else NotVul }
                  ihand%4 match {
                    case 0 => (NotVul, NotVul, round.dealerOrder(0))
                    case 1 => (vulIfNSDealer(!nsDealer),vulIfNSDealer(nsDealer), round.dealerOrder(1))
                    case 2 => (vulIfNSDealer(nsDealer),vulIfNSDealer(!nsDealer), round.dealerOrder(2))
                    case 3 => (Vul, Vul, round.dealerOrder(3))
                  }
                }

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
                                       Chicago,
                                       None,
                                       0,
                                       0,
                                       round.round.north,
                                       round.round.south,
                                       round.round.east,
                                       round.round.west,
                                       dealer)
              PageHand( contract,
                        viewHandCallbackOk(iround,ihand,mc.isQuintet()),
                        viewHandCallbackCancel(mc.isQuintet()),
                        newhand=true,
                        allowPassedOut=false,
                        helppage = Some("../help/chicago/hand.html"))
            }
          )
        case _ =>
          <.div("Oops")
      }
    }

    def viewHandCallbackOk(iround: Int, ihand: Int, quintet: Boolean)( contract: Contract ) =
      scope.props >>= { props => {
        ChicagoController.updateChicagoHand(props.page.chiid, iround, ihand, contract.toHand())
        props.routerCtl.set(if (quintet) props.page.toSummaryView() else props.page.toRoundView())
      }}

    def viewHandCallbackCancel( quintet: Boolean) =
      scope.props >>= { props =>
        props.routerCtl.set(if (quintet) props.page.toSummaryView() else props.page.toRoundView())
      }

  }

  val component = ScalaComponent.builder[Props]("PageChicagoHand")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

