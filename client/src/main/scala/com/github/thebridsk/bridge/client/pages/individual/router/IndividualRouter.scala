package com.github.thebridsk.bridge.client.pages.individual.router

import japgolly.scalajs.react.extra.router.{RouterConfigDsl, RouterCtl, _}
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
// import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageBoardSets
// import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageMovements
import com.github.thebridsk.bridge.client.routes.Module
import com.github.thebridsk.bridge.client.routes.TestBridgeRouter
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import japgolly.scalajs.react.vdom.TagMod
import scala.scalajs.js.URIUtils
import com.github.thebridsk.bridge.client.routes.BridgeRouterBaseWithLogging
// import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageEditBoardSet
// import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageEditMovement
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective
import IndividualDuplicateViewPerspective._
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.client.pages.individual.PageScoreboard
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.client.pages.individual.PageSummary
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule.PlayDuplicate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.{CompleteScoreboardView => DCompleteScoreboardView}
import com.github.thebridsk.bridge.client.pages.individual.pages.PageNewDuplicate

object IndividualDuplicateModule extends Module {
  case class PlayIndividualDuplicate(m: IndividualDuplicatePage) extends AppPage

  def verifyPages(): List[AppPage] =
    IndividualDuplicateRouter.verifyPages
      .map(p => PlayIndividualDuplicate(p).asInstanceOf[AppPage])
      .toList

  def routes(): RoutingRule[AppPage, Unit] = {
    IndividualDuplicateRouter.routes
      .prefixPath_/("#individual")
      .pmap[AppPage](PlayIndividualDuplicate) { case PlayIndividualDuplicate(m) => m } |
      IndividualDuplicateRouter.importRoutes
        .prefixPath_/("#import")
        .pmap[AppPage](PlayIndividualDuplicate) { case PlayIndividualDuplicate(m) => m }
  }

}

sealed trait IndividualDuplicatePage
object IndividualDuplicateRouter {

  val logger: Logger = Logger("bridge.DuplicateRouter")

  /**
    * A BridgeRouter for testing
    * @param base the base url, example: http://localhost:8080/html/index-fastopt.html
    */
  abstract class TestDuplicatePageBridgeRouter(base: BaseUrl)
      extends TestBridgeRouter[IndividualDuplicatePage](base) {
    def refresh: Callback
    def set(page: IndividualDuplicatePage): Callback

    def pathFor(target: IndividualDuplicatePage): Path =
      target match {
        case SummaryView => Path("#/duplicates")
        // TODO implement other pages
        case _ => ???
      }
  }

  class IndividualDuplicateRouterWithLogging(ctl: RouterCtl[IndividualDuplicatePage])
      extends BridgeRouterBaseWithLogging[IndividualDuplicatePage](ctl) {
    override def home: TagMod = IndividualDuplicateModule.gotoAppHome()

    override def toHome: Unit = IndividualDuplicateModule.toHome
    override def toAbout: Unit = IndividualDuplicateModule.toAbout

    override def toInfo: Unit = {
      IndividualDuplicateModule.toInfo
    }

    override def toRootPage(page: AppPage): Unit =
      IndividualDuplicateModule.toRootPage(page)

  }

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter(
      ctl: RouterCtl[IndividualDuplicatePage]
  ): BridgeRouter[IndividualDuplicatePage] =
    new IndividualDuplicateRouterWithLogging(ctl)

  trait BaseBoardView extends IndividualDuplicatePage {
    val sdupid: String
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    val sboardid: String
    def boardid: IndividualBoard.Id = IndividualBoard.id(sboardid)
    def toScoreboardView: BaseScoreboardView
    def toHandView(handid: IndividualDuplicateHand.Id): BaseHandView
    def toBoardView(bid: IndividualBoard.Id): BaseBoardView
    def toAllBoardsView: BaseAllBoardsView
  }

  trait BaseBoardViewWithPerspective extends BaseBoardView {
    def getPerspective: IndividualDuplicateViewPerspective
  }

