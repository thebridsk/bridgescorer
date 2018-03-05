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
import com.example.controller.Controller
import com.example.data.SystemTime
import com.example.routes.BridgeRouter
import com.example.react.AppButton
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.example.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.example.rest2.RestClientDuplicateSummary
import com.example.bridge.store.DuplicateSummaryStore
import com.example.data.duplicate.suggestion.PairsData
import com.example.react.Utils._

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
 * PagePairs( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object PagePairs {
  import PagePairsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage] )

  def apply( routerCtl: BridgeRouter[DuplicatePage] ) = component(Props(routerCtl))

}

object PagePairsInternal {
  import PagePairs._
  import DuplicateStyles._

  val logger = Logger("bridge.PagePairs")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
                    filter: ViewPlayerFilter.Filter = ViewPlayerFilter.Filter(),
                    showPeopleTable: Boolean = false,
                    showPairs: Boolean = false,
                    showPeopleTableDetail: Boolean = false,
                    showPairsDetail: Boolean = false
                  )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def onChange( filter: ViewPlayerFilter.Filter ) = scope.modState( s => s.copy( filter = filter ) )

    def toggleShowPeopleTable() = scope.modState( s => s.copy( showPeopleTable = !s.showPeopleTable) )

    def toggleShowPairs() = scope.modState( s => s.copy( showPairs = !s.showPairs) )

    def toggleShowPeopleTableDetail() = scope.modState( s => s.copy( showPeopleTableDetail = !s.showPeopleTableDetail) )

    def toggleShowPairsDetail() = scope.modState( s => s.copy( showPairsDetail = !s.showPairsDetail) )

    def render( props: Props, state: State ) = {
      <.div(
        dupStyles.divPeopleSummary,
        <.h1("Pairs Data"),
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "Home", "Home", props.routerCtl.home )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "Summary", "Summary", props.routerCtl.setOnClick(SummaryView) )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "BoardSets", "BoardSets", props.routerCtl.setOnClick(BoardSetSummaryView) ),
            " ",
            AppButton( "Movements", "Movements", props.routerCtl.setOnClick(MovementSummaryView) )
          )
        ),
        ViewPlayerFilter(state.filter, onChange _),
        ViewPairsGrid( state.filter ),
        ViewPairsDetailsGrid( state.filter ),
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "ShowPeopleResults",
                       "Show People Results",
                       state.showPeopleTable ?= baseStyles.buttonSelected,
                       ^.onClick-->toggleShowPeopleTable()
                     ),
            AppButton( "ShowPairsResults",
                       "Show Pairs Results",
                       state.showPairs ?= baseStyles.buttonSelected,
                       ^.onClick-->toggleShowPairs()
                     ),
            AppButton( "ShowPeopleDetails",
                       "Show People Details",
                       state.showPeopleTableDetail ?= baseStyles.buttonSelected,
                       ^.onClick-->toggleShowPeopleTableDetail()
                     ),
            AppButton( "ShowPairsDetails",
                       "Show Pairs Details",
                       state.showPairsDetail ?= baseStyles.buttonSelected,
                       ^.onClick-->toggleShowPairsDetail()
                     )
          )
        ),
        state.showPeopleTable ?= ViewPeopleTable( state.filter ),
        state.showPairs ?= ViewPairs(state.filter ),
        state.showPeopleTableDetail ?= ViewPeopleTableDetails( state.filter ),
        state.showPairsDetail ?= ViewPairsDetails(state.filter ),
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "Home2", "Home", props.routerCtl.home )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "Summary2", "Summary", props.routerCtl.setOnClick(SummaryView) )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "BoardSets2", "BoardSets", props.routerCtl.setOnClick(BoardSetSummaryView) ),
            " ",
            AppButton( "Movements2", "Movements", props.routerCtl.setOnClick(MovementSummaryView) )
          )
        )

      )
    }

    private var mounted: Boolean = false

    val storeCallback = scope.modState { s =>
      val pd = DuplicateSummaryStore.getDuplicateSummary().map { lds => new PairsData(lds) }
      s.copy( filter = s.filter.copy( pairsData=pd ) )
    }

    def didMount() = Callback {
      logger.info("PageSummary.didMount")
      DuplicateSummaryStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      Controller.getSummary()
    )}

    def willUnmount() = Callback {
      logger.finer("PageSummary.willUnmount")
      DuplicateSummaryStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PagePairs")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount(scope => scope.backend.willUnmount())
                            .build
}

