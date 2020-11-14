package com.github.thebridsk.bridge.server.backend

import akka.http.scaladsl.model.StatusCode
import com.github.thebridsk.bridge.data.RestMessage

/**
  * Provides type definitions.
  */
package object resource {

  /**
    * The result of an operation on the [[Store]].
    * A successful result is held in a [[Right]], with the value in the [[Right]] object.
    * An error is held in a [[Left]], the contents is a tuple2, the first entry is
    * the HTML status code, and the second is a message.
    */
  type Result[T] = Either[(StatusCode, RestMessage), T]

}
