package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

/**
 * A skeleton Popup.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * Popup( Popup.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object Popup {
  import PopupInternal._

  case class Props( display: Boolean, content: TagMod )

  def apply( display: Boolean, content: TagMod ) = component(Props(display,content))

}

object PopupInternal {
  import Popup._
  /**
   * Internal state for rendering the Popup.
   *
   * I'd like this class to be private, but the instantiation of Popup
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the Popup.
   *
   * I'd like this class to be private, but the instantiation of Popup
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      import com.example.pages.BaseStyles._
      val disp = ^.display.none.when(!props.display)
      <.div(
        <.div(
          ^.id:="overlay",
          baseStyles.divPopupOverlay,
          disp,
        ),
        <.div(
          ^.id:="popup",
          baseStyles.divPopup,
          disp,
          <.div(
            props.content
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("PopupComponent")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

