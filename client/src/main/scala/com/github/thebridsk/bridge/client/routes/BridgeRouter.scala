package com.github.thebridsk.bridge.client.routes

import japgolly.scalajs.react.extra.router.{RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react._

trait BridgeRouter[Page] {
  def baseUrl: BaseUrl
  def refresh: Callback
  def pathFor(target: Page): Path
  def urlFor(page: Page): AbsUrl
  def set(page: Page): Callback
  def setEH(page: Page): ReactEvent => Callback
  def setOnClick(page: Page): TagMod
  def setOnLinkClick(page: Page): TagMod
  def link(page: Page) = // scalafix:ok ExplicitResultTypes; React
    <.a(^.href := urlFor(page).value, setOnLinkClick(page))

  def home: TagMod
  def toHome: Unit = {}
  def toAbout: Unit = {}
  def toInfo: Unit = {
    BridgeRouterBaseWithLogging.logger.info(
      "BridgeRouter.toInfo default handler, does nothing"
    )
  }

  def toRootPage(page: AppPage): Unit = {}

  /**
    * Returns the RouterCtl that is wrapped by this
    * @return None if there is no RouterCtl
    */
  def routerCtl: Option[RouterCtl[Page]] = None
}

class BridgeRouterBase[Page](ctl: RouterCtl[Page]) extends BridgeRouter[Page] {
  def baseUrl: BaseUrl = ctl.baseUrl
  def refresh: Callback = ctl.refresh
  def pathFor(target: Page): Path = ctl.pathFor(target)
  def urlFor(page: Page): AbsUrl = ctl.urlFor(page)
  def set(page: Page): Callback = ctl.set(page)
  def setEH(page: Page): ReactEvent => Callback = ctl.setEH(page)
  def setOnClick(page: Page): TagMod = ctl.setOnClick(page)
  def setOnLinkClick(page: Page): TagMod = ctl.setOnLinkClick(page)
  def home: TagMod = ^.onClick --> CallbackTo {}
  override def routerCtl: Option[RouterCtl[Page]] = Some(ctl)
}

object BridgeRouterBaseWithLogging {
  val logger: Logger = Logger("bridge.BridgeRouterBaseWithLogging")
}

class BridgeRouterBaseWithLogging[Page](ctl: RouterCtl[Page])
    extends BridgeRouter[Page] {
  import BridgeRouterBaseWithLogging._
  def log(msg: => String): Callback =
    Callback {
      logger.fine(msg)
    }
  def baseUrl: BaseUrl = ctl.baseUrl
  def refresh: Callback = ctl.refresh
  def pathFor(target: Page): Path = ctl.pathFor(target)
  def urlFor(page: Page): AbsUrl = ctl.urlFor(page)
  def set(page: Page): Callback = {
    logger.fine(s"""Setting up callback BridgeRouter.set(${page})""")
    log(s"""Callback BridgeRouter.set(${page})""") >> ctl.set(page)
  }

  final def setEH(target: Page): ReactEvent => Callback = {
    logger.fine(s"""Setting up callback BridgeRouter.setEH(${target})""")
    e => set(target).asEventDefault(e).void
  }

  final def setOnClick(target: Page): TagMod = {
    logger.fine(s"""Setting up callback BridgeRouter.setOnClick(${target})""")
    ^.onClick ==> setEH(target)
  }

  def setOnLinkClick(page: Page): TagMod = ctl.setOnLinkClick(page)
  def home: TagMod = ^.onClick --> CallbackTo {}

}

/**
  * A BridgeRouter for testing
  * @param base the base url, example: http://localhost:8080/html/index-fastopt.html
  */
abstract class TestBridgeRouter[Page](base: BaseUrl)
    extends BridgeRouter[Page] {
  def refresh: Callback
  def set(page: Page): Callback

  def pathFor(target: Page): Path

  def baseUrl: BaseUrl = base
  def urlFor(page: Page): AbsUrl = pathFor(page).abs(baseUrl)
  def setEH(page: Page): ReactEvent => Callback =
    e => set(page).asEventDefault(e).void
  def setOnClick(page: Page): TagMod = ^.onClick ==> setEH(page)
  def setOnLinkClick(page: Page): TagMod = {
    def go(e: ReactMouseEvent): Callback =
      CallbackOption.unless(ReactMouseEvent targetsNewTab_? e) >>
        setEH(page)(e)
    ^.onClick ==> go
  }
}
