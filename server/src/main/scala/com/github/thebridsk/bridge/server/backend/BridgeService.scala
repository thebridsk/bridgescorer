package com.github.thebridsk.bridge.server.backend

import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.LoggerConfig
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.sample.TestMatchDuplicate
import com.github.thebridsk.bridge.server.backend.resource.MultiStore
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.server.logging.RemoteLoggingConfig
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.server.backend.resource.Store
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.InMemoryStore
import com.github.thebridsk.bridge.server.backend.resource.Implicits
import com.github.thebridsk.bridge.data.RestMessage
import java.io.OutputStream
import java.util.zip.ZipOutputStream
import java.nio.charset.StandardCharsets
import com.github.thebridsk.bridge.data.VersionedInstance
import java.util.zip.ZipEntry
import java.io.BufferedOutputStream
import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import scala.reflect.io.Directory
import scala.reflect.io.Path
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces
import com.github.thebridsk.bridge.data.duplicate.stats.CalculatePlayerPlaces
import com.github.thebridsk.bridge.data.ImportStoreData
import com.github.thebridsk.bridge.server.backend.resource.ZipStoreInternal
import com.github.thebridsk.bridge.server.version.VersionServer
import com.github.thebridsk.bridge.data.version.VersionShared
import com.github.thebridsk.utilities.version.VersionUtilities
import scala.util.Using
import java.io.FileInputStream
import java.io.InputStream
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.bridge.individual.{ DuplicateSummary => IDuplicateSummary }

/**
  * The backend trait for our service.
  * Implementation of this trait should persistently store all
  * the objects that make up the API.
  */
abstract class BridgeService(val id: String) {

  def getDate: Timestamp = SystemTime.currentTimeMillis()

  def loggerConfig(ip: String, iPad: Boolean): LoggerConfig

  def setDefaultLoggerConfig(default: LoggerConfig, iPad: Boolean): Unit

  val chicagos: Store[MatchChicago.Id, MatchChicago]
  val duplicates: Store[MatchDuplicate.Id, MatchDuplicate]
  val individualduplicates: Store[IndividualDuplicate.Id, IndividualDuplicate]
  val duplicateresults: Store[MatchDuplicateResult.Id, MatchDuplicateResult]
  val rubbers: Store[MatchRubber.Id, MatchRubber]

  val boardSets: Store[BoardSet.Id, BoardSet]
  val movements: Store[Movement.Id, Movement]
  val individualMovements: Store[IndividualMovement.Id, IndividualMovement]

  /**
    * The import store.  Some some of the implementations support this, and will override this value.
    */
  val importStore: Option[ImportStore] = None

  val diagnosticDir: Option[Directory] = None

  def importStoreData: Future[ImportStoreData] = {

    def count[T](frs: Future[Result[Set[T]]]) =
      frs.map { rs =>
        rs match {
          case Right(s)    => s.size
          case Left(value) => 0
        }
      }

    val r = for {
      nd <- count(duplicates.getAllIds())
      ndr <- count(duplicateresults.getAllIds())
      nc <- count(chicagos.getAllIds())
      nr <- count(rubbers.getAllIds())
    } yield {
      ImportStoreData(id, getDate, nd, ndr, nc, nr)
    }
    r
  }

  /**
    * Writes the store contents in export format to the output stream.
    * The output stream is NOT closed.
    *
    * @param out the output stream
    * @param filter the filter, None means everything
    * @return a future to a result that has a list of all the Ids of entities that are exported.
    */
  def doExport(
      out: OutputStream,
      filter: Option[List[String]] = None,
      diagnostics: Boolean = false
  ): Future[Result[List[String]]] = {
    BridgeServiceWithLogging.log.fine(
      s"exporting: diagnostics=$diagnostics, filter=$filter"
    )

    val converters = new BridgeServiceFileStoreConverters(true)

    val buf = new BufferedOutputStream(out)
    val zip = new ZipOutputStream(buf, StandardCharsets.UTF_8)

    Future {
      {
        val nameInZip = "version.txt"
        val ze = new ZipEntry(nameInZip)
        BridgeServiceWithLogging.log.fine(
          s"Adding version info => ${ze.getName}"
        )
        zip.putNextEntry(ze)
        val v =
          s"""${VersionServer.toString}\n${VersionShared.toString}\n${VersionUtilities.toString}"""
        zip.write(v.getBytes("UTF8"))
        zip.closeEntry()
      }

      // BridgeService.copyResourceToZip(
      //   "com/github/thebridsk/bridge/bridgescorer/version/VersionBridgeScorer.properties",
      //   "VersionBridgeScorer.properties",
      //   zip
      // )

      // BridgeService.copyResourceToZip(
      //   "com/github/thebridsk/utilities/version/VersionUtilities.properties",
      //   "VersionUtilities.properties",
      //   zip
      // )

      if (diagnostics) {
        diagnosticDir.foreach { dir =>
          BridgeServiceWithLogging.log.fine(
            s"Looking for logs in directory ${dir}"
          )
          dir.files
            .filter(f => f.extension == "log" || f.extension == "csv")
            .foreach { f =>
              zip.putNextEntry(new ZipEntry("logs/" + f.name))
              Using.resource(new FileInputStream(f.jfile)) { in =>
                BridgeService.copy(in, zip)
              }
            }
        }
      }
    }.flatMap { u =>
      exportToZip(zip, filter).map { r =>
        zip.finish(); buf.flush(); r
      }
    }

  }

