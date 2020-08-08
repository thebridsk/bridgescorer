package com.github.thebridsk.bridge.client.pages.chicagos

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.pages.chicagos.PagePlayers.Props
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.data.util.Strings._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

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
object ViewPlayersFive {
  import ViewPlayersFiveInternal._

  def apply(props: Props) =
    component(props) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object ViewPlayersFiveInternal {
  import ChicagoStyles._

  val logger: Logger = Logger("bridge.ViewPlayersFive")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      scoring: ChicagoScoring,
      possibleNext: Map[String, Set[ChicagoScoring.Fixture]],
      sittingOut: Option[String],
      fixture: Option[ChicagoScoring.Fixture],
      north: Option[String],
      south: Option[String],
      east: Option[String],
      west: Option[String],
      dealer: Option[PlayerPosition]
  )

  object State {
    def apply(props: Props): State = {
      val s = ChicagoScoring(props.chicago)
      val pathSoFar = s.getFixturesSoFar
      logger.info("Path So Far: " + pathSoFar)
      val npf = s.getNextPossibleFixtures
      logger.info("Next Fixtures: " + npf)
      val n = npf.keys.size
      val e = if (n == 1) {
        npf.keys.headOption
      } else {
        None
      }
      val f = e match {
        case Some(sittingOut) =>
          val fs = npf(sittingOut)
          if (fs.size == 1) fs.headOption
          else None
        case None => None
      }
      val (north, south, east, west) = {
        f match {
          case Some(fix) =>
            (Some(fix.north), Some(fix.south), Some(fix.east), Some(fix.west))
          case None =>
            (None, None, None, None)
        }
      }
      new State(s, npf, e, f, north, south, east, west, None)

    }
  }

  private val southArrow = Strings.arrowUpDown
  private val northArrow = Strings.arrowUpDown
  private val eastArrow = Strings.arrowRightLeft
  private val westArrow = Strings.arrowLeftRight

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    val reset: Callback = scope.props >>= { props =>
      scope.modState(s => State(props))
    }

    def setPlayerSittingOut(p: String): Callback =
      scope.modState(s => {
        val fix = s.possibleNext.get(p)
        (fix match {
          case None => None
          case Some(fs) =>
            if (fs.size == 1) fs.headOption
            else None
        }) match {
          case Some(f) =>
            s.copy(
              sittingOut = Some(p),
              fixture = Some(f),
              north = Some(f.north),
              south = Some(f.south),
              east = Some(f.east),
              west = Some(f.west)
            )
          case None =>
            s.copy(sittingOut = Some(p), fixture = None)
        }
      })

    def renderSelectSittingOut(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      <.div(
        chiStyles.divPageSelectSittingOut,
        <.p("Sitting out"),
        <.table(
          <.tbody(
            state.possibleNext.keys.toList.sorted
              .map(p => {
                val selected = state.sittingOut match {
                  case Some(s) => s == p
                  case _       => false
                }
                <.tr(
                  <.td(
                    AppButton.withKey(p)(
                      "Player_" + p,
                      p,
                      baseStyles.appButton100,
                      ^.onClick --> setPlayerSittingOut(p),
                      BaseStyles.highlight(
                        selected = selected,
                        required = state.sittingOut.isEmpty
                      )
                    )
                  )
                )
              })
              .toTagMod
          )
        )
      )
    }

    def setFixture(f: ChicagoScoring.Fixture): Callback =
      scope.modState(s =>
        s.copy(
          fixture = Some(f),
          north = Some(f.north),
          south = Some(f.south),
          east = Some(f.east),
          west = Some(f.west)
        )
      )

    def renderSelectFixture(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

      def renderSelectFixtureSittingOut(
          props: Props,
          state: State,
          sittingOut: String
      ) = {
        val fixtures = state.possibleNext(sittingOut).toList
        <.div(
          chiStyles.divPageSelectPairs,
          <.p("Select Pairings"),
          <.div(
            fixtures.zipWithIndex
              .map(e => {
                val (f, i) = e
                val selected = state.fixture match {
                  case Some(fix) => fix == f
                  case _         => false
                }
                <.div(
                  AppButton(
                    "Fixture" + i,
                    "Select",
                    ^.onClick --> setFixture(f),
                    BaseStyles.highlight(
                      selected = selected,
                      required = state.fixture.isEmpty
                    )
                  ),
                  <.p(f.north + "-" + f.south),
                  <.p(f.east + "-" + f.west)
                )
              })
              .toTagMod
          )
        )
      }

      state.sittingOut match {
        case Some(sittingOut) =>
          renderSelectFixtureSittingOut(props, state, sittingOut)
        case None =>
          val n = state.possibleNext.keys.size
          if (n == 1) {
            val extra = state.possibleNext.keys.head
            renderSelectFixtureSittingOut(props, state, extra)
          } else {
            <.div()
          }

      }
    }

