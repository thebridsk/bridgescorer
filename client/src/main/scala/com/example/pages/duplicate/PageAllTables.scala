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
import com.example.data.bridge.DuplicateViewPerspective
import com.example.bridge.store.DuplicateStore
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.MatchDuplicateScore.Round
import com.example.pages.hand.ComponentInputStyleButton
import com.example.pages.duplicate.DuplicateRouter.AllTableView
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.react.AppButton

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageAllTables( routerCtl: RouterCtl[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageAllTables {
  import PageAllTablesInternal._

  case class Props( routerCtl: RouterCtl[DuplicatePage], page: AllTableView )

  def apply( routerCtl: RouterCtl[DuplicatePage], page: AllTableView ) = component(Props(routerCtl,page))

}

object PageAllTablesInternal {
  import PageAllTables._

  val logger = Logger("bridge.PageAllTables")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

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
      DuplicateStore.getCompleteView() match {
        case Some(score) =>
          val clickPage = CompleteScoreboardView(props.page.dupid)
          <.div(
            dupStyles.divAllTablesPage,
            score.tables.keys.toList.sortWith((t1,t2)=>t1<t2).map { table =>
              ViewTable( props.routerCtl, props.page.toTableView( table ), true )
            }.toTagMod,
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton( "Game", "Completed Games Scoreboard", props.routerCtl.setOnClick(clickPage)
                )
              ),
              <.div(
                baseStyles.divFooterCenter,
                ComponentInputStyleButton( Callback{} )
              )
            )
          )
        case None =>
          <.div( <.p("Waiting to load information") )
      }
    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    def didMount() = CallbackTo {
      logger.info("PageAllTables.didMount")
      DuplicateStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => CallbackTo(
      Controller.monitorMatchDuplicate(p.page.dupid)
    )}

    def willUnmount() = CallbackTo {
      logger.info("PageAllTables.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageAllTables")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

