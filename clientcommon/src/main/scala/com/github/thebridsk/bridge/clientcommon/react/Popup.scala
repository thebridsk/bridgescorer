package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger

/**
  * A Popup react component.
  *
  * Displays a popup.  The entire page is dimmed, while the content of the popup is centered on the display.
  *
  * Usage:
  *
  * {{{
  * Popup(
  *   display = true,
  *   content = <.div("Hello World!")
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the parameters.
  *
  * @author werewolf
  */
object Popup {
  import Internal._

  case class Props(
    display: Boolean,
    content: TagMod,
    id: Option[String],
    clickaway: Option[Callback]
  )

  /**
    * Instantiate the react component.
    *
    * @param display true if the popup is displayed, false if hidden.
    * @param content the content that is displayed in the popup.
    * @param id the id attribute of the root element of the popup.
    * @param clickaway - the optional callback for the clickaway.
    *
    * @return the unmounted react component.
    */
  def apply(
    display: Boolean,
    content: TagMod,
    id: Option[String] = None,
    clickaway: Option[Callback] = None
  ) =
    component(
      Props(display, content, id, clickaway)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  object Internal {

    val logger: Logger = Logger("bridge.Popup")

    case class State()

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
        val disp = ^.display.none.when(!props.display)
        <.div(
          props.id.whenDefined { i =>
            logger.info(s"""Popup.render setting id to $i""")
            ^.id := i
          },
          <.div(
            ^.id := "overlay",
            baseStyles.divPopupOverlay,
            disp
          ),
          <.div(
            ^.id := "popup",
            baseStyles.divPopup,
            disp,
            props.clickaway.whenDefined( ^.onClick --> _),
            <.div(
              props.content
            )
          )
        )
      }
    }

    private[react] val component = ScalaComponent
      .builder[Props]("PopupComponent")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
