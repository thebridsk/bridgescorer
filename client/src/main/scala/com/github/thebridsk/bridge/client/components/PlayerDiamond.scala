package com.github.thebridsk.bridge.client.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles.baseStyles2

/**
  * Displays 4 players in a diamond.
  *
  * To use, just code the following:
  *
  * {{{
  * PlayerDiamond( List(top, left, right, bottom) )
  * PlayerDiamond( top, left, right, bottom )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PlayerDiamond {
  import Internal._

  case class Props(
    players: List[TagMod]
  )

  /**
    *
    *
    * @return the unmounted react component
    *
    * @see [[PlayerDiamond$]] for usage.
    */
  def apply(players: List[TagMod]) =
    component(Props(players)) // scalafix:ok ExplicitResultTypes; ReactComponent

  def apply(top: TagMod, left: TagMod, right: TagMod, bottom: TagMod) =
    component(Props(List(top, left, right, bottom)))

  protected object Internal {

    case class State()

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          baseStyles2.viewPlayerDiamond,
          TagMod(
            props.players:_*
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

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps != props) {
          // props have change, reinitialize state
          cdu.setState(State())
        }
      }

    val component = ScalaComponent
      .builder[Props]("PlayerDiamond")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
