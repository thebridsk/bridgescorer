package com.github.thebridsk.bridge.server.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import com.github.thebridsk.bridge.server.backend.resource.InMemoryStore
import com.github.thebridsk.bridge.data.Id
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
import com.github.thebridsk.bridge.server.backend.DuplicateTeamsNestedResource
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.backend.resource.JavaResourceStore
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
import com.github.thebridsk.bridge.server.backend.resource.ChangeContextData
import scala.concurrent.duration._
import com.github.thebridsk.bridge.data.Movement
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.server.backend.resource.FileStore
import scala.reflect.io.Directory
import com.github.thebridsk.utilities.file.FileIO
import com.github.thebridsk.bridge.server.backend.resource.MultiStore
import com.github.thebridsk.bridge.server.backend.resource.Store
import com.github.thebridsk.bridge.server.backend.resource.InMemoryPersistent
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.server.backend.resource.StoreSupport
import scala.util.Try
import com.github.thebridsk.bridge.server.backend.resource.ChangeContextData
import com.github.thebridsk.bridge.server.backend.resource.JavaResourcePersistentSupport
import com.github.thebridsk.bridge.server.backend.resource.Implicits._
import com.github.thebridsk.bridge.data.RestMessage
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.server.test.backend.TestFailurePersistent
import com.github.thebridsk.bridge.server.test.backend.TestFailureStore

object TestCacheStoreForFailures {
  import MustMatchers._

  val testlog = com.github.thebridsk.utilities.logging.Logger[TestCacheStoreForFailures]

  TestStartLogging.startLogging()

  val bridgeResources = BridgeResources()
  import bridgeResources._

  val boardsetsPersistent = JavaResourcePersistentSupport[String,BoardSet]("/com/github/thebridsk/bridge/server/backend/", "Boardsets.txt", getClass.getClassLoader)
  val movementsPersistent = JavaResourcePersistentSupport[String,Movement]("/com/github/thebridsk/bridge/server/backend/", "Movements.txt", getClass.getClassLoader)

  def standardBoardset = boardsetsPersistent.read("StandardBoards") match {
    case Right(v) => v
    case Left( error ) =>
      throw new Exception(s"Unable to get standard boardset ${error}")
  }

  def movement = movementsPersistent.read("Mitchell3Table") match {
    case Right(v) => v
    case Left( error ) =>
      throw new Exception(s"Unable to get Mitchell3Table ${error}")
  }

  def getStore: (Store[Id.MatchDuplicate,MatchDuplicate],
                 TestFailurePersistent[Id.MatchDuplicate,MatchDuplicate]) = {
    val s = TestFailureStore[Id.MatchDuplicate,MatchDuplicate]( "test", null, 5, 100, Duration.Inf, Duration.Inf )
    (s,s.testFailurePersistent)
  }

  import ExecutionContext.Implicits.global

  trait Tester {
    def test( name: String, change: Option[ChangeContext] )(implicit pos: Position): Assertion
  }

  trait TesterExists extends Tester {

    final override
    def test( name: String, change: Option[ChangeContext] )(implicit pos: Position): Assertion = {
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

    def testSpecific( change: ChangeContextData )(implicit pos: Position): Assertion
  }

  class TesterEmpty extends Tester {
    def test( name: String, change: Option[ChangeContext] )(implicit pos: Position): Assertion = {
      if (change.isDefined) fail(s"Expecting ${name} change to be empty: ${change}")
      else 0 mustBe 0
    }

  }

  class Listener extends StoreListener {

    var changeCreate: Option[ChangeContext] = None
    var changeUpdate: Option[ChangeContext] = None
    var changeDelete: Option[ChangeContext] = None

    override
    def create( change: ChangeContext ): Unit = { changeCreate = Some(change) }
    override
    def update( change: ChangeContext ): Unit = { changeUpdate = Some(change) }
    override
    def delete( change: ChangeContext ): Unit = { changeDelete = Some(change) }

    def clear() = {
      changeCreate = None
      changeUpdate = None
      changeDelete = None
    }

    def testEmpty()(implicit pos: Position): Assertion = {
      (new TesterEmpty).test("create", changeCreate)
      (new TesterEmpty).test("update", changeUpdate)
      (new TesterEmpty).test("delete", changeDelete)
    }