  trait BaseScoreboardView extends IndividualDuplicatePage {
    val sdupid: String
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    def toBoardView(id: IndividualBoard.Id): BaseBoardView
    def toAllBoardsView: BaseAllBoardsView
  }

  trait BaseAllBoardsView extends IndividualDuplicatePage {
    val sdupid: String
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    def toBoardView(id: IndividualBoard.Id): BaseBoardView
    def toScoreboardView: BaseScoreboardView
    def toHandView(boardid: IndividualBoard.Id, handid: IndividualDuplicateHand.Id): BaseHandView
  }

  trait BaseScoreboardViewWithPerspective extends BaseScoreboardView {
    def getPerspective: IndividualDuplicateViewPerspective
  }

  trait BaseAllBoardsViewWithPerspective extends BaseAllBoardsView {
    def getPerspective: IndividualDuplicateViewPerspective
  }

  trait BaseHandView extends IndividualDuplicatePage {
    val sdupid: String
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    val sboardid: String
    def boardid: IndividualBoard.Id = IndividualBoard.id(sboardid)
    val shandid: String
    def handid: IndividualDuplicateHand.Id = IndividualDuplicateHand.id(shandid)
    def toBoardView: BaseBoardView
    def getPerspective: Option[IndividualDuplicateViewPerspective]
  }

  trait SummaryViewBase extends IndividualDuplicatePage {
    def getScoreboardPage(
        dupid: MatchDuplicate.Id
    ): AppPage
    def getIndividualScoreboardPage(
        dupid: IndividualDuplicate.Id
    ): BaseScoreboardViewWithPerspective
    def getDuplicateResultPage(
        dupid: MatchDuplicateResult.Id
    ): DuplicateResultViewBase
    def getImportId: Option[String] = None
  }
  case object SummaryView extends SummaryViewBase {
    def getScoreboardPage(
        dupid: MatchDuplicate.Id
    ): AppPage = PlayDuplicate(DCompleteScoreboardView(dupid.id))
    def getIndividualScoreboardPage(
        dupid: IndividualDuplicate.Id
    ): BaseScoreboardViewWithPerspective = CompleteScoreboardView(dupid.id)
    def getDuplicateResultPage(
        dupid: MatchDuplicateResult.Id
    ): DuplicateResultViewBase = DuplicateResultView(dupid.id)
  }

//	object creation impossible.
//  Missing implementations for 2 members. Stub implementations follow:
//    def getIndividualScoreboardPage(dupid: com.github.thebridsk.bridge.data.IndividualDuplicate.Id): com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseScoreboardViewWithPerspective = ???
//    def getScoreboardPage(dupid: com.github.thebridsk.bridge.data.MatchDuplicate.Id): com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseScoreboardViewWithPerspective = ???

  case object StatsView extends IndividualDuplicatePage
  case object NewDuplicateView extends IndividualDuplicatePage
  case object SelectMatchView extends IndividualDuplicatePage
  case object SuggestionView extends IndividualDuplicatePage

  case class NamesView(sdupid: String) extends IndividualDuplicatePage {
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
  }

  case object FinishedScoreboardsView0 extends IndividualDuplicatePage

  /**
    * @param dupids - a comma separated list of duplicate ids
    */
  case class FinishedScoreboardsView(dupids: String) extends IndividualDuplicatePage {
    def getIds: List[DuplicateSummary.Id] =
      dupids
        .split(",")
        .toList
        .map(_.trim)
        .filter(_.length() > 0)
        .map(DuplicateSummary.id(_))
  }

  case class DuplicateBoardSetView(sdupid: String) extends IndividualDuplicatePage {
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    def toScoreboard: CompleteScoreboardView = CompleteScoreboardView(sdupid)
  }

  case class CompleteScoreboardView(sdupid: String)
      extends BaseScoreboardViewWithPerspective {
    def toBoardView(id: IndividualBoard.Id): CompleteBoardView =
      CompleteBoardView(sdupid, id.id)
    def toAllBoardsView: CompleteAllBoardView = CompleteAllBoardView(sdupid)
    def getPerspective: IndividualDuplicateViewPerspective = PerspectiveComplete
  }

