package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.model.HttpResponse

abstract class RoutingSpec extends AnyFlatSpec with ScalatestRouteTest with Matchers with Directives {

  val Ok = HttpResponse()
  val completeOk = complete(Ok)
}
