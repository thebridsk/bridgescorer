package com.github.thebridsk.bridge.client.pages.chicagos

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.EditNamesView
import com.github.thebridsk.bridge.client.bridge.store.ChicagoStore
import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.data.MatchChicago

import scala.scalajs.js
import com.github.thebridsk.bridge.client.components.EnterName

/**
  * Shows the team x board table and has a totals column that shows the number of points the team has.
  *
  * The ScoreboardView object will identify which MatchDuplicate to look at.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * PageEditNames( routerCtl: BridgeRouter[ChicagoPage], page: BaseBoardViewWithPerspective )
  * </code></pre>
  *
  * @author werewolf
  */
object PageEditNames {
  import PageEditNamesInternal._

  case class Props(page: EditNamesView, routerCtl: BridgeRouter[ChicagoPage])

  def apply(page: EditNamesView, routerCtl: BridgeRouter[ChicagoPage]) =
    component(
      Props(page, routerCtl)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object PageEditNamesInternal {
  import PageEditNames._

  val logger: Logger = Logger("bridge.PageEditNames")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      id: MatchChicago.Id,
      players: List[String] = List(),
      newnames: Map[String, String] = Map(),
      nameSuggestions: Option[List[String]] = None
  ) {
    import scala.scalajs.js.JSConverters._

    def reset: State =
      ChicagoStore.getChicago match {
        case Some(mc) if mc.id == id =>
          copy(id, mc.players, Map(), nameSuggestions)
        case _ =>
          copy(id, Nil, Map(), nameSuggestions)
      }
    def getSuggestions: js.Array[String] =
      nameSuggestions.getOrElse(List()).toJSArray
    def gettingNames = nameSuggestions.isEmpty

    def getName(p: String): String = newnames.getOrElse(p, p)

  }

  private[chicagos] val Header = ScalaComponent
    .builder[Props]("PageEditNames.Header")
    .render_P(props => {
      <.thead(
        <.tr(
          <.th("Old Name"),
          <.th("New Name")
        )
      )

    })
    .build

  private def noNull(s: String) = Option(s).getOrElse("")
  private def playerValid(s: String) = s != null && s.length != 0

  private[chicagos] val TeamRow = ScalaComponent
    .builder[(Int, String, Backend, State, Props)]("PageEditNames.TeamRow")
    .render_P(args => {
      val (row, player, backend, st, pr) = args
      val busy = st.gettingNames
      val names = st.getSuggestions
      logger.fine(s"""busy=${busy}, names=${names}""")
      <.tr(
        <.td(player),
        <.td(
          EnterName(
            id = "I_" + row,
            name = st.getName(player),
            tabIndex = -1,
            onChange = backend.setPlayer(player) _
          )
        )
      )
    })
    .build

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      import ChicagoStyles._
      import com.github.thebridsk.bridge.clientcommon.react.Utils._
      logger.info(
        "Rendering " + props.page + " suggestions=" + state.nameSuggestions
      )
      <.div(
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span(
                "Edit Names"
              )
            )
          ),
          helpurl = "../help/chicago/summary.html",
          routeCtl = props.routerCtl
        )(
        ),
        ChicagoStore.getChicago match {
          case Some(mc) if mc.id == props.page.chiid =>
            val names = state.newnames.values.map { p => p.trim() }.toList
            val namesvalid = names.find(p => p == "").isEmpty
            val valid = namesvalid && names.length == names.distinct.length
            <.div(
              chiStyles.divEditNamesPage,
              <.h2("Only change the spelling of a players name"),
              <.h2("or replace a player."),
              <.h2("Duplicate names are not allowed."),
              <.h2("Do NOT swap players."),
              <.table(
                Header(props),
                <.tbody(
                  state.players.sorted.zipWithIndex.map { e =>
                    val (p, i) = e
                    TeamRow.withKey(i)((i, p, this, state, props))
                  }.toTagMod
                )
              ),
              !valid ?= <.h2(
                if (namesvalid) "There is a duplicate name"
                else "A name is missing"
              ),
              AppButton(
                "OK",
                "OK",
                ^.onClick --> okCallback,
                ^.disabled := !valid
              ),
              " ",
              AppButton("Reset", "Reset", ^.onClick --> resetCallback),
              " ",
              AppButton(
                "Cancel",
                "Cancel",
                props.routerCtl.setOnClick(props.page.toSummaryView)
              )
            )
          case _ =>
            HomePage.loading
        }
      )
    }

    def setPlayer(player: String)(name: String): Callback =
      scope.modState(ps => ps.copy(newnames = ps.newnames + (player -> name)))

    val doUpdate: Callback = scope.state >>= { state =>
      Callback {
        ChicagoStore.getChicago match {
          case Some(mc) =>
            mc.modifyPlayers(
              state.newnames.map { e => (e._1, e._2.trim()) }
            ) match {
              case Some(newmc) =>
                logger.fine(s"""Updating names to $newmc from $mc""")
                ChicagoController.updateMatch(newmc)
              case None =>
                logger.fine(s"""Did not change names $mc""")
            }
          case None =>
            logger.fine(s"""No match to update""")
        }
      }
    }

    def okCallback: Callback =
      doUpdate >> scope.props >>= { props =>
        props.routerCtl.set(props.page.toSummaryView)
      }

    val resetCallback: Callback = scope.props >>= { props =>
      scope.modState(s => s.reset)
    }

    val storeCallback: Callback = scope.modState { s => s.reset }

    val namesCallback: Callback = scope.modState { s =>
      val sug = NamesStore.getNames
      logger.fine(s"""Got names ${sug}""")
      s.copy(nameSuggestions = Some(sug.toList))
    }

    val didMount: Callback = scope.props >>= { props =>
      Callback {
        logger.info("PageEditNames.didMount")
        NamesStore.ensureNamesAreCached(Some(namesCallback))
        ChicagoStore.addChangeListener(storeCallback)

        ChicagoController.monitor(props.page.chiid)
      }
    }

    val willUnmount: Callback = Callback {
      logger.info("PageEditNames.willUnmount")
      ChicagoStore.removeChangeListener(storeCallback)
      ChicagoController.delayStop()
    }
  }

  def didUpdate(
      cdu: ComponentDidUpdate[Props, State, Backend, Unit]
  ): Callback =
    Callback {
      val props = cdu.currentProps
      val prevProps = cdu.prevProps
      if (prevProps.page != props.page) {
        ChicagoController.monitor(props.page.chiid)
      }
    }

  private[chicagos] val component = ScalaComponent
    .builder[Props]("PageEditNames")
    .initialStateFromProps { props => State(props.page.chiid).reset }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .componentDidUpdate(didUpdate)
    .build
}
