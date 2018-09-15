package com.example.routes

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, Router, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.example.data.bridge._
import com.example.pages.info.InfoPage
import com.example.pages.HomePage
import utils.logging.Logger
import japgolly.scalajs.react.Callback
import com.example.data.Id
import com.example.routes.AppRouter.AppPage
import japgolly.scalajs.react.CallbackTo
import org.scalajs.dom.html
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagOf

trait BridgeRouter[Page] {
  def baseUrl: BaseUrl
  def refresh: Callback
  def pathFor(target: Page): Path
  def urlFor(page: Page): AbsUrl
  def set( page: Page ): Callback
  def setEH( page: Page ): ReactEvent => Callback
  def setOnClick( page: Page ): TagMod
  def setOnLinkClick( page: Page ): TagMod
  def link(page: Page): TagOf[html.Anchor] =
    <.a(^.href := urlFor(page).value, setOnLinkClick(page))

  def home: TagMod
}

class BridgeRouterBase[Page]( ctl: RouterCtl[Page] ) extends BridgeRouter[Page] {
  def baseUrl: BaseUrl = ctl.baseUrl
  def refresh: Callback = ctl.refresh
  def pathFor(target: Page): Path = ctl.pathFor(target)
  def urlFor(page: Page): AbsUrl = ctl.urlFor(page)
  def set( page: Page ): Callback = ctl.set(page)
  def setEH( page: Page ): ReactEvent => Callback = ctl.setEH(page)
  def setOnClick( page: Page ) = ctl.setOnClick(page)
  def setOnLinkClick( page: Page ): TagMod = ctl.setOnLinkClick(page)
  def home = ^.onClick-->CallbackTo {}
}

object BridgeRouterBaseWithLogging {
  val logger = Logger("bridge.BridgeRouterBaseWithLogging")
}

class BridgeRouterBaseWithLogging[Page]( ctl: RouterCtl[Page] ) extends BridgeRouter[Page] {
  import BridgeRouterBaseWithLogging._
  def log( msg: =>String ) = Callback {
    logger.fine(msg)
  }
  def baseUrl: BaseUrl = ctl.baseUrl
  def refresh: Callback = ctl.refresh
  def pathFor(target: Page): Path = ctl.pathFor(target)
  def urlFor(page: Page): AbsUrl = ctl.urlFor(page)
  def set( page: Page ): Callback = {
    logger.fine(s"""Setting up callback BridgeRouter.set(${page})""")
    log(s"""Callback BridgeRouter.set(${page})""") >> ctl.set(page)
  }

  final def setEH(target: Page): ReactEvent => Callback = {
    logger.fine(s"""Setting up callback BridgeRouter.setEH(${target})""")
    e => CallbackOption.asEventDefault(e, set(target))
  }

  final def setOnClick(target: Page): TagMod = {
    logger.fine(s"""Setting up callback BridgeRouter.setOnClick(${target})""")
    ^.onClick ==> setEH(target)
  }

  def setOnLinkClick( page: Page ): TagMod = ctl.setOnLinkClick(page)
  def home = ^.onClick-->CallbackTo {}
}

/**
 * A BridgeRouter for testing
 * @param base the base url, example: http://localhost:8080/html/index-fastopt.html
 */
abstract class TestBridgeRouter[Page]( base: BaseUrl ) extends BridgeRouter[Page] {
  def refresh: Callback
  def set( page: Page ): Callback

  def pathFor(target: Page): Path

  def baseUrl: BaseUrl = base
  def urlFor(page: Page): AbsUrl = pathFor(page).abs(baseUrl)
  def setEH( page: Page ): ReactEvent => Callback = e => CallbackOption.asEventDefault(e, set(page))
  def setOnClick( page: Page ) = ^.onClick ==> setEH(page)
  def setOnLinkClick( page: Page ): TagMod = {
    def go(e: ReactMouseEvent): Callback =
      CallbackOption.unless(ReactMouseEvent targetsNewTab_? e) >>
        setEH(page)(e)
    ^.onClick ==> go
  }
}

