package com.github.thebridsk.bridge.datastore

import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import scala.reflect.io.Path
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import java.io.File
import scala.io.Source
import scala.io.BufferedSource
import scala.util.Left
import java.io.InputStream
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
import scala.util.Using

trait SetNamesCommand

object SetNamesCommand extends Subcommand("setnames") {
  import DataStoreCommands.optionStore

  val log = Logger[SetNamesCommand]()

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("set names in a datastore")

  banner(s"""
Set names in a datastore

Syntax:
  ${DataStoreCommands.cmdName} setnames [options]
Options:""")

  val optionMapFileName = opt[String](
    "mapfile",
    short = 'm',
    descr = "the name mapping file",
    required = false,
    default = None
  )

  val optionIds = opt[List[String]](
    "ids",
    short = 'i',
    descr = "the ids to map",
    required = false,
    default = None
  )

  val optionSort = opt[String](
    "sort",
    noshort = true,
    descr =
      "the sort mechanism to use, valid values are id, created.  default is id",
    required = false,
    validate = (s) => s == "id" || s == "created",
    default = Some("id")
  )

  val optionTarget = opt[Path](
    "target",
    short = 't',
    descr = "directory for new datastore, default is to overwrite existing",
    required = false,
    default = None
  )

  val optionAdd = toggle(
    "add",
    default = Some(false),
    noshort = true,
    descrYes = "add modified items to datastore, generates new Ids",
    descrNo =
      "replace modified items to new datastore, uses same Id as original"
  )

  footer(s"""
This can be used to fix names in the datastore, or replace the names.

If newdatastore is not specified, then the source datastore is overwritten.
If a newdatastore is specified, then the IDs are rewritten according to the new datastore.

Care must be used when creating the mapping file.  A new name must NOT be the same as any old name, this is checked.
Any name in the datastore not specified is not changed.  All names are case sensitive.
The file must use the UTF-8 character encoding.
Mapping file syntax, any line that starts with a '#' character is a comment.  Any blank is ignored.
All remaining lines have the following syntax:

  sp* oldname sp* "->" sp* newname sp*

All spaces within oldname or newname are preserved.

example:
  Nancy ->   Sally
Sam->Norman

""")

//  def keyCompare( k1: String, k2: String ) = {
//    Id.idComparer(k1, k2) < 0
//  }

  def getidd(t: MatchDuplicate) = t.id
  def getiddr(t: MatchDuplicateResult) = t.id
  def getidc(t: MatchChicago) = t.id
  def getidr(t: MatchRubber) = t.id

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

      val storedir = optionStore().toDirectory
      log.info(s"Using datastore ${storedir}")
      val datastore = new BridgeServiceFileStore(storedir)

      val (outdatastore, istemp, add) = if (optionTarget.isDefined) {
        val newdir = optionTarget().toDirectory
        log.info(s"Copying to datastore ${newdir}")
        val a = optionAdd.getOrElse(false)
        (new BridgeServiceFileStore(newdir, !a, true), false, a)
      } else {
        log.info(s"Modifying datastore ${storedir}")
        (datastore, false, false)
//        ( new BridgeServiceInMemory(true), true )
      }

      val ids = optionIds.toOption

      def idFilter(id: Id[_]) = ids.map(l => l.contains(id.id)).getOrElse(true)

      val allnames = datastore.getAllNames()

      val nameMap = optionMapFileName.toOption
        .map { m =>
          getNamesMap(new File(m))
        }
        .getOrElse(Map())

      val notmodified = (Await.result(allnames, 30.seconds) match {
        case Right(list) => list
        case Left(error) => List()
      }).filter(n => !nameMap.contains(n)).mkString("\n  ", "\n  ", "")

      // Must update names without setting timestamps
      val names =
        nameMap.map((e) => e._1 + " -> " + e._2).mkString("\n  ", "\n  ", "")
      log.info(
        s"""Starting to convert, changing: ${names}\nNot modifying:${notmodified}"""
      )

      change(
        "Duplicate",
        datastore.duplicates,
        outdatastore.duplicates,
        istemp,
        sortByMDId _,
        sortByMDCreated _,
        getidd _,
        add,
        idFilter _
      ) { (id, md) =>
        md.modifyPlayers(nameMap)
      }

      change(
        "DuplicateResult",
        datastore.duplicateresults,
        outdatastore.duplicateresults,
        istemp,
        sortByMDRId _,
        sortByMDRCreated _,
        getiddr _,
        add,
        idFilter _
      ) { (id, mdr) =>
        mdr.modifyPlayers(nameMap)
      }

      change(
        "Chicago",
        datastore.chicagos,
        outdatastore.chicagos,
        istemp,
        sortByMCId _,
        sortByMCCreated _,
        getidc _,
        add,
        idFilter _
      ) { (id, md) =>
        md.modifyPlayers(nameMap)
      }

      change(
        "Rubber",
        datastore.rubbers,
        outdatastore.rubbers,
        istemp,
        sortByMRId _,
        sortByMRCreated _,
        getidr _,
        add,
        idFilter _
      ) { (id, md) =>
        md.modifyPlayers(nameMap)
      }

      log.info("Done")

