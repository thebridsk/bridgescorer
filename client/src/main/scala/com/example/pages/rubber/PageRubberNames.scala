package com.example.pages.rubber

import scala.scalajs.js
import scala.scalajs.js.UndefOr.any2undefOrA

import org.scalajs.dom.raw.Element

import com.example.bridge.store.NamesStore
import com.example.controller.RubberController
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
import com.example.routes.BridgeRouter
import utils.logging.Logger
import com.example.bridge.store.RubberStore
import com.example.react.ComboboxOrInput
import com.example.pages.rubber.RubberRouter.RubberMatchNamesView
import com.example.react.AppButton
import com.example.react.Utils._
import com.example.pages.rubber.RubberRouter.ListView

object PageRubberNames {
  import PageRubberNamesInternal._

  case class Props(page: RubberMatchNamesView, router: BridgeRouter[RubberPage])

  def apply(page: RubberMatchNamesView, router: BridgeRouter[RubberPage]) =
    component(Props(page,router))

}

object PageRubberNamesInternal {
  import PageRubberNames._

  val logger = Logger("bridge.PageRubberNames")

  def playerValid( s: String ) = s.length!=0

  case class PlayerState( north: String,
                          south: String,
                          east: String,
                          west: String,
                          dealer: Option[PlayerPosition],
                          gotNames: Boolean = false,
                          names: List[String] = Nil
                        ) {
    def isDealerValid() = dealer.isDefined
    def areAllPlayersValid() = playerValid(north) && playerValid(south) && playerValid(east) && playerValid(west)

    def isValid() = areAllPlayersValid()&& isDealerValid()

    def isDealer( p: PlayerPosition ) = dealer match {
      case Some(d) => d == p
      case None => false
    }

    def isDealer(p: String) =
        p match {
          case `north` => dealer == North
          case `south` => dealer == South
          case `east` =>  dealer == East
          case `west` =>  dealer == West
          case _ => false
        }

    def getDealerName() = dealer match {
      case Some(d) =>
        d match {
          case North => north
          case South => south
          case East => east
          case West => west
        }
      case None => ""
    }
  }

  class Backend(scope: BackendScope[Props, PlayerState]) {

    def traceSetname( pos: String, state: PlayerState ): PlayerState = {
      logger.finer("PageRubberNames: Setting player "+pos+": "+state)
      state
    }

    def setNorth( text: String ): Callback = scope.modState( ps => {traceSetname("North",ps.copy(north=text))})
    def setSouth( text: String ): Callback = scope.modState( ps => {traceSetname("South",ps.copy(south=text))})
    def setEast( text: String ): Callback =  scope.modState( ps => {traceSetname("East",ps.copy(east=text))})
    def setWest( text: String ): Callback =  scope.modState( ps => {traceSetname("West",ps.copy(west=text))})

    def reset = scope.modState( ps => PlayerState("","","","",None, gotNames = ps.gotNames, names=ps.names))

    def setFirstDealer( p: PlayerPosition ) = scope.modState(ps => ps.copy(dealer=Some(p)))

    private def noNull( s: String ) = if (s == null) ""; else s

