package com.github.thebridsk.bridge.clienttest.routes

import japgolly.scalajs.react.extra.router.{
  Resolution,
  RouterConfigDsl,
  RouterCtl,
  Router,
  _
}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.document
import com.github.thebridsk.bridge.clienttest.pages.HomePage
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.clientcommon.logger.{Info => LogInfo}

object AppRouter {

  def apply() = new AppRouter

  val logger: Logger = Logger("bridge.Router")

  trait AppPage {
    def mainMenu = {

    }
  }

  case object Home extends AppPage
  case object About extends AppPage
  case object ImportsList extends AppPage
  case object Export extends AppPage
  case object Info extends AppPage
  case object ThankYou extends AppPage
  case object ShowDuplicateHand extends AppPage // for debugging
  case object ShowChicagoHand extends AppPage // for debugging
  case object ShowRubberHand extends AppPage // for debugging
  case object PageTest extends AppPage // for debugging
  case object GraphQLAppPage extends AppPage
  case object ColorView extends AppPage
  case class VoyagerView( query: Map[String, String]) extends AppPage
  object VoyagerViewDefault extends VoyagerView(Map())
  case object GraphiQLView extends AppPage
  case object LogView extends AppPage

  private var instance: Option[AppRouter] = None

}

import AppRouter._
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import org.scalactic.source.Position

class AppRouter {
  self =>

  def verifyPages: List[AppPage] = {

    val root =
      Home ::
        Nil

    root
  }

  def logToServer: RouterConfig.Logger =
    s => ( () => logger.fine(s"AppRouter: " + s))

  import scala.language.implicitConversions
  implicit def routerCtlToBridgeRouter[P](ctl: RouterCtl[P]): BridgeRouter[P] =
    new BridgeRouterBase[P](ctl) {
      override def home: TagMod = gotoHome()

      override def toHome: Unit = self.toRootPage(Home, "")
      override def toAbout: Unit = self.toRootPage(About, "#about")
      override def toInfo: Unit = {
        logger.info("in AppRouter, going to info page")
        self.toRootPage(Info, "#info")
      }

      override def toRootPage(page: AppPage): Unit = self.toRootPage(page, "")
    }

  def logit[T](f: => T)(implicit pos: Position): T = Alerter.tryit(f)

  val config: RouterWithPropsConfig[AppPage, Unit] =
    RouterConfigDsl[AppPage].buildConfig { dsl =>
      import dsl._

      (emptyRule // trimSlashes
//      | rewritePathR("^\\?(.*)$".r, m => redirectToPath(m group 1)(Redirect.Replace))     // to get past iPad quirks
        | staticRoute(root, Home) ~> renderR((routerCtl) =>
          logit(HomePage(routerCtl))
        ))
        .notFound(p =>
          logit {
            document.defaultView.alert("Could not find path " + p)
            logger.fine("AppRouter: Unable to find path for: " + p)
            redirectToPage(Home)(SetRouteVia.HistoryReplace)
          }
        )
        .verify(Home, verifyPages: _*)
        .renderWith(layout _)
        .logWith(logToServer)
    }

  def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]): VdomElement = {
    logger.fine("AppRouter: Rendering page " + r.page)
    Navigator(r, c)
  }

  val isFromServer: Boolean = {
    val prot = LogInfo.location.protocol
    val r = (prot == "http:" || prot == "https:")
    logger.info("Page was loaded with " + prot + ", isFromServer " + r)
    r
  }

  // Demo mode sets isEnabled to false
  // if not in demo mode, isEnabled is None
  AjaxResult.setEnabled(AjaxResult.isEnabled.getOrElse(isFromServer))

  def router: Router[AppPage] = routerComponentAndLogic()._1

  def routerWithURL(base: BaseUrl = LogInfo.baseUrl): Router[AppPage] =
    routerComponentAndLogic(base)._1

  private var fRouter: Option[Router[AppRouter.AppPage]] = None
  private var fRouterLogic: Option[RouterLogic[AppRouter.AppPage, Unit]] = None
  private var fRouterCtl: Option[RouterCtl[AppRouter.AppPage]] = None

  def routerComponentAndLogic(
      base: BaseUrl = LogInfo.baseUrl
  ): (Router[AppPage], RouterLogic[AppPage, Unit]) = {

    val (r, c) = Router.componentAndLogic(base, config)
    fRouter = Some(r)
    fRouterLogic = Some(c)
    fRouterCtl = Some(c.ctl)
    (r, c)
  }

  def gotoHome(): TagMod = {
    logger.info("AppRouter: setting up onClick for going home")
    fRouterCtl match {
      case Some(ctl) =>
        ctl.setOnClick(Home)
      case None =>
        ^.onClick --> Callback {
          val window = document.defaultView
          window.location.href = LogInfo.baseUrl.value
        }
    }
  }

  def toRootPage(page: AppPage, suffix: String): Unit = {
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
