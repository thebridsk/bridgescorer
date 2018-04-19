package com.example.pages.duplicate

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, Router, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.example.data.bridge._
import com.example.pages.info.InfoPage
import com.example.pages.HomePage
import utils.logging.Logger
import japgolly.scalajs.react.Callback
import com.example.data.Id
import com.example.bridge.store.DuplicateStore
import com.example.routes.AppRouter.AppPage
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react._
import com.example.pages.duplicate.boardsets.PageBoardSets
import com.example.pages.duplicate.boardsets.PageMovements
import japgolly.scalajs.react.extra.router.StaticDsl.Rule
import com.example.routes.Module
import com.example.routes.TestBridgeRouter
import com.example.routes.BridgeRouter
import com.example.routes.BridgeRouterBase
import com.example.routes.BridgeRouterBase
import com.example.routes.BridgeRouter
import com.example.skeleton.react.BeepComponent
import japgolly.scalajs.react.vdom.TagMod
import com.example.pages.duplicate.DuplicateRouter.BaseScoreboardView
import scala.scalajs.js.URIUtils
import com.example.routes.BridgeRouterBaseWithLogging

object DuplicateModule extends Module {
  case class PlayDuplicate(m: DuplicatePage ) extends AppPage

  def verifyPages(): List[AppPage] = DuplicateRouter.verifyPages.map( p => PlayDuplicate(p).asInstanceOf[AppPage]).toList

  def routes(): Rule[AppPage] = {
    DuplicateRouter.routes.prefixPath_/("#duplicate").pmap[AppPage](PlayDuplicate){ case PlayDuplicate(m) => m } |
    DuplicateRouter.importRoutes.prefixPath_/("#import").pmap[AppPage](PlayDuplicate){ case PlayDuplicate(m) => m }
  }

  override
  def canRender(selectedPage: Resolution[AppPage]): Boolean = selectedPage.page.isInstanceOf[DuplicateModule.PlayDuplicate]

  override
  def render(selectedPage: Resolution[AppPage]): TagMod =
    TagMod(
      selectedPage.render(),
      BeepComponent(()=>{
        selectedPage.page match {
          case PlayDuplicate(p) => p.isInstanceOf[BaseScoreboardView]
          case _ => false
        }
      })
    )

}

sealed trait DuplicatePage
object DuplicateRouter {

  val logger = Logger("bridge.DuplicateRouter")

  /**
   * A BridgeRouter for testing
   * @param base the base url, example: http://localhost:8080/html/index-fastopt.html
   */
  abstract class TestDuplicatePageBridgeRouter( base: BaseUrl ) extends TestBridgeRouter[DuplicatePage](base) {
    def refresh: Callback
    def set( page: DuplicatePage ): Callback

    def pathFor(target: DuplicatePage): Path = target match {
      case SummaryView => Path("#/duplicates")
      // TODO implement other pages
      case _ => ???
    }
  }

  class DuplicateRouterWithLogging( ctl: RouterCtl[DuplicatePage] ) extends BridgeRouterBaseWithLogging[DuplicatePage](ctl) {
    override
    def home: TagMod = DuplicateModule.gotoAppHome()
  }

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter( ctl: RouterCtl[DuplicatePage] ): BridgeRouter[DuplicatePage] =
    new DuplicateRouterWithLogging(ctl)

  trait BaseBoardView extends DuplicatePage {
    val dupid: String
    val boardid: String
    def toScoreboardView(): BaseScoreboardView
    def toHandView( handid: String ): BaseHandView
    def toBoardView( bid: String ): BaseBoardView
  }

  trait BaseBoardViewWithPerspective extends BaseBoardView {
    def getPerspective(): DuplicateViewPerspective
  }

  trait BaseScoreboardView extends DuplicatePage {
    val dupid: String
    def toBoardView( id: Id.DuplicateBoard): BaseBoardView
    def toAllBoardsView(): BaseAllBoardsView
  }

  trait BaseAllBoardsView extends DuplicatePage {
    val dupid: String
    def toBoardView( id: Id.DuplicateBoard): BaseBoardView
    def toScoreboardView(): BaseScoreboardView
    def toHandView(  boardid: Id.DuplicateBoard, handid: String ): BaseHandView
  }

  trait BaseScoreboardViewWithPerspective extends BaseScoreboardView {
    def getPerspective(): DuplicateViewPerspective
  }

  trait BaseAllBoardsViewWithPerspective extends BaseAllBoardsView {
    def getPerspective(): DuplicateViewPerspective
  }

