package com.github.thebridsk.bridge.server.service.graphql.schema

import sangria.schema._
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.BridgeService

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchDuplicateResult

import SchemaBase.{log => _}
import SchemaHand.{log => _}
import SchemaDuplicate.{log => _, _}
import SchemaRubber.{log => _, _}
import SchemaChicago.{log => _, _}
import SchemaService.{log => _, _}
import scala.util.Using

object SchemaMutation {

  val log = Logger(SchemaMutation.getClass.getName)

  val MutationImportType = ObjectType(
    "MutationImport",
    fields[BridgeService, BridgeService](
      Field(
        "id",
        ImportIdType,
        Some("The id of the import store"),
        resolve = _.value.id
      ),
      Field(
        "importduplicate",
        MatchDuplicateType,
        description = Some("Import a duplicate match."),
        arguments = ArgDuplicateId :: Nil,
        resolve = ctx =>
          ImportAction.importDuplicate(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "importduplicateresult",
        MatchDuplicateResultType,
        description = Some("Import a duplicate result."),
        arguments = ArgDuplicateResultId :: Nil,
        resolve = ctx =>
          ImportAction.importDuplicateResult(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "importrubber",
        MatchRubberType,
        description = Some("Import a rubber match."),
        arguments = ArgRubberId :: Nil,
        resolve = ctx =>
          ImportAction.importRubber(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "importchicago",
        MatchChicagoType,
        description = Some("Import a chicago match."),
        arguments = ArgChicagoId :: Nil,
        resolve = ctx =>
          ImportAction.importChicago(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "delete",
        BooleanType,
        description = Some("Delete the import store"),
        resolve = ImportAction.deleteImportStore
      )
    )
  )

  val MutationType = ObjectType(
    "Mutation",
    fields[BridgeService, BridgeService](
      Field(
        "import",
        OptionType(MutationImportType),
        description = Some(
          "Selecting the import store from which imports are done.  returns null if not found."
        ),
        arguments = ArgImportId :: Nil,
        resolve = ServiceAction.getImportFromRoot
      )
    )
  )

}

object ImportAction {
  import SchemaMutation._

  def importDuplicate(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchDuplicate] = {
    val dupId = ctx arg ArgDuplicateId
    val bs = ctx.value
    bs.duplicates.read(dupId).flatMap { rdup =>
      rdup match {
        case Right(dup) =>
          ctx.ctx.duplicates.importChild(dup).map { rc =>
            rc match {
              case Right(cdup) =>
                val importedId = cdup.id
                bs.duplicates.persistent.listFiles(dup.id) match {
                  case Left((statusCode, msg)) =>
                    throw new Exception(
                      s"Error importing images into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                    )
                  case Right(mdfs) =>
                    mdfs.foreach { mdf =>
                      bs.duplicates.persistent.read(dup.id,mdf) match {
                        case Left((statusCode, msg)) =>
                          throw new Exception(
                            s"Error importing images into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                          )
                        case Right(data) =>
                          Using.resource(data) { is =>
                            ctx.ctx.duplicates.persistent.write(importedId,is,mdf) match {
                              case Left((statusCode, msg)) =>
                                throw new Exception(
                                  s"Error importing images into store: ${dupId} from import store ${bs.getClass().getName()}(${bs.id}) to ${ctx.ctx.getClass().getName()}(${ctx.ctx.id}): ${statusCode} ${msg.msg}"
                                )
                              case Right(_) =>
                            }
                          }
                      }
                    }
                }
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def importDuplicateResult(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchDuplicateResult] = {
    val dupId = ctx arg ArgDuplicateResultId
    val bs = ctx.value
    bs.duplicateresults.read(dupId).flatMap { rdup =>
      rdup match {
        case Right(dup) =>
          ctx.ctx.duplicateresults.importChild(dup).map { rc =>
            rc match {
              case Right(cdup) =>
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def importRubber(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchRubber] = {
    val rubId = ctx arg ArgRubberId
    val bs = ctx.value
    bs.rubbers.read(rubId).flatMap { rdup =>
      rdup match {
        case Right(rub) =>
          ctx.ctx.rubbers.importChild(rub).map { rc =>
            rc match {
              case Right(cdup) =>
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${rubId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${rubId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def importChicago(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchChicago] = {
    val chiId = ctx arg ArgChicagoId
    val bs = ctx.value
    bs.chicagos.read(chiId).flatMap { rdup =>
      rdup match {
        case Right(chi) =>
          ctx.ctx.chicagos.importChild(chi).map { rc =>
            rc match {
              case Right(cdup) =>
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${chiId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${chiId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def deleteImportStore(
      ctx: Context[BridgeService, BridgeService]
  ): Future[Boolean] = {
    val todelete = ctx.value
    ctx.ctx.importStore match {
      case Some(is) =>
        is.delete(todelete.id)
          .map { rd =>
            rd match {
              case Right(x) =>
                log.warning(s"Deleted import store ${todelete.id}")
                true
              case Left(error) =>
                log.warning(
                  s"Error deleting import store ${todelete.id}: ${error}"
                )
                false
            }
          }
          .recover {
            case x: Exception =>
              log.warning(s"Error deleting import store ${todelete.id}", x)
              false
          }
      case None =>
        log.fine("Did not find import store")
        Future.successful(false)
    }
  }

}
