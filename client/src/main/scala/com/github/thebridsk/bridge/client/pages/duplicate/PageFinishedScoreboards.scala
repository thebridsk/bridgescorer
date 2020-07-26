package com.github.thebridsk.bridge.client.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicateSummary
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.data.bridge.DuplicateViewPerspective
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
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

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageScoreboard( routerCtl: BridgeRouter[DuplicatePage], game: BaseScoreboardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageFinishedScoreboards {
  import PageFinishedScoreboardsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], game: FinishedScoreboardsView )

  def apply( routerCtl: BridgeRouter[DuplicatePage], game: FinishedScoreboardsView ) = component(Props(routerCtl,game))

}

object PageFinishedScoreboardsInternal {
  import PageFinishedScoreboards._

  val logger = Logger("bridge.PageFinishedScoreboards")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( games: Map[MatchDuplicate.Id,MatchDuplicateScore],
                    results: Map[MatchDuplicateResult.Id,MatchDuplicateResult],
                    ids: List[DuplicateSummary.Id],
                    summaries: List[DuplicateSummary.Id]
                  ) {
    def gotall = {
      games.size+results.size == ids.length
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      import DuplicateStyles._
      def scoreboards() = {
        var i = 0
        state.ids.map { id =>
          DuplicateSummary.useId(
            id,
            mdid =>
              state.games.get(mdid).whenDefined { s =>
                ViewScoreboard(props.routerCtl, FinishedScoreboardView(s.id.id), s )
              },
            mdrid =>
              state.results.get(mdrid).whenDefined { s =>
                val wss = s.getWinnerSets
                wss.zipWithIndex.map { arg =>
                  val (ws,iws) = arg
                  ViewPlayerMatchResult( s.placeByWinnerSet(ws), s, iws+1, wss.length )
                }.toTagMod
              },
            TagMod.empty
          )
        }.toTagMod
      }

      def matches(add: Boolean) = {
        state.summaries.filter(id => state.ids.contains(id)^add).sortWith((l,r)=>l < r).map(id => {
          AppButton( s"Duplicate_${id.id}", id.id, baseStyles.hideInPrint, ^.onClick --> (if (add) addMatch(id) else removeMatch(id))
          )
        }).toTagMod
      }

      <.div(
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Finished Scoreboard",
                    )
                )),
          helpurl = "../help/duplicate/scoreboardcomplete.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          dupStyles.divFinishedScoreboardsPage,
          scoreboards(),
          <.div(
            baseStyles.hideInPrint,
            AppButton( "Summary", "Summary", props.routerCtl.setOnClick(SummaryView) ),
            <.p( "Add: ", matches(true)),
            <.p( "Remove: ", matches(false))
  //          <.p( "Add and remove the matches that should be printed. ",
  //               "Due to a layout problem, do NOT print more than one page. ",
  //               "For 2 table matches, 2 matches can be printed in landscape, 3 in portrait." )
          )
        )
      )
    }

    def removeMatch (id: DuplicateSummary.Id ) = scope.modState(s => s.copy(ids = s.ids.filter(s=>s!=id)) )

    def addMatch( id: DuplicateSummary.Id ) =
      scope.modState(s => s.copy(ids = s.ids:::(id::Nil)),
        Callback {
          val state = scope.withEffectsImpure.state
          logger.finest("PageFinishedScoreboards.addMatch: getting "+id)
          DuplicateSummary.useId(
            id,
            mdid =>
              state.games.get(mdid).map { _ =>  }.getOrElse {
                Controller.getMatchDuplicate(mdid).recordFailure().foreach( md => gotMatch(md) )
              },
            mdrid =>
              state.results.get(mdrid).map { _ =>  }.getOrElse {
                Controller.getDuplicateResult(mdrid).recordFailure().foreach( md => gotMatchResult(md) )
              },
            {}
          )
        }
      )

    def gotMatch( md: MatchDuplicate ) = {
      logger.info("PageFinishedScoreboards.gotMatch: got "+md.id)
      if (mounted) {
        logger.finest("PageFinishedScoreboards.gotMatch: Is mounted ")
        scope.withEffectsImpure.modState { s =>
            logger.info("PageFinishedScoreboards.gotMatch: updating state")
            val newgames = s.games + ( md.id -> MatchDuplicateScore(md, PerspectiveComplete))
            s.copy( games = newgames )
          }
      }
    }

    def gotMatchResult( md: MatchDuplicateResult ) = {
      logger.info("PageFinishedScoreboards.gotMatchResult: got "+md.id)
      if (mounted) {
        logger.finest("PageFinishedScoreboards.gotMatch: Is mounted ")
        scope.withEffectsImpure.modState { s =>
            logger.finest("PageFinishedScoreboards.gotMatch: updating state")
            val newgames = s.results + ( md.id -> md)
            s.copy( results = newgames )
          }
      }
    }

    var mounted = false

    val didMount = Callback {
      mounted = true
      logger.finest("PageFinishedScoreboards.didMount")
    } >> scope.state >>= { s => Callback {
      s.ids.foreach { id =>
        DuplicateSummary.useId(
          id,
          mdid =>
            s.games.get(mdid).map { _ =>  }.getOrElse {
              Controller.getMatchDuplicate(mdid).recordFailure().foreach( md => gotMatch(md) )
            },
          mdrid =>
            s.results.get(mdrid).map { _ =>  }.getOrElse {
              Controller.getDuplicateResult(mdrid).recordFailure().foreach( md => gotMatchResult(md) )
            },
          {}
        )
      }
      RestClientDuplicateSummary.list().recordFailure().foreach(list=>
        scope.withEffectsImpure.modState( s => s.copy(summaries=list.map(ds => ds.id).toList))
      )
    }}

    val willUnmount = Callback {
      mounted = false
      logger.info("PageFinishedScoreboards.willUnmount")
    }
  }

  val component = ScalaComponent.builder[Props]("PageFinishedScoreboards")
                            .initialStateFromProps { props => State(Map(), Map(), props.game.getIds.toList, List() ) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

