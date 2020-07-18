package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.client.routes.Module
import com.github.thebridsk.bridge.client.routes.BridgeRouterBase
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import scala.scalajs.js.URIUtils
import com.github.thebridsk.bridge.data.MatchChicago

object ChicagoModule extends Module {
  case class PlayChicago2(m: ChicagoPage) extends AppPage

  def verifyPages(): List[AppPage] =
    ChicagoRouter.verifyPages
      .map(p => PlayChicago2(p).asInstanceOf[AppPage])
      .toList

  def routes() =
    ChicagoRouter.routes.prefixPath_/("#chicago").pmap[AppPage](PlayChicago2) {
      case PlayChicago2(m) => m
    } |
      ChicagoRouter.importRoutes
        .prefixPath_/("#import")
        .pmap[AppPage](PlayChicago2) { case PlayChicago2(m) => m }

}

sealed trait ChicagoPage

object ChicagoRouter {

  val logger = Logger("bridge.ChicagoRouter")

  trait ListViewBase extends ChicagoPage

  object ListView extends ListViewBase
  case class ImportListView(importId: String) extends ListViewBase {
    def getDecodedId = URIUtils.decodeURI(importId)
  }

  case class SummaryView(schiid: String) extends ChicagoPage {
    def chiid = MatchChicago.id(schiid)
    def toRoundView(round: Int) = RoundView(schiid, round)
    def toNamesView(round: Int) = NamesView(schiid, round)
    def toHandView(round: Int, hand: Int) = HandView(schiid, round, hand)
    def toEditNamesView = EditNamesView(schiid)
  }
  case class EditNamesView(schiid: String) extends ChicagoPage {
    def chiid = MatchChicago.id(schiid)
    def toSummaryView = SummaryView(schiid)
    def toRoundView(round: Int) = RoundView(schiid, round)
    def toNamesView(round: Int) = NamesView(schiid, round)
    def toHandView(round: Int, hand: Int) = HandView(schiid, round, hand)
  }
  case class RoundView(schiid: String, round: Int) extends ChicagoPage {
    def chiid = MatchChicago.id(schiid)
    def toSummaryView = SummaryView(schiid)
    def toNamesView(round: Int) = NamesView(schiid, round)
    def toHandView(hand: Int) = HandView(schiid, round, hand)
    def toEditNamesView = EditNamesView(schiid)
  }
  case class NamesView(schiid: String, round: Int) extends ChicagoPage {
    def chiid = MatchChicago.id(schiid)
    def toSummaryView = SummaryView(schiid)
    def toRoundView = RoundView(schiid, round)
    def toHandView(hand: Int) = HandView(schiid, round, hand)
  }
  case class HandView(schiid: String, round: Int, hand: Int)
      extends ChicagoPage {
    def chiid = MatchChicago.id(schiid)
    def toSummaryView = SummaryView(schiid)
    def toNamesView = NamesView(schiid, round)
    def toRoundView = RoundView(schiid, round)
  }

  val verifyPages = ListView ::
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

  val routes = RouterConfigDsl[ChicagoPage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | dynamicRouteCT(
        (string("[a-zA-Z0-9]+") / "names").caseClass[EditNamesView]
      )
        ~> dynRenderR((p, routerCtl) => PageEditNames(p, routerCtl))
      | dynamicRouteCT(
        (string("[a-zA-Z0-9]+") / "rounds" / int / "names").caseClass[NamesView]
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
        ~> renderR(routerCtl => PageChicagoList(routerCtl, ListView))
    )
  }

  val importRoutes = RouterConfigDsl[ChicagoPage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | dynamicRouteCT((string(".+") / "chicago").caseClass[ImportListView])
        ~> dynRenderR((p, routerCtl) => PageChicagoList(routerCtl, p)))
  }

}
