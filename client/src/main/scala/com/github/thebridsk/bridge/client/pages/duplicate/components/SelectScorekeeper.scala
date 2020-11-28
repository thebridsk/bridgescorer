package com.github.thebridsk.bridge.client.pages.duplicate.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.client.components.SelectionButtons

/**
  * Select the scorekeeper
  *
  * Props:
  *   teams - a list of two team objects.  Each team object has a list of two names and a list of two positions
  *   selectedName - the selected name
  *   setScoreKeeper - function called when a name is selected
  *   selectedPosition - the selected position
  *   setScoreKeeperPosition - function called when position is selected
  */
object SelectScorekeeper {

  case class TeamData(
    names: List[String],
    positions: List[PlayerPosition]
  ) {
    def sorted: TeamData = {
      TeamData(names.sorted, positions)
    }

    def contains(name: String) = names.contains(name)

    def contains(position: PlayerPosition) = positions.contains(position)
  }

  case class Props(
    teams: List[TeamData],     // two teams
    selectedName: Option[String],
    setScoreKeeper: String => Callback,
    selectedPosition: Option[PlayerPosition],
    setScoreKeeperPosition: PlayerPosition => Callback
  )

  def apply(
    teams: List[TeamData],     // two teams
    selectedName: Option[String],
    setScoreKeeper: String => Callback,
    selectedPosition: Option[PlayerPosition],
    setScoreKeeperPosition: PlayerPosition => Callback
  ) =
    Internal.component(
      Props(teams, selectedName, setScoreKeeper, selectedPosition, setScoreKeeperPosition)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State()

    case class PlayerName(
      team: Int,     // index in Props.teams
      label: String,
      id: String,
      disabled: Boolean
    ) extends SelectionButtons.ButtonData {
      def this(team: Int, name: String, disabled: Boolean = false) =
        this(team, name, s"P_${name}", disabled)
    }

    object PlayerName {
      def apply(team: Int, name: String, disabled: Boolean = false) =
        new PlayerName(team, name, disabled)
    }

    case class PlayerPos(
      team: Int,     // index in Props.teams
      position: PlayerPosition,
      label: String,
      id: String,
      disabled: Boolean
    ) extends SelectionButtons.ButtonData {
      def this(team: Int, position: PlayerPosition, disabled: Boolean = false) =
        this(team, position, position.name, s"SK_${position.pos}", disabled)
    }

    object PlayerPos {
      def apply(team: Int, position: PlayerPosition, disabled: Boolean = false) =
        new PlayerPos(team, position, disabled)
    }

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      def setScoreKeeperName(player: PlayerName) = scope.props >>= { props =>
        props.setScoreKeeper(player.label)
      }

      def setScoreKeeperPos(player: PlayerPos) = scope.props >>= { props =>
        props.setScoreKeeperPosition(player.position)
      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        // -1 for not selected, 0,1 the selected team
        // teamForPos indicates a name was selected, this limits options for position
        val teamForPos = props.selectedName.flatMap { selected =>
          props.teams.zipWithIndex.find { case (team, index) =>
            team.contains(selected)
          }.map(_._2)
        }.getOrElse(-1)
        // teamForName indicates a position was selected, this limits options for name
        val teamForName = if (teamForPos == -1) {
          props.selectedPosition.flatMap { selected =>
            props.teams.zipWithIndex.find { case (team, index) =>
              team.contains(selected)
            }.map(_._2)
          }.getOrElse(-1)
        } else {
          -1  // if position is limited, don't limit name.
        }

        def isNameDisabled( team: Int ) =
          if (teamForName < 0) false
          else teamForName != team

        def isPosDisabled( team: Int ) =
          if (teamForPos < 0) false
          else teamForPos != team

        val (team0, team1) = props.teams match {
          case List(t1, t2) => (t1, t2)
        }

        val players =
          PlayerName(0, team0.names.head, isNameDisabled(0)) ::
          PlayerName(0, team0.names.tail.head, isNameDisabled(0)) ::
          PlayerName(1, team1.names.head, isNameDisabled(1)) ::
          PlayerName(1, team1.names.tail.head, isNameDisabled(1)) ::
          Nil

        val positions =
          PlayerPos(0, team0.positions.head, isPosDisabled(0)) ::
          PlayerPos(0, team0.positions.tail.head, isPosDisabled(0)) ::
          PlayerPos(1, team1.positions.head, isPosDisabled(1)) ::
          PlayerPos(1, team1.positions.tail.head, isPosDisabled(1)) ::
          Nil

        <.div(
          <.h1("Select scorekeeper:"),
          SelectionButtons(
            onChange = setScoreKeeperName,
            selected = props.selectedName.flatMap { sel =>
              players.find(_.label == sel)
            },
            players
          ),
          <.h1("Select scorekeeper's position:"),
          SelectionButtons(
            onChange = setScoreKeeperPos,
            selected = props.selectedPosition.flatMap { sel =>
              positions.find(_.position == sel)
            },
            positions
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

    private[SelectScorekeeper] val component = ScalaComponent
      .builder[Props]("SelectScorekeeper")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
