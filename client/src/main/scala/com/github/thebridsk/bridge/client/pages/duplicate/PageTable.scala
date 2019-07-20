package com.github.thebridsk.bridge.client.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.data.bridge.DuplicateViewPerspective
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore.Round
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageTable( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageTable {
  import PageTableInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: TableView )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: TableView ) = component(Props(routerCtl,page))

}

object PageTableInternal {
  import PageTable._

  val logger = Logger("bridge.PageTable")

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
          score.tables.get(props.page.tableid) match {
            case Some(rounds) =>
              <.div(
                dupStyles.divTablePage,
                DuplicatePageBridgeAppBar(
                  id = Some(props.page.dupid),
                  tableIds = List(),
                  title = Seq[CtorType.ChildArg](
                        MuiTypography(
                            variant = TextVariant.h6,
                            color = TextColor.inherit,
                        )(
                            <.span(
                              s"Table ${props.page.tableid}",
                            )
                        )),
                  helpurl = "../help/duplicate/table.html",
                  routeCtl = props.routerCtl
                )(

                ),
                <.div(
                  ViewTable(props.routerCtl,props.page),
                  <.div(
                    dupStyles.divTableHelp,
                    <.b(
                      <.p("When starting a new round, wait until you are ready to play the first hand."),
                      <.p("To enter the results of the next hand, either:"),
                      <.ul(
                        <.li("Hit the button for a board to go directly to the scoring for the board"),
                        <.li("Hit the button for the current round to go to the scoreboard for the table")
                      )
                    )
                  ),
                  <.div( baseStyles.divFlexBreak ),
                  <.div(
                    baseStyles.divFooter,
                    <.div(
                      baseStyles.divFooterCenter,
                      AppButton( "Game", "Completed Games Scoreboard",
                                 props.routerCtl.setOnClick(CompleteScoreboardView(props.page.dupid))
                      ),
                      ComponentInputStyleButton( Callback{} )
                    )
                  )
                )
              )
            case None =>
              <.div( <.p("Table not found"), <.p(
                  AppButton( "Game", "Completed Games Scoreboard", props.routerCtl.setOnClick(CompleteScoreboardView(props.page.dupid)) )
                  ) )
          }
        case None =>
          <.div( <.p("Waiting") )
      }
    }

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("PageTable.didMount")
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitor(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageTable.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
    }
  }

  val component = ScalaComponent.builder[Props]("PageTable")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

