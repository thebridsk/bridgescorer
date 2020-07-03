package com.github.thebridsk.bridge.client.pages.chicagos

import scala.scalajs.js

import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.West
import com.github.thebridsk.bridge.client.pages.info.InfoPage
import com.github.thebridsk.bridge.clientcommon.react.Utils.ExtendReactEventFromInput
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.react.ComboboxOrInput
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import com.github.thebridsk.bridge.clientcommon.react.RadioButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import scala.scalajs.js.JSON

object ViewPlayersVeryFirstRound {
  import PagePlayers._
  import PagePlayersInternal._

  def apply( props: Props ) = component(props)

  class Backend(scope: BackendScope[Props, PlayerState]) {
    // PlayerState.dealer contains "north", "south", "east", "west"

    def setNorth( text: String ) = scope.modState( ps => {traceSetname("setNorth",ps.copy(north=text))})
    def setSouth( text: String ) = scope.modState( ps => {traceSetname("setSouth",ps.copy(south=text))})
    def setEast( text: String ) =  scope.modState( ps => {traceSetname("setEast",ps.copy(east=text))})
    def setWest( text: String ) =  scope.modState( ps => {traceSetname("setWest",ps.copy(west=text))})

    def setExtra( text: String ) = scope.modState( ps => {traceSetname("setExtra",ps.copy(extra=Some(text)))})

    def traceSetname( pos: String, state: PlayerState ): PlayerState = {
      logger.fine("ViewPlayersVeryFirstRound: Setting player "+pos+": "+state)
      state
    }

    val reset = scope.modState( ps => PlayerState("","","","",None, gotNames = ps.gotNames, names=ps.names))

    def setFirstDealer( p: PlayerPosition ) = scope.modState(ps => ps.copy(dealer=Some(p)))

    private def noNull( s: String ) = Option(s).getOrElse("")

    def render( props: Props, state: PlayerState ) = {
      import ChicagoStyles._
      val valid = state.isValid()
      val errormsg = if (valid) ""
                     else if (!state.areAllPlayersValid()) "Please enter missing player name(s)"
                     else if (!state.isDealerValid()) "Please select a dealer"
                     else if (!state.areAllPlayersUnique()) "Please fix duplicate player names"
                     else "Unknown error"
      def getButton(position: PlayerPosition, player: String,  tabindex: Int) =
        AppButton("Player"+position.pos+"FirstDealer",
                  "Dealer",
                  baseStyles.nameButton,
                  ^.onClick --> setFirstDealer(position),
                  BaseStyles.highlight( selected=state.isDealer(position), required=state.dealer.isEmpty ),
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
            ComboboxOrInput(
              callback = cb,
              defaultvalue = noNull(name),
              data = names,
              filter = "startsWith",
              tabIndex = tabInput,
              name = playerPos,
              msgEmptyList="No suggested names",
              msgEmptyFilter="No names matched",
              busy = busy,
              id=s"Combo_${playerPos}"
            ),
            BaseStyles.highlight( requiredName = !playerValid(name) )
          )
        )
      }

      def isExtraValid() = {
        state.extra.getOrElse("").length() > 0
      }

