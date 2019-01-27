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
import com.example.pages.duplicate.DuplicateRouter.TableView
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.react.AppButton
import com.example.react.HelpButton
import com.example.routes.BridgeRouter
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor

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
                  helpurl = "/help/duplicate/table.html",
                  routeCtl = props.routerCtl
                )(

                ),
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
                <.div(
                  baseStyles.divFooter,
                  <.div(
                    baseStyles.divFooterLeft,
                    AppButton( "Game", "Completed Games Scoreboard",
                               props.routerCtl.setOnClick(CompleteScoreboardView(props.page.dupid))
                    )
                  ),
                  <.div(
                    baseStyles.divFooterCenter,
                    ComponentInputStyleButton( Callback{} )
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

      Controller.monitorMatchDuplicate(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageTable.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
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

