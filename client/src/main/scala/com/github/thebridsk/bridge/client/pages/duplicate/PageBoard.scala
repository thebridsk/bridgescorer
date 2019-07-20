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
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.BoardScore
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseBoardViewWithPerspective
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableBoardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageBoard( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageBoard {
  import PageBoardInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective ) = {
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
  case class State( useIMP: Option[Boolean] = None ) {

    def isMP = !useIMP.getOrElse(false)
    def isIMP = useIMP.getOrElse(false)

    def toggleIMP = {
      copy( useIMP = Some(!isIMP) )
    }

    def nextIMPs = {
      val n = useIMP match {
        case None => Some(false)
        case Some(false) => Some(true)
        case Some(true) => None
      }
      copy(useIMP=n)
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val nextIMPs = scope.modState { s => s.nextIMPs }

    def render( props: Props, state: State ) = {
      logger.info(s"Rendering board ${props.page} routectl=${props.routerCtl.getClass.getName}")
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
            val id = board.id
            val selected = id == props.page.boardid
            val clickPage = if (played) {
              props.page.toBoardView(id)
            } else {
              props.page.toBoardView(id).toHandView(ns)
            }
            Seq[TagMod](
              <.span(" "),
              AppButton( "Board_"+board.id, "Board "+Id.boardIdToBoardNumber(id),
                         BaseStyles.highlight(
                             selected=selected,
                             requiredNotNext = !played && !selected
                         ),
                         props.routerCtl.setOnClick(clickPage)
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
            <.span(
              s"Table ${Id.tableIdToTableNumber(currentTable)} Round ${currentRound}" ,
              s" Board ${Id.boardIdToBoardNumber(props.page.boardid)}",
              s" Teams ${Id.teamIdToTeamNumber(team1)} and ${Id.teamIdToTeamNumber(team2)}",
            )
          case PerspectiveDirector =>
            <.span(s"Director's View of Board ${Id.boardIdToBoardNumber(props.page.boardid)}")
          case PerspectiveComplete =>
            <.span(s"Completed View of Board ${Id.boardIdToBoardNumber(props.page.boardid)}")
        }

      }


      <.div( baseStyles.divBoardPage,
        DuplicatePageBridgeAppBar(
          id = Some(props.page.dupid),
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      title(),
                    )
                )),
          helpurl = "../help/duplicate/boardcomplete.html",
          routeCtl = props.routerCtl
        )(

        ),
        DuplicateStore.getView( perspective ) match {
          case Some(score) =>
            val allplayedInRound = score.getRound(currentTable, currentRound) match {
              case Some(r) => r.complete
              case _ => false
            }
            val clickToScoreboard = props.page.toScoreboardView()
            val clickToTableView = tableBoardView.map( tbv => tbv.toTableView() )
            <.div(
              dupStyles.divBoardPage,
//              title(),
              ViewBoard( props.routerCtl, props.page, score, props.page.boardid, state.isIMP ),
              <.p,
              <.div(
                baseStyles.fontTextNormal,
                if (tableperspective.isDefined) {
                  TagMod(
                    boardsFromTable(score),
                    <.p
                  )
                } else {
                  TagMod()
                },
                AppButton( "Game", "Scoreboard",
                           allplayedInRound ?= baseStyles.requiredNotNext,
                           props.routerCtl.setOnClick(clickToScoreboard) ),
                " ",
                clickToTableView.isDefined?= AppButton( "Table", "Table "+Id.tableIdToTableNumber(currentTable),
                                                        allplayedInRound ?= baseStyles.requiredNotNext,
                                                        props.routerCtl.setOnClick(clickToTableView.get) ),
                " ",
                AppButton( "AllBoards", "All Boards",
                           props.routerCtl.setOnClick(props.page.toAllBoardsView())
                         ),
                " ",
                PageScoreboardInternal.scoringMethodButton( state.useIMP, Some( score.isIMP), false, nextIMPs ),
                if (tableperspective.isEmpty) boards(score)
                else TagMod()
              )
            )
          case None =>
            <.h1( "Waiting" )
        }
      )
    }

    val storeCallback = scope.modStateOption { s =>
      DuplicateStore.getMatch().map( md => s.copy( useIMP = Some(md.isIMP) ) )
    }

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("PageBoard.didMount")
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitor(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageBoard.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
      Controller.delayStop()
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
                          val clickToBoard = props.page.toScoreboardView().toBoardView(id)
                          logger.fine(s"Target for setOnClick is ${clickToBoard}")
                          <.td(
                            AppButton( "Board_"+id, "Board "+Id.boardIdToBoardNumber(id),
                                       BaseStyles.highlight(
                                           selected = me == id,
                                           required = me != id && bs.allplayed,
                                           requiredNotNext = me != id && bs.anyplayed
                                       ),
                                       baseStyles.appButton100,
                                       props.routerCtl.setOnClick(clickToBoard)
                            )
                          )
                        }).build

  val component = ScalaComponent.builder[Props]("PageBoard")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

