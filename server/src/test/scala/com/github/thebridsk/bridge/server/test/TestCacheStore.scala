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
import scala.concurrent.Await
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
import scala.concurrent.ExecutionContext.Implicits.global

object TestCacheStore {
  import MustMatchers._

  val testlog = com.github.thebridsk.utilities.logging.Logger[TestCacheStore]

  TestStartLogging.startLogging()

  val bridgeResources = BridgeResources()
  import bridgeResources._

  def getStore: Store[Id.MatchDuplicate,MatchDuplicate] =
    InMemoryStore("test")

  def getMatchDuplicateFileStore( dir: Directory ): Store[Id.MatchDuplicate,MatchDuplicate] =
    FileStore("test",dir)

  def getBoardSetStore: Store[String,BoardSet] = {
    JavaResourceStore("test","/com/github/thebridsk/bridge/server/backend/", "Boardsets.txt", getClass.getClassLoader)
  }

  def getMovementStore: Store[String,Movement] = {
    JavaResourceStore("test","/com/github/thebridsk/bridge/server/backend/", "Movements.txt", getClass.getClassLoader)
  }

  def getBoardSetFileStore( dir: Directory ): Store[String,BoardSet] = {
    FileStore("test",dir)
  }

  def getBoardSetMultiStore( dir: Directory ): Store[String,BoardSet] = {
    MultiStore.createFileAndResource("test",dir, "/com/github/thebridsk/bridge/server/backend/", "Boardsets.txt", getClass.getClassLoader)
  }