  /**
    * Writes the store contents in export format to the output stream.
    * The zip output stream is NOT finished.
    *
    * @param zip the zip output stream
    * @param filter the filter, None means everything
    * @return a future to a result that has a list of all the Ids of entities that are exported.
    */
  def exportToZip(
      zip: ZipOutputStream,
      filter: Option[List[String]] = None
  ): Future[Result[List[String]]] = {
    val converters = new BridgeServiceFileStoreConverters(true)

    exportStore(zip, duplicates, filter).flatMap { rd =>
      exportStore(zip, duplicateresults, filter).flatMap { re =>
        exportStore(zip, chicagos, filter).flatMap { rc =>
          exportStore(zip, rubbers, filter).map { rr =>
            val ld = rd.toOption.getOrElse(List())
            val le = re.toOption.getOrElse(List())
            val lc = rc.toOption.getOrElse(List())
            val lr = rr.toOption.getOrElse(List())
            val l = ld ::: le ::: lc ::: lr
            if (l.isEmpty) {
              if (rd.isLeft) rd
              else if (re.isLeft) re
              else if (rc.isLeft) rc
              else if (rr.isLeft) rr
              else Result(StatusCodes.NotFound, "Nothing to export")
            } else {
              Result(ld ::: le ::: lc ::: lr)
            }
          }
        }
      }
    }
  }

  def exportStore[TId <: Comparable[TId], T <: VersionedInstance[T, T, TId]](
      zip: ZipOutputStream,
      store: Store[TId, T],
      filter: Option[List[String]]
  ): Future[Result[List[String]]] = {
    ZipStoreInternal.exportStore(zip, store, filter)
  }

  val defaultBoards = BoardSet.default
  val defaultMovement = Movement.default
  val defaultIndividualBoards = BoardSet.standard
  val defaultIndividualMovement = IndividualMovement.default

  def fillBoards(dup: MatchDuplicate): Future[Result[MatchDuplicate]] = {
    fillBoards(dup, defaultBoards, defaultMovement)
  }

  def fillBoards(dup: IndividualDuplicate): Future[Result[IndividualDuplicate]] = {
    fillBoards(dup, defaultIndividualBoards, defaultIndividualMovement)
  }

  def fillBoards(
      dup: MatchDuplicate,
      boardset: BoardSet.Id,
      movement: Movement.Id
  ): Future[Result[MatchDuplicate]] = {

    val fmv = movements.read(movement)
    val fbb = boardSets.read(boardset)

    fbb.flatMap { rbb =>
      rbb match {
        case Right(bb) =>
          fmv.map { rmv =>
            rmv match {
              case Right(mv) =>
                Result(dup.fillBoards(bb, mv))
              case Left(error) =>
                Result(
                  StatusCodes.BadRequest,
                  s"Movement $movement was not found"
                )
            }
          }
        case Left(error) =>
          Result.future(
            StatusCodes.BadRequest,
            s"Boardset $boardset was not found"
          )
      }
    }

  }

  def fillBoards(
      dup: IndividualDuplicate,
      boardset: BoardSet.Id,
      movement: IndividualMovement.Id
  ): Future[Result[IndividualDuplicate]] = {

    val fmv = individualMovements.read(movement)
    val fbb = boardSets.read(boardset)

    fbb.flatMap { rbb =>
      rbb match {
        case Right(bb) =>
          fmv.map { rmv =>
            rmv match {
              case Right(mv) =>
                Result(dup.fillBoards(bb, mv))
              case Left(error) =>
                Result(
                  StatusCodes.BadRequest,
                  s"Movement $movement was not found"
                )
            }
          }
        case Left(error) =>
          Result.future(
            StatusCodes.BadRequest,
            s"Boardset $boardset was not found"
          )
      }
    }

  }

