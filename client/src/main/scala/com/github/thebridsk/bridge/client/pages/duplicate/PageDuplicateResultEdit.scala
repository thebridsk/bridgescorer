package com.github.thebridsk.bridge.client.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.DuplicateResultView
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.DuplicateResultStore
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.DuplicateResultEditView
import com.github.thebridsk.bridge.data.DuplicateSummaryEntry
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.BoardResults
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicateResult
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.Team
import DuplicateStyles._
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.clientcommon.react.DateTimePicker
import scala.scalajs.js.Date
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.components.EnterName

/**
  * A component page that displays and allows editing of a duplicate result.
  *
  * A duplicate result just identifies the teams, and the points they scored.
  *
  * To use, just code the following:
  *
  * {{{
  * PageDuplicateResultEdit(
  *   routerCtl = router,
  *   page = DuplicateResultEditView("E1")
  * )
  * }}}
  *
  * @see See [[apply]] for description of arguments.
  *
  * @author werewolf
  */
object PageDuplicateResultEdit {
  import Internal._

  case class Props(
      routerCtl: BridgeRouter[DuplicatePage],
      page: DuplicateResultEditView
  )

  /**
    * Instantiate the component
    *
    * @param routerCtl
    * @param page the DuplicateResultView that identifies the match to display.
    *             The Id must be for a DuplicateResult.
    *
    * @return the unmounted react component
    */
  def apply(
      routerCtl: BridgeRouter[DuplicatePage],
      page: DuplicateResultEditView
  ) =
    component(
      Props(routerCtl, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.PageDuplicateResultEdit")

    case class DSE(team: Team, result: String) {

      def isValid: Boolean = {
        try {
          result.toDouble
          team.player1 != null && team.player1 != "" &&
          team.player2 != null && team.player2 != ""
        } catch {
          case _: Exception => false
        }
      }

      def toDuplicateSummaryEntry(useIMP: Boolean): DuplicateSummaryEntry = {
        if (useIMP) {
          DuplicateSummaryEntry(
            team,
            None,
            None,
            None,
            Some(result.toDouble),
            Some(0)
          )
        } else {
          DuplicateSummaryEntry(team, Some(result.toDouble), Some(0))
        }
      }
    }

    def toDSE(dupS: DuplicateSummaryEntry, imp: Boolean): DSE = {
      val v =
        if (imp) dupS.resultImp.getOrElse(0.0).toString()
        else dupS.result.getOrElse(0.0).toString()
      DSE(dupS.team, v)
    }

    def toTeams(teams: List[List[DSE]]): Unit = {}

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(
        original: Option[MatchDuplicateResult] = None, // if None, not initialized
        teams: List[List[DSE]] = List(),
        boardresults: Option[List[BoardResults]] = None,
        played: SystemTime.Timestamp = SystemTime.currentTimeMillis(),
        comment: Option[String] = None,
        notfinished: Boolean = false,
        useIMP: Boolean = false
    ) {

      def getMDR(): MatchDuplicateResult = {

        val time = SystemTime.currentTimeMillis()
        val t = teams.map(l => l.map(e => e.toDuplicateSummaryEntry(useIMP)))
        val c = comment match {
          case Some(c) if (c.length() > 0) => comment
          case _                           => None
        }
        val nf = if (notfinished) Some(true) else None
        val sm =
          if (useIMP) MatchDuplicate.InternationalMatchPoints
          else MatchDuplicate.MatchPoints
        original.get
          .copy(
            results = t,
            boardresults = boardresults,
            comment = c,
            notfinished = nf,
            played = played,
            updated = time,
            scoringmethod = sm
          )
          .fixup
      }

      def setPlayer(iwinnerset: Int, teamid: Team.Id, iplayer: Int)(
          name: String
      ): State = {
        copy(teams = teams.zipWithIndex.map { e =>
          val (ws, i) = e
          if (i == iwinnerset) {
            ws.map { dse =>
              if (dse.team.id == teamid) {
                val time = SystemTime.currentTimeMillis()
                val nt = if (iplayer == 1) {
                  dse.team.copy(player1 = name, updated = time)
                } else {
                  dse.team.copy(player2 = name, updated = time)
                }
                dse.copy(team = nt)
              } else {
                dse
              }
            }
          } else {
            ws
          }
        })
      }

      def setPoints(iwinnerset: Int, teamid: Team.Id)(points: String): State = {
        copy(teams = teams.zipWithIndex.map { e =>
          val (ws, i) = e
          if (i == iwinnerset) {
            ws.map { dse =>
              if (dse.team.id == teamid) {
                val time = SystemTime.currentTimeMillis()
                dse.copy(result = points)
              } else {
                dse
              }
            }
          } else {
            ws
          }
        })
      }

      def updateOriginal(mdr: Option[MatchDuplicateResult]): State = {
        val imp = mdr.map(m => m.isIMP).getOrElse(false)
        val t = mdr
          .map(m => m.results.map(l => l.map(e => toDSE(e, imp))))
          .getOrElse(List())
        val b = mdr.map(m => m.boardresults).getOrElse(None)
        val p = mdr
          .map(m => m.played)
          .filter(x => x != 0)
          .getOrElse(SystemTime.doubleToTimestamp(0))
        val c = mdr.map(m => m.comment).getOrElse(None)
        val f =
          mdr.map(m => m.notfinished).getOrElse(Some(false)).getOrElse(false)
        copy(
          original = mdr,
          teams = t,
          boardresults = b,
          comment = c,
          notfinished = f,
          played = p,
          useIMP = imp
        )
      }

      def isValid(): Boolean = {
        played > 0 &&
        teams.flatten.find(t => !t.isValid).isEmpty
      }

    }

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      val ok: Callback = scope.state >>= { state =>
        Callback {
          val props = scope.withEffectsImpure.props
          state.original match {
            case Some(mdr) =>
              val newmdr = state.getMDR()
              logger.fine(
                s"""Updating, state.played=${state.played} MDR: ${newmdr}"""
              )
              BridgeDispatcher.updateDuplicateResult(newmdr)
              import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
              RestClientDuplicateResult
                .update(newmdr.id, newmdr)
                .recordFailure()
                .foreach { e =>
                  logger.info("Updated " + newmdr)
                }
            case None =>
          }

          if (mounted)
            props.routerCtl.set(DuplicateResultView(props.page.sdupid)).runNow()

        }
      }

      val cancel: Callback = scope.props >>= { props =>
        Callback {

          if (mounted)
            props.routerCtl.set(DuplicateResultView(props.page.sdupid)).runNow()

        }
      }

      def setPlayer(iwinnerset: Int, teamid: Team.Id, iplayer: Int)(
          name: String
      ): Callback =
        scope.modState(s => s.setPlayer(iwinnerset, teamid, iplayer)(name.trim()))

      def setPoints(iwinnerset: Int, teamid: Team.Id)(
          e: ReactEventFromInput
      ): Callback =
        e.inputText(points =>
          scope.modState(s => s.setPoints(iwinnerset, teamid)(points))
        )

      def setPlayed(value: Date): Unit = {
        logger.fine(s"""Setting date to ${value}: ${value.getTime()}""")
        scope
          .modState { s =>
            val t =
              if (js.isUndefined(value) || value == null) 0 else value.getTime()
            val ns = s.copy(played = t)
            logger.fine(s"""New date in state is ${ns.played}""")
            ns
          }
          .runNow()
      }

      val toggleComplete: Callback =
        scope.modState(s => s.copy(notfinished = !s.notfinished))

      val toggleIMP: Callback = scope.modState(s => s.copy(useIMP = !s.useIMP))

      def setComment(e: ReactEventFromInput): Callback =
        e.inputText { comment =>
          scope.modState(s =>
            s.copy(comment =
              if (comment == null || comment == "") None else Some(comment)
            )
          )
        }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        def getWinnerSet(iws: Int, ws: List[DSE], tabstart: Int) = {
          <.table(
            Header(props),
            <.tbody(
              ws.zipWithIndex.map { entry =>
                val (dse, i) = entry
                val t = tabstart + i * 3
                TeamRow.withKey(dse.team.id.id)(
                  (
                    iws,
                    dse.team.id,
                    dse.team.player1,
                    dse.team.player2,
                    dse.result,
                    dse.isValid,
                    this,
                    props,
                    state,
                    t
                  )
                )
              }.toTagMod,
              TotalRow((ws, this, props, state))
            )
          )
        }

        <.div(
          DuplicatePageBridgeAppBar(
            id = None,
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  "Edit Duplicate Result"
                )
              )
            ),
            helpurl = "../help/duplicate/summary.html",
            routeCtl = props.routerCtl
          )(
          ),
          DuplicateResultStore.getDuplicateResult() match {
            case Some(dre) if dre.id == props.page.dupid =>
              val finished = !state.notfinished
              val comment = state.comment.getOrElse("")
              TagMod(
                <.div(
                  dupStyles.divDuplicateResultEditPage,
                  if (state.original.isEmpty) {
                    <.h1("Working")
                  } else {
                    TagMod(
                      <.div(
                        <.p("Played: "),
                        DateTimePicker(
                          "played",
                          defaultValue =
                            if (state.played == 0) Some(new Date())
                            else Some(new Date(state.played)),
                          defaultCurrentDate = Some(new Date()),
                          onChange = Some(setPlayed),
                          disabled = false
                        ),
                        CheckBox(
                          "Complete",
                          "Match complete",
                          finished,
                          toggleComplete
                        ),
                        CheckBox("IMP", "Use IMP", state.useIMP, toggleIMP),
                        <.br,
                        <.label(
                          "Comment: ",
                          <.input(
                            ^.`type` := "text",
                            ^.name := "Comment",
                            ^.value := comment,
                            ^.onChange ==> setComment
                          )
                        )
                      ),
                      state.teams.zipWithIndex.map { e =>
                        val (ws, i) = e
                        val t = i * ws.length * 3
                        getWinnerSet(i, ws, t)
                      }.toTagMod,
                      !state.isValid() ?= <.p("Data not valid"),
                      <.p(
                        "Created: ",
                        DateUtils.formatDate(dre.created),
                        ", updated ",
                        DateUtils.formatDate(dre.updated)
                      )
                    )
                  },
                  <.div(baseStyles.divFlexBreak),
                  <.div(
                    baseStyles.divFooter,
                    <.div(
                      baseStyles.divFooterLeft,
                      AppButton(
                        "OK",
                        "OK",
                        ^.disabled := !state.isValid(),
                        ^.onClick --> ok
                      )
                    ),
                    <.div(
                      baseStyles.divFooterCenter,
                      AppButton("Cancel", "Cancel", ^.onClick --> cancel)
                    )
                  )
                )
              )
            case _ =>
              HomePage.loading
          }
        )
      }

      var mounted = false
      val storeCallback: Callback = scope.modState { s =>
        val mdr = DuplicateResultStore.getDuplicateResult()
        s.updateOriginal(mdr)
      }

      val didMount: Callback = scope.props >>= { (p) =>
        Callback {
          mounted = true
          logger.info("PageDuplicateResultEdit.didMount")
          DuplicateResultStore.addChangeListener(storeCallback)
          Controller.monitorDuplicateResult(p.page.dupid)
        }
      }

      val willUnmount: japgolly.scalajs.react.Callback = Callback {
        mounted = false
        logger.info("PageDuplicateResultEdit.willUnmount")
        DuplicateResultStore.removeChangeListener(storeCallback)
        Controller.stopMonitoringDuplicateResult()
      }

    }

    private val Header = ScalaComponent
      .builder[Props]("PageDuplicateResultEdit.Header")
      .render_P(props => {
        <.thead(
          <.tr(
            <.th("Team"),
            <.th("Player1"),
            <.th("Player2"),
            <.th("Points")
          )
        )
      })
      .build

    private def noNull(s: String) = Option(s).getOrElse("")

    private val TeamRow = ScalaComponent
      .builder[
        (
            Int,
            Team.Id,
            String,
            String,
            String,
            Boolean,
            Backend,
            Props,
            State,
            Int
        )
      ]("PageDuplicateResultEdit.TeamRow")
      .render_P(args => {
        val (
          iws,
          id,
          player1,
          player2,
          points,
          valid,
          backend,
          props,
          state,
          tabstart
        ) = args

        <.tr(
          <.td(id.toNumber),
          <.td(
            <.div(
              EnterName(
                id = s"P${iws}T${id.id}P1",
                name = player1,
                tabIndex = -1,
                onChange = backend.setPlayer(iws, id, 1) _
              )
            )
          ),
          <.td(
            <.div(
              EnterName(
                id = s"P${iws}T${id.id}P2",
                name = player2,
                tabIndex = -1,
                onChange = backend.setPlayer(iws, id, 2) _
              )
            )
          ),
          <.td(
            <.input(
              ^.`type` := "number",
              ^.name := s"P${iws}T${id.id}PP",
              ^.onChange ==> backend.setPoints(iws, id),
              ^.value := points.toString()
            )
          ),
          BaseStyles.highlight(required = !valid)
        )
      })
      .build

    private val TotalRow = ScalaComponent
      .builder[(List[DSE], Backend, Props, State)](
        "PageDuplicateResultEdit.TotalRow"
      )
      .render_P(args => {
        val (ws, backend, props, state) = args
        val total = ws
          .map { dse =>
            try {
              dse.result.toDouble
            } catch {
              case _: Exception => 0
            }
          }
          .foldLeft(0.0)((ac, v) => ac + v)

        <.tr(
          <.td(),
          <.td(),
          <.td("Total"),
          <.td(total)
        )

      })
      .build

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): japgolly.scalajs.react.Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps.page != props.page) {
          Controller.monitorDuplicateResult(props.page.dupid)
        }
      }

    val component = ScalaComponent
      .builder[Props]("PageDuplicateResultEdit")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
