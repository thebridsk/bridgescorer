package com.example.pages.chicagos

import scala.scalajs.js
import scala.scalajs.js.UndefOr.any2undefOrA

import org.scalajs.dom.raw.Element

import com.example.bridge.store.NamesStore
import com.example.controller.ChicagoController
import com.example.data.Round
import com.example.data.bridge.East
import com.example.data.bridge.North
import com.example.data.bridge.PlayerPosition
import com.example.data.bridge.South
import com.example.data.bridge.West
import com.example.pages.info.InfoPage
import com.example.react.Combobox
import com.example.react.Utils.ExtendReactEventFromInput
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.example.react.ComboboxOrInput
import com.example.react.AppButton
import com.example.react.CheckBox
import com.example.react.RadioButton
import com.example.react.Utils._
import com.example.pages.BaseStyles

object ViewPlayersVeryFirstRound {
  import PagePlayers._
  import PagePlayersInternal._

  def apply( props: Props ) = component(props)

  class Backend(scope: BackendScope[Props, PlayerState]) {
    // PlayerState.dealer contains "north", "south", "east", "west"

    def setNorth( text: String ): Callback = scope.modState( ps => {traceSetname("setNorth",ps.copy(north=text))})
    def setSouth( text: String ): Callback = scope.modState( ps => {traceSetname("setSouth",ps.copy(south=text))})
    def setEast( text: String ): Callback =  scope.modState( ps => {traceSetname("setEast",ps.copy(east=text))})
    def setWest( text: String ): Callback =  scope.modState( ps => {traceSetname("setWest",ps.copy(west=text))})

    def setExtra( text: String ): Callback = scope.modState( ps => {traceSetname("setExtra",ps.copy(extra=Some(text)))})

    def traceSetname( pos: String, state: PlayerState ): PlayerState = {
      logger.fine("ViewPlayersVeryFirstRound: Setting player "+pos+": "+state)
      state
    }

    def reset = scope.modState( ps => PlayerState("","","","",North, gotNames = ps.gotNames, names=ps.names))

    def setFirstDealer( p: PlayerPosition ) = scope.modState(ps => ps.copy(dealer=p))

    private def noNull( s: String ) = if (s == null) ""; else s

