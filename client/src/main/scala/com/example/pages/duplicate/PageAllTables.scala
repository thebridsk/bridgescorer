package com.example.pages.duplicate


import scala.scalajs.js
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
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import com.example.routes.BridgeRouter

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageAllTables( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageAllTables {
  import PageAllTablesInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: AllTableView )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: AllTableView ) = component(Props(routerCtl,page))

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
      <.div(
          DuplicatePageBridgeAppBar(
            id = Some(props.page.dupid),
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
                  MuiTypography(
                      variant = TextVariant.h6,
                      color = TextColor.inherit,
                  )(
                      <.span(
                        "All Tables",
                      )
                  )),
            helpurl = "../help/duplicate/table.html",
            routeCtl = props.routerCtl
          )(

          ),
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
                    baseStyles.divFooterCenter,
                    AppButton( "Game", "Completed Games Scoreboard", props.routerCtl.setOnClick(clickPage)
                    ),
                    ComponentInputStyleButton( Callback{} )
                  )
                )
              )
            case None =>
              <.div(
                  <.h1("Waiting to load information")
              )
          }
      )
    }

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("PageAllTables.didMount")
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitorMatchDuplicate(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageAllTables.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageAllTables")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

