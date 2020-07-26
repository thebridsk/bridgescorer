package com.github.thebridsk.bridge.client.pages.duplicate


import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.BoardScore
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseScoreboardViewWithPerspective
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableRoundScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.FinishedScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.routes.BridgeRouter


/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewScoreboard( routerCtl: BridgeRouter[DuplicatePage], score: MatchDuplicateScore )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewScoreboard {
  import ViewScoreboardInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: BaseScoreboardViewWithPerspective, score: MatchDuplicateScore, useIMP: Boolean = false )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: BaseScoreboardViewWithPerspective, score: MatchDuplicateScore, useIMP: Boolean = false ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(routerCtl,page,score,useIMP))

}

object ViewScoreboardInternal {
  import ViewScoreboard._
  import DuplicateStyles._

  val logger: Logger = Logger("bridge.ViewScoreboard")

  private[duplicate]
  val Header = ScalaComponent.builder[Props]("ViewScoreboard.Header")
                      .render_P( props => {
                        val (currentRound,currentTable) = props.page match {
                          case TableRoundScoreboardView( dupid, tableid, roundid ) => (roundid,tableid)
                          case _ => (-1,"")
                        }
                        val tablePerspective: Option[PerspectiveTable] = props.page.getPerspective match {
                          case p: PerspectiveTable => Some(p)
                          case _ => None
                        }
                        def isBoardInRound( b: BoardScore ) = {
                          import scala.util.control.Breaks._
                          if (currentRound == -1 || tablePerspective.isEmpty) true
                          else {
                            var result = false
                            val p1 = tablePerspective.get.teamId1
                            val p2 = tablePerspective.get.teamId2
                            breakable {
                              for (h <- b.board.hands) {
                                val t1 = h.nsTeam
                                val t2 = h.ewTeam
                                if ((p1==t1 && p2==t2)
                                    || (p1==t2 && p2==t1)) {
                                  result = h.round==currentRound
                                  break()
                                }
                              }
                            }
                            result
                          }
                        }
                        def wasPlayed( b: BoardScore ) = {
                          import scala.util.control.Breaks._
                          if (currentRound == -1 || tablePerspective.isEmpty) (true,Team.idNul)
                          else {
                            var result = true
                            var ns = Team.idNul
                            val p1 = tablePerspective.get.teamId1
                            val p2 = tablePerspective.get.teamId2
                            breakable {
                              for (h <- b.board.hands) {
                                val t1 = h.nsTeam
                                val t2 = h.ewTeam
                                if ((p1==t1 && p2==t2)
                                    || (p1==t2 && p2==t1)) {
                                  if (h.round==currentRound) {
                                    result = h.wasPlayed
                                    ns = h.nsTeam
                                  }
                                  break()
                                }
                              }
                            }
                            (result,ns)
                          }
                        }
                        def boardButton( b: BoardScore ) = {
                          val id = b.id
                          val (played,ns) = wasPlayed(b)
                          val clickPage = if (played) props.page.toBoardView(id) else props.page.toBoardView(id).toHandView(ns)
                          AppButton( s"Board_${id.id}", id.toNumber,
                                     !played ?= baseStyles.requiredNotNext,
                                     props.routerCtl.setOnClick(clickPage) )
                        }

                        <.thead(
                          <.tr(
                            <.th( ^.rowSpan:=2, "Team"),
                            <.th( ^.rowSpan:=2, "Players"),
                            <.th( ^.rowSpan:=2, "Total"),
                            <.th( ^.colSpan:=props.score.sortedBoards.size, "Boards", props.useIMP?=" (IMPs)")
                          ),
                          <.tr(
                            props.score.sortedBoards.map { b =>
                              if (isBoardInRound(b)) {
                                <.th( dupStyles.cellScoreboardBoardColumn, boardButton(b))
                              } else {
                                <.th( dupStyles.cellScoreboardBoardColumn, b.id.toNumber )
                              }
                            }.toTagMod
                          )
                        )

                      }).build

