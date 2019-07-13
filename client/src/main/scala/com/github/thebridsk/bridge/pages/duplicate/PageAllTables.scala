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
import com.github.thebridsk.bridge.data.bridge.DuplicateViewPerspective
import com.github.thebridsk.bridge.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore.Round
import com.github.thebridsk.bridge.pages.hand.ComponentInputStyleButton
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.AllTableView
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.routes.BridgeRouter

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
                <.div( baseStyles.divFlexBreak ),
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

      Controller.monitor(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageAllTables.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
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

