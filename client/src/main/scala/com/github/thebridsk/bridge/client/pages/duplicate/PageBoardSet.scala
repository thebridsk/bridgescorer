package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
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
  * A component page that shows the board set in use for a duplicate match.
  *
  * This shows the dealer and vulnerbility of all boards.
  *
  * To use, just code the following:
  *
  * {{{
  * val page = DuplicateBoardSetView(dupid)
  *
  * PageBoardSet(
  *   routerCtl = router,
  *   page = page
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageBoardSet {
  import Internal._

  case class Props(
      routerCtl: BridgeRouter[DuplicatePage],
      page: DuplicateBoardSetView
  )

  /**
    * Instantiate the component
    *
    * @param routerCtl - the router
    * @param page - a DuplicateBoardSetView that identifies the duplicate match.
    *
    * @return the unmounted react component
    *
    * @see See [[PageBoardSet$]] for usage information.
    */
  def apply(
      routerCtl: BridgeRouter[DuplicatePage],
      page: DuplicateBoardSetView
  ) =
    component(
      Props(routerCtl, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.PageBoardSet")

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State()

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        import DuplicateStyles._
        <.div(
          DuplicatePageBridgeAppBar(
            id = Some(props.page.dupid),
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  "BoardSet"
                )
              )
            ),
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
                  <.h1("Boards used for the match")
                ),
                ViewBoardSet(boardset, 2),
                <.div(baseStyles.divFlexBreak),
                <.div(
                  baseStyles.divFooter,
                  <.div(
                    baseStyles.divFooterCenter,
                    AppButton(
                      "Game",
                      "Completed Games Scoreboard",
                      props.routerCtl.setOnClick(props.page.toScoreboard)
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

      val didMount: Callback = scope.props >>= { (p) =>
        logger.info("PageBoardSet.didMount")
        DuplicateStore.addChangeListener(storeCallback)
        CallbackTo(Controller.monitor(p.page.dupid))
      }

      val willUnmount: Callback = CallbackTo {
        logger.info("PageBoardSet.willUnmount")
        DuplicateStore.removeChangeListener(storeCallback)
        Controller.delayStop()
      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps.page != props.page) {
          Controller.monitor(props.page.dupid)
        }
      }

    private[duplicate] val component = ScalaComponent
      .builder[Props]("PageBoardSet")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
