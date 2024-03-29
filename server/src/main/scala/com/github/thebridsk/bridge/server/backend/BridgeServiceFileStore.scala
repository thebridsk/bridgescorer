package com.github.thebridsk.bridge.server.backend

import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.server.backend.resource.FileStore
import scala.reflect.io.Directory
import com.github.thebridsk.bridge.server.backend.resource.MultiStore
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.MatchDuplicateV1
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchChicagoV1
import com.github.thebridsk.bridge.data.MatchChicagoV2
import com.github.thebridsk.bridge.server.backend.resource.VersionedInstanceJson
import com.github.thebridsk.bridge.data.MatchDuplicateV2
import com.github.thebridsk.bridge.server.yaml.YamlSupport._
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.data.MatchDuplicateResultV1
import com.github.thebridsk.bridge.server.backend.resource.Converter
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.IndividualDuplicate

class BridgeServiceFileStoreConverters(yaml: Boolean) {

  implicit val converter: Converter = Converter.getConverter(yaml)

  implicit val matchChicagoJson
      : VersionedInstanceJson[MatchChicago.Id, MatchChicago] =
    VersionedInstanceJson[MatchChicago.Id, MatchChicago]
      .add[MatchChicagoV2]
      .add[MatchChicagoV1]

  implicit val matchDuplicateJson
      : VersionedInstanceJson[MatchDuplicate.Id, MatchDuplicate] =
    VersionedInstanceJson[MatchDuplicate.Id, MatchDuplicate]
      .add[MatchDuplicateV2]
      .add[MatchDuplicateV1]

  implicit val individualDuplicateJson
      : VersionedInstanceJson[IndividualDuplicate.Id, IndividualDuplicate] =
    VersionedInstanceJson[IndividualDuplicate.Id, IndividualDuplicate]

  implicit val matchDuplicateResultJson
      : VersionedInstanceJson[MatchDuplicateResult.Id, MatchDuplicateResult] =
    VersionedInstanceJson[MatchDuplicateResult.Id, MatchDuplicateResult]
      .add[MatchDuplicateResultV1]

  implicit val matchRubberJson
      : VersionedInstanceJson[MatchRubber.Id, MatchRubber] =
    VersionedInstanceJson[MatchRubber.Id, MatchRubber]

  implicit val boardSetJson: VersionedInstanceJson[BoardSet.Id, BoardSet] =
    VersionedInstanceJson[BoardSet.Id, BoardSet]

  implicit val movementJson: VersionedInstanceJson[Movement.Id, Movement] =
    VersionedInstanceJson[Movement.Id, Movement]

  implicit val individualMovementJson: VersionedInstanceJson[IndividualMovement.Id, IndividualMovement] =
    VersionedInstanceJson[IndividualMovement.Id, IndividualMovement]

}

object BridgeServiceFileStore {

  val log: Logger = Logger(getClass().getName)

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
)(implicit
    execute: ExecutionContext
) extends BridgeServiceWithLogging(oid.getOrElse(dir.toString())) {
  self =>

  val bridgeResources: BridgeResources =
    BridgeResources(useYaml, false, useIdFromValue, dontUpdateTime)
  import bridgeResources._

  dir.createDirectory(true, false)

  val chicagos: FileStore[MatchChicago.Id, MatchChicago] =
    FileStore[MatchChicago.Id, MatchChicago](id, dir)
  val duplicates: FileStore[MatchDuplicate.Id, MatchDuplicate] =
    FileStore[MatchDuplicate.Id, MatchDuplicate](id, dir)
  val individualduplicates: FileStore[IndividualDuplicate.Id, IndividualDuplicate] =
    FileStore[IndividualDuplicate.Id, IndividualDuplicate](id, dir)
  val duplicateresults
      : FileStore[MatchDuplicateResult.Id, MatchDuplicateResult] =
    FileStore[MatchDuplicateResult.Id, MatchDuplicateResult](id, dir)
  val rubbers: FileStore[MatchRubber.Id, MatchRubber] =
    FileStore[MatchRubber.Id, MatchRubber](id, dir)

  val boardSets: MultiStore[BoardSet.Id, BoardSet] =
    MultiStore.createFileAndResource[BoardSet.Id, BoardSet](
      id,
      dir,
      "/com/github/thebridsk/bridge/server/backend/",
      "Boardsets.txt",
      self.getClass.getClassLoader
    )

  val movements: MultiStore[Movement.Id, Movement] =
    MultiStore.createFileAndResource[Movement.Id, Movement](
      id,
      dir,
      "/com/github/thebridsk/bridge/server/backend/",
      "Movements.txt",
      self.getClass.getClassLoader
    )

  val individualMovements: MultiStore[IndividualMovement.Id, IndividualMovement] =
    MultiStore.createFileAndResource[IndividualMovement.Id, IndividualMovement](
      id,
      dir,
      "/com/github/thebridsk/bridge/server/backend/",
      "IndividualMovements.txt",
      self.getClass.getClassLoader
    )

  override val importStore: Some[FileImportStore] = {
    val importdir = (dir / "import").toDirectory
    Some(new FileImportStore(importdir))
  }

  override def toString() = "BridgeServiceFileStore"
}
