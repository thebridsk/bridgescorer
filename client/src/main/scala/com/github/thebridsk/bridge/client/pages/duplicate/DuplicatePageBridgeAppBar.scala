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
import com.github.thebridsk.materialui.component.MyMenu
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
      id: Option[MatchDuplicate.Id],
      tableIds: List[Table.Id],
      pageMenuItems: Seq[CtorType.ChildArg],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[DuplicatePage]
  )

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
}

object DuplicatePageBridgeAppBarInternal {
  import DuplicatePageBridgeAppBar._

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

      def callbackPage(page: DuplicatePage)(e: ReactEvent) =
        props.routeCtl.set(page).runNow()

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

  private[duplicate] val component = ScalaComponent
    .builder[Props]("DuplicatePageBridgeAppBar")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build
}
