package com.github.thebridsk.bridgedemo.test

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import java.io.File
import java.net.URL
import akka.http.scaladsl.Http
import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Simple test server that serves a directory.
  *
  * Usage:
  * {{{
  * val server = DemoServer(
  *   dir = new File("./demo"),
  *   listen = new URL("http://localhost:8080"),
  *   timeout = 5 minutes
  * )
  * }}}
  *
  * @constructor
  * @param dir the directory to serve
  * @param listen the listening URL, protocol is ignored
  * @param timeout the max time the server will run
  * @param system ActorSystem
  * @param ec ExecutionContext
  */
class DemoServer(
  dir: File,
  listen: URL = new URL("http://localhost:8080"),
  timeout: FiniteDuration = 5 minutes
)(
  implicit
    system: ActorSystem,
    ec: ExecutionContext = ExecutionContext.global
) {
  import DemoServer.log

  private val route =
    getFromDirectory(dir.toString())

  private val bindingFuture =
    Http().newServerAt(listen.getHost, listen.getPort()).bind(route)

  private val timer = system.scheduler.scheduleOnce(timeout) {
    stopServerI()
  }

  bindingFuture.onComplete { _ match {
    case Success(binding) =>
      val localAddress = binding.localAddress
      log.info(
        s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort} for HTTP"
      )

    case Failure(exception) =>
      log.severe(s"Unable to start Demo Server at ${listen}", exception)
  }}

  /**
    * Check if the server is running
    *
    * @return true if serving requests or still coming up.
    *         false if the server failed, or terminated.
    */
  def isRunning: Boolean = {
    bindingFuture.value match {
      case Some(Success(sb)) =>
        // server is running and accepting requests
        // unless the termination signal was issued
        !sb.whenTerminationSignalIssued.isCompleted
      case Some(Failure(ex)) =>
        // server failed to start
        false
      case None =>
        // server is still starting
        true
    }
  }

  /**
    * Stop the server
    */
  def stopServer(): Unit = {
    timer.cancel()
    stopServerI()
  }

  private def stopServerI(): Unit = {
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

/**
  * Start a demo server.
  *
  * The [[apply]] function is used to start a demo server in tests.
  * It is recommended that the server be started in a beforeAll,
  * and stopped in an afterAll.
  *
  * The demo server may also be started as a main program.
  * See [[main]] for details.
  *
  */
object DemoServer {

  val log = Logger[DemoServer.type]()

  /**
    * Start up a demo server.
    *
    * Starts up an ActorSystem, and terminates it when done.
    *
    * @param dir the directory to serve
    * @param listen the listening URL, protocol is ignored
    * @param timeout the max time the server will run
    * @return the DemoServer object
    */
  def apply(
    dir: File,
    listen: URL = new URL("http://localhost:8080"),
    timeout: FiniteDuration = 5 minutes
  ): DemoServer = {

    implicit val system = ActorSystem("DemoActorSystem")
    implicit val ec = ExecutionContext.global

    new DemoServer(dir, listen, timeout)
  }

  /**
    * Main entry point for starting the demo server.
    *
    * Syntax:
    * {{{
    *   java com.github.thebridsk.bridgedemo.test.DemoServer demodir listen
    *
    *   sbt "demo/runMain com.github.thebridsk.bridgedemo.test.DemoServer demodir listen"
    * }}}
    * Where:
    * - `demodir` is the demo directory to serve
    * - `listen` is a URL that identifies the host and port the server should listen on.
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    val dir = new File(args(0)).getAbsoluteFile().getCanonicalFile()
    val listen = new URL(args(1))

    val ds = DemoServer(dir,listen)
    println(s"Server online at ${listen}, serving ${dir}\nPress RETURN to stop...")
    Thread.sleep(5*60*1000L)
    ds.stopServer()

    System.exit(0)
  }
}
