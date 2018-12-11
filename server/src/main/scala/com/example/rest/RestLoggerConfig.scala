package com.example.rest

import com.example.data.LoggerConfig
import com.example.data.Ack
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.example.util.HasActorSystem
import java.util.Date
import com.example.backend.BridgeService
import javax.ws.rs.Path
import com.example.data.RestMessage
import com.example.data.ServerURL
import com.example.Server
import org.rogach.scallop.exceptions.IncompleteBuildException
import com.example.data.ServerVersion
import com.example.version.VersionServer
import com.example.version.VersionShared
import com.example.utilities.version.VersionUtilities
import utils.logging.Logger
import com.example.data.BoardSetsAndMovements
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.backend.resource.Result
import scala.util.Success
import scala.util.Failure
import com.example.backend.resource.Implicits
import java.util.concurrent.atomic.AtomicInteger

case class ServerPort( httpPort: Option[Int], httpsPort: Option[Int] )

object RestLoggerConfig {
  val log = Logger( getClass.getName )

  private val pclientid = new AtomicInteger()

  def nextClientId = Some(pclientid.incrementAndGet().toString())
}

/**
 * Rest API implementation for the logger config
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path("/rest")
@Api(tags= Array("Utility"), description = "Utility operations.", produces="application/json", protocols="http, https")
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
  def route = pathPrefix( "loggerConfig") {
    getLoggerConfig
  } ~
  pathPrefix( "serverurls" ) {
    getServerURL
  } ~
  pathPrefix( "serverversion" ) {
    getServerVersion
  } ~
  pathPrefix( "boardsetsandmovements" ) {
    getBoardSetsAndMovements
  }

  @Path("/loggerConfig")
//  @Api(tags= Array("utility"), description = "Logger configuration", produces="application/json")
  @ApiOperation(value = "Get the logger config", response=classOf[LoggerConfig], notes = "", nickname = "getLoggerConfig", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The logger config, as a JSON object", response=classOf[LoggerConfig]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getLoggerConfig = pathEndOrSingleSlash {
    get {
      extractClientIP { ip => {
        optionalHeaderValueByName("User-Agent") { userAgent =>
          val ips = ip.toString()
          RestLoggerConfig.log.fine(s"From ${ips} User-Agent: ${userAgent.getOrElse("<None>")}")
          val iPad = userAgent.map( ua=> ua.indexOf("iPad") >= 0 ).getOrElse(false)
          val config: LoggerConfig = restService.loggerConfig(ips, iPad)
          if (config == null) {
            RestLoggerConfig.log.warning("Logger configuration not found for "+ips)
            complete(StatusCodes.NotFound,RestMessage("Logger config not found"))
          } else {
            RestLoggerConfig.log.info("Logger configuration for "+ips+": "+config)
            complete(StatusCodes.OK, config.copy(clientid=RestLoggerConfig.nextClientId))
          }
        }
      }}
    }
  }

  @Path("/serverversion")
//  @Api(tags= Array("utility"), description = "Server versions", produces="application/json")
  @ApiOperation(value = "Get the server version", notes = "", response=classOf[ServerVersion], responseContainer="List", nickname = "getServerVersion", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The server version", response=classOf[ServerVersion], responseContainer="List"),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getServerVersion = pathEndOrSingleSlash {
    get {
      val serverversion = List(
                            ServerVersion( "Server", VersionServer.version, VersionServer.builtAtString+" UTC"),
                            ServerVersion( "Shared", VersionShared.version, VersionShared.builtAtString+" UTC"),
                            ServerVersion( "Utilities", VersionUtilities.version, VersionUtilities.builtAtString+" UTC")
                          )
      complete(StatusCodes.OK, serverversion)
    }
  }

  @Path("/serverurls")
//  @Api(tags= Array("utility"), description = "Server URLs", produces="application/json")
  @ApiOperation(value = "Get the server URL", notes = "", response=classOf[ServerURL], responseContainer="List", nickname = "getServerURL", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The server URLs", response=classOf[ServerURL], responseContainer="List"),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getServerURL = pathEndOrSingleSlash {
    get {
      val serverurl = List(serverURL())
      complete(StatusCodes.OK, serverurl)
    }
  }

  @Path("/boardsetsandmovements")
//  @Api(tags= Array("utility"), description = "Server URLs", produces="application/json")
  @ApiOperation(value = "Get the boardsets and movements", notes = "", response=classOf[BoardSetsAndMovements], responseContainer="List", nickname = "getBoardsetsAndMovements", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The boardsets and movements", response=classOf[BoardSetsAndMovements], responseContainer="List"),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getBoardSetsAndMovements = pathEndOrSingleSlash {
    get {
      val fbs = restService.boardSets.readAll().map { r =>
        r match {
          case Right(bs) => Result(bs.values.toList.sortWith((l,r)=>l.name < r.name ))
          case Left(error) => Result(error)
        }
      }
      val fmv = restService.movements.readAll().map { r =>
        r match {
          case Right(mv) => Result(mv.values.toList.sortWith((l,r)=>l.name < r.name ))
          case Left(error) => Result(error)
        }
      }
      import Implicits._
      onComplete(fbs) {
        case Success(rbs) =>
          onComplete(fmv) {
            case Success(rmv) =>
              if (rbs.isOk && rmv.isOk) {
                val bm = List(BoardSetsAndMovements(rbs.right.get,rmv.right.get))
                complete( StatusCodes.OK, bm )
              } else {
                val (code,msg) = rbs.left.getOrElse( rmv.left.getOrElse( (StatusCodes.InternalServerError, RestMessage("Unknown error")) ))
                complete( code, msg )
              }
            case Failure(ex) =>
              RestLoggerConfig.log.info("Exception getting boardsets and movements: ", ex)
              complete( StatusCodes.InternalServerError, "Internal server error" )
          }
        case Failure(ex) =>
          RestLoggerConfig.log.info("Exception getting boardsets and movements: ", ex)
          complete( StatusCodes.InternalServerError, "Internal server error" )
      }

    }
  }

  def serverURL(): ServerURL = {
    import java.net.NetworkInterface
    import scala.collection.JavaConverters._
    import java.net.Inet4Address

    val x = NetworkInterface.getNetworkInterfaces.asScala.
      filter { x => x.isUp() && !x.isLoopback() }.
      flatMap { ni => ni.getInetAddresses.asScala }.
      filter { x => x.isInstanceOf[Inet4Address] }.
      map { x => getURLs(x.getHostAddress).toList }.
      flatten.toList

    ServerURL( x )

  }

  /**
   * Get the URLs for an interface
   * @param interface the interface IP address for the URLs
   * @return A list of URLs
   */
  def getURLs( interface: String ) = {
    val httpsURL = ports.httpsPort match {
      case Some(port) =>
        if (port==443) Some("https://"+interface+"/")
        else Some("https://"+interface+":"+port+"/")
      case None => None
    }
    val httpURL = ports.httpPort match {
      case Some(port) =>
        if (port==80) Some("http://"+interface+"/")
        else Some("http://"+interface+":"+port+"/")
      case None if (httpsURL.isEmpty) => Some("http://"+interface+":8080/")
      case None => None
    }

    httpURL ++ httpsURL
  }
}
