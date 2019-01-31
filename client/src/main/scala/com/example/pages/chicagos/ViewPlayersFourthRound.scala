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
import com.example.react.HelpButton
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor

object ViewPlayersFourthRound {
  import PagePlayers._

  def apply( props: Props ) = component(props)

  class Backend(scope: BackendScope[Props, ViewPlayersSecondRound.State]) {

    def show( desc: String, ps: ViewPlayersSecondRound.State ) = ps

    def setNorth(p: String)( e: ReactEventFromInput ) = scope.modState( ps => {show("setNorth",ps.copy(north=p, south="", east="", west="", changingScoreKeeper = false))})
    def setSouth(p: String)( e: ReactEventFromInput ) = scope.modState( ps => {show("setSouth",complete(ps.removePlayer(p).copy(south=p)))})
    def setEast(p: String)( e: ReactEventFromInput ) = scope.modState( ps => {show("setEast",complete(ps.removePlayer(p).copy(east=p)))})
    def setWest(p: String)( e: ReactEventFromInput ) = scope.modState( ps => {show("setWest",complete(ps.removePlayer(p).copy(west=p)))})

    def setFirstDealer( p: PlayerPosition ) = scope.modState(ps => ps.copy(dealer=Some(p)))

    val changeScoreKeeper = scope.modState(s => s.copy(changingScoreKeeper = true))

    val reset = scope.modState(s=> s.copy(north=s.north, south="", east="", west="", changingScoreKeeper = false) )

    /**
     * Only call from within a scope.modState()
     */
    def complete( state: ViewPlayersSecondRound.State ) = {
      val props = scope.withEffectsImpure.props

      val lastRound = props.chicago.rounds( props.chicago.rounds.size-1 )

      val ns = lastRound.north::lastRound.south::Nil
      val ew = lastRound.east::lastRound.west::Nil

      val (potentialS,lastsouth) = if (ns.contains(state.north)) (ew,if (ns(0) == state.north) ns(1); else ns(0));
                                   else (ns,if (ew(0) == state.north) ew(1); else ew(0))
      var potentialSouth = potentialS ::: (lastsouth::Nil)
      var potentialEast = potentialSouth
      var potentialWest = potentialSouth

      def remove( player: String ) = {
        potentialSouth = potentialSouth.filter { p => p!=player }
        potentialEast = potentialEast.filter { p => p!=player }
        potentialWest = potentialWest.filter { p => p!=player }
      }

      var south = state.south
      var east = state.east
      var west = state.west

      if (south != "") remove(south)
      if (east != "") remove(east)
      if (west != "") remove(west)

      if (south == "" && potentialSouth.size == 1) {
        south = potentialSouth.head
        remove(south)
      }
      if (east == "" && potentialEast.size == 1) {
        east = potentialEast.head
        remove(east)
      }
      if (west == "" && potentialWest.size == 1) {
        west = potentialWest.head
        remove(west)
      }
      if (east == "" && potentialEast.size == 1) {
        east = potentialEast.head
        remove(east)
      }
      if (south == "" && potentialSouth.size == 1) {
        south = potentialSouth.head
        remove(south)
      }

      state.copy(south=south, east=east, west=west)
    }

    def render( props: Props, state: ViewPlayersSecondRound.State ) = {
      import ChicagoStyles._

      val lastRound = props.chicago.rounds( props.chicago.rounds.size-1 )

      val ns = lastRound.north::lastRound.south::Nil
      val ew = lastRound.east::lastRound.west::Nil

      val allFromLastRound = ns:::ew

      val (potential,lastsouth) = if (ns.contains(state.north)) (ew,if (ns(0) == state.north) ns(1); else ns(0));
                                  else (ns,if (ew(0) == state.north) ew(1); else ew(0))
      val potentialE = potential ::: ( lastsouth :: Nil )
      val potentialW = potentialE
      val potentialS = potentialE

      // check if all players from last round are mentioned in new round
      val valid = state.isDealerValid() && (allFromLastRound forall (state.allPlayers contains _)) && !state.changingScoreKeeper

      val allassignedplayers = state.allPlayers

      val extraWidth = Properties.defaultChicagoNameButtonPaddingAndBorder +
                       Properties.defaultChicagoNameButtonBorderRadius
      val maxPlayerLen = s"${Pixels.maxLength( allFromLastRound: _* )+extraWidth}px"

      def getButton( id: String, p: String, current: String, action: ReactEventFromInput=>Callback, missingRequired: Boolean ): TagMod = {
        AppButton( id, p,
                  baseStyles.nameButton,
                  ^.width:=maxPlayerLen,
                  BaseStyles.highlight(
                      selected= p==current,
                      required=missingRequired
                  ),
                  ^.disabled:=p!=current && allassignedplayers.contains(p),
                  ^.onClick ==> action )
      }

      def getButtons( id: String, players: List[String], current: String, setter: (String)=>(ReactEventFromInput)=>Callback ) = {
        var i: Int = 0
        val missingRequired = !players.contains(current)
        players.flatMap { p =>
          i=i+1
          Seq[TagMod](
            (i==3) ?= <.br,
            getButton( id+i, p, current, setter(p), missingRequired )
          )
        }.toTagMod
      }

      def getDealerButton(position: PlayerPosition, player: String,  tabindex: Int) =
          AppButton("Player"+position.pos+"FirstDealer",
                    "Dealer",
                    baseStyles.nameButton,
                    ^.onClick --> setFirstDealer(position),
                    BaseStyles.highlight( selected=state.isDealer(position) ),
                    ^.tabIndex:=tabindex
                  )

      <.div(
        chiStyles.viewPlayersFourthRound,
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit,
            )(
                <.span( "Select partners and first dealer" )
            )),
          helpurl = "../help/chicago/four/selectnames4.html",
          routeCtl = props.router
        )(),
//        <.h1("Select partners and first dealer"),
        <.table(
          <.tbody(
            <.tr(
                  <.td(),
                  <.td(
//                        "South",
//                        <.br,
                        getButtons("South", potentialS, state.south, setSouth ),
                        <.br,
                        getDealerButton(South,state.south,8)
                      ),
                  <.td() ),
            <.tr(
                  <.td(
//                        "East",
//                        <.br,
                        getButtons("East", potentialE, state.east, setEast ),
                        <.br,
                        getDealerButton(East,state.east,8)
                      ),
                  <.td(),
                  <.td(
//                        "West",
//                        <.br,
                        getButtons("West", potentialW, state.west, setWest ),
                        <.br,
                        getDealerButton(West,state.west,8)
                      ) ),
            <.tr(
                  <.td(),
                  <.td(
//                        "North, "+
                        "Scorekeeper",
                        <.br,
                        if (state.changingScoreKeeper) {
                          var i = 0
                          <.span(
                              allFromLastRound.map { p =>
                                i=i+1
                                AppButton("ChangeScoreKeeper"+i, p,
                                          baseStyles.nameButton,
                                          ^.width:=maxPlayerLen,
                                          BaseStyles.highlight( selected = p==state.north ),
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
            AppButton( "Reset", "Reset", baseStyles.appButton, ^.onClick --> reset),
//            HelpButton("../help/chicago/four/selectnames4.html")
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

  val component = ScalaComponent.builder[Props]("ViewPlayersFourthRound")
                            .initialStateFromProps { props => {
                              val lr = props.chicago.rounds( props.chicago.rounds.size-1 )
                              ViewPlayersSecondRound.State(lr.north,"","","",None,false)
                            } }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}