  def createTestDuplicate(
      dup: MatchDuplicate
  ): Future[Result[MatchDuplicate]] = {
    fillBoards(dup).map { rd =>
      rd match {
        case Right(dd) =>
          var d = dd
          val hands = TestMatchDuplicate.getHand(
            d,
            Board.id(3),
            Team.id(1),
            3,
            "N",
            "N",
            "N",
            true,
            5
          ) ::
            TestMatchDuplicate.getHand(
              d,
              Board.id(3),
              Team.id(3),
              3,
              "N",
              "N",
              "N",
              true,
              5
            ) ::
            TestMatchDuplicate.getHands(d)
          for (h <- hands) {
            d = d.updateHand(h)
          }
          for ((id, t) <- TestMatchDuplicate.teams()) {
            d = d.updateTeam(t)
          }
          Result(d)
        case Left(error) =>
          Result(error)
      }
    }
  }

  def getAllNames(): Future[Result[List[String]]] = {
    import Implicits._
    var list = Set[String]()
    def isNotEmpty(s: String) = s != null && s.length() > 0
    def contains(s: String) =
      list.find { ss =>
        s.equalsIgnoreCase(ss)
      }.isDefined
    def add(s: String) = {
      if (isNotEmpty(s) && !contains(s)) list = list + s
    }
    val n1 = duplicates.readAll().map { rd =>
      rd match {
        case Right(dups) =>
          Result(
            dups.values
              .flatMap { dup =>
                dup.teams.flatMap { t =>
                  t.player1 :: t.player2 :: Nil
                }
              }
              .filter(p => p.length() > 0)
              .toList
              .distinct
          ).logit("getAllNames() on duplicates")
        case Left(error) =>
          BridgeServiceWithLogging.log
            .fine(s"getAllNames() got error on duplicates.readAll $error")
          Result(error)
      }
    }
    val n2 = duplicateresults.readAll().map { rd =>
      rd match {
        case Right(dups) =>
          Result(
            dups.values
              .flatMap { dup =>
                dup.results.flatten.flatMap { t =>
                  t.team.player1 :: t.team.player2 :: Nil
                }
              }
              .filter(p => p.length() > 0)
              .toList
              .distinct
          ).logit("getAllNames() on duplicateresults")
        case Left(error) =>
          BridgeServiceWithLogging.log
            .fine(s"getAllNames() got error on duplicateresults.readAll $error")
          Result(error)
      }
    }
    val n3 = chicagos.readAll().map { rch =>
      rch match {
        case Right(chic) =>
          Result(
            chic.values
              .flatMap { c =>
                c.players
              }
              .filter(p => p.length() > 0)
              .toList
              .distinct
          ).logit("getAllNames() on chicagos")
        case Left(error) =>
          BridgeServiceWithLogging.log
            .fine(s"getAllNames() got error on chicagos.readAll $error")
          Result(error)
      }
    }
    val n4 = rubbers.readAll().map { rr =>
      rr match {
        case Right(rub) =>
          Result(
            rub.values
              .flatMap { r =>
                r.north :: r.south :: r.east :: r.west :: Nil
              }
              .filter(p => p.length() > 0)
              .toList
              .distinct
          ).logit("getAllNames() on rubbers")
        case Left(error) =>
          BridgeServiceWithLogging.log
            .fine(s"getAllNames() got error on rubbers.readAll $error")
          Result(error)
      }
    }
    val n5 = individualduplicates.readAll().map { rd =>
      rd match {
        case Right(dups) =>
          Result(
            dups.values
              .flatMap(_.players)
              .filter(p => p.length() > 0)
              .toList
              .distinct
          ).logit("getAllNames() on individualduplicates")
        case Left(error) =>
          BridgeServiceWithLogging.log
            .fine(s"getAllNames() got error on individualduplicates.readAll $error")
          Result(error)
      }
    }

    def compare(s1: String, s2: String) = {
      s1.toLowerCase() < s2.toLowerCase()
    }

    val futures = List(n1, n2, n3, n4, n5)

    (Future
      .foldLeft(futures)(List[String]()) { (ac, v) =>
        v match {
          case Right(vlist) =>
            BridgeServiceWithLogging.log
              .finest(s"getAllNames() got $vlist adding to $ac")
            ac ::: vlist
          case Left(error) =>
            BridgeServiceWithLogging.log
              .fine(s"getAllNames() got error $error returning $ac")
            ac
        }
      })
      .map { l =>
        val d = l.distinct.sortWith(compare _)
        BridgeServiceWithLogging.log.finest(s"getAllNames() returning $d")
        Result(d)
      }
  }