    val rotateClockwise: Callback = scope.modState(s =>
      s.copy(north = s.west, south = s.east, east = s.north, west = s.south)
    )

    val rotateCounterClockwise: Callback = scope.modState(s =>
      s.copy(north = s.east, south = s.west, east = s.south, west = s.north)
    )

    val swapEW: Callback =
      scope.modState(s => s.copy(east = s.west, west = s.east))

    val swapNS: Callback =
      scope.modState(s => s.copy(north = s.south, south = s.north))

    val ok: Callback = scope.stateProps { (state, props) =>
      val r = if (props.chicago.rounds.size <= props.page.round) {
        Round.create(
          props.page.round.toString(),
          state.north.get,
          state.south.get,
          state.east.get,
          state.west.get,
          state.dealer.get.pos.toString(),
          Nil
        )
      } else {
        props.chicago
          .rounds(props.page.round)
          .copy(
            north = state.north.get,
            south = state.south.get,
            east = state.east.get,
            west = state.west.get,
            dealerFirstRound = state.dealer.get.pos.toString()
          )
      }
      ChicagoController.updateChicagoRound(props.chicago.id, r)

      props.router.set(props.page.toHandView(0))
    }

    def setDealer(pos: PlayerPosition): Callback =
      scope.modState(s => s.copy(dealer = Some(pos)))

    def renderSelectPos(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      val valid = state.fixture.isDefined

      def pos(
          name: String,
          arrow: String,
          pos: PlayerPosition,
          swap: Callback
      ) = {
        val selected = state.dealer match {
          case Some(s) => s == pos
          case None    => false
        }
        <.td(
          <.b(name),
          <.br,
          AppButton(
            "Dealer" + pos.pos,
            "Dealer",
            ^.onClick --> setDealer(pos),
            BaseStyles.highlight(
              selected = selected,
              required = state.dealer.isEmpty
            )
          ),
          AppButton(
            "Swap" + pos.pos,
            arrow,
            ^.onClick --> swap,
            baseStyles.requiredNotNext
          )
        )
      }
      <.div(
        chiStyles.divPageSelectPos,
        //
        //      swap NS, swap EW, rotate clockwise, rotate counterclockwise, see Strings class for unicode values
        //      select first dealer

//            south
//            dealer V
//
//  east        OO       west
//  dealer >             dealer <
//            north
//            dealer ^
        <.p("Select dealer and seats"),
        <.table(
          <.tbody(
            <.tr(
              <.td(
              ),
              pos(state.south.getOrElse("south"), southArrow, South, swapNS),
              <.td(
              )
            ),
            <.tr(
              pos(state.east.getOrElse("east"), eastArrow, East, swapEW),
              <.td(
                AppButton(
                  "clockwise",
                  clockwiseCircleArrow,
                  ^.onClick --> rotateClockwise,
                  baseStyles.requiredNotNext
                ),
                <.br,
                AppButton(
                  "anticlockwise",
                  antiClockwiseCircleArrow,
                  ^.onClick --> rotateCounterClockwise,
                  baseStyles.requiredNotNext
                )
              ),
              pos(state.west.getOrElse("west"), westArrow, West, swapEW)
            ),
            <.tr(
              <.td(
              ),
              pos(state.north.getOrElse("north"), northArrow, North, swapNS),
              <.td(
              )
            )
          )
        ),
        !valid ?= baseStyles.notVisible
      )
    }

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      val valid =
        state.north.isDefined && state.south.isDefined && state.east.isDefined && state.west.isDefined && state.dealer.isDefined
      <.div(
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span("Five Player")
            )
          ),
          helpurl = "../help/chicago/five/selectnames5.html",
          routeCtl = props.router
        )(),
        <.div(
          <.div(
            chiStyles.divPageFive,
            renderSelectSittingOut(props, state),
            renderSelectFixture(props, state),
            renderSelectPos(props, state)
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton(
                "OK",
                "OK",
                ^.disabled := !valid,
                BaseStyles.highlight(required = valid),
                ^.onClick --> ok
              )
            ),
            <.div(
              baseStyles.divFooterCenter,
              AppButton("Reset", "Reset", ^.onClick --> reset)
            ),
            <.div(
              baseStyles.divFooterRight,
              AppButton(
                "Cancel",
                "Cancel",
                props.router.setOnClick(props.page.toSummaryView)
              )
              //            HelpButton("../help/chicago/five/selectnames5.html")
            )
          )
        )
      )
    }
  }

  private[chicagos] val component = ScalaComponent
    .builder[Props]("ViewPlayersFive")
    .initialStateFromProps { props =>
      State(props)
    }
    .backend(new Backend(_))
    .renderBackend
    .build
}
