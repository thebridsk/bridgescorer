package com.github.thebridsk.bridge.javascript

import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.ArrayBufferView
import scala.scalajs.js.typedarray.ArrayBuffer



/**
 * See https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder/TextDecoder
 *
 * @constructor
 * @param utfLable defaulting to "utf-8", containing the label of the encoder. Each label is associated with a specific encoding type.
 * @param options Is a TextDecoderOptions dictionary with the property:
 *                   fatal
 *                      A Boolean flag indicating if the TextDecoder.decode() method must throw a DOMException
 *                      with the "EncodingError" value when an coding error is found. It defaults to false.
 *                      Can use TextDecoder.fatal if this is the only property.
 */
@js.native
@JSGlobal
class TextDecoder(
    utfLabel: js.UndefOr[String] = js.undefined,
    options: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
) extends js.Object {
  val encoding: String = js.native

  /**
   *
   * @param bufferOptional Is either an ArrayBuffer or an ArrayBufferView containing the text to decode.
   * @param optionsOptional Is a TextDecodeOptions dictionary with the property:
   *                            stream
   *                                A Boolean flag indicating that additional data will follow in subsequent calls to decode().
   *                                Set to true if processing the data in chunks, and false for the final chunk or if the data
   *                                is not chunked. It defaults to false.
   *                                Can use TextDecoder.stream if this is the only property.
   */
  def decode(
      buffer: js.UndefOr[ ArrayBufferView | ArrayBuffer ] = js.undefined,
      options: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): String = js.native
}

object TextDecoder {

  /**
   * @param utfLable defaulting to "utf-8", containing the label of the encoder. Each label is associated with a specific encoding type.
   * @param options Is a TextDecoderOptions dictionary with the property:
   *                   fatal
   *                      A Boolean flag indicating if the TextDecoder.decode() method must throw a DOMException
   *                      with the "EncodingError" value when an coding error is found. It defaults to false.
   *                      Can use TextDecoder.fatal if this is the only property.
   */
  def apply(
      utfLabel: js.UndefOr[String] = js.undefined,
      options: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ) = new TextDecoder(utfLabel,options)

  /**
   * For setting the fatal property in the options parameter in the constructor.
   */
  def fatal = js.Dictionary[js.Any]("fatal"->true)

  /**
   * For setting the stream property in the options parameter in the decode method.
   */
  def stream = js.Dictionary[js.Any]("stream"->true)
}
