package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.West
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.pages.Pixels
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.data.util.Strings

object ViewPlayersThirdRound {
  import PagePlayers._

  def apply(props: Props) =
    component(props) // scalafix:ok ExplicitResultTypes; ReactComponent

  private val eastArrow = Strings.arrowRightLeft
  private val westArrow = Strings.arrowLeftRight

  class Backend(scope: BackendScope[Props, ViewPlayersSecondRound.State]) {

    def show(desc: String, ps: ViewPlayersSecondRound.State) = ps

    def setNorth(p: String)(e: ReactEventFromInput): Callback =
      scope.modState(s => {
        if (s.north == p) {
          s.copy(changingScoreKeeper = false)
        } else if (s.south == p) {
          s.copy(north = s.south, south = s.north, changingScoreKeeper = false)
        } else if (s.east == p) {
          s.copy(
            north = s.east,
            south = s.west,
            east = s.north,
            west = s.south,
            changingScoreKeeper = false
          )
        } else {
          s.copy(
            north = s.west,
            south = s.east,
            east = s.north,
            west = s.south,
            changingScoreKeeper = false
          )
        }
      })

    val swapEW: Callback = scope.modState(s =>
      s.copy(
        north = s.north,
        south = s.south,
        east = s.west,
        west = s.east,
        changingScoreKeeper = false
      )
    )

    def setFirstDealer(p: PlayerPosition): Callback =
      scope.modState(ps => ps.copy(dealer = Some(p)))

    val changeScoreKeeper: Callback =
      scope.modState(s => s.copy(changingScoreKeeper = true))

    def render(props: Props, state: ViewPlayersSecondRound.State) = { // scalafix:ok ExplicitResultTypes; React
      import ChicagoStyles._
      val numberRounds = props.chicago.rounds.length
      val lr = props.chicago.rounds(numberRounds - 1)
      val allFromLastRound = lr.north :: lr.south :: lr.east :: lr.west :: Nil

      // check if all players from last round are mentioned in new round
      val valid = !state.changingScoreKeeper && state.dealer.isDefined

      def getSwapButton(id: String, east: Boolean) = {
        AppButton(
          id,
          s"Swap ${if (east) eastArrow else westArrow}",
          baseStyles.nameButton,
          ^.onClick --> swapEW
        )
      }

      def getDealerButton(
          position: PlayerPosition,
          player: String,
          tabindex: Int
      ) =
        AppButton(
          "Player" + position.pos + "FirstDealer",
          "Dealer",
          baseStyles.nameButton,
          ^.onClick --> setFirstDealer(position),
          BaseStyles.highlight(
            selected = state.isDealer(position),
            required = state.dealer.isEmpty
          ),
          ^.tabIndex := tabindex
        )

      <.div(
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span("Select partners and first dealer")
            )
          ),
          helpurl = "../help/chicago/four/selectnames4.html",
          routeCtl = props.router
        )(),
//        <.h1("Select partners and first dealer"),
        <.div(
          chiStyles.viewPlayersThirdRound,
          <.div(
            <.table(
              <.tbody(
                <.tr(
                  <.td(),
                  <.td(
                    //                        "South",
                    //                        <.br,
                    state.south,
                    <.br,
                    getDealerButton(South, state.south, 8)
                  ),
                  <.td()
                ),
                <.tr(
                  <.td(
                    //                        "East",
                    //                        <.br,
                    state.east,
                    <.br,
                    getDealerButton(East, state.east, 8),
                    <.br,
                    getSwapButton("East", true)
                  ),
                  <.td(),
                  <.td(
                    //                        "West",
                    //                        <.br,
                    state.west,
                    <.br,
                    getDealerButton(West, state.west, 8),
                    <.br,
                    getSwapButton("West", false)
                  )
                ),
                <.tr(
                  <.td(),
                  <.td(
                    //                        "North, "+
                    "Scorekeeper",
                    <.br,
                    if (state.changingScoreKeeper) {
                      var i = 0
                      val extraWidth =
                        Properties.defaultChicagoNameButtonPaddingAndBorder +
                          Properties.defaultChicagoNameButtonBorderRadius
                      val maxPlayerLen =
                        s"${Pixels.maxLength(allFromLastRound: _*) + extraWidth}px"
                      <.span(
                        allFromLastRound.map { p =>
                          i = i + 1
                          AppButton(
                            "ChangeScoreKeeper" + i,
                            p,
                            baseStyles.nameButton,
                            ^.width := maxPlayerLen,
                            BaseStyles.highlight(selected = p == state.north),
                            ^.onClick ==> setNorth(p) _
                          )
                        }.toTagMod
                      )
                    } else {
                      <.span(
                        state.north,
                        <.br,
                        getDealerButton(North, state.north, 8),
                        <.br,
                        AppButton(
                          "ChangeScoreKeeper",
                          "Change Scorekeeper",
                          baseStyles.nameButton,
                          ^.onClick --> changeScoreKeeper
                        )
                      )
                    }
                  ),
                  <.td()
                )
              )
            ),
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton(
                  "Ok",
                  "OK",
                  ^.disabled := !valid,
                  BaseStyles.highlight(required = valid),
                  baseStyles.appButton,
                  ^.onClick --> ok
                )
              ),
              <.div(
                baseStyles.divFooterCenter,
                AppButton(
                  "Cancel",
                  "Cancel",
                  baseStyles.appButton,
                  props.router.setOnClick(props.page.toSummaryView)
                )
              ),
              <.div(
                baseStyles.divFooterRight
                // HelpButton("../help/chicago/four/selectnames4.html")
              )
            )
          )
        )
      )
    }

    val ok: Callback = scope.stateProps { (state, props) =>
      val r = if (props.chicago.rounds.size <= props.page.round) {
        Round.create(
          props.page.round.toString(),
          state.north,
          state.south,
          state.east,
          state.west,
          state.getDealer,
          Nil
        )
      } else {
        props.chicago
          .rounds(props.page.round)
          .copy(
            north = state.north,
            south = state.south,
            east = state.east,
            west = state.west,
            dealerFirstRound = state.getDealer
          )
      }
      ChicagoController.updateChicagoRound(props.chicago.id, r)
      props.router.set(props.page.toHandView(0))
    }

  }

  private[chicagos] val component = ScalaComponent
    .builder[Props]("ViewPlayersThirdRound")
    .initialStateFromProps { props =>
      {
        val numberRounds = props.chicago.rounds.length
        val lastRound = props.chicago.rounds(numberRounds - 1)
        val secondToLastRound = props.chicago.rounds(numberRounds - 2)

        val p1 = lastRound.north
        val p3 =
          lastRound.partnerOf(p1) // can't be player 2, already partnered with 1
        val p4 =
          secondToLastRound.partnerOf(
            p1
          ) // can't be player 2, already partnered with 1
        val p2 = lastRound.partnerOf(p4)
        ViewPlayersSecondRound.State(p1, p2, p3, p4, None, false)
      }
    }
    .backend(new Backend(_))
    .renderBackend
    .build
}
