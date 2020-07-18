package com.github.thebridsk.bridge.client.pages.rubber

import com.github.thebridsk.utilities.logging.Logger
//import com.github.thebridsk.bridge.client.pages.rubber.PageMatch
//import com.github.thebridsk.bridge.client.pages.rubber.PageDetails
//import com.github.thebridsk.bridge.client.pages.rubber.PagePlayers
//import com.github.thebridsk.bridge.client.pages.rubber.PageRubberHand
//import com.github.thebridsk.bridge.client.pages.rubber.PageRubberList

import japgolly.scalajs.react._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.extra.router.{ RouterConfigDsl, RouterCtl, _ }
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.client.routes.Module
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.routes.BridgeRouterBase
import scala.scalajs.js.URIUtils
import com.github.thebridsk.bridge.data.MatchRubber

object RubberModule extends Module {
  case class PlayRubber( m: RubberPage ) extends AppPage

  def verifyPages(): List[AppPage] = RubberRouter.verifyPages.map( p => PlayRubber(p).asInstanceOf[AppPage]).toList

  def routes() = RubberRouter.routes.prefixPath_/("#rubber").pmap[AppPage](PlayRubber){ case PlayRubber(m) => m } |
    RubberRouter.importRoutes.prefixPath_/("#import").pmap[AppPage](PlayRubber){ case PlayRubber(m) => m }

}

sealed trait RubberPage
object RubberRouter {

  val logger = Logger("bridge.RubberRouter")

  trait ListViewBase extends RubberPage

  object ListView extends ListViewBase
  case class ImportListView( importId: String ) extends ListViewBase {
    def getDecodedId = URIUtils.decodeURI(importId)
  }

  trait HasRubberId {
    val srid: String
    def rid = MatchRubber.id(srid)
  }

  trait RubberMatchViewBase extends RubberPage with HasRubberId {
    def toRubber = RubberMatchDetailsView(srid)
    def toDetails = RubberMatchDetailsView(srid)
    def toNames = RubberMatchNamesView(srid)
    def toHand( handid: String ) = RubberMatchHandView(srid,handid)
  }
  case class RubberMatchView( srid: String ) extends RubberMatchViewBase
  case class RubberMatchDetailsView( srid: String ) extends RubberMatchViewBase

  case class RubberMatchNamesView( srid: String ) extends RubberPage with HasRubberId {
    def toRubber = RubberMatchView(srid)
    def toDetails = RubberMatchDetailsView(srid)
    def toHand( handid: String ) = RubberMatchHandView(srid,handid)
  }
  case class RubberMatchHandView( srid: String, handid: String ) extends RubberPage with HasRubberId {
    def toRubber = RubberMatchView(srid)
    def toDetails = RubberMatchDetailsView(srid)
    def toNames = RubberMatchNamesView(srid)
    def toHand( handid: String ) = RubberMatchHandView(srid,handid)
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
      // | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "details" ).caseClass[RubberMatchDetailsView])
      //   ~> dynRenderR( (p,routerCtl) => PageRubberMatch(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "names" ).caseClass[RubberMatchNamesView])
        ~> dynRenderR( (p,routerCtl) => PageRubberNames(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "hands" / string("[a-zA-Z0-9]+") ).caseClass[RubberMatchHandView])
        ~> dynRenderR( (p,routerCtl) => PageRubberMatchHand(p,routerCtl) )
      | dynamicRouteCT( (string("[a-zA-Z0-9]+") / "hands" ).caseClass[RubberMatchDetailsView])
        ~> dynRenderR( (p,routerCtl) => PageRubberMatch(p,routerCtl) )
      | staticRoute( root, ListView )
        ~> renderR( routerCtl => PageRubberList(ListView,routerCtl) )
      | dynamicRouteCT( string("[a-zA-Z0-9]+").caseClass[RubberMatchView])
        ~> dynRenderR( (p,routerCtl) => PageRubberMatch(p,routerCtl) )
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
