package com.github.thebridsk.bridge.server.test

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.RemoteAddress.IP
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import java.net.InetAddress

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Left
import scala.util.Right
import scala.concurrent.Future
import scala.concurrent.duration._
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.server.backend.BridgeResources
import com.github.thebridsk.bridge.server.backend.resource.Result
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.backend.resource.JavaResourceStore
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.server.backend.resource.Store
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.server.test.backend.TestFailurePersistent
import com.github.thebridsk.bridge.server.test.backend.TestFailureStore
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.server.rest.UtilsPlayJson
import akka.http.scaladsl.testkit.RouteTestTimeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.compatible.Assertion
import com.github.thebridsk.utilities.logging.Logger

object TestCacheStoreWithRoute {
  import Matchers._

  val testlog: Logger =
    com.github.thebridsk.utilities.logging.Logger[TestCacheStoreWithRoute]()

  TestStartLogging.startLogging()

  val bridgeResources: BridgeResources = BridgeResources()
  import bridgeResources._

  def getBoardSetStore: Store[BoardSet.Id, BoardSet] = {
    JavaResourceStore(
      "test",
      "/com/github/thebridsk/bridge/server/backend/",
      "Boardsets.txt",
      getClass.getClassLoader
    )
  }

  def getMovementStore: Store[Movement.Id, Movement] = {
    JavaResourceStore(
      "test",
      "/com/github/thebridsk/bridge/server/backend/",
      "Movements.txt",
      getClass.getClassLoader
    )
  }

  import ExecutionContext.Implicits.global

  /**
    * An implementation of the backend for our service.
    * This is used for testing, and is predefined with
    * resources to facilitate testing.
    * @author werewolf
    */
  class BridgeServiceTestFailure extends BridgeServiceInMemory("test") {

    val persistent: TestFailurePersistent[MatchDuplicate.Id, MatchDuplicate] =
      TestFailurePersistent[MatchDuplicate.Id, MatchDuplicate]()
    override val duplicates
        : TestFailureStore[MatchDuplicate.Id, MatchDuplicate] =
      TestFailureStore("test", persistent)

    val syncDuplicates = duplicates.syncStore

    syncDuplicates.createChild(BridgeServiceTesting.testingMatch)

    override def toString() = "BridgeServiceTestFailure"
  }

  implicit class WrapFuture[T](private val f: Future[Result[T]])
      extends AnyVal {
    def resultFailed(
        comment: String
    )(implicit pos: SourcePosition): Future[Result[T]] = {
      f.map(r => r.resultFailed(comment)(pos))
    }

    def test(comment: String)(
        block: T => Assertion
    )(implicit pos: SourcePosition): Future[Assertion] = {
      f.map(r => r.test(comment)(block)(pos))
    }

    def testfuture(comment: String)(
        block: T => Future[Assertion]
    )(implicit pos: SourcePosition): Future[Assertion] = {
      f.flatMap(r => r.testfuture(comment)(block)(pos))
    }

    def expectFail(comment: String, expectStatusCode: StatusCode)(implicit
        pos: SourcePosition
    ): Future[Assertion] = {
      f.map(r => r.expectFail(comment, expectStatusCode)(pos))
    }

  }

