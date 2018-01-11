package com.example.service

import com.example.rest.Service
import akka.event.Logging.DebugLevel
import akka.event.Logging.ErrorLevel
import akka.event.Logging.LogLevel
import com.example.backend.BridgeServiceInMemory
import akka.actor.ActorRef
import akka.io.Tcp
import akka.io.IO
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import com.example.backend.BridgeService
import java.util.Date
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import com.example.util.HasActorSystem
import com.github.swagger.akka.model._
import akka.stream.ActorMaterializer
import akka.event.Logging
import akka.util.ByteString
import akka.event.LoggingAdapter
import io.swagger.config.FilterFactory
import com.example.Server
import com.example.StartServer
import scala.concurrent.Future
import com.example.webjar.WebJar
import io.swagger.models.Scheme
import com.example.rest.ServerPort
import io.swagger.models.Tag

/**
 * The service trait.
 * This trait defines our service behavior independently from the service actor,
 * this allows us to test this class without spinning up a server.
 */
trait MyService extends Service with JsService with WebJar with LoggingService with ServerService with HasActorSystem {
  hasActorSystem: HasActorSystem =>

// This is commented out because the baseURL in swagger.json is /v1/rest
// which causes these to not be found when trying it out.
  val addLoggingAndServerToSwagger = true

  lazy val log = Logging(actorSystem, classOf[MyService])

  override
  lazy val cacheDuration = Duration("0s")

  def host = "loopback"
  def ports = ServerPort( None, None )

  val excptHandler = ExceptionHandler {
    case x: Throwable =>
      log.error(x, "Error processing a REST request")
      complete((StatusCodes.InternalServerError, "Internal Server Error, please notify the system administrator!"))
  }

  /**
   * Handler for converting rejections into HttpResponse
   */
  def totallyMissingHandler = RejectionHandler.newBuilder()
    .handle { case MissingCookieRejection(cookieName) =>
      complete(HttpResponse(StatusCodes.BadRequest, entity = "No cookies, no service!!!"))
    }
    .handle { case AuthorizationFailedRejection =>
      complete(StatusCodes.Forbidden, "You're out of your depth!")
    }
    .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete(StatusCodes.MethodNotAllowed, s"Can't do that! Supported: ${names mkString " or "}!")
    }
    .handle { case UnsupportedWebSocketSubprotocolRejection( protocol ) =>
      complete(HttpResponse(StatusCodes.BadRequest, entity = "Protocol not accepted"))
    }
    .handleNotFound { complete(StatusCodes.NotFound, "Not here!myservice") }
    .result()

  val myRouteWithLoggingDebugging =
//    logRequest(("topLevel", Logging.DebugLevel)) {
//      logResult(("topLevel", Logging.DebugLevel)) {
//        extractClientIP { ip =>
        headerValueByName("Remote-Address") { ip =>
          pathEndOrSingleSlash {
            redirect("/public", StatusCodes.PermanentRedirect)
          }
        }
//      }
//    }

  /**
   * Spray routing which logs all requests and responses at the Debug level.
   */
  def myRouteWithLogging = handleExceptions(excptHandler) {
//    logRequest(("topLevel", Logging.DebugLevel)) {
//      logResult(("topLevel", Logging.DebugLevel)) {
        logRouteWithIp
//      }
//    }
  }

  /**
   * Spray routing with logging of incoming IP address.
   */
  def logRouteWithIp =
    extractClientIP { ip =>
      {
        pathPrefix("v1") {
          loggingRoute ~ serverRoute
        } ~
        myRoute
      }
    }

  /**
   * The main spray route of the service
   */
  def myRoute = handleRejections(totallyMissingHandler) {
      encodeResponse {
        logRequest("myRoute", Logging.DebugLevel) { logResult("myRoute", Logging.DebugLevel) {
          routeRest  ~        // for REST API of the service
          webjars ~
          swaggerRoute ~      // for the Swagger-UI documentation pages
          html                // for the static html files for our application
        }}
      } // ~
//      html            // for the static html files for our application
  }

  //
  // need to define this here, otherwise I get a name
  // collision when I try to use the actorRefFactory variable
  // in the definition of swaggerService.
  // This can be removed if I figure out how to reference
  // the parent classes actorRefFactory in the definition of
  // swaggerService.
  //
