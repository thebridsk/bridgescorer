package com.github.thebridsk.bridge.server.service

import com.github.thebridsk.bridge.server.rest.Service
import akka.event.Logging.DebugLevel
import akka.event.Logging.ErrorLevel
import akka.event.Logging.LogLevel
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import akka.actor.ActorRef
import akka.io.Tcp
import akka.io.IO
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import com.github.thebridsk.bridge.server.backend.BridgeService
import java.util.Date
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.swagger.akka.model._
import akka.stream.ActorMaterializer
import akka.event.Logging
import akka.util.ByteString
import akka.event.LoggingAdapter
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.bridge.server.StartServer
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.webjar.WebJar
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.server.service.graphql.GraphQLRoute
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.ExternalDocumentation
import com.github.thebridsk.bridge.server.rest.ImportExport
import io.swagger.v3.jaxrs2.Reader
import com.github.swagger.akka.SwaggerHttpService
import io.swagger.v3.oas.integration.SwaggerConfiguration
import com.github.thebridsk.bridge.server.rest.RestDuplicate
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import java.util.TreeMap
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.core.util.Json
import scala.util.control.NonFatal
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI

/**
  * The service trait.
  * This trait defines our service behavior independently from the service actor,
  * this allows us to test this class without spinning up a server.
  */
