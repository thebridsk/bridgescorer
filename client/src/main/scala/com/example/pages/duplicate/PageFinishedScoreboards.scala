package com.example.pages.duplicate


import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.rest2.RestClientDuplicateSummary
import com.example.controller.Controller
import com.example.data.bridge.DuplicateViewPerspective
import com.example.bridge.store.DuplicateStore
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.PerspectiveTable
import com.example.data.bridge.PerspectiveComplete
import com.example.react.DateUtils
import com.example.data.MatchDuplicate
import com.example.pages.duplicate.DuplicateRouter.FinishedScoreboardsView
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.FinishedScoreboardView
import com.example.react.AppButton
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.data.MatchDuplicateResult

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageScoreboard( routerCtl: RouterCtl[DuplicatePage], game: BaseScoreboardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageFinishedScoreboards {
  import PageFinishedScoreboardsInternal._

  case class Props( routerCtl: RouterCtl[DuplicatePage], game: FinishedScoreboardsView )

  def apply( routerCtl: RouterCtl[DuplicatePage], game: FinishedScoreboardsView ) = component(Props(routerCtl,game))

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
  case class State( games: Map[String,MatchDuplicateScore],
                    results: Map[String,MatchDuplicateResult],
                    ids: List[String],
                    summaries: List[Id.MatchDuplicate]
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
          val tm: TagMod =
            state.games.get(id) match {
              case Some(s) =>
                ViewScoreboard(props.routerCtl, FinishedScoreboardView(s.id), s )
              case None =>
                state.results.get(id) match {
                  case Some(s) =>
                    val wss = s.getWinnerSets
                    wss.zipWithIndex.map { arg =>
                      val (ws,iws) = arg
                      ViewPlayerMatchResult( s.placeByWinnerSet(ws), s, iws+1, wss.length )
                    }.toTagMod
                  case None =>
                    EmptyVdom
                }
            }
          tm
        }.toTagMod
      }

      def matches(add: Boolean) = {
        state.summaries.filter(id => state.ids.contains(id)^add).sortWith((l,r)=>Id.idComparer(l, r)<0).map(id => {
          AppButton( "Duplicate_"+id, id, baseStyles.hideInPrint, ^.onClick --> (if (add) addMatch(id) else removeMatch(id))
          )
        }).toTagMod
      }

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
    }

    def removeMatch (id: Id.MatchDuplicate ) = scope.modState(s => s.copy(ids = s.ids.filter(s=>s!=id)) )

    def addMatch( id: Id.MatchDuplicate ) =
      scope.modState(s => s.copy(ids = (id::s.ids.reverse).reverse),
        Callback {
          logger.finest("PageFinishedScoreboards.addMatch: getting "+id)
          if ( id.toString.charAt(0) == 'E' ) {
            if (scope.withEffectsImpure.state.results.get(id).isEmpty)
              Controller.getDuplicateResult(id).recordFailure().foreach( md => gotMatchResult(md) )
          } else {
            if (scope.withEffectsImpure.state.games.get(id).isEmpty)
              Controller.getMatchDuplicate(id).recordFailure().foreach( md => gotMatch(md) )
          }
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

    def didMount() = Callback {
      mounted = true
      logger.finest("PageFinishedScoreboards.didMount")
    } >> Callback {
      val s = scope.withEffectsImpure.state
      s.ids.foreach { id => {
        if (id.toString().charAt(0) == 'E') {
          logger.finest("PageFinishedScoreboards.didMount: getting MatchDuplicateResult "+id)
          Controller.getDuplicateResult(id).recordFailure().foreach(md => gotMatchResult(md))
        } else {
          logger.finest("PageFinishedScoreboards.didMount: getting MatchDuplicate "+id)
          Controller.getMatchDuplicate(id).recordFailure().foreach(md => gotMatch(md))
        }

      }}
      RestClientDuplicateSummary.list().recordFailure().foreach(list=>
        scope.withEffectsImpure.modState( s => s.copy(summaries=list.map(ds => ds.id).toList))
      )
    }

    def willUnmount() = Callback {
      mounted = false
      logger.info("PageFinishedScoreboards.willUnmount")
    }
  }

  val component = ScalaComponent.builder[Props]("PageFinishedScoreboards")
                            .initialStateFromProps { props => State(Map(), Map(), props.game.getIds().toList, List() ) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