  case class FinishedScoreboardView(sdupid: String)
      extends BaseScoreboardViewWithPerspective {
    def toBoardView(id: IndividualBoard.Id): CompleteBoardView =
      CompleteBoardView(sdupid, id.id)
    def toAllBoardsView: CompleteAllBoardView = CompleteAllBoardView(sdupid)
    def getPerspective: IndividualDuplicateViewPerspective = PerspectiveComplete
  }

  trait DuplicateResultViewBase extends IndividualDuplicatePage {
    val sdupid: String
    def dupid: MatchDuplicateResult.Id = MatchDuplicateResult.id(sdupid)
  }
  case class DuplicateResultView(sdupid: String) extends DuplicateResultViewBase

  case class DuplicateResultEditView(sdupid: String)
      extends DuplicateResultViewBase

  case class CompleteAllBoardView(sdupid: String)
      extends BaseAllBoardsViewWithPerspective {
    def toBoardView(id: IndividualBoard.Id): CompleteBoardView =
      CompleteBoardView(sdupid, id.id)
    def toScoreboardView: CompleteScoreboardView =
      CompleteScoreboardView(sdupid)
    def getPerspective: IndividualDuplicateViewPerspective = PerspectiveComplete
    def toHandView(boardid: IndividualBoard.Id, handid: IndividualDuplicateHand.Id): CompleteHandView =
      CompleteHandView(sdupid, boardid.id, handid.id)
  }
  case class CompleteBoardView(sdupid: String, sboardid: String)
      extends BaseBoardViewWithPerspective {
    def toScoreboardView: CompleteScoreboardView =
      CompleteScoreboardView(sdupid)
    def getPerspective: IndividualDuplicateViewPerspective = PerspectiveComplete
    def toHandView(handid: IndividualDuplicateHand.Id): CompleteHandView =
      CompleteHandView(sdupid, sboardid, handid.id)
    def toBoardView(bid: IndividualBoard.Id): CompleteBoardView =
      CompleteBoardView(sdupid, bid.id)
    def toAllBoardsView: CompleteAllBoardView = CompleteAllBoardView(sdupid)
  }
  case class CompleteHandView(sdupid: String, sboardid: String, shandid: String)
      extends BaseHandView {
    def toBoardView: CompleteBoardView = CompleteBoardView(sdupid, sboardid)
    def getPerspective: Option[IndividualDuplicateViewPerspective] =
      Some(PerspectiveComplete)
  }

  case class DirectorScoreboardView(sdupid: String)
      extends BaseScoreboardViewWithPerspective {
    def toBoardView(id: IndividualBoard.Id): DirectorBoardView =
      DirectorBoardView(sdupid, id.id)
    def toAllBoardsView: DirectorAllBoardView = DirectorAllBoardView(sdupid)
    def getPerspective: IndividualDuplicateViewPerspective = PerspectiveDirector
  }

  case class DirectorAllBoardView(sdupid: String)
      extends BaseAllBoardsViewWithPerspective {
    def toBoardView(id: IndividualBoard.Id): DirectorBoardView =
      DirectorBoardView(sdupid, id.id)
    def toScoreboardView: DirectorScoreboardView =
      DirectorScoreboardView(sdupid)
    def getPerspective: IndividualDuplicateViewPerspective = PerspectiveDirector
    def toHandView(boardid: IndividualBoard.Id, handid: IndividualDuplicateHand.Id): DirectorHandView =
      DirectorHandView(sdupid, boardid.id, handid.id)
  }
  case class DirectorBoardView(sdupid: String, sboardid: String)
      extends BaseBoardViewWithPerspective {
    def toScoreboardView: DirectorScoreboardView =
      DirectorScoreboardView(sdupid)
    def getPerspective: IndividualDuplicateViewPerspective = PerspectiveDirector
    def toHandView(handid: IndividualDuplicateHand.Id): DirectorHandView =
      DirectorHandView(sdupid, sboardid, handid.id)
    def toBoardView(bid: IndividualBoard.Id): DirectorBoardView =
      DirectorBoardView(sdupid, bid.id)
    def toAllBoardsView: DirectorAllBoardView = DirectorAllBoardView(sdupid)
  }
  case class DirectorHandView(sdupid: String, sboardid: String, shandid: String)
      extends BaseHandView {
    def toBoardView: DirectorBoardView = DirectorBoardView(sdupid, sboardid)
    def getPerspective: Option[IndividualDuplicateViewPerspective] =
      Some(PerspectiveDirector)
  }

