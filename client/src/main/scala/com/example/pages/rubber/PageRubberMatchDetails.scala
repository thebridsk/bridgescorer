package com.example.pages.rubber

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import utils.logging.Logger
import com.example.bridge.store.RubberStore
import com.example.controller.RubberController
import japgolly.scalajs.react._
import com.example.bridge.store.RubberStore
import com.example.bridge.store.RubberStore
import com.example.data.rubber.RubberScoring
import com.example.data.rubber.GameScoring
import com.example.data.bridge._
import com.example.react.AppButton
import com.example.routes.BridgeRouter
import com.example.pages.rubber.RubberRouter.RubberMatchViewBase
import com.example.pages.rubber.RubberRouter.RubberMatchNamesView
import com.example.pages.rubber.RubberRouter.ListView
import com.example.react.Utils._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageRubberMatchDetails( PageRubberMatchDetails.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageRubberMatchDetails {
  import PageRubberMatchDetailsInternal._

  case class Props( page: RubberMatchViewBase, routerCtl: BridgeRouter[RubberPage], noFooter: Boolean )

  def apply( page: RubberMatchViewBase, routerCtl: BridgeRouter[RubberPage], noFooter: Boolean = false ) =
    component( Props( page, routerCtl, noFooter ) )

}

object PageRubberMatchDetailsInternal {
  import PageRubberMatchDetails._
  import RubberStyles._

  val logger = Logger("bridge.PageRubberMatchDetails")

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
      RubberStore.getRubber match {
        case Some(rub) if (rub.id == props.page.rid && rub.gotAllPlayers()) =>
          val score = RubberScoring(rub)
          val (nsAbove,nsBelow,ewAbove,ewBelow) = score.totals

          def showRow( label: String, ns: String, ew: String ) = {
            <.tr(
                <.td( label , ^.colSpan:=5, rubStyles.tableLabel ),
                <.td( ns, rubStyles.tableNumber ),
                <.td( ew, rubStyles.tableNumber )
                )
          }

          var nextDealer = PlayerPosition(if (score.rubber.dealerFirstHand == "") "N" else score.rubber.dealerFirstHand )
          def getDealerAndBump() = {
            val cur = nextDealer
            nextDealer = nextDealer.nextDealer
            cur
          }

          def playerAtPosition( pos: PlayerPosition ) = {
            pos match {
              case North => rub.north
              case South => rub.south
              case East => rub.east
              case West => rub.west
            }
          }

          def showGame( gameNumber: Int, game: GameScoring ) = {
            val len = game.scoredHands.length
            val g = Seq(
                <.tr(
                    <.td( ^.colSpan:=7, "Game "+gameNumber, rubStyles.tableGameLabel )
                ),
                (0 until len).map { i =>
                  val sh = game.scoredHands(i)
                  val (nsv,ewv) = {
                    val (nsAbove, nsBelow, ewAbove, ewBelow) = {
                      if (sh.nsScored) {
                        ( sh.above, sh.below, 0, 0 )
                      } else {
                        ( 0, 0, sh.above, sh.below )
                      }
                    }
                    val (nsHonor, ewHonor) = {
                      if (sh.honors > 0) {
                        if (sh.nsScoredHonors) (sh.honors,0)
                        else (0, sh.honors)
                      } else {
                        (0,0)
                      }
                    }
                    def toStr( above: Int, below: Int, honor: Int ) = {
                      val r = if (above==0 && below==0) ""
                      else {
                        if (below == 0) "("+above+")"
                        else if (above==0) below.toString()
                        else below.toString()+" ("+above+")"
                      }
                      if (honor == 0) r
                      else if (r.length() == 0) "H"+honor
                      else r+" H"+honor
                    }
                    ( toStr(nsAbove,nsBelow,nsHonor), toStr(ewAbove,ewBelow,ewHonor) )
                  }
                  <.tr(
                      <.td( playerAtPosition(getDealerAndBump()) ),
                      <.td(
                            AppButton( "Hand"+i, sh.contractAsString("Vul",""),
                                       baseStyles.appButton100,
                                       ^.onClick-->props.routerCtl.set(props.page.toHand(sh.id)) )
                          ),
                      <.td( playerAtPosition(sh.declarer) ),
                      <.td( if (sh.madeOrDown==Made) sh.tricks.toString() else "" ),
                      <.td( if (sh.madeOrDown==Down) sh.tricks.toString() else "" ),
                      <.td( nsv, rubStyles.tableNumber ),
                      <.td( ewv, rubStyles.tableNumber )
                      )
                }.toTagMod
            )
            (if (game.done) g
            else {
              val ng =
              <.tr(
                  <.td( playerAtPosition(getDealerAndBump()) ),
                  <.td( AppButton( "DetailNextHand", "Next Hand", baseStyles.requiredNotNext, ^.onClick-->nextHand() ) ),
                  <.td(),
                  <.td(),
                  <.td(),
                  <.td(),
                  <.td()
                  )::
              Nil
              g.toList:::ng
            }).toTagMod
          }

          def showGames() = {
            val n = score.games.length
            val x = for (i <- 0 until n) yield {
              showGame( i+1, score.games(i) )
            }
            val y = x
            y.toTagMod
          }

          <.div( rubStyles.divDetailsView,
            <.div(
              <.table(
                <.thead(
                  <.tr(
                    <.th( "Dealer" ),
                    <.th( "Contract" ),
                    <.th( "By" ),
                    <.th( "Made" ),
                    <.th( "Down" ),
                    <.th( rub.north, " ", rub.south),
                    <.th( rub.east, " ", rub.west )
                  )
                ),
                <.tfoot(
                    showRow("Bonus", score.nsBonus.toString(), score.ewBonus.toString()),
                    showRow("Total", score.nsTotal.toString(), score.ewTotal.toString())
                ),
                <.tbody(
                    showGames(),
                )
              )
            ),
            !props.noFooter ?= <.div( baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                !score.done ?= AppButton( "NextHand", "Next Hand", baseStyles.requiredNotNext, ^.onClick-->nextHand() )
              ),
              <.div(
                baseStyles.divFooterLeft,
                AppButton( "EditNames", "Edit Names", ^.onClick-->tonames() ),
                AppButton( "EditNames", "Rubber Match", ^.onClick-->toRubber() )
              ),
              <.div(
                baseStyles.divFooterRight,
                AppButton( "Quit", "Quit", score.done ?= baseStyles.requiredNotNext, ^.onClick-->quit() )
              )
            )
          )
        case Some(rub) if (rub.id == props.page.rid && !rub.gotAllPlayers()) =>
          <.div(
            PageRubberNames( RubberMatchNamesView( props.page.rid ), props.routerCtl )
          )
        case _ =>
          <.div(<.h1("Waiting to load data"))
      }
    }

    def toRubber() = scope.props >>= { props => props.routerCtl.set(props.page.toRubber()) }

    def tonames() = scope.props >>= { props => props.routerCtl.set(props.page.toNames()) }

    def quit() = scope.props >>= { props => props.routerCtl.set(ListView) }

    def nextHand() = {
      RubberStore.getRubber match {
        case Some(rub) =>
          val handid = "new"
          val props = scope.withEffectsImpure.props
          props.routerCtl.set(props.page.toHand(handid))
        case _ => CallbackTo {}
      }
    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    def didMount() = CallbackTo {
      logger.info("PageRubberNames.didMount")
      RubberStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      RubberController.ensureMatch(p.page.rid))
    }

    def willUnmount() = CallbackTo {
      logger.info("PageRubberNames.willUnmount")
      RubberStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageRubberMatchDetails")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

