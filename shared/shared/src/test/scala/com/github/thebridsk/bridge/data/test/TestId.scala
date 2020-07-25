package com.github.thebridsk.bridge.data.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import play.api.libs.json.Json
import com.github.thebridsk.bridge.data.MatchDuplicate
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.DuplicateSummary

class TestId extends AnyFlatSpec with Matchers {

  behavior of "the Id class"

  it should "Serialize an Id and deserialize it" in {

    val id = MatchDuplicate.id("M1")

    val jsonId = Json.toJson(id)
    val stringId = Json.prettyPrint(jsonId)

    val idFromString = Json.parse(stringId)
    val idFromJson = Json.fromJson[MatchDuplicate.Id](idFromString) match {
      case JsSuccess(x,_) =>
        x mustBe id
        x
      case JsError(e) =>
        fail(s"Unable to deserialize '$stringId'")
    }

    val jsonId2 = Json.toJson(idFromJson)
    val stringId2 = Json.prettyPrint(jsonId2)

    stringId2 mustBe stringId

    println( s"Id = ${id} ==> json = ${stringId2}")

  }

  it should "be able to compare two Ids" in {
    val id1 = MatchDuplicate.id("M1")
    val id2 = MatchDuplicate.id("M2")
    val id3 = MatchDuplicate.id("M3")
    val id2a = MatchDuplicate.id("M2")

    id1 < id2 mustBe true
    id1 <= id2 mustBe true
    id1 > id2 mustBe false
    id1 >= id2 mustBe false
    id1 == id2 mustBe false
    id1 != id2 mustBe true

    id2 < id2a mustBe false
    id2 <= id2a mustBe true
    id2 > id2a mustBe false
    id2 >= id2a mustBe true
    id2 == id2a mustBe true
    id2 != id2a mustBe false

    id2 < id3 mustBe true
    id2 <= id3 mustBe true
    id2 > id3 mustBe false
    id2 >= id3 mustBe false
    id2 == id3 mustBe false
    id2 != id3 mustBe true

  }

  it should "have a compile error when comparing different types of IDs" in {
    val id1 = MatchDuplicate.id(1)
    val id2 = MatchChicago.id(1)

    (id1 == id2) mustBe false
    (id1 != id2) mustBe true

    // id1 < id2    // compile error
    // id1 > id2    // compile error
    // id1 <= id2   // compile error
    // id1 >= id2   // compile error
  }

  it should "throw an exception trying to create a DuplicateSummary.Id" in {
    assertThrows[IllegalArgumentException] {
      DuplicateSummary.id(1)
    }
    assertThrows[IllegalArgumentException] {
      DuplicateSummary.id("A1")
    }

  }

  it should "allow going to a base type and back" in {
    val id1 = MatchDuplicate.id(1)
    val id2 = MatchDuplicateResult.id(1)

    val id1b: DuplicateSummary.Id = id1

    id1b mustBe id1
    id1b must not be id2

    val id1s: MatchDuplicate.Id = id1b.toSubclass.get

    id1b mustBe id1
    id1b must not be id2

  }

  it should "allow going to a base type and back for result" in {
    val id1 = MatchDuplicate.id(1)
    val id2 = MatchDuplicateResult.id(1)

    val id1b: DuplicateSummary.Id = id1

    import com.github.thebridsk.bridge.data.rest.JsonSupport._

    val json = writeJson(id1b)

    val id1j = readJson[DuplicateSummary.Id](json)

    id1j mustBe id1

    val id1js: MatchDuplicate.Id = id1j.toSubclass.get

    id1js mustBe id1

    val json2 = writeJson(id2)

    val id2j = readJson[DuplicateSummary.Id](json2)

    id2j mustBe id2

    val id2js: MatchDuplicateResult.Id = id2j.toSubclass.get

    id2js mustBe id2
  }

}
