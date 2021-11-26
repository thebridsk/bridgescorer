package com.github.thebridsk.bridge.client.pages.individual.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.TableView
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.IndividualHandInTable
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles._
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.utilities.logging.Logger

/**
  * React component that shows a bridge table card.
  *
  * the table's rows represent the rounds, for each round the players and boards are identified.
  *
  * The bridge table card can be shown in one of two ways,
  * - without an active match, this shows the movements
  * - with an active match, this has buttons to go to the
  *   scoreboard for a round or a board for a round.
  *
  * To use, just code the following:
  *
  * {{{
  * val page = TableView(dupid, tableid)
  *
  * ViewTable(
  *   page
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object ViewTable {
  import Internal._

  case class PropsMatch(
    duplicate: IndividualDuplicateScore,
    tableView: TableView
  )

  case class Props(
    router: BridgeRouter[IndividualDuplicatePage],
    table: Table.Id,
    movement: IndividualMovement,
    matchProps: Option[PropsMatch]
  ) {

  }

  /**
    * Instantiate the react component
    *
    * Shows the bridge table card without a match.
    *
    * @param router
    * @param table
    * @param movement
    *
    * @return the unmounted react component
    *
    * @see [[ViewTable$]] for usage.
    */
  def apply(
    router: BridgeRouter[IndividualDuplicatePage],
    table: Table.Id,
    movement: IndividualMovement,
  ) = component(  // scalafix:ok ExplicitResultTypes; ReactComponent
    Props(
      router,
      table,
      movement,
      None
    )
  )

  /**
    * Instantiate the react component
    *
    * Shows the bridge table card with a match.
    *
    * @param router
    * @param table
    * @param movement
    * @param duplicate the match
    * @param tableView
    *
    * @return the unmounted react component
    *
    * @see [[ViewTable$]] for usage.
    */
  def apply(
    router: BridgeRouter[IndividualDuplicatePage],
    table: Table.Id,
    movement: IndividualMovement,
    duplicate: IndividualDuplicateScore,
    tableView: TableView
  ) = component(  // scalafix:ok ExplicitResultTypes; ReactComponent
    Props(
      router,
      table,
      movement,
      Some(PropsMatch(duplicate, tableView))
    )
  )

  protected object Internal {
    private val log = Logger("bridge.ViewTable")

    case class State()

    /**
      * The header for the bridge table card
      *
      * @param relay true if there is a relay in the movements
      *
      * @return the thead element
      */
    def Header(
      relay: Boolean
    ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
      <.thead(
        <.tr(
          <.th("Round"),
          <.th("North"),
          <.th("South"),
          <.th("East"),
          <.th("West"),
          <.th("Boards"),
          relay ?= <.th("Relay")
        )
      )
    }

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val table = props.table.toInt

        val activeRound = props.matchProps.map { mp =>
          mp.duplicate.duplicate.activeRound
        }

        def getName(p: Int, players: List[String]) = {
          if (0 < p && p <= players.length) {
            s" ${players(p-1)}"
          } else {
            ""
          }
        }

        /**
          * A row, this represents a round in the match.
          *
          * @param h
          * @param players
          * @param relay if None then no relay column is added.
          *              If Some(true) then "relay" is added in relay column
          *              If Some(false) then "" is added in relay column
          *
          * @return the tr element
          */
        def Row(
          h: IndividualHandInTable,
          players: List[String],
          relay: Option[Boolean]
        ) = { // scalafix:ok ExplicitResultTypes; ReactComponent

          val round = h.round

          val allUnplayed = {
            props.matchProps.map { mp =>
              val hands = mp
                .duplicate
                .duplicate
                .getHandsInRound(props.table, round)
              val p = hands
                .find(_.played.isDefined)
                .isEmpty
              log.fine(s"allUnplayed=${p}, activeRound=${activeRound} hands=${hands.mkString(", ")}")
              p
            }.getOrElse(false)
          }

          // props.matchProps must be defined
          def clickFromRound() = {
            val next = if (allUnplayed) {
              props.matchProps.get.tableView.toTableNamesView(round)
            } else {
              props.matchProps.get.tableView.toRoundView(round)
            }
            next
          }

          def roundCell(): TagMod = {
            <.td(
              activeRound.map { ar =>
                AppButton(
                  s"Round_${round}",
                  round.toString,
                  ^.disabled := round > ar,
                  round == ar ?= baseStyles.requiredNotNext,
                  props.router.setOnClick(clickFromRound())
                ).toTagMod
              }.getOrElse {
                TagMod(s"${round}")
              }
            )
          }

          <.tr(
            roundCell(),
            <.td(s"${h.north}${getName(h.north, players)}"),
            <.td(s"${h.south}${getName(h.south, players)}"),
            <.td(s"${h.east}${getName(h.east, players)}"),
            <.td(s"${h.west}${getName(h.west, players)}"),
            <.td(
              props.matchProps.map { mp =>
                val currentRound = activeRound.get
                h.boards.map(IndividualBoard.id(_))
                  .sorted
                  .map { board =>
                    val (clickFromBoard, isPlayed) =
                      if (allUnplayed) {
                        (mp.tableView.toTableNamesView(h.round, board), false)
                      } else {
                        mp
                          .duplicate
                          .duplicate
                          .getHand(mp.tableView.tableid, round, board)
                          .filter(_.played.isDefined)
                          .map { hand =>
                            (
                              mp.tableView
                                .toBoardView(round, board)
                                .toHandView(hand.id)
                              , true
                            )
                          }.getOrElse {
                            (mp.tableView.toBoardView(round, board), false)
                          }
                      }

                    AppButton(
                      s"Board_${board.id}",
                      board.toNumber,
                      ^.disabled := round > currentRound,
                      (round == currentRound && !isPlayed) ?= baseStyles.requiredNotNext,
                      props.router.setOnClick(clickFromBoard)
                    )

                  }
                  .toTagMod
              }.getOrElse {
                h.boards.mkString(", ")
              }
            ),
            relay.whenDefined { r =>
              <.td(
                if (r) "relay"
                else ""
              )
            }
          )
        }

        val matchRelay = props.movement.matchHasRelay
        val players = props.matchProps.map { mp =>
          mp.duplicate.duplicate.players
        }.getOrElse(List())
        <.table(
          dupStyles.viewTable,
          <.caption( s"Table ${table}"),
          Header(matchRelay),
          <.tbody(
            props.movement
                .hands
                .filter(_.table == table)
                .sortWith((l,r) => l.round < r.round)
                .map { h =>
                  val roundRelay = if (matchRelay) {
                    props
                      .movement
                      .tableRoundRelay(h.table, h.round)
                      .headOption
                      .map( _ => true)
                      .orElse(Some(false))
                  } else {
                    None
                  }
                  Row(h, players, roundRelay)
                }.toTagMod
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
        if (prevProps.table != props.table
            || prevProps.movement != props.movement
            || prevProps.matchProps != props.matchProps
        ) {
          // props have change, reinitialize state
          cdu.forceUpdate
        }
      }

    val component = ScalaComponent
      .builder[Props]("ViewTable")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
