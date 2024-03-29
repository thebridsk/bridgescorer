package com.github.thebridsk.bridge.client.pages.rubber

import com.github.thebridsk.bridge.client.controller.RubberController
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.West
import com.github.thebridsk.bridge.client.pages.info.InfoPage

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.RubberStore
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.RubberMatchNamesView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.ListView
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.client.components.EnterName

object PageRubberNames {
  import Internal._

  case class Props(page: RubberMatchNamesView, router: BridgeRouter[RubberPage])

  def apply(
      page: RubberMatchNamesView,
      router: BridgeRouter[RubberPage]
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(page, router))

  protected object Internal {

    val logger: Logger = Logger("bridge.PageRubberNames")

    def playerValid(s: String): Boolean = s.length != 0

    case class PlayerState(
        north: String,
        south: String,
        east: String,
        west: String,
        dealer: Option[PlayerPosition],
    ) {
      def isDealerValid() = dealer.isDefined
      def areAllPlayersValid(): Boolean =
        playerValid(north) && playerValid(south) && playerValid(
          east
        ) && playerValid(west)

      def areAllPlayersUnique(): Boolean = {
        val p =
          north.trim ::
            south.trim ::
            east.trim ::
            west.trim ::
            Nil
        val before = p.length
        val after = p.distinct.length
        before == after
      }

      def isValid(): Boolean =
        areAllPlayersValid() && isDealerValid() && areAllPlayersUnique()

      def isDealer(p: PlayerPosition): Boolean =
        dealer match {
          case Some(d) => d == p
          case None    => false
        }

      def isDealer(p: String): Boolean =
        p match {
          case `north` => dealer == North
          case `south` => dealer == South
          case `east`  => dealer == East
          case `west`  => dealer == West
          case _       => false
        }

      def getDealerName(): String =
        dealer match {
          case Some(d) =>
            d match {
              case North => north
              case South => south
              case East  => east
              case West  => west
            }
          case None => ""
        }
    }

    class Backend(scope: BackendScope[Props, PlayerState]) {

      def traceSetname(pos: String, state: PlayerState): PlayerState = {
        logger.finer("PageRubberNames: Setting player " + pos + ": " + state)
        state
      }

      def setNorth(text: String): Callback =
        scope.modState(ps => { traceSetname("North", ps.copy(north = text)) })
      def setSouth(text: String): Callback =
        scope.modState(ps => { traceSetname("South", ps.copy(south = text)) })
      def setEast(text: String): Callback =
        scope.modState(ps => { traceSetname("East", ps.copy(east = text)) })
      def setWest(text: String): Callback =
        scope.modState(ps => { traceSetname("West", ps.copy(west = text)) })

      val reset: Callback = scope.modState(ps =>
        PlayerState(
          "",
          "",
          "",
          "",
          None
        )
      )

      def setFirstDealer(p: PlayerPosition): Callback =
        scope.modState(ps => ps.copy(dealer = Some(p)))

      private def noNull(s: String) = Option(s).getOrElse("")

