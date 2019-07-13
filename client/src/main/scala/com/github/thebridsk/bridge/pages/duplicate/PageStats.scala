package com.github.thebridsk.bridge.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.controller.Controller
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.routes.BridgeRouter
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.github.thebridsk.bridge.rest2.RestClientDuplicateSummary
import com.github.thebridsk.bridge.bridge.store.DuplicateSummaryStore
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsData
import com.github.thebridsk.bridge.react.Utils._
import com.github.thebridsk.bridge.pages.BaseStyles
import com.github.thebridsk.bridge.data.duplicate.stats.DuplicateStats
import com.github.thebridsk.bridge.react.PopupOkCancel
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

/**
 * Shows a summary page of all duplicate matches from the database.
 * Each match has a button that that shows that match, by going to the ScoreboardView(id) page.
 * There is also a button to create a new match, by going to the NewScoreboardView page.
 *
 * The data is obtained from the DuplicateStore object.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageStats( routerCtl: BridgeRouter[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object PageStats {
  import PageStatsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage] )

  def apply( routerCtl: BridgeRouter[DuplicatePage] ) = component(Props(routerCtl))

}

object PageStatsInternal {
  import PageStats._
  import DuplicateStyles._

  val logger = Logger("bridge.PageStats")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
                    filter: ViewPlayerFilter.Filter = ViewPlayerFilter.Filter(),
                    stats: Option[DuplicateStats] = None,
                    gotStats: Boolean = false,

                    showPeopleTable: Boolean = false,
                    showPairs: Boolean = false,
                    showPeopleTableDetail: Boolean = false,
                    showPairsDetail: Boolean = false,

                    showPairsGrid: Boolean = true,
                    showMadeDownGrid: Boolean = false,
                    showPlayerContractResults: Boolean = false,
                    showPlayerDoubledContractResults: Boolean = false,
                    showContractResults: Boolean = false,
                    showPlayerAggressiveness: Boolean = false,

                    showPlayerOpponentsStatsTable: Boolean = false,
                    showPlayerOpponentsPairsStatsTable: Boolean = false,
                    showPlayerOpponentsStatsGraph: Boolean = false,

                    msg: Option[TagMod] = None
                  )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    import scala.concurrent.ExecutionContext.Implicits.global

    def onChange( filter: ViewPlayerFilter.Filter ) = scope.modState( s => s.copy( filter = filter ) )

    val toggleShowPeopleTable = scope.modState( s => s.copy( showPeopleTable = !s.showPeopleTable) )

    val toggleShowPairs = scope.modState( s => s.copy( showPairs = !s.showPairs) )

    val toggleShowPeopleTableDetail = scope.modState( s => s.copy( showPeopleTableDetail = !s.showPeopleTableDetail) )

    val toggleShowPairsDetail = scope.modState( s => s.copy( showPairsDetail = !s.showPairsDetail) )

    val toggleShowPairsGrid = scope.modState( s => s.copy( showPairsGrid = !s.showPairsGrid) )

    val toggleShowMadeDownGrid = scope.modState( s => s.copy( showMadeDownGrid = !s.showMadeDownGrid) )

    val togglePlayerOpponentsStatsTable = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerOpponentsStatsTable = !s.showPlayerOpponentsStatsTable),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true),
      )
    }

    val togglePlayerOpponentsPairsStatsTable = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerOpponentsPairsStatsTable = !s.showPlayerOpponentsPairsStatsTable),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true),
      )
    }

    val togglePlayerOpponentsStatsGraph = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerOpponentsStatsGraph = !s.showPlayerOpponentsStatsGraph),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true),
      )
    }

    val toggleShowPlayerContractResults = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerContractResults = !s.showPlayerContractResults),
          playerStats = s.stats.map( cs => cs.playerStats.isEmpty ).getOrElse(true),
          contractStats = s.stats.map( cs => cs.contractStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerDoubledContractResults = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerDoubledContractResults = !s.showPlayerDoubledContractResults),
          playerDoubledStats = s.stats.map( cs => cs.playerDoubledStats.isEmpty ).getOrElse(true),
          contractStats = s.stats.map( cs => cs.contractStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowContractResults = scope.modState { s =>
      getDuplicateStats(
          s.copy( showContractResults = !s.showContractResults),
          contractStats = s.stats.map( cs => cs.contractStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerOpponentsStatsTable = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerOpponentsStatsTable = !s.showPlayerOpponentsStatsTable),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerOpponentsStatsGraph = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerOpponentsStatsGraph = !s.showPlayerOpponentsStatsGraph),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerAggressiveness = scope.modState { s =>
      getDuplicateStats(
          s.copy( showPlayerAggressiveness = !s.showPlayerAggressiveness),
          comparisonStats = s.stats.map( cs => cs.comparisonStats.isEmpty ).getOrElse(true),
      )
    }

    def getDuplicateStats(
        s: State,
        playerStats: Boolean = false,
        contractStats: Boolean = false,
        playerDoubledStats: Boolean = false,
        comparisonStats: Boolean = false,
        playersOpponentsStats: Boolean = false
    ) = {
      if ( s.stats.isEmpty ||
           (playerStats && s.stats.get.playerStats.isEmpty) ||
           (contractStats && s.stats.get.contractStats.isEmpty) ||
           (playerDoubledStats && s.stats.get.playerDoubledStats.isEmpty) ||
           (comparisonStats && s.stats.get.comparisonStats.isEmpty) ||
           (playersOpponentsStats && s.stats.get.playersOpponentsStats.isEmpty)
      ) {
        QueryDuplicateStats.getDuplicateStats(playerStats,contractStats,playerDoubledStats,comparisonStats,playersOpponentsStats).map { result =>
          scope.withEffectsImpure.modState { s =>
            result match {
              case Right(stats) =>
                s.copy(stats = s.stats.map( s => s.update(stats.duplicatestats) ).orElse(Option(stats.duplicatestats)))
              case Left(error) =>
                s.copy(msg = Some(TagMod("Error getting stats")))
            }
          }
        }
        s.copy( gotStats=true )
      } else {
        s
      }
    }

    val cancel = scope.modState( s => s.copy(msg = None) )

    val working = <.div( "Working on getting data" )

    def optionalView( show: Boolean, view: => TagMod, data: Option[_]* ) = {
      if (show) {
        if (data.find( d => d.isEmpty ).isEmpty) {
          view
        } else {
          working
        }
      } else {
        TagMod()
      }
    }

    def render( props: Props, state: State ) = {
      <.div(
        dupStyles.divPageStats,
        PopupOkCancel( state.msg, None, Some(cancel) ),
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Statistics",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "ShowPairsGrid",
                         "Show Pairs",
                         BaseStyles.highlight(selected = state.showPairsGrid ),
                         ^.onClick-->toggleShowPairsGrid
                       ),
              AppButton( "ShowMadeDownGrid",
                         "Show Made/Down",
                         BaseStyles.highlight(selected = state.showMadeDownGrid ),
                         ^.onClick-->toggleShowMadeDownGrid
                       ),
              AppButton( "ShowPlayerContractResults",
                         "Show Player Contracts",
                         BaseStyles.highlight(selected = state.showPlayerContractResults ),
                         ^.onClick-->toggleShowPlayerContractResults
                       ),
              AppButton( "ShowPlayerDoubledContractResults",
                         "Show Player Doubled Contracts",
                         BaseStyles.highlight(selected = state.showPlayerDoubledContractResults ),
                         ^.onClick-->toggleShowPlayerDoubledContractResults
                       ),
              AppButton( "ShowContractResults",
                         "Show Contracts",
                         BaseStyles.highlight(selected = state.showContractResults ),
                         ^.onClick-->toggleShowContractResults
                       ),
              AppButton( "AggressiveStats",
                         "Aggressive Stats",
                         BaseStyles.highlight(selected = state.showPlayerAggressiveness ),
                         ^.onClick-->toggleShowPlayerAggressiveness
                       )
            )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "ShowPlayerOpponentResults",
                         "Show Opponent Results",
                         BaseStyles.highlight(selected = state.showPlayerOpponentsStatsTable ),
                         ^.onClick-->togglePlayerOpponentsStatsTable
                       ),
              AppButton( "ShowPlayerOpponentPairsResults",
                         "Show Opponent Pairs Results",
                         BaseStyles.highlight(selected = state.showPlayerOpponentsPairsStatsTable ),
                         ^.onClick-->togglePlayerOpponentsPairsStatsTable
                       ),
              AppButton( "ShowPlayerOpponentGrid",
                         "Show Opponent Graph",
                         BaseStyles.highlight(selected = state.showPlayerOpponentsStatsGraph ),
                         ^.onClick-->togglePlayerOpponentsStatsGraph
                       ),
            )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "ShowPeopleResults",
                         "Show Player Results",
                         BaseStyles.highlight(selected = state.showPeopleTable ),
                         ^.onClick-->toggleShowPeopleTable
                       ),
              AppButton( "ShowPairsResults",
                         "Show Pairs Results",
                         BaseStyles.highlight(selected = state.showPairs ),
                         ^.onClick-->toggleShowPairs
                       ),
              AppButton( "ShowPeopleDetails",
                         "Show Player Hand Results",
                         BaseStyles.highlight(selected = state.showPeopleTableDetail ),
                         ^.onClick-->toggleShowPeopleTableDetail
                       ),
              AppButton( "ShowPairsDetails",
                         "Show Pairs Hand Results",
                         BaseStyles.highlight(selected = state.showPairsDetail ),
                         ^.onClick-->toggleShowPairsDetail
                       )
            )
          ),
          if (state.filter.pairsData.isDefined) {
            TagMod(
              ViewPlayerFilter(state.filter, onChange _),
              state.showPairsGrid ?= ViewPairsGrid( state.filter ),
              state.showMadeDownGrid ?= ViewPairsMadeDownGrid( state.filter )
            )
          } else {
            <.div(
              "Working"
            )
          },
          if (state.showPlayerOpponentsStatsTable ||
              state.showPlayerOpponentsPairsStatsTable ||
              state.showPlayerOpponentsStatsGraph
          ) {
            state.stats.map { cs =>
              TagMod(
                optionalView(
                    state.showPlayerOpponentsStatsTable,
                    ViewPlayerOpponentStatsTable( cs.playersOpponentsStats, false, true ),
                    cs.playersOpponentsStats),
                optionalView(
                    state.showPlayerOpponentsPairsStatsTable,
                    ViewPlayerOpponentStatsTable( cs.playersOpponentsStats, true, true ),
                    cs.playersOpponentsStats),
                optionalView(
                    state.showPlayerOpponentsStatsGraph,
                    ViewPlayerOpponentStatsGraph( cs.playersOpponentsStats, false ),
                    cs.playersOpponentsStats),
              )
            }.getOrElse(
              <.div(
                "Working on contract stats"
              )
            )
          } else {
            EmptyVdom
          },
          if (state.showContractResults ||
              state.showPlayerContractResults ||
              state.showPlayerDoubledContractResults ||
              state.showPlayerAggressiveness
          ) {
            state.stats.map { cs =>
              TagMod(
                optionalView(
                    state.showPlayerContractResults,
                    ViewPlayerContractResults( cs.playerStats.get, cs.contractStats.get ),
                    cs.playerStats,
                    cs.contractStats),
                optionalView(
                    state.showPlayerDoubledContractResults,
                    ViewPlayerDoubledContractResults( cs.playerDoubledStats.get, cs.contractStats.get ),
                    cs.playerDoubledStats,
                    cs.contractStats ),
                optionalView(
                    state.showContractResults,
                    ViewContractResults( cs.contractStats.get ),
                    cs.contractStats ),
                optionalView(
                    state.showPlayerAggressiveness,
                    ViewPlayerAggressiveness( cs.comparisonStats.get ),
                    cs.comparisonStats )
              )
            }.getOrElse(
              <.div(
                "Working on contract stats"
              )
            )
          } else {
            EmptyVdom
          },
          state.showPeopleTable ?= ViewPairsTable( state.filter, false ),
          state.showPairs ?= ViewPairsTable(state.filter, true ),
          state.showPeopleTableDetail ?= ViewPairsMadeDownTable( state.filter, false ),
          state.showPairsDetail ?= ViewPairsMadeDownTable(state.filter, true ),
        )
      )
    }

    private var mounted: Boolean = false

    val storeCallback = scope.modState { s =>
      val pd = DuplicateSummaryStore.getDuplicateSummary().map { lds => new PairsData(lds) }
      s.copy( filter = s.filter.copy( pairsData=pd ) )
    }

    val didMount = Callback {
      logger.info("PageSummary.didMount")
      DuplicateSummaryStore.addChangeListener(storeCallback)
      Controller.getSummary(
          () => scope.withEffectsImpure.modState( s => s.copy(msg=Some("Error getting duplicate summary")))
      )
    }

    val willUnmount = Callback {
      logger.finer("PageSummary.willUnmount")
      DuplicateSummaryStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageStats")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount(scope => scope.backend.willUnmount)
                            .build
}
