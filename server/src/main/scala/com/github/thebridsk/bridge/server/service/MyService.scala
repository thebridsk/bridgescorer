package com.github.thebridsk.bridge.server.service

import com.github.thebridsk.bridge.server.rest.Service
import akka.actor.ActorSystem
import scala.concurrent.duration._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.swagger.akka.model._
import akka.event.Logging
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.webjar.WebJar
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.server.service.graphql.GraphQLRoute
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.oas.integration.SwaggerConfiguration
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import java.util.TreeMap
import io.swagger.v3.core.util.Json
import scala.util.control.NonFatal
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.server.rest.UtilsPlayJson
import akka.event.LoggingAdapter

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

  val addLoggingAndServerToSwagger = true

  lazy val log: LoggingAdapter = Logging(actorSystem, classOf[MyService])

  override lazy val cacheDuration: Duration = Duration("0s")

  def listenInterface: List[String] = List()
  def host = "loopback"
  def ports: ServerPort = ServerPort(None, None)

  val excptHandler: ExceptionHandler = ExceptionHandler {
    case x: IllegalArgumentException =>
      log.warning(s"MyService: Illegal argument in request: ${x.getMessage()}")
      import UtilsPlayJson._
      complete(
        StatusCodes.BadRequest,
        RestMessage(s"Illegal argument in request: ${x.getMessage()}")
      )
    case x: Throwable =>
      log.error(x, "Error processing a REST request")
      import UtilsPlayJson._
      complete(
        StatusCodes.InternalServerError,
        RestMessage(
          "Internal Server Error, please notify the system administrator!"
        )
      )
  }

  /**
    * Handler for converting rejections into HttpResponse
    */
  val totallyMissingHandler: RejectionHandler = RejectionHandler
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

  val serverService = new ServerService(totallyMissingHandler) {
    val listenInterface = MyService.this.listenInterface
  }

  val myRouteWithLoggingDebugging: RequestContext => Future[RouteResult] =
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
  val myRouteWithLogging: Route = {
    import CorsDirectives._
    cors() {
      handleExceptions(excptHandler) {
        handleRejections(totallyMissingHandler) {
          // logRequest(("topLevel", Logging.DebugLevel)) {
          //   logResult(("topLevel", Logging.DebugLevel)) {
          logRouteWithIp
          //   }
          // }
        }
      }
    }
  }

  /**
    * Spray routing with logging of incoming IP address.
    */
  val logRouteWithIp: RequestContext => Future[RouteResult] =
    extractClientIP { ip =>
      import CorsDirectives._
      pathPrefix("v1") {
        loggingRoute ~
          cors() {
            logRequest(s"logRouteWithIp from ${ip}", Logging.DebugLevel) {
              logResult(s"logRouteWithIp from ${ip}", Logging.DebugLevel) {
//            handleRejections(totallyMissingHandler) {
                serverService.serverRoute
//            }
              }
            }
          }
      } ~
        myRoute
    }

  val certhttppath: Option[Route] = None

  /**
    * The main spray route of the service
    */
  val myRoute: Route = handleRejections(totallyMissingHandler) {
    import CorsDirectives._
    logRequest("myRoute", Logging.DebugLevel) {
      logResult("myRoute", Logging.DebugLevel) {
        encodeResponse {
          cors() {
            graphQLRoute ~
              routeRest
          } ~ // for REST API of the service
            (List(
              webjars,
              swaggerRoute, // for the Swagger-UI documentation pages
              html // for the static html files for our application
            ) ::: certhttppath.toList).reduceLeft((ac, v) => ac ~ v)
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

  def getHostPort: String = {
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

  def getScheme: String = ports.httpsPort.map(p => "HTTPS").getOrElse("HTTP")

  def getSchemeWS: String = ports.httpsPort.map(p => "WSS").getOrElse("WS")

  def getSchemes: List[String] =
    ports.httpsPort.map(p => List("HTTPS")).getOrElse(Nil) :::
      ports.httpPort.map(p => List("HTTP")).getOrElse(Nil)

  def getSchemesWS: List[String] =
    ports.httpsPort.map(p => List("WSS")).getOrElse(List("WS"))

  val x: Class[LoggingService] = classOf[LoggingService]

  val serverRestTypes: Set[Class[_]] = {
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
  val swaggerService: swaggerService = new swaggerService
  class swaggerService extends MySwaggerService with HasActorSystem {
    implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem

//    override val apiVersionURISegment = "v1"

    override def apiClasses = serverRestTypes

    // let swagger-ui determine the host and port
    override def host = getHostPort

    override def basePath: String = s"/${apiVersionURISegment}"

    override def apiDocsPath: String = "api-docs"

    override def info: Info =
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

    override def schemes: List[String] =
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

    def readerConfig: SwaggerConfiguration = {
      val rc = new SwaggerConfiguration()
      rc.setCacheTTL(300000L)
      rc.setReadAllResources(false)
      rc.setPrettyPrint(true)
      rc
    }

    override def reader: Reader = {
      val r = new Reader(readerConfig.openAPI(swaggerConfig))
      r
    }

    private def myFilteredSwagger: OpenAPI = {
      import scala.jdk.CollectionConverters._
      val swagger: OpenAPI = reader.read(apiClasses.asJava)
      if (!unwantedDefinitions.isEmpty) {
        val filteredSchemas = asScala(swagger.getComponents.getSchemas).view
          .filterKeys(definitionName =>
            !unwantedDefinitions.contains(definitionName)
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
    override def swaggerConfig: OpenAPI = {
      import io.swagger.v3.oas.models.tags.Tag

      var s = super.swaggerConfig
//      s = if (addLoggingAndServerToSwagger) s.scheme(getSchemeWS)
//          else s
      val r = s
        .addTagsItem(
          new Tag().name("Duplicate").description("Duplicate bridge operations")
        )
        .addTagsItem(
          new Tag().name("IndividualDuplicate").description("Individual duplicate bridge operations")
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
