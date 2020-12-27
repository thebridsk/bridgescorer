package com.github.thebridsk.bridge.server.backend

import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.server.backend.resource.MultiStore
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.ExecutionContext
import scala.reflect.io.File
import com.github.thebridsk.bridge.server.backend.resource.ZipFileForStore
import com.github.thebridsk.bridge.server.backend.resource.ZipStore
import com.github.thebridsk.bridge.server.backend.resource.JavaResourcePersistentSupport
import com.github.thebridsk.bridge.server.backend.resource.ZipPersistentSupport
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.IndividualDuplicate

object BridgeServiceZipStore {

  val log: Logger = Logger(getClass().getName)

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
)(implicit
    execute: ExecutionContext
) extends BridgeServiceWithLogging(id) {
  self =>

  override def getDate: Timestamp = {
    zipfilename.lastModified.toDouble
  }

  val bridgeResources: BridgeResources =
    BridgeResources(useYaml, true, false, true)
  import bridgeResources._

  val zipfile = new ZipFileForStore(zipfilename)

  val chicagos: ZipStore[MatchChicago.Id, MatchChicago] =
    ZipStore[MatchChicago.Id, MatchChicago](id, zipfile)
  val duplicates: ZipStore[MatchDuplicate.Id, MatchDuplicate] =
    ZipStore[MatchDuplicate.Id, MatchDuplicate](id, zipfile)
  val individualduplicates: ZipStore[IndividualDuplicate.Id, IndividualDuplicate] =
    ZipStore[IndividualDuplicate.Id, IndividualDuplicate](id, zipfile)
  val duplicateresults
      : ZipStore[MatchDuplicateResult.Id, MatchDuplicateResult] =
    ZipStore[MatchDuplicateResult.Id, MatchDuplicateResult](id, zipfile)
  val rubbers: ZipStore[MatchRubber.Id, MatchRubber] =
    ZipStore[MatchRubber.Id, MatchRubber](id, zipfile)

  val boardSets: MultiStore[BoardSet.Id, BoardSet] =
    MultiStore[BoardSet.Id, BoardSet](
      id,
      List(
        new ZipPersistentSupport(zipfile),
        new JavaResourcePersistentSupport(
          "/com/github/thebridsk/bridge/server/backend/",
          "Boardsets.txt",
          self.getClass.getClassLoader
        )
      )
    )

  val movements: MultiStore[Movement.Id, Movement] =
    MultiStore[Movement.Id, Movement](
      id,
      List(
        new ZipPersistentSupport(zipfile),
        new JavaResourcePersistentSupport(
          "/com/github/thebridsk/bridge/server/backend/",
          "Movements.txt",
          self.getClass.getClassLoader
        )
      )
    )

  val individualMovements: MultiStore[IndividualMovement.Id, IndividualMovement] =
    MultiStore[IndividualMovement.Id, IndividualMovement](
      id,
      List(
        new ZipPersistentSupport(zipfile),
        new JavaResourcePersistentSupport(
          "/com/github/thebridsk/bridge/server/backend/",
          "IndividualMovements.txt",
          self.getClass.getClassLoader
        )
      )
    )

  /**
    * closes the store.  Do not call other methods after calling this method.
    */
  def close(): Unit = {
    zipfile.close()
  }

  /**
    * closes and deletes the zip file.  Do not call other methods after calling this method.
    * @return  <code>true</code> if zip file is successfully deleted; <code>false</code> otherwise
    */
  override def delete(): Future[Result[String]] =
    Future {
      zipfile.close()
      zipfilename.delete()
      Result(zipfilename.name)
    }

  override def toString() = "BridgeServiceZipStore"
}
