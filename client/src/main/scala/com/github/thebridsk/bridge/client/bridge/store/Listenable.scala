package com.github.thebridsk.bridge.client.bridge.store

import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.clientcommon.logger.Alerter

trait Listenable {

  val loggerListener: Logger = Logger("bridge.Listenable")

  type Event = String

  case class Listener(event: Event, cb: Callback, origcb: Option[Callback])

  private var fListeners = List[Listener]()

  def addListener(event: Event, cb: Callback): Unit = {
    loggerListener.info(
      "Adding " + event + " callback" + ", had " + fListeners.size
    )
    fListeners ::= Listener(event, cb, None)
  }

  def addOnceListener(event: Event, cb: Callback): Unit = {
    loggerListener.info(
      "Adding " + event + " once callback" + ", had " + fListeners.size
    )
    var alreadyFired = false
    val autoRemoveCb = Callback { () =>
      removeListener(event, cb)
      if (!alreadyFired) {
        alreadyFired = true
        cb.runNow()
      }
    }
    fListeners ::= Listener(event, autoRemoveCb, Some(cb))
  }

  def noListener: Unit = {}

  def removeListener(event: Event, cb: Callback): Unit = {
    loggerListener.info(
      "Removing " + event + " callback" + ", had " + fListeners.size
    )
    fListeners = fListeners.filterNot({
      case Listener(e, callback, Some(origCb)) =>
        e == event && (callback == cb || origCb == cb)
      case Listener(e, callback, _) => e == event && callback == cb
      case _                        => false
    })
    loggerListener.info(
      "Removing " + event + " callback done" + ", have " + fListeners.size
    )
    if (fListeners.isEmpty) noListener
  }

  def removeAllListener(event: Event): Unit = {
    loggerListener.info(
      "Removing all " + event + " callbacks" + ", had " + fListeners.size
    )
    fListeners = fListeners.filterNot({
      case Listener(e, _, _) => e == event
      case _                 => false
    })
    loggerListener.info(
      "Removing all " + event + " callbacks done" + ", have " + fListeners.size
    )
    if (fListeners.isEmpty) noListener
  }

  def removeListener(): Unit = {
    loggerListener.info("Removing all callbacks" + ", had " + fListeners.size)
    fListeners = List()
    noListener
  }

  def notify(event: Event): Unit = {
//    loggerListener.info("Notify "+event+" to "+fListeners.size+" callbacks")
    fListeners
      .filter(_.event == event)
      .foreach(l =>
        Alerter.tryitWithUnit {
          l.cb.runNow()
        }
      )
  }
}

object ChangeListenable {
  val event = "change"
}

trait ChangeListenable extends Listenable {

  def addChangeListener(cb: Callback): Unit =
    addListener(ChangeListenable.event, cb)
  def removeChangeListener(cb: Callback): Unit =
    removeListener(ChangeListenable.event, cb)

  def notifyChange(): Unit = notify(ChangeListenable.event)
}