  def getDuplicateSummaries(): Future[Result[List[DuplicateSummary]]] = {
    val fmds = duplicates.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map(md => DuplicateSummary.create(md)).toList
        case Left(r) =>
          Nil
      }
    }
    val fmdrs = duplicateresults.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map(md => DuplicateSummary.create(md)).toList
        case Left(r) =>
          Nil
      }
    }

    fmds.flatMap { mds =>
      fmdrs.map { mdrs =>
        Result((mds ::: mdrs).sortWith((one, two) => one.created > two.created))
      }
    }
  }

  def getIndividualDuplicateSummaries(): Future[Result[List[IDuplicateSummary]]] = {
    val fmds = duplicates.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map(md => IDuplicateSummary.create(md)).toList
        case Left(r) =>
          Nil
      }
    }
    val fmids = individualduplicates.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map(md => IDuplicateSummary.create(md)).toList
        case Left(r) =>
          Nil
      }
    }
    val fmdrs = duplicateresults.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map(md => IDuplicateSummary.create(md)).toList
        case Left(r) =>
          Nil
      }
    }

    for {
      mds <- fmds
      mdrs <- fmdrs
      mids <- fmids
    } yield {
      Result((mds ::: mdrs ::: mids).sortWith((one, two) => one.created > two.created))
    }
  }

  def getDuplicatePlaceResults(
      scoringMethod: CalculatePlayerPlaces.ScoringMethod =
        CalculatePlayerPlaces.AsPlayedScoring
  ): Future[Result[PlayerPlaces]] = {
    val fmds = duplicates.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map(md => DuplicateSummary.create(md)).toList
        case Left(r) =>
          Nil
      }
    }
    val fmdrs = duplicateresults.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map(md => DuplicateSummary.create(md)).toList
        case Left(r) =>
          Nil
      }
    }

    fmds.flatMap { mds =>
      fmdrs.map { mdrs =>
        Result {
          val calc = new CalculatePlayerPlaces(scoringMethod)
          (mds ::: mdrs).map(d => calc.add(d))
          calc.finish()
        }
      }
    }

  }

  /**
    * Delete this BridgeService.
    */
  def delete(): Future[Result[String]] = {
    Result.future(StatusCodes.BadRequest, RestMessage("Delete not supported"))
  }
}

object BridgeService {

  /**
    * Create a persistent BridgeService that stores data on disk at the specified path.
    * @param path a zip file or a directory.  If a zip file is specified it must exist.
    *             if path does not exist, then a directory will be created.
    * @param useIdFromValue
    * @param dontUpdateTime
    * @param useYaml
    * @param id the id of the store.
    * @param execute
    * @throw IllegalArgumentException if path is not a zip file or directory
    */
  def apply(
      path: Path,
      useIdFromValue: Boolean = false,
      dontUpdateTime: Boolean = false,
      useYaml: Boolean = true,
      id: Option[String] = None,
      diagnosticDirectory: Option[Directory] = None
  )(implicit
      execute: ExecutionContext
  ): BridgeServiceWithLogging = {
    if (path.isFile) {
      if (path.extension == "zip") {
        new BridgeServiceZipStore(
          id.getOrElse(path.name),
          path.toFile,
          useYaml
        )(execute) {
          override val diagnosticDir: Option[Directory] = diagnosticDirectory
        }
      } else {
        throw new IllegalArgumentException(
          "path parameter must be a zipfile or a directory"
        )
      }
    } else {
      new BridgeServiceFileStore(
        path.toDirectory,
        useIdFromValue,
        dontUpdateTime,
        useYaml,
        id
      )(execute) {
        override val diagnosticDir: Option[Directory] = diagnosticDirectory
      }
    }
  }

  /**
    * Copy the specified resource into the zip file with the given name.
    * If resource does not exist, then this is a noop.
    * @param resource the resource to load
    * @param nameInZip the name of the file in the zipfile.
    * @param zip the zip output stream
    */
  def copyResourceToZip(
      resource: String,
      nameInZip: String,
      zip: ZipOutputStream
  ): Unit = {
    val cl = getClass.getClassLoader
    val instream = cl.getResourceAsStream(resource)
    if (instream != null) {
      Using.resource(instream) { in =>
        val ze = new ZipEntry(nameInZip)
        BridgeServiceWithLogging.log.fine(
          s"Adding version info => ${ze.getName}"
        )
        zip.putNextEntry(ze)
        copy(in, zip)
        zip.closeEntry()
      }
    } else {
      BridgeServiceWithLogging.log.fine(s"Did not find resource $resource")
    }
  }

