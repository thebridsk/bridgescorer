package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.data.LoggerConfig
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.thebridsk.bridge.server.backend.BridgeService
import jakarta.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.data.ServerVersion
import com.github.thebridsk.bridge.server.version.VersionServer
import com.github.thebridsk.bridge.data.version.VersionShared
import com.github.thebridsk.utilities.version.VersionUtilities
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.BoardSetsAndMovements
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.server.backend.resource.Result
import java.util.concurrent.atomic.AtomicInteger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import jakarta.ws.rs.GET
import akka.http.scaladsl.server.Route

case class ServerPort(httpPort: Option[Int], httpsPort: Option[Int])

object RestLoggerConfig {
  val log: Logger = Logger(getClass.getName)

  private val pclientid = new AtomicInteger()

  def nextClientId: Some[String] = Some(pclientid.incrementAndGet().toString())

  def serverURL(ports: ServerPort, includeLocalhost: Boolean = false): ServerURL = {
    import java.net.NetworkInterface
    import scala.jdk.CollectionConverters._
    import java.net.Inet4Address

    val x = NetworkInterface.getNetworkInterfaces.asScala
      .filter { x =>
        x.isUp() && !x.isLoopback()
      }
      .flatMap { ni =>
        ni.getInetAddresses.asScala
      }
      .filter { x =>
        x.isInstanceOf[Inet4Address]
      }
      .map { x =>
        getURLs(x.getHostAddress, ports).toList
      }
      .flatten
      .toList
    val local = Option.when(includeLocalhost)(getURLs("localhost", ports).toList).getOrElse(List())

    ServerURL(x:::local)

  }

  /**
    * Get the URLs for an interface
    * @param interface the interface IP address for the URLs
    * @return A list of URLs
    */
  def getURLs(interface: String, ports: ServerPort): Iterable[String] = {
    val httpsURL = ports.httpsPort match {
      case Some(port) =>
        if (port == 443) Some("https://" + interface + "/")
        else Some("https://" + interface + ":" + port + "/")
      case None => None
    }
    val httpURL = ports.httpPort match {
      case Some(port) =>
        if (port == 80) Some("http://" + interface + "/")
        else Some("http://" + interface + ":" + port + "/")
      case None if (httpsURL.isEmpty) => Some("http://" + interface + ":8080/")
      case None                       => None
    }

    httpURL ++ httpsURL
  }

}

import RestLoggerConfig._

