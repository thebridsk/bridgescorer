package com.github.thebridsk.bridge.server.manualtest

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import scala.concurrent.Future

object TestServer extends App {
  implicit val system: ActorSystem = ActorSystem("my-system")

  val route: Route =
    path("hello") {
      get {
        complete {
          <h1>Say hello to akka-http</h1>
        }
      }
    }

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("localhost", 8080).bind(route)
//    Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  scala.io.StdIn.readLine()

  import system.dispatcher // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
