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
import com.example.data.bridge.PerspectiveTable
import com.example.pages.duplicate.DuplicateRouter.BaseAllBoardsViewWithPerspective
import com.example.pages.duplicate.DuplicateRouter.TableRoundAllBoardView
import com.example.react.AppButton
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
 * PageAllBoards( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageAllBoards {
  import PageAllBoardsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: BaseAllBoardsViewWithPerspective )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: BaseAllBoardsViewWithPerspective ) = component(Props(routerCtl,page))

}

object PageAllBoardsInternal {
  import PageAllBoards._

  val logger = Logger("bridge.PageAllBoards")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( useIMP: Option[Boolean] = None ) {

    def isMP = useIMP.getOrElse(true)
    def isIMP = useIMP.getOrElse(false)

    def toggleIMP = {
      copy( useIMP = Some(!isIMP) )
    }

    def nextIMPs = {
      val n = useIMP match {
        case None => Some(false)
        case Some(false) => Some(true)
        case Some(true) => None
      }
      copy(useIMP=n)
    }
  }

  class Backend(scope: BackendScope[Props, State]) {

    val nextIMPs = scope.modState { s => s.nextIMPs }

    def render( props: Props, state: State ) = {
      import DuplicateStyles._
      logger.info("Rendering board "+props.page)

      def title() = {
        props.page.getPerspective() match {
          case PerspectiveTable(team1, team2) =>
            val (currentRound,currentTable) = props.page match {
              case TableRoundAllBoardView( dupid, tableid, roundid ) => (roundid,tableid)
              case _ => (-1,"")
            }
            <.span(
              s"Table ${Id.tableIdToTableNumber(currentTable)} Round ${currentRound}" ,
              s" Teams ${Id.teamIdToTeamNumber(team1)} and ${Id.teamIdToTeamNumber(team2)}",
              " Board View"
            )
          case PerspectiveDirector =>
            <.span("Director's Board View")
          case PerspectiveComplete =>
            <.span("Completed Board View")
        }

      }

      <.div(
        dupStyles.divAllBoardsPage,
        DuplicatePageBridgeAppBar(
          id = Some(props.page.dupid),
          tableIds = List(),
          title = Seq[CtorType.ChildArg](MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      title(),
                    )
                )),
          helpurl = "../help/duplicate/boardcomplete.html",
          routeCtl = props.routerCtl
        )(

        ),
        DuplicateStore.getView( props.page.getPerspective()) match {
          case Some(score) =>
            <.div(
              <.div(
                AppButton( "Game", "Scoreboard", props.routerCtl.setOnClick(props.page.toScoreboardView()) )
              ),
              score.sortedBoards.map { b =>
                ViewBoard( props.routerCtl, props.page.toBoardView(b.id), score, b.id, state.isIMP )
              }.toTagMod,
              <.div(
                AppButton( "Game2", "Scoreboard", props.routerCtl.setOnClick(props.page.toScoreboardView()) ),
                PageScoreboardInternal.scoringMethodButton( state.useIMP, Some( score.isIMP), false, nextIMPs )
              )
            )
          case None =>
            <.p( "Waiting" )
        }
      )
    }

    val storeCallback = scope.modStateOption { s =>
      DuplicateStore.getMatch().map( md => s.copy( useIMP = Some(md.isIMP) ) )
    }

    val didMount = scope.props >>= { (p) => CallbackTo {
      logger.info("PageAllBoards.didMount")
      DuplicateStore.addChangeListener(storeCallback)
      Controller.monitorMatchDuplicate(p.page.dupid)
    }}

    val willUnmount = CallbackTo {
      logger.info("PageAllBoards.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageAllBoards")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

