package com.example.test

import com.example.data.MatchDuplicate
import com.example.data.sample.TestMatchDuplicate
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.example.data.rest.JsonSupport._

class TestSerialize extends FlatSpec with MustMatchers {

  behavior of "TestSerialize in bridgescorer-client"

  it should "SerializeEmpty MatchDuplicate" in {

    val md = MatchDuplicate.create()

    val s = writeJson(md)

    s mustBe """{"id":"","teams":[],"boards":[],"boardset":"","movement":"","created":0,"updated":0}"""


  }

  val serializedData = """{"id":"M1","""+
                       """"teams":[{"id":"T1","player1":"Nancy","player2":"Norman","created":0,"updated":0},"""+
                       """{"id":"T2","player1":"Ellen","player2":"Edward","created":0,"updated":0},"""+
                       """{"id":"T3","player1":"Susan","player2":"Sam","created":0,"updated":0},"""+
                       """{"id":"T4","player1":"Wilma","player2":"Wayne","created":0,"updated":0}],"""+
                       """"boards":[{"id":"B1","nsVul":false,"ewVul":false,"dealer":"N","""+
                                  """"hands":[{"played":[],"table":"1","round":1,"board":"B1","nsTeam":"T1","nIsPlayer1":true,"ewTeam":"T2","eIsPlayer1":true,"created":0,"updated":0},"""+
                                           """{"played":[],"table":"2","round":2,"board":"B1","nsTeam":"T3","nIsPlayer1":true,"ewTeam":"T4","eIsPlayer1":true,"created":0,"updated":0}],"""+
                                  """"created":0,"updated":0},"""+
                                 """{"id":"B2","nsVul":true,"ewVul":false,"dealer":"E","""+
                                  """"hands":[{"played":[],"table":"1","round":1,"board":"B2","nsTeam":"T1","nIsPlayer1":true,"ewTeam":"T2","eIsPlayer1":true,"created":0,"updated":0},"""+
                                           """{"played":[],"table":"2","round":2,"board":"B2","nsTeam":"T3","nIsPlayer1":true,"ewTeam":"T4","eIsPlayer1":true,"created":0,"updated":0}],"""+
                                  """"created":0,"updated":0},"""+
                                 """{"id":"B7","nsVul":true,"ewVul":true,"dealer":"S","""+
                                  """"hands":[{"played":[],"table":"2","round":4,"board":"B7","nsTeam":"T2","nIsPlayer1":true,"ewTeam":"T4","eIsPlayer1":true,"created":0,"updated":0},"""+
                                           """{"played":[],"table":"1","round":3,"board":"B7","nsTeam":"T3","nIsPlayer1":true,"ewTeam":"T1","eIsPlayer1":true,"created":0,"updated":0}],"""+
                                  """"created":0,"updated":0},"""+
                                 """{"id":"B8","nsVul":false,"ewVul":false,"dealer":"W","""+
                                  """"hands":[{"played":[],"table":"2","round":4,"board":"B8","nsTeam":"T2","nIsPlayer1":true,"ewTeam":"T4","eIsPlayer1":true,"created":0,"updated":0},"""+
                                           """{"played":[],"table":"1","round":3,"board":"B8","nsTeam":"T3","nIsPlayer1":true,"ewTeam":"T1","eIsPlayer1":true,"created":0,"updated":0}],"""+
                                  """"created":0,"updated":0}],"""+
                       """"boardset":"","movement":"","created":0,"updated":0}"""

  it should "SerializeTest MatchDuplicate" in {

    val md = TestMatchDuplicate.create("M1")

    val s = writeJson(md)

    println("s is "+s)

    s mustBe serializedData

  }

}