    def render( props: Props, state: PlayerState ) = {
      import ChicagoStyles._
      val valid = state.isValid()
      def getButton(position: PlayerPosition, player: String,  tabindex: Int) =
        AppButton("Player"+position.pos+"FirstDealer",
                  "Dealer",
                  baseStyles.nameButton,
                  ^.onClick --> setFirstDealer(position),
                  BaseStyles.highlight( selected=state.isDealer(position) ),
                  ^.tabIndex:=tabindex
                 )
      import scala.scalajs.js.JSConverters._

      val busy = !state.gotNames
      val names = state.names.toJSArray

        //              "South", South, state.south, false, setSouth, 2, 6
      def putName( playerPos: String, playerPosition: PlayerPosition, name: String, scorekeeper: Boolean, cb: String=>Callback, tabInput: Int, tabDealer: Int ) = {
        <.div(
          getButton(playerPosition,name,tabDealer),
          <.div(
            ComboboxOrInput( cb, noNull(name), names, "startsWith", tabInput, playerPos,
                             msgEmptyList="No suggested names", msgEmptyFilter="No names matched",
                             id=s"Combo_${playerPos}" ),
            BaseStyles.highlight( requiredName = !playerValid(name) )
          )
        )
      }

      def isExtraValid() = {
        state.extra.getOrElse("").length() > 0
      }

      <.div(
        chiStyles.viewPlayersVeryFirstRound,
        <.h1(InfoPage.showOnlyInLandscapeOnTouch(), "Rotate to portrait for a better view"),
        <.h1("Enter players and identify first dealer"),
        <.table(
          <.tbody(
            <.tr(
              !state.chicago5 ?= baseStyles.notVisible,
              <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7),
              <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7),
              <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7,
                "Sitting out",
                <.br,
                ComboboxOrInput( setExtra, noNull(state.extra.getOrElse("")), names, "startsWith", 9, "Extra",
                                             msgEmptyList="No suggested names", msgEmptyFilter="No names matched"),
                <.br,
                CheckBox( "Quintet", "Fast Rotation", state.quintet, toggleQuintet() ),
                if (state.quintet) {
                  Seq[TagMod](
                    <.br,
                    RadioButton( "Simple", "Simple Rotation", state.simpleRotation, setSimpleRotation(true) ),
                    <.br,
                    RadioButton( "Fair", "Fair Rotation", !state.simpleRotation, setSimpleRotation(false) )
                  ).toTagMod
                } else {
                  EmptyVdom
                },
                BaseStyles.highlight( required = !isExtraValid() )
              )
            ),
            <.tr(
              <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7),
              <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("South", South, state.south, false, setSouth, 2, 6)),
              <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7)
            ),
            <.tr(
              <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("East", East, state.east, false, setEast, 1, 5)),
              <.td( ^.colSpan := 1, tableStyles.tableCellWidth1Of7),
              <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("West", West, state.west, false, setWest, 3, 7))
            ),
            <.tr(
              <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7),
              <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("North", North, state.north, false, setNorth, 4, 8)),
              <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7)
            )
          )
        ),
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "Ok", "OK", ^.disabled := !valid, BaseStyles.highlight( requiredNotNext=valid ), ^.onClick-->ok() ),
            AppButton(
              "ToggleFive",
              if (state.chicago5) "Four" else "Five",
              ^.onClick-->doChicagoFive()
            )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "ResetNames", "Reset", ^.onClick --> reset)
          ),
          <.div(
            baseStyles.divFooterRight,
            AppButton( "Cancel", "Cancel", props.router.setOnClick(props.page.toSummaryView()) )
          )
        )
      )
    }

    def toggleQuintet() = scope.modState( s=> s.copy( quintet = !s.quintet))

    def toggleSimpleRotation() = scope.modState( s=> s.copy( simpleRotation = !s.simpleRotation))

    def setSimpleRotation( simple: Boolean ) = scope.modState( s=> s.copy( simpleRotation = simple))

    def doChicagoFive() = scope.modState({ ps => ps.copy(chicago5 = !ps.chicago5) } )

    def ok() = CallbackTo {
      val state = scope.withEffectsImpure.state
      val props = scope.withEffectsImpure.props
      val e = if (state.chicago5 && state.extra.isDefined) {
        val ex = state.extra.get
        if (ex == "") None
        else Some(ex.trim)
      } else {
        None
      }
      val quintet = state.chicago5 && state.quintet
      val north = state.north.trim
      val south = state.south.trim
      val east = state.east.trim
      val west = state.west.trim
      ChicagoController.updateChicagoNames(props.chicago.id, north, south, east, west, e, quintet, state.simpleRotation)
      val r = if (props.chicago.rounds.isEmpty) {
        Round.create("0",
             north,
             south,
             east,
             west,
             state.dealer.pos.toString(),
             Nil )
      } else {
        props.chicago.rounds(0).copy(north=north, south=south, east=east, west=west, dealerFirstRound=state.dealer.pos.toString())
      }
      ChicagoController.updateChicagoRound(props.chicago.id, r)
      props
    } >>= {
      props => props.router.set(props.page.toHandView(0))
    }

    val namesCallback = scope.modState(s => {
      val names = NamesStore.getNames
      s.copy(gotNames=true, names=names)
    })

    def didMount() = CallbackTo {
      logger.info("ViewPlayersVeryFirstRound.didMount")
      NamesStore.ensureNamesAreCached(Some(namesCallback))
    }

    def willUnmount() = CallbackTo {
      logger.info("ViewPlayersVeryFirstRound.willUnmount")
    }

  }

  val component = ScalaComponent.builder[Props]("ViewPlayersVeryFirstRound")
                            .initialStateFromProps { props => {
                              val chi = props.chicago
                              val (n,s,e,w) =
                                if (chi.rounds.isEmpty) {
                                  (chi.players(0),chi.players(1),chi.players(2),chi.players(3))
                                } else {
                                  val r = chi.rounds(0)
                                  (r.north,r.south,r.east,r.west)
                                }
                              PlayerState(n,s,e,w,North, quintet=chi.isQuintet())
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}
