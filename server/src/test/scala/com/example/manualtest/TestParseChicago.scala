package com.example.manualtest

import com.example.data.MatchChicago
import com.example.data.MatchChicagoV2
import com.example.data.MatchChicagoV1
import utils.main.Main

import scala.reflect.ClassTag
import scala.language.implicitConversions
import com.example.backend.resource.VersionedInstanceJson
import play.api.libs.json._
import com.example.yaml.YamlSupport._
import com.example.backend.resource.JsonYamlConverter
import com.example.backend.resource.JsonConverter
import com.example.backend.resource.YamlConverter
import com.example.data.rest.JsonException
import com.example.backend.resource.BaseConverter

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

  val jsonConverter = new JsonConverter
  val yamlConverter = new YamlConverter
  val converterJsonYaml = new JsonYamlConverter( jsonConverter, yamlConverter )
  val converterYamlJson = new JsonYamlConverter( yamlConverter, jsonConverter )

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
               converter: JsonYamlConverter
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
