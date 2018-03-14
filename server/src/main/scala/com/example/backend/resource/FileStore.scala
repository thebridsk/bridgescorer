package com.example.backend.resource

import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes
import com.example.data.RestMessage
import com.example.data.VersionedInstance
import scala.reflect.io.Directory
import utils.logging.Logger
import scala.annotation.tailrec
import java.io.IOException

import FileStore.log
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.example.data.Id

object FileStore {
  val log = Logger[FileStore[_,_]]

  def apply[VId,VType <: VersionedInstance[VType,VType,VId]](
                            directory: Directory,
                            cacheInitialCapacity: Int = 5,
                            cacheMaxCapacity: Int = 100,
                            cacheTimeToLive: Duration = 10.minutes,
                            cacheTimeToIdle: Duration = 9.minutes,
                          )(
                            implicit
                              cachesupport: StoreSupport[VId,VType],
                              execute: ExecutionContext
                          ): FileStore[VId,VType] = {
    new FileStore(directory,cacheInitialCapacity, cacheMaxCapacity, cacheTimeToLive, cacheTimeToIdle )
  }
}

class FilePersistentSupport[VId,VType <: VersionedInstance[VType,VType,VId]](
          val directory: Directory
        )(
          implicit
            support: StoreSupport[VId, VType],
            execute: ExecutionContext
        ) extends PersistentSupport[VId,VType] {

  self =>

  val resourceName = support.resourceName

  /**
   * Get all the IDs from persistent storage
   */
  def getAllIdsFromPersistent(cacheKeys: ()=>Set[VId]): Set[VId] = {
    val pattern = (resourceName+"\\.([^.]+)\\..*").r

    val keys = directory.files.map { path => {
      path.name
    }}.flatMap {
      case pattern(sid) =>
        support.stringToId(sid)
      case _ =>
        None
    }.toSet

    log.finer(s"getAllIdsFromPersistent for ${resourceURI} returning ${keys}")

    keys
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
    Future {
      synchronized {
        (useId match {
          case Some(i) => Result(i)
          case None =>
            generateNextId(v)
        }) match {
          case Right(id) =>
            val nv = v.setId(id, true, dontUpdateTimes)
            write(id, nv)
          case Left(error) =>
            Result(error)
        }
      }
    }
  }

  /**
   * Read a resource from the persistent store
   * @param id
   * @return the result containing the resource or an error
   */
  def getFromPersistent(
                         id: VId
                       ): Future[Result[VType]] = {
    Future( read(id) )
  }

  /**
   * Read a resource from the persistent store
   * @param id
   * @return the result containing the resource or an error
   */
  def read( id: VId ): Result[VType] = self.synchronized {

    @tailrec
    def read( list: List[String] ): Result[VType] = {
      if (list.isEmpty) notFound(id)
      else {
        val f = list.head

        val ovt = try {
                    val v = FileIO.readFileSafe(f)
                    val (goodOnDisk,vt) = support.fromJSON(v)
                    if (!goodOnDisk) {
                      val nf = writeFilename(id)
                      log.warning(s"Writing current version to disk for file=${f} -> ${nf}")
                      write(id,vt)
                      if ( nf != f) {  // did the extension change?
                        FileIO.deleteFileSafe(f)       // delete old file if extension changed
                      }
                    }
                    Option(vt)
                  } catch {
                    case x: Exception =>
                      log.info(s"Unable to read ${f}: ${x}")
                      None
                  }
        ovt match {
          case Some(vt) => Result(vt)
          case None => read( list.tail )
        }
      }
    }

    read( readFilenames(id) )
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
                     ): Future[Result[VType]] = Future( self.synchronized {
       write(id,v)
    })

  private def write( id: VId, v: VType ) = {
    try {
      FileIO.writeFileSafe(writeFilename(id), support.toJSON(v))
      Result(v)
    } catch {
      case e: IOException =>
        log.severe(s"Error writing ${id} to disk",e)
        internalError
    }
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
                     ): Future[Result[VType]] =  {
    Future {
      val r = cacheValue.map( v => Result(v)).getOrElse( read(id) )
      r.foreach { v =>  // we have an existing value
        FileIO.deleteFileSafe(writeFilename(id))
      }
      r
    }
  }

  def readFilenames( id: VId ) = {
    support.getReadExtensions().map{ e =>
      (directory / (resourceName+"."+id+e)).toString()
    }
  }

  def writeFilename( id: VId ) = {
    (directory / (resourceName+"."+id+support.getWriteExtension())).toString()
  }

  private def newfilename( filename: String ) = FileIO.newfilename(filename)

}

object FilePersistentSupport {
  def apply[VId,VType <: VersionedInstance[VType,VType,VId]](
          directory: Directory
        )(
          implicit
            support: StoreSupport[VId, VType],
            execute: ExecutionContext
        ): FilePersistentSupport[VId, VType] = {
    new FilePersistentSupport(directory)
  }
}

class FileStore[VId,VType <: VersionedInstance[VType,VType,VId]](
                            val directory: Directory,
                            cacheInitialCapacity: Int = 5,
                            cacheMaxCapacity: Int = 100,
                            cacheTimeToLive: Duration = 10.minutes,
                            cacheTimeToIdle: Duration = 9.minutes,
                          )(
                            implicit
                              cachesupport: StoreSupport[VId,VType],
                              execute: ExecutionContext
                          ) extends Store[VId,VType]( new FilePersistentSupport[VId,VType](directory),
                                                           cacheInitialCapacity,
                                                           cacheMaxCapacity,
                                                           cacheTimeToLive,
                                                           cacheTimeToIdle
                                                         ) {

}
