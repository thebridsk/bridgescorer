package com.github.thebridsk.bridge.clientapi.routes

import AppRouter.AppPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import scala.scalajs.js
import japgolly.scalajs.react.extra.router.Resolution
import com.github.thebridsk.bridge.clientcommon.logger.Init
import com.github.thebridsk.bridge.clientcommon.debug.DebugLoggerComponent
import japgolly.scalajs.react.vdom.VdomElement
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientapi.pages.ServerURLPopup

/**
 * @author werewolf
 */
object Navigator {

  case class Props( selectedPage: Resolution[AppPage], ctrl: RouterCtl[AppPage] )

  case class State()

  class Backend( me: BackendScope[Props, State]) {
    def render(props: Props, state: State) = {
      val p = props.selectedPage.render()

      <.div(
        p,
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("Navigator")
        .initialStateFromProps( props => new State )
        .backend( backendScope => new Backend(backendScope))
        .renderBackend
        .build

  def apply( selectedPage: Resolution[AppPage], ctrl: RouterCtl[AppPage]): VdomElement = {
    component( Props(selectedPage, ctrl))
  }

}
