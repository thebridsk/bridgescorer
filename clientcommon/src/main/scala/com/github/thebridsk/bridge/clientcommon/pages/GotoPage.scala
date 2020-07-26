package com.github.thebridsk.bridge.clientcommon.pages

import org.scalajs.dom.document
import org.scalajs.dom.experimental.URL
import com.github.thebridsk.bridge.clientcommon.react.AppButtonLinkNewWindow

object GotoPage {

  val location = document.defaultView.location

  /**
   * Open the specified uri in a new window.
   * Only one new window is created.  If it is already open,
   * then the contents of the window is replaced by this URI.
   * @param uri the URI, can be relative or absolute
   */
  def inNewWindow( uri: String ): Unit = {

    AppButtonLinkNewWindow.topage( getURL(uri) )
  }

  /**
   * Open the specified uri in the same window
   * @param uri the URI, can be relative or absolute
   */
  def inSameWindow( uri: String ): Unit = {
    location.href = getURL(uri)
  }

  /**
   * returns the full URL given a relative or absolute URI
   * @param uri the URI, can be relative or absolute
   */
  def getURL( uri: String ): String = {
    val origin = location.href
    val url = new URL(uri,origin).href
    url
  }

  def currentURL: String = {
    location.href
  }

  def hostname: String = {
    location.hostname
  }
}
