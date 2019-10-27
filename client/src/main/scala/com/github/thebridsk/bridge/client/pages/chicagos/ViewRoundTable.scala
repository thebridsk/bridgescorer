package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.data.bridge.DuplicateBridge
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.RoundView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.BridgeRouter

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
object ViewRoundTable {
  import ViewRoundTableInternal._

  case class Props( scoring: ChicagoScoring, roundid: Int, page: RoundView, routerCtl: BridgeRouter[ChicagoPage] )

  def apply( scoring: ChicagoScoring, roundid: Int, page: RoundView, routerCtl: BridgeRouter[ChicagoPage] ) =
    component( Props( scoring, roundid, page, routerCtl ) )

  def withKey( key: String )( scoring: ChicagoScoring, roundid: Int, page: RoundView, routerCtl: BridgeRouter[ChicagoPage] ) =
    component.withKey(key)(Props(scoring, roundid, page, routerCtl))
}

object ViewRoundTableInternal {
  import ViewRoundTable._

  val logger = Logger("bridge.ViewRoundTable")

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
      val roundNumber = props.roundid
      val scoring = props.scoring.rounds(roundNumber)

      logger.finer("ViewRounds.totalsTable "+scoring)
      val players = scoring.players
      logger.finer( "Players: "+players.mkString(", "))
      val byHands = scoring.hands
      val totals = scoring.totals
      <.table(
          <.thead(
            <.tr(
              <.th( ^.colSpan := 6, "Round "+(roundNumber+1)),
              <.th( ^.rowSpan := 2, scoring.round.north, <.br(), scoring.round.south),
              <.th( ^.rowSpan := 2, scoring.round.east, <.br(), scoring.round.west)
            ),
            <.tr(
              <.th( "Hand"),
              <.th( "Contract"),
              <.th( "By"),
              <.th( "Made"),
              <.th( "Tricks"),
              <.th( "Dealer")
            )
          ),
          <.tfoot(
              <.tr(
                <.td( "Totals", ^.colSpan := 6),
                <.td( ^.textAlign := "right", totals(0).toString),
                <.td( ^.textAlign := "right", totals(2).toString)
              )
          ),
          <.tbody(
              (0 until byHands.length).map { i =>
                val key="Round"+roundNumber+"Hand"+i
                val v = byHands(i)
                val dealer = scoring.dealerForHand(i+1)
                summaryHandRow.withKey(key)((roundNumber,i,Some(v),dealer,props))
              }.toTagMod,
              (byHands.length until props.scoring.gamesPerRound).map { i =>
                val key="Round"+roundNumber+"Hand"+i
                val dealer = scoring.dealerForHand(i+1)
                summaryHandRow.withKey(key)((roundNumber,i,None,dealer,props))
              }.toTagMod,
          )
      )
    }
  }

  val summaryHandRow = ScalaComponent.builder[(Int,Int,Option[DuplicateBridge.ScoreHand],PlayerPosition,Props)]("SummaryHandRow")
  .render_P(myprops => {
    val (round,hand, scores, dealer, props) = myprops
    def scoreOrBlank(v: Int) = if (v>0) v.toString(); else ""

    def playerAtPos( pos: PlayerPosition ) = pos match {
      case North => props.scoring.rounds(round).round.north
      case South => props.scoring.rounds(round).round.south
      case East => props.scoring.rounds(round).round.east
      case West => props.scoring.rounds(round).round.west
      case PositionUndefined => ""
    }

    val c = scores.map( _.contractAsString( "Vul", "" ) ).getOrElse("")
    val dec = scores.map( s => playerAtPos(s.declarer) ).getOrElse("")
    val md = scores.map( _.madeOrDown.forScore ).getOrElse("")
    val t = scores.map( _.tricks.toString()).getOrElse("")
    val nss = scores.map( s => scoreOrBlank(s.score.ns)).getOrElse("")
    val ews = scores.map( s => scoreOrBlank(s.score.ew)).getOrElse("")

    <.tr(
        <.td(
          if (scores.isDefined) {
            AppButton( "Hand_"+(hand+1), (hand+1).toString(), props.routerCtl.setOnClick(props.page.toHandView(hand)) )
          } else {
            (hand+1).toString()
          }
        ),
        <.td( c ),
        <.td( dec ),
        <.td( md ),
        <.td( t ),
        <.td( playerAtPos(dealer)),
        <.td( nss, ^.textAlign := "right"),
        <.td( ews, ^.textAlign := "right")
    )
  }).build

  val component = ScalaComponent.builder[Props]("ViewRoundTable")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