  trait BaseHandView extends DuplicatePage {
    val dupid: String
    val boardid: String
    val handid: String
    def toBoardView(): BaseBoardView
    def getPerspective(): Option[DuplicateViewPerspective]
  }

  trait SummaryViewBase extends DuplicatePage {
    def getScoreboardPage(dupid: String): BaseScoreboardViewWithPerspective
    def getDuplicateResultPage(dupid: String): DuplicateResultViewBase
  }
  case object SummaryView extends SummaryViewBase {
    def getScoreboardPage(dupid: String): BaseScoreboardViewWithPerspective = CompleteScoreboardView(dupid)
    def getDuplicateResultPage(dupid: String): DuplicateResultViewBase = DuplicateResultView(dupid)
  }

  case object PairsView extends DuplicatePage
  case object StatsView extends DuplicatePage
  case object NewDuplicateView extends DuplicatePage
  case object SelectMatchView extends DuplicatePage
  case object SuggestionView extends DuplicatePage

  case class NamesView( dupid: String ) extends DuplicatePage

  case object FinishedScoreboardsView0 extends DuplicatePage

  /**
   * @param dupids - a comma separated list of duplicate ids
   */
  case class FinishedScoreboardsView( dupids: String ) extends DuplicatePage {
    import collection.breakOut
    def getIds() = dupids.split(",").map(_.trim).filter(_.length()>0) // (breakOut)
  }

  case class DuplicateBoardSetView( dupid: String ) extends DuplicatePage {
    def toScoreboard() = CompleteScoreboardView(dupid)
  }

  case class CompleteScoreboardView( dupid: String ) extends BaseScoreboardViewWithPerspective {
    def toBoardView( id: Id.DuplicateBoard) = CompleteBoardView( dupid, id )
    def toAllBoardsView() = CompleteAllBoardView(dupid)
    def getPerspective(): DuplicateViewPerspective = PerspectiveComplete
  }

  case class FinishedScoreboardView( dupid: String ) extends BaseScoreboardViewWithPerspective {
    def toBoardView( id: Id.DuplicateBoard) = CompleteBoardView( dupid, id )
    def toAllBoardsView() = CompleteAllBoardView(dupid)
    def getPerspective(): DuplicateViewPerspective = PerspectiveComplete
  }

  trait DuplicateResultViewBase extends DuplicatePage {
    val dupid: String
  }
  case class DuplicateResultView( dupid: String ) extends DuplicateResultViewBase

  case class DuplicateResultEditView( dupid: String ) extends DuplicatePage

  case class CompleteAllBoardView( dupid: String ) extends BaseAllBoardsViewWithPerspective {
    def toBoardView( id: Id.DuplicateBoard) = CompleteBoardView( dupid, id )
    def toScoreboardView() = CompleteScoreboardView(dupid)
    def getPerspective(): DuplicateViewPerspective = PerspectiveComplete
    def toHandView( boardid: Id.DuplicateBoard, handid: String ) = CompleteHandView(dupid,boardid,handid)
  }
  case class CompleteBoardView( dupid: String, boardid: String ) extends BaseBoardViewWithPerspective {
    def toScoreboardView() = CompleteScoreboardView(dupid)
    def getPerspective(): DuplicateViewPerspective = PerspectiveComplete
    def toHandView( handid: String ) = CompleteHandView(dupid,boardid,handid)
    def toBoardView( bid: String ) = CompleteBoardView(dupid,bid)
  }
  case class CompleteHandView( dupid: String, boardid: String, handid: String ) extends BaseHandView {
    def toBoardView() = CompleteBoardView(dupid,boardid)
    def getPerspective(): Option[DuplicateViewPerspective] = Some(PerspectiveComplete)
  }

  case class DirectorScoreboardView( dupid: String ) extends BaseScoreboardViewWithPerspective {
    def toBoardView( id: Id.DuplicateBoard) = DirectorBoardView( dupid, id )
    def toAllBoardsView() = DirectorAllBoardView(dupid)
    def getPerspective(): DuplicateViewPerspective = PerspectiveDirector
  }

