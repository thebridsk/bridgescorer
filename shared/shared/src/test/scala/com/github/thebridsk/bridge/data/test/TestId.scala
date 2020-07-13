package com.github.thebridsk.bridge.data.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import play.api.libs.json.Json
import com.github.thebridsk.bridge.data.MatchDuplicate
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.data.MatchChicago

class TestRubber extends AnyFlatSpec with Matchers {

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
    val id1 = MatchDuplicate.id("M1")
    val id2 = Id[MatchChicago]("M1")

    (id1 == id2) mustBe false
    (id1 != id2) mustBe true

    // id1 < id2    // compile error
    // id1 > id2    // compile error
    // id1 <= id2   // compile error
    // id1 >= id2   // compile error
  }

}
