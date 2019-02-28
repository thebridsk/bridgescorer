package com.example.routes

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, Router, _}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.document
import com.example.data.bridge._
import com.example.pages.info.InfoPage
import com.example.pages.HomePage
import com.example.pages.hand.PageHand
import utils.logging.Logger
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.CallbackTo
import com.example.pages.ThankYouPage
import com.example.pages.AboutPage
import japgolly.scalajs.react.extra.router.StaticDsl.Rule
import com.example.pages.BaseStyles.baseStyles

object AppRouter {

  val logger = Logger("bridge.Router")

  trait AppPage

  case object Home extends AppPage
  case object About extends AppPage
  case object ImportsList extends AppPage
  case object Export extends AppPage
  case object Info extends AppPage
  case object ThankYou extends AppPage
  case object ShowDuplicateHand extends AppPage   // for debugging
  case object ShowChicagoHand extends AppPage     // for debugging
  case object ShowRubberHand extends AppPage      // for debugging
  case object PageTest extends AppPage  // for debugging
  case object GraphQLAppPage extends AppPage
  case object ColorView extends AppPage
  case object VoyagerView extends AppPage
  case object GraphiQLView extends AppPage

  private var instance: Option[AppRouter] = None

  def apply( modules: Module* ) = {
    instance match {
      case Some(i) => throw new IllegalStateException("Only one AppRouter can be created")
      case None =>
        val i = new AppRouter(modules:_*)
        instance = Some(i)
        i
    }
  }

  val location = document.location
  val hostUrl = location.protocol+"//"+location.host
  val baseUrl = new BaseUrl(hostUrl+location.pathname)

}

import AppRouter._
import com.example.rest2.AjaxResult
import com.example.testpage.TestPage
import com.example.logger.Alerter
import org.scalactic.source.Position
import com.example.pages.GraphQLPage
import com.example.pages.ExportPage
import com.example.pages.ImportsListPage
import com.example.pages.ColorPage
import com.example.pages.VoyagerPage
import com.example.pages.GraphiQLPage
import com.example.pages.duplicate.DuplicatePageBridgeAppBar
import com.example.pages.RootBridgeAppBar
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor

trait ModuleRenderer {

  def canRender(selectedPage: Resolution[AppPage]): Boolean
  def render(selectedPage: Resolution[AppPage]): TagMod = selectedPage.render()
}

object ModuleRenderer extends ModuleRenderer {
  override
  def canRender(selectedPage: Resolution[AppPage]): Boolean = true
}

trait Module extends ModuleRenderer {

  private var varGotoAppHome: Option[()=>TagMod] = None
  private var varAppRouter: Option[AppRouter] = None

  private[routes] def setGotoAppHome( appRouter: AppRouter, f: ()=>TagMod ) = {
    varAppRouter = Some(appRouter)
    varGotoAppHome=Some(f)
  }

  /**
   * Get an ^.onClick-->Callback TagMod that goes to the home page when invoked.
   * If setGotoAppHome has not been called the returned TagMod does nothing when invoked.
   */
  def gotoAppHome() = varGotoAppHome match {
    case Some(f) =>
      logger.info("DuplicateRouter: going to App home page")
      f()
    case None =>
      logger.warning("Du1plicateRouter: ignoring going to home")
      ^.onClick-->Callback {}
  }

  def toHome: Unit = varAppRouter.foreach( _.toRootPage(Home,"") )

  def toAbout: Unit = varAppRouter.foreach( _.toRootPage(About,"#about") )

  def toInfo: Unit = varAppRouter.foreach( _.toRootPage(Info,"#info") )

  def toRootPage( page: AppPage, suffix: String = "" ): Unit = varAppRouter.foreach( _.toRootPage(page,suffix) )

  /**
   * All the pages of the module, to verify that they are all routable.
   */
  def verifyPages(): List[AppPage]

  /**
   * The routes for the module
   */
  def routes(): Rule[AppPage]
}

class AppRouter( modules: Module* ) {
  self =>