  case class DirectorAllBoardView( dupid: String ) extends BaseAllBoardsViewWithPerspective {
    def toBoardView( id: Id.DuplicateBoard) = DirectorBoardView( dupid, id )
    def toScoreboardView() = DirectorScoreboardView(dupid)
    def getPerspective(): DuplicateViewPerspective = PerspectiveDirector
    def toHandView( boardid: Id.DuplicateBoard, handid: String ) = DirectorHandView(dupid,boardid,handid)
  }
  case class DirectorBoardView( dupid: String, boardid: String ) extends BaseBoardViewWithPerspective {
    def toScoreboardView() = DirectorScoreboardView(dupid)
    def getPerspective(): DuplicateViewPerspective = PerspectiveDirector
    def toHandView( handid: String ) = DirectorHandView(dupid,boardid,handid)
    def toBoardView( bid: String ) = DirectorBoardView(dupid,bid)
  }
  case class DirectorHandView( dupid: String, boardid: String, handid: String ) extends BaseHandView {
    def toBoardView() = DirectorBoardView(dupid,boardid)
    def getPerspective(): Option[DuplicateViewPerspective] = Some(PerspectiveDirector)
  }

  case class TableView( dupid: String, tableid: String ) extends DuplicatePage {
    def toBoardView( round: Int, id: Id.DuplicateBoard) = TableBoardView( dupid, tableid, round, id )
    def toRoundView( roundid: Int ) = TableRoundScoreboardView(dupid,tableid,roundid)

    def toTableTeamView( roundid: Int) = TableTeamByRoundView(dupid,tableid,roundid)
    def toTableTeamView( roundid: Int, boardid: Id.DuplicateBoard) = TableTeamByBoardView(dupid,tableid,roundid,boardid)
  }

  case class AllTableView( dupid: String ) extends DuplicatePage {
    def toTableView( tableid: String ) = TableView(dupid,tableid)
    def toBoardView( tableid: String, round: Int, id: Id.DuplicateBoard) = TableBoardView( dupid, tableid, round, id )
    def toRoundView( tableid: String, roundid: Int ) = TableRoundScoreboardView(dupid,tableid,roundid)
  }

  trait TableTeamView extends DuplicatePage {
    val dupid: String
    val tableid: String
    val round: Int

    def editPlayers: Boolean = false

    def setEditPlayers( flag: Boolean ): TableTeamView

    def toNextView(): DuplicatePage
    def toTableView(): TableView
  }

  case class TableTeamByRoundView( dupid: String, tableid: String, round: Int ) extends TableTeamView {
    def toNextView() = TableRoundScoreboardView(dupid,tableid,round)
    def toTableView() = TableView(dupid, tableid)

    def setEditPlayers( flag: Boolean ) = if (flag) TableTeamByRoundEditView( dupid, tableid, round ) else this
  }

  case class TableTeamByRoundEditView( dupid: String, tableid: String, round: Int ) extends TableTeamView {
    def toNextView() = TableRoundScoreboardView(dupid,tableid,round)
    def toTableView() = TableView(dupid, tableid)

    override def editPlayers: Boolean = true

    def setEditPlayers( flag: Boolean ) = if (flag) this else TableTeamByRoundEditView( dupid, tableid, round )
  }

  case class TableTeamByBoardView( dupid: String, tableid: String, round: Int, boardId: Id.DuplicateBoard ) extends TableTeamView {
    def toNextView() = TableBoardView( dupid, tableid, round, boardId )
    def toTableView() = TableView(dupid, tableid)
    def setEditPlayers( flag: Boolean ) = if (flag) TableTeamByBoardEditView( dupid, tableid, round, boardId ) else this
  }

  case class TableTeamByBoardEditView( dupid: String, tableid: String, round: Int, boardId: Id.DuplicateBoard ) extends TableTeamView {
    def toNextView() = TableBoardView( dupid, tableid, round, boardId )
    def toTableView() = TableView(dupid, tableid)
    override def editPlayers: Boolean = true
    def setEditPlayers( flag: Boolean ) = if (flag) this else TableTeamByBoardEditView( dupid, tableid, round, boardId )
  }