    def render( props: Props, state: PlayerState ) = {
      import RubberStyles._
      val valid = state.isValid()
      def getButton(position: PlayerPosition, player: String,  tabindex: Int) =
          AppButton("Player"+position.pos+"FirstDealer",
                    "Dealer",
                    ^.onClick --> setFirstDealer(position),
                    state.isDealer(position) ?= baseStyles.buttonSelected,
                    state.dealer.isEmpty ?= baseStyles.required,
                    ^.tabIndex:=tabindex
                  )
      import scala.scalajs.js.JSConverters._

      val busy = !state.gotNames
      val names = state.names.toJSArray

        //              "South", South, state.south, false, setSouth, onChangeS, 2, 6
      def putName( playerPos: String, playerPosition: PlayerPosition, name: String, scorekeeper: Boolean, cb: String=>Callback, tabInput: Int, tabDealer: Int ) = {
        <.table(
          <.tbody(
            <.tr(
              <.td( <.span( !playerValid(name) ?= baseStyles.required, playerPos)),
              <.td( getButton(playerPosition,name,tabDealer)),
              <.td( scorekeeper ?= "Scorekeeper" )
            ),
            <.tr(
              <.td( !playerValid(name) ?= baseStyles.requiredName,
                  ^.colSpan := 3,
                    ComboboxOrInput( cb, noNull(name), names, "startsWith", tabInput, playerPos,
                                     msgEmptyList="No suggested names", msgEmptyFilter="No names matched", busy=busy)
              )
            )
          )
        )
      }

      <.div(
        rubStyles.namesPage,
        <.h1(InfoPage.showOnlyInLandscapeOnTouch(), "Rotate to portrait for a better view"),
        <.h1("Enter players and identify first dealer"),
        <.table(
          <.tbody(
            <.tr(
                  <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7),
                  <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("South", South, state.south, false, setSouth, 2, 6) ),
                  <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7)
                 ),
            <.tr(
                  <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("East", East, state.east, false, setEast, 1, 5) ),
                  <.td( ^.colSpan := 1, tableStyles.tableCellWidth1Of7),
                  <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("West", West, state.west, false, setWest, 3, 7) )
                ),
            <.tr(
                  <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7),
                  <.td( ^.colSpan := 3, tableStyles.tableCellWidth3Of7, putName("North", North, state.north, true, setNorth, 4, 8) ),
                  <.td( ^.colSpan := 2, tableStyles.tableCellWidth2Of7)
                )
          )
        ),
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "Ok", "OK", ^.disabled := !valid, valid ?= baseStyles.requiredNotNext, ^.onClick-->ok() )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "ResetNames", "Reset", ^.onClick --> reset )
          ),
          <.div(
            baseStyles.divFooterRight,
            AppButton( "Cancel", "Cancel", props.router.setOnClick( ListView /*props.page.toRubber()*/) )
          )
        )
      )
    }

    def ok() = CallbackTo {
      val state = scope.withEffectsImpure.state
      val props = scope.withEffectsImpure.props
      RubberController.updateRubberNames(props.page.rid, state.north.trim, state.south.trim, state.east.trim, state.west.trim, state.dealer.get)
      props
    } >>= {
      props => props.router.set(props.page.toRubber())
    }

    val storeCallback = Callback { scope.withEffectsImpure.modState(s => {
      val rubid = scope.withEffectsImpure.props.page.rid
      val (north,south,east,west,dealer) = getNamesFromStore(rubid)
      s.copy(north, south, east, west, dealer)
    })}

    val namesCallback = scope.modState(s => {
      val names = NamesStore.getNames
      s.copy(gotNames=true, names=names)
    })

    def didMount() = CallbackTo {
      logger.info("PageRubberNames.didMount")
      NamesStore.ensureNamesAreCached(Some(namesCallback))
      RubberStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      RubberController.ensureMatch(p.page.rid))
    }

    def willUnmount() = CallbackTo {
      logger.info("PageRubberNames.willUnmount")
      RubberStore.removeChangeListener(storeCallback)
    }

  }

  def getNamesFromStore( rubid: String ): (String,String,String,String,Option[PlayerPosition]) = {
    RubberStore.getRubber match {
      case Some(rub) if (rub.id == rubid) =>
        val dealer = try {
          Some(PlayerPosition(rub.dealerFirstHand))
        } catch {
          case _: Exception => None
        }
        (rub.north,rub.south,rub.east,rub.west,dealer)
      case _ => ("", "", "", "", None)
    }
  }

  val component = ScalaComponent.builder[Props]("PageRubberNames")
                            .initialStateFromProps { props => {
                              val rubid = props.page.rid
                              val (n,s,e,w,dealer) = getNamesFromStore(rubid)
                              PlayerState(n,s,e,w,dealer)
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}
