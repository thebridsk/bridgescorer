package com.github.thebridsk.bridge.client.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.component.MyMenu
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import japgolly.scalajs.react.vdom.VdomNode
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.DuplicateBoardSetView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.AllTableView
import com.github.thebridsk.bridge.client.pages.BridgeAppBar
import com.github.thebridsk.bridge.clientcommon.react.BeepComponent
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.materialui.AnchorOrigin
import com.github.thebridsk.materialui.AnchorOriginHorizontalValue
import com.github.thebridsk.materialui.AnchorOriginVerticalValue

/**
  * A simple AppBar for the duplicate pages.
  *
  * The AppBar has in the banner from left to right:
  *
  * 1. Left Buttons
  *   - Main Menu button
  *   - Home button
  * 2. Title
  * 3. Logo
  * 4. Right Buttons
  *   - Help button
  *   - Show Server URL
  *   - Dark mode selector
  *   - Fullscreen
  *   - More menu
  *
  * The more menu contains:
  * - About and info pages
  * - Debug logging pages
  *
  * The Main Menu button is only displayed if there is an main menu item.
  * Provide popup for Server URLs.
  *
  * To use, just code the following:
  *
  * {{{
  * case class Props(
  *     title: Seq[VdomNode],
  *     helpurl: Option[String],
  *     routeCtl: BridgeRouter[AppPage],
  * )
  *
  * case class State(
  *     anchorMainEl: js.UndefOr[Element] = js.undefined,
  * }
  *
  * class Backend(scope: BackendScope[Props, State]) {
  *
  *   def handleMainClick(event: ReactEvent): Unit = {
  *     event.stopPropagation()
  *     event.extract(_.currentTarget)(currentTarget =>
  *       scope.modState(s => s.copy(anchorMainEl = currentTarget)).runNow()
  *     )
  *   }
  *   def handleMainClose( /* event: js.Object, reason: String */ ): Unit = {
  *     scope.modState(s => s.copy(anchorMainEl = js.undefined)).runNow()
  *   }
  *
  *   def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
  *     DuplicatePageBridgeAppBar(
  *        handleMainClick = handleMainClick _,
  *        maintitle = List("App title"),
  *        title = props.title
  *        helpurl = props.helpurl.getOrElse("../help/introduction.html"),
  *        routeCtl = props.routeCtl,
  *     )(
  *       MyMain(
  *           anchorEl = state.anchorMainEl,
  *           onClose = handleMainClose _,
  *           anchorOrigin = AnchorOrigin(
  *              AnchorOriginHorizontalValue.left,
  *              AnchorOriginVerticalValue.bottom
  *           ),
  *           transformOrigin = AnchorOrigin(
  *              AnchorOriginHorizontalValue.left,
  *              AnchorOriginVerticalValue.top
  *           )
  *       )(
  *         MuiMenuItem(
  *           onClick = ...
  *         )( "item label" )
  *       )
  *     )
  *   }
  * }
  * }}}
  *
  * Note that if an item is clicked on the menu, the menu will not automatically close.
  * The onClick handler on the menu item must close the menu.
  *
  * @see See [[apply]] for the description of the arguments to instantiate the component.
  *
  * @author werewolf
  */
object DuplicatePageBridgeAppBar {
  import Internal._

  case class Props(
      id: Option[MatchDuplicate.Id],
      tableIds: List[Table.Id],
      pageMenuItems: Seq[CtorType.ChildArg],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[DuplicatePage]
  )

