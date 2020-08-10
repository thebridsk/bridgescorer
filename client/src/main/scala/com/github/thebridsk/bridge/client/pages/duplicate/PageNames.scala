package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.NamesView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.ComboboxOrInput
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

import scala.scalajs.js

/**
  * Shows the team x board table and has a totals column that shows the number of points the team has.
  *
  * The ScoreboardView object will identify which MatchDuplicate to look at.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * PageNames( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
  * </code></pre>
  *
  * @author werewolf
  */
object PageNames {
  import PageNamesInternal._

  case class Props(
      routerCtl: BridgeRouter[DuplicatePage],
      page: NamesView,
      returnPage: DuplicatePage
  )

  def apply(
      routerCtl: BridgeRouter[DuplicatePage],
      page: NamesView,
      returnPage: DuplicatePage
  ) =
    component(
      Props(routerCtl, page, returnPage)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object PageNamesInternal {
  import PageNames._

  val logger: Logger = Logger("bridge.PageNames")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      teams: Map[Team.Id, Team] = Map(),
      nameSuggestions: Option[List[String]] = None
  ) {
    import scala.scalajs.js.JSConverters._

    def reset: State =
      DuplicateStore.getMatch() match {
        case Some(md) => copy(teams = md.teams.map(t => t.id -> t).toMap)
        case None     => copy(teams = Map())
      }

    def getSuggestions: js.Array[String] =
      nameSuggestions.getOrElse(List()).toJSArray
    def gettingNames = nameSuggestions.isEmpty

  }

  private[duplicate] val Header = ScalaComponent
    .builder[Props]("PageNames.Header")
    .render_P(props => {
      <.thead(
        <.tr(
          <.th("Team"),
          <.th("Player1"),
          <.th("Player2")
        )
      )

    })
    .build

  private def noNull(s: String) = Option(s).getOrElse("")
  private def playerValid(s: String) = s != null && s.length != 0

  private[duplicate] val TeamRow = ScalaComponent
    .builder[(Team, Backend, State, Props)]("PageNames.TeamRow")
    .render_P(args => {
      val (team, backend, st, p) = args
      val busy = st.gettingNames
      val names = st.getSuggestions
      logger.fine(s"""busy=${busy}, names=${names}""")
      <.tr(
        <.td(team.id.toNumber),
        <.td(
          ComboboxOrInput(
            p => backend.setPlayer(team.id, 1)(p),
            noNull(team.player1),
            names,
            "startsWith",
            -1,
            "I_" + team.id.id + "_1",
            msgEmptyList = "No suggested names",
            msgEmptyFilter = "No names matched",
            id = "I_" + team.id.id + "_1",
            busy = busy
          )
        ),
        <.td(
          ComboboxOrInput(
            p => backend.setPlayer(team.id, 2)(p),
            noNull(team.player2),
            names,
            "startsWith",
            -1,
            "I_" + team.id.id + "_2",
            msgEmptyList = "No suggested names",
            msgEmptyFilter = "No names matched",
            id = "I_" + team.id.id + "_2",
            busy = busy
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
      import DuplicateStyles._
      import com.github.thebridsk.bridge.clientcommon.react.Utils._
      logger.info(
        "Rendering " + props.page + " suggestions=" + state.nameSuggestions
      )
      <.div(
        DuplicatePageBridgeAppBar(
          id = Some(props.page.dupid),
          tableIds = List(),
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
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(
        ),
        DuplicateStore.getMatch() match {
          case Some(md) if md.id == props.page.dupid =>
            val names = state.teams
              .flatMap(t => t._2.player1.trim :: t._2.player2.trim :: Nil)
              .toList
            val namesvalid = names.find(p => p == null || p == "").isEmpty
            val valid = namesvalid && names.length == names.distinct.length
            <.div(
              dupStyles.divNamesPage,
              //              <.h1("Edit Names"),
              <.h2("Only change the spelling of a players name"),
              <.h2("or replace a player."),
              <.h2("Duplicate names are not allowed."),
              <.h2("Do NOT swap players."),
              <.table(
                Header(props),
                <.tbody(
                  state.teams.values.toList
                    .sortWith((t1, t2) => t1.id < t2.id)
                    .map { team =>
                      TeamRow.withKey(team.id.id)((team, this, state, props))
                    }
                    .toTagMod
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
                props.routerCtl.setOnClick(props.returnPage)
              )
            )
          case _ =>
            HomePage.loading
        }
      )
    }

    def setPlayer(teamid: Team.Id, player: Int)(name: String): Callback =
      scope.modState(ps => {
        ps.teams.get(teamid) match {
          case Some(team) =>
            val newteam = team.setPlayers(
              if (player == 1) name else team.player1,
              if (player == 2) name else team.player2
            )
            val s = ps.copy(teams = ps.teams + (team.id -> newteam))
            logger.fine(
              s"""Updating name ${teamid} ${player} to ${name}: ${s}"""
            )
            s
          case None =>
            logger.fine(s"""Did not find team ${teamid}""")
            ps
        }
      })

    val doUpdate: Callback = scope.state >>= { state =>
      Callback {
        DuplicateStore.getMatch() match {
          case Some(md) =>
            state.teams.values.foreach { team =>
              {
                val t = team.copy(
                  player1 = team.player1.trim,
                  player2 = team.player2.trim
                )
                val changed = md.getTeam(t.id) match {
                  case Some(original) =>
                    val changed = !t.equalsIgnoreModifyTime(original)
                    if (changed) {
                      logger.fine(s"""Updating team ${t}: was ${original}""")
                      Controller.updateTeam(md, t)
                    } else {
                      logger.fine(s"""Updating team ${t}: was ${original}""")
                    }
                  case None =>
                    logger.fine(
                      s"""Updating team ${t}: did not find old team"""
                    )
                }
              }
            }
          case None =>
        }
      }
    }

    def okCallback: Callback =
      doUpdate >> scope.props >>= { props =>
        props.routerCtl.set(props.returnPage)
      }

    val resetCallback: Callback = scope.props >>= { props =>
      scope.modState(s => s.reset)
    }

    val storeCallback: Callback = scope.modState { s => s.reset }

    val namesCallback: Callback = scope.modState { s =>
      val sug = NamesStore.getNames
      logger.fine(s"""Got names ${sug}""")
      s.copy(nameSuggestions = Some(sug))
    }

    val didMount: Callback = scope.props >>= { props =>
      Callback {
        logger.info("PageNames.didMount")
        NamesStore.ensureNamesAreCached(Some(namesCallback))
        DuplicateStore.addChangeListener(storeCallback)

        Controller.monitor(props.page.dupid)
      }
    }

    val willUnmount: Callback = Callback {
      logger.info("PageNames.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
    }
  }

  def didUpdate(
      cdu: ComponentDidUpdate[Props, State, Backend, Unit]
  ): Callback =
    Callback {
      val props = cdu.currentProps
      val prevProps = cdu.prevProps
      if (prevProps.page != props.page) {
        Controller.monitor(props.page.dupid)
      }
    }

  private[duplicate] val component = ScalaComponent
    .builder[Props]("PageNames")
    .initialStateFromProps { props => State().reset }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .componentDidUpdate(didUpdate)
    .build
}