  case class TableRoundScoreboardView( dupid: String, tableid: String, round: Int ) extends BaseScoreboardViewWithPerspective {
    def toBoardView( boardid: Id.DuplicateBoard) = TableBoardView( dupid, tableid, round, boardid )
    def toAllBoardsView() = TableRoundAllBoardView(dupid,tableid,round)
    def toHandView( boardid: Id.DuplicateBoard, handid: Id.DuplicateHand ) = TableHandView(dupid,tableid,round,boardid,handid)
    def toTableView() = TableView(dupid,tableid)
    def getPerspective(): DuplicateViewPerspective = DuplicateStore.getTablePerspectiveFromRound(tableid, round) match {
      case Some(p) => p
      case None => PerspectiveComplete
    }
  }
  case class TableRoundAllBoardView( dupid: String, tableid: String, round: Int ) extends BaseAllBoardsViewWithPerspective {
    def toBoardView( boardid: Id.DuplicateBoard) = TableBoardView( dupid, tableid, round, boardid )
    def toScoreboardView() = TableRoundScoreboardView(dupid,tableid,round)
    def getPerspective(): DuplicateViewPerspective = DuplicateStore.getTablePerspectiveFromRound(tableid, round) match {
      case Some(p) => p
      case None => PerspectiveComplete
    }
    def toHandView( boardid: Id.DuplicateBoard, handid: String ) = TableHandView(dupid,tableid,round,boardid,handid)
  }
  case class TableBoardView( dupid: String, tableid: String, round: Int, boardid: String ) extends BaseBoardViewWithPerspective {
    def toTableView() = TableView( dupid, tableid )
    def toScoreboardView() = TableRoundScoreboardView(dupid,tableid,round)
    def toHandView( handid: String ) = TableHandView(dupid,tableid,round,boardid,handid)
    def toBoardView( bid: String ) = TableBoardView(dupid,tableid,round,bid)
    def getPerspective(): DuplicateViewPerspective = DuplicateStore.getTablePerspectiveFromRound(tableid, round) match {
      case Some(p) => p
      case None => PerspectiveComplete
    }
  }
  case class TableHandView( dupid: String, tableid: String, round: Int, boardid: String, handid: String ) extends BaseHandView {
    def toBoardView() = TableBoardView(dupid,tableid,round,boardid)
    def getPerspective(): Option[DuplicateViewPerspective] = DuplicateStore.getTablePerspectiveFromRound(tableid, round)
  }

  case object BoardSetSummaryView extends DuplicatePage

  case class BoardSetView( display: String ) extends DuplicatePage

  case object MovementSummaryView extends DuplicatePage

  case class MovementView( display: String ) extends DuplicatePage

  case class ImportSummaryView( importId: String ) extends SummaryViewBase {
    def getScoreboardPage(dupid: String): BaseScoreboardViewWithPerspective = CompleteScoreboardView(dupid)
    def getDuplicateResultPage(dupid: String): DuplicateResultViewBase = DuplicateResultView(dupid)
    def getDecodedId = URIUtils.decodeURI(importId)
  }

  val verifyPages = SummaryView::
                    PairsView::
                    StatsView::
                    NewDuplicateView::
                    SelectMatchView::
                    SuggestionView::
                    NamesView("M1")::
                    DuplicateBoardSetView("M1")::
                    CompleteScoreboardView("M1")::
                    FinishedScoreboardView("M1")::
                    FinishedScoreboardsView0::
                    FinishedScoreboardsView("M1,M2")::
                    CompleteAllBoardView("M1")::
                    DirectorScoreboardView("M1")::
                    DirectorAllBoardView("M1")::
                    DuplicateResultView("E1")::
                    DuplicateResultEditView("E1")::
                    AllTableView("M1")::
                    TableView("M1","1")::
                    TableRoundScoreboardView("M1","1",1)::
                    TableRoundAllBoardView("M1","1",1)::
                    TableHandView("M1","1",1,"1","T1")::
                    CompleteHandView("M1","1","T1")::
                    DirectorHandView("M1","1","T1")::
                    TableBoardView("M1","1",1,"1")::
                    CompleteBoardView("M1","1")::
                    DirectorBoardView("M1","1")::
                    TableTeamByRoundView("M1","1",1)::
                    TableTeamByBoardView("M1","1",1,"1")::
                    TableTeamByRoundEditView("M1","1",1)::
                    TableTeamByBoardEditView("M1","1",1,"1")::
                    BoardSetSummaryView::
                    BoardSetView("ArmonkBoards")::
                    MovementSummaryView::
                    MovementView("Armonk2Tables")::
                    ImportSummaryView("import.zip")::
                    Nil

