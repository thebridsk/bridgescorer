package com.example.pages.chicagos

import utils.logging.Logger
import japgolly.scalajs.react._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.extra.router.{ RouterConfigDsl, RouterCtl, _ }
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.router.StaticDsl.Rule
import com.example.routes.AppRouter.AppPage
import com.example.routes.Module
import com.example.routes.BridgeRouterBase
import com.example.routes.BridgeRouter
import scala.scalajs.js.URIUtils

object ChicagoModule extends Module {
  case class PlayChicago2( m: ChicagoPage ) extends AppPage

  def verifyPages(): List[AppPage] = ChicagoRouter.verifyPages.map( p => PlayChicago2(p).asInstanceOf[AppPage]).toList

  def routes(): Rule[AppPage] =
    ChicagoRouter.routes.prefixPath_/("#chicago").pmap[AppPage](PlayChicago2){ case PlayChicago2(m) => m } |
    ChicagoRouter.importRoutes.prefixPath_/("#import").pmap[AppPage](PlayChicago2){ case PlayChicago2(m) => m }

  override
  def canRender(selectedPage: Resolution[AppPage]): Boolean = selectedPage.page.isInstanceOf[PlayChicago2]

}

sealed trait ChicagoPage

object ChicagoRouter {

  val logger = Logger("bridge.ChicagoRouter")

  trait ListViewBase extends ChicagoPage

  object ListView extends ListViewBase
  case class ImportListView( importId: String ) extends ListViewBase {
    def getDecodedId = URIUtils.decodeURI(importId)
  }

  case class SummaryView( chiid: String ) extends ChicagoPage {
    def toRoundView( round: Int ) = RoundView(chiid,round)
    def toNamesView( round: Int ) = NamesView(chiid,round)
    def toHandView( round: Int, hand: Int ) = HandView(chiid,round,hand)
  }
  case class RoundView( chiid: String, round: Int ) extends ChicagoPage {
    def toSummaryView() = SummaryView(chiid)
    def toNamesView( round: Int ) = NamesView(chiid,round)
    def toHandView( hand: Int ) = HandView(chiid,round,hand)
  }
  case class NamesView( chiid: String, round: Int ) extends ChicagoPage {
    def toSummaryView() = SummaryView(chiid)
    def toRoundView() = RoundView(chiid,round)
    def toHandView( hand: Int ) = HandView(chiid,round,hand)
  }
  case class HandView( chiid: String, round: Int, hand: Int ) extends ChicagoPage {
    def toSummaryView() = SummaryView(chiid)
    def toNamesView() = NamesView(chiid,round)
    def toRoundView() = RoundView(chiid,round)
  }

  val verifyPages = ListView::
                    ImportListView("import.zip")::
                    SummaryView("C1")::
                    RoundView("C1", 1)::
                    NamesView("C1", 1)::
                    HandView("C1", 1, 1)::
                    Nil

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter( ctl: RouterCtl[ChicagoPage] ): BridgeRouter[ChicagoPage] =
    new BridgeRouterBase[ChicagoPage](ctl) {
        override
        def home: TagMod = ChicagoModule.gotoAppHome()

        override
        def toHome: Unit = ChicagoModule.toHome
        override
        def toAbout: Unit = ChicagoModule.toAbout

        override
        def toInfo: Unit = {
          ChicagoModule.toInfo
        }

        override
        def toRootPage( page: AppPage ): Unit = ChicagoModule.toRootPage(page)
    }

  val routes = RouterConfigDsl[ChicagoPage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | dynamicRouteCT( string("[a-zA-Z0-9]+").caseClass[SummaryView])
        ~> dynRenderR( (p,routerCtl) => PageSummary(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "rounds" ).caseClass[SummaryView])
        ~> dynRenderR( (p,routerCtl) => PageSummary(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "rounds" / int ).caseClass[RoundView])
        ~> dynRenderR( (p,routerCtl) => PageSummary(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "rounds" / int / "names" ).caseClass[NamesView])
        ~> dynRenderR( (p,routerCtl) => PagePlayers(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "rounds" / int / "hands" ).caseClass[RoundView])
        ~> dynRenderR( (p,routerCtl) => PageSummary(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "rounds" / int / "hands" / int ).caseClass[HandView])
        ~> dynRenderR( (p,routerCtl) => PageChicagoHand(p,routerCtl) )
      | staticRoute( root, ListView )
        ~> renderR( routerCtl => PageChicagoList(routerCtl,ListView) )
      )
  }

  val importRoutes = RouterConfigDsl[ChicagoPage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | dynamicRouteCT( (string(".+") / "chicago" ).caseClass[ImportListView])
        ~> dynRenderR( (p,routerCtl) => PageChicagoList(routerCtl,p) )
      )
  }

}