      def render(props: Props, state: PlayerState) = { // scalafix:ok ExplicitResultTypes; React
        import RubberStyles._
        val valid = state.isValid()
        val errormsg =
          if (valid) ""
          else if (!state.areAllPlayersValid())
            "Please enter missing player name(s)"
          else if (!state.areAllPlayersUnique())
            "Please fix duplicate player names"
          else if (!state.isDealerValid()) "Please select a dealer"
          else "Unknown error"
        def getButton(position: PlayerPosition, player: String, tabindex: Int) =
          AppButton(
            "Player" + position.pos + "FirstDealer",
            "Dealer",
            ^.onClick --> setFirstDealer(position),
            BaseStyles.highlight(
              selected = state.isDealer(position),
              required = state.dealer.isEmpty
            ),
            ^.tabIndex := tabindex
          )
        import scala.scalajs.js.JSConverters._

        //              "South", South, state.south, false, setSouth, onChangeS, 2, 6
        def putName(
            playerPos: String,
            playerPosition: PlayerPosition,
            name: String,
            scorekeeper: Boolean,
            cb: String => Callback,
            tabInput: Int,
            tabDealer: Int
        ) = {
          <.table(
            <.tbody(
              <.tr(
  //              <.td( <.span( BaseStyles.highlight(required = !playerValid(name)), playerPos)),
                <.td(getButton(playerPosition, name, tabDealer)),
                <.td(scorekeeper ?= "Scorekeeper")
              ),
              <.tr(
                <.td(
                  ^.colSpan := 3,
                  EnterName(
                    playerPos,
                    name,
                    tabInput,
                    cb
                  ),
                  !playerValid(name) ?= baseStyles.requiredName
                )
              )
            )
          )
        }

        <.div(
          RubberPageBridgeAppBar(
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span("Enter players and identify first dealer")
              )
            ),
            helpurl = "../help/rubber/names.html",
            routeCtl = props.router
          )(),
          <.div(
            rubStyles.namesPage,
            <.h1(
              InfoPage.showOnlyInLandscapeOnTouch(),
              "Rotate to portrait for a better view"
            ),
            <.table(
              <.tbody(
                <.tr(
                  <.td(^.colSpan := 2, tableStyles.tableCellWidth2Of7),
                  <.td(
                    ^.colSpan := 3,
                    tableStyles.tableCellWidth3Of7,
                    putName("South", South, state.south, false, setSouth, 2, 6)
                  ),
                  <.td(^.colSpan := 2, tableStyles.tableCellWidth2Of7)
                ),
                <.tr(
                  <.td(
                    ^.colSpan := 3,
                    tableStyles.tableCellWidth3Of7,
                    putName("East", East, state.east, false, setEast, 1, 5)
                  ),
                  <.td(^.colSpan := 1, tableStyles.tableCellWidth1Of7),
                  <.td(
                    ^.colSpan := 3,
                    tableStyles.tableCellWidth3Of7,
                    putName("West", West, state.west, false, setWest, 3, 7)
                  )
                ),
                <.tr(
                  <.td(^.colSpan := 2, tableStyles.tableCellWidth2Of7),
                  <.td(
                    ^.colSpan := 3,
                    tableStyles.tableCellWidth3Of7,
                    putName("North", North, state.north, true, setNorth, 4, 8)
                  ),
                  <.td(^.colSpan := 2, tableStyles.tableCellWidth2Of7)
                )
              )
            ),
            <.div(baseStyles.divFlexBreak),
            <.div(
              ^.id := "ErrorMsg",
              <.p(
                !valid ?= errormsg
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
                  valid ?= baseStyles.requiredNotNext,
                  ^.onClick --> ok
                )
              ),
              <.div(
                baseStyles.divFooterCenter,
                AppButton("ResetNames", "Reset", ^.onClick --> reset)
              ),
              <.div(
                baseStyles.divFooterRight,
                AppButton(
                  "Cancel",
                  "Cancel",
                  props.router.setOnClick(ListView /*props.page.toRubber()*/ )
                )
                //            HelpButton("../help/rubber/names.html")
              )
            )
          )
        )
      }

      val ok: Callback = CallbackTo {
        val state = scope.withEffectsImpure.state
        val props = scope.withEffectsImpure.props
        RubberController.updateRubberNames(
          props.page.rid,
          state.north.trim,
          state.south.trim,
          state.east.trim,
          state.west.trim,
          state.dealer.get
        )
        props
      } >>= { props =>
        props.router.set(props.page.toRubber)
      }

      val storeCallback: Callback = Callback {
        scope.withEffectsImpure.modState(s => {
          val rubid = scope.withEffectsImpure.props.page.rid
          val (north, south, east, west, dealer) = getNamesFromStore(rubid)
          s.copy(north, south, east, west, dealer)
        })
      }

      val didMount: Callback = CallbackTo {
        logger.info("PageRubberNames.didMount")
        RubberStore.addChangeListener(storeCallback)
      } >> scope.props >>= { (p) =>
        Callback(RubberController.ensureMatch(p.page.rid))
      }

      val willUnmount: Callback = CallbackTo {
        logger.info("PageRubberNames.willUnmount")
        RubberStore.removeChangeListener(storeCallback)
      }

    }

    def getNamesFromStore(
        rubid: MatchRubber.Id
    ): (String, String, String, String, Option[PlayerPosition]) = {
      RubberStore.getRubber match {
        case Some(rub) if (rub.id == rubid) =>
          val dealer =
            try {
              Some(PlayerPosition(rub.dealerFirstHand))
            } catch {
              case _: Exception => None
            }
          (rub.north, rub.south, rub.east, rub.west, dealer)
        case _ => ("", "", "", "", None)
      }
    }

    private[rubber] val component = ScalaComponent
      .builder[Props]("PageRubberNames")
      .initialStateFromProps { props =>
        {
          val rubid = props.page.rid
          val (n, s, e, w, dealer) = getNamesFromStore(rubid)
          PlayerState(n, s, e, w, dealer)
        }
      }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }
}