    def testCreate( tester: Tester )(implicit pos: Position): Assertion = {
      tester.test("create", changeCreate)
      (new TesterEmpty).test("update", changeUpdate)
      (new TesterEmpty).test("delete", changeDelete)
    }

    def testUpdate( tester: Tester )(implicit pos: Position): Assertion = {
      (new TesterEmpty).test("create", changeCreate)
      tester.test("update", changeUpdate)
      (new TesterEmpty).test("delete", changeDelete)
    }

    def testDelete( tester: Tester )(implicit pos: Position): Assertion = {
      (new TesterEmpty).test("create", changeCreate)
      (new TesterEmpty).test("update", changeUpdate)
      tester.test("delete", changeDelete)
    }
  }

  class TestCreateMatchDuplicateId( id: Id.MatchDuplicate) extends TesterExists {
    def testSpecific( change: ChangeContextData )(implicit pos: Position): Assertion = {
      change match {
        case CreateChangeContext(newvalue,field) =>
          newvalue match {
            case md: MatchDuplicate =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${id}"
                case None =>
                  fail( "Expecting a parent field, got None" )
              }
            case x =>
              fail( s"Expecting a MatchDuplicate, got ${x.getClass.getName}" )
          }
        case x =>
          fail( s"Expecting a CreateChangeContext, got ${x.getClass.getName}" )
      }
    }
  }

