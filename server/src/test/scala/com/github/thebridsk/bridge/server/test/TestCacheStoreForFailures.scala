package com.github.thebridsk.bridge.server.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.server.backend.BridgeResources
import com.github.thebridsk.bridge.data.sample.TestMatchDuplicate
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.server.backend.resource.ChangeContext
import scala.util.Left
import scala.util.Right
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
import org.scalactic.source.Position
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.BoardSet
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.server.backend.resource.StoreListener
import com.github.thebridsk.bridge.server.backend.resource.CreateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.UpdateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.DeleteChangeContext
import com.github.thebridsk.bridge.server.backend.DuplicateBoardsNestedResource
import com.github.thebridsk.bridge.server.backend.DuplicateHandsNestedResource
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Board
import scala.concurrent.duration._
import com.github.thebridsk.bridge.data.Movement
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.server.backend.resource.Store
import scala.util.Try
import com.github.thebridsk.bridge.server.backend.resource.ChangeContextData
import com.github.thebridsk.bridge.server.backend.resource.JavaResourcePersistentSupport
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.server.test.backend.TestFailurePersistent
import com.github.thebridsk.bridge.server.test.backend.TestFailureStore
import org.scalatest.matchers.must.Matchers
import org.scalatest.compatible.Assertion
import org.scalatest.flatspec.AsyncFlatSpec
import com.github.thebridsk.utilities.logging.Logger

object TestCacheStoreForFailures {
  import Matchers._

  val testlog: Logger =
    com.github.thebridsk.utilities.logging.Logger[TestCacheStoreForFailures]()

  TestStartLogging.startLogging()

  val bridgeResources: BridgeResources = BridgeResources()
  import bridgeResources._

  def boardsetsPersistent(implicit
      ec: ExecutionContext
  ): JavaResourcePersistentSupport[BoardSet.Id, BoardSet] =
    JavaResourcePersistentSupport[BoardSet.Id, BoardSet](
      "/com/github/thebridsk/bridge/server/backend/",
      "Boardsets.txt",
      getClass.getClassLoader
    )
  def movementsPersistent(implicit
      ec: ExecutionContext
  ): JavaResourcePersistentSupport[Movement.Id, Movement] =
    JavaResourcePersistentSupport[Movement.Id, Movement](
      "/com/github/thebridsk/bridge/server/backend/",
      "Movements.txt",
      getClass.getClassLoader
    )

  def standardBoardset(implicit ec: ExecutionContext): BoardSet =
    boardsetsPersistent.read(BoardSet.standard) match {
      case Right(v) => v
      case Left(error) =>
        throw new Exception(s"Unable to get standard boardset ${error}")
    }

  def movement(implicit ec: ExecutionContext): Movement =
    movementsPersistent.read(Movement.id("Mitchell3Table")) match {
      case Right(v) => v
      case Left(error) =>
        throw new Exception(s"Unable to get Mitchell3Table ${error}")
    }

  def getStore(implicit ec: ExecutionContext): (
      Store[MatchDuplicate.Id, MatchDuplicate],
      TestFailurePersistent[MatchDuplicate.Id, MatchDuplicate]
  ) = {
    val s = TestFailureStore[MatchDuplicate.Id, MatchDuplicate](
      "test",
      null,
      5,
      100,
      Duration.Inf,
      Duration.Inf
    )
    (s, s.testFailurePersistent)
  }

  trait Tester {
    def test(name: String, change: Option[ChangeContext])(implicit
        pos: Position
    ): Assertion
  }

  trait TesterExists extends Tester {

    final override def test(name: String, change: Option[ChangeContext])(
        implicit pos: Position
    ): Assertion = {
      change match {
        case Some(cc) =>
          cc.getSpecificChange() match {
            case Some(sc) =>
              testSpecific(sc)
            case None =>
              fail("Did not find a specific change context")
          }
        case None =>
          fail("Did not find a create change context")
      }
    }

    def testSpecific(change: ChangeContextData)(implicit
        pos: Position
    ): Assertion
  }

  class TesterEmpty extends Tester {
    def test(name: String, change: Option[ChangeContext])(implicit
        pos: Position
    ): Assertion = {
      if (change.isDefined)
        fail(s"Expecting ${name} change to be empty: ${change}")
      else 0 mustBe 0
    }

  }

