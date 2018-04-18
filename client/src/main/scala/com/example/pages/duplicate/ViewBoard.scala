package com.example.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.data.bridge.DuplicateViewPerspective
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.Team
import com.example.bridge.store.DuplicateStore
import com.example.data.bridge.TeamBoardScore
import com.example.data.bridge.BoardScore
import com.example.data.bridge.PerspectiveTable
import com.example.data.bridge.PerspectiveComplete
import com.example.data.bridge.PerspectiveDirector
import com.example.data.util.Strings
import com.example.pages.duplicate.DuplicateRouter.BaseBoardView
import com.example.react.AppButton
import com.example.react.Utils._
import com.example.routes.BridgeRouter

/**
 * Shows the board results
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewBoard( routerCtl: RouterCtl[DuplicatePage], score: MatchDuplicateScore, board: Id.DuplicateBoard )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewBoard {
  import ViewBoardInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardView, score: MatchDuplicateScore, board: Id.DuplicateBoard, useIMPs: Boolean = false )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardView, score: MatchDuplicateScore, board: Id.DuplicateBoard, useIMPs: Boolean = true ) = component(Props(routerCtl, page, score, board, useIMPs))

}

object ViewBoardInternal {
  import ViewBoard._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewBoard")

  val Header = ScalaComponent.builder[(Props,Option[BoardScore])]("ViewBoard.Header")
                      .render_P( cprops => {
                        val (props,board) = cprops
                        <.thead(
                          <.tr(
                            <.th( ^.rowSpan:=2, "NS pair"),
                            <.th( ^.rowSpan:=2, "Contract"),
                            <.th( ^.rowSpan:=2, "By"),
                            <.th( ^.rowSpan:=2, "Made"),
                            <.th( ^.rowSpan:=2, "Down"),
                            <.th( ^.rowSpan:=1, ^.colSpan:=2, "Score"),
                            <.th( ^.rowSpan:=2, "EW pair"),
                            <.th( ^.rowSpan:=2, (if (props.useIMPs) "IMPs" else "Match Points") )
                          ),
                          <.tr(
                            <.th( ^.rowSpan:=1, "NS"),
                            <.th( ^.rowSpan:=1, "EW")
                          )
                        )

                      }).build

  val TeamRow = ScalaComponent.builder[(Team,Option[BoardScore],Props)]("ViewBoard.TeamRow")
                      .render_P( props => {
                        val (team, boardscore, p) = props
                        logger.fine(s"ViewBoard Team ${team} boardscore ${boardscore} page ${p.page}")
                        def teamButton( tbs: TeamBoardScore ): TagMod = {
                          logger.fine(s"ViewBoard.teamButton tbs ${tbs}")
                          if (tbs.hidden) {
                            <.span(Id.teamIdToTeamNumber(tbs.teamId))
                          } else {
                            val enabled =
                              if (boardscore.map(b=>b.allplayed).getOrElse(false)) {
                                true
                              } else {
                                p.score.perspective match {
                                  case PerspectiveTable(team1, team2) =>
                                    if (team.id == team1 || team.id == team2) {
                                      boardscore.flatMap( bs => bs.findOpponent(team.id) ).map{ opp =>
                                        if (team.id == team1) opp==team2
                                        else opp==team1
                                      }.getOrElse(false)
                                    } else {
                                      false
                                    }

                                  case PerspectiveComplete => false
                                  case PerspectiveDirector => true
                                }
                              }
                            val tbsteamId = tbs.teamId
                            if (enabled) {
                              val clickPage = p.page.toHandView(tbsteamId)
                              AppButton( "Hand_"+tbs.teamId, Id.teamIdToTeamNumber(tbsteamId),
                                         p.routerCtl.setOnClick(clickPage) )
                            } else {
                              <.span(Id.teamIdToTeamNumber(tbsteamId))
                            }
                          }
                        }
                        val md = p.score

                        val numberTeams = md.teams.length

                        md.boards.get(p.board) match {
                          case Some(board) =>
                            board.scores(false /*numberTeams!=4*/).get(team.id) match {
                              case Some(tbs) =>
                                val hidden = tbs.played&&tbs.hidden
                                def show( s: String ) = if (hidden) Strings.checkmark else s                 // check mark if hidden
                                def showPoints( v: Either[String,Double] ) = if (hidden) Strings.checkmark   // check mark
                                                                             else v match {
                                                                               case Left(s) => s
                                                                               case Right(d) => Utils.toPointsString(d)
                                                                             }
                                if (tbs.isNS) {
                                  <.tr(
                                    <.td( teamButton(tbs)),
                                    <.td( if (hidden) "played" else tbs.showContract),
                                    <.td( show(tbs.showDeclarer)),
                                    <.td( show(tbs.showMade)),
                                    <.td( show(tbs.showDown)),
                                    <.td( show(tbs.showScore)),
                                    <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                    <.td( tbs.opponent match {
                                      case Some(t) => Id.teamIdToTeamNumber(t)
                                      case None => "?"
                                    } ),
                                    <.td( (if (p.useIMPs) show(tbs.showImps) else  showPoints(tbs.getPoints) ))
                                  )
                                } else {
                                  <.tr(
                                    <.td( Id.teamIdToTeamNumber(team.id)),
                                    <.td( dupStyles.tableCellGray, ^.textAlign := "center", if (hidden) "played" else ""),
                                    <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                    <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                    <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                    <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                    <.td( show(tbs.showScore)),
                                    <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                    <.td( (if (p.useIMPs) show(tbs.showImps) else  showPoints(tbs.getPoints) ))
                                  )
                                }
                              case None =>
                                <.tr(
                                  <.td( Id.teamIdToTeamNumber(team.id)),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", ""),
                                  <.td( dupStyles.tableCellGray, ^.textAlign := "center", "")
                                )
                            }
                          case None =>
                            <.div()
                        }

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
    def render( props: Props, state: State ) = {
      val board = props.score.boards.get(props.board)
      <.div(
        dupStyles.divBoardView,
        <.table(
          ^.id:="Board_"+props.board,
          <.caption(
            <.span(
              ^.float:="left",
              "Board "+Id.boardIdToBoardNumber(props.board)
            ),
            <.span(
              ^.float:="right",
              board match {
                case Some(b) => b.showVul
                case None => ""
              }
            )
          ),
          Header((props,board)),
          <.tbody(
            props.score.teams.toList.sortWith( (t1,t2)=>Id.idComparer(t1.id,t2.id)<0).map { team =>
              TeamRow.withKey( team.id )((team,board,props))
            }.toTagMod
          )
        )
      )
    }

  }

  val component = ScalaComponent.builder[Props]("ViewBoard")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

