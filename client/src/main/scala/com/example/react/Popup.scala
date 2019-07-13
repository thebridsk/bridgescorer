package com.github.thebridsk.bridge.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger

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

  case class Props( display: Boolean, content: TagMod, id: Option[String] )

  def apply( display: Boolean, content: TagMod, id: Option[String] = None ) = component(Props(display,content,id))

}

object PopupInternal {
  import Popup._

  val logger = Logger("bridge.Popup")

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
      import com.github.thebridsk.bridge.pages.BaseStyles._
      val disp = ^.display.none.when(!props.display)
      <.div(
        props.id.whenDefined { i =>
          logger.info(s"""Popup.render setting id to $i""")
           ^.id:=i
        },
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