trait MyService
    extends Service
    with JsService
    with WebJar
    with LoggingService
    with GraphQLRoute
    with HasActorSystem {
  hasActorSystem: HasActorSystem =>

  import hasActorSystem._

  val addLoggingAndServerToSwagger = true

  lazy val log = Logging(actorSystem, classOf[MyService])

  override lazy val cacheDuration = Duration("0s")

  def host = "loopback"
  def ports = ServerPort(None, None)

  val excptHandler = ExceptionHandler {
    case x: Throwable =>
      log.error(x, "Error processing a REST request")
      complete(
        (
          StatusCodes.InternalServerError,
          "Internal Server Error, please notify the system administrator!"
        )
      )
  }

  /**
    * Handler for converting rejections into HttpResponse
    */
  val totallyMissingHandler = RejectionHandler
    .newBuilder()
    .handle {
      case MissingCookieRejection(cookieName) =>
        complete(
          HttpResponse(
            StatusCodes.BadRequest,
            entity = "No cookies, no service!!!"
          )
        )
    }
    .handle {
      case AuthorizationFailedRejection =>
        complete(StatusCodes.Forbidden, "You're out of your depth!")
    }
    .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete(
        StatusCodes.MethodNotAllowed,
        s"Can't do that! Supported: ${names mkString " or "}!"
      )
    }
    .handle {
      case UnsupportedWebSocketSubprotocolRejection(protocol) =>
        complete(
          HttpResponse(StatusCodes.BadRequest, entity = "Protocol not accepted")
        )
    }
    .handleNotFound { complete(StatusCodes.NotFound, "Not here!myservice") }
    .result()

  val serverService = new ServerService(totallyMissingHandler)

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
  val myRouteWithLogging = handleExceptions(excptHandler) {
    handleRejections(totallyMissingHandler) {
//    logRequest(("topLevel", Logging.DebugLevel)) {
//      logResult(("topLevel", Logging.DebugLevel)) {
      logRouteWithIp
//      }
//    }
    }
  }

  /**
    * Spray routing with logging of incoming IP address.
    */
  val logRouteWithIp =
    extractClientIP { ip =>
      import CorsDirectives._
      pathPrefix("v1") {
        loggingRoute ~
          cors() {
            logRequest("logRouteWithIp", Logging.DebugLevel) {
              logResult("myRoute", Logging.DebugLevel) {
//            handleRejections(totallyMissingHandler) {
                serverService.serverRoute
//            }
              }
            }
          }
      } ~
        myRoute
    }

  /**
    * The main spray route of the service
    */
  val myRoute = handleRejections(totallyMissingHandler) {
    import CorsDirectives._
    encodeResponse {
      logRequest("myRoute", Logging.DebugLevel) {
        logResult("myRoute", Logging.DebugLevel) {
          cors() {
            graphQLRoute ~
              routeRest
          } ~ // for REST API of the service
            webjars ~
            swaggerRoute ~ // for the Swagger-UI documentation pages
            html // for the static html files for our application
        }
      }
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
      case Some(port) => Some(host + ":" + port)
      case None       => None
    }
    val httpURL = ports.httpPort match {
      case Some(port)                 => Some(host + ":" + port)
      case None if (httpsURL.isEmpty) => Some(host + ":8080")
      case None                       => None
    }

    httpsURL.getOrElse(httpURL.get)
  }

  def getScheme = ports.httpsPort.map(p => "HTTPS").getOrElse("HTTP")

  def getSchemeWS = ports.httpsPort.map(p => "WSS").getOrElse("WS")

  def getSchemes =
    ports.httpsPort.map(p => List("HTTPS")).getOrElse(Nil) :::
      ports.httpPort.map(p => List("HTTP")).getOrElse(Nil)

  def getSchemesWS = ports.httpsPort.map(p => List("WSS")).getOrElse(List("WS"))

  val x = classOf[LoggingService]

  val serverRestTypes = {
    (classOf[GraphQLRoute] :: restTypes.toList :::
      (if (addLoggingAndServerToSwagger) {
         List(
           classOf[LoggingService],
           classOf[ServerService]
         )
       } else {
         Nil
       })).toSet
  }

  /**
    * The Swagger-UI HttpService
    */
  val swaggerService = new MySwaggerService with HasActorSystem {
    implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    implicit lazy val materializer: ActorMaterializer =
      hasActorSystem.materializer

//    override val apiVersionURISegment = "v1"

    override def apiClasses = serverRestTypes

    // let swagger-ui determine the host and port
    override def host = getHostPort

    override def basePath: String = s"/${apiVersionURISegment}"

    override def apiDocsPath: String = "api-docs"

    override def info =
      Info(
        description =
          "Scorekeeper for a Duplicate bridge, Chicago bridge, and Rubber bridge.",
        version = "v1",
        title = "Duplicate Bridge Scorekeeper",
        termsOfService = "/public/termsOfService.html",
        contact = Some(
          Contact(
            name = "The Bridge Scorekeeper",
            url = "https://github.com/thebridsk/bridgescorer",
            email = ""
          )
        ),
        license = Some(License(name = "MIT", url = "/public/license.html")),
        vendorExtensions = Map.empty
      )

    override def components: Option[Components] = None

    override def schemes =
      getSchemes ::: (if (addLoggingAndServerToSwagger) getSchemesWS else Nil)

    override def security: List[SecurityRequirement] = List()

    override def securitySchemes: Map[String, SecurityScheme] = Map.empty

    override def externalDocs: Option[ExternalDocumentation] = None

    override def vendorExtensions: Map[String, Object] = Map.empty

    override def unwantedDefinitions: Seq[String] =
      Seq(
        // this is a akka.http.scaladsl.server.Route
        // RequestContext => Future[RouteResult]
        // This could be generated since all the CRUD calls return a Route
//          "Function1RequestContextFutureRouteResult",
      )

    def readerConfig = {
      val rc = new SwaggerConfiguration()
      rc.setCacheTTL(300000L)
      rc.setReadAllResources(false)
      rc.setPrettyPrint(true)
      rc
    }

    override def reader = {
      val r = new Reader(readerConfig.openAPI(swaggerConfig))
      r
    }

    private def myFilteredSwagger: OpenAPI = {
      import scala.collection.JavaConverters._
      val swagger: OpenAPI = reader.read(apiClasses.asJava)
      if (!unwantedDefinitions.isEmpty) {
        val filteredSchemas = asScala(swagger.getComponents.getSchemas)
          .filterKeys(
            definitionName => !unwantedDefinitions.contains(definitionName)
          )
          .toMap
          .asJava
        swagger.getComponents.setSchemas(new TreeMap(filteredSchemas))
      } else {
        swagger.getComponents.setSchemas(
          new TreeMap(swagger.getComponents.getSchemas)
        )
      }
      swagger
    }

    override def generateSwaggerJson: String = {
      try {
        Json.pretty().writeValueAsString(myFilteredSwagger)
      } catch {
        case NonFatal(t) => {
          log.error("Issue with creating swagger.json", t)
          throw t
        }
      }
    }

    override def generateSwaggerYaml: String = {
      try {
        Yaml.pretty().writeValueAsString(myFilteredSwagger)
      } catch {
        case NonFatal(t) => {
          log.error("Issue with creating swagger.yaml", t)
          throw t
        }
      }
    }

    /**
      * Add the ws scheme to the swagger config.
      * The SwaggerHttpService only supports setting one scheme
      */
    override def swaggerConfig = {
      import io.swagger.v3.oas.models.tags.Tag

      var s = super.swaggerConfig
//      s = if (addLoggingAndServerToSwagger) s.scheme(getSchemeWS)
//          else s
      val r = s
        .addTagsItem(
          new Tag().name("Duplicate").description("Duplicate bridge operations")
        )
        .addTagsItem(
          new Tag().name("Chicago").description("Chicage bridge operations")
        )
        .addTagsItem(
          new Tag().name("Rubber").description("Rubber bridge operations")
        )
//               .addTagsItem( new Tag().name("Utility").description("Utility operations") )
//               .addTagsItem( new Tag().name("Logging").description("Logging operations") )
        .addTagsItem(new Tag().name("Server").description("Server operations"))
      r
    }
  }

}

trait ShutdownHook {
  import scala.concurrent.duration._
  import scala.language.postfixOps
  def terminateServerIn(duration: Duration = 10 seconds): Future[_]

}

object MyService {
  var shutdownHook: Option[ShutdownHook] = None
}
