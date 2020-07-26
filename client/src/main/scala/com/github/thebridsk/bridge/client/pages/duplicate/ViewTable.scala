package com.github.thebridsk.bridge.client.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.data.bridge.DuplicateViewPerspective
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore.Round
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.data.Team

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewTable( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewTable {
  import ViewTableInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: TableView, showTableButton: Boolean )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: TableView, showTableButton: Boolean = false ) = component(Props(routerCtl,page,showTableButton))

}

object ViewTableInternal {
  import ViewTable._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewTable")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  val Header = ScalaComponent.builder[(Props,Boolean)]("ComponentBoard.Header")
                      .render_P { args =>
                        val (props, relay) = args
                        <.thead(
                          <.tr(
                            <.th( "Round" ),
                            <.th( "NS" ),
                            <.th( "EW" ),
                            <.th( "Boards" ),
                            relay ?= <.th( "Relay" )
                          )
                        )

                      }.build

  val RoundRow = ScalaComponent.builder[(Round,MatchDuplicateScore,Props,Int,Boolean)]("ComponentBoard.TeamRow")
                      .render_P( cprops => {
                        val (round,score,props,currentRound,relay) = cprops
                        val allUnplayed = round.allUnplayedOnTable

                        def showTeamOld( teamId: Team.Id, p1: String, p2: String ) = {
                          <.span( teamId.toNumber, <.br, p1, " ", p2 )
                        }
                        def showTeam( teamId: Team.Id, p1: String, p2: String ) = {
                          <.table(
                            <.tbody(
                              <.tr(
                                <.td( teamId.toNumber ),
                                (p1.length>0 || p2.length>0) ?= <.td( p1, <.br, p2 )
                              )
                            )
                          )
                        }

                        val clickFromRound = if (allUnplayed) {
                                               props.page.toTableTeamView(round.round)
                                             } else {
                                               props.page.toRoundView(round.round)
                                             }

                        <.tr( ^.textAlign:="center",
                          <.td(
                            AppButton( "Round_"+round.round.toString, round.round.toString,
                                       dupStyles.boardButtonInTable,
                                       ^.disabled:=round.round>currentRound,
                                       round.round==currentRound ?= baseStyles.requiredNotNext,
                                       props.routerCtl.setOnClick(clickFromRound)
                                     )
                          ),
                          <.td( showTeam(round.ns.id, round.ns.player1, round.ns.player2 ) ),
                          <.td( showTeam(round.ew.id, round.ew.player1, round.ew.player2 ) ),
                          <.td(
                            round.boards.sortWith((b1,b2)=> b1.id < b2.id).map { board =>
                              val clickFromBoard =
                                if (allUnplayed) {
                                  props.page.toTableTeamView(round.round,board.id)
                                } else {
                                  if (board.hasTeamPlayed(round.ns.id)) {
                                    props.page.toBoardView(round.round,board.id)
                                  } else {
                                    props.page.toBoardView(round.round,board.id).toHandView(round.ns.id)
                                  }
                                }

                              <.span(
                                <.span( ^.dangerouslySetInnerHtml:="&nbsp;&nbsp;"),
                                AppButton( s"Board_${board.id.id}", board.id.toNumber,
                                           dupStyles.boardButtonInTable,
                                           ^.disabled:=round.round>currentRound,
                                           (round.round==currentRound && !board.hasTeamPlayed(round.ns.id)) ?= baseStyles.requiredNotNext,
                                           props.routerCtl.setOnClick(clickFromBoard)
                                         )
                              )

                            }.toTagMod,
                            <.span( ^.dangerouslySetInnerHtml:="&nbsp;&nbsp;")
                          ),
                          if (relay) {
                            val rrelay = score.tableRoundRelay(round.table, round.round)
                            <.td(
                              rrelay.map( id => id.toNumber).mkString(", ")
                            )
                          } else {
                            TagMod()
                          }
                        )
                      }).build


  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      DuplicateStore.getCompleteView() match {
        case Some(score) =>
          val relay = score.matchHasRelay
          score.tables.get(props.page.tableid) match {
            case Some(rounds) =>
              <.div(
                dupStyles.divTableView,
                <.table(
                  <.caption(
                    if (props.showTableButton) {
                      val table = props.page.tableid.toNumber
                      AppButton( "Table_"+table, "Table "+table, props.routerCtl.setOnClick(props.page) )
                    } else {
                      "Table "+props.page.tableid.toNumber
                    }
                  ),
                  Header((props,relay)),
                  <.tbody(
                      {
                        var lastRoundAllPlay = 0
                        rounds.sortWith((r1,r2)=>r1.round<r2.round).map { round =>
                          if (round.complete) lastRoundAllPlay = round.round
                          RoundRow.withKey( "Round_"+round.round )((round,score,props,lastRoundAllPlay+1,relay))
                        }.toTagMod
                      }
                  )
                )
              )
            case None =>
              <.div( dupStyles.divTableView, <.p("Table "+props.page.tableid.toNumber+" not found") )
          }
        case None =>
          <.div( dupStyles.divTableView, <.p("Waiting to load information") )
      }
    }

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("ViewTable.didMount")
      DuplicateStore.addChangeListener(storeCallback)
      Controller.monitor(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("ViewTable.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
    }
  }

  val component = ScalaComponent.builder[Props]("ViewTable")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