/**
  * Rest API implementation for the logger config
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest")
trait RestLoggerConfig extends HasActorSystem {

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService

  implicit val ports: ServerPort

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix("loggerConfig") {
    getLoggerConfig
  } ~
    pathPrefix("serverurls") {
      getServerURL
    } ~
    pathPrefix("serverversion") {
      getServerVersion
    } ~
    pathPrefix("boardsetsandmovements") {
      getBoardSetsAndMovements
    }

  @Path("/loggerConfig")
  @GET
  @Operation(
    tags = Array("Server"),
    summary = "Get the logger config",
    operationId = "getLoggerConfig",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The logger config, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[LoggerConfig])
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[RestMessage])
          )
        )
      )
    )
  )
  def xxxgetLoggerConfig(): Unit = {}
  val getLoggerConfig: Route = pathEndOrSingleSlash {
    get {
      extractClientIP { ip =>
        {
          optionalHeaderValueByName("User-Agent") { userAgent =>
            val ips = ip.toString()
            RestLoggerConfig.log.fine(
              s"From ${ips} User-Agent: ${userAgent.getOrElse("<None>")}"
            )
            val iPad =
              userAgent.map(ua => ua.indexOf("iPad") >= 0).getOrElse(false)
            val config: LoggerConfig = restService.loggerConfig(ips, iPad)
            if (config == null) {
              RestLoggerConfig.log.warning(
                "Logger configuration not found for " + ips
              )
              complete(
                StatusCodes.NotFound,
                RestMessage("Logger config not found")
              )
            } else {
              RestLoggerConfig.log.info(
                "Logger configuration for " + ips + ": " + config
              )
              complete(
                StatusCodes.OK,
                config.copy(clientid = RestLoggerConfig.nextClientId)
              )
            }
          }
        }
      }
    }
  }

  @Path("/serverversion")
  @GET
  @Operation(
    tags = Array("Server"),
    summary = "Get the server version",
    operationId = "getServerVersion",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The server versions",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[ServerVersion])
            )
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[RestMessage])
          )
        )
      )
    )
  )
  def xxxgetServerVersion(): Unit = {}
  val getServerVersion: Route = pathEndOrSingleSlash {
    get {
      val serverversion = List(
        ServerVersion(
          "Server",
          VersionServer.version,
          VersionServer.builtAtString + " UTC"
        ),
        ServerVersion(
          "Shared",
          VersionShared.version,
          VersionShared.builtAtString + " UTC"
        ),
        ServerVersion(
          "Utilities",
          VersionUtilities.version,
          VersionUtilities.builtAtString + " UTC"
        )
      )
      complete(StatusCodes.OK, serverversion)
    }
  }

  @Path("/serverurls")
  @GET
  @Operation(
    tags = Array("Server"),
    summary = "Get the server URLs",
    operationId = "getServerURL",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The server URLs",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[ServerURL])
            )
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[RestMessage])
          )
        )
      )
    )
  )
  def xxxgetServerURL(): Unit = {}
  val getServerURL: Route = pathEndOrSingleSlash {
    get {
      val serverurl = List(serverURL(ports))
      complete(StatusCodes.OK, serverurl)
    }
  }

  @Path("/boardsetsandmovements")
  @GET
  @Operation(
    tags = Array("Duplicate"),
    summary = "Get the boardsets and movements",
    operationId = "getBoardSetsAndMovements",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The boardsets and movements",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema =
                new Schema(implementation = classOf[BoardSetsAndMovements])
            )
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[RestMessage])
          )
        )
      )
    )
  )
  def xxxgetBoardSetsAndMovements(): Unit = {}
  val getBoardSetsAndMovements: Route = pathEndOrSingleSlash {
    get {
      val fbs = restService.boardSets.readAll().map { r =>
        r match {
          case Right(bs) =>
            Result(bs.values.toList.sortWith((l, r) => l.name < r.name))
          case Left(error) =>
            log.fine(s"Error querying all boardsets, ignoring: ${error._2.msg}")
            Result(List.empty)
        }
      }
      val fmv = restService.movements.readAll().map { r =>
        r match {
          case Right(mv) =>
            Result(mv.values.toList.sortWith((l, r) => l.name < r.name))
          case Left(error) =>
            log.fine(s"Error querying all movements, ignoring: ${error._2.msg}")
            Result(List.empty)
        }
      }
      val fimv = restService.individualMovements.readAll().map { r =>
        r match {
          case Right(mv) =>
            Result(mv.values.toList.sortWith((l, r) => l.name < r.name))
          case Left(error) =>
            log.fine(s"Error querying all individual movements, ignoring: ${error._2.msg}")
            Result(List.empty)
        }
      }

      val r =
      for {
        rbs <- fbs.recover{
          case x: Exception =>
            log.fine(s"Exception querying all boardsets", x)
            Result(List.empty)
        }
        rmv <- fmv.recover{
          case x: Exception =>
            log.fine(s"Exception querying all movements", x)
            Result(List.empty)
        }
        rimv <- fimv.recover{
          case x: Exception =>
            log.fine(s"Exception querying all individual movements", x)
            Result(List.empty)
        }
      } yield {
        Result(
          List(
            BoardSetsAndMovements(
              rbs.getOrElse(List()),
              rmv.getOrElse(List()),
              rimv.getOrElse(List())
            )
          )
        )
      }
      resourceList(r)
    }
  }
}
