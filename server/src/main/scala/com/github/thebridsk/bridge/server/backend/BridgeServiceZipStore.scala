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
import scala.reflect.io.File
import com.github.thebridsk.bridge.server.backend.resource.ZipFileForStore
import com.github.thebridsk.bridge.server.backend.resource.ZipStore
import com.github.thebridsk.bridge.server.backend.resource.JavaResourcePersistentSupport
import com.github.thebridsk.bridge.server.backend.resource.ZipPersistentSupport
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.SystemTime.Timestamp

object BridgeServiceZipStore {

  val log = Logger(getClass().getName)

}

/**
  * An implementation of the backend for our service.
  * This is used for testing, and is predefined with
  * resources to facilitate testing.
  * @author werewolf
  */
class BridgeServiceZipStore(
    id: String,
    val zipfilename: File,
    useYaml: Boolean = true
)(
    implicit
    execute: ExecutionContext
) extends BridgeServiceWithLogging(id) {
  self =>

  import BridgeServiceZipStore._

  import scala.collection.mutable.Map

  override def getDate: Timestamp = {
    zipfilename.lastModified
  }

  val bridgeResources = BridgeResources(useYaml, true, false, true)
  import bridgeResources._

  val zipfile = new ZipFileForStore(zipfilename)

  val chicagos = ZipStore[Id.MatchDuplicate, MatchChicago](id, zipfile)
  val duplicates = ZipStore[Id.MatchDuplicate, MatchDuplicate](id, zipfile)
  val duplicateresults =
    ZipStore[Id.MatchDuplicateResult, MatchDuplicateResult](id, zipfile)
  val rubbers = ZipStore[String, MatchRubber](id, zipfile)

  val boardSets = MultiStore[String, BoardSet](
    id,
    List(
      new ZipPersistentSupport(zipfile),
      new JavaResourcePersistentSupport(
        "/com/github/thebridsk/bridge/backend/",
        "Boardsets.txt",
        self.getClass.getClassLoader
      )
    )
  )

  val movements = MultiStore[String, Movement](
    id,
    List(
      new ZipPersistentSupport(zipfile),
      new JavaResourcePersistentSupport(
        "/com/github/thebridsk/bridge/backend/",
        "Movements.txt",
        self.getClass.getClassLoader
      )
    )
  )

  /**
    * closes the store.  Do not call other methods after calling this method.
    */
  def close() = {
    zipfile.close()
  }

  /**
    * closes and deletes the zip file.  Do not call other methods after calling this method.
    * @return  <code>true</code> if zip file is successfully deleted; <code>false</code> otherwise
    */
  override def delete() = Future {
    zipfile.close()
    zipfilename.delete()
    Result(zipfilename.name)
  }

  override def toString() = "BridgeServiceZipStore"
}
