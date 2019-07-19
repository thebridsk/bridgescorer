package com.github.thebridsk.bridge.client.bridge.store

import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientNames
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.client.Bridge

object NamesStore extends ChangeListenable {
  val logger = Logger("bridge.ViewPlayers")

  /**
   * Required to instantiate the store.
   */
  def init() = {
//    refreshNames()
  }

  private var names: List[String] = Nil

  def getNames = names

  /**
   * Refresh the names in the list.
   * @param cb - callback that should be called when the names have been updated
   */
  def refreshNames( cb: Option[Callback] = None ) = {
    if (Bridge.isDemo) {
      import scala.scalajs.js.timers._

      setTimeout(1) { // note the absence of () =>
        names = List("Barry","Bill","Cathy","Irene","June","Kelly","Larry","Norman","Victor")
        cb.foreach { _.runNow() }
      }
    } else {
      import scala.concurrent.ExecutionContext.Implicits.global
      RestClientNames.list().recordFailure().foreach( list => Alerter.tryitWithUnit {
        names = list.toList
        cb.foreach { _.runNow() }
      })
    }
  }


  /**
   * Ensure names are cached, get names from server if they are not.
   * @param cb - callback that should be called when the names have been updated.  Will call the callback immediately if the names are cached.
   */
  def ensureNamesAreCached( cb: Option[Callback] = None ) = Alerter.tryitWithUnit {
    if (names.isEmpty) refreshNames(cb)
    else cb.foreach(_.runNow())
  }
}