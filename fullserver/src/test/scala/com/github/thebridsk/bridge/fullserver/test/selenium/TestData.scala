package com.github.thebridsk.bridge.fullserver.test.selenium

import scala.reflect.io.Directory
import scala.reflect.io.File
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import com.github.thebridsk.utilities.file.FileIO
import org.scalatest.Assertions._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import scala.concurrent.ExecutionContext.Implicits.global
import scala.annotation.tailrec
import java.io.FileNotFoundException
import com.github.thebridsk.bridge.server.test.util.TestServer

object TestData {

  val log = Logger( TestData.getClass.getName )

  /**
   * The matches to replay.  The value is obtained from the system property or environment variable MatchToTest.
   * If None, then all games are replayed,
   * if Some(List(id,...)) where id is just the digits of the MatchDuplicate ID
   */
  val matchToTest = TestServer.getProp("MatchToTest").map{ ms =>
      ms.split("[ ,]+").flatMap(s => if (s==null || s.length() == 0) Nil else s::Nil ).toList
    }

  val testData = {
    val rawdir = TestServer.getProp("TestDataDirectory").map { dir => Directory(dir) }.getOrElse( Directory("../testdata") )
    val dir = Directory(rawdir.toAbsolute.jfile.getCanonicalFile)
    log.info(s"Using directory ${dir}")
    dir
  }

  def getFilenames( id: String ) = {
    instanceJson.getReadExtensions.map { ext =>
      testData.toString()+File.separator+"MatchDuplicate.M"+id+ext
    }
  }

  val instanceJson = new BridgeServiceFileStoreConverters(true).matchDuplicateJson

  def getOneGame( g: String ) = {
    def toMD( s: String ): (MatchDuplicate) = {
      (instanceJson.parse(s)._2)
    }

    val files = getFilenames(g)

    @tailrec
    def read( list: List[String] ): List[MatchDuplicate] = {
      if (list.isEmpty) {
        log.severe(s"Did not find any files with names: ${files}")
        throw new IllegalArgumentException(s"Did not find any files with names: ${files}")
      }
      val s = try {
        Some(FileIO.readFileSafe(list.head))
      } catch {
        case ex: FileNotFoundException =>
          None
      }
      s match {
        case Some(json) =>
          List(toMD(json))
        case None =>
          read( list.tail )
      }
    }

    read(files)
  }

  def getAllGamesFromDisk() = {
    val store = new BridgeServiceFileStore( testData )
    store.duplicates.syncStore.readAll() match {
      case Right(map) =>
        map.values.toList.sortWith((l,r) => l.id < r.id)
      case Left((statuscode,restmessage)) =>
        fail("Failed to get MatchDuplicates from testdata")
    }
  }

  def getAllGames() = {
    val r = matchToTest match {
      case Some(g) => g.flatMap( g1 => getOneGame(g1) )
      case None => getAllGamesFromDisk()
    }
    log.info(s"""In directory ${testData} found: ${r.map(md=>md.id).mkString(", ")}""")
    r
  }

}
