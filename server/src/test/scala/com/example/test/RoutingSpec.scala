package com.example.test

import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._

abstract class RoutingSpec extends FlatSpec with ScalatestRouteTest with MustMatchers with Directives {

  val Ok = HttpResponse()
  val completeOk = complete(Ok)
}