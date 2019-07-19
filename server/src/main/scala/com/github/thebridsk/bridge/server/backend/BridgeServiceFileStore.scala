package com.github.thebridsk.bridge.server.backend

import com.github.thebridsk.bridge.server.backend.resource.StoreSupport
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.server.backend.resource.FileStore
import scala.reflect.io.Directory
import com.github.thebridsk.bridge.server.backend.resource.MultiStore
import com.github.thebridsk.bridge.server.backend.resource.JavaResourceStore
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.MatchDuplicateV1
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchChicagoV1
import com.github.thebridsk.bridge.data.MatchChicagoV2
import com.github.thebridsk.bridge.server.backend.resource.VersionedInstanceJson
import com.github.thebridsk.bridge.data.MatchDuplicateV2
import com.github.thebridsk.bridge.server.backend.resource.FileIO
import com.github.thebridsk.bridge.server.backend.resource.JsonYamlConverter
import com.github.thebridsk.bridge.server.backend.resource.JsonConverter
import com.github.thebridsk.bridge.server.backend.resource.YamlConverter
import play.api.libs.json._
import com.github.thebridsk.bridge.server.yaml.YamlSupport._
import com.github.thebridsk.bridge.data.Id
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.data.MatchDuplicateResultV1
import com.github.thebridsk.bridge.server.backend.resource.Converter

class BridgeServiceFileStoreConverters(yaml: Boolean) {

  implicit val converter = Converter.getConverter(yaml)

  implicit val matchChicagoJson = VersionedInstanceJson[String, MatchChicago]
    .add[MatchChicagoV2]
    .add[MatchChicagoV1]

  implicit val matchDuplicateJson =
    VersionedInstanceJson[String, MatchDuplicate]
      .add[MatchDuplicateV2]
      .add[MatchDuplicateV1]

  implicit val matchDuplicateResultJson =
    VersionedInstanceJson[String, MatchDuplicateResult]
      .add[MatchDuplicateResultV1]

  implicit val matchRubberJson = VersionedInstanceJson[String, MatchRubber]

  implicit val boardSetJson = VersionedInstanceJson[String, BoardSet]

  implicit val movementJson = VersionedInstanceJson[String, Movement]

}

object BridgeServiceFileStore {

  val log = Logger(getClass().getName)

}

/**
  * An implementation of the backend for our service.
  * This is used for testing, and is predefined with
  * resources to facilitate testing.
  * @author werewolf
  */
class BridgeServiceFileStore(
    val dir: Directory,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false,
    useYaml: Boolean = true,
    oid: Option[String] = None
)(
    implicit
    execute: ExecutionContext
) extends BridgeServiceWithLogging(oid.getOrElse(dir.toString())) {
  self =>

  import BridgeServiceFileStore._

  import scala.collection.mutable.Map

  val bridgeResources =
    BridgeResources(useYaml, false, useIdFromValue, dontUpdateTime)
  import bridgeResources._

  dir.createDirectory(true, false)

  val chicagos = FileStore[Id.MatchDuplicate, MatchChicago](id, dir)
  val duplicates = FileStore[Id.MatchDuplicate, MatchDuplicate](id, dir)
  val duplicateresults =
    FileStore[Id.MatchDuplicateResult, MatchDuplicateResult](id, dir)
  val rubbers = FileStore[String, MatchRubber](id, dir)

  val boardSets = MultiStore.createFileAndResource[String, BoardSet](
    id,
    dir,
    "/com/github/thebridsk/bridge/backend/",
    "Boardsets.txt",
    self.getClass.getClassLoader
  )

  val movements = MultiStore.createFileAndResource[String, Movement](
    id,
    dir,
    "/com/github/thebridsk/bridge/backend/",
    "Movements.txt",
    self.getClass.getClassLoader
  )

  override val importStore = {
    val importdir = (dir / "import").toDirectory
    Some(new FileImportStore(importdir))
  }

  override def toString() = "BridgeServiceFileStore"
}
