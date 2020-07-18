package com.github.thebridsk.bridge.datastore

import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import scala.reflect.io.Path
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import java.io.File
import java.io.Reader
import java.io.BufferedReader
import scala.io.Source
import scala.io.BufferedSource
import scala.util.Left
import java.io.InputStream
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Await
import scala.concurrent.duration._
import com.github.thebridsk.bridge.server.backend.resource.Store
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.server.backend.BridgeServiceZipStore

trait Copy

object Copy extends Subcommand("copy") {
  import DataStoreCommands.optionStore

  val log = Logger[Copy]()

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("Copies a datastore, reassigning Ids")

  banner(s"""
Copies a datastore, reassigning Ids

Syntax:
  ${DataStoreCommands.cmdName} copy [options]
Options:""")

  val optionIds = opt[List[String]](
    "ids",
    short = 'i',
    descr = "the ids to map",
    required = false,
    default = None
  )

  val optionSort = opt[String](
    "sort",
    short = 's',
    descr =
      "the sort mechanism to use, valid values are id, created.  default is created",
    required = false,
    validate = (s) => s == "id" || s == "created",
    default = Some("created")
  )

  val paramTarget = trailArg[Path](
    name = "target",
    descr = "directory for new datastore",
    required = true
  )

//  def keyCompare( k1: String, k2: String ) = {
//    Id.idComparer(k1, k2) < 0
//  }

  def sortByMDCreated(t1: MatchDuplicate, t2: MatchDuplicate): Boolean =
    t1.created < t2.created
  def sortByMDRCreated(
      t1: MatchDuplicateResult,
      t2: MatchDuplicateResult
  ): Boolean = t1.played < t2.played
  def sortByMCCreated(t1: MatchChicago, t2: MatchChicago): Boolean =
    t1.created < t2.created
  def sortByMRCreated(t1: MatchRubber, t2: MatchRubber): Boolean =
    t1.created < t2.created

  def sortByMDId(t1: MatchDuplicate, t2: MatchDuplicate): Boolean =
    t1.id < t2.id
  def sortByMDRId(t1: MatchDuplicateResult, t2: MatchDuplicateResult): Boolean =
    t1.id < t2.id
  def sortByMCId(t1: MatchChicago, t2: MatchChicago): Boolean =
    t1.id < t2.id
  def sortByMRId(t1: MatchRubber, t2: MatchRubber): Boolean =
    t1.id < t2.id

  def executeSubcommand(): Int = {

    try {

      val storedir = optionStore()
      val datastore = if (storedir.isDirectory) {
        log.info(s"Using datastore ${storedir}")
        new BridgeServiceFileStore(storedir.toDirectory)
      } else if (storedir.isFile && storedir.extension == "zip") {
        new BridgeServiceZipStore("", storedir.toFile)
      } else {
        log.severe(s"""Unknown store or it doesn't exist: ${storedir}""")
        return 1
      }

      val outdatastore = new BridgeServiceFileStore(paramTarget().toDirectory)

      val ids = optionIds.toOption

      def idFilter(id: Id[_]) = ids.map(l => l.contains(id.id)).getOrElse(true)

      log.info(s"""Starting to copy""")

      copy(
        "Duplicate",
        datastore.duplicates,
        outdatastore.duplicates,
        sortByMDId _,
        sortByMDCreated _,
        idFilter _
      )

      copy(
        "DuplicateResult",
        datastore.duplicateresults,
        outdatastore.duplicateresults,
        sortByMDRId _,
        sortByMDRCreated _,
        idFilter _
      )

      copy(
        "Chicago",
        datastore.chicagos,
        outdatastore.chicagos,
        sortByMCId _,
        sortByMCCreated _,
        idFilter _
      )

      copy(
        "Rubber",
        datastore.rubbers,
        outdatastore.rubbers,
        sortByMRId _,
        sortByMRCreated _,
        idFilter _
      )

      copyIfNotExist(
        "Boardsets",
        datastore.boardSets,
        outdatastore.boardSets,
        idFilter _
      )

      copyIfNotExist(
        "Movements",
        datastore.movements,
        outdatastore.movements,
        idFilter _
      )

      log.info("Done")

      0
    } catch {
      case x: Exception =>
        log.severe("Error changing names", x)
        1
    }

  }

  def await[T](fut: Future[T]) = Await.result(fut, 30.seconds)

  def setValue[K <: Comparable[K], T <: VersionedInstance[T, T, K]](
      name: String,
      out: Store[K, T],
      id: K,
      newmd: T
  ): Option[K] = {
    await(out.importChild(newmd)) match {
      case Left((status, msg)) =>
        log.severe(s"Error creating ${name} match ${id} ${msg.msg}")
        None
      case Right(v) =>
        Some(v.id)
    }
  }

  /**
    * Modify all values from in store and write to out store
    * @param name the name of the store
    * @param in
    * @param out
    * @param converter A function that changes the names in the value.
    *                  If the function returns None, than this value is not modified.  It will be copied.
    */
  def copy[K <: Comparable[K], T <: VersionedInstance[T, T, K]](
      name: String,
      in: Store[K, T],
      out: Store[K, T],
      keyComparer: (T, T) => Boolean,
      dateComparer: (T, T) => Boolean,
      idfilter: K => Boolean
  ) = {
    val comparer = optionSort() match {
      case "id" =>
        log.info("Sorting by ID")
        keyComparer
      case "created" =>
        log.info("Sorting by created")
        dateComparer
      case _ =>
        log.info("Sorting by ID, unknown")
        keyComparer
    }
    await(in.readAll()) match {
      case Right(dups) =>
        val keys = dups.values.toList.sortWith(comparer).map { t =>
          t.id
        }
        log.info(s"${name} found ${keys.mkString(", ")}")
        keys.foreach { id =>
          if (idfilter(id)) {
            val md = dups(id)
            log.fine(s"Working on ${name} ${id}")
            setValue(name, out, id, md) match {
              case Some(newid) =>
                log.info(s"Copied ${name} ${id}  --> ${newid}")
              case None =>
            }
          }
        }
      case Left((code, msg)) =>
        log.severe(s"Error getting ${name} matches ${msg.msg}")
    }
  }

  /**
    * Modify all values from in store and write to out store
    * @param name the name of the store
    * @param in
    * @param out
    * @param converter A function that changes the names in the value.
    *                  If the function returns None, than this value is not modified.  It will be copied.
    */
  def copyIfNotExist[K <: Comparable[K], T <: VersionedInstance[T, T, K]](
      name: String,
      in: Store[K, T],
      out: Store[K, T],
      idfilter: K => Boolean
  ) = {
    await(in.readAll()) match {
      case Right(dups) =>
        val keys = dups.keys.toList
        log.info(s"${name} found ${keys.mkString(", ")}")
        keys.filter(id => idfilter(id)).foreach { id =>
          val md = dups(id)
          if (await(out.read(id)) match {
                case Left(err) =>
                  true
                case Right(v) =>
                  if (md == v) {
                    log.info(
                      s"Not copying ${name} ${id}, already in the target store"
                    )
                    false
                  } else {
                    true
                  }
              }) {
            log.fine(s"Working on ${name} ${id}")
            setValue(name, out, id, md) match {
              case Some(newid) =>
                log.info(s"Copied ${name} ${id}  --> ${newid}")
              case None =>
            }
          }
        }
      case Left((code, msg)) =>
        log.severe(s"Error getting ${name} matches ${msg.msg}")
    }
  }
}
