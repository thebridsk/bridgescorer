package com.example.backend

import com.example.data.Board
import com.example.data.Table
import com.example.data.Hand
import com.example.data.MatchChicago
import com.example.data.MatchChicago
import com.example.data.MatchChicago
import com.example.data.LoggerConfig
import com.example.data.bridge.North
import com.example.data.bridge.East
import com.example.data.MatchDuplicate
import com.example.data.DuplicateHand
import akka.http.scaladsl.model.StatusCodes._
import com.example.data.SystemTime
import com.example.data.Team
import com.example.data.Id
import com.example.data.Board
import scala.util.parsing.json.JSON
import scala.io.Source
import scala.collection.immutable.Map
import com.example.data.sample.TestMatchDuplicate
import com.example.backend.resource.MultiStore
import com.example.backend.resource.JavaResourceStore
import akka.http.scaladsl.model.StatusCodes
import com.example.data.BoardSet
import com.example.data.Movement
import com.example.data.MatchRubber
import com.example.logging.RemoteLoggingConfig
import utils.logging.Logger
import com.example.data.MatchDuplicateResult
import com.example.data.DuplicateSummary
import com.example.backend.resource.Store
import com.example.backend.resource.Result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.concurrent.Future
import com.example.backend.resource.InMemoryStore
import com.example.backend.resource.Implicits
import com.example.data.RestMessage

/**
 * The backend trait for our service.
 * Implementation of this trait should persistently store all
 * the objects that make up the API.
 */
abstract class BridgeService( val id: String ) {

  def loggerConfig( ip: String, iPad: Boolean ): LoggerConfig

  def setDefaultLoggerConfig( default: LoggerConfig, iPad: Boolean )

  val chicagos: Store[String,MatchChicago]
  val duplicates: Store[Id.MatchDuplicate,MatchDuplicate]
  val duplicateresults: Store[Id.MatchDuplicateResult,MatchDuplicateResult]
  val rubbers: Store[String,MatchRubber]

  val boardSets: Store[String,BoardSet]
  val movements: Store[String,Movement]

  /**
   * The import store.  Some some of the implementations support this, and will override this value.
   */
  val importStore: Option[ImportStore] = None

  val defaultBoards = "ArmonkBoards"
  val defaultMovement = "Armonk2Tables"

  def fillBoards( dup: MatchDuplicate ): Future[Result[MatchDuplicate]] = {
    fillBoards( dup, defaultBoards, defaultMovement )
  }

  def fillBoards( dup: MatchDuplicate,
                  boardset: String,
                  movement: String
                ): Future[Result[MatchDuplicate]] = {

    val fmv = movements.read(movement)
    val fbb = boardSets.read(boardset)

    fbb.flatMap { rbb =>
      rbb match {
        case Right(bb) =>
          fmv.map { rmv =>
            rmv match {
              case Right(mv) =>
                Result( dup.fillBoards(bb, mv) )
              case Left(error) =>
                Result(StatusCodes.BadRequest,s"Movement $movement was not found")
            }
          }
        case Left(error) =>
          Result.future(StatusCodes.BadRequest,s"Boardset $boardset was not found")
      }
    }

  }