  case class TableView(sdupid: String, stableid: String) extends IndividualDuplicatePage {
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    def tableid: Table.Id = Table.id(stableid)
    def toBoardView(round: Int, id: IndividualBoard.Id): TableBoardView =
      TableBoardView(sdupid, stableid, round, id.id)
    def toRoundView(roundid: Int): TableRoundScoreboardView =
      TableRoundScoreboardView(sdupid, stableid, roundid)

    def toTableTeamView(roundid: Int): TableTeamByRoundView =
      TableTeamByRoundView(sdupid, stableid, roundid)
    def toTableTeamView(roundid: Int, boardid: IndividualBoard.Id): TableTeamByBoardView =
      TableTeamByBoardView(sdupid, stableid, roundid, boardid.id)
  }

  case class AllTableView(sdupid: String) extends IndividualDuplicatePage {
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    def toTableView(tableid: Table.Id): TableView =
      TableView(sdupid, tableid.id)
    def toBoardView(
        tableid: Table.Id,
        round: Int,
        id: IndividualBoard.Id
    ): TableBoardView = TableBoardView(sdupid, tableid.id, round, id.id)
    def toRoundView(tableid: Table.Id, roundid: Int): TableRoundScoreboardView =
      TableRoundScoreboardView(sdupid, tableid.id, roundid)
  }

  trait TableTeamView extends IndividualDuplicatePage {
    val sdupid: String
    def dupid: IndividualDuplicate.Id = IndividualDuplicate.id(sdupid)
    val stableid: String
    def tableid: Table.Id = Table.id(stableid)
    val round: Int

    def toNextView: IndividualDuplicatePage
    def toTableView: TableView
  }

  case class TableTeamByRoundView(sdupid: String, stableid: String, round: Int)
      extends TableTeamView {
    def toNextView: TableRoundScoreboardView =
      TableRoundScoreboardView(sdupid, stableid, round)
    def toTableView: TableView = TableView(sdupid, stableid)

  }

  case class TableTeamByBoardView(
      sdupid: String,
      stableid: String,
      round: Int,
      sboardid: String
  ) extends TableTeamView {
    def boardid: IndividualBoard.Id = IndividualBoard.id(sboardid)
    def toNextView: TableBoardView =
      TableBoardView(sdupid, stableid, round, sboardid)
    def toTableView: TableView = TableView(sdupid, stableid)
  }

