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
  * A component page that shows all the tables.
  *
  * The tables will show the rounds, and for each round which teams are playing where, and the boards being played.
  *
  * To use, just code the following:
  *
  * {{{
  * PageAllTables(
  *   routerCtl = router,
  *   page = AllTableView(dupid)

  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageAllTables {
  import Internal._

  case class Props(routerCtl: BridgeRouter[DuplicatePage], page: AllTableView)

  /**
    * Instantiate the component
    *
    * @param routerCtl
    * @param page - a AllTableView object that identifies the match.
    *
    * @return the unmounted react component
    *
    * @see See [[PageAllTables$]] for usage information.
    */
  def apply(routerCtl: BridgeRouter[DuplicatePage], page: AllTableView) =
    component(
      Props(routerCtl, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.PageAllTables")

    case class State()

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

}

