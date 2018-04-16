package com.example.pages.chicagos

import com.example.controller.ChicagoController
import com.example.data.Round
import com.example.data.bridge.East
import com.example.data.bridge.North
import com.example.data.bridge.PlayerPosition
import com.example.data.bridge.South
import com.example.data.bridge.West
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.example.react.AppButton
import com.example.react.Utils._
import com.example.pages.Pixels
import com.example.pages.BaseStyles

object ViewPlayersThirdRound {
  import PagePlayers._

  def apply( props: Props ) = component(props)

  class Backend(scope: BackendScope[Props, ViewPlayersSecondRound.State]) {

    def show( desc: String, ps: ViewPlayersSecondRound.State ) = ps

    def setNorth( p: String )( e: ReactEventFromInput ) =
      scope.modState(s => {
        if (s.north == p) {
          s.copy(changingScoreKeeper = false)
        } else if (s.south == p) {
          s.copy(north=s.south, south=s.north, changingScoreKeeper = false)
        } else if (s.east == p) {
          s.copy(north=s.east, south=s.west, east=s.north, west=s.south, changingScoreKeeper = false)
        } else {
          s.copy(north=s.west, south=s.east, east=s.north, west=s.south, changingScoreKeeper = false)
        }
      })

    val swapEW = scope.modState(s =>
          s.copy(north=s.north, south=s.south, east=s.west, west=s.east, changingScoreKeeper = false)
          )

    def setFirstDealer( p: PlayerPosition ) = scope.modState(ps => ps.copy(dealer=Some(p)))

    val changeScoreKeeper = scope.modState(s => s.copy(changingScoreKeeper = true))

    def render( props: Props, state: ViewPlayersSecondRound.State ) = {
      import ChicagoStyles._
      val numberRounds = props.chicago.rounds.length
      val lr = props.chicago.rounds(numberRounds-1)
      val allFromLastRound = lr.north::lr.south::lr.east::lr.west::Nil

      // check if all players from last round are mentioned in new round
      val valid = !state.changingScoreKeeper

      def getSwapButton( id: String ) = {
        AppButton(id, "Swap East West",
                  baseStyles.nameButton,
                  ^.onClick --> swapEW
                  )
      }

      def getDealerButton(position: PlayerPosition, player: String,  tabindex: Int) =
          AppButton("Player"+position.pos+"FirstDealer", "Dealer",
                    baseStyles.nameButton,
                    ^.onClick --> setFirstDealer(position),
                    BaseStyles.highlight(selected = state.isDealer(position) ),
                    ^.tabIndex:=tabindex
                  )

      <.div(
        chiStyles.viewPlayersThirdRound,
        <.h1("Select partners and first dealer"),
        <.table(
          <.tbody(
            <.tr(
                  <.td(),
                  <.td(
//                        "South",
//                        <.br,
                        state.south,
                        <.br,
                        getDealerButton(South,state.south,8)
                      ),
                  <.td() ),
            <.tr(
                  <.td(
//                        "East",
//                        <.br,
                        state.east,
                        <.br,
                        getDealerButton(East,state.east,8),
                        <.br,
                        getSwapButton("East")
                      ),
                  <.td(),
                  <.td(
//                        "West",
//                        <.br,
                        state.west,
                        <.br,
                        getDealerButton(West,state.west,8),
                        <.br,
                        getSwapButton("West")
                      ) ),
            <.tr(
                  <.td(),
                  <.td(
//                        "North, "+
                        "Scorekeeper",
                        <.br,
                        if (state.changingScoreKeeper) {
                          var i = 0
                          val extraWidth = Properties.defaultChicagoNameButtonPaddingAndBorder +
                                           Properties.defaultChicagoNameButtonBorderRadius
                          val maxPlayerLen = s"${Pixels.maxLength( allFromLastRound: _* )+extraWidth}px"
                          <.span(
                              allFromLastRound.map { p =>
                                i=i+1
                                AppButton("ChangeScoreKeeper"+i, p,
                                          baseStyles.nameButton,
                                          ^.width:=maxPlayerLen,
                                          BaseStyles.highlight(selected = p==state.north),
                                          ^.onClick ==> setNorth(p) _ )
                              }.toTagMod
                          )
                        } else {
                          <.span(
                            state.north,
                            <.br,
                            getDealerButton(North,state.north,8),
                            <.br,
                            AppButton( "ChangeScoreKeeper", "Change Scorekeeper", baseStyles.nameButton, ^.onClick --> changeScoreKeeper)
                          )
                        }
                      ),
                  <.td()
              )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "Ok", "OK" , ^.disabled := !valid, BaseStyles.highlight(requiredNotNext=valid ), baseStyles.appButton, ^.onClick --> ok )
            ),
            <.div(
              baseStyles.divFooterCenter,
              AppButton( "Cancel", "Cancel", baseStyles.appButton, props.router.setOnClick(props.page.toSummaryView()))
            ),
            <.div(
              baseStyles.divFooterRight,
            )
          )
        )
      )
    }

    val ok = scope.stateProps { (state,props) =>
      val r = if (props.chicago.rounds.size <= props.page.round) {
        Round.create(props.page.round.toString(),
             state.north,
             state.south,
             state.east,
             state.west,
             state.getDealer,
             Nil )
      } else {
        props.chicago.rounds(props.page.round).copy(north=state.north, south=state.south, east=state.east, west=state.west, dealerFirstRound=state.getDealer)
      }
      ChicagoController.updateChicagoRound(props.chicago.id, r)
      props.router.set(props.page.toHandView(0))
    }

  }

  val component = ScalaComponent.builder[Props]("ViewPlayersThirdRound")
                            .initialStateFromProps { props => {
                              val numberRounds = props.chicago.rounds.length
                              val lastRound = props.chicago.rounds(numberRounds-1)
                              val secondToLastRound = props.chicago.rounds(numberRounds-2)

                              val p1 = lastRound.north
                              val p3 = lastRound.partnerOf(p1)         // can't be player 2, already partnered with 1
                              val p4 = secondToLastRound.partnerOf(p1) // can't be player 2, already partnered with 1
                              val p2 = lastRound.partnerOf(p4)
                              ViewPlayersSecondRound.State(p1,p2,p3,p4,None,false)
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}