  implicit class TestResult[T](private val r: Result[T]) extends AnyVal {
    def resultFailed(
        comment: String
    )(implicit pos: SourcePosition): Result[T] = {
      r match {
        case Left((statusCode, msg)) =>
          fail(s"""${pos.line} ${comment} failed with $statusCode $msg""")
        case _ => r
      }
    }

    def test(
        comment: String
    )(block: T => Assertion)(implicit pos: SourcePosition): Assertion = {
      withClue(s"${pos.line} ${comment}") {
        r match {
          case Right(t) => block(t)
          case Left((statusCode, msg)) =>
            fail(s"""failed with $statusCode $msg""")
        }
      }
    }

    def testfuture(comment: String)(
        block: T => Future[Assertion]
    )(implicit pos: SourcePosition): Future[Assertion] = {
      withClue(s"${pos.line} ${comment}") {
        r match {
          case Right(t) => block(t)
          case Left((statusCode, msg)) =>
            fail(s"""failed with $statusCode $msg""")
        }
      }
    }

    def expectFail(comment: String, expectStatusCode: StatusCode)(implicit
        pos: SourcePosition
    ): Assertion = {
      withClue(s"${pos.line} ${comment}") {
        r match {
          case Right(t) => fail(s"Expected a fail ${expectStatusCode}, got $t")
          case Left((statusCode, msg)) =>
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
class TestCacheStoreWithRoute
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers
    with BeforeAndAfterAll
    with MyService {
  import TestCacheStoreWithRoute._

  val restService = new BridgeServiceTestFailure

  val httpport = 8080
  override def ports: ServerPort = ServerPort(Option(httpport), None)

  implicit lazy val actorSystem = system //scalafix:ok ExplicitResultTypes
  implicit lazy val actorExecutor = executor //scalafix:ok ExplicitResultTypes
  implicit lazy val actorMaterializer =
    materializer //scalafix:ok ExplicitResultTypes

  TestStartLogging.startLogging()

  import UtilsPlayJson._

  behavior of "Store"

  val remoteAddress = `Remote-Address`(
    IP(InetAddress.getLocalHost, Some(12345))
  ) // scalafix:ok ; Remote-Address

  var dupid: Option[MatchDuplicate.Id] = None

  var dupidNotCached: Option[MatchDuplicate.Id] = None

  it should "store a value in the store" in {
    val omd = MatchDuplicate.create(MatchDuplicate.idNul)
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

        val nmd = md.updateTeam(Team(Team.id(1), "Fred", "George", 0, 0))
        Put(s"/v1/rest/duplicates/${md.id.id}", nmd) ~> addHeader(
          remoteAddress
        ) ~>
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
    Get(s"/v1/rest/duplicates/${id.id}") ~> addHeader(remoteAddress) ~>
      Route.seal { myRouteWithLogging } ~>
      check {
        status mustBe StatusCodes.InternalServerError
        responseAs[
          String
        ] mustBe "An error occurred: Failure reading from persistent store!"

        restService.duplicates.getCached(id) mustBe None

        restService.persistent.failRead = false

        Get(s"/v1/rest/duplicates/${id.id}") ~> addHeader(remoteAddress) ~>
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

    val md = MatchDuplicate.create(MatchDuplicate.idNul)
    Post("/v1/rest/duplicates?default=true", md) ~> addHeader(remoteAddress) ~>
      Route.seal { myRouteWithLogging } ~>
      check {
        status mustBe StatusCodes.InternalServerError
        responseAs[
          String
        ] mustBe "An error occurred: Failure writing to persistent store!"

        restService.persistent.failWrite = false
        Post("/v1/rest/duplicates?default=true", md) ~> addHeader(
          remoteAddress
        ) ~>
          Route.seal { myRouteWithLogging } ~>
          check {
            status mustBe StatusCodes.Created

          }

      }
  }

  it should "store a value in the store even if getAllIds fails" in {

    restService.persistent.failWrite = false
    restService.persistent.failGetAllIds = true

    val md = MatchDuplicate.create(MatchDuplicate.idNul)
    Post("/v1/rest/duplicates?default=true", md) ~> addHeader(remoteAddress) ~>
      Route.seal { myRouteWithLogging } ~>
      check {
        status mustBe StatusCodes.Created

      }
  }

  it should "fail to update a hand value in the store" in {

    val id = dupidNotCached.get
    Get(s"/v1/rest/duplicates/${id.id}") ~> addHeader(remoteAddress) ~>
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

                val nhand = hand.updateHand(
                  Hand(
                    handid.id,
                    3,
                    "S",
                    "N",
                    "N",
                    board.nsVul,
                    board.ewVul,
                    true,
                    3,
                    0,
                    0
                  )
                )

                restService.persistent.failWrite = true

                import akka.testkit.TestDuration
                import scala.language.postfixOps
                implicit val timeout = RouteTestTimeout(5.seconds dilated)

                Put(
                  s"/v1/rest/duplicates/${id.id}/boards/${boardid.id}/hands/${handid.id}",
                  nhand
                ) ~> addHeader(remoteAddress) ~>
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
                                fail(
                                  "could not find hand in MatchDuplicate in persistent store"
                                )
                            }
                          case None =>
                            fail(
                              "could not find board in MatchDuplicate in persistent store"
                            )
                        }
                      case None =>
                        fail(
                          "could not find MatchDuplicate in persistent store"
                        )
                    }

                    restService.persistent.failWrite = false
                    Put(
                      s"/v1/rest/duplicates/${id.id}/boards/${boardid.id}/hands/${handid.id}",
                      nhand
                    ) ~> addHeader(remoteAddress) ~>
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
                                    fail(
                                      "could not find hand in MatchDuplicate in persistent store"
                                    )
                                }
                              case None =>
                                fail(
                                  "could not find board in MatchDuplicate in persistent store"
                                )
                            }
                          case None =>
                            fail(
                              "could not find MatchDuplicate in persistent store"
                            )
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
