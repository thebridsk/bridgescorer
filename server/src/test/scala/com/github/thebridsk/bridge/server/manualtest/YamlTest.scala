package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.MainNoArgs

import java.util.{List => JList, Map => JMap}
import scala.jdk.CollectionConverters._
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.StringReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

object Preconditions {
  def checkNotNull[T](v: T, msg: String): T =
    if (v != null) v else throw new NullPointerException(msg)
}

class Thing(
    @JsonProperty("colour") _colour: String,
    @JsonProperty("priority") _priority: Int
) {
  val colour: String =
    Preconditions.checkNotNull(_colour, "colour cannot be null")
  val priority: Int =
    Preconditions.checkNotNull(_priority, "priority cannot be null")

  override def toString(): String =
    s"<Thing colour=${colour} priority=${priority}>"
}

class Sample(
    @JsonProperty("name") _name: String,
    @JsonProperty("parameters") _parameters: JMap[String, String],
    @JsonProperty("things") _things: JList[Thing]
) {
  val name: String = Preconditions.checkNotNull(_name, "name cannot be null")
  val parameters: Map[String, String] = Preconditions
    .checkNotNull(_parameters, "parameters cannot be null")
    .asScala
    .toMap
  val things: List[Thing] =
    Preconditions.checkNotNull(_things, "things cannot be null").asScala.toList

  override def toString(): String =
    s"<Sample name=${name} parameters=${parameters} things=${things}>"

}

object YamlTest extends MainNoArgs {

  val test = """
name: test
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
    val config: Sample = mapper.readValue(reader, classOf[Sample])

    println(s"config: ${config}")
    0
  }
}