  def createTestDuplicate( dup: MatchDuplicate ): Future[Result[MatchDuplicate]] = {
    fillBoards(dup).map { rd =>
      rd match {
        case Right(dd) =>
          var d = dd
          val hands = TestMatchDuplicate.getHand(d, "B3", "T1", 3, "N", "N", "N", true, 5 ) ::
                      TestMatchDuplicate.getHand(d, "B3", "T3", 3, "N", "N", "N", true, 5 ) ::
                      TestMatchDuplicate.getHands(d)
          for (h <- hands) {
            d = d.updateHand(h)
          }
          for ((id,t) <- TestMatchDuplicate.teams()) {
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
      def isNotEmpty( s: String ) = s!=null&&s.length()>0
      def contains( s: String ) = list.find { ss => s.equalsIgnoreCase(ss) }.isDefined
      def add(s: String) = {
        if (isNotEmpty(s) && !contains(s)) list = list + s
      }
      val n1 = duplicates.readAll().map { rd =>
        rd match {
          case Right(dups) =>
            Result(dups.values.flatMap { dup =>
              dup.teams.flatMap { t => t.player1::t.player2::Nil }
            }.filter(p => p.length()>0).toList.distinct).logit("getAllNames() on duplicates")
          case Left(error) =>
            BridgeServiceWithLogging.log.fine( s"getAllNames() got error on duplicates.readAll $error" )
            Result(error)
        }
      }
      val n2 = duplicateresults.readAll().map { rd =>
        rd match {
          case Right(dups) =>
            Result(dups.values.flatMap { dup =>
              dup.results.flatten.flatMap { t => t.team.player1::t.team.player2::Nil }
            }.filter(p => p.length()>0).toList.distinct).logit("getAllNames() on duplicateresults")
          case Left(error) =>
            BridgeServiceWithLogging.log.fine( s"getAllNames() got error on duplicateresults.readAll $error" )
            Result(error)
        }
      }
      val n3 = chicagos.readAll().map { rch =>
        rch match {
          case Right(chic) =>
            Result(chic.values.flatMap { c => c.players }.filter(p => p.length()>0).toList.distinct).logit("getAllNames() on chicagos")
          case Left(error) =>
            BridgeServiceWithLogging.log.fine( s"getAllNames() got error on chicagos.readAll $error" )
            Result(error)
        }
      }
      val n4 = rubbers.readAll().map { rr =>
        rr match {
          case Right(rub) =>
            Result(rub.values.flatMap { r => r.north::r.south::r.east::r.west::Nil}.filter(p => p.length()>0).toList.distinct ).logit("getAllNames() on rubbers")
          case Left(error) =>
            BridgeServiceWithLogging.log.fine( s"getAllNames() got error on rubbers.readAll $error" )
            Result(error)
        }
      }

      def compare( s1: String, s2: String ) = {
        s1.toLowerCase() < s2.toLowerCase()
      }

      val futures = List(n1,n2,n3,n4)

      (Future.foldLeft(futures)(List[String]()) { (ac,v)=>
        v match {
          case Right(vlist) =>
            BridgeServiceWithLogging.log.finest( s"getAllNames() got $vlist adding to $ac" )
            ac:::vlist
          case Left(error) =>
            BridgeServiceWithLogging.log.fine( s"getAllNames() got error $error returning $ac" )
            ac
        }
      }).map { l =>
        val d = l.distinct.sortWith(compare _)
        BridgeServiceWithLogging.log.finest( s"getAllNames() returning $d" )
        Result(d)
      }
  }

  def getDuplicateSummaries(): Future[Result[List[DuplicateSummary]]] = {
    val fmds = duplicates.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map( md => DuplicateSummary.create(md) ).toList
        case Left(r) =>
          Nil
      }
    }
    val fmdrs = duplicateresults.readAll().map { fmds =>
      fmds match {
        case Right(m) =>
          m.values.map( md => DuplicateSummary.create(md) ).toList
        case Left(r) =>
          Nil
      }
    }

    fmds.flatMap { mds =>
      fmdrs.map { mdrs =>
        Result((mds:::mdrs).sortWith((one,two)=>one.created>two.created))
      }
    }
  }

  /**
   * Delete this BridgeService.
   */
  def delete(): Future[Result[String]] = {
    Result.future(StatusCodes.BadRequest,RestMessage("Delete not supported"))
  }
}

object BridgeServiceWithLogging {
  val log = Logger[BridgeServiceWithLogging]

  def getDefaultRemoteLoggerConfig() = {
    RemoteLoggingConfig.getDefaultRemoteLoggerConfig()
  }
}


abstract class BridgeServiceWithLogging( id: String ) extends BridgeService(id) {
  import BridgeServiceWithLogging._

  import scala.collection.mutable.Map

  protected val fLoggerConfigs = Map[String,LoggerConfig]()

  protected var defaultLoggerConfig = LoggerConfig( Nil, Nil)
  protected var defaultLoggerConfigIPad = LoggerConfig( Nil, Nil)

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

  def loggerConfig( ip: String, iPad: Boolean ) = {
    fLoggerConfigs.getOrElse(ip, if (iPad) defaultLoggerConfigIPad else defaultLoggerConfig )
  }

  def setDefaultLoggerConfig( default: LoggerConfig, iPad: Boolean ) = if (iPad) {
    log.fine(s"Setting default iPad logging to ${default}")
    defaultLoggerConfigIPad = default
  } else {
    log.fine(s"Setting default browser logging to ${default}")
    defaultLoggerConfig = default
  }

  def getDefaultLoggerConfig(iPad: Boolean) = if (iPad) {
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
class BridgeServiceInMemory( id: String, useIdFromValue: Boolean = false, useYaml: Boolean = true ) extends BridgeServiceWithLogging(id) {

  self =>

  val bridgeResources = BridgeResources(useYaml)
  import bridgeResources._

  val chicagos: Store[Id.MatchChicago,MatchChicago] = InMemoryStore[Id.MatchChicago,MatchChicago]()

  val duplicates: Store[Id.MatchDuplicate,MatchDuplicate] = InMemoryStore[Id.MatchDuplicate,MatchDuplicate]()

  val duplicateresults: Store[Id.MatchDuplicateResult,MatchDuplicateResult] = InMemoryStore[Id.MatchDuplicateResult,MatchDuplicateResult]()

  val rubbers: Store[String,MatchRubber] = InMemoryStore[String,MatchRubber]()

  val boardSets: Store[String,BoardSet] = MultiStore.createInMemoryAndResource[String,BoardSet]("/com/example/backend/", "Boardsets.txt", self.getClass.getClassLoader)

  val movements: Store[String,Movement] = MultiStore.createInMemoryAndResource[String,Movement]("/com/example/backend/", "Movements.txt", self.getClass.getClassLoader)

  override
  def toString() = "BridgeServiceInMemory"
}
