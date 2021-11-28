package com.github.thebridsk.bridge.clientcommon.pages

import org.scalajs.dom.document
import org.scalajs.dom.StorageEvent
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js

object LocalStorage {

  val log: Logger = Logger("bridge.LocalStorage")

  val window = document.defaultView
  val storage = window.localStorage

  var length: Int = storage.length

  def getItem(key: String): String = storage.getItem(key)

  def setItem(key: String, data: String): Unit = storage.setItem(key, data)

  def clear(): Unit = storage.clear()

  def removeItem(key: String): Unit = storage.removeItem(key)

  def key(index: Int): String = storage.key(index)

  def item(key: String): Option[String] = {
    // val nkeys = storage.length
    // val value = (0 until nkeys).find( i => storage.key(i) == key).map( i => storage.getItem(key) )
    val value = Option(storage.getItem(key))
    log.info(s"LocalStorage.item(${key}): ${value}")
    value
  }

  def onstorage(listener: js.Function1[StorageEvent, _]): Unit = {
    window.onstorage = listener
  }

  def removeOnstorage(listener: js.Function1[StorageEvent, _]): Unit = {
    if (window.onstorage == listener)
      window.asInstanceOf[js.Dictionary[_]] -= "onstorage"

  }

}
