package com.github.thebridsk.bridge.clientcommon.component

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import scala.concurrent.ExecutionContext
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.store.ServerURLStore

/**
  * A popup component that shows the server URLs.
  *
  * To use, just code the following:
  *
  * {{{
  * case class State(
  *   showServerURL: Boolean = false
  * )
  *
  * val dismissCB = scope.modState(_.copy(showServerURL = false))
  *
  * ServerURLPopup( state.showServerURL, dismissCB )
  * }}}
  *
  * @see See [[apply]] for the description of the arguments to instantiate the component.
  *
  * @author werewolf
  */
object ServerURLPopup {
  import Internal._

  case class Props(
    showURL: Boolean,
    dismissCB: Callback
  )

  /**
    * Instantiate the ServerURLPopup component.
    *
    * @param showURL - show the popup
    * @param dismissCB - callback to dismiss the popup.
    * @return the unmounted react component
    *
    * @see [[ServerURLPopup]] for usage.
    */
  def apply(
    showURL: Boolean,
    dismissCB: Callback
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(showURL, dismissCB))
  }

  protected object Internal {

    private val logger = Logger("bridge.ServerURLPopup")

    case class State()

    class Backend(scope: BackendScope[Props, State]) {

      val cancel: Option[Callback] = None

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; ReactComponent
        val ok: Option[Callback] = Some(props.dismissCB)
        val content: Option[TagMod] = if (props.showURL) {
          implicit val ec = ExecutionContext.global
          Some(
            <.div(
              <.h1("Server URL"),
              <.ul(ServerURLStore.getURLItems)
            )
          )
        } else {
          None
        }
        PopupOkCancel(content, ok, cancel, Some("ServerURLPopupDiv"), ok)
      }

      private var mounted = false

      val urlStoreListener = scope.forceUpdate

      val didMount: Callback = Callback {
        mounted = true
        ServerURLStore.addChangeListener(urlStoreListener)
        ServerURLStore.updateURLs()
      }

      val willUnmount: Callback = Callback {
        mounted = false
        ServerURLStore.removeChangeListener(urlStoreListener)
      }

    }

    val component = ScalaComponent
      .builder[Props]("ServerURLPopup")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
