package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.utilities.main.Main
import com.github.thebridsk.bridge.backend.resource.FileIO
import org.scalatest.MustMatchers
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.backend.MatchDuplicateCacheStoreSupport

object CheckTimes extends Main with MustMatchers {

  def execute() = {
    checkTimeFromFile("DuplicateTest.TimeError.json")
  }

  def readFromFile( file: String ) = {
    FileIO.readFileSafe(file)
  }

  def checkTimeFromFile( file: String ) = {
    val json = readFromFile(file)
    val md = convertToMatch(json)
    checkTimeMatch(md)
  }

  val converters = new BridgeServiceFileStoreConverters(true)
  import converters._

  def convertToMatch( json: String ) = {

    val (id,played) = new MatchDuplicateCacheStoreSupport(false).fromJSON(json)
    played
  }

  def checkTimeMatch( played: MatchDuplicate ) = {

    val created = played.created
    val updated = played.updated

    created must not be (0)
    updated must not be (0)
    assert( created <= updated)

    played.boards.foreach( b => {
      b.created must not be (0)
      b.updated must not be (0)
      assert( b.created <= b.updated)
      assert( created-100 <= b.created && b.created <= updated+100 )
      assert( created-100 <= b.updated && b.updated <= updated+100 )
      b.hands.foreach( h=> {
        h.created must not be (0)
        h.updated must not be (0)
        assert( h.created <= h.updated)
        assert( b.created-100 <= h.created && h.created <= b.updated+100 )
        assert( b.created-100 <= h.updated && h.updated <= b.updated+100 )
      })
    })
    played.teams.foreach(t =>{
      t.created must not be (0)
      t.updated must not be (0)
      assert( t.created <= t.updated)
      assert( created-100 <= t.created && t.created <= updated+100 )
      assert( created-100 <= t.updated && t.updated <= updated+100 )
    })

    0
  }
}