  class TestUpdateHandId( dupid: Id.MatchDuplicate, bid: Id.DuplicateBoard, id: Id.Team ) extends TesterExists {
    def testSpecific( change: ChangeContextData )(implicit pos: Position): Assertion = {
      change match {
        case UpdateChangeContext(newvalue,field) =>
          newvalue match {
            case md: DuplicateHand =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${dupid}/boards/${bid}/hands/${id}"
                case None =>
                  fail( "Expecting a parent field, got None" )
              }
            case x =>
              fail( s"Expecting a MatchDuplicate, got ${x.getClass.getName}" )
          }
        case x =>
          fail( s"Expecting a CreateChangeContext, got ${x.getClass.getName}" )
      }
    }
  }

  class TestUpdateMatchDuplicateId( id: Id.MatchDuplicate) extends TesterExists {
    def testSpecific( change: ChangeContextData )(implicit pos: Position): Assertion = {
      change match {
        case UpdateChangeContext(newvalue,field) =>
          newvalue match {
            case md: MatchDuplicate =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${id}"
                case None =>
                  fail( "Expecting a parent field, got None" )
              }
            case x =>
              fail( s"Expecting a MatchDuplicate, got ${x.getClass.getName}" )
          }
        case x =>
          fail( s"Expecting a CreateChangeContext, got ${x.getClass.getName}" )
      }
    }
  }

  class TestDeleteMatchDuplicateId( id: Id.MatchDuplicate) extends TesterExists {
    def testSpecific( change: ChangeContextData )(implicit pos: Position): Assertion = {
      change match {
        case DeleteChangeContext(oldvalue,field) =>
          oldvalue match {
            case md: MatchDuplicate =>
              md.id mustBe id
              field match {
                case Some(f) =>
                  f mustBe s"/duplicates/${id}"
                case None =>
                  fail( "Expecting a parent field, got None" )
              }
            case x =>
              fail( s"Expecting a MatchDuplicate, got ${x.getClass.getName}" )
          }
        case x =>
          fail( s"Expecting a CreateChangeContext, got ${x.getClass.getName}" )
      }
    }
  }

  def testWithStore( fun: (Store[Id.MatchDuplicate,MatchDuplicate],
                           TestFailurePersistent[Id.MatchDuplicate,MatchDuplicate],
                           Listener,
                           MatchDuplicate
                          )=>Future[Assertion]
                   ) = {
    val (store,per) = getStore
    val md = TestMatchDuplicate.create("?")

    val listener = new Listener
    store.addListener(listener)

    val fut = store.createChild(md).test("Creating match duplicate") { tryresult =>
      tryresult match {
        case Success(Right(nmd)) =>
          Thread.sleep(1000)
          fun(store,per,listener,nmd)
        case Success(Left(e)) =>
          fail("fail")
        case Failure(ex) =>
          fail(s"Failed with exception ${ex}")
      }
    }
    fut.onComplete( t => store.removeListener(listener) )
    fut
  }

  implicit class WrapFuture[T]( val f: Future[Result[T]] ) extends AnyVal {

    def test( comment: String )(block: Try[Result[T]]=>Future[Assertion])( implicit pos: SourcePosition): Future[Assertion] = {
      f.transformWith(block)
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
class TestCacheStoreForFailures extends AsyncFlatSpec with ScalatestRouteTest with MustMatchers {
  import TestCacheStoreForFailures._

  import ExecutionContext.Implicits.global

  behavior of "Store with Failures"

  it should "store a value in the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    val md = TestMatchDuplicate.create("?")
    store.createChild(md).test("Creating match duplicate") { tnmd =>
      tnmd match {
        case Success(Right(nmd)) =>
          nmd.equalsIgnoreModifyTime( md.copy(id=nmd.id), true) mustBe true
          listener.testCreate( new TestCreateMatchDuplicateId( nmd.id ))
        case Success(Left(error)) =>
          fail(s"Got error ${error}")
        case Failure(ex) =>
          fail(s"Got failure ${ex}")
      }
    }
  }

  it should "read a value from the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    val md = MatchDuplicate.create("M2")
    val id = md.id
    persistent.add(md)
    try {
      store.read(id).test("Reading match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            nmd.id mustBe id
            listener.testEmpty()
            store.getCached(id) must not be None
          case Success(Left(error)) =>
            fail(s"Got error ${error}")
          case Failure(ex) =>
            fail(s"Got failure ${ex}")
        }
      }
    } catch {
      case x: Exception =>
        testlog.warning("Unexpected exception reading from store",x)
        fail(s"Unexpected exception reading from store ${x}")
    }
  }

  it should "fail to read a value from the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultRead = Some( Result( StatusCodes.InsufficientStorage, "Oops can't read" ) )
    val md = MatchDuplicate.create("M2")
    val id = md.id
    persistent.add(md)
    try {
      store.read(id).test("Reading match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail("Expecting a failure")
          case Success(Left((statuscode,RestMessage(msg)))) =>
            listener.testEmpty()
            statuscode mustBe StatusCodes.InsufficientStorage
            msg mustBe "Oops can't read"
            Thread.sleep(1000L)
            store.getCached(id) mustBe None
          case Failure(ex) =>
            listener.testEmpty()
            fail(s"Got error ${ex}")
        }
      }
    } catch {
      case x: Exception =>
        testlog.warning("Unexpected exception reading from store",x)
        fail(s"Unexpected exception reading from store ${x}")
    }
  }

  it should "fail to read a value from the store, next read works" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultRead = Some( Result( StatusCodes.InsufficientStorage, "Oops can't read" ) )
    val md = MatchDuplicate.create("M2")
    val id = md.id
    persistent.add(md)
    try {
      store.read(id).test("Reading match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail("Expecting a failure")
          case Success(Left((statuscode,RestMessage(msg)))) =>
            listener.testEmpty()
            statuscode mustBe StatusCodes.InsufficientStorage
            msg mustBe "Oops can't read"

            persistent.failResultRead = None

            store.read(id).test("Reading match duplicate again") { tnmd =>
              tnmd match {
                case Success(Right(nmd)) =>
                  nmd.id mustBe id
                case Success(Left(error)) =>
                  listener.testEmpty()

                  withClue(s"Clearing error for ${id}") {
                    store.clearError(id) mustBe false
                  }

                  store.read(id).test("Reading match duplicate again") { tnmd =>
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
                  listener.testEmpty()
                  fail(s"Got failure ${ex}")
              }
            }
          case Failure(ex) =>
            listener.testEmpty()
            fail(s"Got error ${ex}")
        }
      }
    } catch {
      case x: Exception =>
        testlog.warning("Unexpected exception reading from store",x)
        fail(s"Unexpected exception reading from store ${x}")
    }
  }

  it should "fail to read a value from the store with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failRead = true
    val md = MatchDuplicate.create("M2")
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
        testlog.warning("Unexpected exception reading from store",x)
        fail(s"Unexpected exception reading from store ${x}")
    }
  }

  it should "fail to read a value from the store with exception, next read works" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failRead = true
    val md = MatchDuplicate.create("M2")
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
        testlog.warning("Unexpected exception reading from store",x)
        fail(s"Unexpected exception reading from store ${x}")
    }
  }

  it should "create a value in the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    val md = TestMatchDuplicate.create("?")
    try {
      store.createChild(md).test("Creating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            nmd.equalsIgnoreModifyTime( md.copy(id=nmd.id), true) mustBe true
            listener.testCreate( new TestCreateMatchDuplicateId( nmd.id ))
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

  it should "fail to create a value in the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultWrite = Some( Result( StatusCodes.InsufficientStorage, "Oops can't write" ) )
    val md = TestMatchDuplicate.create("?")
    try {
      store.createChild(md).test("Creating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
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

  it should "fail to create a value in the store with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failWrite = true
    val md = TestMatchDuplicate.create("?")
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

  it should "update a value in the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    val md = md1.updateTeam(Team("T1","Fred","George",0,0))
    try {
      store.update(md.id,md).test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            nmd.equalsIgnoreModifyTime( md.copy(id=nmd.id), true) mustBe true
            listener.testUpdate( new TestUpdateMatchDuplicateId( nmd.id ) )
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

  it should "fail to update a value in the store with write failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultWrite = Some( Result( StatusCodes.InsufficientStorage, "Oops can't write" ) )
    val md = md1.updateTeam(Team("T1","Fred","George",0,0))
    try {
      store.update(md.id,md).test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
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

  it should "fail to update a value in the store with write failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failWrite = true
    val md = md1.updateTeam(Team("T1","Fred","George",0,0))
    try {
      store.update(md.id,md).test("Updating match duplicate") { tnmd =>
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

  it should "fail to update a value in the store with read failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultRead = Some( Result( StatusCodes.InsufficientStorage, "Oops can't read" ) )
    val md2 = MatchDuplicate.create("M2")
    val id = md2.id
    persistent.add(md2)
    val md = md2.updateTeam(Team("T1","Fred","George",0,0))
    try {
      store.update(md.id,md).test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
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

  it should "fail to update a value in the store with read failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failRead = true
    val md2 = MatchDuplicate.create("M2")
    val id = md2.id
    persistent.add(md2)
    val md = md2.updateTeam(Team("T1","Fred","George",0,0))
    try {
      store.update(md.id,md).test("Updating match duplicate") { tnmd =>
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

  it should "delete a value from the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    try {
      store.delete(md1.id).test("Deleting match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            nmd.id mustBe md1.id
            listener.testDelete( new TestDeleteMatchDuplicateId( nmd.id ) )
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

  it should "fail to delete a value from the store with delete failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultDelete = Some( Result( StatusCodes.InsufficientStorage, "Oops can't delete" ) )
    try {
      store.delete(md1.id).test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
            listener.testEmpty()
            statuscode mustBe StatusCodes.InsufficientStorage
            msg mustBe "Oops can't delete"
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

  it should "fail to delete a value from the store using select with delete failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultDelete = Some( Result( StatusCodes.InsufficientStorage, "Oops can't delete" ) )
    try {
      store.select(md1.id).delete().test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
            listener.testEmpty()
            statuscode mustBe StatusCodes.InsufficientStorage
            msg mustBe "Oops can't delete"
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

  it should "fail to delete a value from the store with delete failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failDelete = true
    val md2 = MatchDuplicate.create("M2")
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

  it should "fail to delete a value from the store using select with delete failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failDelete = true
    val md2 = MatchDuplicate.create("M2")
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

  it should "fail to delete a value from the store with read failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultRead = Some( Result( StatusCodes.InsufficientStorage, "Oops can't read" ) )
    val md2 = MatchDuplicate.create("M2")
    val id = md2.id
    persistent.add(md2)
    try {
      store.getCached(id) mustBe 'empty
      store.delete(md2.id).test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
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

  it should "fail to delete a value from the store using select with read failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultRead = Some( Result( StatusCodes.InsufficientStorage, "Oops can't read" ) )
    val md2 = MatchDuplicate.create("M2")
    val id = md2.id
    persistent.add(md2)
    try {
      store.select(md2.id).delete().test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
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

  it should "fail to delete a value from the store with read failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failRead = true
    val md2 = MatchDuplicate.create("M2")
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

  it should "fail to delete a value from the store using select with read failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failRead = true
    val md2 = MatchDuplicate.create("M2")
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

  it should "update a hand value in the store" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    val md2 = MatchDuplicate.create("M2").fillBoards(standardBoardset, movement)
    val id = md2.id
    persistent.add(md2)

    val boardid = "B1"
    val handid = "T1"
    val dh = md2.getBoard(boardid).map( b => b.getHand(handid) ).getOrElse( throw new Exception("Unable to get board")).getOrElse(throw new Exception("Unable to get hand"))
    dh.updateHand(Hand(handid,3,"S","N","N",false,false,true,3,0,0))

    try {
      store.select(id).
            nestedResource(DuplicateBoardsNestedResource).
            select(boardid).
            nestedResource(DuplicateHandsNestedResource).
            select(handid).
            update(dh).
            test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            nmd.id mustBe handid
            listener.testUpdate( new TestUpdateHandId(id,boardid,handid ) )
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

  it should "fail to update a hand value in the store with read failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultRead = Some( Result( StatusCodes.InsufficientStorage, "Oops can't read" ) )
    val md2 = MatchDuplicate.create("M2").fillBoards(standardBoardset, movement)
    val id = md2.id
    persistent.add(md2)

    val boardid = "B1"
    val handid = "T1"
    val dh = md2.getBoard(boardid).map( b => b.getHand(handid) ).getOrElse( throw new Exception("Unable to get board")).getOrElse(throw new Exception("Unable to get hand"))
    dh.updateHand(Hand(handid,3,"S","N","N",false,false,true,3,0,0))

    try {
      store.select(id).
            nestedResource(DuplicateBoardsNestedResource).
            select(boardid).
            nestedResource(DuplicateHandsNestedResource).
            select(handid).
            update(dh).
            test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
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

  it should "fail to update a hand value in the store with read failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failRead = true
    val md2 = MatchDuplicate.create("M2").fillBoards(standardBoardset, movement)
    val id = md2.id
    persistent.add(md2)

    val boardid = "B1"
    val handid = "T1"
    val dh = md2.getBoard(boardid).map( b => b.getHand(handid) ).getOrElse( throw new Exception("Unable to get board")).getOrElse(throw new Exception("Unable to get hand"))
    dh.updateHand(Hand(handid,3,"S","N","N",false,false,true,3,0,0))

    try {
      store.select(id).
            nestedResource(DuplicateBoardsNestedResource).
            select(boardid).
            nestedResource(DuplicateHandsNestedResource).
            select(handid).
            update(dh).
            test("Updating match duplicate") { tnmd =>
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

  it should "fail to update a hand value in the store with write failure" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failResultWrite = Some( Result( StatusCodes.InsufficientStorage, "Oops can't write" ) )
    val md2 = MatchDuplicate.create("M2").fillBoards(standardBoardset, movement)
    val id = md2.id
    persistent.add(md2)

    val boardid = "B1"
    val handid = "T1"
    val dh = md2.getBoard(boardid).map( b => b.getHand(handid) ).getOrElse( throw new Exception("Unable to get board")).getOrElse(throw new Exception("Unable to get hand"))
    dh.updateHand(Hand(handid,3,"S","N","N",false,false,true,3,0,0))

    try {
      store.select(id).
            nestedResource(DuplicateBoardsNestedResource).
            select(boardid).
            nestedResource(DuplicateHandsNestedResource).
            select(handid).
            update(dh).
            test("Updating match duplicate") { tnmd =>
        tnmd match {
          case Success(Right(nmd)) =>
            fail(s"Expecting a failure, got ${nmd.id}")
          case Success(Left((statuscode,RestMessage(msg)))) =>
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

  it should "fail to update a hand value in the store with write failure with exception" in testWithStore { (store,persistent,listener,md1) =>
    listener.clear()
    persistent.failWrite = true
    val md2 = MatchDuplicate.create("M2").fillBoards(standardBoardset, movement)
    val id = md2.id
    persistent.add(md2)

    val boardid = "B1"
    val handid = "T1"
    val dh = md2.getBoard(boardid).map( b => b.getHand(handid) ).getOrElse( throw new Exception("Unable to get board")).getOrElse(throw new Exception("Unable to get hand"))
    dh.updateHand(Hand(handid,3,"S","N","N",false,false,true,3,0,0))

    try {
      store.select(id).
            nestedResource(DuplicateBoardsNestedResource).
            select(boardid).
            nestedResource(DuplicateHandsNestedResource).
            select(handid).
            update(dh).
            test("Updating match duplicate") { tnmd =>
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
