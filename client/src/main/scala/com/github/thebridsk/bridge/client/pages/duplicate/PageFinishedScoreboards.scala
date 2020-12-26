package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicateSummary
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.FinishedScoreboardsView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.FinishedScoreboardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.data.DuplicateSummary
import japgolly.scalajs.react.internal.Effect

/**
  * A component page that shows final scoreboard view.
  *
  * The FinishedScoreboardView object will identify which MatchDuplicate to look at.
  *
  * To use, just code the following:
  *
  * {{{
  * PageScoreboard(
  *   routerCtl = ...,
  *   game = FinishedScoreboardsView(id)
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageFinishedScoreboards {
  import Internal._

  case class Props(
      routerCtl: BridgeRouter[DuplicatePage],
      game: FinishedScoreboardsView
  )

  /**
    * Instantiate the component
    *
    * @param routerCtl the react router
    * @param game a [[FinishedScoreboardsView]] object that identifies the match to display.
    * @return the unmounted react component
    *
    * @see See [[PageFinishedScoreboards]] for usage.
    */
  def apply(
      routerCtl: BridgeRouter[DuplicatePage],
      game: FinishedScoreboardsView
  ) =
    component(
      Props(routerCtl, game)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.PageFinishedScoreboards")

    case class State(
        games: Map[MatchDuplicate.Id, MatchDuplicateScore],
        results: Map[MatchDuplicateResult.Id, MatchDuplicateResult],
        ids: List[DuplicateSummary.Id],
        summaries: List[DuplicateSummary.Id]
    ) {
      def gotall: Boolean = {
        games.size + results.size == ids.length
      }
    }

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        import DuplicateStyles._
        def scoreboards() = {
          var i = 0
          state.ids.map { id =>
            DuplicateSummary.useId(
              id,
              mdid =>
                state.games.get(mdid).whenDefined { s =>
                  ViewScoreboard(
                    props.routerCtl,
                    FinishedScoreboardView(s.id.id),
                    s
                  )
                },
              mdrid =>
                state.results.get(mdrid).whenDefined { s =>
                  val wss = s.getWinnerSets
                  wss.zipWithIndex.map { arg =>
                    val (ws, iws) = arg
                    ViewPlayerMatchResult(
                      s.placeByWinnerSet(ws),
                      s,
                      iws + 1,
                      wss.length
                    )
                  }.toTagMod
                },
              TagMod.empty
            )
          }.toTagMod
        }

        def matches(add: Boolean) = {
          state.summaries
            .filter(id => state.ids.contains(id) ^ add)
            .sortWith((l, r) => l < r)
            .map(id => {
              AppButton(
                s"Duplicate_${id.id}",
                id.id,
                baseStyles.hideInPrint,
                ^.onClick --> (if (add) addMatch(id)
                              else removeMatch(id))
              )
            })
            .toTagMod
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
                  "Finished Scoreboard"
                )
              )
            ),
            helpurl = "../help/duplicate/scoreboardcomplete.html",
            routeCtl = props.routerCtl
          )(
          ),
          <.div(
            dupStyles.divFinishedScoreboardsPage,
            scoreboards(),
            <.div(
              baseStyles.hideInPrint,
              AppButton(
                "Summary",
                "Summary",
                props.routerCtl.setOnClick(SummaryView)
              ),
              <.p("Add: ", matches(true)),
              <.p("Remove: ", matches(false))
              //          <.p( "Add and remove the matches that should be printed. ",
              //               "Due to a layout problem, do NOT print more than one page. ",
              //               "For 2 table matches, 2 matches can be printed in landscape, 3 in portrait." )
            )
          )
        )
      }

      def removeMatch(id: DuplicateSummary.Id): Callback =
        scope.modState(s => s.copy(ids = s.ids.filter(s => s != id)))

      def addMatch(id: DuplicateSummary.Id): Callback =
        scope.modState(
          s => s.copy(ids = s.ids ::: (id :: Nil)),
          Callback {
            val state = scope.withEffectsImpure.state
            logger.finest("PageFinishedScoreboards.addMatch: getting " + id)
            DuplicateSummary.useId(
              id,
              mdid =>
                state.games.get(mdid).map { _ => }.getOrElse {
                  Controller
                    .getMatchDuplicate(mdid)
                    .recordFailure()
                    .foreach(md => gotMatch(md))
                },
              mdrid =>
                state.results.get(mdrid).map { _ => }.getOrElse {
                  Controller
                    .getDuplicateResult(mdrid)
                    .recordFailure()
                    .foreach(md => gotMatchResult(md))
                },
              {}
            )
          }
        )

      def gotMatch(md: MatchDuplicate): Effect.Id[Unit] = {
        logger.info("PageFinishedScoreboards.gotMatch: got " + md.id)
        if (mounted) {
          logger.finest("PageFinishedScoreboards.gotMatch: Is mounted ")
          scope.withEffectsImpure.modState { s =>
            logger.info("PageFinishedScoreboards.gotMatch: updating state")
            val newgames =
              s.games + (md.id -> MatchDuplicateScore(md, PerspectiveComplete))
            s.copy(games = newgames)
          }
        }
      }

      def gotMatchResult(md: MatchDuplicateResult): Effect.Id[Unit] = {
        logger.info("PageFinishedScoreboards.gotMatchResult: got " + md.id)
        if (mounted) {
          logger.finest("PageFinishedScoreboards.gotMatch: Is mounted ")
          scope.withEffectsImpure.modState { s =>
            logger.finest("PageFinishedScoreboards.gotMatch: updating state")
            val newgames = s.results + (md.id -> md)
            s.copy(results = newgames)
          }
        }
      }

      var mounted = false

      val didMount: Callback = Callback {
        mounted = true
        logger.finest("PageFinishedScoreboards.didMount")
      } >> scope.state >>= { s =>
        Callback {
          s.ids.foreach { id =>
            DuplicateSummary.useId(
              id,
              mdid =>
                s.games.get(mdid).map { _ => }.getOrElse {
                  Controller
                    .getMatchDuplicate(mdid)
                    .recordFailure()
                    .foreach(md => gotMatch(md))
                },
              mdrid =>
                s.results.get(mdrid).map { _ => }.getOrElse {
                  Controller
                    .getDuplicateResult(mdrid)
                    .recordFailure()
                    .foreach(md => gotMatchResult(md))
                },
              {}
            )
          }
          RestClientDuplicateSummary
            .list()
            .recordFailure()
            .foreach(list =>
              scope.withEffectsImpure.modState(s =>
                s.copy(summaries = list.map(ds => ds.id).toList)
              )
            )
        }
      }

      val willUnmount: Callback = Callback {
        mounted = false
        logger.info("PageFinishedScoreboards.willUnmount")
      }
    }

    val component = ScalaComponent
      .builder[Props]("PageFinishedScoreboards")
      .initialStateFromProps { props =>
        State(Map(), Map(), props.game.getIds.toList, List())
      }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
