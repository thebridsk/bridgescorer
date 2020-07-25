package com.github.thebridsk.bridge.server.backend.resource

import com.github.thebridsk.bridge.data.VersionedInstance
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.duration._

import InMemoryStore.log
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import Implicits._

import MetaData.MetaDataFile
import scala.reflect.io.File
import java.io.InputStream
import java.io.ByteArrayInputStream
import scala.util.Using
import java.io.FileInputStream
import org.scalactic.source.Position

class InMemoryPersistent[VId <: Comparable[VId], VType <: VersionedInstance[VType, VType, VId]](
    implicit
    support: StoreSupport[VId, VType],
    execute: ExecutionContext,
    position: Position
) extends PersistentSupport[VId, VType] {

  self =>

  val valuesInPersistent = {
    import scala.jdk.CollectionConverters._
    new java.util.concurrent.ConcurrentHashMap[VId, VType]().asScala
  }

  val deletedKeys = collection.mutable.Set[VId]()

  def clearPersistent = valuesInPersistent.clear()
  def add(v: VType) = {
    if (!support.isIdFromValue) addId(v.id)
    valuesInPersistent += v.id -> v
  }
  def internalAdd(v: VType) = {
    valuesInPersistent += v.id -> v
  }
  def get(id: VId) = valuesInPersistent.get(id)
  def remove(id: VId) = {
    deletedKeys += id
    valuesInPersistent.remove(id)
  }

  /**
    * Get all the IDs from persistent storage
    */
  def getAllIdsFromPersistent(): Set[VId] = {
    self.synchronized {
      log.fine(s"""getAllIdsFromPersistent""")
      valuesInPersistent.keySet.toSet ++ deletedKeys
    }
  }

  /**
    * Create an entry in the persistent store.
    * @param useId use this Id if specified.
    * @param v the value to create.  The id field is ignored and will be assigned.
    * @return a future to the stored value, with the correct ID.
    */
  override def createInPersistent(
      useId: Option[VId],
      v: VType,
      dontUpdateTimes: Boolean = false
  ): Future[Result[VType]] = {
    log.fine(s"""createInPersistent useId=${useId}, v=${v}""")
    Future {
      self.synchronized {
        useId match {
          case Some(id) =>
            val nv = v.setId(id, true, dontUpdateTimes).readyForWrite
            add(nv)
            Result(nv)
          case None =>
            generateNextId(v) match {
              case Right(id) =>
                try {
                  val nv = v.setId(id, true, false)
                  add(nv)
                  Result(nv)
                } catch {
                  case x: Exception =>
                    // println(s"Oops store created ${position} ${support.idSupport.toString(id)} ${useId}, ${v}")
                    // x.printStackTrace(System.out)
                    throw x
                }
              case Left(error) =>
                Result(error)
            }
        }
      }
    }.logit("createInPersistent")
  }

  /**
    * Read a resource from the persistent store
    * @param id
    * @return the result containing the resource or an error
    */
  override def getFromPersistent(
      id: VId
  ): Future[Result[VType]] = {
    log.fine(s"""getFromPersistent id=${support.idSupport.toString(id)}""")
    Future {
      get(id) match {
        case Some(v) => Result(v)
        case None    => notFound(id)
      }
    }
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the resource or an error
    */
  override def putToPersistent(
      id: VId,
      v: VType
  ): Future[Result[VType]] = {
    log.fine(s"""putToPersistent id=${support.idSupport.toString(id)}, v=${v}""")
    Future {
      val nv = v.setId(id, false, false).readyForWrite
      internalAdd(nv)
      Result(nv)
    }
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the old resource or an error
    */
  override def deleteFromPersistent(
      id: VId,
      cacheValue: Option[VType]
  ): Future[Result[VType]] = {
    log.fine(s"""deleteFromPersistent id=${support.idSupport.toString(id)}, cacheValue=${cacheValue}""")
    Future {
      self.synchronized {
        cacheValue match {
          case Some(ov) =>
            remove(id)
            Result(ov)
          case None =>
            remove(id) match {
              case Some(ov) =>
                Result(ov)
              case None =>
                notFound(id)
            }
        }
      }
    }
  }

  // Metadata support

  case class MDData( name: MetaDataFile, data: Array[Byte] )

  var metadataStore = Map[VId, List[MDData]]()

  /**
   * List all the files for the specified match, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  override
  def listFiles( id: VId ): Result[Iterator[MetaDataFile]] = {
    val it = metadataStore.get(id).map { list =>
      list.map( s => s.name ).iterator
    }.getOrElse( List().iterator )

    Result(
      it
    )
  }

  /**
   * List all the files for the specified match that match the filter, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  override
  def listFilesFilter( id: VId )( filter: MetaDataFile=>Boolean ): Result[Iterator[MetaDataFile]] = {
    listFiles(id).map( _.filter(filter) )
  }

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  override
  def write( id: VId, sourceFile: File, targetFile: MetaDataFile ): Result[Unit] = {
    val len = sourceFile.length
    val data = new Array[Byte](len.toInt)
    Using.resource( new FileInputStream(sourceFile.jfile)) { is =>
      is.read(data)
    }
    write(id,data,targetFile)
  }

  val maxBuff = 100000
  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  override
  def write( id: VId, source: InputStream, targetFile: MetaDataFile ): Result[Unit] = {
    val datalist =
    Iterator.continually {
      val data = new Array[Byte](maxBuff)
      val l = source.read(data)
      (data,l)
    }.takeWhile( e => e._2>0 ).toList
    val len = datalist.foldLeft(0) { (ac,v) => ac+v._2}
    val data = new Array[Byte](len)
    datalist.foldLeft(0) { (ac,v) =>
      Array.copy(v._1,0,data,ac,v._2)
      ac + v._2
    }
    write(id,data,targetFile)
  }

  def write( id: VId, data: Array[Byte], targetFile: MetaDataFile ): Result[Unit] = synchronized {
    metadataStore.get(id) match {
      case Some(old) =>
        val n = MDData(targetFile,data) :: old.filter(d => d.name != targetFile)
        metadataStore = metadataStore + (id->n)
      case None =>
        metadataStore = metadataStore + ( id -> List(MDData(targetFile,data)))
    }
    Result.unit
  }

  /**
   * read the specified file, the file is relative to the store directory for specified match.
   */
  override
  def read( id: VId, file: MetaDataFile ): Result[InputStream] = {
    metadataStore.get(id).map { list =>
      list.find { e =>
        e.name == file
      }.map { e =>
        Result( new ByteArrayInputStream(e.data))
      }.getOrElse(Result((StatusCodes.NotFound,RestMessage("Not found"))))
    }.getOrElse( Result((StatusCodes.NotFound,RestMessage("Not found"))))
  }

  /**
   * delete the specified file, the file is relative to the store directory for specified match.
   */
  override
  def delete( id: VId, file: MetaDataFile ): Result[Unit] = synchronized {
    metadataStore = metadataStore.map { e =>
      val (eid,elist) = e
      if (eid == id) {
        (
          eid,
          elist.filter( d => d.name != file )
        )
      } else {
        e
      }
    }
    Result.unit
  }

  /**
   * delete all the metadata files for the match
   */
  override
  def deleteAll( id: VId ): Result[Unit] = synchronized {
    metadataStore = metadataStore - id
    Result.unit
  }

}

object InMemoryPersistent {
  def apply[VId <: Comparable[VId], VType <: VersionedInstance[VType, VType, VId]](
      implicit
      support: StoreSupport[VId, VType],
      execute: ExecutionContext,
      position: Position
  ): InMemoryPersistent[VId, VType] = {
    new InMemoryPersistent
  }
}

class InMemoryStore[VId <: Comparable[VId], VType <: VersionedInstance[VType, VType, VId]](
    name: String,
    cacheInitialCapacity: Int = 5,
    cacheMaxCapacity: Int = 100,
    cacheTimeToLive: Duration = 10.minutes,
    cacheTimeToIdle: Duration = 9.minutes
)(
    implicit
    cachesupport: StoreSupport[VId, VType],
    execute: ExecutionContext,
    position: Position
) extends Store[VId, VType](
      name,
      new InMemoryPersistent[VId, VType],
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    ) {}

object InMemoryStore {

  val log = Logger[InMemoryStore[_, _]]()

  def apply[VId <: Comparable[VId], VType <: VersionedInstance[VType, VType, VId]](
      name: String,
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = 10.minutes,
      cacheTimeToIdle: Duration = 9.minutes
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext,
      position: Position
  ): InMemoryStore[VId, VType] = {
    new InMemoryStore(
      name,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }
}
