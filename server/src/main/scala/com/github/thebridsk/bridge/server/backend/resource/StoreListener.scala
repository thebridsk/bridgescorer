package com.github.thebridsk.bridge.server.backend.resource

import com.github.thebridsk.utilities.logging.Logger

trait StoreListener {
  def create(change: ChangeContext): Unit = {}
  def update(change: ChangeContext): Unit = {}
  def delete(change: ChangeContext): Unit = {}
}

object StoreListenerManager {
  val log: Logger = Logger[StoreListenerManager]()
}

import StoreListenerManager._

trait StoreListenerManager {
  private var listeners = Set[StoreListener]()

  def notify(f: StoreListener => Unit): Unit = {
    listeners.foreach(f)
  }

  def addListener(l: StoreListener): Unit = {
    listeners = listeners + l
  }

  def removeListener(l: StoreListener): Unit = {
    listeners = listeners - l
  }

  def notify(context: ChangeContext): Unit = {
    log.fine(s"Notifying change to ${listeners.size} listeners: $context")
    context.getSpecificChange() match {
      case Some(d) =>
        d match {
          case cc: CreateChangeContext => notify((l) => l.create(context))
          case uc: UpdateChangeContext => notify((l) => l.update(context))
          case dc: DeleteChangeContext => notify((l) => l.delete(context))
        }
      case None =>
    }
  }
}
