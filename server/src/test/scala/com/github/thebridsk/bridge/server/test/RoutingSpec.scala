package com.github.thebridsk.bridge.server.test

import akka.http.scaladsl.model.AttributeKey
import akka.http.scaladsl.model.RemoteAddress.IP
import java.net.InetAddress
import akka.http.scaladsl.model.AttributeKeys
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.StandardRoute

trait RoutingSpec extends Directives {

  val Ok: HttpResponse = HttpResponse()
  val completeOk: StandardRoute = complete(Ok)

  def remoteAddress( host: InetAddress, port: Int ): Map[AttributeKey[_],_] = Map(
    AttributeKeys.remoteAddress -> IP(host, Some(port))
  )


  val remoteAddress: Map[AttributeKey[_],_] = remoteAddress(InetAddress.getLocalHost, 12345)

  val remoteAddressLocal: Map[AttributeKey[_],_] = remoteAddress(InetAddress.getLoopbackAddress, 12345)

}
