package com.example.service.graphql

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import play.api.libs.json._
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import sangria.execution.Executor
import sangria.execution.deferred.DeferredResolver
import sangria.execution.QueryAnalysisError
import sangria.execution.ErrorWithResolver
import scala.util.Failure
import com.example.backend.BridgeService
import akka.http.scaladsl.server.Route
import com.example.data.rest.JsonSupport._
import com.example.rest.UtilsPlayJson._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

trait GraphQLRoute {

  implicit val restService: BridgeService

  val query = new Query()

  implicit val jsObjectReads = Reads.JsObjectReads

  val graphQLRoute: Route =
    (post & path("graphql")) {
      entity(as[JsObject]) { requestJson ⇒
        val f = query.query(requestJson, restService)
        onComplete(f) {
          case Success((statusCode,obj)) =>
            complete(statusCode, obj)
          case Failure(ex) =>
            complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
        }
      }
}
}
