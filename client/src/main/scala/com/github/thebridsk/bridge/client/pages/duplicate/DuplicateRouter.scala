package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, Router, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.client.pages.info.InfoPage
import com.github.thebridsk.bridge.client.pages.HomePage
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageBoardSets
import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageMovements
import com.github.thebridsk.bridge.client.routes.Module
import com.github.thebridsk.bridge.client.routes.TestBridgeRouter
import com.github.thebridsk.bridge.client.routes.BridgeRouterBase
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseScoreboardView
import scala.scalajs.js.URIUtils
import com.github.thebridsk.bridge.client.routes.BridgeRouterBaseWithLogging
import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageEditBoardSet
import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageEditMovement
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board

object DuplicateModule extends Module {
  case class PlayDuplicate(m: DuplicatePage ) extends AppPage

  def verifyPages(): List[AppPage] = DuplicateRouter.verifyPages.map( p => PlayDuplicate(p).asInstanceOf[AppPage]).toList

  def routes() = {
    DuplicateRouter.routes.prefixPath_/("#duplicate").pmap[AppPage](PlayDuplicate){ case PlayDuplicate(m) => m } |
    DuplicateRouter.importRoutes.prefixPath_/("#import").pmap[AppPage](PlayDuplicate){ case PlayDuplicate(m) => m }
  }

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

    override
    def toHome: Unit = DuplicateModule.toHome
    override
    def toAbout: Unit = DuplicateModule.toAbout

    override
    def toInfo: Unit = {
      DuplicateModule.toInfo
    }

