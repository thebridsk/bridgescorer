package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.client.pages.BridgeAppBar
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom.Element
import org.scalajs.dom.Node
import scala.scalajs.js

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
  * ChicagoPageBridgeAppBar( ChicagoPageBridgeAppBar.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object ChicagoPageBridgeAppBar {
  import ChicagoPageBridgeAppBarInternal._

  case class Props(
      pageMenuItems: Seq[CtorType.ChildArg],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[ChicagoPage]
  )

  def apply(
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[ChicagoPage]
  )(
      mainMenuItems: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(mainMenuItems, title, helpurl, routeCtl))
  }
}

object ChicagoPageBridgeAppBarInternal {
  import ChicagoPageBridgeAppBar._

  val logger: Logger = Logger("bridge.ChicagoPageBridgeAppBar")

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

    def handleMainClick(event: ReactEvent): Unit =
      event.extract(_.currentTarget)(currentTarget =>
        scope.modState(s => s.openMainMenu(currentTarget)).runNow()
      )
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

      def callbackPage(page: ChicagoPage)(e: ReactEvent) =
        props.routeCtl.set(page).runNow()

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
                "Chicago Bridge"
              )
            )
          ),
          title = props.title,
          helpurl = props.helpurl,
          routeCtl = props.routeCtl
        )(
          // main menu
//              MyMenu(
//                  anchorEl=state.anchorMainEl,
//                  onClickAway = handleMainClose _,
//                  onItemClick = handleMainCloseClick _,
//              )(
//              )
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

  private[chicagos] val component = ScalaComponent
    .builder[Props]("ChicagoPageBridgeAppBar")
    .initialStateFromProps { props =>
      State()
    }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build
}
