package com.github.thebridsk.bridge.client.pages.duplicate.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.West
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.client.components.EnterName
import com.github.thebridsk.bridge.client.components.SelectionButtons

/**
  *
  */
object EnterScorekeeper {

  case class Props(
    name: String,
    setScoreKeeper: String => Callback,
    tabIndex: Int,
    selected: Option[PlayerPosition],
    setScoreKeeperPosition: PlayerPosition => Callback
  )

  def apply(
    name: String,
    setScoreKeeper: String => Callback,
    tabIndex: Int,
    selected: Option[PlayerPosition],
    setScoreKeeperPosition: PlayerPosition => Callback
  ) =
    Internal.component(
      Props(name,setScoreKeeper,tabIndex,selected,setScoreKeeperPosition)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State()

    case class Player(
      position: PlayerPosition,
      label: String,
      id: String,
      disabled: Boolean
    ) extends SelectionButtons.ButtonData {
      def this( position: PlayerPosition, disabled: Boolean = false) = this(position, position.name, s"SK_${position.pos}", disabled)
    }

    object Player {
      def apply(position: PlayerPosition, disabled: Boolean = false) = new Player(position, disabled)
    }

    val players = List(
      Player(North),
      Player(South),
      Player(East),
      Player(West)
    )

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      def setScoreKeeper(player: Player) = scope.props >>= { props =>
        props.setScoreKeeperPosition(player.position)
      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          <.h1("Enter scorekeeper:"),
          EnterName(
            id = "Scorekeeper",
            name = props.name,
            tabIndex = props.tabIndex,
            onChange = props.setScoreKeeper
          ),
          <.h1("Select scorekeeper's position:"),
          SelectionButtons(
            onChange = setScoreKeeper,
            selected = props.selected.flatMap { sel =>
              players.find(_.position == sel)
            },
            players
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

    private[EnterScorekeeper] val component = ScalaComponent
      .builder[Props]("EnterScorekeeper")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
