package com.example.backend

import akka.http.scaladsl.model.StatusCode
import com.example.data.RestMessage
import scala.concurrent.Promise
import scala.concurrent.Future
import utils.logging.Logger
import org.scalactic.source.Position
import com.example.source.SourcePosition
import com.example.data.VersionedInstance
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure
import java.util.concurrent.atomic.AtomicInteger

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