  import ExecutionContext.Implicits.global

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
  }


  def testWithStore( fun: (Store[Id.MatchDuplicate,MatchDuplicate], Listener)=>Future[Assertion] ) = {
    val store = getStore
    val md = TestMatchDuplicate.create("?")

    val listener = new Listener
    store.addListener(listener)

    val fut = store.createChild(md).resultFailed("Creating match duplicate").flatMap { result =>
      result match {
        case Right(nmd) =>
          fun(store,listener)
        case Left(e) =>
          fail("fail")
      }
    }
    fut.onComplete( t => store.removeListener(listener) )
    fut
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
class TestCacheStore extends AsyncFlatSpec with ScalatestRouteTest with MustMatchers with BeforeAndAfterAll {
  import TestCacheStore._

  import ExecutionContext.Implicits.global

  var tempDir: Directory = null

  override def beforeAll() = {
    tempDir = Directory.makeTemp( "TestFileStore", ".teststore" )
    testlog.fine( "Using temporary directory "+tempDir)
  }

  override def afterAll() = {
    testlog.fine( "Deleting temporary directory "+tempDir)
    tempDir.deleteRecursively()
  }

  behavior of "Store"

  it should "store a value in the store" in {
    val store = getStore

    val md = TestMatchDuplicate.create("?")

    val listener = new Listener
    store.addListener(listener)

    val fut =
    store.createChild(md).test("Creating match duplicate") { nmd =>
      nmd.equalsIgnoreModifyTime( md.copy(id=nmd.id), true) mustBe true

      listener.changeCreate mustBe 'defined
      listener.changeUpdate mustBe 'empty
      listener.changeDelete mustBe 'empty

      listener.changeCreate.map { cc =>
        cc.changes.length mustBe 1
        cc.changes.head match {
          case CreateChangeContext(newvalue,parentfield) =>
            parentfield mustBe Some("/duplicates/M1")
            newvalue match {
              case md: MatchDuplicate =>
                md.id mustBe nmd.id
              case x =>
                fail(s"expecting MatchDuplicate, got ${x.getClass.getName}")
            }
          case UpdateChangeContext(newvalue,parentfield) =>
            fail("expecting create, got update")
          case DeleteChangeContext(oldvalue,parentfield) =>
            fail("expecting create, got delete")
        }
      }.getOrElse(fail("changeCreate was empty"))
    }
    fut.onComplete( t => store.removeListener(listener))
    fut
  }

  it should "change team 1" in testWithStore { (store,listener) =>

    val team = store.select("M1").nestedResource(DuplicateTeamsNestedResource).select("T1").read()
    team.testfuture("Testing team T1 in match M1") { ot =>
      ot.id mustBe "T1"
      ot.player1 mustBe "Nancy"
      ot.player2 mustBe "Norman"

      listener.clear()

      val updatedTeam = store.select("M1").nestedResource(DuplicateTeamsNestedResource).select("T1").update(Team("T1","Fred","George",0,0))
      updatedTeam.testfuture("Testing updated team T1 in match M1") { t =>
        t.id mustBe "T1"
        t.player1 mustBe "Fred"
        t.player2 mustBe "George"

        listener.changeCreate mustBe 'empty
        listener.changeUpdate mustBe 'defined
        listener.changeDelete mustBe 'empty

        listener.changeUpdate.map { cc =>
          cc.changes.length mustBe 2
          cc.changes.head match {
            case UpdateChangeContext(newvalue,parentfield) =>
              parentfield mustBe Some("/duplicates/M1")
              newvalue match {
                case md: MatchDuplicate =>
                  md.getTeam("T1") match {
                    case Some(tt) =>
                      tt.id mustBe t.id
                      tt.player1 mustBe t.player1
                      tt.player2 mustBe t.player2
                    case None =>
                      fail(s"expecting to find team T1 in MatchDuplicate")
                  }
                case x =>
                  fail(s"expecting MatchDuplicate, got ${x.getClass.getName}")
              }
            case x =>
              fail("expecting update, got ${x}")
          }
          cc.getSpecificChange() match {
            case Some(UpdateChangeContext(newvalue,parentfield)) =>
              parentfield mustBe Some("/duplicates/M1/teams/T1")
              newvalue match {
                case tt: Team =>
                  tt.id mustBe t.id
                  tt.player1 mustBe t.player1
                  tt.player2 mustBe t.player2
                case x =>
                  fail(s"expecting MatchDuplicate, got ${x.getClass.getName}")
              }
            case x =>
              fail("expecting update, got ${x}")
          }
        }.getOrElse(fail("changeUpdate was empty"))

        val againteam = store.select("M1").nestedResource(DuplicateTeamsNestedResource).select("T1").read()
        againteam.test("Testing team T1 in match M1") { t =>
          t.id mustBe "T1"
          t.player1 mustBe "Fred"
          t.player2 mustBe "George"
        }
      }
    }
  }

  it should "delete team 1" in testWithStore { (store,listener) =>

    listener.clear()

    val team = store.select("M1").nestedResource(DuplicateTeamsNestedResource).select("T1").delete()
    team.testfuture("Testing team T1 in match M1") { ot =>
      ot.id mustBe "T1"
      ot.player1 mustBe "Nancy"
      ot.player2 mustBe "Norman"

      listener.changeCreate mustBe 'empty
      listener.changeUpdate mustBe 'empty
      listener.changeDelete mustBe 'defined

      listener.changeDelete.map { cc =>
        cc.changes.length mustBe 2
        cc.changes.head match {
          case UpdateChangeContext(newvalue,parentfield) =>
            parentfield mustBe Some("/duplicates/M1")
            newvalue match {
              case md: MatchDuplicate =>
                md.getTeam("T1") mustBe 'empty
              case x =>
                fail(s"expecting MatchDuplicate, got ${x.getClass.getName}")
            }
          case x =>
            fail("expecting update, got ${x}")
        }
        cc.getSpecificChange() match {
          case Some(DeleteChangeContext(oldvalue,parentfield)) =>
            parentfield mustBe Some("/duplicates/M1/teams/T1")
            oldvalue match {
              case tt: Team =>
                tt.id mustBe ot.id
                tt.player1 mustBe ot.player1
                tt.player2 mustBe ot.player2
              case x =>
                fail(s"expecting Team, got ${x.getClass.getName}")
            }
          case x =>
            fail("expecting delete, got ${x}")
        }
      }.getOrElse(fail("changeDelete was empty"))
    }
  }

  it should "change hand T1 on board B1" in testWithStore { (store,listener) =>

    val handf = store.select("M1").nestedResource(DuplicateBoardsNestedResource).select("B1").nestedResource(DuplicateHandsNestedResource).select("T1").read()
    handf.testfuture("Testing board B1 hand T1 in match M1") { dh =>
      dh.id mustBe "T1"
      dh.board mustBe "B1"
      dh.nsTeam mustBe "T1"
      dh.ewTeam mustBe "T2"

      listener.clear()

      val nh = dh.updateHand( Hand("T1", 1, "S", "N", "N", false, false, true, 2, 0, 0) )

      val uphandf = store.select("M1").nestedResource(DuplicateBoardsNestedResource).select("B1").nestedResource(DuplicateHandsNestedResource).select("T1").update(nh)
      uphandf.testfuture("Testing updating board B1 hand T1 in match M1") { udh =>
        udh.id mustBe "T1"
        udh.hand.get.equalsIgnoreModifyTime(nh.hand.get) mustBe true

        listener.changeCreate mustBe 'empty
        listener.changeUpdate mustBe 'defined
        listener.changeDelete mustBe 'empty

        listener.changeUpdate.map { cc =>
          cc.changes.length mustBe 3
          cc.changes.head match {
            case UpdateChangeContext(newvalue,parentfield) =>
              parentfield mustBe Some("/duplicates/M1")
              newvalue match {
                case md: MatchDuplicate =>
                  md.getBoard("B1") match {
                    case Some(tt) =>
                      tt.id mustBe "B1"
                      tt.getHand("T1") match {
                        case Some(th) =>
                          th.id mustBe "T1"
                          th.hand.get.equalsIgnoreModifyTime(nh.hand.get) mustBe true
                        case None =>
                          fail(s"expecting to find hand T1 in Board B1 in MatchDuplicate")
                      }
                    case None =>
                      fail(s"expecting to find Board B1 in MatchDuplicate")
                  }
                case x =>
                  fail(s"expecting MatchDuplicate, got ${x.getClass.getName}")
              }
            case x =>
              fail("expecting update, got ${x}")
          }
          cc.changes.tail.head match {
            case UpdateChangeContext(newvalue,parentfield) =>
              parentfield mustBe Some("/duplicates/M1/boards/B1")
              newvalue match {
                case md: Board =>
                  md.getHand("T1") match {
                    case Some(tt) =>
                      tt.id mustBe "T1"
                      tt.hand.get.equalsIgnoreModifyTime(nh.hand.get) mustBe true
                    case None =>
                      fail(s"expecting to find Hand T1 in Board B1 in MatchDuplicate")
                  }
                case x =>
                  fail(s"expecting Board, got ${x.getClass.getName}")
              }
            case x =>
              fail("expecting update, got ${x}")
          }
          cc.getSpecificChange() match {
            case Some(UpdateChangeContext(newvalue,parentfield)) =>
              parentfield mustBe Some("/duplicates/M1/boards/B1/hands/T1")
              newvalue match {
                case tt: DuplicateHand =>
                  tt.id mustBe nh.id
                  tt.hand.get.equalsIgnoreModifyTime(nh.hand.get) mustBe true
                case x =>
                  fail(s"expecting MatchDuplicate, got ${x.getClass.getName}")
              }
            case x =>
              fail("expecting update, got ${x}")
          }
        }.getOrElse(fail("changeUpdate was empty"))

      }
    }
  }

  it should "delete match M1" in testWithStore { (store,listener) =>
    testlog.fine("Starting delete match M1")
    listener.clear()
    store.select("M1").delete().flatMap { rmd =>
      rmd match {
        case Left((statuscode,msg)) =>
          fail(s"did not find match M1: ${statuscode} ${msg}")
        case Right(md) =>
          listener.changeCreate mustBe 'empty
          listener.changeUpdate mustBe 'empty
          listener.changeDelete mustBe 'defined

          listener.changeDelete.map { cc =>
            cc.changes.length mustBe 1
            cc.getSpecificChange() match {
              case Some(DeleteChangeContext(oldvalue,parentfield)) =>
                parentfield mustBe Some("/duplicates/M1")
                oldvalue match {
                  case nmd: MatchDuplicate =>
                    nmd.id mustBe md.id
                  case x =>
                    fail(s"expecting MatchDuplicate, got ${x.getClass.getName}")
                }
              case x =>
                fail("expecting update, got ${x}")
            }
          }.getOrElse(fail("changeDelete was empty"))
          store.select("M1").read().map { rfail =>
            rfail match {
              case Left((statuscode,msg)) =>
                statuscode mustBe StatusCodes.NotFound
              case Right(xxmd) =>
                fail(s"expecting match M1 to not exist: ${xxmd}")
            }
          }
      }
    }
  }

  def addHand(
               store: Store[Id.MatchDuplicate, MatchDuplicate],
               dupid: String,
               boardid: String,
               handid: String,
               results: CollectResult
             ): (String,Future[Result[(String,DuplicateHand)]]) = {
    val x =
    store.select(dupid).
             nestedResource(DuplicateBoardsNestedResource).select(boardid).
             nestedResource(DuplicateHandsNestedResource).select(handid).
             read().flatMap { rdh =>
      testlog.info(s"addHand($dupid-${boardid}-${handid}) starting to add hand")
      rdh match {
        case Right(dh) =>
          val ndh = dh.updateHand( Hand("handid", 1, "S", "N", "N", false, false, true, 2, 0, 0) )
          val context = ChangeContext()
          val fut = store.select(dupid).
                   nestedResource(DuplicateBoardsNestedResource).select(boardid).
                   nestedResource(DuplicateHandsNestedResource).select(handid).
                   update(ndh,context)
          fut.map { rndh =>
            rndh match {
              case Right(cdh) =>
                context.changes.head match {
                  case UpdateChangeContext(md: MatchDuplicate,field) =>
                    results.add(md)
                    val played = md.boards.flatMap { board =>
                      board.hands.filter( h => h.played.length == 1).map( h => s"$dupid-${board.id}-${h.id}" )
                    }.sorted
                    testlog.info(s"""addHand($dupid-${boardid}-${handid}) added, already played(${played.length}) = ${played.mkString(",")}""")
                  case x =>
                    results.add(s"addHand($dupid-${boardid}-${handid}) did not get correct context data: $x")
                    testlog.info(s"addHand($dupid-${boardid}-${handid}) did not get correct context data: $x")
                }
              case Left(error) =>
                results.add(s"addHand($dupid-${boardid}-${handid}) failed ${error}")
                testlog.info(s"addHand($dupid-${boardid}-${handid}) failed ${error}")
            }
            rndh
          }.map( r => r.map( dh => (s"$dupid-${boardid}-${handid}",dh)) )
        case Left(error) =>
          Result.future(error)
      }
    }
    (s"$dupid-${boardid}-${handid}",x)
  }

  def getBoardset() = {
    val fut = getBoardSetStore.select("StandardBoards").read()
    Await.result(fut, 10.seconds) match {
      case Right(bs) => bs
      case Left(error) =>
        error._1 mustBe StatusCodes.OK
        throw new Exception
    }
  }

  def getMovement() = {
    val fut = getMovementStore.select("Mitchell3Table").read()
    Await.result(fut, 10.seconds) match {
      case Right(bs) => bs
      case Left(error) =>
        error._1 mustBe StatusCodes.OK
        throw new Exception
    }
  }

  def processFutures( futures: Map[String,Future[Result[(String,DuplicateHand)]]] ): Future[List[String]] = {
    val futs = futures.toList.map { e =>
      val (id,fut) = e
      fut.map { r =>
        (id,r)
      }
    }
    Future.foldLeft(
         futs
       )(
         List[String]()
       ) { (ac,v) =>
         val (id,r) = v
         r match {
           case Right(dh) =>
             id::ac
           case Left(error) =>
             testlog.warning(s"""Error updating ${id}: ${error}""")
             ac
         }
       }
  }

  class CollectResult( id: Id.MatchDuplicate ) {
    private var ferrors: List[String] = List()
    private var fmatches: List[MatchDuplicate] = List()

    def add( err: String ) = synchronized { ferrors = err::ferrors }
    def add( md: MatchDuplicate ) = synchronized {
      if (md.id != id) {
        testlog.severe(s"Ids don't match: ${id}, got ${md.id}")
      }
      fmatches = md::fmatches
    }

    def errors = ferrors
    def matches = fmatches
  }

  def playAllHands( store: Store[Id.MatchDuplicate,MatchDuplicate],
                    bs: BoardSet,
                    mov: Movement
                  ): Future[(Assertion,Option[Id.MatchDuplicate])] = {

    val md = MatchDuplicate.create().fillBoards(bs, mov)

    store.createChild(md).flatMap { rmd =>
      rmd match {
        case Right(nmd) =>
          val dupid = nmd.id

          testlog.info(s"New duplicate id is ${dupid}")

          val results = new CollectResult(dupid)

          val futures = nmd.boards.flatMap { board =>
            board.hands.map { hand =>
              addHand( store, dupid, board.id, hand.id, results )
            }
          }.toMap
          val a = processFutures(futures).flatMap { list =>
            list.length mustBe 18*3

            results.errors mustBe 'empty
            results.matches.length mustBe 18*3
            val sorted = results.matches.map( md => md.numberPlayedHands).sortWith((l,r)=>l<r)
            withClue(s"${dupid} Sorted number of hands played ${sorted}") {
              sorted.zipWithIndex.foreach { e =>
                val (md,i) = e
                if (md != i+1) {
                  testlog.severe(s"${dupid} Sorted number of hands played ${sorted}")
                }
                md mustBe (i+1)
              }
            }

            store.select(dupid).read().test("Checking result") { rmd =>
              val handsPlayed = rmd.boards.map { board =>
                board.hands.filter( dh => dh.hand.isDefined).length
              }.foldLeft(0)( (ac,v) => ac+v )
              handsPlayed mustBe 18*3
            }
          }
          a.map( r => (r,Option(dupid)) )
        case Left(error) =>
          testlog.warning(s"Unable to create a Mitchell3Table match: ${error}")
          fail(s"Unable to create a Mitchell3Table match: ${error}")
      }
    }

  }

  it should "allow all hands on a match to be played at the same time" in testWithStore { (store,listener) =>

    val bs = getBoardset()
    val mov = getMovement()
    playAllHands(store,bs,mov).map( r => r._1 )
  }

  it should "allow all hands on n matches to be played at the same time" in testWithStore { (store,listener) =>

    val bs = getBoardset()
    val mov = getMovement()
    val n = 20
    val futures = for ( i <- 1 to n ) yield {
      playAllHands(store,bs,mov)
    }
    val asserts = Future.foldLeft(futures)((List[Option[Id.MatchDuplicate]](),List[(Assertion,Option[Id.MatchDuplicate])]())) { (ac,v) =>
      val (assert,id) = v
      assert match {
        case Succeeded =>
          testlog.info(s"${id} -> Succeeded")
          (id::ac._1, ac._2)
        case _ =>
          testlog.info(s"${id} -> ${assert}")
          val n2 = ((assert,id))::ac._2
          ( id::ac._1, n2 )
      }
    }

    asserts.map { errors =>
      errors._2.length mustBe 0
      errors._1.length mustBe n

      errors._1.map( oid => oid.get).distinct.length mustBe n
    }

  }

  it should "find 2 boardset" in {
    val store = getBoardSetStore

    store.readAll().test("Reading all in boardset store") { map =>
      map.size mustBe 2
      map.keySet must contain theSameElementsAs "StandardBoards"::"ArmonkBoards"::Nil
    }
  }

  it should "find standard boardset, and update should get a bad request" in {
    val store = getBoardSetStore

    store.select("StandardBoards").read().testfuture("Reading standard boardset") { bs =>
      bs.name mustBe "StandardBoards"

      store.select("StandardBoards").update(bs).expectFail( "Updating to read only store should fail", StatusCodes.BadRequest)
    }
  }

  it should "find standard boardset, and delete should a bad request" in {
    val store = getBoardSetStore

    store.select("StandardBoards").read().testfuture("Reading standard boardset") { bs =>
      bs.name mustBe "StandardBoards"

      store.select("StandardBoards").delete().expectFail( "deleting from a read only store should fail", StatusCodes.BadRequest)
    }
  }

  it should "write a boardset to a file store" in {
    val store = getBoardSetStore

    store.select("StandardBoards").read().testfuture("Reading standard boardset") { bs =>
      bs.name mustBe "StandardBoards"

      val fstore = getBoardSetFileStore(tempDir)

      val changeContext = ChangeContext()

      fstore.createChild(bs, changeContext).test("Writing standard boardset to a filestore") { fbs =>
        fbs.copy(creationTime = None, updateTime = None) mustBe bs.copy(creationTime = None, updateTime = None)

        tempDir.list.foreach( p => testlog.fine(s"Found in tempdir: $p") )

        val f = tempDir/"Boardset.StandardBoards.yaml"
        val v = FileIO.readFileSafe(f.toString())
        val (goodOnDisk,vt) = fstore.support.fromJSON(v)
        vt.copy(creationTime = None, updateTime = None) mustBe fbs.copy(creationTime = None, updateTime = None)
      }

    }
  }

  it should "fail to create a boardset with same name to a multi store" in {
    val store = getBoardSetMultiStore(tempDir)

    store.select("StandardBoards").read().testfuture("Reading standard boardset") { bs =>
      bs.name mustBe "StandardBoards"

      val changeContext = ChangeContext()

      store.createChild(bs, changeContext).
            expectFail("Writing standard boardset to a multistore",StatusCodes.BadRequest)
    }
  }

  it should "write a boardset to a multi store" in {
    val store = getBoardSetMultiStore(tempDir)

    store.select("StandardBoards").read().testfuture("Reading standard boardset") { bs =>
      bs.name mustBe "StandardBoards"

      val changeContext = ChangeContext()

      store.select(bs.id).update(bs, changeContext).test("Writing standard boardset to a multistore") { fbs =>
        fbs mustBe bs

        tempDir.list.foreach( p => testlog.fine(s"Found in tempdir: $p") )

        val f = tempDir/"Boardset.StandardBoards.yaml"
        val v = FileIO.readFileSafe(f.toString())
        val (goodOnDisk,vt) = store.support.fromJSON(v)
        vt mustBe fbs
      }

    }
  }

  def playOnFileStore(
                       store: Store[Id.MatchDuplicate, MatchDuplicate],
                       tempDir: Directory,
                       bs: BoardSet,
                       mov: Movement
                    ): Future[(Assertion, Option[Id.MatchDuplicate])] = {
    playAllHands(store,bs,mov).flatMap { aid =>
      val (a,Some(id)) = aid
      tempDir.list.foreach( p => testlog.fine(s"After playing(${id}), Found in tempdir: $p") )
      val f = tempDir/s"MatchDuplicate.${id}.yaml"
      val v = FileIO.readFileSafe(f.toString())
      val (goodOnDisk,vt) = store.support.fromJSON(v)

      val handsPlayed = vt.boards.map { board =>
        board.hands.filter( dh => dh.hand.isDefined).length
      }.foldLeft(0)( (ac,v) => ac+v )

      Future( handsPlayed mustBe 18*3 ).flatMap { assert =>
        assert match {
          case Succeeded =>
            store.select(id).delete().test(s"Testing delete with filestore after success on ${id}") { oldv =>
              tempDir.list.foreach( p => testlog.fine(s"After delete(${id}), found in tempdir: $p") )
              f.exists mustBe false
            }
          case _ =>
            store.select(id).delete().test(s"Testing delete with filestore after failure ${id}") { oldv =>
              tempDir.list.foreach( p => testlog.fine(s"After delete(${id}), found in tempdir: $p") )
              f.exists mustBe false
              assert
            }
        }
      }.map { a => (a,Some(id)) }
    }
  }

  it should "allow all hands on a match to be played at the same time with a filestore" in {
    val store = getMatchDuplicateFileStore(tempDir)
    val bs = getBoardset()
    val mov = getMovement()
    playOnFileStore(store,tempDir,bs,mov).map { v => v._1 }
  }

  it should "allow all hands on n matches to be played at the same time with a filestore" in {
    val store = getMatchDuplicateFileStore(tempDir)
    val bs = getBoardset()
    val mov = getMovement()
    val n = 20
    val futures = for ( i <- 1 to n ) yield {
      playOnFileStore(store,tempDir,bs,mov)
    }
    val asserts = Future.foldLeft(futures)(List[(Assertion,Option[Id.MatchDuplicate])]()) { (ac,v) =>
      val (assert,id) = v
      assert match {
        case Succeeded =>
          testlog.info(s"${id} -> Succeeded")
          ac
        case _ =>
          testlog.info(s"${id} -> ${assert}")
          ((assert,id))::ac
      }
    }

    asserts.map { errors =>
      errors.length mustBe 0
    }
  }

}
