package com.github.thebridsk.bridge.client.routes

import AppRouter.AppPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.router.Resolution
import japgolly.scalajs.react.vdom.VdomElement

/**
  * @author werewolf
  */
object Navigator {

  case class Props(
      selectedPage: Resolution[AppPage],
      ctrl: RouterCtl[AppPage],
      modules: List[Module]
  )

  case class State()

  class Backend(me: BackendScope[Props, State]) {
    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      <.div(
        props.selectedPage.render()
      )
    }
  }

  private val component = ScalaComponent
    .builder[Props]("Navigator")
    .initialStateFromProps(props => new State)
    .backend(backendScope => new Backend(backendScope))
    .renderBackend
    .build

  def apply(
      selectedPage: Resolution[AppPage],
      ctrl: RouterCtl[AppPage],
      modules: List[Module]
  ): VdomElement = {
    component(Props(selectedPage, ctrl, modules))
  }

}
