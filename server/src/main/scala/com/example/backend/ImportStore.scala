package com.example.backend

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import com.example.backend.resource.MyCache
import com.example.backend.resource.Result
import scala.reflect.io.Directory
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import com.example.backend.resource.Implicits._
import com.example.data.MatchDuplicate
import java.io.InputStream
import scala.reflect.io.File
import java.nio.file.Files
import com.example.backend.resource.FileIO
import java.io.IOException
import utils.logging.Logger
import ImportStore.log
import java.nio.file.Path

object ImportStore {

  val log = Logger[ImportStore]
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

  def get( id: String ): Future[Result[BridgeService]]

  def delete( id: String ): Future[Result[BridgeService]]

  /**
   * Create a new store for imports.  This will copy the zip file into the imports area of the persistent store.
   * @param id
   * @param zipfile the filename of the zip file to import.
   * @return a future to the result of the operation.  Returns a BridgeService if successful.
   * Error is returned if id already exists.
   */
  def create( id: String, zipfile: File ): Future[Result[BridgeService]]
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

  private val cache = new MyCache[String,Result[BridgeService]]( cacheInitialCapacity, cacheMaxCapacity, cacheTimeToLive, cacheTimeToIdle )

  lazy val dir = {
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
          dir.dirs.map( dir => dir.name ).toList :::
          dir.files.filter( file => file.extension == "zip" ).map( file => file.name ).toList
      )
    }.logit(s"getAllIds /imports")
  }

  def get( id: String ): Future[Result[BridgeService]] = {
    cache.read(id, {
      val path = dir / id
      if (path.isDirectory) {
        Result( new BridgeServiceFileStore( path.toDirectory, false, true ) )
      } else if (path.extension == "zip") {
        Result( new BridgeServiceZipStore( path.name, path.toFile ) )
      } else {
        Result( StatusCodes.NotFound, "Not found" )
      }
    }).logit(s"get /imports/${id}")
  }

  def delete( id: String ): Future[Result[BridgeService]] = {
    cache.delete(id,
                 None,        // optional function to get old value, None means use cache value
                 { ofrbs =>   // old value => Future[Result[BridgeService]]  return becomes the return of the function
                   ofrbs match {
                     case Some(frbs) => frbs.flatMap { rbs =>
                       rbs match {
                         case Right(bs) =>
                           bs.delete().logit(s"delete BridgeService /imports/${id}").map { s => rbs }
                         case Left(error) =>
                           Result(error).toFuture
                       }
                     }
                     case None =>
                       Result( StatusCodes.NotFound, "Not found" ).toFuture
                   }
                 },
                 { frbs =>   // future calls to get(id) get this   returns Future[Result[BridgeService]]
                   Result( StatusCodes.NotFound, "Not found" ).toFuture
                 }
                ).logit(s"delete /imports/${id}")
  }

  /**
   * Create a new store for imports.  This will copy the zip file into the imports area of the persistent store.
   * @param id
   * @param zipfile the filename of the zip file to import.
   * @return a future to the result of the operation.  Returns a BridgeService if successful.
   * Error is returned if id already exists.
   */
  def create( id: String, zipfile: File ): Future[Result[BridgeService]] = {
    if (!id.endsWith(".zip")) Result( StatusCodes.BadRequest, s"Not a valid Id: ${id}").toFuture
    cache.createOnlyIfNotExist(
                          id,
                          ()=> Future {
                            val importzipfile = dir / id
                            var source: Path = null
                            var target: Path = null
                            try {
                              source = FileIO.getPath(zipfile.toString())
                              target = FileIO.getPath(importzipfile.toString())
                              FileIO.copyFile(source, target)
                              Result( new BridgeServiceZipStore( importzipfile.name, importzipfile.toFile ) )
                            } catch {
                              case x: IOException =>
                                log.warning(s"""Error during importing, copy zipfile ${source} to ${target} failed""", x)
                                Result( StatusCodes.InternalServerError, s"Oops" )
                            }
                          },
                          Result( StatusCodes.BadRequest, s"Id /imports/$id already exists" ).toFuture
                        ).logit(s"create /imports/${id} zipfile ${zipfile}")
  }
}