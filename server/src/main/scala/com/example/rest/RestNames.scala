package com.example.rest

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
import scala.util.Success
import scala.util.Failure


/**
 * Rest API implementation for the logger config
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path("/rest/names")
@Api(tags= Array("Utility"), description = "Getting all known names.", produces="application/json", protocols="http, https")
trait RestNames extends HasActorSystem {

  /**
   * The bridge service backend
   */
  implicit val restService: BridgeService

  import UtilsPlayJson._

  /**
   * spray route for all the methods on this resource
   */
  def route = pathPrefix("names") {
    getNames
  }

  @ApiOperation(value = "Get all known names", notes = "", response=classOf[String], responseContainer="List", nickname = "getNames", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The names as a list of string objects", response=classOf[String], responseContainer="List")
  ))
  def getNames = pathEndOrSingleSlash {
    get {
      resourceList( restService.getAllNames() )
    }
  }
}