  def verifyPages: List[AppPage] = {

    val modulepages = modules.map(_.verifyPages()).flatten.toList

    val root =
              Home::
              ThankYou::
              About::
              ImportsList::
              Export::
              Info::
              ShowDuplicateHand::
              ShowChicagoHand::
              ShowRubberHand::
              PageTest::
              GraphQLAppPage::
              ColorView::
              VoyagerView::
              GraphiQLView::
              Nil

    root:::modulepages
  }

  val defaultContract = Contract( "0", ContractTricks(0), NoTrump, NotDoubled, North, Vul, NotVul, Made, 1, None, None, TestRubber, None, 1, 3, "North Player", "South Player", "East Player", "West Player", North )

  def defaultHand( scoring: ScoringSystem ) = defaultContract.copy( scoringSystem = scoring )

  def scoringViewWithHonorsCallbackOk( routerCtl: RouterCtl[AppPage])( contract: Contract, honors: Int, pos: PlayerPosition ) = {
    routerCtl.set(Home)  // show the Info page.
  }

  def scoringViewCallbackOk( routerCtl: RouterCtl[AppPage])( contract: Contract ) = {
    routerCtl.set(Home)  // show the Info page.
  }

  def scoringViewCallbackCancel( routerCtl: RouterCtl[AppPage])() = {
    routerCtl.set(Home)  // show the Info page.
  }

  def logToServer: RouterConfig.Logger =
    s => Callback { logger.fine(s"AppRouter: "+s) }

  val moduleAllRoutes = modules.map(_.routes())
  def moduleRoutes() = moduleAllRoutes.reduce(_ | _)

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter[P]( ctl: RouterCtl[P] ): BridgeRouter[P] =
    new BridgeRouterBase[P](ctl) {
        override
        def home: TagMod = gotoHome

        override
        def toHome: Unit = self.toRootPage(Home,"")
        override
        def toAbout: Unit = self.toRootPage(About,"#about")
        override
        def toInfo: Unit = {
          logger.info("in AppRouter, going to info page")
          self.toRootPage(Info,"#info")
        }

        override
        def toRootPage( page: AppPage ): Unit = self.toRootPage(page,"")
    }

  def logit[T]( f: => T )(implicit pos: Position): T = Alerter.tryit(f)

