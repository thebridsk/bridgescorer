package com.example.pages.chicagos

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.data.MatchChicago
import com.example.routes.BridgeRouter
import com.example.pages.chicagos.PagePlayers.Props
import com.example.data.chicago.ChicagoScoring
import com.example.data.Round
import com.example.data.bridge.PlayerPosition
import com.example.controller.ChicagoController
import com.example.data.util.Strings._
import com.example.data.bridge._
import utils.logging.Logger
import com.example.data.util.Strings
import com.example.react.AppButton
import com.example.bridge.rotation.Table
import com.example.react.Utils._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * Component( Component.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayersQuintet {
  import ViewPlayersQuintetInternal._

  def apply( props: Props ) = component(props)

}

object ViewPlayersQuintetInternal {
  import ViewPlayersQuintet._
  import ChicagoStyles._

  val logger = Logger( "bridge.ViewPlayersQuintet" )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( scoring: ChicagoScoring,
                    sittingOut: String,
                    north: String,
                    south: String,
                    east: String,
                    west: String,
                    dealer: PlayerPosition,
                    nextSittingOut: Option[String],    // for fair rotation selecting next person out
                    swapping: List[(String,String)]
                  ) {

    def getPossibleSittingOut(): List[String] = {
      var players = scoring.chicago.players
      val completed = scoring.rounds.size/5
      val start = completed*5
      for (i <- start until scoring.rounds.size) {
        if (!scoring.chicago.rounds(i).hands.isEmpty) {
          val roundplayers = scoring.chicago.rounds(i).players
          players = players.filter(p => roundplayers.contains(p))
        }
      }
      if (players.size == scoring.chicago.players.size) {
        players = scoring.chicago.rounds.last.players
      }

      players
    }

    def isSimple() = scoring.chicago.simpleRotation

    def getTable() = {
      val players = scoring.players
      val lastround = {
        val last = scoring.rounds.last
        if (last.hands.isEmpty) {
          scoring.rounds( scoring.rounds.size - 2 )
        } else {
          last
        }
      }
      val no = lastround.round.north
      val so = lastround.round.south
      val ea = lastround.round.east
      val we = lastround.round.west

      val lastplayers = no::so::ea::we::Nil

      val ex = players.find( p => !lastplayers.contains(p)).get

      val t = Table(no,so,ea,we,ex)
      t
    }

    def getLastDealer() = {
      scoring.rounds.last.dealerFirstRound
    }

    def fairRotation( nextSittingOut: String ) = {
      val t = getTable()
      val l = t.find(nextSittingOut).get
      val n = t.rotateSwapRightPartnerOf(l)
      val swap = t.getSwappingPlayersInRotateSwapRightPartnerOf(l)
      copy(sittingOut=n.sittingOut, north=n.north, south=n.south, east=n.east, west=n.west, nextSittingOut=Some(nextSittingOut), swapping=swap)
    }
  }

  object State {
    def apply( props: Props ) = {
      val score = ChicagoScoring(props.chicago)

      val players = score.players
      val lastround = {
        val last = score.rounds.last
        if (last.hands.isEmpty) {
          score.rounds( score.rounds.size - 2 )
        } else {
          last
        }
      }

      val north = lastround.round.north
      val south = lastround.round.south
      val east = lastround.round.east
      val west = lastround.round.west

      val lastplayers = north::south::east::west::Nil

      val extra = players.find( p => !lastplayers.contains(p)).get

      val nextdealer = lastround.dealerFirstRound.nextDealer

      val (n,s,e,w,ex, swap) = lastround.dealerFirstRound match {
        case North => (extra,south,east,west,north, (north,extra))
        case South => (north,extra,east,west,south, (south,extra))
        case East => (north,south,extra,west,east, (east,extra))
        case West => (north,south,east,extra,west, (west,extra))
      }

      val state = new State(score, ex, n, s, e, w, nextdealer,None,Nil)

      if (state.isSimple()) {
        state.copy(swapping=swap::Nil)
      } else {
        state.getPossibleSittingOut() match {
          case next::Nil =>
            state.fairRotation(next)
          case next::tail =>
            state
          case Nil =>
            state
        }
      }

    }
  }

  private val southArrow = Strings.arrowUpDown
  private val northArrow = Strings.arrowUpDown
  private val eastArrow = Strings.arrowRightLeft
  private val westArrow = Strings.arrowLeftRight

//  val colors = ("lightgreen", "aquamarine")::("aqua", "paleturquoise")::Nil
  val colors = ("lightgreen", "lightgreen")::("aqua", "aqua")::Nil

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def reset() = scope.props >>= { props => scope.modState( s => State(props) ) }

    def ok() = CallbackTo {
        val state = scope.withEffectsImpure.state
        val props = scope.withEffectsImpure.props
        val r = if (props.chicago.rounds.size <= props.page.round) {
          Round.create(props.page.round.toString(),
               state.north,
               state.south,
               state.east,
               state.west,
               state.dealer.pos.toString(),
               Nil )
        } else {
          props.chicago.rounds(props.page.round).copy(north=state.north,
                                                      south=state.south,
                                                      east=state.east,
                                                      west=state.west,
                                                      dealerFirstRound=state.dealer.pos.toString())
        }
        ChicagoController.updateChicagoRound(props.chicago.id, r)
        props
      } >>= {
        props => props.router.set(props.page.toHandView(0))
      }


    def setDealer( pos: PlayerPosition ) = scope.modState(s => s.copy(dealer = pos))

    def setPlayerSittingOut( p: String ) = scope.modState(s => s.fairRotation(p))

    def render( props: Props, state: State ) = {
      val valid = state.isSimple() || state.nextSittingOut.isDefined

      val playerColors = state.swapping.zip( colors ).flatMap(e => {
        val ((p1,p2),(c1,c2)) = e
        (p1, ^.background:=c1)::(p2,^.background:=c2)::Nil
      }).toMap

      def getDealer( pos: PlayerPosition, dealer: PlayerPosition ) = {
        if (dealer == pos) {
          Seq[TagMod]( "Dealer", <.br ).toTagMod
        } else {
          EmptyVdom
        }
      }

      def selectNextSittingOut() = {
        if (state.isSimple()) EmptyVdom
        else {
          <.div(
            chiStyles.divPageSelectSittingOut,
            <.p("Sitting out"),
            <.table(
              <.tbody(
                state.getPossibleSittingOut().sorted.map( p => {
                  val selected = state.nextSittingOut match {
                    case Some(s) => s==p
                    case _ => false
                  }
                  <.tr(
                    <.td(
                      AppButton.withKey(p)( "Player_"+p, p,
                                            baseStyles.appButton100,
                                            ^.onClick --> setPlayerSittingOut(p),
                                            selected ?= baseStyles.buttonSelected,
                                            state.sittingOut.isEmpty ?= baseStyles.required
                                          )
                    )
                  )
                }).toTagMod
              )
            )
          )
        }
      }

      def showPlayerWithColor( name: String, colormap: Map[String,TagMod] = playerColors ) = {
        <.span(
          colormap.get(name).whenDefined( c => c ),
          <.b(name)
        )
      }

      def showNewPositions( show: Boolean, header: String, dealer: PlayerPosition, north: String, south: String, east: String, west: String, sittingOut: String ) = {
        if ( show ) {
          <.div(
            chiStyles.viewShowNewPosition,
            <.p( header ),
            <.table(
              <.tbody(
                <.tr(
                  <.td( ^.colSpan:=2,
                      "Sitting out",
                      <.br,
                      showPlayerWithColor(sittingOut)
                  )
                ),
                <.tr(
                  <.td(),
                  <.td( ^.colSpan := 2,
                      getDealer(South,dealer),
                      showPlayerWithColor(south)
                  ),
                  <.td()
                ),
                <.tr(
                  <.td( ^.colSpan := 2,
                      getDealer(East,dealer),
                      showPlayerWithColor(east)
                  ),
                  <.td( ^.colSpan := 2,
                      getDealer(West,dealer),
                      showPlayerWithColor(west)
                  )
                ),
                <.tr(
                  <.td(),
                  <.td( ^.colSpan := 2,
                      getDealer(North,dealer),
                      showPlayerWithColor(north)
                  ),
                  <.td()
                )
              )
            )
          )
        } else {
          <.div()
        }
      }

      def showDescription() = {
        if (state.isSimple()) {
          val t = state.getTable()
          val goingOut = state.sittingOut
          val goingIn = t.sittingOut
          <.div(
            s"${goingIn} replaces ${goingOut}"
          )
        } else if (state.nextSittingOut.isDefined) {
          val t = state.getTable()
          val goingOut = state.nextSittingOut.get
          val goingIn = t.sittingOut
          val loc = t.find(goingOut).get
          val right = t.find(t.rightLocOf(loc).get)
          val partner = t.find(t.partnerLocOf(loc).get)
          <.div(
            s"${goingIn} replaces ${goingOut}",
            <.br,
            s"${right} swap with ${partner}"
          )
        } else {
          <.div()
        }
      }

      val lasthand = state.getTable()
      val lastdealer = PlayerPosition.prevDealer( state.dealer )

      <.div(
        <.div(
          baseStyles.divText100,
          <.h1(
            if (state.isSimple()) "Simple Rotation" else "Fair Rotation"
          )
        ),
        <.div(
          chiStyles.divPageQuintet,
          ^.alignItems:="center",
          showNewPositions( true, "Prior hand", lastdealer, lasthand.north, lasthand.south, lasthand.east, lasthand.west, lasthand.sittingOut ),
          selectNextSittingOut(),
          showNewPositions( state.isSimple() || state.nextSittingOut.isDefined, "Next hand", state.dealer, state.north, state.south, state.east, state.west, state.sittingOut ),
          showDescription()
        ),
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton("OK","OK", ^.disabled:= !valid, valid?=baseStyles.requiredNotNext, ^.onClick --> ok )
          ),
          !state.isSimple() ?=
            <.div(
              baseStyles.divFooterCenter,
              AppButton("Reset", "Reset", ^.onClick --> reset() )
            ),
          <.div(
            baseStyles.divFooterRight,
            AppButton("Cancel", "Cancel", props.router.setOnClick(props.page.toSummaryView()) )
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPlayersQuintet")
                            .initialStateFromProps { props => State(props) }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

