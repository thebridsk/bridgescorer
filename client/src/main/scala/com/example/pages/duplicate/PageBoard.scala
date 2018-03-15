package com.example.pages.duplicate


import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.data.bridge.DuplicateViewPerspective
import com.example.bridge.store.DuplicateStore
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete
import com.example.react.Utils._
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.PerspectiveTable
import com.example.data.bridge.BoardScore
import com.example.pages.duplicate.DuplicateRouter.BaseBoardViewWithPerspective
import com.example.pages.duplicate.DuplicateRouter.TableBoardView
import com.example.react.AppButton
import com.example.pages.BaseStyles

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageBoard( routerCtl: RouterCtl[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageBoard {
  import PageBoardInternal._

  case class Props( routerCtl: RouterCtl[DuplicatePage], page: BaseBoardViewWithPerspective )

  def apply( routerCtl: RouterCtl[DuplicatePage], page: BaseBoardViewWithPerspective ) = {
    logger.info(s"PageBoard with page = ${page}")
    component(Props(routerCtl,page))
  }

}

object PageBoardInternal {
  import PageBoard._
  import DuplicateStyles._

  val logger = Logger("bridge.PageBoard")

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
      logger.info("Rendering board "+props.page)
      val perspective = props.page.getPerspective()
      val tableperspective = perspective match {
        case tp: PerspectiveTable => Some(tp)
        case _ => None
      }

      def buttons( label: String, boards: List[BoardScore], ns: Id.Team, played: Boolean ) = {
        val bbs = boards // .filter { board => board.id!=props.page.boardid }
        <.span(
          !bbs.isEmpty ?= <.b(label),
          !bbs.isEmpty ?= bbs.map { board =>
            val selected = board.id == props.page.boardid
            Seq[TagMod](
              <.span(" "),
              AppButton( "Board_"+board.id, "Board "+Id.boardIdToBoardNumber(board.id),
                         BaseStyles.highlight(
                             selected=selected,
                             requiredNotNext = !played && !selected
                         ),
                         if (played) {
                           props.routerCtl.setOnClick(props.page.toBoardView(board.id))
                         } else {
                           props.routerCtl.setOnClick(props.page.toBoardView(board.id).toHandView(ns))
                         }
              )
            ).toTagMod
          }.toTagMod
        )
      }

      def boardsFromTable( mds: MatchDuplicateScore ) = {
        props.page match {
          case tbpage: TableBoardView =>
            mds.getRound(tbpage.tableid, tbpage.round) match {
              case Some(round) =>
                val (played,unplayed) = round.playedAndUnplayedBoards()
                <.span(
                  <.span(^.dangerouslySetInnerHtml:="&nbsp;&nbsp;"),
                  buttons("Played: ", played, round.ns.id, true),
                  <.span(^.dangerouslySetInnerHtml:="&nbsp;&nbsp;"),
                  buttons("Unplayed: ", unplayed, round.ns.id, false)
                )
              case _ =>
                <.span()
            }
          case _ => None
            <.span()
        }
      }

      def boards( mds: MatchDuplicateScore ) = {
        var counter = 0
        def getKey() = {
          counter = counter+1
          "KeyBoardsRow_"+counter
        }
        <.div(
          <.table(
            <.tbody(
              mds.sortedBoards.map { board => board.id }.grouped(6).map { row =>
                BoardsRow.withKey(getKey())((row,props,mds))
              }.toTagMod
            )
          )
        )
      }

      val (tableBoardView,currentRound,currentTable) = props.page match {
        case tbv: TableBoardView => (Some(tbv),tbv.round,tbv.tableid)
        case _ => (None,-1,"")
      }

      def title() = {
        props.page.getPerspective() match {
          case PerspectiveTable(team1, team2) =>
            <.div(
              <.h1(s"Board ${Id.boardIdToBoardNumber(props.page.boardid)} viewed from Table ${Id.tableIdToTableNumber(currentTable)} Round ${currentRound}"),
              <.h2("Teams "+Id.teamIdToTeamNumber(team1)+" and "+Id.teamIdToTeamNumber(team2))
            )
          case PerspectiveDirector =>
            <.h1(s"Director's View of Board ${Id.boardIdToBoardNumber(props.page.boardid)}")
          case PerspectiveComplete =>
            <.h1(s"Completed View of Board ${Id.boardIdToBoardNumber(props.page.boardid)}")
        }

      }


      <.div(
        DuplicateStore.getView( perspective ) match {
          case Some(score) =>
            val allplayedInRound = score.getRound(currentTable, currentRound) match {
              case Some(r) => r.complete
              case _ => false
            }
            <.div(
              dupStyles.divBoardPage,
              title(),
              ViewBoard( props.routerCtl, props.page, score, props.page.boardid, PageScoreboard.useIMPs ),
              <.p,
              <.div(
                baseStyles.fontTextNormal,
                AppButton( "Game", "Scoreboard",
                           allplayedInRound ?= baseStyles.requiredNotNext,
                           props.routerCtl.setOnClick(props.page.toScoreboardView()) ),
                " ",
                tableBoardView.isDefined?= AppButton( "Table", "Table "+Id.tableIdToTableNumber(currentTable),
                                                      allplayedInRound ?= baseStyles.requiredNotNext,
                                                      props.routerCtl.setOnClick(tableBoardView.get.toTableView()) ),
                if (tableperspective.isDefined) boardsFromTable(score)
                else boards(score)
              )
            )
          case None =>
            <.p( "Waiting" )
        }
      )
    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    def didMount() = CallbackTo {
      logger.info("PageBoard.didMount")
      DuplicateStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => CallbackTo(
      Controller.monitorMatchDuplicate(p.page.dupid)
    )}

    def willUnmount() = CallbackTo {
      logger.info("PageBoard.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val BoardsRow = ScalaComponent.builder[(List[Id.DuplicateBoard],Props,MatchDuplicateScore)]("PageBoard.BoardsRow")
                        .render_P( args => {
                          val (row,props,mds) = args
                          <.tr(
                            row.map { id => BoardCell.withKey("KeyBoard_"+id)((id,props,mds.boards(id))) }.toTagMod
                          )
                        }).build

  val BoardCell = ScalaComponent.builder[(Id.DuplicateBoard,Props,BoardScore)]("PageBoard.BoardCell")
                        .render_P( args => {
                          val (id,props,bs) = args
                          val me = props.page.boardid
                          <.td(
                            AppButton( "Board_"+id, "Board "+Id.boardIdToBoardNumber(id),
                                       BaseStyles.highlight(
                                           selected = me == id,
                                           required = me != id && bs.allplayed,
                                           requiredNotNext = me != id && bs.anyplayed
                                       ),
                                       baseStyles.appButton100,
                                       props.routerCtl.setOnClick(props.page.toScoreboardView().toBoardView(id))
                            )
                          )
                        }).build

  val component = ScalaComponent.builder[Props]("PageBoard")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