  case class TableRoundScoreboardView(
      sdupid: String,
      stableid: String,
      round: Int
  ) extends BaseScoreboardViewWithPerspective {
    def tableid: Table.Id = Table.id(stableid)
    def toBoardView(boardid: IndividualBoard.Id): TableBoardView =
      TableBoardView(sdupid, stableid, round, boardid.id)
    def toAllBoardsView: TableRoundAllBoardView =
      TableRoundAllBoardView(sdupid, stableid, round)
    def toHandView(boardid: IndividualBoard.Id, handid: IndividualDuplicateHand.Id): TableHandView =
      TableHandView(sdupid, stableid, round, boardid.id, handid.id)
    def toTableView: TableView = TableView(sdupid, stableid)
    def getPerspective: IndividualDuplicateViewPerspective =
      PerspectiveTable(tableid, round)
  }
  case class TableRoundAllBoardView(
      sdupid: String,
      stableid: String,
      round: Int
  ) extends BaseAllBoardsViewWithPerspective {
    def tableid: Table.Id = Table.id(stableid)
    def toBoardView(boardid: IndividualBoard.Id): TableBoardView =
      TableBoardView(sdupid, stableid, round, boardid.id)
    def toScoreboardView: TableRoundScoreboardView =
      TableRoundScoreboardView(sdupid, stableid, round)
    def getPerspective: IndividualDuplicateViewPerspective =
      PerspectiveTable(tableid, round)
    def toHandView(boardid: IndividualBoard.Id, handid: IndividualDuplicateHand.Id): TableHandView =
      TableHandView(sdupid, stableid, round, boardid.id, handid.id)
  }
  case class TableBoardView(
      sdupid: String,
      stableid: String,
      round: Int,
      sboardid: String
  ) extends BaseBoardViewWithPerspective {
    def tableid: Table.Id = Table.id(stableid)
    def toTableView: TableView = TableView(sdupid, stableid)
    def toScoreboardView: TableRoundScoreboardView =
      TableRoundScoreboardView(sdupid, stableid, round)
    def toHandView(handid: IndividualDuplicateHand.Id): TableHandView =
      TableHandView(sdupid, stableid, round, sboardid, handid.id)
    def toBoardView(bid: IndividualBoard.Id): TableBoardView =
      TableBoardView(sdupid, stableid, round, bid.id)
    def toAllBoardsView: TableRoundAllBoardView =
      TableRoundAllBoardView(sdupid, stableid, round)
    def getPerspective: IndividualDuplicateViewPerspective =
      PerspectiveTable(tableid, round)
  }
  case class TableHandView(
      sdupid: String,
      stableid: String,
      round: Int,
      sboardid: String,
      shandid: String
  ) extends BaseHandView {
    def tableid: Table.Id = Table.id(stableid)
    def toBoardView: TableBoardView =
      TableBoardView(sdupid, stableid, round, sboardid)
    def getPerspective: Option[IndividualDuplicateViewPerspective] =
      Some(PerspectiveTable(tableid, round))
  }

  case object BoardSetSummaryView extends IndividualDuplicatePage

  case class BoardSetView(sdisplay: String) extends IndividualDuplicatePage {
    def display: BoardSet.Id = BoardSet.id(sdisplay)
  }

  case object BoardSetNewView extends IndividualDuplicatePage
  case class BoardSetEditView(sdisplay: String) extends IndividualDuplicatePage {
    def display: BoardSet.Id = BoardSet.id(sdisplay)
  }

  case object MovementSummaryView extends IndividualDuplicatePage

  case class MovementView(sdisplay: String) extends IndividualDuplicatePage {
    def display: IndividualMovement.Id = IndividualMovement.id(sdisplay)
  }

  case object MovementNewView extends IndividualDuplicatePage
  case class MovementEditView(sdisplay: String) extends IndividualDuplicatePage {
    def display: IndividualMovement.Id = IndividualMovement.id(sdisplay)
  }

  case class ImportSummaryView(importId: String) extends SummaryViewBase {
    def getScoreboardPage(
        dupid: MatchDuplicate.Id
    ): AppPage =  PlayDuplicate(DCompleteScoreboardView(dupid.id))
    def getIndividualScoreboardPage(
        dupid: IndividualDuplicate.Id
    ): BaseScoreboardViewWithPerspective = CompleteScoreboardView(dupid.id)
    def getDuplicateResultPage(
        dupid: MatchDuplicateResult.Id
    ): DuplicateResultViewBase = DuplicateResultView(dupid.id)
    def getDecodedId: String = URIUtils.decodeURI(importId)
    override def getImportId: Option[String] = Some(getDecodedId)
  }

