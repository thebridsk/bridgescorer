package com.github.thebridsk.bridge.server.backend.resource

import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.VersionedInstance
import java.io.InputStream
import scala.io.BufferedSource
import com.github.thebridsk.utilities.logging.Logger

import JavaResourceStore._
import Implicits._
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.Using

object JavaResourceStore {
  val log = Logger[JavaResourceStore[_, _]]()

  def apply[VId <: Comparable[VId], VType <: VersionedInstance[VType, VType, VId]](
      name: String,
      resourcedirectory: String,
      masterfile: String,
      loader: ClassLoader,
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = 10.minutes,
      cacheTimeToIdle: Duration = 9.minutes
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): JavaResourceStore[VId, VType] = {
    new JavaResourceStore(
      name,
      resourcedirectory,
      masterfile,
      loader,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }
}

/**
  * @param resourcedirectory must start and end with a '/'
  * @param masterfile the master file that contains the names of all the resource files.  This file must be in resourcedirectory.
  */
class JavaResourcePersistentSupport[VId <: Comparable[VId], VType <: VersionedInstance[
  VType,
  VType,
  VId
]](
    val resourcedirectory: String,
    val masterfile: String,
    val loader: ClassLoader
)(
    implicit
    support: StoreSupport[VId, VType],
    execute: ExecutionContext
) extends PersistentSupport[VId, VType] {

  log.fine(
    s"JavaResourcePersistentSupport ${resourceURI} resourceDirectory=${resourcedirectory}, masterfile=${masterfile}"
  )

  private var knownIds: Option[Set[VId]] = None

  private def getSource[T](resource: String)( f: BufferedSource => Option[T]): Option[T] = {
    val res = resource.substring(1) // remove leading slash
    log.fine(
      s"JavaResourcePersistentSupport ${resourceURI} looking for ${res} resourceDirectory=${resourcedirectory}, masterfile=${masterfile}"
    )
    val stream: InputStream = loader.getResourceAsStream(res)
    if (stream != null) {
      val bs = scala.io.Source.fromInputStream(stream)
      Using.resource(bs)(f)
    } else {
      log.warning(s"Did not find Java resource '${resource}'")
      None
    }
  }

  private def getFilenameLine(line: String): Option[String] = {
    val v = line.trim()
    if (v.length() == 0) None
    else if (v(0) == '#') None
    else Some(v)
  }

  private def getFromFile(f: String) = {
    log.finest(s"Trying to read from Java resource ${f}")
    try {
      getSource(f) { source =>
        val json = source.mkString
        log.finest("JSON is " + json)
        val (goodInStore, vt) = support.fromJSON(json)
        if (!goodInStore) {
          log.warning(
            "Entry is not good in store, resource=" + f + " json is " + json
          )
        }
        Some(vt)
      }
    } catch {
      case e: Exception =>
        log.severe(s"Error processing resource $f: ${e.toString()}")
        None
    }
  }

  /**
    * Get all the IDs from persistent storage
    */
  def getAllIdsFromPersistent(): Set[VId] = {
    val ret =
      knownIds.getOrElse {
        val resdir =
          if (resourcedirectory.endsWith("/")) resourcedirectory
          else (resourcedirectory + "/")
        val map = getSource(resdir + masterfile) { mfile =>
          Some(
            mfile.getLines().flatMap { line =>
              getFilenameLine(line).flatMap { f =>
                getFromFile(resdir + f).map(vt => vt.id)
              }
            }.toSet
          )
        }
        knownIds = map
        log.fine(s"JavaResourcePersistentSupport ${resourceURI} Ids are $map")
        map.getOrElse(Set())
      }
    log.fine(
      s"JavaResourcePersistentSupport ${resourceURI} resourceDirectory=${resourcedirectory}, masterfile=${masterfile}, Ids=${ret}"
    )
    ret
  }

  private def getFilename(id: VId, ext: String) = {
    s"""${support.resourceName}.${support.idSupport.toString(id)}${ext}"""
  }

//  /**
//   * Read a resource from the persistent store
//   * @param id
//   * @return the result containing the resource or an error
//   */
//  def getFromPersistentOld( id: VId ): Result[VType] =
//    getFilename(id).flatMap { f => getFromFile(f).map( vt=> Result(vt) ) }.getOrElse(
//      Result(StatusCodes.NotFound,RestMessage(s"Did not find resource $resourceURI/$id"))
//    ).logit("JavaResourcePersistentSupport.getFromPersistent")

  def readFilenames(id: VId) = {
    val r = support.getReadExtensions.map { e =>
      s"""${resourcedirectory}${support.resourceName}.${support.idSupport.toString(id)}${e}"""
    }
    log.fine(s"For ${resourceURI}/${support.idSupport.toString(id)}, Java resources are ${r}")
    r
  }

  /**
    * Create an entry in the persistent store.
    * @param v the value to create.  The id field is ignored and will be assigned.
    * @return a future to the stored value, with the correct ID.
    */
  def createInPersistent(
      useId: Option[VId],
      v: VType,
      dontUpdateTimes: Boolean = false
  ): Future[Result[VType]] = {
    storeIsReadOnly.logit("JavaResourcePersistentSupport.createInPersistent")
  }

  /**
    * Read a resource from the persistent store
    * @param id
    * @return the result containing the resource or an error
    */
  def getFromPersistent(
      id: VId
  ): Future[Result[VType]] = {
    Future(read(id))
  }

  /**
    * Read a resource from the persistent store
    * @param id
    * @return the result containing the resource or an error
    */
  def read(id: VId): Result[VType] = synchronized {

    val potential = readFilenames(id)

    @tailrec
    def read(list: List[String]): Result[VType] = {
      if (list.isEmpty) {
        log.warning(
          s"Did not find resource $resourceURI/${support.idSupport.toString(id)}, tried Java resources ${potential}"
        )
        notFound(id)
      } else {
        val f = list.head

        val ovt = try {
          val v = getFromFile(f)
          log.finest(s"For $resourceURI/${support.idSupport.toString(id)}, found ${f}: ${v}")
          v
        } catch {
          case x: Exception =>
            log.severe(s"Unable to read ${f}: ${x}")
            None
        }
        ovt match {
          case Some(vt) => Result(vt)
          case None     => read(list.tail)
        }
      }
    }

    read(potential)
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the resource or an error
    */
  def putToPersistent(
      id: VId,
      v: VType
  ): Future[Result[VType]] = {
    storeIsReadOnly.logit("JavaResourcePersistentSupport.getFromPersistent")
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the old resource or an error
    */
  def deleteFromPersistent(
      id: VId,
      cacheValue: Option[VType]
  ): Future[Result[VType]] =
    storeIsReadOnly.logit("JavaResourcePersistentSupport.getFromPersistent")

}

object JavaResourcePersistentSupport {
  def apply[VId <: Comparable[VId], VType <: VersionedInstance[VType, VType, VId]](
      resourcedirectory: String,
      masterfile: String,
      loader: ClassLoader
  )(
      implicit
      support: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): JavaResourcePersistentSupport[VId, VType] = {
    new JavaResourcePersistentSupport(resourcedirectory, masterfile, loader)
  }
}

/**
  * @param resourcedirectory must end with a '/'
  * @param masterfile the master file that contains the names of all the resource files.  This file must
  * be in resourcedirectory.
  * @param cacheInitialCapacity
  * @param cacheMaxCapacity
  * @param cacheTimeToLive
  * @param cacheTimeToIdle this value must be less than cacheTimeToLive
  */
class JavaResourceStore[VId <: Comparable[VId], VType <: VersionedInstance[VType, VType, VId]](
    name: String,
    val resourcedirectory: String,
    val masterfile: String,
    val loader: ClassLoader,
    cacheInitialCapacity: Int = 5,
    cacheMaxCapacity: Int = 100,
    cacheTimeToLive: Duration = 10.minutes,
    cacheTimeToIdle: Duration = 9.minutes
)(
    implicit
    cachesupport: StoreSupport[VId, VType],
    execute: ExecutionContext
) extends Store[VId, VType](
      name,
      new JavaResourcePersistentSupport[VId, VType](
        resourcedirectory,
        masterfile,
        loader
      ),
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    ) {}
