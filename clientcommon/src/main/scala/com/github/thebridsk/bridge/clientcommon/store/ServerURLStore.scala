package com.github.thebridsk.bridge.clientcommon.store

import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.ServerURL
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerURL
import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.dispatcher.ChangeListenable
import com.github.thebridsk.bridge.clientcommon.dispatcher.Dispatcher
import com.github.thebridsk.bridge.clientcommon.dispatcher.ActionUpdateServerURLs

object ServerURLStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.ServerURLStore")

  private var updateRequested: Boolean = false
  private var urls: Option[ServerURL] = None

  def hasURLs = urls.isDefined
  def getURLs: ServerURL = urls.getOrElse(ServerURL(List()))

  /**
    * @return A TagMod that contains one or more li elements.
    */
  def getURLItems: TagMod = {
    if (BridgeDemo.isDemo) {
      <.li(
        "Demo mode, all data entered will be lost on page refresh or closing page"
      )
    } else {
      val urls = ServerURLStore.getURLs.serverUrl
      if (urls.isEmpty) {
        if (updateRequested) {
          <.li("Waiting for response from server")
        } else {
          <.li("No network interfaces found")
        }
      } else {
        urls.map { url => <.li(url) }.toTagMod
      }
    }
  }

  private var dispatchToken: Option[DispatchToken] = Some(
    Dispatcher.register(dispatch _)
  )

  def dispatch(msg: Any): Unit =
    Alerter.tryitWithUnit {
      msg match {
        case ActionUpdateServerURLs(serverurl) =>
          urls = Some(serverurl)
          notifyChange()
        case _ =>
      }
    }

  def updateURLs(force: Boolean = false): Unit = {
    if (force || !hasURLs) {
      if (!BridgeDemo.isDemo) {
        updateRequested = true
        RestClientServerURL
          .list()
          .recordFailure()
          .foreach(serverUrl => {
            Dispatcher.updateServerURL(serverUrl(0))
            updateRequested = false
          })
      }
    }

  }
}
