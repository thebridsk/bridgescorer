package com.github.thebridsk.bridge.client.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import scala.concurrent.ExecutionContext
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.ServerURLStore

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ServerURLPopup( ServerURLPopup.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ServerURLPopup {
  import ServerURLPopupInternal._

  case class Props( )

  def apply( ) = component(Props())

  private var showURL = false

  def setShowServerURLPopup( f: Boolean ): Unit = {
    showURL = f
    scalajs.js.timers.setTimeout(1) {
      ServerURLStore.notifyChange()
    }
  }

  def isShowServerURLPopup = showURL

}

object ServerURLPopupInternal {
  import ServerURLPopup._

  private val logger = Logger("bridge.ServerURLPopup")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val ok: Option[Callback] = Some( Callback {
      setShowServerURLPopup(false)
      scope.withEffectsImpure.forceUpdate
    })
    val cancel: Option[Callback] = None

    def render( props: Props, state: State ) = {
      val content: Option[TagMod] = if (isShowServerURLPopup) {
        implicit val ec = ExecutionContext.global
        Some(
          <.div(
            <.h1("Server URL"),
            <.ul( ServerURLStore.getURLItems )
          )
        )
      } else {
        None
      }
      PopupOkCancel( content, ok, cancel, Some("ServerURLPopupDiv") )
    }

    private var mounted = false

    val urlStoreListener = scope.forceUpdate

    val didMount = Callback {
      mounted = true
      ServerURLStore.addChangeListener(urlStoreListener)
      ServerURLStore.updateURLs()
    }

    val willUnmount = Callback {
      mounted = false
      ServerURLStore.removeChangeListener(urlStoreListener)
    }

  }

  val component = ScalaComponent.builder[Props]("ServerURLPopup")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

