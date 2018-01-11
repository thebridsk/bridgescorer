package com.example.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest.Matchers

import com.example.data.bridge._
import utils.logging.Logger
import com.example.data.MatchChicagoV3
import com.example.data.MatchChicagoV2
import com.example.data.MatchChicagoV1
import com.example.backend.resource.VersionedInstanceJson
import com.example.data.VersionedInstance
import com.example.data.MatchChicago
import com.example.backend.resource.JsonConverter
import com.example.backend.resource.YamlConverter
import com.example.backend.resource.JsonYamlConverter
import play.api.libs.json._
import com.example.yaml.YamlSupport._
import com.example.data.rest.JsonException
import com.example.backend.resource.BaseConverter
import com.example.backend.BridgeServiceFileStoreConverters

class TestVersionedInstance extends FlatSpec with MustMatchers {

  behavior of "FileStore"

  val mc3 = MatchChicagoV3("M0",
                           List("player1","player2","player3","player4"),
                           Nil,
                           0,
                           true,
                           0,0
                          )
  val mc2 = MatchChicagoV2("M2",
                           List("player1","player2","player3","player4"),
                           Nil,
                           0,
                           0,0
                          )
  val mc1 = MatchChicagoV1("M1",
                           "player1","player2","player3","player4",
                           Nil,
                           0,
                           0,0
                         )

  val jsonConverter = new JsonConverter
  val yamlConverter = new YamlConverter
  implicit val converterJsonYaml = new JsonYamlConverter( yamlConverter, jsonConverter )

  val converters = new BridgeServiceFileStoreConverters(true)
  import converters.matchChicagoJson

  val matchChicagoJsonV1Only = VersionedInstanceJson[String,MatchChicago].add[MatchChicagoV1]

  def test[C <: VersionedInstance[C,C,String],T <: VersionedInstance[C,T,String]](
      v: T,
      good: Boolean,
      converter: VersionedInstanceJson[String,C],
      writeConverter: BaseConverter
  )(implicit writer: Writes[T]) = {
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
