package com.example.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.materialui.MuiAppBar
import com.example.materialui.Position
import com.example.materialui.MuiToolbar
import com.example.materialui.MuiIconButton
import com.example.materialui.icons.MuiMenuIcon
import com.example.materialui.MuiTypography
import com.example.materialui.ColorVariant
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import com.example.materialui.icons.MuiHomeIcon
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import utils.logging.Logger
import com.example.materialui.icons.MuiHelpIcon
import japgolly.scalajs.react.vdom.HtmlStyles
import com.example.materialui.component.MyMenu
import com.example.materialui.MuiMenuItem
import com.example.routes.AppRouter.AppPage
import com.example.routes.BridgeRouter
import com.example.routes.AppRouter.About
import com.example.react.AppButtonLinkNewWindow
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.VdomNode
import com.example.routes.AppRouter.Home
import com.example.pages.BaseStyles
import com.example.data.Id
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.TableView
import com.example.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.example.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.example.pages.duplicate.DuplicateRouter.DuplicateBoardSetView
import com.example.pages.duplicate.DuplicateRouter.AllTableView
import com.example.pages.BridgeAppBar
import com.example.materialui.icons.MuiCheckIcon
import com.example.pages.HomePage
import com.example.materialui.icons.SvgColor
import com.example.skeleton.react.BeepComponent

/**
 * A simple AppBar for the Bridge client.
 *
 * It can be used for all pages but the home page.
 *
 * The AppBar has in the banner from left to right:
 *
 * <ol>
 * <li>Page Menu button
 * <li>Home button
 * <li>title
 * <li>Help button
 * </ol>
 *
 * To use, just code the following:
 *
 * <pre><code>
 * DuplicatePageBridgeAppBar( DuplicatePageBridgeAppBar.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object DuplicatePageBridgeAppBar {
  import DuplicatePageBridgeAppBarInternal._

  case class Props(
      id: Option[Id.MatchDuplicate],
      tableIds: List[String],
      pageMenuItems: Seq[CtorType.ChildArg],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[DuplicatePage]
  )

  def apply(
      id: Option[Id.MatchDuplicate],
      tableIds: List[String],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[DuplicatePage]
  )(
      mainMenuItems: CtorType.ChildArg*,
  ) =
    component(Props(id,tableIds,mainMenuItems,title,helpurl,routeCtl))

}

object DuplicatePageBridgeAppBarInternal {
  import DuplicatePageBridgeAppBar._

  val logger = Logger("bridge.DuplicatePageBridgeAppBar")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
      anchorMainEl: js.UndefOr[Element] = js.undefined
  ) {

    def openMainMenu( n: Node ) = copy( anchorMainEl = n.asInstanceOf[Element] )
    def closeMainMenu() = copy( anchorMainEl = js.undefined )
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def handleMainClick( event: ReactEvent ) = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openMainMenu(currentTarget)).runNow() )
    def handleMainCloseClick( event: ReactEvent ) = scope.modState(s => s.closeMainMenu()).runNow()
    def handleMainClose( /* event: js.Object, reason: String */ ) = {
      logger.fine("MainClose called")
      scope.modState(s => s.closeMainMenu()).runNow()
    }

    def render( props: Props, state: State ) = {
      import BaseStyles._

      def handleGotoHome(e: ReactEvent) = props.routeCtl.toHome
      def handleGotoAbout(e: ReactEvent) = props.routeCtl.toAbout

      def callbackPage(page: DuplicatePage)(e: ReactEvent) = props.routeCtl.set(page).runNow()

      def tableMenuItem( dupid: String, tid: String ): CtorType.ChildArg = {
        MuiMenuItem(
            id = s"Table$tid",
            onClick = callbackPage(TableView(dupid,tid)) _
        )(
            s"Table $tid"
        ),
      }

      <.div( ^.width := "100%",
          BridgeAppBar(
            handleMainClick = handleMainClick _,
            maintitle =
              List[VdomNode](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Duplicate Bridge",
                    )
                )
              ),
            title = props.title,
            helpurl = props.helpurl,
            routeCtl = props.routeCtl
          )(
              // main menu
              MyMenu(
                  anchorEl=state.anchorMainEl,
                  onClickAway = handleMainClose _,
                  onItemClick = handleMainCloseClick _,
              )(
                (
                  props.pageMenuItems.toList :::
                  props.id.map { id =>
                    List[CtorType.ChildArg](
                      MuiMenuItem(
                          id = "Complete",
                          onClick = callbackPage(CompleteScoreboardView(id)) _
                      )(
                          "Scoreboard"
                      ),
                      MuiMenuItem(
                          id = "BoardSet",
                          onClick = callbackPage(DuplicateBoardSetView(id)) _
                      )(
                          "BoardSet"
                      ),
                      MuiMenuItem(
                          id = "Tables",
                          onClick = callbackPage(AllTableView(id)) _
                      )(
                          "Tables"
                      ),
                    ) :::
                    props.tableIds.sortWith( (l,r) => l.toInt < r.toInt).map { tid => tableMenuItem(id,tid) }
                  }.getOrElse(
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
                      ),
                    )
                  ) ::: List[CtorType.ChildArg](
                      MuiMenuItem(
                          id = "Summary",
                          onClick = callbackPage(SummaryView) _
                      )(
                          "Summary"
                      ),
//                      MuiMenuItem(
//                          id = "FastClick",
//                          onClick = ( (e: ReactEvent) => (HomePage.fastclickToggle>>scope.forceUpdate).runNow() ),
//                          classes = js.Dictionary("root" -> "mainMenuItem").asInstanceOf[js.Object]
//
//                      )(
//                          "FastClick ",
//                          MuiCheckIcon(
//                              color= (if (HomePage.isFastclickOn) SvgColor.inherit else SvgColor.disabled),
//                              classes = js.Dictionary("root" -> "mainMenuItemIcon").asInstanceOf[js.Object]
//                          )()
//                      ),
                      BeepComponent.getMenuItem( ()=>scope.withEffectsImpure.forceUpdate ),
                  )
                ): _*
              )
          )
      )
    }

    private var mounted = false

    val didMount = Callback {
      mounted = true

    }

    val willUnmount = Callback {
      mounted = false

    }
  }

  val component = ScalaComponent.builder[Props]("DuplicatePageBridgeAppBar")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

