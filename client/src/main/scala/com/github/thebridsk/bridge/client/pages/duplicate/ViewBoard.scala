package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.bridge.TeamBoardScore
import com.github.thebridsk.bridge.data.bridge.BoardScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseBoardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.pages.hand.Picture
import _root_.com.github.thebridsk.bridge.data.DuplicatePicture
import com.github.thebridsk.materialui.icons.Photo
import com.github.thebridsk.bridge.client.pages.hand.HandStyles.handStyles
import com.github.thebridsk.bridge.data.Board

/**
  * Shows the board results
  *
  * To use, just code the following:
  *
  * <pre><code>
  * ViewBoard( routerCtl: BridgeRouter[DuplicatePage], score: MatchDuplicateScore, board: Board.Id )
  * </code></pre>
  *
  * @author werewolf
  */
object ViewBoard {
  import ViewBoardInternal._

  case class Props(
      routerCtl: BridgeRouter[DuplicatePage],
      page: BaseBoardView,
      score: MatchDuplicateScore,
      board: Board.Id,
      useIMPs: Boolean = false,
      pictures: List[DuplicatePicture] = List()
  )

  def apply(
      routerCtl: BridgeRouter[DuplicatePage],
      page: BaseBoardView,
      score: MatchDuplicateScore,
      board: Board.Id,
      useIMPs: Boolean = true,
      pictures: List[DuplicatePicture] = List()
  ) =
    component(
      Props(routerCtl, page, score, board, useIMPs, pictures)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object ViewBoardInternal {
  import ViewBoard._
  import DuplicateStyles._

  val logger: Logger = Logger("bridge.ViewBoard")

  private[duplicate] val Header = ScalaComponent
    .builder[(Props, Option[BoardScore])]("ViewBoard.Header")
    .render_P(cprops => {
      val (props, board) = cprops
      <.thead(
        <.tr(
          <.th(^.rowSpan := 2, "NS pair"),
          <.th(^.rowSpan := 2, "Contract"),
          <.th(^.rowSpan := 2, "By"),
          <.th(^.rowSpan := 2, "Made"),
          <.th(^.rowSpan := 2, "Down"),
          <.th(^.rowSpan := 1, ^.colSpan := 2, "Score"),
          <.th(^.rowSpan := 2, "EW pair"),
          <.th(^.rowSpan := 2, (if (props.useIMPs) "IMPs" else "Match Points")),
          <.th(^.rowSpan := 2, "Picture")
        ),
        <.tr(
          <.th(^.rowSpan := 1, "NS"),
          <.th(^.rowSpan := 1, "EW")
        )
      )

    })
    .build

  private[duplicate] val TeamRow = ScalaComponent
    .builder[(Team, Option[BoardScore], Props, Backend)]("ViewBoard.TeamRow")
    .render_P(props => {
      val (team, boardscore, p, backend) = props
      logger.fine(
        s"ViewBoard Team ${team} boardscore ${boardscore} page ${p.page}"
      )
      def teamButton(tbs: TeamBoardScore): TagMod = {
        logger.fine(s"ViewBoard.teamButton tbs ${tbs}")
        if (tbs.hidden) {
          <.span(tbs.teamId.toNumber)
        } else {
          val enabled =
            if (boardscore.map(b => b.allplayed).getOrElse(false)) {
              true
            } else {
              p.score.perspective match {
                case PerspectiveTable(team1, team2) =>
                  if (team.id == team1 || team.id == team2) {
                    boardscore
                      .flatMap(bs => bs.findOpponent(team.id))
                      .map { opp =>
                        if (team.id == team1) opp == team2
                        else opp == team1
                      }
                      .getOrElse(false)
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
            AppButton(
              "Hand_" + tbs.teamId.id,
              tbsteamId.toNumber,
              p.routerCtl.setOnClick(clickPage)
            )
          } else {
            <.span(tbsteamId.toNumber)
          }
        }
      }
      val md = p.score

      val numberTeams = md.teams.length

      md.boards.get(p.board) match {
        case Some(board) =>
          board.scores(false /*numberTeams!=4*/ ).get(team.id) match {
            case Some(tbs) =>
              val hidden = tbs.played && tbs.hidden
              def show(s: String) =
                if (hidden) Strings.checkmark else s // check mark if hidden
              def showPoints(v: Either[String, Double]) =
                if (hidden) Strings.checkmark // check mark
                else
                  v match {
                    case Left(s)  => s
                    case Right(d) => Utils.toPointsString(d)
                  }
              if (tbs.isNS) {
                <.tr(
                  <.td(teamButton(tbs)),
                  <.td(if (hidden) "played" else tbs.showContract),
                  <.td(show(tbs.showDeclarer)),
                  <.td(show(tbs.showMade)),
                  <.td(show(tbs.showDown)),
                  <.td(show(tbs.showScore)),
                  <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                  <.td(tbs.opponent match {
                    case Some(t) => t.toNumber
                    case None    => "?"
                  }),
                  <.td(
                    (if (p.useIMPs) show(tbs.showImps)
                     else showPoints(tbs.getPoints))
                  ),
                  <.td(
                    ^.textAlign := "center",
                    if (hidden) {
                      Strings.checkmark
                    } else {
                      p.pictures
                        .find(dp =>
                          dp.boardId == board.id && dp.handId == tbs.teamId
                        )
                        .map { dp =>
                          <.button(
                            ^.`type` := "button",
                            handStyles.footerButton,
                            ^.onClick --> backend.doShowPicture(dp.url),
                            ^.id := "ShowPicture_" + tbs.teamId.id,
                            Photo()
                          )
                        }
                    }
                  )
                )
              } else {
                <.tr(
                  <.td(team.id.toNumber),
                  <.td(
                    dupStyles.tableCellGray,
                    ^.textAlign := "center",
                    if (hidden) "played" else ""
                  ),
                  <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                  <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                  <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                  <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                  <.td(show(tbs.showScore)),
                  <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                  <.td(
                    (if (p.useIMPs) show(tbs.showImps)
                     else showPoints(tbs.getPoints))
                  ),
                  <.td(dupStyles.tableCellGray, ^.textAlign := "center", "")
                )
              }
            case None =>
              <.tr(
                <.td(team.id.toNumber),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", ""),
                <.td(dupStyles.tableCellGray, ^.textAlign := "center", "")
              )
          }
        case None =>
          <.div()
      }

    })
    .build

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      val showPicture: Option[String] = None
  )

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    val popupOk: Callback = scope.modState(s => s.copy(showPicture = None))

    def doShowPicture(url: String): Callback =
      scope.modState(s => s.copy(showPicture = Some(url)))

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      val board = props.score.boards.get(props.board)

      <.div(
        PopupOkCancel(
          content = state.showPicture.map(p => Picture(None, Some(p))),
          ok = Some(popupOk)
        ),
        <.div(
          dupStyles.divBoardView,
          <.table(
            ^.id := s"Board_${props.board.id}",
            <.caption(
              <.span(
                ^.float := "left",
                "Board " + props.board.toNumber
              ),
              <.span(
                ^.float := "right",
                board match {
                  case Some(b) => b.showVul
                  case None    => ""
                }
              )
            ),
            Header((props, board)),
            <.tbody(
              props.score.teams.toList
                .sortWith((t1, t2) => t1.id < t2.id)
                .map { team =>
                  TeamRow.withKey(team.id.id)((team, board, props, this))
                }
                .toTagMod
            )
          )
        )
      )
    }

  }

  private[duplicate] val component = ScalaComponent
    .builder[Props]("ViewBoard")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .build
}