    override
    def toRootPage( page: AppPage ): Unit = DuplicateModule.toRootPage(page)

  }

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter( ctl: RouterCtl[DuplicatePage] ): BridgeRouter[DuplicatePage] =
    new DuplicateRouterWithLogging(ctl)

  trait BaseBoardView extends DuplicatePage {
    val dupid: String
    val sboardid: String
    def boardid = Board.id(sboardid)
    def toScoreboardView: BaseScoreboardView
    def toHandView( handid: Team.Id ): BaseHandView
    def toBoardView( bid: Board.Id ): BaseBoardView
    def toAllBoardsView: BaseAllBoardsView
  }

  trait BaseBoardViewWithPerspective extends BaseBoardView {
    def getPerspective: DuplicateViewPerspective
  }

  trait BaseScoreboardView extends DuplicatePage {
    val dupid: String
    def toBoardView( id: Board.Id): BaseBoardView
    def toAllBoardsView: BaseAllBoardsView
  }

  trait BaseAllBoardsView extends DuplicatePage {
    val dupid: String
    def toBoardView( id: Board.Id): BaseBoardView
    def toScoreboardView: BaseScoreboardView
    def toHandView(  boardid: Board.Id, handid: Team.Id ): BaseHandView
  }

  trait BaseScoreboardViewWithPerspective extends BaseScoreboardView {
    def getPerspective: DuplicateViewPerspective
  }

  trait BaseAllBoardsViewWithPerspective extends BaseAllBoardsView {
    def getPerspective: DuplicateViewPerspective
  }

  trait BaseHandView extends DuplicatePage {
    val dupid: String
    val sboardid: String
    val boardid = Board.id(sboardid)
    val shandid: String
    def handid = Team.id(shandid)
    def toBoardView: BaseBoardView
    def getPerspective: Option[DuplicateViewPerspective]
  }

  trait SummaryViewBase extends DuplicatePage {
    def getScoreboardPage(dupid: String): BaseScoreboardViewWithPerspective
    def getDuplicateResultPage(dupid: String): DuplicateResultViewBase
    def getImportId: Option[String] = None
  }
  case object SummaryView extends SummaryViewBase {
    def getScoreboardPage(dupid: String): BaseScoreboardViewWithPerspective = CompleteScoreboardView(dupid)
    def getDuplicateResultPage(dupid: String): DuplicateResultViewBase = DuplicateResultView(dupid)
  }

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
    def getIds = dupids.split(",").map(_.trim).filter(_.length()>0)
  }

  case class DuplicateBoardSetView( dupid: String ) extends DuplicatePage {
    def toScoreboard = CompleteScoreboardView(dupid)
  }

  case class CompleteScoreboardView( dupid: String ) extends BaseScoreboardViewWithPerspective {
    def toBoardView( id: Board.Id) = CompleteBoardView( dupid, id.id )
    def toAllBoardsView = CompleteAllBoardView(dupid)
    def getPerspective: DuplicateViewPerspective = PerspectiveComplete
  }

  case class FinishedScoreboardView( dupid: String ) extends BaseScoreboardViewWithPerspective {
    def toBoardView( id: Board.Id) = CompleteBoardView( dupid, id.id )
    def toAllBoardsView = CompleteAllBoardView(dupid)
    def getPerspective: DuplicateViewPerspective = PerspectiveComplete
  }

  trait DuplicateResultViewBase extends DuplicatePage {
    val dupid: String
  }
  case class DuplicateResultView( dupid: String ) extends DuplicateResultViewBase

  case class DuplicateResultEditView( dupid: String ) extends DuplicatePage

  case class CompleteAllBoardView( dupid: String ) extends BaseAllBoardsViewWithPerspective {
    def toBoardView( id: Board.Id) = CompleteBoardView( dupid, id.id )
    def toScoreboardView = CompleteScoreboardView(dupid)
    def getPerspective: DuplicateViewPerspective = PerspectiveComplete
    def toHandView( boardid: Board.Id, handid: Team.Id ) = CompleteHandView(dupid,boardid.id,handid.id)
  }
  case class CompleteBoardView( dupid: String, sboardid: String ) extends BaseBoardViewWithPerspective {
    def toScoreboardView = CompleteScoreboardView(dupid)
    def getPerspective: DuplicateViewPerspective = PerspectiveComplete
    def toHandView( handid: Team.Id ) = CompleteHandView(dupid,sboardid,handid.id)
    def toBoardView( bid: Board.Id ) = CompleteBoardView(dupid,bid.id)
    def toAllBoardsView = CompleteAllBoardView(dupid)
  }
  case class CompleteHandView( dupid: String, sboardid: String, shandid: String ) extends BaseHandView {
    def toBoardView = CompleteBoardView(dupid,sboardid)
    def getPerspective: Option[DuplicateViewPerspective] = Some(PerspectiveComplete)
  }

  case class DirectorScoreboardView( dupid: String ) extends BaseScoreboardViewWithPerspective {
    def toBoardView( id: Board.Id) = DirectorBoardView( dupid, id.id )
    def toAllBoardsView = DirectorAllBoardView(dupid)
    def getPerspective: DuplicateViewPerspective = PerspectiveDirector
  }

  case class DirectorAllBoardView( dupid: String ) extends BaseAllBoardsViewWithPerspective {
    def toBoardView( id: Board.Id) = DirectorBoardView( dupid, id.id )
    def toScoreboardView = DirectorScoreboardView(dupid)
    def getPerspective: DuplicateViewPerspective = PerspectiveDirector
    def toHandView( boardid: Board.Id, handid: Team.Id ) = DirectorHandView(dupid,boardid.id,handid.id)
  }
  case class DirectorBoardView( dupid: String, sboardid: String ) extends BaseBoardViewWithPerspective {
    def toScoreboardView = DirectorScoreboardView(dupid)
    def getPerspective: DuplicateViewPerspective = PerspectiveDirector
    def toHandView( handid: Team.Id ) = DirectorHandView(dupid,sboardid,handid.id)
    def toBoardView( bid: Board.Id ) = DirectorBoardView(dupid,bid.id)
    def toAllBoardsView = DirectorAllBoardView(dupid)
  }
  case class DirectorHandView( dupid: String, sboardid: String, shandid: String ) extends BaseHandView {
    def toBoardView = DirectorBoardView(dupid,sboardid)
    def getPerspective: Option[DuplicateViewPerspective] = Some(PerspectiveDirector)
  }

  case class TableView( dupid: String, stableid: String ) extends DuplicatePage {
    def tableid = Table.id(stableid)
    def toBoardView( round: Int, id: Board.Id) = TableBoardView( dupid, stableid, round, id.id )
    def toRoundView( roundid: Int ) = TableRoundScoreboardView(dupid,stableid,roundid)

    def toTableTeamView( roundid: Int) = TableTeamByRoundView(dupid,stableid,roundid)
    def toTableTeamView( roundid: Int, boardid: Board.Id) = TableTeamByBoardView(dupid,stableid,roundid,boardid.id)
  }

  case class AllTableView( dupid: String ) extends DuplicatePage {
    def toTableView( tableid: Table.Id ) = TableView(dupid,tableid.id)
    def toBoardView( tableid: Table.Id, round: Int, id: Board.Id) = TableBoardView( dupid, tableid.id, round, id.id )
    def toRoundView( tableid: Table.Id, roundid: Int ) = TableRoundScoreboardView(dupid,tableid.id,roundid)
  }

  trait TableTeamView extends DuplicatePage {
    val dupid: String
    val stableid: String
    def tableid = Table.id(stableid)
    val round: Int

    def editPlayers: Boolean = false

    def setEditPlayers( flag: Boolean ): TableTeamView

    def toNextView: DuplicatePage
    def toTableView: TableView
  }

  case class TableTeamByRoundView( dupid: String, stableid: String, round: Int ) extends TableTeamView {
    def toNextView = TableRoundScoreboardView(dupid,stableid,round)
    def toTableView = TableView(dupid, stableid)

    def setEditPlayers( flag: Boolean ) = if (flag) TableTeamByRoundEditView( dupid, stableid, round ) else this
  }

  case class TableTeamByRoundEditView( dupid: String, stableid: String, round: Int ) extends TableTeamView {
    def toNextView = TableRoundScoreboardView(dupid,stableid,round)
    def toTableView = TableView(dupid, stableid)

    override def editPlayers: Boolean = true

    def setEditPlayers( flag: Boolean ) = if (flag) this else TableTeamByRoundEditView( dupid, stableid, round )
  }

  case class TableTeamByBoardView( dupid: String, stableid: String, round: Int, sboardid: String ) extends TableTeamView {
    def boardid = Board.id(sboardid)
    def toNextView = TableBoardView( dupid, stableid, round, sboardid )
    def toTableView = TableView(dupid, stableid)
    def setEditPlayers( flag: Boolean ) = if (flag) TableTeamByBoardEditView( dupid, stableid, round, sboardid ) else this
  }

  case class TableTeamByBoardEditView( dupid: String, stableid: String, round: Int, sboardid: String ) extends TableTeamView {
    def boardid = Board.id(sboardid)
    def toNextView = TableBoardView( dupid, stableid, round, sboardid )
    def toTableView = TableView(dupid, stableid)
    override def editPlayers: Boolean = true
    def setEditPlayers( flag: Boolean ) = if (flag) this else TableTeamByBoardEditView( dupid, stableid, round, sboardid )
  }

  case class TableRoundScoreboardView( dupid: String, stableid: String, round: Int ) extends BaseScoreboardViewWithPerspective {
    def tableid = Table.id(stableid)
    def toBoardView( boardid: Board.Id) = TableBoardView( dupid, stableid, round, boardid.id )
    def toAllBoardsView = TableRoundAllBoardView(dupid,stableid,round)
    def toHandView( boardid: Board.Id, handid: Team.Id ) = TableHandView(dupid,stableid,round,boardid.id,handid.id)
    def toTableView = TableView(dupid,stableid)
    def getPerspective: DuplicateViewPerspective = DuplicateStore.getTablePerspectiveFromRound(tableid, round) match {
      case Some(p) => p
      case None => PerspectiveComplete
    }
  }
  case class TableRoundAllBoardView( dupid: String, stableid: String, round: Int ) extends BaseAllBoardsViewWithPerspective {
    def tableid = Table.id(stableid)
    def toBoardView( boardid: Board.Id) = TableBoardView( dupid, stableid, round, boardid.id )
    def toScoreboardView = TableRoundScoreboardView(dupid,stableid,round)
    def getPerspective: DuplicateViewPerspective = DuplicateStore.getTablePerspectiveFromRound(tableid, round) match {
      case Some(p) => p
      case None => PerspectiveComplete
    }
    def toHandView( boardid: Board.Id, handid: Team.Id ) = TableHandView(dupid,stableid,round,boardid.id,handid.id)
  }
  case class TableBoardView( dupid: String, stableid: String, round: Int, sboardid: String ) extends BaseBoardViewWithPerspective {
    def tableid = Table.id(stableid)
    def toTableView = TableView( dupid, stableid )
    def toScoreboardView = TableRoundScoreboardView(dupid,stableid,round)
    def toHandView( handid: Team.Id ) = TableHandView(dupid,stableid,round,sboardid,handid.id)
    def toBoardView( bid: Board.Id ) = TableBoardView(dupid,stableid,round,bid.id)
    def toAllBoardsView = TableRoundAllBoardView(dupid,stableid,round)
    def getPerspective: DuplicateViewPerspective = DuplicateStore.getTablePerspectiveFromRound(tableid, round) match {
      case Some(p) => p
      case None => PerspectiveComplete
    }
  }
  case class TableHandView( dupid: String, stableid: String, round: Int, sboardid: String, shandid: String ) extends BaseHandView {
    def tableid = Table.id(stableid)
    def toBoardView = TableBoardView(dupid,stableid,round,sboardid)
    def getPerspective: Option[DuplicateViewPerspective] = DuplicateStore.getTablePerspectiveFromRound(tableid, round)
  }

  case object BoardSetSummaryView extends DuplicatePage

  case class BoardSetView( display: String ) extends DuplicatePage

  case object BoardSetNewView extends DuplicatePage
  case class BoardSetEditView( display: String ) extends DuplicatePage

  case object MovementSummaryView extends DuplicatePage

  case class MovementView( display: String ) extends DuplicatePage

  case object MovementNewView extends DuplicatePage
  case class MovementEditView( display: String ) extends DuplicatePage

  case class ImportSummaryView( importId: String ) extends SummaryViewBase {
    def getScoreboardPage(dupid: String): BaseScoreboardViewWithPerspective = CompleteScoreboardView(dupid)
    def getDuplicateResultPage(dupid: String): DuplicateResultViewBase = DuplicateResultView(dupid)
    def getDecodedId = URIUtils.decodeURI(importId)
    override
    def getImportId: Option[String] = Some(getDecodedId)
  }

  val verifyPages = SummaryView::
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
                    BoardSetNewView::
                    BoardSetEditView("ArmonkBoards")::
                    MovementSummaryView::
                    MovementView("2TablesArmonk")::
                    MovementNewView::
                    MovementEditView("2TablesArmonk")::
                    ImportSummaryView("import.zip")::
                    Nil

  val routes = RouterConfigDsl[DuplicatePage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | staticRoute( "stats", StatsView )
        ~> renderR( routerCtl => PageStats(routerCtl) )
      | staticRoute( "suggestion", SuggestionView )
        ~> renderR( routerCtl => PageSuggestion(routerCtl) )
      | staticRoute( "boardsets" / "#new", BoardSetNewView )
        ~> renderR( routerCtl => PageEditBoardSet(routerCtl,BoardSetNewView) )
      | dynamicRouteCT( ("boardsets" / string("[a-zA-Z0-9]+") / "edit").caseClass[BoardSetEditView])
        ~> dynRenderR( (p,routerCtl) => PageEditBoardSet(routerCtl,p) )
      | dynamicRouteCT( ("boardsets" / string("[a-zA-Z0-9]+")).caseClass[BoardSetView])
        ~> dynRenderR( (p,routerCtl) => PageBoardSets(routerCtl,SummaryView,Some(p.display)) )
      | staticRoute( "boardsets", BoardSetSummaryView )
        ~> renderR( routerCtl => PageBoardSets(routerCtl,SummaryView,None) )
      | staticRoute( "movements" / "#new", MovementNewView )
        ~> renderR( routerCtl => PageEditMovement(routerCtl,MovementNewView) )
      | dynamicRouteCT( ("movements" / string("[a-zA-Z0-9]+") / "edit").caseClass[MovementEditView])
        ~> dynRenderR( (p,routerCtl) => PageEditMovement(routerCtl,p) )
      | dynamicRouteCT( ("movements" / string("[a-zA-Z0-9]+")).caseClass[MovementView])
        ~> dynRenderR( (p,routerCtl) => PageMovements(routerCtl,SummaryView,Some(p.display)) )
      | staticRoute( "movements", MovementSummaryView )
        ~> renderR( routerCtl => PageMovements(routerCtl,SummaryView,None) )
      | dynamicRouteCT( ("results" / string("[a-zA-Z0-9]+")).caseClass[DuplicateResultView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateResult(routerCtl,p) )
      | dynamicRouteCT( ("results" / string("[a-zA-Z0-9]+") / "edit").caseClass[DuplicateResultEditView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateResultEdit(routerCtl,p) )
      | dynamicRouteCT( ("finished" / string("[a-zA-Z0-9,]*")).caseClass[FinishedScoreboardsView])
        ~> dynRenderR( (p,routerCtl) => PageFinishedScoreboards(routerCtl,p) )
      | staticRoute( "finished", FinishedScoreboardsView0 )
        ~> renderR( routerCtl => PageFinishedScoreboards(routerCtl,FinishedScoreboardsView("")) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "names").caseClass[NamesView])
        ~> dynRenderR( (p,routerCtl) => PageNames(routerCtl,p,CompleteScoreboardView(p.dupid)) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "boardset").caseClass[DuplicateBoardSetView])
        ~> dynRenderR( (p,routerCtl) => PageBoardSet(routerCtl,p))
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "boards" / string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+")).caseClass[CompleteHandView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateHand(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "boards" / string("[a-zA-Z0-9]+")).caseClass[CompleteBoardView])
        ~> dynRenderR( (p,routerCtl) => PageBoard(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "boards").caseClass[CompleteAllBoardView])
        ~> dynRenderR( (p,routerCtl) => PageAllBoards(routerCtl,p))

      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "finished").caseClass[FinishedScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "director" / "boards" / string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+")).caseClass[DirectorHandView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateHand(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "director" / "boards" / string("[a-zA-Z0-9]+")).caseClass[DirectorBoardView])
        ~> dynRenderR( (p,routerCtl) => PageBoard(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "director" / "boards").caseClass[DirectorAllBoardView])
        ~> dynRenderR( (p,routerCtl) => PageAllBoards(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "director").caseClass[DirectorScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )

      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") / "teams").caseClass[TableTeamByBoardView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") / "editteams").caseClass[TableTeamByBoardEditView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+")).caseClass[TableHandView])
        ~> dynRenderR( (p,routerCtl) => PageDuplicateHand(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards" / string("[a-zA-Z0-9]+") ).caseClass[TableBoardView])
        ~> dynRenderR( (p,routerCtl) => PageBoard(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "teams").caseClass[TableTeamByRoundView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "editteams").caseClass[TableTeamByRoundEditView])
        ~> dynRenderR( (p,routerCtl) => PageTableTeams(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "game").caseClass[TableRoundScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+") / "round" / int / "boards").caseClass[TableRoundAllBoardView])
        ~> dynRenderR( (p,routerCtl) => PageAllBoards(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+")).caseClass[TableView])
        ~> dynRenderR( (p,routerCtl) => PageTable(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+") / "table" ).caseClass[AllTableView])
        ~> dynRenderR( (p,routerCtl) => PageAllTables(routerCtl,p) )
      | dynamicRouteCT( ("match" / string("[a-zA-Z0-9]+" )).caseClass[CompleteScoreboardView])
        ~> dynRenderR( (p,routerCtl) => PageScoreboard(routerCtl,p) )

      | staticRoute( "new", NewDuplicateView )
        ~> renderR( routerCtl => PageNewDuplicate(routerCtl,NewDuplicateView) )

      | staticRoute( "select", SelectMatchView )
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
