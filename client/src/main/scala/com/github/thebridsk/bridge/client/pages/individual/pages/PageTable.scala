package com.github.thebridsk.bridge.client.pages.individual.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.TableView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.bridge.store.IndividualDuplicateStore
import com.github.thebridsk.bridge.client.controller.IndividualController
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles.dupStyles
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles.baseStyles
import com.github.thebridsk.bridge.client.pages.individual.components.DuplicateBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import com.github.thebridsk.bridge.client.pages.individual.components.ViewTable
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton

/**
  * Show the table card for a table in a match.
  *
  * To use, just code the following:
  *
  * {{{
  * PageTable(
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageTable {
  import Internal._

  case class Props(
    router:  BridgeRouter[IndividualDuplicatePage],
    page: TableView
  )

  /**
    * Instantiate the component
    *
    * @param router
    * @param page
    *
    * @return the unmounted react component
    *
    * @see [[PageTable$]] for usage.
    */
  def apply(
    router:  BridgeRouter[IndividualDuplicatePage],
    page: TableView
  ) =
    component(Props(router, page)) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State()

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          DuplicateBridgeAppBar(
            id = Some(props.page.dupid),
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  s"Table ${props.page.tableid.toNumber}"
                )
              )
            ),
            helpurl = "../help/duplicate/table.html",
            routeCtl = props.router
          )(
          ),
          <.div(
            dupStyles.pageTable,
            IndividualDuplicateStore.getCompleteView() match {
              case Some(score) if score.duplicate.id == props.page.dupid =>
                val movement = score.duplicate.getIndividualMovement
                TagMod(
                  ViewTable(
                    props.router,
                    props.page.tableid,
                    movement,
                    score,
                    props.page
                  )
                )
              case _ =>
                HomePage.loading
            },
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterCenter,
                AppButton(
                  "Game",
                  "Completed Games Scoreboard",
                  props.router.setOnClick(
                    CompleteScoreboardView(props.page.sdupid)
                  )
                ),
                ComponentInputStyleButton(Callback {})
              )
            )
          )
        )
      }

      private var mounted = false

      val storeCallback = scope.forceUpdate

      val didMount: Callback = scope.props >>= { p =>
        Callback {
          mounted = true
          IndividualDuplicateStore.addChangeListener(storeCallback)
          IndividualController.monitor(p.page.dupid)
        }
      }

      val willUnmount: Callback = Callback {
        mounted = false
        IndividualDuplicateStore.removeChangeListener(storeCallback)
        IndividualController.delayStop()
      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps.page != props.page) {
          // props have change, reinitialize state
          IndividualController.monitor(props.page.dupid)
          cdu.forceUpdate.runNow()
        }
      }

    val component = ScalaComponent
      .builder[Props]("PageTable")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
