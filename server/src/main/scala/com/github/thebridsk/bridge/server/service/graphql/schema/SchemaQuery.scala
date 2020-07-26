package com.github.thebridsk.bridge.server.service.graphql.schema

import sangria.schema._
import com.github.thebridsk.bridge.server.backend.BridgeService

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger

import SchemaBase.{log => _}
import SchemaHand.{log => _}
import SchemaDuplicate.{log => _}
import SchemaRubber.{log => _}
import SchemaChicago.{log => _}
import SchemaService.{log => _, _}

object SchemaQuery {

  val log: Logger = Logger(SchemaQuery.getClass.getName)

  val QueryType: ObjectType[BridgeService,BridgeService] = ObjectType(
    "Query",
    fields[BridgeService, BridgeService](
      Field(
        "importIds",
        ListType(ImportIdType),
        resolve = _.ctx.importStore match {
          case Some(is) =>
            is.getAllIds()
              .map(
                rlist =>
                  rlist match {
                    case Right(list) =>
                      list
                    case Left((statusCode, msg)) =>
                      throw new Exception(
                        s"Error getting import store ids: ${statusCode} ${msg.msg}"
                      )
                  }
              )
          case None =>
            throw new Exception("Did not find the import store")
        }
      ),
      Field(
        "importsCount",
        IntType,
        resolve = _.ctx.importStore match {
          case Some(is) =>
            is.getAllIds()
              .map(
                rlist =>
                  rlist match {
                    case Right(list) =>
                      list.size
                    case Left((statusCode, msg)) =>
                      throw new Exception(
                        s"Error getting number of imports: ${statusCode} ${msg.msg}"
                      )
                  }
              )
          case None =>
            throw new Exception("Did not find the import store")
        }
      ),
      Field(
        "imports",
        ListType(BridgeServiceType),
        resolve = ServiceAction.getAllImportFromRoot
      ),
      Field(
        "import",
        OptionType(BridgeServiceType),
        arguments = ArgImportId :: Nil,
        resolve = ServiceAction.getImportFromRoot
      ),
      Field(
        "mainStore",
        OptionType(BridgeServiceType),
        resolve = ctx => ctx.ctx
      )
    ) ++
      BridgeServiceFields
  )

}