  val verifyPages: List[IndividualDuplicatePage] =
    // SummaryView ::
    // StatsView ::
    NewDuplicateView ::
    // SelectMatchView ::
    // SuggestionView ::
    // NamesView("M1") ::
    // DuplicateBoardSetView("M1") ::
    CompleteScoreboardView("M1") ::
    FinishedScoreboardView("M1") ::
    // FinishedScoreboardsView0 ::
    // FinishedScoreboardsView("M1,M2") ::
    // CompleteAllBoardView("M1") ::
    DirectorScoreboardView("M1") ::
    // DirectorAllBoardView("M1") ::
    // DuplicateResultView("E1") ::
    // DuplicateResultEditView("E1") ::
    // AllTableView("M1") ::
    // TableView("M1", "1") ::
    TableRoundScoreboardView("M1", "1", 1) ::
    // TableRoundAllBoardView("M1", "1", 1) ::
    // TableHandView("M1", "1", 1, "1", "T1") ::
    // CompleteHandView("M1", "1", "T1") ::
    // DirectorHandView("M1", "1", "T1") ::
    // TableBoardView("M1", "1", 1, "1") ::
    // CompleteBoardView("M1", "1") ::
    // DirectorBoardView("M1", "1") ::
    // TableTeamByRoundView("M1", "1", 1) ::
    // TableTeamByBoardView("M1", "1", 1, "1") ::
    // BoardSetSummaryView ::
    // BoardSetView("ArmonkBoards") ::
    // BoardSetNewView ::
    // BoardSetEditView("ArmonkBoards") ::
    // MovementSummaryView ::
    // MovementView("2TablesArmonk") ::
    // MovementNewView ::
    // MovementEditView("2TablesArmonk") ::
    // ImportSummaryView("import.zip") ::
    Nil

  val routes: RoutingRule[IndividualDuplicatePage, Unit] =
    RouterConfigDsl[IndividualDuplicatePage].buildRule { dsl =>
      import dsl._

      (emptyRule
        // | staticRoute("stats", StatsView)
        //   ~> renderR(routerCtl => PageStats(routerCtl))
        // | staticRoute("suggestion", SuggestionView)
        //   ~> renderR(routerCtl => PageSuggestion(routerCtl))
        // | staticRoute("boardsets" / "#new", BoardSetNewView)
        //   ~> renderR(routerCtl => PageEditBoardSet(routerCtl, BoardSetNewView))
        // | dynamicRouteCT(
        //   ("boardsets" / string("[a-zA-Z0-9]+") / "edit")
        //     .caseClass[BoardSetEditView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageEditBoardSet(routerCtl, p))
        // | dynamicRouteCT(
        //   ("boardsets" / string("[a-zA-Z0-9]+")).caseClass[BoardSetView]
        // )
        //   ~> dynRenderR((p, routerCtl) =>
        //     PageBoardSets(routerCtl, SummaryView, Some(p.display))
        //   )
        // | staticRoute("boardsets", BoardSetSummaryView)
        //   ~> renderR(routerCtl => PageBoardSets(routerCtl, SummaryView, None))
        // | staticRoute("movements" / "#new", MovementNewView)
        //   ~> renderR(routerCtl => PageEditMovement(routerCtl, MovementNewView))
        // | dynamicRouteCT(
        //   ("movements" / string("[a-zA-Z0-9]+") / "edit")
        //     .caseClass[MovementEditView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageEditMovement(routerCtl, p))
        // | dynamicRouteCT(
        //   ("movements" / string("[a-zA-Z0-9]+")).caseClass[MovementView]
        // )
        //   ~> dynRenderR((p, routerCtl) =>
        //     PageMovements(routerCtl, SummaryView, Some(p.display))
        //   )
        // | staticRoute("movements", MovementSummaryView)
        //   ~> renderR(routerCtl => PageMovements(routerCtl, SummaryView, None))
        // | dynamicRouteCT(
        //   ("results" / string("[a-zA-Z0-9]+")).caseClass[DuplicateResultView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageDuplicateResult(routerCtl, p))
        // | dynamicRouteCT(
        //   ("results" / string("[a-zA-Z0-9]+") / "edit")
        //     .caseClass[DuplicateResultEditView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageDuplicateResultEdit(routerCtl, p))
        // | dynamicRouteCT(
        //   ("finished" / string("[a-zA-Z0-9,]*"))
        //     .caseClass[FinishedScoreboardsView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageFinishedScoreboards(routerCtl, p))
        // | staticRoute("finished", FinishedScoreboardsView0)
        //   ~> renderR(routerCtl =>
        //     PageFinishedScoreboards(routerCtl, FinishedScoreboardsView(""))
        //   )
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "names").caseClass[NamesView]
        // )
        //   ~> dynRenderR((p, routerCtl) =>
        //     PageNames(routerCtl, p, CompleteScoreboardView(p.sdupid))
        //   )
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "boardset")
        //     .caseClass[DuplicateBoardSetView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageBoardSet(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "boards" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "hands" / string("[a-zA-Z0-9]+")).caseClass[CompleteHandView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageDuplicateHand(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "boards" / string("[a-zA-Z0-9]+"))
        //     .caseClass[CompleteBoardView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageBoard(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "boards")
        //     .caseClass[CompleteAllBoardView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageAllBoards(routerCtl, p))

