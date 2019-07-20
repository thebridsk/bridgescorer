package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import play.api.libs.json._

object TestPlayJson extends Main {

  case class XX( y: Option[String] = Some("yy") )

  implicit val formatXX = Json.format[XX]

  def execute() = {

    test( XX( Some("goodbye")) )
    test( XX( None ) )
    test( XX( ) )

    0
  }

  def test( x: XX ) = {
    val jsv = Json.toJson(x)
    val sv = Json.prettyPrint(jsv)
    println(s"JSON of ${x} is:\n${sv}" )

    val rjs = Json.parse(sv)
    val y = Json.fromJson(rjs)
    println(s"Read: ${y}" )
  }
}
