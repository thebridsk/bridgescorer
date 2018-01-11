package com.example.test

import java.net.InetAddress

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.RemoteAddress.IP
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import akka.stream.scaladsl.Flow
import org.scalatest._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.event.Logging
import java.net.InetAddress

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Left
import scala.util.Right
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure
import scala.reflect.io.Directory
import org.scalactic.source.Position
import org.scalatest._
import com.example.backend.resource.InMemoryStore
import com.example.data.Id
import com.example.data.MatchDuplicate
import com.example.backend.BridgeResources
import com.example.data.sample.TestMatchDuplicate
import com.example.backend.resource.ChangeContext
import com.example.backend.resource.Result
import com.example.source.SourcePosition
import com.example.backend.DuplicateTeamsNestedResource
import com.example.data.Team
import com.example.data.BoardSet
import com.example.backend.resource.JavaResourceStore
import com.example.backend.resource.StoreListener
import com.example.backend.resource.CreateChangeContext
import com.example.backend.resource.UpdateChangeContext
import com.example.backend.resource.DeleteChangeContext
import com.example.backend.DuplicateBoardsNestedResource
import com.example.backend.DuplicateHandsNestedResource
import com.example.data.Hand
import com.example.data.DuplicateHand
import com.example.data.Board
import com.example.backend.resource.ChangeContextData
import com.example.data.Movement
import com.example.backend.resource.FileStore
import com.example.backend.resource.FileIO
import com.example.backend.resource.MultiStore
import com.example.backend.resource.Store
import com.example.backend.BridgeServiceInMemory
import com.example.test.backend.TestFailurePersistent
import com.example.test.backend.TestFailureStore
import com.example.test.backend.BridgeServiceTesting
import com.example.service.MyService
import com.example.rest.ServerPort
import com.example.json.BridgePlayJsonSupport
import com.example.rest.UtilsPlayJson
import akka.http.scaladsl.testkit.RouteTestTimeout

object TestCacheStoreWithRoute {
  import MustMatchers._

  val testlog = utils.logging.Logger[TestCacheStoreWithRoute]

  TestStartLogging.startLogging()

  val bridgeResources = BridgeResources()
  import bridgeResources._

  def getBoardSetStore: Store[String,BoardSet] = {
    JavaResourceStore("/com/example/backend/", "Boardsets.txt", getClass.getClassLoader)
  }

  def getMovementStore: Store[String,Movement] = {
    JavaResourceStore("/com/example/backend/", "Movements.txt", getClass.getClassLoader)
  }

  import ExecutionContext.Implicits.global

  /**
   * An implementation of the backend for our service.
   * This is used for testing, and is predefined with
   * resources to facilitate testing.
   * @author werewolf
   */
  class BridgeServiceTestFailure extends BridgeServiceInMemory {

    val persistent = TestFailurePersistent[Id.MatchDuplicate,MatchDuplicate]()
    override
    val duplicates = TestFailureStore(persistent)

    val syncDuplicates = duplicates.syncStore

    syncDuplicates.createChild(BridgeServiceTesting.testingMatch)

    override
    def toString() = "BridgeServiceTestFailure"
  }

  implicit class WrapFuture[T]( val f: Future[Result[T]] ) extends AnyVal {
    def resultFailed( comment: String )( implicit pos: SourcePosition): Future[Result[T]] = {
      f.map(r => r.resultFailed(comment)(pos))
    }

    def test( comment: String )(block: T=>Assertion)( implicit pos: SourcePosition): Future[Assertion] = {
      f.map( r => r.test(comment)(block)(pos) )
    }

    def testfuture( comment: String )(block: T=>Future[Assertion])( implicit pos: SourcePosition): Future[Assertion] = {
      f.flatMap( r => r.testfuture(comment)(block)(pos) )
    }

    def expectFail( comment: String, expectStatusCode: StatusCode )( implicit pos: SourcePosition): Future[Assertion] = {
      f.map( r => r.expectFail(comment,expectStatusCode)(pos) )
    }

  }

  implicit class TestResult[T]( val r: Result[T] ) extends AnyVal {
    def resultFailed( comment: String )( implicit pos: SourcePosition): Result[T] = {
      r match {
        case Left((statusCode,msg)) =>
          fail(s"""${pos.line} ${comment} failed with $statusCode $msg""")
        case _ => r
      }
    }

    def test( comment: String )(block: T=>Assertion)( implicit pos: SourcePosition): Assertion = {
      withClue(s"${pos.line} ${comment}") {
        r match {
          case Right(t) => block(t)
          case Left((statusCode,msg)) =>
            fail(s"""failed with $statusCode $msg""")
        }
      }
    }

    def testfuture( comment: String )(block: T=>Future[Assertion])( implicit pos: SourcePosition): Future[Assertion] = {
      withClue(s"${pos.line} ${comment}") {
        r match {
          case Right(t) => block(t)
          case Left((statusCode,msg)) =>
            fail(s"""failed with $statusCode $msg""")
        }
      }
    }

    def expectFail( comment: String, expectStatusCode: StatusCode )( implicit pos: SourcePosition): Assertion = {
      withClue(s"${pos.line} ${comment}") {
        r match {
          case Right(t) => fail( s"Expected a fail ${expectStatusCode}, got $t" )
          case Left((statusCode,msg)) =>
            withClue(s"""failed with $statusCode $msg""") {
              statusCode mustBe expectStatusCode
            }
        }
      }
    }
  }
}

/**
 * Test class to start the logging system
 */
class TestCacheStoreWithRoute extends FlatSpec with ScalatestRouteTest with MustMatchers with BeforeAndAfterAll with MyService {
  import TestCacheStoreWithRoute._

  val restService = new BridgeServiceTestFailure

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer

  TestStartLogging.startLogging()

  import UtilsPlayJson._

  behavior of "Store"

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  var dupid: Option[Id.MatchDuplicate] = None

  var dupidNotCached: Option[Id.MatchDuplicate] = None

  it should "store a value in the store" in {
    val omd = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?default=true", omd) ~> addHeader(remoteAddress) ~>
        Route.seal { myRouteWithLogging } ~>
        check {
      status mustBe StatusCodes.Created

      val md = responseAs[MatchDuplicate]

      dupid = Some(md.id)

      restService.persistent.generateNextId(md) match {
        case Right(newid) =>
          dupidNotCached = Some(newid)
          val nmd = md.setId(newid, true, false)
          restService.persistent.add(nmd)
        case Left(error) =>
          fail(s"Unexpected error getting next ID: ${error}")
      }

      val nmd = md.updateTeam(Team("T1","Fred","George",0,0))
      Put(s"/v1/rest/duplicates/${md.id}", nmd) ~> addHeader(remoteAddress) ~>
          Route.seal { myRouteWithLogging } ~>
          check {
        status mustBe StatusCodes.NoContent
      }
    }
  }

  it should "fail to read a value in the store" in {

    restService.persistent.failRead = true

    val id = dupidNotCached.get
    restService.duplicates.getCached(id) mustBe None
    Get(s"/v1/rest/duplicates/${id}") ~> addHeader(remoteAddress) ~>
        Route.seal { myRouteWithLogging } ~>
        check {
      status mustBe StatusCodes.InternalServerError
      responseAs[String] mustBe "An error occurred: Failure reading from persistent store!"

      restService.duplicates.getCached(id) mustBe None

      restService.persistent.failRead = false

      Get(s"/v1/rest/duplicates/${id}") ~> addHeader(remoteAddress) ~>
          Route.seal { myRouteWithLogging } ~>
          check {
        status mustBe StatusCodes.OK

        restService.duplicates.getCached(id) must not be None
      }
    }
  }

  it should "fail to store a value in the store" in {

    restService.persistent.failRead = false
    restService.persistent.failWrite = true

    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?default=true", md) ~> addHeader(remoteAddress) ~>
        Route.seal { myRouteWithLogging } ~>
        check {
      status mustBe StatusCodes.InternalServerError
      responseAs[String] mustBe "An error occurred: Failure writing to persistent store!"

      restService.persistent.failWrite = false
      Post("/v1/rest/duplicates?default=true", md) ~> addHeader(remoteAddress) ~>
          Route.seal { myRouteWithLogging } ~>
          check {
        status mustBe StatusCodes.Created

      }

    }
  }

  it should "store a value in the store even if getAllIds fails" in {

    restService.persistent.failWrite = false
    restService.persistent.failGetAllIds = true

    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?default=true", md) ~> addHeader(remoteAddress) ~>
        Route.seal { myRouteWithLogging } ~>
        check {
      status mustBe StatusCodes.Created

    }
  }

  it should "fail to update a hand value in the store" in {

    val id = dupidNotCached.get
    Get(s"/v1/rest/duplicates/${id}") ~> addHeader(remoteAddress) ~>
        Route.seal { myRouteWithLogging } ~>
        check {
      status mustBe StatusCodes.OK
      val md = responseAs[MatchDuplicate]

      md.boards.headOption match {
        case Some(board) =>
          board.hands.headOption match {
            case Some(hand) =>
              val boardid = board.id
              val handid = hand.id

              val nhand = hand.updateHand(Hand(handid,3,"S","N","N",board.nsVul,board.ewVul,true,3,0,0))

              restService.persistent.failWrite = true

              import akka.testkit.TestDuration
              import scala.language.postfixOps
              implicit val timeout = RouteTestTimeout(5.seconds dilated)

              Put(s"/v1/rest/duplicates/${id}/boards/${boardid}/hands/${handid}", nhand) ~> addHeader(remoteAddress) ~>
                  Route.seal { myRouteWithLogging } ~>
                  check {
                status mustBe StatusCodes.InternalServerError

                restService.persistent.get(id) match {
                  case Some(rmd) =>
                    md.getBoard(boardid) match {
                      case Some(rboard) =>
                        rboard.getHand(handid) match {
                          case Some(rhand) =>
                            withClue("Hand should not be changed") {
                              rhand.equalsIgnoreModifyTime(hand)
                            }
                          case None =>
                            fail("could not find hand in MatchDuplicate in persistent store")
                        }
                      case None =>
                        fail("could not find board in MatchDuplicate in persistent store")
                    }
                  case None =>
                    fail("could not find MatchDuplicate in persistent store")
                }

                restService.persistent.failWrite = false
                Put(s"/v1/rest/duplicates/${id}/boards/${boardid}/hands/${handid}", nhand) ~> addHeader(remoteAddress) ~>
                    Route.seal { myRouteWithLogging } ~>
                    check {
                  status mustBe StatusCodes.NoContent

                  restService.persistent.get(id) match {
                    case Some(rmd) =>
                      md.getBoard(boardid) match {
                        case Some(rboard) =>
                          rboard.getHand(handid) match {
                            case Some(rhand) =>
                              withClue("Hand should not be changed") {
                                rhand.equalsIgnoreModifyTime(nhand)
                              }
                            case None =>
                              fail("could not find hand in MatchDuplicate in persistent store")
                          }
                        case None =>
                          fail("could not find board in MatchDuplicate in persistent store")
                      }
                    case None =>
                      fail("could not find MatchDuplicate in persistent store")
                  }

                  restService.persistent.failWrite = false
                }
              }


            case None =>
              fail("Did not find any hands")
          }
        case None =>
          fail("Did not find any boards")
      }
    }
  }

}
