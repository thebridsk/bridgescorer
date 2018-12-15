package com.example.backend

import com.example.backend.resource.StoreSupport
import com.example.data.Team
import com.example.data.MatchChicago
import com.example.data.MatchDuplicate
import com.example.data.MatchDuplicateResult
import com.example.data.Hand
import com.example.data.Board
import com.example.data.Table
import com.example.backend.resource.FileStore
import scala.reflect.io.Directory
import com.example.backend.resource.MultiStore
import com.example.backend.resource.JavaResourceStore
import com.example.data.BoardSet
import com.example.data.Movement
import com.example.data.MatchRubber
import com.example.data.MatchDuplicateV1
import utils.logging.Logger
import com.example.data.MatchChicagoV1
import com.example.data.MatchChicagoV2
import com.example.backend.resource.VersionedInstanceJson
import com.example.data.MatchDuplicateV2
import com.example.backend.resource.FileIO
import com.example.backend.resource.JsonYamlConverter
import com.example.backend.resource.JsonConverter
import com.example.backend.resource.YamlConverter
import play.api.libs.json._
import com.example.yaml.YamlSupport._
import com.example.data.Id
import scala.concurrent.ExecutionContext
import com.example.data.MatchDuplicateResultV1
import com.example.backend.resource.Converter

class BridgeServiceFileStoreConverters( yaml: Boolean ) {

  implicit val converter =  Converter.getConverter(yaml)

  implicit val matchChicagoJson = VersionedInstanceJson[String,MatchChicago].add[MatchChicagoV2].add[MatchChicagoV1]

  implicit val matchDuplicateJson = VersionedInstanceJson[String,MatchDuplicate].add[MatchDuplicateV2].add[MatchDuplicateV1]

  implicit val matchDuplicateResultJson = VersionedInstanceJson[String,MatchDuplicateResult].add[MatchDuplicateResultV1]

  implicit val matchRubberJson = VersionedInstanceJson[String,MatchRubber]

  implicit val boardSetJson = VersionedInstanceJson[String,BoardSet]

  implicit val movementJson = VersionedInstanceJson[String,Movement]

}

object BridgeServiceFileStore {

  val log = Logger( getClass().getName )

}

/**
 * An implementation of the backend for our service.
 * This is used for testing, and is predefined with
 * resources to facilitate testing.
 * @author werewolf
 */
class BridgeServiceFileStore( val dir: Directory,
                              useIdFromValue: Boolean = false,
                              dontUpdateTime: Boolean = false,
                              useYaml: Boolean = true,
                              oid: Option[String] = None
                            )(
                              implicit
                                execute: ExecutionContext
                            ) extends BridgeServiceWithLogging( oid.getOrElse( dir.toString() ) ) {
  self =>

  import BridgeServiceFileStore._

  import scala.collection.mutable.Map

  val bridgeResources = BridgeResources(useYaml,false,useIdFromValue,dontUpdateTime)
  import bridgeResources._

  dir.createDirectory(true, false)

  val chicagos = FileStore[Id.MatchDuplicate,MatchChicago](id,dir)
  val duplicates = FileStore[Id.MatchDuplicate,MatchDuplicate](id,dir)
  val duplicateresults = FileStore[Id.MatchDuplicateResult,MatchDuplicateResult](id,dir)
  val rubbers = FileStore[String,MatchRubber](id,dir)

  val boardSets = MultiStore.createFileAndResource[String,BoardSet](id,dir, "/com/example/backend/", "Boardsets.txt", self.getClass.getClassLoader)

  val movements = MultiStore.createFileAndResource[String,Movement](id,dir, "/com/example/backend/", "Movements.txt", self.getClass.getClassLoader)

  override
  val importStore = {
    val importdir = (dir / "import").toDirectory
    Some( new FileImportStore( importdir ))
  }

  override
  def toString() = "BridgeServiceFileStore"
}
