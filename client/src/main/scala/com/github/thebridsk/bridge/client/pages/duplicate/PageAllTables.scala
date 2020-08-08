package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.AllTableView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.routes.BridgeRouter
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
  * PageAllTables( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
  * </code></pre>
  *
  * @author werewolf
  */
object PageAllTables {
  import PageAllTablesInternal._

  case class Props(routerCtl: BridgeRouter[DuplicatePage], page: AllTableView)

  def apply(routerCtl: BridgeRouter[DuplicatePage], page: AllTableView) =
    component(
      Props(routerCtl, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object PageAllTablesInternal {
  import PageAllTables._

  val logger: Logger = Logger("bridge.PageAllTables")

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
                "All Tables"
              )
            )
          ),
          helpurl = "../help/duplicate/table.html",
          routeCtl = props.routerCtl
        )(
        ),
        DuplicateStore.getCompleteView() match {
          case Some(score) if score.id == props.page.dupid =>
            val clickPage = CompleteScoreboardView(props.page.sdupid)
            <.div(
              dupStyles.divAllTablesPage,
              score.tables.keys.toList
                .sortWith((t1, t2) => t1 < t2)
                .map { table =>
                  ViewTable(
                    props.routerCtl,
                    props.page.toTableView(table),
                    true
                  )
                }
                .toTagMod,
              <.div(baseStyles.divFlexBreak),
              <.div(
                baseStyles.divFooter,
                <.div(
                  baseStyles.divFooterCenter,
                  AppButton(
                    "Game",
                    "Completed Games Scoreboard",
                    props.routerCtl.setOnClick(clickPage)
                  ),
                  ComponentInputStyleButton(Callback {})
                )
              )
            )
          case _ =>
            <.div(
              HomePage.loading
            )
        }
      )
    }

    val storeCallback = scope.forceUpdate

    val didMount: Callback = scope.props >>= { (p) =>
      Callback {
        logger.info("PageAllTables.didMount")
        DuplicateStore.addChangeListener(storeCallback)

        Controller.monitor(p.page.dupid)
      }
    }

    val willUnmount: Callback = Callback {
      logger.info("PageAllTables.willUnmount")
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
    .builder[Props]("PageAllTables")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .componentDidUpdate(didUpdate)
    .build
}
