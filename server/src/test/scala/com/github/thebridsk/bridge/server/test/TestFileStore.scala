package com.github.thebridsk.bridge.server.test

import org.scalatest.matchers.must.Matchers

import org.scalatest.BeforeAndAfterAll
import scala.reflect.io.Directory
import com.github.thebridsk.utilities.file.FileIO
import java.io.File
import com.github.thebridsk.bridge.server.backend.resource.FileStore
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicateV1
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.MatchDuplicateV2
import play.api.libs.json.Writes
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.resource.Store
import com.github.thebridsk.bridge.server.backend.MatchRubberCacheStoreSupport
import com.github.thebridsk.bridge.server.backend.MatchDuplicateCacheStoreSupport
import org.scalatest.flatspec.AsyncFlatSpec
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.MatchRubberV1

class TestFileStore extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  var tempDir: Directory = null
  val resourceName = "MatchRubber"
  val resourceURI = "/rubbers"

  val testlog: Logger = Logger[TestFileStore]()

  TestStartLogging.startLogging()

  val converters = new BridgeServiceFileStoreConverters(true)
  import converters._

  override def beforeAll(): Unit = {
    tempDir = Directory.makeTemp( "TestFileStore", ".teststore" )
    testlog.fine( "Using temporary directory "+tempDir)
  }

  override def afterAll(): Unit = {
    testlog.fine( "Deleting temporary directory "+tempDir)
    tempDir.deleteRecursively()
  }

  behavior of "FileStore"

  val team: MatchRubberV1 = MatchRubber(MatchRubber.id(1), "Fred", "Barney", "", "", "N", Nil)
  val team2: MatchRubberV1 = MatchRubber(MatchRubber.id(2), "Wilma", "Betty", "", "", "N", Nil)
  var teamStore: Store[MatchRubber.Id,MatchRubber] = null

  implicit val support: MatchRubberCacheStoreSupport = new MatchRubberCacheStoreSupport(false)
  implicit val supportD: MatchDuplicateCacheStoreSupport = new MatchDuplicateCacheStoreSupport(false)

  def filename( id: String ): String = resourceName+"."+id+support.getWriteExtension
  def path(id: String ): String = tempDir.toString()+File.separator+filename(id)

  it should "read a team object from the filestore" in {
    val steam = support.toJSON(team)
    val steam2 = support.toJSON(team2)
    FileIO.writeFileSafe(path(team.id.id), steam)
    FileIO.writeFileSafe(path(team2.id.id)+".new", steam2)

    FileIO.exists(path(team.id.id)) mustBe true
    FileIO.exists(path(team.id.id)+".new") mustBe false
    FileIO.exists(path(team2.id.id)) mustBe false
    FileIO.exists(path(team2.id.id)+".new") mustBe true

    teamStore = FileStore( "test", tempDir)

    teamStore.select(team.id).read() flatMap { fromStore =>

      fromStore mustBe Right(team)

      FileIO.exists(path(team.id.id)) mustBe true
      FileIO.exists(path(team.id.id)+".new") mustBe false
      FileIO.exists(path(team2.id.id)) mustBe false
      FileIO.exists(path(team2.id.id)+".new") mustBe true

      teamStore.select(team2.id).read() map { fromStore2 =>

        fromStore2 mustBe Right(team2)

        FileIO.exists(path(team.id.id)) mustBe true
        FileIO.exists(path(team.id.id)+".new") mustBe false
        FileIO.exists(path(team2.id.id)) mustBe true
        FileIO.exists(path(team2.id.id)+".new") mustBe false
      }

    }
  }

  it should "return the new team when adding another team, and create file in temp directory" in {
    val team3 = MatchRubber(MatchRubber.id(1), "Yogi", "Boo Boo", "", "", "N", Nil)

    teamStore.createChild(team3).map { result =>
      result match {
        case Right(r) =>
          testlog.fine(s"Created R3, got ${r}")
          r.id mustBe MatchRubber.id(3)
          r.north mustBe team3.north
          r.south mustBe team3.south
          FileIO.exists(path(r.id.id)) mustBe true
          FileIO.exists(path(r.id.id)+".new") mustBe false
        case Left((statuscode,msg)) =>
          fail("Was expecting success, got "+statuscode+" "+msg )
      }
    }
  }

  it should "read the same record twice" in {
    testlog.fine(s"Starting: allow team 3 to be updated with new names")
    teamStore.select(MatchRubber.id(3)).read().flatMap { result=>
      result match {
        case Right(t3) =>
          testlog.fine(s"Read R3, got ${t3}")
          teamStore.select(MatchRubber.id(3)).read().map { resulta=>
            resulta match {
              case Right(t3a) =>
                testlog.fine(s"Read R3, got ${t3a}")
                t3a mustBe t3
              case Left((statuscode,msg)) =>
                fail("Was expecting success on reading T3, got "+statuscode+" "+msg )
            }
          }
        case Left((statuscode,msg)) =>
          fail("Was expecting success on reading T3, got "+statuscode+" "+msg )
      }
    }

  }

  it should "allow team 3 to be updated with new names" in {
    testlog.fine(s"Starting: allow team 3 to be updated with new names")
    teamStore.select(MatchRubber.id(3)).read().flatMap { result=>
      result match {
        case Right(t3) =>
          testlog.fine(s"Read R3, got ${t3}")
          val nt = t3.setPlayers("Rocky", "Bullwinkle", t3.east, t3.west)
          teamStore.select(t3.id).update(nt).map { resu =>
            resu match {
              case Right(nt3) =>
                testlog.fine(s"Updated R3, got ${nt3}")
                nt.equalsIgnoreModifyTime(nt3) mustBe true
                t3.equalsIgnoreModifyTime(nt3) mustBe false
                FileIO.exists(path(nt3.id.id)) mustBe true
                FileIO.exists(path(nt3.id.id)+".new") mustBe false

                val s3 = FileIO.readFileSafe(path(nt3.id.id))
                val (goodOnDisk,d3) = support.fromJSON(s3)
                d3 mustBe nt3

              case Left((statuscode,msg)) =>
                fail("Was expecting success on updating T3, got "+statuscode+" "+msg )
            }
          }
        case Left((statuscode,msg)) =>
          fail("Was expecting success on reading T3, got "+statuscode+" "+msg )
      }
    }
  }

  it should "allow team 2 to be deleted" in {
    teamStore.select(MatchRubber.id(2)).delete().map { result =>
      result match {
        case Right(t2) =>
          t2.id mustBe MatchRubber.id(2)
          FileIO.exists(path(t2.id.id)) mustBe false
          FileIO.exists(path(t2.id.id)+".new") mustBe false
        case Left((statuscode,msg)) =>
          fail("Was expecting success on reading T3, got "+statuscode+" "+msg )
      }
    }
  }

  it should "only have teams 1 and 3 left" in {
    teamStore.readAll().map { result =>
      result match {
        case Right(teams) =>
          teams.keys.toArray must contain theSameElementsAs Array( MatchRubber.id(1), MatchRubber.id(3) )
        case Left((statuscode,msg)) =>
          fail("Was expecting success on reading T3, got "+statuscode+" "+msg )
      }
    }
  }

  var duplicateStore: Store[MatchDuplicate.Id,MatchDuplicate] = null

  behavior of "Filestore updating MatchDuplicate versions"

  import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

  def toString[T]( md: T )(implicit writer: Writes[T]): String = {
    writePrettyJson(md)
  }

  it should "be able to get an old MatchDuplicateV1 from the store" in {
    val md = MatchDuplicateV1.create(MatchDuplicate.id(1))
    val oldmd = toString(md)

    def filenameMD( id: String ) = "MatchDuplicate."+id+".json"
    def pathMD(id: String ) = tempDir.toString()+File.separator+filenameMD(id)

    FileIO.writeFileSafe(pathMD(md.id.id), oldmd)

    FileIO.exists(pathMD(md.id.id)) mustBe true
    FileIO.exists(pathMD(md.id.id)+".new") mustBe false

    duplicateStore = FileStore( "test", tempDir )

    duplicateStore.select(md.id).read().map { result =>
      result match {
        case Right(newmd) =>
          newmd.id mustBe md.id
          newmd.boards.map(b=>b.id->b).toMap mustBe md.boards
          newmd.teams.map(b=>b.id->b).toMap mustBe md.teams
          newmd.boardset mustBe BoardSet.default
          newmd.movement mustBe Movement.default

        case Left((statusCode,restMessage)) =>
          fail("Failed to get MatchDuplicate with id "+md.id+": statuscode="+statusCode+", message="+restMessage )
      }
    }

  }

  it should "be able to get an old MatchDuplicateV2 from the store" in {
    val md = MatchDuplicateV2.create(MatchDuplicate.id(1))
    val oldmd = toString(md)

    def filenameMD( id: String ) = "MatchDuplicate."+id+".json"
    def pathMD(id: String ) = tempDir.toString()+File.separator+filenameMD(id)

    FileIO.writeFileSafe(pathMD(md.id.id), oldmd)

    FileIO.exists(pathMD(md.id.id)) mustBe true
    FileIO.exists(pathMD(md.id.id)+".new") mustBe false

    duplicateStore = FileStore("test", tempDir )

    duplicateStore.select(md.id).read().map { result =>
      result match {
        case Right(newmd) =>
          newmd.id mustBe md.id
          newmd.boards.map(b=>b.id->b).toMap mustBe md.boards
          newmd.teams.map(b=>b.id->b).toMap mustBe md.teams
          newmd.boardset mustBe BoardSet.default
          newmd.movement mustBe Movement.default

        case Left((statusCode,restMessage)) =>
          fail("Failed to get MatchDuplicate with id "+md.id+": statuscode="+statusCode+", message="+restMessage )
      }
    }

  }
}