      0
    } catch {
      case x: Exception =>
        log.severe("Error changing names", x)
        1
    }

  }

  def await[T](fut: Future[T]) = Await.result(fut, 30.seconds)

  /**
    * Copy all values from src store to dest store
    * @param name the name of the store
    * @param src
    * @param dest
    */
  def copyStore[K <: Comparable[K], T <: VersionedInstance[T, T, K]](
      name: String,
      src: Store[K, T],
      dest: Store[K, T]
  ) = {
    await(src.readAll()) match {
      case Right(dups) =>
        dups.foreach { e =>
          val (id, md) = e
          log.fine(s"Working on ${name} ${id}")
          await(dest.select(id) update (md)) match {
            case Right(dups) =>
              log.info(s"Updated ${name} ${id}")
            case Left((code, msg)) =>
              log.severe(s"Error setting ${name} match ${id}: ${msg.msg}")
          }
        }
      case Left((code, msg)) =>
        log.severe(s"Error getting ${name} matches ${msg.msg}")
    }
  }

  def setValue[K <: Comparable[K], T <: VersionedInstance[T, T, K]](
      name: String,
      out: Store[K, T],
      id: K,
      newmd: T,
      getid: T => K,
      add: Boolean
  ): Option[K] = {
    if (add) {
      await(out.createChild(newmd)) match {
        case Left((status, msg)) =>
          log.severe(s"Error creating ${name} match ${id} ${msg.msg}")
          None
        case Right(v) =>
          Some(getid(v))
      }
    } else {
      await(out.select(id).update(newmd)) match {
        case Left((status, msg)) =>
          if (status == StatusCodes.NotFound) {
            await(out.createChild(newmd)) match {
              case Left((status, msg)) =>
                log.severe(s"Error creating ${name} match ${id} ${msg.msg}")
                None
              case Right(v) =>
                Some(getid(v))
            }
          } else {
            log.severe(s"Error updating ${name} match ${id} ${msg.msg}")
            None
          }
        case Right(v) =>
          Some(getid(v))
      }
    }
  }

  /**
    * Modify all values from in store and write to out store
    * @param name the name of the store
    * @param in
    * @param out
    * @param istemp true indicates out is a temporary store, and the modified values should be written to in store.
    * @param converter A function that changes the names in the value.
    *                  If the function returns None, than this value is not modified.  It will be copied.
    */
  def change[K <: Comparable[K], T <: VersionedInstance[T, T, K]](
      name: String,
      in: Store[K, T],
      out: Store[K, T],
      istemp: Boolean,
      keyComparer: (T, T) => Boolean,
      dateComparer: (T, T) => Boolean,
      getid: T => K,
      add: Boolean,
      idfilter: K => Boolean
  )(converter: (K, T) => Option[T]) = {
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

            converter(id, md) match {
              case Some(newmd) =>
                setValue(name, out, id, newmd, getid, add) match {
                  case Some(newid) =>
                    log.info(s"Modified ${name} ${id}  --> ${newid}")
                  case None =>
                }
              case None =>
                if (istemp) {
                  log.info(s"Ignoring ${name} ${id}")
                } else {
                  setValue(name, out, id, md, getid, add) match {
                    case Some(newid) =>
                      log.info(s"Copying ${name} ${id}  --> ${newid}")
                    case None =>
                  }
                }
            }
          }
        }
        if (istemp) copyStore(name, out, in)
      case Left((code, msg)) =>
        log.severe(s"Error getting ${name} matches ${msg.msg}")
    }
  }

  val patternName = """\s*(.*?)\s*\-\>\s*(.*?)\s*""".r
  val patternComment = """\s*\#.*""".r
  val patternBlankLine = """\s*""".r

  /**
    * Parse the names in the specified reader (a mapfile)
    * @param mapfile
    * @return None if there was an error, Some(map), where map is a String->String map
    */
  def getNamesMap(r: BufferedSource, name: String): Map[String, String] = {
    r.getLines()
      .zipWithIndex
      .flatMap {
        case (line, index) =>
          line match {
            case patternName(oldname, newname) =>
              List(oldname -> newname)
            case patternComment() =>
              List()
            case patternBlankLine() =>
              List()
            case _ =>
              val linenumber = index + 1 // index is 0 based
              throw new Exception(
                s"Line not valid in ${name}:${linenumber}: ${line}"
              )
          }
      }
      .toMap
  }

  /**
    * Parse the names in the specified mapfile
    * @param mapfile the stream.  The stream will NOT be closed.
    * @param name
    * @return None if there was an error, Some(map), where map is a String->String map
    */
  def getNamesMap(mapfile: InputStream, name: String): Map[String, String] = {
    getNamesMap(Source.fromInputStream(mapfile, "UTF8"), name)
  }

  /**
    * Parse the names in the specified mapfile
    * @param mapfile
    * @param name
    * @return None if there was an error, Some(map), where map is a String->String map
    */
  def getNamesMap(mapfile: File): Map[String, String] = {
    val name = mapfile.toString()
    val m = Using.resource(Source.fromFile(mapfile, "UTF8")) { s =>
      getNamesMap(s, name)
    }
    m
    // m.either.either match {
    //   case Left(err) =>
    //     val e = new Exception(s"Error reading ${name}")
    //     err.foreach(x => e.addSuppressed(x))
    //     throw e
    //   case Right(om) => om
    // }
  }
}
