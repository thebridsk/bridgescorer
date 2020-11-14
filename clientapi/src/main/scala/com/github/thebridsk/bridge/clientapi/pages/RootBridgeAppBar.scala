package com.github.thebridsk.bridge.clientapi.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.icons
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.materialui.component.MyMenu
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import japgolly.scalajs.react.vdom.VdomNode
import com.github.thebridsk.bridge.clientcommon.logger.Info
import com.github.thebridsk.materialui.icons.SvgColor
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import japgolly.scalajs.react.internal.Effect

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
  * RootBridgeAppBar( RootBridgeAppBar.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object RootBridgeAppBar {
  import RootBridgeAppBarInternal._

  case class Props(
      title: Seq[VdomNode],
      helpurl: Option[String],
      routeCtl: BridgeRouter[AppPage]
  )

  def apply(
      title: Seq[VdomNode],
      helpurl: Option[String],
      routeCtl: BridgeRouter[AppPage]
  )(): TagMod = {
    TagMod(
      ServerURLPopup(),
      component(Props(title, helpurl, routeCtl))
    )
  }
}

object RootBridgeAppBarInternal {
  import RootBridgeAppBar._

  val logger: Logger = Logger("bridge.RootBridgeAppBar")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      userSelect: Boolean = false,
      anchorMainEl: js.UndefOr[Element] = js.undefined,
      anchorMainTestHandEl: js.UndefOr[Element] = js.undefined
  ) {

    def openMainMenu(n: Node): State =
      copy(anchorMainEl = n.asInstanceOf[Element])
    def closeMainMenu(): State = copy(anchorMainEl = js.undefined)

    def openMainTestHandMenu(n: Node): State =
      copy(anchorMainTestHandEl = n.asInstanceOf[Element])
    def closeMainTestHandMenu(): State =
      copy(anchorMainTestHandEl = js.undefined)
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
      scope.modState { s => s.closeMainMenu() }.runNow()
    }

    def gotoPage(uri: String): Unit = {
      GotoPage.inSameWindow(uri)
    }

    def handleGotoPageClick(uri: String)(event: ReactEvent): Unit = {
      logger.info(s"""Going to page ${uri}""")
      handleMainClose()
      gotoPage(uri)
    }

    val toggleUserSelect: ReactEvent => Effect.Id[Unit] = {
      (event: ReactEvent) =>
        scope.withEffectsImpure.modState { s =>
          val newstate = s.copy(userSelect = !s.userSelect)
          val style = Info.getElement("allowSelect")
          if (newstate.userSelect) {
            style.innerHTML = """
                                |* {
                                |  user-select: text;
                                |}
                                |""".stripMargin
          } else {
            style.innerHTML = ""
          }
          newstate
        }
    }

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

      def callbackPage(page: AppPage)(e: ReactEvent) = {
        logger.info(s"""Goto page $page""")
        handleMainClose()
        props.routeCtl.set(page).runNow()
      }

      val maintitle: Seq[VdomNode] =
        List(
          MuiTypography(
            variant = TextVariant.h6,
            color = TextColor.inherit
          )(
            <.span(
              "Bridge ScoreKeeper"
            )
          )
        )

      <.div(
        baseStyles.divAppBar,
        BridgeAppBar(
          handleMainClick = handleMainClick _,
          maintitle = maintitle,
          title = props.title,
          helpurl = props.helpurl.getOrElse("../help/introduction.html"),
          routeCtl = props.routeCtl,
          showHomeButton = !props.title.isEmpty
        )(
          // main menu
          MyMenu(
            anchorEl = state.anchorMainEl,
            onClickAway = handleMainClose _
//                onItemClick = handleMainCloseClick _,
          )(
            {
              val path = GotoPage.currentURL
              val (newp, color, check) =
                if (path.indexOf("indexNoScale") >= 0) {
                  ("""index.html""", SvgColor.disabled, false)
                } else {
                  ("""indexNoScale.html""", SvgColor.inherit, true)
                }
              val newpath = if (path.endsWith(".gz")) {
                s"""${newp}.gz"""
              } else {
                newp
              }
              MuiMenuItem(
                id = "NoScaling",
                onClick = handleGotoPageClick(newp) _,
                classes = js.Dictionary("root" -> "mainMenuItem")
              )(
                "Scaling ",
//                      icons.Check(
//                          color=color,
//                          classes = js.Dictionary("root" -> "mainMenuItemIcon")
//                      )
                if (check) {
                  icons.CheckBox()
                } else {
                  icons.CheckBoxOutlineBlank()
                }
              )
            },
            MuiMenuItem(
              id = "UserSelect",
              onClick = toggleUserSelect,
              classes = js.Dictionary("root" -> "mainMenuItem")
            )(
              "Allow Select", {
//                      val color = if (state.userSelect) SvgColor.inherit else SvgColor.disabled
//                      icons.Check(
//                          color=color,
//                          classes = js.Dictionary("root" -> "mainMenuItemIcon")
//                      )
                if (state.userSelect) {
                  icons.CheckBox()
                } else {
                  icons.CheckBoxOutlineBlank()
                }
              }
            )
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

  private[pages] val component = ScalaComponent
    .builder[Props]("RootBridgeAppBar")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build
}
