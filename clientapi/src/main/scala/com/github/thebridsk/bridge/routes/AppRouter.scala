package com.github.thebridsk.bridge.routes

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, Router, _}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.document
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.pages.info.InfoPage
import com.github.thebridsk.bridge.pages.HomePage
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.CallbackTo
import com.github.thebridsk.bridge.pages.ThankYouPage
import com.github.thebridsk.bridge.pages.AboutPage
import japgolly.scalajs.react.extra.router.StaticDsl.Rule
import com.github.thebridsk.bridge.pages.BaseStyles.baseStyles
import com.github.thebridsk.bridge.logger.{ Info => LogInfo }

object AppRouter {

  def apply() = new AppRouter

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
  case object LogView extends AppPage

  private var instance: Option[AppRouter] = None

}

import AppRouter._
import com.github.thebridsk.bridge.rest2.AjaxResult
import com.github.thebridsk.bridge.logger.Alerter
import org.scalactic.source.Position
import com.github.thebridsk.bridge.pages.GraphQLPage
import com.github.thebridsk.bridge.pages.ColorPage
import com.github.thebridsk.bridge.pages.VoyagerPage
import com.github.thebridsk.bridge.pages.GraphiQLPage
import com.github.thebridsk.bridge.pages.RootBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.pages.LogPage

class AppRouter {
  self =>

  def verifyPages: List[AppPage] = {

    val root =
              Home::
              ThankYou::
              About::
              Info::
              ShowDuplicateHand::
              ShowChicagoHand::
              ShowRubberHand::
              GraphQLAppPage::
              ColorView::
              VoyagerView::
              GraphiQLView::
              LogView::
              Nil

    root
  }

  def logToServer: RouterConfig.Logger =
    s => Callback { logger.fine(s"AppRouter: "+s) }

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

  val config = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._

    (emptyRule // trimSlashes
//      | rewritePathR("^\\?(.*)$".r, m => redirectToPath(m group 1)(Redirect.Replace))     // to get past iPad quirks
      | staticRoute(root, Home) ~> renderR( (routerCtl) => logit(HomePage(routerCtl)) )
      | staticRoute("#about", About) ~> renderR( (routerCtl) => logit(AboutPage(routerCtl)) )
      | staticRoute("#info", Info) ~> renderR( (routerCtl) => logit(InfoPage(routerCtl)) )
      | staticRoute("#thankyou", ThankYou) ~> renderR( (routerCtl) => logit(ThankYouPage()) )
      | staticRoute("#graphql", GraphQLAppPage) ~> renderR( (routerCtl) => logit(GraphQLPage(routerCtl)) )
      | staticRoute("#color", ColorView) ~> renderR( (routerCtl) => logit(ColorPage(routerCtl)) )
      | staticRoute("#voyager", VoyagerView) ~> renderR( (routerCtl) => logit(VoyagerPage(routerCtl)) )
      | staticRoute("#graphiql", GraphiQLView) ~> renderR( (routerCtl) => logit(GraphiQLPage(routerCtl)) )
      | staticRoute("#log", LogView) ~> renderR( (routerCtl) => logit(LogPage(routerCtl)) )
      ).notFound( p => logit {
        document.defaultView.alert("Could not find path "+p)
        logger.fine("AppRouter: Unable to find path for: "+ p)
        redirectToPage(Home)(Redirect.Replace)
      } )
      .verify(Home, verifyPages:_*)
      .renderWith(layout _)
      .logWith(logToServer)
  }

  def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]) = {
    logger.fine("AppRouter: Rendering page "+ r.page)
    Navigator(r,c)
  }

  val isFromServer = {
    val prot = LogInfo.location.protocol
    val r = (prot=="http:" || prot=="https:")
    logger.info("Page was loaded with "+prot+", isFromServer "+r)
    r
  }

  // Demo mode sets isEnabled to false
  // if not in demo mode, isEnabled is None
  AjaxResult.setEnabled(AjaxResult.isEnabled.getOrElse( isFromServer ))

  def router = routerComponentAndLogic()._1

  def routerWithURL( base: BaseUrl = LogInfo.baseUrl) = routerComponentAndLogic(base)._1

  private var fRouter: Option[ Router[AppRouter.AppPage]] = None
  private var fRouterLogic: Option[ RouterLogic[AppRouter.AppPage] ] = None
  private var fRouterCtl: Option[ RouterCtl[AppRouter.AppPage]] = None

  def routerComponentAndLogic( base: BaseUrl = LogInfo.baseUrl)  = {

    val (r,c) = Router.componentAndLogic( base, config)
    fRouter = Some(r)
    fRouterLogic = Some(c)
    fRouterCtl = Some(c.ctl)
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
          window.location.href = LogInfo.baseUrl.value
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
        window.location.href = LogInfo.baseUrl.value + suffix
    }
  }
}