  val routes = RouterConfigDsl[DuplicatePage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | staticRoute( "pairs", PairsView )
        ~> renderR( routerCtl => PagePairs(routerCtl) )
      | staticRoute( "stats", StatsView )
        ~> renderR( routerCtl => PageStats(routerCtl) )
      | staticRoute( "suggestion", SuggestionView )
        ~> renderR( routerCtl => PageSuggestion(routerCtl) )
      | staticRoute( "boardsets", BoardSetSummaryView )
        ~> renderR( routerCtl => PageBoardSets(routerCtl,SummaryView,None) )
      | dynamicRouteCT( ("boardsets" / string("[a-zA-Z0-9]+")).caseClass[BoardSetView])
        ~> dynRenderR( (p,routerCtl) => PageBoardSets(routerCtl,SummaryView,Some(p.display)) )
      | staticRoute( "movements", MovementSummaryView )
        ~> renderR( routerCtl => PageMovements(routerCtl,SummaryView,None) )
      | dynamicRouteCT( ("results" / string("[a-zA-Z0-9]+")).caseClass[DuplicateResultView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateResult(routerCtl,p) )
      | dynamicRouteCT( ("results" / string("[a-zA-Z0-9]+") / "edit").caseClass[DuplicateResultEditView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateResultEdit(routerCtl,p) )
      | dynamicRouteCT( ("movements" / string("[a-zA-Z0-9]+")).caseClass[MovementView])
        ~> dynRenderR( (p,routerCtl) => PageMovements(routerCtl,SummaryView,Some(p.display)) )
      | staticRoute( "finished", FinishedScoreboardsView0 )
        ~> renderR( routerCtl => PageFinishedScoreboards(routerCtl,FinishedScoreboardsView("")) )
      | dynamicRouteCT( ("finished" / string("[a-zA-Z0-9,]*")).caseClass[FinishedScoreboardsView])
        ~> dynRenderR( (p,routerCtl) => PageFinishedScoreboards(routerCtl,p) )
      | dynamicRouteCT( string("[a-zA-Z0-9]+").caseClass[CompleteScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "names").caseClass[NamesView])
        ~> dynRenderR( (p,routerCtl) => PageNames(routerCtl,p,CompleteScoreboardView(p.dupid)) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "boardset").caseClass[DuplicateBoardSetView])
        ~> dynRenderR( (p,routerCtl) => PageBoardSet(routerCtl,p))
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "boards").caseClass[CompleteAllBoardView])
        ~> dynRenderR( (p,routerCtl) => PageAllBoards(routerCtl,p))
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "boards" / string("[a-zA-Z0-9]+")).caseClass[CompleteBoardView])
        ~> dynRenderR( (p,routerCtl) => PageBoard(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "boards" / string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+")).caseClass[CompleteHandView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateHand(routerCtl,p) )

      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "finished").caseClass[FinishedScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "director").caseClass[DirectorScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "director" / "boards").caseClass[DirectorAllBoardView])
        ~> dynRenderR( (p,routerCtl) => PageAllBoards(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "director" / "boards" / string("[a-zA-Z0-9]+")).caseClass[DirectorBoardView])
        ~> dynRenderR( (p,routerCtl) => PageBoard(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "director" / "boards" / string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+")).caseClass[DirectorHandView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateHand(routerCtl,p) )

      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" ).caseClass[AllTableView])
        ~> dynRenderR( (p,routerCtl) => PageAllTables(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+")).caseClass[TableView])
        ~> dynRenderR( (p,routerCtl) => PageTable(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "teams").caseClass[TableTeamByRoundView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "editteams").caseClass[TableTeamByRoundEditView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "game").caseClass[TableRoundScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards").caseClass[TableRoundAllBoardView])
        ~> dynRenderR( (p,routerCtl) => PageAllBoards(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") ).caseClass[TableBoardView])
        ~> dynRenderR( (p,routerCtl) => PageBoard(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") / "teams").caseClass[TableTeamByBoardView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") / "editteams").caseClass[TableTeamByBoardEditView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+")).caseClass[TableHandView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateHand(routerCtl,p) )

      | staticRoute( "#new", NewDuplicateView )
        ~> renderR( routerCtl => PageNewDuplicate(routerCtl,NewDuplicateView) )

      | staticRoute( "#select", SelectMatchView )
        ~> renderR( routerCtl => PageSelectMatch(routerCtl,SelectMatchView) )

      | staticRoute( root, SummaryView )
        ~> renderR( routerCtl => PageSummary(routerCtl, SummaryView) )
      )
  }

  val importRoutes = RouterConfigDsl[DuplicatePage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | dynamicRouteCT( (string(".+") / "duplicate" ).caseClass[ImportSummaryView])
        ~> dynRenderR( (p,routerCtl) => PageSummary(routerCtl,p) )
      )
  }

}
