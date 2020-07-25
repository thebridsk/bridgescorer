package com.github.thebridsk.bridge.client.pages.duplicate


import scala.util.Success
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.bridge.store.DuplicateSummaryStore
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsData
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.data.duplicate.stats.DuplicateStats
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicatePlayerPlaces
import com.github.thebridsk.bridge.data.duplicate.stats.ContractStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerStats
import com.github.thebridsk.bridge.client.pages.HomePage

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

  sealed trait ShowView

  object ShowPeopleTable extends ShowView
  object ShowPairs extends ShowView
  object ShowPeopleTableDetail extends ShowView
  object ShowPairsDetail extends ShowView

  object ShowPairsGrid extends ShowView
  object ShowMadeDownGrid extends ShowView
  object ShowPlayerContractResults extends ShowView
  object ShowPlayerDoubledContractResults extends ShowView
  object ShowContractResults extends ShowView
  object ShowPlayerAggressiveness extends ShowView

  object ShowPlayerOpponentsStatsTable extends ShowView
  object ShowPlayerOpponentsPairsStatsTable extends ShowView
  object ShowPlayerOpponentsStatsGraph extends ShowView

  object ShowPlayerPlacesGraph extends ShowView

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

                    showViews: List[ShowView] = ShowPairsGrid::Nil,

                    msg: Option[TagMod] = None
  ) {

    def toggle( view: ShowView ) = {
      val r = showViews.filter( sv => sv != view )
      val newsv = if (r.length == showViews.length) view::showViews
      else r
      copy(showViews = newsv)
    }

    def isVisible( view: ShowView ) = {
      showViews.find( sv => sv==view ).isDefined
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

    import scala.concurrent.ExecutionContext.Implicits.global

    def onChange( filter: ViewPlayerFilter.Filter ) = scope.modState( s => s.copy( filter = filter ) )

    val toggleShowPeopleTable = scope.modState( s => s.toggle( ShowPeopleTable ) )

    val toggleShowPairs = scope.modState( s => s.toggle( ShowPairs ) )

    val toggleShowPeopleTableDetail = scope.modState( s => s.toggle( ShowPeopleTableDetail ) )

    val toggleShowPairsDetail = scope.modState( s => s.toggle( ShowPairsDetail ) )

    val toggleShowPairsGrid = scope.modState( s => s.toggle( ShowPairsGrid ) )

    val toggleShowMadeDownGrid = scope.modState( s => s.toggle( ShowMadeDownGrid ) )

    val togglePlayerOpponentsStatsTable = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerOpponentsStatsTable ),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true),
      )
    }

    val togglePlayerOpponentsPairsStatsTable = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerOpponentsPairsStatsTable ),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true),
      )
    }

    val togglePlayerOpponentsStatsGraph = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerOpponentsStatsGraph ),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true),
      )
    }

    val toggleShowPlayerContractResults = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerContractResults ),
          playerStats = s.stats.map( cs => cs.playerStats.isEmpty ).getOrElse(true),
          contractStats = s.stats.map( cs => cs.contractStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerDoubledContractResults = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerDoubledContractResults ),
          playerDoubledStats = s.stats.map( cs => cs.playerDoubledStats.isEmpty ).getOrElse(true),
          contractStats = s.stats.map( cs => cs.contractStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowContractResults = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowContractResults ),
          contractStats = s.stats.map( cs => cs.contractStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerOpponentsStatsTable = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerOpponentsStatsTable ),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerOpponentsStatsGraph = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerOpponentsStatsGraph ),
          playersOpponentsStats = s.stats.map( cs => cs.playersOpponentsStats.isEmpty ).getOrElse(true)
      )
    }

    val toggleShowPlayerAggressiveness = scope.modState { s =>
      getDuplicateStats(
          s.toggle( ShowPlayerAggressiveness ),
          comparisonStats = s.stats.map( cs => cs.comparisonStats.isEmpty ).getOrElse(true),
      )
    }

    val toggleShowPlayerPlaces = scope.modState { s =>
      getPlayerPlaces(
          s.toggle( ShowPlayerPlacesGraph )
      )
    }

    def getPlayerPlaces(
      s: State
    ) = {
      logger.fine(s"""getPlayerPlaces, playerPlaces s=${s}""")
      if ( s.isVisible(ShowPlayerPlacesGraph)
           && (s.stats.isEmpty || s.stats.get.playerPlacesStats.isEmpty)
      ) {
        RestClientDuplicatePlayerPlaces.get("").recordFailure().onComplete { tpp =>
          scope.withEffectsImpure.modState { ss =>
            tpp match {
              case Success(ppStats) =>
                logger.fine(s"""Received player places stats, ${ppStats}""")
                val ds = DuplicateStats(None,None,None,None,None,Some(ppStats))
                ss.copy(stats = ss.stats.map( sss => sss.update(ds) ).orElse(Option(ds)))
              case _ =>
                ss.copy(msg = Some(TagMod("Error getting stats")))
            }
          }
        }
        s.copy( gotStats=true )
      } else {
        s
      }
    }

    def getDuplicateStats(
        s: State,
        playerStats: Boolean = false,
        contractStats: Boolean = false,
        playerDoubledStats: Boolean = false,
        comparisonStats: Boolean = false,
        playersOpponentsStats: Boolean = false,
    ) = {
      if ( s.stats.isEmpty ||
           (playerStats && s.stats.get.playerStats.isEmpty) ||
           (contractStats && s.stats.get.contractStats.isEmpty) ||
           (playerDoubledStats && s.stats.get.playerDoubledStats.isEmpty) ||
           (comparisonStats && s.stats.get.comparisonStats.isEmpty) ||
           (playersOpponentsStats && s.stats.get.playersOpponentsStats.isEmpty)
      ) {
        QueryDuplicateStats.getDuplicateStats(
            playerStats,contractStats,
            playerDoubledStats,
            comparisonStats,
            playersOpponentsStats
        ).map { result =>
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

    val working = HomePage.loading

    def optionalView( view: => TagMod, data: Option[_]* ): TagMod = {
      if (data.find( d => d.isEmpty ).isEmpty) {
        view
      } else {
        working
      }
    }

    def render( props: Props, state: State ) = {

      def optionalStatsView( view: DuplicateStats => TagMod ): TagMod = {
        state.stats.map { cs =>
          if (cs.playersOpponentsStats.isDefined) {
            view(cs)
          } else {
            working
          }
        }.getOrElse( working )
      }

      def optionalStats1View[S]( view: (S) => TagMod, stat: DuplicateStats => Option[S] ): TagMod = {
        state.stats.map { cs =>
          stat(cs).map { s =>
            view(s)
          }.getOrElse( working )
        }.getOrElse( working )
      }

      def optionalStats2View[S,T]( view: (S,T) => TagMod, statS: DuplicateStats => Option[S], statT: DuplicateStats => Option[T] ) = {
        state.stats.map { cs =>
          statS(cs).map { s =>
            statT(cs).map { t =>
              view(s,t)
            }.getOrElse( working )
          }.getOrElse( working )
        }.getOrElse( working )
      }

      <.div(
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
          dupStyles.divPageStats,
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "ShowPairsGrid",
                         "Show Pairs",
                         BaseStyles.highlight(selected = state.isVisible( ShowPairsGrid ) ),
                         ^.onClick-->toggleShowPairsGrid
              ),
              AppButton( "ShowPeopleResults",
                         "Show Player Results",
                         BaseStyles.highlight(selected = state.isVisible( ShowPeopleTable ) ),
                         ^.onClick-->toggleShowPeopleTable
              ),
              AppButton( "ShowPairsResults",
                         "Show Pairs Results",
                         BaseStyles.highlight(selected = state.isVisible( ShowPairs ) ),
                         ^.onClick-->toggleShowPairs
              ),
            )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,

              AppButton( "ShowMadeDownGrid",
                         "Show Made/Down",
                         BaseStyles.highlight(selected = state.isVisible( ShowMadeDownGrid ) ),
                         ^.onClick-->toggleShowMadeDownGrid
              ),
              AppButton( "ShowPeopleDetails",
                         "Show Player Hand Results",
                         BaseStyles.highlight(selected = state.isVisible( ShowPeopleTableDetail ) ),
                         ^.onClick-->toggleShowPeopleTableDetail
              ),
              AppButton( "ShowPairsDetails",
                         "Show Pairs Hand Results",
                         BaseStyles.highlight(selected = state.isVisible( ShowPairsDetail ) ),
                         ^.onClick-->toggleShowPairsDetail
              )
            )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "ShowPlayerOpponentGrid",
                         "Show Opponent Graph",
                         BaseStyles.highlight(selected = state.isVisible( ShowPlayerOpponentsStatsGraph ) ),
                         ^.onClick-->togglePlayerOpponentsStatsGraph
                       ),
              AppButton( "ShowPlayerOpponentResults",
                         "Show Opponent Results",
                         BaseStyles.highlight(selected = state.isVisible( ShowPlayerOpponentsStatsTable ) ),
                         ^.onClick-->togglePlayerOpponentsStatsTable
                       ),
              AppButton( "ShowPlayerOpponentPairsResults",
                         "Show Opponent Pairs Results",
                         BaseStyles.highlight(selected = state.isVisible( ShowPlayerOpponentsPairsStatsTable ) ),
                         ^.onClick-->togglePlayerOpponentsPairsStatsTable
                       ),
            )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "ShowContractResults",
                         "Show Contracts",
                         BaseStyles.highlight(selected = state.isVisible( ShowContractResults ) ),
                         ^.onClick-->toggleShowContractResults
              ),
              AppButton( "ShowPlayerContractResults",
                         "Show Player Contracts",
                         BaseStyles.highlight(selected = state.isVisible( ShowPlayerContractResults ) ),
                         ^.onClick-->toggleShowPlayerContractResults
              ),
              AppButton( "ShowPlayerDoubledContractResults",
                         "Show Player Doubled Contracts",
                         BaseStyles.highlight(selected = state.isVisible( ShowPlayerDoubledContractResults ) ),
                         ^.onClick-->toggleShowPlayerDoubledContractResults
              ),
            )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "ShowPlayerPlaces",
                         "Show Player Places",
                         BaseStyles.highlight(selected = state.isVisible( ShowPlayerPlacesGraph ) ),
                         ^.onClick-->toggleShowPlayerPlaces
                       ),
              AppButton( "AggressiveStats",
                         "Aggressive Stats",
                         BaseStyles.highlight(selected = state.isVisible( ShowPlayerAggressiveness ) ),
                         ^.onClick-->toggleShowPlayerAggressiveness
                       )
            )
          ),
          if (state.filter.pairsData.isDefined) {
            ViewPlayerFilter(state.filter, onChange _)
          } else {
            working
          },
          (state.showViews.map { view =>
            val v: TagMod = view match {
              case ShowPairsGrid => optionalView( ViewPairsGrid( state.filter ), state.filter.pairsData )
              case ShowMadeDownGrid => optionalView( ViewPairsMadeDownGrid( state.filter ), state.filter.pairsData )
              case ShowPlayerOpponentsStatsTable =>
                optionalStatsView(
                    cs => ViewPlayerOpponentStatsTable( cs.playersOpponentsStats, false, true )
                )
              case ShowPlayerOpponentsPairsStatsTable =>
                optionalStatsView(
                  cs => ViewPlayerOpponentStatsTable( cs.playersOpponentsStats, true, true ),
                )
              case ShowPlayerOpponentsStatsGraph =>
                optionalStatsView(
                  cs => ViewPlayerOpponentStatsGraph( cs.playersOpponentsStats, false ),
                )
              case ShowPlayerContractResults =>
                optionalStats2View(
                    (s: PlayerStats, t: ContractStats) => ViewPlayerContractResults( s,t ),
                    _.playerStats,
                    _.contractStats)
              case ShowPlayerDoubledContractResults =>
                optionalStats2View(
                    (s: PlayerStats, t: ContractStats) => ViewPlayerDoubledContractResults( s,t ),
                    _.playerDoubledStats,
                    _.contractStats )
              case ShowContractResults =>
                optionalStats1View(
                    (cs: ContractStats) => ViewContractResults( cs ),
                    _.contractStats )
              case ShowPlayerAggressiveness =>
                optionalStats1View(
                    (cs: PlayerComparisonStats) => ViewPlayerAggressiveness( cs ),
                    _.comparisonStats )
              case ShowPlayerPlacesGraph => ViewPlayerPlacesGraph( state.stats.flatMap( s => s.playerPlacesStats) )
              case ShowPeopleTable => ViewPairsTable( state.filter, false )
              case ShowPairs => ViewPairsTable(state.filter, true )
              case ShowPeopleTableDetail => ViewPairsMadeDownTable( state.filter, false )
              case ShowPairsDetail => ViewPairsMadeDownTable(state.filter, true )
            }
            v
          }).toTagMod
        )
      )
    }

    private var mounted: Boolean = false

    val storeCallback = scope.modState { s =>
      val pd = DuplicateSummaryStore.getDuplicateSummary.map { lds => new PairsData(lds) }
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