      def logClick( event: ReactMouseEvent ) = event.extract( e => js.Object.assign(js.Object(),e).asInstanceOf[ReactMouseEvent])( e => Callback {
        logger.fine(s"TD clicked event=${e.asInstanceOf[js.Dictionary[js.Any]].map { case (k,v) =>
          (k, v.toString)
        } }")
      })

      <.div(
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit,
            )(
                <.span( "Enter players and identify first dealer" )
            )),
          helpurl = if (state.chicago5) "../help/chicago/five/names5.html" else "../help/chicago/four/names4.html",
          routeCtl = props.router
        )(),
        <.div(
          chiStyles.viewPlayersVeryFirstRound,
          <.div(
            <.h1(InfoPage.showOnlyInLandscapeOnTouch(), "Rotate to portrait for a better view"),
            <.table(
              <.tbody(
                <.tr(
                  // !state.chicago5 ?= baseStyles.collapse,
                  state.chicago5 ?= TagMod(
                    <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7),
                    <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7),
                    <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7,
                      <.div(
                        "Sitting out",
                        <.br,
                        ComboboxOrInput(
                          callback = setExtra,
                          defaultvalue = noNull(state.extra.getOrElse("")),
                          data = names,
                          filter = "startsWith",
                          tabIndex = 9,
                          name = "Extra",
                          msgEmptyList="No suggested names",
                          msgEmptyFilter="No names matched",
                          busy = busy
                        ),
                        CheckBox( "Quintet", "Fast Rotation", state.quintet, toggleQuintet ),
                        if (state.quintet) {
                          Seq[TagMod](
                            RadioButton( "Simple", "Simple Rotation", state.simpleRotation, setSimpleRotation(true) ),
                            RadioButton( "Fair", "Fair Rotation", !state.simpleRotation, setSimpleRotation(false) )
                          ).toTagMod
                        } else {
                          EmptyVdom
                        },
                      ),
                      BaseStyles.highlight( required = !isExtraValid() )
                    )
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
                  <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7, ^.onClick ==> logClick),
                  <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("North", North, state.north, false, setNorth, 4, 8)),
                  <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7)
                )
              )
            ),
            <.div(
              ^.id:="ErrorMsg",
              <.p(
                !valid ?= errormsg
              )
            ),
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton( "Ok", "OK", ^.disabled := !valid, BaseStyles.highlight( required=valid ), ^.onClick-->ok ),
                AppButton(
                  "ToggleFive",
                  if (state.chicago5) "Four" else "Five",
                  ^.onClick-->doChicagoFive
                )
              ),
              <.div(
                baseStyles.divFooterCenter,
                AppButton( "ResetNames", "Reset", ^.onClick --> reset)
              ),
              <.div(
                baseStyles.divFooterRight,
                AppButton( "Cancel", "Cancel", props.router.setOnClick(props.page.toSummaryView) ),
              )
            )
          )
        )
      )
    }

    val toggleQuintet = scope.modState( s=> s.copy( quintet = !s.quintet))

    val toggleSimpleRotation = scope.modState( s=> s.copy( simpleRotation = !s.simpleRotation))

    def setSimpleRotation( simple: Boolean ) = scope.modState( s=> s.copy( simpleRotation = simple))

    val doChicagoFive = scope.modState({ ps => ps.copy(chicago5 = !ps.chicago5) } )

    val ok = scope.stateProps { (state,props) =>
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
      val chinames = props.chicago.setPlayers( north, south, east, west )
      val chine = e.map( ext => chinames.playChicago5(ext)).getOrElse(chinames)
      val chineq = if (quintet) chine.setQuintet(state.simpleRotation)
                   else chine
//      ChicagoController.updateChicagoNames(props.chicago.id, north, south, east, west, e, quintet, state.simpleRotation)
      val chi2 = if (props.chicago.rounds.isEmpty) {
        val r = Round.create("0",
             north,
             south,
             east,
             west,
             state.getDealer,
             Nil )
        chineq.addRound(r)
      } else {
        val r = props.chicago.rounds(0).copy(north=north, south=south, east=east, west=west, dealerFirstRound=state.getDealer)
        chineq.updateRound(r)
      }
//      ChicagoController.updateChicagoRound(props.chicago.id, r)

      ChicagoController.updateMatch(chi2)

      props.router.set(props.page.toHandView(0))
    }

    val namesCallback = scope.modState(s => {
      val names = NamesStore.getNames
      s.copy(gotNames=true, names=names)
    })

    val didMount = CallbackTo {
      logger.info("ViewPlayersVeryFirstRound.didMount")
      NamesStore.ensureNamesAreCached(Some(namesCallback))
    }

    val willUnmount = CallbackTo {
      logger.info("ViewPlayersVeryFirstRound.willUnmount")
    }

  }

  val component = ScalaComponent.builder[Props]("ViewPlayersVeryFirstRound")
                            .initialStateFromProps { props => {
                              val chi = props.chicago
                              val (n,s,e,w, dealer) =
                                if (chi.rounds.isEmpty) {
                                  (chi.players(0),chi.players(1),chi.players(2),chi.players(3), None)
                                } else {
                                  val r = chi.rounds(0)
                                  (r.north,r.south,r.east,r.west, Some(PlayerPosition(r.dealerFirstRound)))
                                }
                              val extra = chi.players.drop(4).headOption
                              PlayerState(
                                n,s,e,w,
                                dealer = dealer,
                                chicago5 = extra.isDefined,
                                quintet=chi.isQuintet(),
                                simpleRotation=chi.simpleRotation,
                                extra = extra
                              )
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}
