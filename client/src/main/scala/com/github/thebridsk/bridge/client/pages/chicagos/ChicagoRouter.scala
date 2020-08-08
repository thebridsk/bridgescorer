package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.routes.BridgeRouterBase
import com.github.thebridsk.bridge.client.routes.Module
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^._
import scala.scalajs.js.URIUtils
import japgolly.scalajs.react.extra.router.RoutingRule

object ChicagoModule extends Module {
  case class PlayChicago2(m: ChicagoPage) extends AppPage

  def verifyPages(): List[AppPage] =
    ChicagoRouter.verifyPages
      .map(p => PlayChicago2(p).asInstanceOf[AppPage])
      .toList

  def routes(): RoutingRule[AppPage, Unit] =
    ChicagoRouter.routes.prefixPath_/("#chicago").pmap[AppPage](PlayChicago2) {
      case PlayChicago2(m) => m
    } |
      ChicagoRouter.importRoutes
        .prefixPath_/("#import")
        .pmap[AppPage](PlayChicago2) { case PlayChicago2(m) => m }

}

sealed trait ChicagoPage

object ChicagoRouter {

  val logger: Logger = Logger("bridge.ChicagoRouter")

  trait ListViewBase extends ChicagoPage

  object ListView extends ListViewBase
  case class ImportListView(importId: String) extends ListViewBase {
    def getDecodedId: String = URIUtils.decodeURI(importId)
  }

  case class SummaryView(schiid: String) extends ChicagoPage {
    def chiid: MatchChicago.Id = MatchChicago.id(schiid)

    /**
      * @param round 0 based
      * @return
      */
    def toRoundView(round: Int): RoundView = RoundView(schiid, round)

    /**
      * @param round 0 based
      * @return
      */
    def toNamesView(round: Int): NamesView = NamesView(schiid, round)

    /**
      * @param round 0 based
      * @param hand 0 based
      * @return
      */
    def toHandView(round: Int, hand: Int): HandView =
      HandView(schiid, round, hand)
    def toEditNamesView: EditNamesView = EditNamesView(schiid)
  }
  case class EditNamesView(schiid: String) extends ChicagoPage {
    def chiid: MatchChicago.Id = MatchChicago.id(schiid)
    def toSummaryView: SummaryView = SummaryView(schiid)

    /**
      * @param round 0 based
      * @param hand 0 based
      * @return
      */
    def toRoundView(round: Int): RoundView = RoundView(schiid, round)

    /**
      * @param round 0 based
      * @return
      */
    def toNamesView(round: Int): NamesView = NamesView(schiid, round)

    /**
      * @param round 0 based
      * @return
      */
    def toHandView(round: Int, hand: Int): HandView =
      HandView(schiid, round, hand)
  }
  case class RoundView(schiid: String, round: Int) extends ChicagoPage {
    def chiid: MatchChicago.Id = MatchChicago.id(schiid)
    def toSummaryView: SummaryView = SummaryView(schiid)

    /**
      * @param round 0 based
      * @return
      */
    def toNamesView(round: Int): NamesView = NamesView(schiid, round)

    /**
      * @param hand 0 based
      * @return
      */
    def toHandView(hand: Int): HandView = HandView(schiid, round, hand)
    def toEditNamesView: EditNamesView = EditNamesView(schiid)
  }
  case class NamesView(schiid: String, round: Int) extends ChicagoPage {
    def chiid: MatchChicago.Id = MatchChicago.id(schiid)
    def toSummaryView: SummaryView = SummaryView(schiid)
    def toRoundView: RoundView = RoundView(schiid, round)

    /**
      * @param hand 0 based
      * @return
      */
    def toHandView(hand: Int): HandView = HandView(schiid, round, hand)
  }
  case class HandView(schiid: String, round: Int, hand: Int)
      extends ChicagoPage {
    def chiid: MatchChicago.Id = MatchChicago.id(schiid)
    def toSummaryView: SummaryView = SummaryView(schiid)
    def toNamesView: NamesView = NamesView(schiid, round)
    def toRoundView: RoundView = RoundView(schiid, round)
  }

  val verifyPages: List[ChicagoPage] = ListView ::
    ImportListView("import.zip") ::
    SummaryView("C1") ::
    RoundView("C1", 1) ::
    NamesView("C1", 1) ::
    HandView("C1", 1, 1) ::
    Nil

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter(
      ctl: RouterCtl[ChicagoPage]
  ): BridgeRouter[ChicagoPage] =
    new BridgeRouterBase[ChicagoPage](ctl) {
      override def home: TagMod = ChicagoModule.gotoAppHome()

      override def toHome: Unit = ChicagoModule.toHome
      override def toAbout: Unit = ChicagoModule.toAbout

      override def toInfo: Unit = {
        ChicagoModule.toInfo
      }

      override def toRootPage(page: AppPage): Unit =
        ChicagoModule.toRootPage(page)
    }

  val routes: RoutingRule[ChicagoPage, Unit] =
    RouterConfigDsl[ChicagoPage].buildRule { dsl =>
      import dsl._

      (emptyRule
        | dynamicRouteCT(
          (string("[a-zA-Z0-9]+") / "names").caseClass[EditNamesView]
        )
          ~> dynRenderR((p, routerCtl) => PageEditNames(p, routerCtl))
        | dynamicRouteCT(
          (string("[a-zA-Z0-9]+") / "rounds" / int / "names")
            .caseClass[NamesView]
        )
          ~> dynRenderR((p, routerCtl) => PagePlayers(p, routerCtl))
        | dynamicRouteCT(
          (string("[a-zA-Z0-9]+") / "rounds" / int / "hands" / int)
            .caseClass[HandView]
        )
          ~> dynRenderR((p, routerCtl) => PageChicagoHand(p, routerCtl))
      // | dynamicRouteCT(
      //   (string("[a-zA-Z0-9]+") / "rounds" / int / "hands").caseClass[RoundView]
      // )
      //   ~> dynRenderR((p, routerCtl) => PageSummary(p, routerCtl))
        | dynamicRouteCT(
          (string("[a-zA-Z0-9]+") / "rounds" / int).caseClass[RoundView]
        )
          ~> dynRenderR((p, routerCtl) => PageSummary(p, routerCtl))
      // | dynamicRouteCT(
      //   (string("[a-zA-Z0-9]+") / "rounds").caseClass[SummaryView]
      // )
      //   ~> dynRenderR((p, routerCtl) => PageSummary(p, routerCtl))
        | dynamicRouteCT(string("[a-zA-Z0-9]+").caseClass[SummaryView])
          ~> dynRenderR((p, routerCtl) => PageSummary(p, routerCtl))
        | staticRoute(root, ListView)
          ~> renderR(routerCtl => PageChicagoList(routerCtl, ListView)))
    }

  val importRoutes: RoutingRule[ChicagoPage, Unit] =
    RouterConfigDsl[ChicagoPage].buildRule { dsl =>
      import dsl._

      (emptyRule
        | dynamicRouteCT((string(".+") / "chicago").caseClass[ImportListView])
          ~> dynRenderR((p, routerCtl) => PageChicagoList(routerCtl, p)))
    }

}