  class Listener extends StoreListener {

    var changeCreate: Option[ChangeContext] = None
    var changeUpdate: Option[ChangeContext] = None
    var changeDelete: Option[ChangeContext] = None

    override def create(change: ChangeContext): Unit = {
      changeCreate = Some(change)
    }
    override def update(change: ChangeContext): Unit = {
      changeUpdate = Some(change)
    }
    override def delete(change: ChangeContext): Unit = {
      changeDelete = Some(change)
    }

    def clear(): Unit = {
      changeCreate = None
      changeUpdate = None
      changeDelete = None
    }

    def testEmpty()(implicit pos: Position): Assertion = {
      (new TesterEmpty).test("create", changeCreate)
      (new TesterEmpty).test("update", changeUpdate)
      (new TesterEmpty).test("delete", changeDelete)
    }

    def testCreate(tester: Tester)(implicit pos: Position): Assertion = {
      tester.test("create", changeCreate)
      (new TesterEmpty).test("update", changeUpdate)
      (new TesterEmpty).test("delete", changeDelete)
    }

    def testUpdate(tester: Tester)(implicit pos: Position): Assertion = {
      (new TesterEmpty).test("create", changeCreate)
      tester.test("update", changeUpdate)
      (new TesterEmpty).test("delete", changeDelete)
    }

    def testDelete(tester: Tester)(implicit pos: Position): Assertion = {
      (new TesterEmpty).test("create", changeCreate)
      (new TesterEmpty).test("update", changeUpdate)
      tester.test("delete", changeDelete)
    }
  }

