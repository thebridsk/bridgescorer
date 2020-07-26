package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import com.github.thebridsk.bridge.data.MatchChicagoV3
import com.github.thebridsk.bridge.data.MatchChicagoV2
import com.github.thebridsk.bridge.data.MatchChicagoV1
import com.github.thebridsk.bridge.server.backend.resource.VersionedInstanceJson
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.server.backend.resource.JsonConverter
import com.github.thebridsk.bridge.server.backend.resource.YamlConverter
import play.api.libs.json._
import com.github.thebridsk.bridge.server.yaml.YamlSupport._
import com.github.thebridsk.bridge.data.rest.JsonException
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.resource.Converter

class TestVersionedInstance extends AnyFlatSpec with Matchers {

  behavior of "FileStore"

  val mc3: MatchChicagoV3 = MatchChicagoV3(MatchChicago.id(0),
                           List("player1","player2","player3","player4"),
                           Nil,
                           0,
                           true,
                           0,0
                          )
  val mc2: MatchChicagoV2 = MatchChicagoV2(MatchChicago.id(2),
                           List("player1","player2","player3","player4"),
                           Nil,
                           0,
                           0,0
                          )
  val mc1: MatchChicagoV1 = MatchChicagoV1(MatchChicago.id(1),
                           "player1","player2","player3","player4",
                           Nil,
                           0,
                           0,0
                         )

  val jsonConverter = JsonConverter
  val yamlConverter = YamlConverter
  implicit val converterYamlJson: Converter = Converter.getConverter(true)

  val converters = new BridgeServiceFileStoreConverters(true)
  import converters.matchChicagoJson

  val matchChicagoJsonV1Only: VersionedInstanceJson[MatchChicago.Id,MatchChicago] = VersionedInstanceJson[MatchChicago.Id,MatchChicago].add[MatchChicagoV1]

  def test[C <: VersionedInstance[C,C,MatchChicago.Id],T <: VersionedInstance[C,T,MatchChicago.Id]](
      v: T,
      good: Boolean,
      converter: VersionedInstanceJson[MatchChicago.Id,C],
      writeConverter: Converter
  )(implicit writer: Writes[T]): C = {
    val j = writeConverter.write(v)
    val vc = converter.parse( j )
    if (vc._1) {
      println("good "+vc._2)
    } else {
      println("converted "+vc._2)
    }
    vc._1 mustBe good
    vc._2
  }

  it should "read a V3 chicago json" in {
    val c3 = test(mc3,false,matchChicagoJson, jsonConverter)

    c3.id mustBe mc3.id
    c3.players mustBe mc3.players
    c3.gamesPerRound mustBe mc3.gamesPerRound
    c3.simpleRotation mustBe mc3.simpleRotation
  }

  it should "read a V3 chicago yaml" in {
    val c3 = test(mc3,true,matchChicagoJson, yamlConverter)

    c3.id mustBe mc3.id
    c3.players mustBe mc3.players
    c3.gamesPerRound mustBe mc3.gamesPerRound
    c3.simpleRotation mustBe mc3.simpleRotation
  }

  it should "read a V2 chicago json" in {
    val c2 = test(mc2,false,matchChicagoJson, jsonConverter)

    c2.id mustBe mc2.id
    c2.players mustBe mc2.players
    c2.gamesPerRound mustBe mc2.gamesPerRound
    c2.simpleRotation mustBe false
  }

  it should "read a V2 chicago yaml" in {
    val c2 = test(mc2,false,matchChicagoJson, yamlConverter)

    c2.id mustBe mc2.id
    c2.players mustBe mc2.players
    c2.gamesPerRound mustBe mc2.gamesPerRound
    c2.simpleRotation mustBe false
  }

  it should "read a V1 chicago json" in {
    val c1 = test(mc1,false,matchChicagoJson, jsonConverter)

    c1.id mustBe mc1.id
    c1.players mustBe List(mc1.player1,mc1.player2,mc1.player3,mc1.player4)
    c1.gamesPerRound mustBe mc1.gamesPerRound
    c1.simpleRotation mustBe false
  }

  it should "read a V1 chicago yaml" in {
    val c1 = test(mc1,false,matchChicagoJson, yamlConverter)

    c1.id mustBe mc1.id
    c1.players mustBe List(mc1.player1,mc1.player2,mc1.player3,mc1.player4)
    c1.gamesPerRound mustBe mc1.gamesPerRound
    c1.simpleRotation mustBe false
  }

  it should "read a V3 chicago json again" in {
    val c3 = test(mc3,false,matchChicagoJsonV1Only, jsonConverter)

    c3.id mustBe mc3.id
    c3.players mustBe mc3.players
    c3.gamesPerRound mustBe mc3.gamesPerRound
    c3.simpleRotation mustBe mc3.simpleRotation
  }

  it should "read a V3 chicago yaml again" in {
    val c3 = test(mc3,true,matchChicagoJsonV1Only, yamlConverter)

    c3.id mustBe mc3.id
    c3.players mustBe mc3.players
    c3.gamesPerRound mustBe mc3.gamesPerRound
    c3.simpleRotation mustBe mc3.simpleRotation
  }

  it should "fail to read a V2 chicago json" in {
    assertThrows[JsonException] {
      val c2 = test(mc2,false,matchChicagoJsonV1Only, jsonConverter)
    }

  }

  it should "fail to read a V2 chicago yaml" in {
    assertThrows[JsonException] {
      val c2 = test(mc2,false,matchChicagoJsonV1Only, yamlConverter)
    }

  }

  it should "read a V1 chicago again json" in {
    val c1 = test(mc1,false,matchChicagoJsonV1Only, jsonConverter)

    c1.id mustBe mc1.id
    c1.players mustBe List(mc1.player1,mc1.player2,mc1.player3,mc1.player4)
    c1.gamesPerRound mustBe mc1.gamesPerRound
    c1.simpleRotation mustBe false
  }

  it should "read a V1 chicago again yaml" in {
    val c1 = test(mc1,false,matchChicagoJsonV1Only, yamlConverter)

    c1.id mustBe mc1.id
    c1.players mustBe List(mc1.player1,mc1.player2,mc1.player3,mc1.player4)
    c1.gamesPerRound mustBe mc1.gamesPerRound
    c1.simpleRotation mustBe false
  }

}
