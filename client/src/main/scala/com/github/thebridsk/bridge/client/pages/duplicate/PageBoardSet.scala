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
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.BoardScore
import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.ViewBoardSet
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.DuplicateBoardSetView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

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
                      "BoardSet",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        DuplicateStore.getMatch() match {
          case Some(md) if md.id == props.page.dupid =>
            val boardset = md.getBoardSetObject()
              <.div(
                dupStyles.divBoardSetPage,
                <.div(
                  <.h1( "Boards used for the match" )
                ),
                ViewBoardSet(boardset,2),
                <.div( baseStyles.divFlexBreak ),
                <.div(
                  baseStyles.divFooter,
                  <.div(
                    baseStyles.divFooterCenter,
                    AppButton( "Game", "Completed Games Scoreboard", props.routerCtl.setOnClick(props.page.toScoreboard())
                    )
                  )
                )
              )
          case _ =>
            HomePage.loading
        }
      )
    }

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { (p) =>
      logger.info("PageBoardSet.didMount")
      DuplicateStore.addChangeListener(storeCallback)
      CallbackTo( Controller.monitor(p.page.dupid) )
    }

    val willUnmount = CallbackTo {
      logger.info("PageBoardSet.willUnmount")
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

  val component = ScalaComponent.builder[Props]("PageBoardSet")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .componentDidUpdate( didUpdate )
                            .build
}

