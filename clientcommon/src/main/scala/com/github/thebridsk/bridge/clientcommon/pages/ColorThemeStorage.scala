
package com.github.thebridsk.bridge.clientcommon.pages

import org.scalajs.dom.document
import com.github.thebridsk.utilities.logging.Logger

object ColorThemeStorage {

  val log: Logger = Logger("bridge.ColorThemeStorage")

  // these values are synchronized with the colortheme.js file
  val key = "thebridsk:bridge:color-theme"
  val bodyAttribute = "data-theme"

  val window = document.defaultView
  val storage = window.localStorage

  // js.Function1[StorageEvent, _]

  // def handler( event: StorageEvent ): Unit = {
  //   log.info(s"ColorThemeStorage.handler: ${event}")
  //   if (event.storageArea == storage && event.key == key ) {
  //     val newvalue = event.newValue
  //     applyTheme(newvalue)
  //   }
  // }

  def applyTheme( theme: String ): Unit = {
    log.info(s"ColorThemeStorage.applyTheme: ${theme}")
    document.body.setAttribute(bodyAttribute,theme)
  }

  def getColorTheme(): Option[String] = {
    val nkeys = storage.length
    val theme = (0 until nkeys).find( i => storage.key(i) == key).map( i => storage.getItem(key) )
    log.info(s"ColorThemeStorage.getColorTheme: ${theme}")
    theme
  }

  def initTheme(): Unit = {
    log.info(s"ColorThemeStorage.initTheme")
    // window.onstorage = handler
    getColorTheme() match {
      case Some(theme) =>
        applyTheme(theme)
      case None =>
        getColorThemeFromBody() match {
          case Some(theme) => setColorTheme(theme)
          case None =>
        }
    }
  }

  def setColorTheme( theme: String ): Unit = {
    log.info(s"ColorThemeStorage.setColorTheme: ${theme}")
    applyTheme(theme)
    storage.setItem(key,theme)
  }

  def getColorThemeFromBody(): Option[String] = {
    val body = document.body
    val theme = if (body.hasAttribute(bodyAttribute)) {
      Some(body.getAttribute(bodyAttribute))
    } else {
      None
    }
    log.info(s"ColorThemeStorage.getColorThemeFromBody: ${theme}")
    theme
  }

}
