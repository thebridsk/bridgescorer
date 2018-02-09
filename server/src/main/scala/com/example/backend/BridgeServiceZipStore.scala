package com.example.backend

import com.example.data.LoggerConfig
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
import scala.reflect.io.File
import com.example.backend.resource.ZipFileForStore
import com.example.backend.resource.ZipStore
import com.example.backend.resource.JavaResourcePersistentSupport
import com.example.backend.resource.ZipPersistentSupport
import com.example.backend.resource.Result
import scala.concurrent.Future
import com.example.data.SystemTime.Timestamp

object BridgeServiceZipStore {

  val log = Logger( getClass().getName )

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
                           ) extends BridgeServiceWithLogging( id ) {
  self =>

  import BridgeServiceZipStore._

  import scala.collection.mutable.Map

  override
  def getDate: Timestamp = {
    zipfilename.lastModified
  }

  val bridgeResources = BridgeResources(useYaml,true,false,true)
  import bridgeResources._

  val zipfile = new ZipFileForStore(zipfilename)

  val chicagos = ZipStore[Id.MatchDuplicate,MatchChicago](zipfile)
  val duplicates = ZipStore[Id.MatchDuplicate,MatchDuplicate](zipfile)
  val duplicateresults = ZipStore[Id.MatchDuplicateResult,MatchDuplicateResult](zipfile)
  val rubbers = ZipStore[String,MatchRubber](zipfile)

  val boardSets = MultiStore[String,BoardSet]( List(
                                                    new ZipPersistentSupport(zipfile),
                                                    new JavaResourcePersistentSupport("/com/example/backend/", "Boardsets.txt", self.getClass.getClassLoader)
                                              ))

  val movements = MultiStore[String,Movement]( List(
                                                    new ZipPersistentSupport(zipfile),
                                                    new JavaResourcePersistentSupport("/com/example/backend/", "Movements.txt", self.getClass.getClassLoader)
                                              ))

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
  override
  def delete() = Future {
    zipfile.close()
    zipfilename.delete()
    Result( zipfilename.name )
  }

  override
  def toString() = "BridgeServiceZipStore"
}
