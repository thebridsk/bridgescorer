package com.example.service

import com.github.swagger.akka.SwaggerHttpService
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import com.example.util.HasActorSystem
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import java.util.Properties
import akka.event.Logging
import io.swagger.models.Scheme
import scala.concurrent.duration.Duration

//import io.swagger.util.Json
//import com.fasterxml.jackson.databind.SerializationConfig
//import com.fasterxml.jackson.databind.MapperFeature

/**
 * Extends the SwaggerHttpService with the spray route
 * for the static pages and the swagger dynamic pages.
 * <p>
 * The static pages are served up with the /v1/docs/... URI.
 * The swagger dynamic pages are served up with a
 * /v1/&lt.{@link SwaggerHttpService#docsPath docsPath}> URI.
 * <p>
 * The index.html file to bring up the page must be on the
 * classpath at the web/swagger-ui/index.html resource.
 * The rest of the Swagger-UI's static pages are served up
 * from the classpath at the web/swagger-ui/ directory.
 */
trait MySwaggerService extends SwaggerHttpService {
  this: HasActorSystem =>

  // does not work.  Trying to alphabetize swagger.json properties
  // Json.mapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)

  lazy val log = Logging(actorSystem, classOf[MySwaggerService])

  val apiVersionURISegment = "v1"

  def classLoader: ClassLoader = classOf[MySwaggerService].getClassLoader

//  val swaggerVersion = {
//    val is = classLoader.getResourceAsStream("META-INF/maven/org.webjars/swagger-ui/pom.properties")
//    val ver = if (is == null) None
//    else {
//      val p = new Properties()
//      try {
//        p.load(is)
//      } catch {
//        case x: Exception =>
//          log.error(x, "Unable to read swagger-ui pom.properties")
//      }
//      val sv = p.getProperty("version")
//      if (sv == null) None
//      else Some(sv)
//    }
//    log.info("Swagger UI version is "+ver)
//    ver
//  }

  // URI for the swagger GUI page
  val swaggergui = "/public/swagger-ui-dist/index.html.gz"

  // See http://swagger.io/docs/swagger-tools/#customization-36
  // for customization of the swagger ui

  // Parameters
  //    validatorUrl=    - (null) to not validate the swagger against
  //                           https://online.swagger.io/validator/debug?url=/v1/api-docs/swagger.json
  //    url=xxx          - URL to swagger.json
  def swaggerURL = swaggergui+"?url=/"+apiVersionURISegment+"/"+apiDocsPath+"/swagger.json&validatorUrl="

  def getSwaggerURL(): String = {
    log.info("SwaggerURL: "+swaggerURL)
    swaggerURL
  }

  lazy val swaggerCacheDuration = Duration("5m")

  lazy val swaggerCacheHeaders = {
    import akka.http.scaladsl.model.headers._
    import akka.http.scaladsl.model.headers.CacheDirectives._
    val sec = swaggerCacheDuration.toSeconds
    if (sec <= 0) {
      Seq(
        `Cache-Control`( `no-cache`, `no-store`, `must-revalidate`),
        RawHeader("Pragma","no-cache"),
        Expires(DateTime(0))    // RawHeader("Expires","0")
      )
    } else {
      Seq(
        `Cache-Control`( `max-age`( sec ), `public` )
      )
    }
  }

  def swaggerRoute =
      get {
        pathPrefix(apiVersionURISegment) {
          logRequest(("topLevel", Logging.DebugLevel)) {
          logResult(("topLevel", Logging.DebugLevel)) {
            pathPrefix("docs") {
              pathEndOrSingleSlash {
                redirect(getSwaggerURL(), StatusCodes.PermanentRedirect)
              } ~
              path("index.html") {
                redirect(getSwaggerURL(), StatusCodes.PermanentRedirect)
              }
            }
          }} ~
          pathPrefix( apiDocsPath ) {
            pathEndOrSingleSlash {
              redirect("/"+apiVersionURISegment+"/"+apiDocsPath+"/swagger.json", StatusCodes.PermanentRedirect)
            }
          } ~
          respondWithHeaders(swaggerCacheHeaders:_*) {
            routes
          }
        }
      }
}
