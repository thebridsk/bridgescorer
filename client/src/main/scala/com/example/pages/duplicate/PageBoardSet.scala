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
import com.example.data.bridge.BoardScore
import com.example.pages.duplicate.boardsets.ViewBoardSet
import com.example.pages.duplicate.DuplicateRouter.DuplicateBoardSetView
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
 * PageBoardSet( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageBoardSet {
  import PageBoardSetInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: DuplicateBoardSetView )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: DuplicateBoardSetView ) = component(Props(routerCtl,page))

}

object PageBoardSetInternal {
  import PageBoardSet._

  val logger = Logger("bridge.PageBoardSet")

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
      DuplicateStore.getMatch() match {
        case Some(md) =>
          val boardset = md.getBoardSetObject()
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
                          "BoardSet",
                        )
                    )),
              helpurl = "../help/duplicate/summary.html",
              routeCtl = props.routerCtl
            )(

            ),
            <.div(
              baseStyles.divText100,
              <.h1( "Boards used for the match" )
            ),
            ViewBoardSet(boardset,2),
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton( "Game", "Completed Games Scoreboard", props.routerCtl.setOnClick(props.page.toScoreboard())
                )
              )
            )
          )
        case None =>
          <.p( "Waiting" )
      }
    }

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { (p) =>
      logger.info("PageBoardSet.didMount")
      DuplicateStore.addChangeListener(storeCallback)
      CallbackTo( Controller.monitorMatchDuplicate(p.page.dupid) )
    }

    val willUnmount = CallbackTo {
      logger.info("PageBoardSet.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageBoardSet")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

