package com.example.service.graphql

import play.api.libs.json._
import sangria.parser.QueryParser
import scala.util.Success
import scala.util.Failure
import sangria.execution.Executor
import com.example.backend.BridgeService
import sangria.execution.deferred.DeferredResolver
import scala.concurrent.ExecutionContext.Implicits.global
import sangria.marshalling.playJson._
import akka.http.scaladsl.model.StatusCodes
import sangria.execution.QueryAnalysisError
import sangria.execution.ErrorWithResolver
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCode
import utils.logging.Logger
import sangria.parser.SyntaxError

object Query {

  val log = Logger[Query]
}

class Query {
  import Query.log

  def query( requestJson: JsObject, store: BridgeService ): Future[(StatusCode,JsValue)] = {
    val JsObject(fields) = requestJson

    val JsString(query) = fields("query")

    val operation = fields.get("operationName") collect {
      case JsString(op) => op
    }

    val vars = fields.get("variables") match {
      case Some(obj: JsObject) => obj
      case _ => JsObject.empty
    }

    try {
      QueryParser.parse(query) match {

        case Success(queryAst) =>
          Executor.execute(SchemaDefinition.BridgeScorerSchema,
                                   queryAst,
                                   store,
                                   variables = vars,
                                   operationName = operation
                                  )
            .map(StatusCodes.OK -> _)
            .recover {
              case error: QueryAnalysisError =>
                log.info( s"Error executing GraphQL query", error )
                StatusCodes.BadRequest -> error.resolveError
              case error: ErrorWithResolver =>
                log.info( s"Error executing GraphQL query", error )
                StatusCodes.InternalServerError -> error.resolveError
            }

        case Failure(error) =>
          error match {
            case error: SyntaxError =>
              log.info( s"Syntax error parsing GraphQL query", error )
              Future.successful( StatusCodes.BadRequest -> JsObject( Seq(("error", JsString(error.getMessage)))) )
            case error: Exception =>
              log.info( s"Syntax error parsing GraphQL query ${error.getClass.getName}", error )
              Future.successful( StatusCodes.InternalServerError -> JsObject( Seq(("error", JsString("Unknown error")))) )
          }
      }
    } catch {
      case error: Exception =>
        log.info( s"Error parsing GraphQL query ${error.getClass.getName}", error )
        Future.successful( StatusCodes.InternalServerError -> JsObject( Seq(("error", JsString("Unknown error")))) )
    }
  }
}