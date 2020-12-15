package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchChicagoV2
import com.github.thebridsk.bridge.data.MatchChicagoV1
import com.github.thebridsk.utilities.main.MainNoArgs

import com.github.thebridsk.bridge.server.backend.resource.VersionedInstanceJson
import play.api.libs.json._
import com.github.thebridsk.bridge.server.yaml.YamlSupport._
import com.github.thebridsk.bridge.server.backend.resource.JsonConverter
import com.github.thebridsk.bridge.server.backend.resource.YamlConverter
import com.github.thebridsk.bridge.server.backend.resource.Converter
import com.github.thebridsk.bridge.data.MatchChicagoV3

object TestParseChicago extends MainNoArgs {

  val log = logger

  val mc: MatchChicagoV3 = MatchChicago(
    MatchChicago.id("M0"),
    List("player1", "player2", "player3", "player4"),
    Nil,
    0,
    true,
    0,
    0
  )
  val mc2: MatchChicagoV2 = MatchChicagoV2(
    MatchChicago.id("M2"),
    List("player1", "player2", "player3", "player4"),
    Nil,
    0,
    0,
    0
  )
  val mc1: MatchChicagoV1 = MatchChicagoV1(
    MatchChicago.id("M1"),
    "player1",
    "player2",
    "player3",
    "player4",
    Nil,
    0,
    0,
    0
  )

  val jsonConverter = JsonConverter
  val yamlConverter = YamlConverter
  val converterJsonYaml: Converter = Converter.getConverter(false)
  val converterYamlJson: Converter = Converter.getConverter(true)

  val matchChicagoInstanceJson
      : VersionedInstanceJson[MatchChicago.Id, MatchChicago] = {
    implicit val converter = converterJsonYaml
    VersionedInstanceJson[MatchChicago.Id, MatchChicago]
      .add[MatchChicagoV2]
      .add[MatchChicagoV1]
  }

  val matchChicagoInstanceYaml
      : VersionedInstanceJson[MatchChicago.Id, MatchChicago] = {
    implicit val converter = converterYamlJson
    VersionedInstanceJson[MatchChicago.Id, MatchChicago]
      .add[MatchChicagoV2]
      .add[MatchChicagoV1]
  }

  def execute(): Int = {
    test(mc, matchChicagoInstanceJson, converterJsonYaml)
    test(mc1, matchChicagoInstanceJson, converterJsonYaml)
    test(mc2, matchChicagoInstanceJson, converterJsonYaml)
    test(mc, matchChicagoInstanceJson, converterYamlJson)
    test(mc1, matchChicagoInstanceJson, converterYamlJson)
    test(mc2, matchChicagoInstanceJson, converterYamlJson)
    0
  }

  def test[T](
      v: T,
      instanceConverter: VersionedInstanceJson[MatchChicago.Id, MatchChicago],
      converter: Converter
  )(implicit format: Format[T]): Unit = {
    println("Testing " + v)
    val j = converter.write(v)
    val old = converter.read[T](j)
    if (old._1) {
      println("old primary " + old._2)
    } else {
      println("old secondary " + old._2)
    }

    val instance = instanceConverter.parse(j)
    if (instance._1) {
      println("instance good " + instance._2)
    } else {
      println("instance converted " + instance._2)
    }
  }

}