  def appBarPage( router: BridgeRouter[AppPage], title: String, page: TagMod, mainStyle: TagMod = TagMod() ) = {
    <.div(
      mainStyle,
      RootBridgeAppBar(
        title = List(MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                        title
                    )
                )),
        helpurl = None,
        routeCtl = router
      )(
      ),
      page
    )
  }

  val config = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._

    (emptyRule // trimSlashes
//      | rewritePathR("^\\?(.*)$".r, m => redirectToPath(m group 1)(Redirect.Replace))     // to get past iPad quirks
      | staticRoute(root, Home) ~> renderR( (routerCtl) => logit(HomePage(routerCtl)) )
      | staticRoute("#handduplicate", ShowDuplicateHand) ~> renderR( (routerCtl) => logit( appBarPage( routerCtl, "Test Duplicate Hand", PageHand(defaultHand(TestDuplicate),
                                                                                    scoringViewCallbackOk(routerCtl),
                                                                                    scoringViewCallbackCancel(routerCtl),
                                                                                    teamNS=Some("1"), teamEW=Some("2"),
                                                                                    newhand=true), baseStyles.divHandPage ))) // ScoringView(defaultContract))
      | staticRoute("#handchicago", ShowChicagoHand) ~> renderR( (routerCtl) => logit( appBarPage( routerCtl, "Test Chicago Hand", PageHand(defaultHand(TestChicago),
                                                                                  scoringViewCallbackOk(routerCtl),
                                                                                  scoringViewCallbackCancel(routerCtl),
                                                                                  newhand=true), baseStyles.divHandPage ))) // ScoringView(defaultContract))
      | staticRoute("#rubberhand", ShowRubberHand) ~> renderR( (routerCtl) => logit( appBarPage( routerCtl, "Test Rubber Hand", PageHand(defaultHand(TestRubber),
                                                                                       scoringViewCallbackOk(routerCtl),
                                                                                       scoringViewCallbackCancel(routerCtl),
                                                                                       newhand=true,
                                                                                       allowPassedOut=false,
                                                                                       callbackWithHonors=Some(scoringViewWithHonorsCallbackOk(routerCtl))), baseStyles.divHandPage ))) // ScoringView(defaultContract))
      | staticRoute("#about", About) ~> renderR( (routerCtl) => logit(AboutPage(routerCtl)) )
      | staticRoute("#imports", ImportsList) ~> renderR( (routerCtl) => logit(ImportsListPage(routerCtl,ImportsList)) )
      | staticRoute("#export", Export) ~> renderR( (routerCtl) => logit(ExportPage(routerCtl)) )
      | staticRoute("#info", Info) ~> renderR( (routerCtl) => logit(InfoPage(routerCtl)) )
      | staticRoute("#thankyou", ThankYou) ~> renderR( (routerCtl) => logit(ThankYouPage()) )
      | staticRoute("#testpage", PageTest) ~> renderR( (routerCtl) => logit(TestPage(Home,routerCtl)) )
      | staticRoute("#graphql", GraphQLAppPage) ~> renderR( (routerCtl) => logit(GraphQLPage(routerCtl)) )
      | staticRoute("#color", ColorView) ~> renderR( (routerCtl) => logit(ColorPage(routerCtl)) )
      | staticRoute("#voyager", VoyagerView) ~> renderR( (routerCtl) => logit(VoyagerPage(routerCtl)) )
      | staticRoute("#graphiql", GraphiQLView) ~> renderR( (routerCtl) => logit(GraphiQLPage(routerCtl)) )
      | moduleRoutes()
      ).notFound( p => logit {
        document.defaultView.alert("Could not find path "+p)
        logger.fine("AppRouter: Unable to find path for: "+ p)
        redirectToPage(Home)(Redirect.Replace)
      } )
      .verify(Home, verifyPages:_*)
      .renderWith(layout _)
      .logWith(logToServer)
  }

  val modulesWithDefault = modules.toList // ::: (ModuleRenderer::Nil)

  def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]) = {
    logger.fine("AppRouter: Rendering page "+ r.page)
    Navigator(r,c,modulesWithDefault)
  }

  val isFromServer = {
    val prot = location.protocol
    val r = (prot=="http:" || prot=="https:")
    logger.info("Page was loaded with "+prot+", isFromServer "+r)
    r
  }

  // Demo mode sets isEnabled to false
  // if not in demo mode, isEnabled is None
  AjaxResult.setEnabled(AjaxResult.isEnabled.getOrElse( isFromServer ))

  def router = routerComponentAndLogic()._1

  def routerWithURL( base: BaseUrl = baseUrl) = routerComponentAndLogic(base)._1

  private var fRouter: Option[ Router[AppRouter.AppPage]] = None
  private var fRouterLogic: Option[ RouterLogic[AppRouter.AppPage] ] = None
  private var fRouterCtl: Option[ RouterCtl[AppRouter.AppPage]] = None

  def routerComponentAndLogic( base: BaseUrl = baseUrl)  = {

    val (r,c) = Router.componentAndLogic( base, config)
    fRouter = Some(r)
    fRouterLogic = Some(c)
    fRouterCtl = Some(c.ctl)
    modules.foreach(_.setGotoAppHome(this,gotoHome _))
    (r,c)
  }

  def gotoHome() = {
    logger.info("AppRouter: setting up onClick for going home")
    fRouterCtl match {
      case Some(ctl) =>
        ctl.setOnClick(Home)
      case None =>
        ^.onClick-->Callback {
          val window = document.defaultView
          window.location.href = baseUrl.value
        }
    }
  }

  def toRootPage( page: AppPage, suffix: String ) = {
    logger.info(s"""toRootPage going to $page, suffix="$suffix".""")
    fRouterCtl match {
      case Some(ctl) =>
        ctl.set(page).runNow()
      case None =>
        val window = document.defaultView
        window.location.href = baseUrl.value + suffix
    }
  }
}