  class TestCreateMatchDuplicateId(id: MatchDuplicate.Id) extends TesterExists {
    def testSpecific(
        change: ChangeContextData
    )(implicit pos: Position): Assertion = {
      change match {
        case CreateChangeContext(newvalue, field) =>
          newvalue match {
            case md: MatchDuplicate =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${id.id}"
                case None =>
                  fail("Expecting a parent field, got None")
              }
            case x =>
              fail(s"Expecting a MatchDuplicate, got ${x.getClass.getName}")
          }
        case x =>
          fail(s"Expecting a CreateChangeContext, got ${x.getClass.getName}")
      }
    }
  }

  class TestUpdateHandId(dupid: MatchDuplicate.Id, bid: Board.Id, id: Team.Id)
      extends TesterExists {
    def testSpecific(
        change: ChangeContextData
    )(implicit pos: Position): Assertion = {
      change match {
        case UpdateChangeContext(newvalue, field) =>
          newvalue match {
            case md: DuplicateHand =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${dupid.id}/boards/${bid.id}/hands/${id.id}"
                case None =>
                  fail("Expecting a parent field, got None")
              }
            case x =>
              fail(s"Expecting a MatchDuplicate, got ${x.getClass.getName}")
          }
        case x =>
          fail(s"Expecting a CreateChangeContext, got ${x.getClass.getName}")
      }
    }
  }

  class TestUpdateMatchDuplicateId(id: MatchDuplicate.Id) extends TesterExists {
    def testSpecific(
        change: ChangeContextData
    )(implicit pos: Position): Assertion = {
      change match {
        case UpdateChangeContext(newvalue, field) =>
          newvalue match {
            case md: MatchDuplicate =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${id.id}"
                case None =>
                  fail("Expecting a parent field, got None")
              }
            case x =>
              fail(s"Expecting a MatchDuplicate, got ${x.getClass.getName}")
          }
        case x =>
          fail(s"Expecting a CreateChangeContext, got ${x.getClass.getName}")
      }
    }
  }

  class TestDeleteMatchDuplicateId(id: MatchDuplicate.Id) extends TesterExists {
    def testSpecific(
        change: ChangeContextData
    )(implicit pos: Position): Assertion = {
      change match {
        case DeleteChangeContext(oldvalue, field) =>
          oldvalue match {
            case md: MatchDuplicate =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${id.id}"
                case None =>
                  fail("Expecting a parent field, got None")
              }
            case x =>
              fail(s"Expecting a MatchDuplicate, got ${x.getClass.getName}")
          }
        case x =>
          fail(s"Expecting a CreateChangeContext, got ${x.getClass.getName}")
      }
    }
  }

  def testWithStore(
      fun: (
          Store[MatchDuplicate.Id, MatchDuplicate],
          TestFailurePersistent[MatchDuplicate.Id, MatchDuplicate],
          Listener,
          MatchDuplicate
      ) => Future[Assertion]
  )(implicit ec: ExecutionContext): Future[Assertion] = {
    val (store, per) = getStore
    val md = TestMatchDuplicate.create(MatchDuplicate.idNul)

    val listener = new Listener
    store.addListener(listener)

    val fut = store.createChild(md).testfuture("Creating match duplicate") {
      tryresult =>
        tryresult match {
          case Success(Right(nmd)) =>
            Thread.sleep(500)
            fun(store, per, listener, nmd)
          case Success(Left(e)) =>
            fail("fail")
          case Failure(ex) =>
            fail(s"Failed with exception ${ex}")
        }
    }
    fut.onComplete(t => store.removeListener(listener))
    fut
  }

  implicit class WrapFuture[T](private val f: Future[Result[T]])
      extends AnyVal {

    def test(comment: String)(
        block: Try[Result[T]] => Assertion
    )(implicit pos: SourcePosition, ec: ExecutionContext): Future[Assertion] = {
      val b: Try[Result[T]] => Future[Assertion] = t => Future { block(t) }
      f.transformWith(b)
    }

    def testfuture(comment: String)(
        block: Try[Result[T]] => Future[Assertion]
    )(implicit pos: SourcePosition, ec: ExecutionContext): Future[Assertion] = {
      f.transformWith(block)
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
class TestCacheStoreForFailures
    extends AsyncFlatSpec
    with ScalatestRouteTest
    with Matchers {
  import TestCacheStoreForFailures._

  behavior of "Store with Failures"

  it should "store a value in the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      val md = TestMatchDuplicate.create(MatchDuplicate.idNul)
      store.createChild(md).test("Creating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            nmd.equalsIgnoreModifyTime(md.copy(id = nmd.id), true) mustBe true
            val a = listener.testCreate(new TestCreateMatchDuplicateId(nmd.id))
            a
          case Success(Left(error)) =>
            fail(s"Got error ${error}")
          case Failure(ex) =>
            fail(s"Got failure ${ex}")
        }
      }
  }

  it should "read a value from the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      val md = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md.id
      persistent.add(md)
      try {
        store.read(id).testfuture("Reading match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              nmd.id mustBe id
              listener.testEmpty()
              store
                .getCached(id)
                .map { f =>
                  f.map { rmd =>
                    rmd match {
                      case Left((statuscode, msg)) =>
                        fail(s"Error getting from cache: ${statuscode} ${msg}")
                      case Right(value) =>
                        value.id mustBe id
                    }
                  }
                }
                .getOrElse(fail(s"getCached return None"))
            case Success(Left(error)) =>
              fail(s"Got error ${error}")
            case Failure(ex) =>
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception reading from store", x)
          fail(s"Unexpected exception reading from store ${x}")
      }
  }

  it should "fail to read a value from the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultRead =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't read"))
      val md = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md.id
      persistent.add(md)
      try {
        store.read(id).test("Reading match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail("Expecting a failure")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Oops can't read"
              Thread.sleep(500L)
              store.getCached(id) mustBe None
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got error ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception reading from store", x)
          fail(s"Unexpected exception reading from store ${x}")
      }
  }

  it should "fail to read a value from the store, next read works" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultRead =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't read"))
      val md = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md.id
      persistent.add(md)
      try {
        store.read(id).testfuture("Reading match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              Future { fail("Expecting a failure") }
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Oops can't read"

              persistent.failResultRead = None

              store.read(id).testfuture("Reading match duplicate again") {
                tnmd =>
                  tnmd match {
                    case Success(Right(nmd)) =>
                      Future { nmd.id mustBe id }
                    case Success(Left(error)) =>
                      listener.testEmpty()

                      withClue(s"Clearing error for ${id.id}") {
                        store.clearError(id) mustBe false
                      }

                      store.read(id).test("Reading match duplicate again") {
                        tnmd =>
                          tnmd match {
                            case Success(Right(nmd)) =>
                              nmd.id mustBe id
                            case Success(Left(error)) =>
                              listener.testEmpty()
                              fail(s"Got error ${error}")
                            case Failure(ex) =>
                              listener.testEmpty()
                              fail(s"Got failure ${ex}")
                          }
                      }
                    case Failure(ex) =>
                      Future {
                        listener.testEmpty()
                        fail(s"Got failure ${ex}")
                      }
                  }
              }
            case Failure(ex) =>
              Future {
                listener.testEmpty()
                fail(s"Got error ${ex}")
              }
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception reading from store", x)
          fail(s"Unexpected exception reading from store ${x}")
      }
  }

  it should "fail to read a value from the store with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failRead = true
      val md = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md.id
      persistent.add(md)
      try {
        store.read(id).test("Reading match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail("Expecting a failure")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure reading from persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception reading from store", x)
          fail(s"Unexpected exception reading from store ${x}")
      }
  }

  it should "fail to read a value from the store with exception, next read works" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failRead = true
      val md = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md.id
      persistent.add(md)
      try {
        store.read(id).testfuture("Reading match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              Future {
                withClue("Expecting a failure") {
                  nmd mustBe null
                }
              }
            case Success(Left(error)) =>
              Future {
                listener.testEmpty()
                fail(s"Got error ${error}")
              }
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure reading from persistent store!"

              persistent.failRead = false

              store.read(id).test("Reading match duplicate again") { tnmd2 =>
                tnmd2 match {
                  case Success(Right(nmd)) =>
                    nmd.id mustBe id
                  case Success(Left(error)) =>
                    listener.testEmpty()
                    fail(s"Got error ${error}")
                  case Failure(ex) =>
                    listener.testEmpty()
                    fail(s"Got failure ${ex}")
                }
              }
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception reading from store", x)
          fail(s"Unexpected exception reading from store ${x}")
      }
  }

  it should "create a value in the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      val md = TestMatchDuplicate.create(MatchDuplicate.idNul)
      try {
        store.createChild(md).test("Creating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              nmd.equalsIgnoreModifyTime(md.copy(id = nmd.id), true) mustBe true
              listener.testCreate(new TestCreateMatchDuplicateId(nmd.id))
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to create a value in the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultWrite =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't write"))
      val md = TestMatchDuplicate.create(MatchDuplicate.idNul)
      try {
        store.createChild(md).test("Creating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Oops can't write"
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to create a value in the store with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failWrite = true
      val md = TestMatchDuplicate.create(MatchDuplicate.idNul)
      try {
        store.createChild(md).test("Creating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure writing to persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "update a value in the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      val md = md1.updateTeam(Team(Team.id(1), "Fred", "George", 0, 0))
      try {
        store.update(md.id, md).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              nmd.equalsIgnoreModifyTime(md.copy(id = nmd.id), true) mustBe true
              listener.testUpdate(new TestUpdateMatchDuplicateId(nmd.id))
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a value in the store with write failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultWrite =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't write"))
      val md = md1.updateTeam(Team(Team.id(1), "Fred", "George", 0, 0))
      try {
        store.update(md.id, md).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Oops can't write"
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a value in the store with write failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failWrite = true
      val md = md1.updateTeam(Team(Team.id(1), "Fred", "George", 0, 0))
      try {
        store.update(md.id, md).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure writing to persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a value in the store with read failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultRead =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't read"))
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      val md = md2.updateTeam(Team(Team.id(1), "Fred", "George", 0, 0))
      try {
        store.update(md.id, md).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Oops can't read"
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a value in the store with read failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failRead = true
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      val md = md2.updateTeam(Team(Team.id(1), "Fred", "George", 0, 0))
      try {
        store.update(md.id, md).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure reading from persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "delete a value from the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      try {
        store.delete(md1.id).test("Deleting match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              nmd.id mustBe md1.id
              listener.testDelete(new TestDeleteMatchDuplicateId(nmd.id))
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store with delete failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultDelete =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't delete"))
      try {
        store.delete(md1.id).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Partial success, metadata deleted, Oops can't delete"
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store using select with delete failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultDelete =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't delete"))
      try {
        store.select(md1.id).delete().test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Partial success, metadata deleted, Oops can't delete"
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store with delete failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failDelete = true
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      try {
        store.delete(md2.id).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure deleting from persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store using select with delete failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failDelete = true
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      try {
        store.select(md2.id).delete().test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure deleting from persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store with read failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultRead =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't read"))
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      try {
        store.getCached(id) mustBe Symbol("empty")
        store.delete(md2.id).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Oops can't read"
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store using select with read failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultRead =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't read"))
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      try {
        store.select(md2.id).delete().test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left((statuscode, RestMessage(msg)))) =>
              listener.testEmpty()
              statuscode mustBe StatusCodes.InsufficientStorage
              msg mustBe "Oops can't read"
            case Failure(ex) =>
              listener.testEmpty()
              fail(s"Got failure ${ex}")
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store with read failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failRead = true
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      try {
        store.delete(md2.id).test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure reading from persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to delete a value from the store using select with read failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failRead = true
      val md2 = MatchDuplicate.create(MatchDuplicate.id("M2"))
      val id = md2.id
      persistent.add(md2)
      try {
        store.select(md2.id).delete().test("Updating match duplicate") { tnmd =>
          tnmd match {
            case Success(Right(nmd)) =>
              fail(s"Expecting a failure, got ${nmd.id}")
            case Success(Left(error)) =>
              listener.testEmpty()
              fail(s"Got error ${error}")
            case Failure(ex) =>
              listener.testEmpty()
              ex.toString() mustBe "java.lang.Exception: Failure reading from persistent store!"
          }
        }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "update a hand value in the store" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      val md2 = MatchDuplicate
        .create(MatchDuplicate.id("M2"))
        .fillBoards(standardBoardset, movement)
      val id = md2.id
      persistent.add(md2)

      val boardid = Board.id(1)
      val handid = Team.id(1)
      val dh = md2
        .getBoard(boardid)
        .map(b => b.getHand(handid))
        .getOrElse(throw new Exception("Unable to get board"))
        .getOrElse(throw new Exception("Unable to get hand"))
      dh.updateHand(
        Hand(handid.id, 3, "S", "N", "N", false, false, true, 3, 0, 0)
      )

      try {
        store
          .select(id)
          .nestedResource(DuplicateBoardsNestedResource)
          .select(boardid)
          .nestedResource(DuplicateHandsNestedResource)
          .select(handid)
          .update(dh)
          .test("Updating match duplicate") { tnmd =>
            tnmd match {
              case Success(Right(nmd)) =>
                nmd.id mustBe handid
                listener.testUpdate(new TestUpdateHandId(id, boardid, handid))
//            fail(s"Expecting a failure, got ${nmd.id}")
              case Success(Left(error)) =>
                listener.testEmpty()
                fail(s"Got error ${error}")
              case Failure(ex) =>
                listener.testEmpty()
                fail(s"Got failure ${ex}")
//            ex.toString() mustBe "java.lang.Exception: Failure reading from persistent store!"
            }
          }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a hand value in the store with read failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultRead =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't read"))
      val md2 = MatchDuplicate
        .create(MatchDuplicate.id("M2"))
        .fillBoards(standardBoardset, movement)
      val id = md2.id
      persistent.add(md2)

      val boardid = Board.id(1)
      val handid = Team.id(1)
      val dh = md2
        .getBoard(boardid)
        .map(b => b.getHand(handid))
        .getOrElse(throw new Exception("Unable to get board"))
        .getOrElse(throw new Exception("Unable to get hand"))
      dh.updateHand(
        Hand(handid.id, 3, "S", "N", "N", false, false, true, 3, 0, 0)
      )

      try {
        store
          .select(id)
          .nestedResource(DuplicateBoardsNestedResource)
          .select(boardid)
          .nestedResource(DuplicateHandsNestedResource)
          .select(handid)
          .update(dh)
          .test("Updating match duplicate") { tnmd =>
            tnmd match {
              case Success(Right(nmd)) =>
                fail(s"Expecting a failure, got ${nmd.id}")
              case Success(Left((statuscode, RestMessage(msg)))) =>
                listener.testEmpty()
                statuscode mustBe StatusCodes.InsufficientStorage
                msg mustBe "Oops can't read"
              case Failure(ex) =>
                listener.testEmpty()
                fail(s"Got error ${ex}")
            }
          }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a hand value in the store with read failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failRead = true
      val md2 = MatchDuplicate
        .create(MatchDuplicate.id("M2"))
        .fillBoards(standardBoardset, movement)
      val id = md2.id
      persistent.add(md2)

      val boardid = Board.id(1)
      val handid = Team.id(1)
      val dh = md2
        .getBoard(boardid)
        .map(b => b.getHand(handid))
        .getOrElse(throw new Exception("Unable to get board"))
        .getOrElse(throw new Exception("Unable to get hand"))
      dh.updateHand(
        Hand(handid.id, 3, "S", "N", "N", false, false, true, 3, 0, 0)
      )

      try {
        store
          .select(id)
          .nestedResource(DuplicateBoardsNestedResource)
          .select(boardid)
          .nestedResource(DuplicateHandsNestedResource)
          .select(handid)
          .update(dh)
          .test("Updating match duplicate") { tnmd =>
            tnmd match {
              case Success(Right(nmd)) =>
                fail(s"Expecting a failure, got ${nmd.id}")
              case Success(Left(error)) =>
                listener.testEmpty()
                fail(s"Got error ${error}")
              case Failure(ex) =>
                listener.testEmpty()
                ex.toString() mustBe "java.lang.Exception: Failure reading from persistent store!"
            }
          }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a hand value in the store with write failure" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failResultWrite =
        Some(Result(StatusCodes.InsufficientStorage, "Oops can't write"))
      val md2 = MatchDuplicate
        .create(MatchDuplicate.id("M2"))
        .fillBoards(standardBoardset, movement)
      val id = md2.id
      persistent.add(md2)

      val boardid = Board.id(1)
      val handid = Team.id(1)
      val dh = md2
        .getBoard(boardid)
        .map(b => b.getHand(handid))
        .getOrElse(throw new Exception("Unable to get board"))
        .getOrElse(throw new Exception("Unable to get hand"))
      dh.updateHand(
        Hand(handid.id, 3, "S", "N", "N", false, false, true, 3, 0, 0)
      )

      try {
        store
          .select(id)
          .nestedResource(DuplicateBoardsNestedResource)
          .select(boardid)
          .nestedResource(DuplicateHandsNestedResource)
          .select(handid)
          .update(dh)
          .test("Updating match duplicate") { tnmd =>
            tnmd match {
              case Success(Right(nmd)) =>
                fail(s"Expecting a failure, got ${nmd.id}")
              case Success(Left((statuscode, RestMessage(msg)))) =>
                listener.testEmpty()
                statuscode mustBe StatusCodes.InsufficientStorage
                msg mustBe "Oops can't write"
              case Failure(ex) =>
                listener.testEmpty()
                fail(s"Got failure ${ex}")
            }
          }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

  it should "fail to update a hand value in the store with write failure with exception" in testWithStore {
    (store, persistent, listener, md1) =>
      listener.clear()
      persistent.failWrite = true
      val md2 = MatchDuplicate
        .create(MatchDuplicate.id("M2"))
        .fillBoards(standardBoardset, movement)
      val id = md2.id
      persistent.add(md2)

      val boardid = Board.id(1)
      val handid = Team.id(1)
      val dh = md2
        .getBoard(boardid)
        .map(b => b.getHand(handid))
        .getOrElse(throw new Exception("Unable to get board"))
        .getOrElse(throw new Exception("Unable to get hand"))
      dh.updateHand(
        Hand(handid.id, 3, "S", "N", "N", false, false, true, 3, 0, 0)
      )

      try {
        store
          .select(id)
          .nestedResource(DuplicateBoardsNestedResource)
          .select(boardid)
          .nestedResource(DuplicateHandsNestedResource)
          .select(handid)
          .update(dh)
          .test("Updating match duplicate") { tnmd =>
            tnmd match {
              case Success(Right(nmd)) =>
                fail(s"Expecting a failure, got ${nmd.id}")
              case Success(Left(error)) =>
                listener.testEmpty()
                fail(s"Got error ${error}")
              case Failure(ex) =>
                listener.testEmpty()
                ex.toString() mustBe "java.lang.Exception: Failure writing to persistent store!"
            }
          }
      } catch {
        case x: Exception =>
          testlog.warning("Unexpected exception creating a value", x)
          listener.testEmpty()
          fail("Unexpected exception ${x}")
      }
  }

}