  def copy(in: InputStream, out: OutputStream): Long = {
    val b = new Array[Byte](1024 * 1024)

    var count: Long = 0
    var rlen = 0
    while ({ rlen = in.read(b); rlen } > 0) {
      out.write(b, 0, rlen)
      count += rlen
    }
    count
  }

}

object BridgeServiceWithLogging {
  val log: Logger = Logger[BridgeServiceWithLogging]()

  def getDefaultRemoteLoggerConfig(): Option[RemoteLoggingConfig] = {
    RemoteLoggingConfig.getDefaultRemoteLoggerConfig()
  }
}

abstract class BridgeServiceWithLogging(id: String) extends BridgeService(id) {
  import BridgeServiceWithLogging._

  import scala.collection.mutable.Map

  protected val fLoggerConfigs: Map[String, LoggerConfig] =
    Map[String, LoggerConfig]()

  protected var defaultLoggerConfig: LoggerConfig = LoggerConfig(Nil, Nil)
  protected var defaultLoggerConfigIPad: LoggerConfig = LoggerConfig(Nil, Nil)

  init()

  private def init() = {
    getDefaultRemoteLoggerConfig() match {
      case Some(rlc) =>
        rlc.browserConfig("default", "default").foreach { lc =>
          log.fine(s"Setting default browser logging to ${lc}")
          defaultLoggerConfig = lc
        }
        rlc.browserConfig("ipad", "default").foreach { lc =>
          log.fine(s"Setting default iPad logging to ${lc}")
          defaultLoggerConfigIPad = lc
        }
      case None =>
        log.fine("Did not find default remote logging profile")
    }
  }

  def loggerConfig(ip: String, iPad: Boolean): LoggerConfig = {
    fLoggerConfigs.getOrElse(
      ip,
      if (iPad) defaultLoggerConfigIPad else defaultLoggerConfig
    )
  }

  def setDefaultLoggerConfig(default: LoggerConfig, iPad: Boolean): Unit =
    if (iPad) {
      log.fine(s"Setting default iPad logging to ${default}")
      defaultLoggerConfigIPad = default
    } else {
      log.fine(s"Setting default browser logging to ${default}")
      defaultLoggerConfig = default
    }

  def getDefaultLoggerConfig(iPad: Boolean): LoggerConfig =
    if (iPad) {
      defaultLoggerConfigIPad
    } else {
      defaultLoggerConfig
    }
}

/**
  * An implementation of the backend for our service.
  * This is used for testing, and is predefined with
  * resources to facilitate testing.
  * @author werewolf
  */
class BridgeServiceInMemory(
    id: String,
    useIdFromValue: Boolean = false,
    useYaml: Boolean = true
) extends BridgeServiceWithLogging(id) {

  self =>

  val bridgeResources: BridgeResources = BridgeResources(useYaml)
  import bridgeResources._

  val chicagos: Store[MatchChicago.Id, MatchChicago] =
    InMemoryStore[MatchChicago.Id, MatchChicago](id)

  val duplicates: Store[MatchDuplicate.Id, MatchDuplicate] =
    InMemoryStore[MatchDuplicate.Id, MatchDuplicate](id)

  val individualduplicates: Store[IndividualDuplicate.Id, IndividualDuplicate] =
    InMemoryStore[IndividualDuplicate.Id, IndividualDuplicate](id)

  val duplicateresults: Store[MatchDuplicateResult.Id, MatchDuplicateResult] =
    InMemoryStore[MatchDuplicateResult.Id, MatchDuplicateResult](id)

  val rubbers: Store[MatchRubber.Id, MatchRubber] =
    InMemoryStore[MatchRubber.Id, MatchRubber](id)

  val boardSets: Store[BoardSet.Id, BoardSet] =
    MultiStore.createInMemoryAndResource[BoardSet.Id, BoardSet](
      id,
      "/com/github/thebridsk/bridge/server/backend/",
      "Boardsets.txt",
      self.getClass.getClassLoader
    )

  val movements: Store[Movement.Id, Movement] =
    MultiStore.createInMemoryAndResource[Movement.Id, Movement](
      id,
      "/com/github/thebridsk/bridge/server/backend/",
      "Movements.txt",
      self.getClass.getClassLoader
    )

  val individualMovements: Store[IndividualMovement.Id, IndividualMovement] =
    MultiStore.createInMemoryAndResource[IndividualMovement.Id, IndividualMovement](
      id,
      "/com/github/thebridsk/bridge/server/backend/",
      "IndividualMovements.txt",
      self.getClass.getClassLoader
    )

  override def toString() = "BridgeServiceInMemory"
}