  private[duplicate]
  val TeamRow = ScalaComponent.builder[(Team,Props)]("ViewScoreboard.TeamRow")
                      .render_P( props => {
                        val (team, p) = props
                        val md = p.score
                        <.tr(
                          <.td( team.id.toNumber ),
                          <.td( team.player1, <.br(), team.player2),
                          <.td( if (p.useIMP) f"${md.teamImps(team.id)}%.1f" else Utils.toPointsString(md.teamScores(team.id))),
                          md.sortedBoards.map { b =>
                            val score = b.scores().get(team.id) match {
                              case Some(ts) =>
                                if (ts.played) {
                                  if (ts.hidden) {
                                    <.span( Strings.checkmark )       // check mark
                                  } else {
                                    if (p.useIMP) {
                                      <.span(ts.showImps)
                                    } else {
                                      <.span(Utils.toPointsString(ts.points))
                                    }
                                  }
                                } else {
                                  <.span("")
                                }
                              case None => <.span(Strings.xmark)
                            }
                            <.td( score )
                          }.toTagMod
                        )

                      }).build

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
    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React
      {
        val names = props.score.teams.flatMap( t => t.player1::t.player2::Nil)
        val boards = props.score.boards.size
        Properties.setViewportContent(boards, names:_*)
      }
      def teamColumn( teams: List[Team] ) = {
        var count = 0
        for (team <- teams.sortWith((t1,t2)=> t1.id < t2.id)) yield {
          count += 1
          <.span( count!=1 ?= <.br(), team.id.toNumber+" "+team.player1+" "+team.player2 )
        }
      }
      val showidbutton = props.page.isInstanceOf[FinishedScoreboardView]
      <.div(
        dupStyles.divViewScoreboard,
        props.page.getPerspective match {
          case PerspectiveDirector => dupStyles.divViewScoreboardAllButtons
          case PerspectiveComplete => dupStyles.divViewScoreboardAllButtons
          case PerspectiveTable(t1, t2) => TagMod()
        },
        <.table( ^.id := "scoreboard",
          dupStyles.tableViewScoreboard,
          <.caption(
            props.page match {
              case FinishedScoreboardView(dupid) => "Scoreboard"
              case _ =>
                props.page.getPerspective match {
                  case PerspectiveDirector => "Scoreboard from Director's view"
                  case PerspectiveComplete => "Scoreboard with completed boards only"
                  case PerspectiveTable(t1, t2) =>
                    val (currentRound,currentTable) = props.page match {
                      case TableRoundScoreboardView( dupid, tableid, roundid ) => (roundid,tableid)
                      case _ => (-1,"")
                    }
                    val (team1,team2) = if (t1<t2) (t1,t2) else (t2,t1)
                    "Scoreboard from table "+currentTable+" round "+currentRound+" for teams "+team1.toNumber+" and "+team2.toNumber
                }
            },
            ", ",
            DateUtils.formatDay(props.score.created),
            <.span(
              ^.float:="right",
              <.span( showidbutton ?= baseStyles.onlyInPrint, props.score.id.id ),
              showidbutton ?= <.span(
                baseStyles.hideInPrint,
                AppButton( "Duplicate_"+props.score.id.id, props.score.id.id,
                           props.routerCtl.setOnClick( CompleteScoreboardView(props.score.id.id) )
                )
              )
            ),
            <.span(
              ^.float:="left",
              <.span( showidbutton ?= baseStyles.onlyInPrint, props.score.id.id ),
              showidbutton ?= <.span(
                baseStyles.hideInPrint,
                AppButton( "Duplicate_"+props.score.id.id, props.score.id.id,
                           props.routerCtl.setOnClick( CompleteScoreboardView(props.score.id.id) )
                )
              )
            )
          ),
          Header(props),
          <.tbody(
            props.score.teams.toList.sortWith( (t1,t2)=>t1.id<t2.id).map { team =>
              TeamRow.withKey( team.id.id )((team,props))
            }.toTagMod
          )
        ),
      )
    }

    val willUnmount: Callback = CallbackTo {
      logger.info("ViewScoreboard.willUnmount")
      Properties.restoreViewportContentWidth()
    }

  }

  private[duplicate]
  val component = ScalaComponent.builder[Props]("ViewScoreboard")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

