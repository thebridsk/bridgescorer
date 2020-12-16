package com.github.thebridsk.bridge.clientcommon.component

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.icons
import com.github.thebridsk.materialui.MuiIconButton
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js

/**
  * A component to change the color theme.
  *
  * The color theme is set on the HTML page by applying the data-theme attribute to the body element.
  * Browser storage for the page is also used to persist the theme name.
  *
  * Themes are:
  * - white - typically with a white background
  * - medium - typically with a gray background
  * - dark - typically with a black background
  *
  * The button displays a piechart with three slices,
  * with the bottom slice indicating the current setting.
  *
  * The themes are defined in css.  The theme is selected by using the following css selector:
  * {{{
  * @media screen {
  *   [data-theme="medium"] {
  *     --color-bg: rgb(50,54,57);
  *   }
  * }
  * }}}
  *
  * Clicking the button changes the theme to the next one.
  *
  * To use, just code the following:
  *
  * {{{
  * val buttonStyle = js.Dictionary("root" -> "toolbarIcon")
  * ServerURLButton(
  *   classes = buttonStyle
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object ServerURLButton {
  import Internal._

  case class Props(
      classes: js.UndefOr[js.Dictionary[String]],
  )

  /**
    * Intantiate the component.
    *
    * @param classes - a material-ui defined object.
    *                  Should have one field, "root" with a value that
    *                  identifies the class name to apply to the root element.
    * @return the unmounted react component
    *
    * @see [[ServerURLButton]] for usage.
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
  ) =
    component(Props(classes)) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.ServerURLButton")

    case class State(
      showServerURL: Boolean = false
    ) {
      def setShowServerURLPopup(f: Boolean): State = {
        copy(showServerURL = f)
      }

      def isShowServerURLPopup = showServerURL
    }

    class Backend(scope: BackendScope[Props, State]) {

      def serverUrlClick(event: ReactEvent): Unit = {
        scope.withEffectsImpure.modState(_.setShowServerURLPopup(true))
      }

      val dismissServerUrl: Callback = scope.modState(_.setShowServerURLPopup(false))

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          ^.display.inline,
          MuiIconButton(
            id = "ServerURL",
            onClick = serverUrlClick _,
            title = "Show server URLs",
            color = ColorVariant.inherit,
            classes = props.classes
          )(
            icons.Place()
          ),
          ServerURLPopup(state.showServerURL, dismissServerUrl)
        )
      }
    }

    val component = ScalaComponent
      .builder[Props]("ServerURLButton")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
