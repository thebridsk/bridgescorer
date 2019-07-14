package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchChicagoV2
import com.github.thebridsk.bridge.data.MatchChicagoV1
import com.github.thebridsk.utilities.main.Main

import scala.reflect.ClassTag
import scala.language.implicitConversions
import com.github.thebridsk.bridge.backend.resource.VersionedInstanceJson
import play.api.libs.json._
import com.github.thebridsk.bridge.yaml.YamlSupport._
import com.github.thebridsk.bridge.backend.resource.JsonYamlConverter
import com.github.thebridsk.bridge.backend.resource.JsonConverter
import com.github.thebridsk.bridge.backend.resource.YamlConverter
import com.github.thebridsk.bridge.data.rest.JsonException
import com.github.thebridsk.bridge.backend.resource.Converter

object TestParseChicago extends Main {

  val log = logger

  val mc = MatchChicago("M0",
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

  val jsonConverter = JsonConverter
  val yamlConverter = YamlConverter
  val converterJsonYaml = Converter.getConverter(false)
  val converterYamlJson = Converter.getConverter(true)

  val matchChicagoInstanceJson = {
    implicit val converter = converterJsonYaml
    VersionedInstanceJson[String,MatchChicago].add[MatchChicagoV2].add[MatchChicagoV1]
  }

  val matchChicagoInstanceYaml = {
    implicit val converter = converterYamlJson
    VersionedInstanceJson[String,MatchChicago].add[MatchChicagoV2].add[MatchChicagoV1]
  }

  def execute() = {
    test(mc, matchChicagoInstanceJson, converterJsonYaml)
    test(mc1, matchChicagoInstanceJson, converterJsonYaml)
    test(mc2, matchChicagoInstanceJson, converterJsonYaml)
    test(mc, matchChicagoInstanceJson, converterYamlJson)
    test(mc1, matchChicagoInstanceJson, converterYamlJson)
    test(mc2, matchChicagoInstanceJson, converterYamlJson)
    0
  }

  def test[T]( v: T,
               instanceConverter: VersionedInstanceJson[String,MatchChicago],
               converter: Converter
             )(implicit format: Format[T]) = {
    println("Testing "+v)
    val j = converter.write(v)
    val old = converter.read[T](j)
    if (old._1) {
      println("old primary "+old._2)
    } else {
      println("old secondary "+old._2)
    }

    val instance = instanceConverter.parse( j )
    if (instance._1) {
      println("instance good "+instance._2)
    } else {
      println("instance converted "+instance._2)
    }
  }

}