        | dynamicRouteCT(
          ("match" / string("[a-zA-Z0-9]+") / "finished")
            .caseClass[FinishedScoreboardView]
        )
          ~> dynRenderR((p, routerCtl) => PageScoreboard(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "director" / "boards" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "hands" / string("[a-zA-Z0-9]+")).caseClass[DirectorHandView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageDuplicateHand(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "director" / "boards" / string(
        //     "[a-zA-Z0-9]+"
        //   )).caseClass[DirectorBoardView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageBoard(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "director" / "boards")
        //     .caseClass[DirectorAllBoardView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageAllBoards(routerCtl, p))
        | dynamicRouteCT(
          ("match" / string("[a-zA-Z0-9]+") / "director")
            .caseClass[DirectorScoreboardView]
        )
          ~> dynRenderR((p, routerCtl) => PageScoreboard(routerCtl, p))

        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "table" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "round" / int / "boards" / string("[a-zA-Z0-9]+") / "teams")
        //     .caseClass[TableTeamByBoardView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageTableTeams(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "table" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "round" / int / "boards" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "hands" / string("[a-zA-Z0-9]+")).caseClass[TableHandView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageDuplicateHand(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "table" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "round" / int / "boards" / string("[a-zA-Z0-9]+"))
        //     .caseClass[TableBoardView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageBoard(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "table" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "round" / int / "teams").caseClass[TableTeamByRoundView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageTableTeams(routerCtl, p))
        | dynamicRouteCT(
          ("match" / string("[a-zA-Z0-9]+") / "table" / string(
            "[a-zA-Z0-9]+"
          ) / "round" / int / "game").caseClass[TableRoundScoreboardView]
        )
          ~> dynRenderR((p, routerCtl) => PageScoreboard(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "table" / string(
        //     "[a-zA-Z0-9]+"
        //   ) / "round" / int / "boards").caseClass[TableRoundAllBoardView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageAllBoards(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "table" / string("[a-zA-Z0-9]+"))
        //     .caseClass[TableView]
        // )
        //   ~> dynRenderR((p, routerCtl) => PageTable(routerCtl, p))
        // | dynamicRouteCT(
        //   ("match" / string("[a-zA-Z0-9]+") / "table").caseClass[AllTableView]
        // )
          // ~> dynRenderR((p, routerCtl) => PageAllTables(routerCtl, p))
        | dynamicRouteCT(
          ("match" / string("[a-zA-Z0-9]+")).caseClass[CompleteScoreboardView]
        )
          ~> dynRenderR((p, routerCtl) => PageScoreboard(routerCtl, p))

        | staticRoute("new", NewDuplicateView)
          ~> renderR(routerCtl => PageNewDuplicate(routerCtl, NewDuplicateView))

        // | staticRoute("select", SelectMatchView)
        //   ~> renderR(routerCtl => PageSelectMatch(routerCtl, SelectMatchView))

        | staticRoute(root, SummaryView)
          ~> renderR(routerCtl => PageSummary(routerCtl, SummaryView))
      )
    }

  val importRoutes: RoutingRule[IndividualDuplicatePage, Unit] =
    RouterConfigDsl[IndividualDuplicatePage].buildRule { dsl =>
      import dsl._

      (emptyRule
        | dynamicRouteCT(
          (string(".+") / "individual").caseClass[ImportSummaryView]
        )
          ~> dynRenderR((p, routerCtl) => PageSummary(routerCtl, p, -1))
      )
    }

}
