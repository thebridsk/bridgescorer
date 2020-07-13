package com.github.thebridsk.bridge.server.test.backend

import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.bridge.Spades
import com.github.thebridsk.bridge.data.bridge.Doubled
import com.github.thebridsk.bridge.data.bridge.Hearts
import com.github.thebridsk.bridge.data.bridge.South
import java.util.Date
import com.github.thebridsk.bridge.data.MatchDuplicateV3
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.server.backend.ImportStore
import com.github.thebridsk.bridge.server.backend.resource.MyCache
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.server.backend.resource.Implicits._
import scala.reflect.io.File


object BridgeServiceTesting {
  val testingMatch =
  {

    val team1 = Team.id(1)
    val team2 = Team.id(2)
    val team3 = Team.id(3)
    val team4 = Team.id(4)

    val teams = List(
                  Team.create(team1,"Nancy","Sam"),
                  Team.create(team2,"Ellen", "Wayne"),
                  Team.create(team3,"Norman","Sally"),
                  Team.create(team4,"Ethan","Wilma")
                )
    val time = System.currentTimeMillis().toDouble
    MatchDuplicateV3("M1", teams, List(
        Board.create("B1", false, false, North.pos, List(
            DuplicateHand.create( Hand.create("H1",7,Spades.suit, Doubled.doubled, North.pos,
                                              false,false,true,7),
                                 Table.id(1), 1, "B1", team1, team2),
            DuplicateHand.create( Hand.create("H2",7,Spades.suit, Doubled.doubled, North.pos,
                                              false,false,false,1),
                                  Table.id(2), 2, "B1", team3, team4)
            )),
        Board.create("B2", true, false, East.pos, List(
            DuplicateHand.create( Hand.create("H1",7,Hearts.suit, Doubled.doubled, North.pos,
                                              false,false,true,7),
                                  Table.id(1), 1, "B2", team1, team2)
            )),
        Board.create("B3", false, true, South.pos, List())
        ), "", "", time, time)

  }
}

/**
 * An implementation of the backend for our service.
 * This is used for testing, and is predefined with
 * resources to facilitate testing.
 * @author werewolf
 */
class BridgeServiceTesting extends BridgeServiceInMemory("test") {

  val storeduplicates = duplicates.syncStore

  storeduplicates.createChild(BridgeServiceTesting.testingMatch)

  override
  def toString() = "BridgeServiceTesting"
}


class ImportStoreTesting(
               val cacheInitialCapacity: Int = 5,
               val cacheMaxCapacity: Int = 100,
               val cacheTimeToLive: Duration = 61 minutes,
               val cacheTimeToIdle: Duration = 60 minutes
             )(
               implicit
                 execute: ExecutionContext
             ) extends ImportStore {

  private val cache = new MyCache[String,Result[BridgeService]]( cacheInitialCapacity, cacheMaxCapacity, cacheTimeToLive, cacheTimeToIdle )

  /**
   * Returns all the IDs of imported stores.  The ID is the the filename of the zip store, or the name of the directory for a file store.
   * @return a future that returns the list of IDs
   */
  def getAllIds(): Future[Result[List[String]]] = {
    Future {
      Result( cache.keys.toList )
    }
  }

  def get( id: String ): Future[Result[BridgeService]] = {
    cache.read(id, {
      Result( StatusCodes.NotFound, "Not found" )
    }).logit(s"get /imports/${id}")
  }

  def delete( id: String ): Future[Result[BridgeService]] = {
    Result.future( StatusCodes.BadRequest, "Not allowed" ).logit(s"delete /imports/${id}")
  }

  /**
   * Create a new store for imports.  This will copy the zip file into the imports area of the persistent store.
   * @param id
   * @param zipfile the filename of the zip file to import.
   * @return a future to the result of the operation.  Returns a BridgeService if successful.
   * Error is returned if id already exists.
   */
  def create( id: String, zipfile: File ): Future[Result[BridgeService]] = {
    Result.future( StatusCodes.BadRequest, "Not allowed" ).logit(s"delete /imports/${id}")
  }

  def addBridgeService( id: String, bs: BridgeService ) = {
    cache.create(id, ()=>Result.future(bs) ).logit(s"addBridgeService /imports/${id}")
  }
}
