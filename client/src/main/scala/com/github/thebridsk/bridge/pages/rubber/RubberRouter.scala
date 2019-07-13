package com.github.thebridsk.bridge.pages.rubber

import com.github.thebridsk.utilities.logging.Logger
//import com.github.thebridsk.bridge.pages.rubber.PageMatch
//import com.github.thebridsk.bridge.pages.rubber.PageDetails
//import com.github.thebridsk.bridge.pages.rubber.PagePlayers
//import com.github.thebridsk.bridge.pages.rubber.PageRubberHand
//import com.github.thebridsk.bridge.pages.rubber.PageRubberList

import japgolly.scalajs.react._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.extra.router.{ RouterConfigDsl, RouterCtl, _ }
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.routes.AppRouter.AppPage
import japgolly.scalajs.react.extra.router.StaticDsl.Rule
import com.github.thebridsk.bridge.routes.Module
import com.github.thebridsk.bridge.routes.BridgeRouter
import com.github.thebridsk.bridge.routes.BridgeRouterBase
import scala.scalajs.js.URIUtils

object RubberModule extends Module {
  case class PlayRubber( m: RubberPage ) extends AppPage

  def verifyPages(): List[AppPage] = RubberRouter.verifyPages.map( p => PlayRubber(p).asInstanceOf[AppPage]).toList

  def routes(): Rule[AppPage] = RubberRouter.routes.prefixPath_/("#rubber").pmap[AppPage](PlayRubber){ case PlayRubber(m) => m } |
    RubberRouter.importRoutes.prefixPath_/("#import").pmap[AppPage](PlayRubber){ case PlayRubber(m) => m }

  override
  def canRender(selectedPage: Resolution[AppPage]): Boolean = selectedPage.page.isInstanceOf[PlayRubber]

}

sealed trait RubberPage
object RubberRouter {

  val logger = Logger("bridge.RubberRouter")

  trait ListViewBase extends RubberPage

  object ListView extends ListViewBase
  case class ImportListView( importId: String ) extends ListViewBase {
    def getDecodedId = URIUtils.decodeURI(importId)
  }

  trait RubberMatchViewBase extends RubberPage {
    val rid: String
    def toRubber() = RubberMatchDetailsView(rid)
    def toDetails() = RubberMatchDetailsView(rid)
    def toNames() = RubberMatchNamesView(rid)
    def toHand( handid: String ) = RubberMatchHandView(rid,handid)
  }
  case class RubberMatchView( rid: String ) extends RubberMatchViewBase
  case class RubberMatchDetailsView( rid: String ) extends RubberMatchViewBase

  case class RubberMatchNamesView( rid: String ) extends RubberPage {
    def toRubber() = RubberMatchView(rid)
    def toDetails() = RubberMatchDetailsView(rid)
    def toHand( handid: String ) = RubberMatchHandView(rid,handid)
  }
  case class RubberMatchHandView( rid: String, handid: String ) extends RubberPage {
    def toRubber() = RubberMatchView(rid)
    def toDetails() = RubberMatchDetailsView(rid)
    def toNames() = RubberMatchNamesView(rid)
    def toHand( handid: String ) = RubberMatchHandView(rid,handid)
  }

  val verifyPages = ListView::
                    ImportListView("import.zip")::
                    RubberMatchView("R1")::
                    RubberMatchDetailsView("R1")::
                    RubberMatchNamesView("R1")::
                    RubberMatchHandView("R1","1")::
                    Nil

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter( ctl: RouterCtl[RubberPage] ): BridgeRouter[RubberPage] =
    new BridgeRouterBase[RubberPage](ctl) {
        override
        def home: TagMod = RubberModule.gotoAppHome()

        override
        def toHome: Unit = RubberModule.toHome
        override
        def toAbout: Unit = RubberModule.toAbout

        override
        def toInfo: Unit = {
          RubberModule.toInfo
        }

        override
        def toRootPage( page: AppPage ): Unit = RubberModule.toRootPage(page)
    }

  val routes = RouterConfigDsl[RubberPage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | dynamicRouteCT( string("[a-zA-Z0-9]+").caseClass[RubberMatchView])
        ~> dynRenderR( (p,routerCtl) => PageRubberMatch(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "details" ).caseClass[RubberMatchDetailsView])
        ~> dynRenderR( (p,routerCtl) => PageRubberMatch(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "names" ).caseClass[RubberMatchNamesView])
        ~> dynRenderR( (p,routerCtl) => PageRubberNames(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "hands" ).caseClass[RubberMatchDetailsView])
        ~> dynRenderR( (p,routerCtl) => PageRubberMatch(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+") ).caseClass[RubberMatchHandView])
        ~> dynRenderR( (p,routerCtl) => PageRubberMatchHand(p,routerCtl) )
      | staticRoute( root, ListView )
        ~> renderR( routerCtl => PageRubberList(ListView,routerCtl) )
      )
  }

  val importRoutes = RouterConfigDsl[RubberPage].buildRule { dsl =>
    import dsl._

    (emptyRule
      | dynamicRouteCT( (string(".+") / "rubber" ).caseClass[ImportListView])
        ~> dynRenderR( (p,routerCtl) => PageRubberList(p,routerCtl) )
      )
  }

}
