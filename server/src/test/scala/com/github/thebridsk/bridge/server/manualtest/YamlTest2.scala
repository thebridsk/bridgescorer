package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main

import java.io.StringReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import play.api.libs.json.jackson.PlayJsonModule
import play.api.libs.json._

case class Thing2(colour: String, priority: Int) {

  override def toString(): String =
    s"<Thing colour=${colour} priority=${priority}>"
}

case class Sample2(
    name: String,
    parameters: Map[String, String],
    things: List[Thing2]
) {

  override def toString(): String =
    s"<Sample name=${name} parameters=${parameters} things=${things}>"

}

object YamlTest2 extends Main {

  val test = """
name: test2
parameters:
  "VERSION": 0.0.1-SNAPSHOT

things:
  - colour: green
    priority: 128
  - colour: red
    priority: 64
"""

  def execute(): Int = {

    val reader = new StringReader(test)
    val mapper = new ObjectMapper(new YAMLFactory())
      .registerModule(new PlayJsonModule(JsonParserSettings()))
    val jsvalue = mapper.readValue(reader, classOf[JsValue])

    implicit val thingFormat = Json.format[Thing2]
    implicit val sampleFormat = Json.format[Sample2]

    val config = Json.fromJson[Sample2](jsvalue)

    println(s"config: ${config}")
    0
  }
}
