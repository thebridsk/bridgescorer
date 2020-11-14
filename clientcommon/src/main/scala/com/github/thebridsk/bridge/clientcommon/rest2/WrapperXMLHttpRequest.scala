package com.github.thebridsk.bridge.clientcommon.rest2

import scala.scalajs.js
import org.scalajs.dom.raw.XMLHttpRequest

trait WrapperXMLHttpRequest {

  /**
    * The status of the response to the request. This is the HTTP result code (for example,
    * status is 200 for a successful request).
    *
    * MDN
    */
  def status: Int

  /**
    * The state of the request: Value State Description 0 UNSENT open()has not been
    * called yet. 1 OPENED send()has not been called yet. 2 HEADERS_RECEIVED send() has
    * been called, and headers and status are available. 3 LOADING Downloading;
    * responseText holds partial data. 4 DONE The operation is complete.
    *
    * MDN
    */
  def readyState: Int

  /**
    * The response to the request as text, or null if the request was unsuccessful or has
    * not yet been sent.
    *
    * MDN
    */
  def responseText: String

  /**
    * Returns the serialized URL of the response or the empty string if the URL is null. If
    * the URL is returned, URL fragment if present in the URL will be stripped away. The
    * value of responseURL will be the final URL obtained after any redirects.
    *
    * This property should be a String, but it isn't implemented by IE, even as new as IE11,
    * hence it must be UndefOr.
    *
    * MDN
    */
  def responseURL: js.UndefOr[String]

  /**
    * The response string returned by the HTTP server. Unlike status, this includes the
    * entire text of the response message ("200 OK", for example).
    *
    * MDN
    */
  def statusText: String

  /**
    * Aborts the request if it has already been sent.
    *
    * MDN
    */
  def abort(): Unit

  def responseType: String

  def getAllResponseHeaders(): String

  def getResponseHeader(header: String): String

}

class WrapperXMLHttpRequestImpl(val req: XMLHttpRequest)
    extends WrapperXMLHttpRequest {

  /**
    * The status of the response to the request. This is the HTTP result code (for example,
    * status is 200 for a successful request).
    *
    * MDN
    */
  def status: Int = req.status

  /**
    * The state of the request: Value State Description 0 UNSENT open()has not been
    * called yet. 1 OPENED send()has not been called yet. 2 HEADERS_RECEIVED send() has
    * been called, and headers and status are available. 3 LOADING Downloading;
    * responseText holds partial data. 4 DONE The operation is complete.
    *
    * MDN
    */
  def readyState: Int = req.readyState

  /**
    * The response to the request as text, or null if the request was unsuccessful or has
    * not yet been sent.
    *
    * MDN
    */
  def responseText: String = req.responseText

  /**
    * Returns the serialized URL of the response or the empty string if the URL is null. If
    * the URL is returned, URL fragment if present in the URL will be stripped away. The
    * value of responseURL will be the final URL obtained after any redirects.
    *
    * This property should be a String, but it isn't implemented by IE, even as new as IE11,
    * hence it must be UndefOr.
    *
    * MDN
    */
  def responseURL: js.UndefOr[String] = req.responseURL

  /**
    * The response string returned by the HTTP server. Unlike status, this includes the
    * entire text of the response message ("200 OK", for example).
    *
    * MDN
    */
  def statusText: String = req.statusText

  /**
    * Aborts the request if it has already been sent.
    *
    * MDN
    */
  def abort(): Unit = req.abort()

  def responseType: String = req.responseType

  def getAllResponseHeaders(): String = req.getAllResponseHeaders()

  def getResponseHeader(header: String): String = req.getResponseHeader(header)
}

class DisabledXMLHttpRequest extends WrapperXMLHttpRequest {
  def status: Int = 0
  def readyState: Int = 0
  def responseText: String = null
  def responseURL: js.UndefOr[String] = js.undefined
  def statusText: String = null
  def abort(): Unit = {}
  def responseType: String = null
  def getAllResponseHeaders(): String = null
  def getResponseHeader(header: String): String = null
}