  /**
    * Instantiate the component
    *
    * @param id - optional duplicate Id that is subject of the page.
    * @param tableIds - the tables in the duplicate match.
    * @param title - the title of the page
    * @param helpurl - the help url for the page.
    * @param routeCtl - the router
    * @param mainMenuItems - the main menu items
    *
    * @return the unmounted react component.
    */
  def apply(
      id: Option[MatchDuplicate.Id],
      tableIds: List[Table.Id],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[DuplicatePage]
  )(
      mainMenuItems: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(id, tableIds, mainMenuItems, title, helpurl, routeCtl))
  }

  protected object Internal {

    val logger: Logger = Logger("bridge.DuplicatePageBridgeAppBar")

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(
        anchorMainEl: js.UndefOr[Element] = js.undefined
    ) {

      def openMainMenu(n: Node): State =
        copy(anchorMainEl = n.asInstanceOf[Element])
      def closeMainMenu(): State = copy(anchorMainEl = js.undefined)
    }

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      def handleMainClick(event: ReactEvent): Unit = {
        event.stopPropagation()
        event.extract(_.currentTarget)(currentTarget =>
          scope.modState(s => s.openMainMenu(currentTarget)).runNow()
        )
      }
      def handleMainCloseClick(event: ReactEvent): Unit =
        scope.modState(s => s.closeMainMenu()).runNow()
      def handleMainClose( /* event: js.Object, reason: String */ ): Unit = {
        logger.fine("MainClose called")
        scope.modState(s => s.closeMainMenu()).runNow()
      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        import BaseStyles._

        def handleGotoHome(e: ReactEvent) = props.routeCtl.toHome
        def handleGotoAbout(e: ReactEvent) = props.routeCtl.toAbout

        def callbackPage(page: DuplicatePage)(e: ReactEvent) = {
          // handleMainClose()
          props.routeCtl.set(page).runNow()
        }

        def tableMenuItem(
            dupid: MatchDuplicate.Id,
            tid: Table.Id
        ): CtorType.ChildArg = {
          MuiMenuItem(
            id = s"Table${tid.toNumber}",
            onClick = callbackPage(TableView(dupid.id, tid.id)) _
          )(
            s"Table ${tid.toNumber}"
          )
        }

        <.div(
          baseStyles.divAppBar,
          BridgeAppBar(
            handleMainClick = handleMainClick _,
            maintitle = List[VdomNode](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  "Duplicate Bridge"
                )
              )
            ),
            title = props.title,
            helpurl = props.helpurl,
            routeCtl = props.routeCtl
          )(
            // main menu
            MyMenu(
              anchorEl = state.anchorMainEl,
              onClose = handleMainClose _,
              anchorOrigin = AnchorOrigin(
                AnchorOriginHorizontalValue.left,
                AnchorOriginVerticalValue.bottom
              ),
              transformOrigin = AnchorOrigin(
                AnchorOriginHorizontalValue.left,
                AnchorOriginVerticalValue.top
              )
            )(
              (
                props.pageMenuItems.toList :::
                  props.id
                    .map { id =>
                      List[CtorType.ChildArg](
                        MuiMenuItem(
                          id = "Complete",
                          onClick = callbackPage(CompleteScoreboardView(id.id)) _
                        )(
                          "Scoreboard"
                        ),
                        MuiMenuItem(
                          id = "BoardSet",
                          onClick = callbackPage(DuplicateBoardSetView(id.id)) _
                        )(
                          "BoardSet"
                        ),
                        MuiMenuItem(
                          id = "Tables",
                          onClick = callbackPage(AllTableView(id.id)) _
                        )(
                          "Tables"
                        )
                      ) :::
                        props.tableIds.sorted.map { tid =>
                          tableMenuItem(id, tid)
                        }
                    }
                    .getOrElse(
                      List[CtorType.ChildArg](
                        MuiMenuItem(
                          id = "BoardSets",
                          onClick = callbackPage(BoardSetSummaryView) _
                        )(
                          "BoardSets"
                        ),
                        MuiMenuItem(
                          id = "Movements",
                          onClick = callbackPage(MovementSummaryView) _
                        )(
                          "Movements"
                        )
                      )
                    ) :::
                  List[CtorType.ChildArg](
                    MuiMenuItem(
                      id = "Summary",
                      onClick = callbackPage(SummaryView) _
                    )(
                      "Summary"
                    ),
                    BeepComponent.getMenuItem(() =>
                      scope.withEffectsImpure.forceUpdate
                    )
                  )
              ): _*
            )
          )
        )
      }

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true

      }

      val willUnmount: Callback = Callback {
        mounted = false

      }
    }

    val component = ScalaComponent
      .builder[Props]("DuplicatePageBridgeAppBar")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
