package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.MainNoArgs
import play.api.libs.json._

object TestPlayJson extends MainNoArgs {

  case class XX(y: Option[String] = Some("yy"))

  implicit val formatXX: OFormat[XX] = Json.format[XX]

  def execute(): Int = {

    test(XX(Some("goodbye")))
    test(XX(None))
    test(XX())

    0
  }

  def test(x: XX): Unit = {
    val jsv = Json.toJson(x)
    val sv = Json.prettyPrint(jsv)
    println(s"JSON of ${x} is:\n${sv}")

    val rjs = Json.parse(sv)
    val y = Json.fromJson(rjs)
    println(s"Read: ${y}")
  }
}
