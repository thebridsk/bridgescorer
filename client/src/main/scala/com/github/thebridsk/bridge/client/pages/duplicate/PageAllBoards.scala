package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseAllBoardsViewWithPerspective
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableRoundAllBoardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.data.Table

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

    def isMP = !useIMP.getOrElse(false)
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
        props.page.getPerspective match {
          case PerspectiveTable(team1, team2) =>
            val (currentRound,currentTable) = props.page match {
              case trabv: TableRoundAllBoardView => (trabv.round,trabv.tableid)
              case _ => (-1,Table.idNul)
            }
            <.span(
              s"Table ${currentTable.toNumber} Round ${currentRound}" ,
              s" Teams ${team1.toNumber} and ${team2.toNumber}",
              " Board View"
            )
          case PerspectiveDirector =>
            <.span("Director's Board View")
          case PerspectiveComplete =>
            <.span("Completed Board View")
        }

      }

      <.div(
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
        DuplicateStore.getView( props.page.getPerspective) match {
          case Some(score) if score.id == props.page.dupid =>
            <.div(
              dupStyles.divAllBoardsPage,
              <.div(
                AppButton( "Game", "Scoreboard", props.routerCtl.setOnClick(props.page.toScoreboardView) )
              ),
              score.sortedBoards.map { b =>
                ViewBoard( props.routerCtl, props.page.toBoardView(b.id), score, b.id, state.isIMP, DuplicateStore.getPicture(props.page.dupid, b.id) )
              }.toTagMod,
              <.div(
                AppButton( "Game2", "Scoreboard", props.routerCtl.setOnClick(props.page.toScoreboardView) ),
                PageScoreboardInternal.scoringMethodButton( state.useIMP, Some( score.isIMP), false, nextIMPs )
              )
            )
          case _ =>
            HomePage.loading
        }
      )
    }

    val storeCallback = scope.modStateOption { s =>
      DuplicateStore.getMatch().map( md => s.copy( useIMP = Some(md.isIMP) ) )
    }

    val didMount = scope.props >>= { (p) => CallbackTo {
      logger.info("PageAllBoards.didMount")
      DuplicateStore.addChangeListener(storeCallback)
      Controller.monitor(p.page.dupid)
    }}

    val willUnmount = CallbackTo {
      logger.info("PageAllBoards.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
    }
  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = Callback {
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (prevProps.page != props.page) {
      Controller.monitor(props.page.dupid)
    }
  }

  val component = ScalaComponent.builder[Props]("PageAllBoards")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .componentDidUpdate( didUpdate )
                            .build
}

