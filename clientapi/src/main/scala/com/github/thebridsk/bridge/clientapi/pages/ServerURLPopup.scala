package com.github.thebridsk.bridge.clientapi.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerURL
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.utilities.logging.Logger

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

  case class Props()

  def apply() =
    component(Props()) // scalafix:ok ExplicitResultTypes; ReactComponent

  private var showURL = false

  def setShowServerURLPopup(f: Boolean): Unit = {
    showURL = f
    notifyListeners
  }

  def isShowServerURLPopup = showURL
}

object ServerURLPopupInternal {
  import ServerURLPopup._

  private val logger = Logger("bridge.ServerURLPopup")

  private var listeners: List[Listener] = Nil

  trait Listener {
    def showChanged(f: Boolean): Unit
  }

  def addListener(l: Listener): Unit = {
    listeners = l :: listeners
  }

  def removeListener(l: Listener): Unit = {
    listeners = listeners.filter(f => f != l)
  }

  def notifyListeners: Unit = {
    listeners.foreach(l => l.showChanged(isShowServerURLPopup))
  }

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      serverUrl: Option[ServerURL] = None,
      requestedUrl: Boolean = false
  )

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    val ok: Option[Callback] = Some(Callback {
      setShowServerURLPopup(false)
      scope.withEffectsImpure.forceUpdate
    })
    val cancel: Option[Callback] = None

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; ReactComponent
      val content: Option[TagMod] = if (isShowServerURLPopup) {
        import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
        val item = if (BridgeDemo.isDemo) {
          Some(
            <.li(
              "Demo mode, all data entered will be lost on page refresh or closing page"
            )
          )
        } else if (state.serverUrl.isEmpty && !state.requestedUrl) {
          js.timers.setTimeout(1) {
            scope.withEffectsImpure.modState(s => s.copy(requestedUrl = true))
          }
          logger.info("In ServerURLPopup.render, no server URLs, requesting")
          RestClientServerURL
            .list()
            .recordFailure()
            .foreach(serverUrl => {
              logger.info(
                s"In ServerURLPopup.render, mounted=$mounted got server URLs: $serverUrl"
              )
              if (mounted) {
                scope.withEffectsImpure
                  .modState(s => s.copy(serverUrl = Some(serverUrl(0))))
              }
            })
          Some(
            <.li("Waiting for response from server")
          )
        } else {
          logger.info(
            s"In ServerURLPopup.render, displaying server URLs: ${state.serverUrl}"
          )
          Some(
            if (
              state.serverUrl.isEmpty || state.serverUrl.get.serverUrl.isEmpty
            ) {
              <.li("No network interfaces found")
            } else {
              state.serverUrl.get.serverUrl.map { url => <.li(url) }.toTagMod
            }
          )
        }
        item.map { i =>
          <.div(
            <.h1("Server URL"),
            <.ul(i)
          )
        }
      } else {
        None
      }
      PopupOkCancel(content, ok, cancel, Some("ServerURLPopupDiv"))
    }

    private var mounted = false

    val didMount: Callback = Callback {
      mounted = true
      addListener(listener)
    }

    val willUnmount: Callback = Callback {
      mounted = false
      removeListener(listener)
    }

    val listener: Listener = new Listener {
      def showChanged(f: Boolean): Unit = {
        logger.info("In showChanged listener: Forcing ServerURLPopup update")
        scope.withEffectsImpure.forceUpdate
      }

    }

  }

  private[pages] val component = ScalaComponent
    .builder[Props]("ServerURLPopup")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build
}