//  private val myActorRefFactory = actorRefFactory

  /**
   * The spray route for the Swagger-UI
   */
  private def swaggerRoute = swaggerService.swaggerRoute

//  implicit val mysystem: ActorSystem = actorSystem
//  implicit val xxMaterializer: ActorMaterializer = materializer

  def getHostPort = {
    val httpsURL = ports.httpsPort match {
      case Some(port) => Some(host+":"+port)
      case None => None
    }
    val httpURL = ports.httpPort match {
      case Some(port) => Some(host+":"+port)
      case None if (httpsURL.isEmpty) => Some(host+":8080")
      case None => None
    }

    httpsURL.getOrElse( httpURL.get )
  }

  def getScheme = ports.httpsPort.map( p => Scheme.HTTPS ).getOrElse(Scheme.HTTP)

  def getSchemeWS = ports.httpsPort.map( p => Scheme.WSS ).getOrElse(Scheme.WS)

  def getSchemes = ports.httpsPort.map( p => List(Scheme.HTTPS) ).getOrElse(Nil):::
                   ports.httpPort.map( p => List(Scheme.HTTP) ).getOrElse(Nil)

  def getSchemesWS = ports.httpsPort.map( p => List(Scheme.WSS) ).getOrElse(List(Scheme.WS))

  val x = classOf[LoggingService]

  val serverRestTypes = ( restTypes.toList:::
                          (if (addLoggingAndServerToSwagger) {
                            List(
                                  classOf[LoggingService],
                                  classOf[ServerService]
                                )
                          } else {
                            Nil
                          })
                        ).toSet

  /**
   * The Swagger-UI HttpService
   */
  val swaggerService = new MySwaggerService with HasActorSystem {
    implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer

//    override val scheme = getScheme
    override val schemes = getSchemes ::: (if (addLoggingAndServerToSwagger) getSchemesWS else Nil)
    override val host = "" // getHostPort

    override val apiClasses = serverRestTypes
    override val apiVersionURISegment = "v1"

    // let swagger-ui determine the host and port
    override val basePath: String = s"/${apiVersionURISegment}"
//    override def docsPath = "api-docs"     // URI will be /v1/api-docs
//    override def actorRefFactory = myActorRefFactory
    override val info = Info("Scorekeeper for a 2 table duplicate bridge match.",
                                            "v1",
                                            "Duplicate Bridge Scorekeeper",
                                            "/public/termsOfService.html",
                                            Some(Contact("Wolfgang Segmuller","","<werewolves@example.net>")),
                                            Some(License("Free","/public/license.html")))

    //authorizations, not used

    /**
     * Add the ws scheme to the swagger config.
     * The SwaggerHttpService only supports setting one scheme
     */
    override
    def swaggerConfig = {
      var s = super.swaggerConfig
//      s = if (addLoggingAndServerToSwagger) s.scheme(getSchemeWS)
//          else s
      s.tag( new Tag().name("Duplicate").description("Duplicate bridge operations") )
       .tag( new Tag().name("Chicago").description("Chicage bridge operations") )
       .tag( new Tag().name("Rubber").description("Rubber bridge operations") )
       .tag( new Tag().name("Utility").description("Utility operations") )
       .tag( new Tag().name("Logging").description("Logging operations") )
       .tag( new Tag().name("Server").description("Server operations") )
    }
  }

}

trait ShutdownHook {
  import scala.concurrent.duration._
  import scala.language.postfixOps
  def terminateServerIn( duration: Duration = 10 seconds ): Future[_]

}

object MyService {
  var shutdownHook: Option[ShutdownHook] = None
}
