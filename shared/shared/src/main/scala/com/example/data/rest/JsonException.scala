package com.example.data.rest

class JsonException(msg: String, cause: Throwable)
    extends Exception(msg, cause) {
  def this(msg: String) = this(msg, null)
}
