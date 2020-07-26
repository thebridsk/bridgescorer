package com.github.thebridsk.bridge.server.backend

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import com.github.thebridsk.bridge.server.backend.resource.MyCache
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.reflect.io.Directory
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.server.backend.resource.Implicits._
import scala.reflect.io.File
import com.github.thebridsk.utilities.file.FileIO
import java.io.IOException
import com.github.thebridsk.utilities.logging.Logger
import ImportStore._
import java.nio.file.Path
import java.io.FileNotFoundException
import com.github.thebridsk.bridge.data.ImportStoreConstants
import java.nio.file.NoSuchFileException

object ImportStore {

  val log: Logger = Logger[ImportStore]()

  val importStoreExtension = ImportStoreConstants.importStoreFileExtension
  val importStoreDotExtension: String = s".${importStoreExtension}"
}

abstract class ImportStore()(
    implicit
    execute: ExecutionContext
) {

  /**
    * Returns all the IDs of imported stores.  The ID is the the filename of the zip store, or the name of the dir for a file store.
    * @return a future that returns the list of IDs
    */
  def getAllIds(): Future[Result[List[String]]]

  def get(id: String): Future[Result[BridgeService]]

  def delete(id: String): Future[Result[BridgeService]]

  /**
    * Create a new store for imports.  This will copy the zip file into the imports area of the persistent store.
    * @param id
    * @param zipfile the filename of the zip file to import.
    * @return a future to the result of the operation.  Returns a BridgeService if successful.
    * Error is returned if id already exists.
    */
  def create(id: String, zipfile: File): Future[Result[BridgeService]]
}

class FileImportStore(
    val directory: Directory,
    val cacheInitialCapacity: Int = 5,
    val cacheMaxCapacity: Int = 100,
    val cacheTimeToLive: Duration = 61 minutes,
    val cacheTimeToIdle: Duration = 60 minutes
)(
    implicit
    execute: ExecutionContext
) extends ImportStore {

  private val cache = new MyCache[String, Result[BridgeService]](
    cacheInitialCapacity,
    cacheMaxCapacity,
    cacheTimeToLive,
    cacheTimeToIdle
  )

  lazy val dir: Directory = {
    directory.createDirectory(true, false)
    directory
  }

  /**
    * Returns all the IDs of imported stores.  The ID is the the filename of the zip store, or the name of the directory for a file store.
    * @return a future that returns the list of IDs
    */
  def getAllIds(): Future[Result[List[String]]] = {
    Future {
      Result(
        dir.dirs.map(dir => dir.name).toList :::
          dir.files
            .filter(file => file.extension == "zip" || file.extension == importStoreExtension)
            .map(file => file.name)
            .toList
      )
    }.logit(s"getAllIds /imports")
  }

  def get(id: String): Future[Result[BridgeService]] = {
    cache
      .read(
        id, {
          val path = dir / id
          if (path.isDirectory) {
            Result(new BridgeServiceFileStore(path.toDirectory, false, true))
          } else if (path.extension == "zip" || path.extension == importStoreExtension) {
            try {
              Result(new BridgeServiceZipStore(path.name, path.toFile))
            } catch {
              case e @ (_ : FileNotFoundException | _ : NoSuchFileException) =>
                Result(StatusCodes.NotFound, "Not found")
              case x: IOException =>
                Result(StatusCodes.NotFound, "IO Error")
            }
          } else {
            Result(StatusCodes.NotFound, "File not a bridgestore file")
          }
        }
      )
      .logit(s"get /imports/${id}")
  }

  def delete(id: String): Future[Result[BridgeService]] = {
    cache
      .delete(
        id,
        None, // optional function to get old value, None means use cache value
        { ofrbs => // old value => Future[Result[BridgeService]]  return becomes the return of the function
          ofrbs match {
            case Some(frbs) =>
              frbs.flatMap { rbs =>
                rbs match {
                  case Right(bs) =>
                    bs.delete()
                      .logit(s"delete BridgeService /imports/${id}")
                      .map { s =>
                        rbs
                      }
                  case Left(error) =>
                    Result(error).toFuture
                }
              }
            case None =>
              Result(StatusCodes.NotFound, "Not found").toFuture
          }
        }, { frbs => // future calls to get(id) get this   returns Future[Result[BridgeService]]
          Result(StatusCodes.NotFound, "Not found").toFuture
        }
      )
      .logit(s"delete /imports/${id}")
  }

  /**
    * Create a new store for imports.  This will copy the zip file into the imports area of the persistent store.
    * @param id
    * @param zipfile the filename of the zip file to import.
    * @return a future to the result of the operation.  Returns a BridgeService if successful.
    * Error is returned if id already exists.
    */
  def create(id: String, zipfile: File): Future[Result[BridgeService]] = {
    if (!id.endsWith(".zip") && !id.endsWith(importStoreDotExtension))
      Result(StatusCodes.BadRequest, s"Not a valid Id: ${id}").toFuture
    else {
      get(id)
        .flatMap { rold =>
          rold match {
            case Right(old) =>
              Result(StatusCodes.BadRequest, s"Id /imports/$id already exists").toFuture
            case Left((statusCode, msg)) =>
              cache.update(
                id,
                oldv =>
                  Future {
                    val importzipfile = dir / id
                    var source: Path = null
                    var target: Path = null
                    try {
                      source = FileIO.getPath(zipfile.toString())
                      target = FileIO.getPath(importzipfile.toString())
                      FileIO.copyFile(source, target)
                      Result(
                        new BridgeServiceZipStore(
                          importzipfile.name,
                          importzipfile.toFile
                        )
                      )
                    } catch {
                      case x: IOException =>
                        log.warning(
                          s"""Error during importing, copy zipfile ${source} to ${target} failed""",
                          x
                        )
                        Result(StatusCodes.InternalServerError, s"Oops")
                    }
                  },
                Some(() => Future(rold))
              )
          }
        }
        .logit(s"create /imports/${id} zipfile ${zipfile}")
    }
  }
}
