package com.github.thebridsk.bridge.server.webjar

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model._
import com.github.thebridsk.utilities.logging.Logger
import akka.event.Logging
import com.github.thebridsk.bridge.data.RestMessage
import scala.concurrent.duration.Duration

trait WebJar {

  private val logger = Logger(getClass.getName, null)

  lazy val cacheDuration = Duration("0s")

  private lazy val cacheHeaders = {
    import akka.http.scaladsl.model.headers._
    import akka.http.scaladsl.model.headers.CacheDirectives._
    val sec = cacheDuration.toSeconds
    if (sec <= 0) {
      Seq(
        `Cache-Control`(`no-cache`, `no-store`, `must-revalidate`),
        RawHeader("Pragma", "no-cache"),
        Expires(DateTime(0)) // RawHeader("Expires","0")
      )
    } else {
      Seq(
        `Cache-Control`(`max-age`(sec), `public`)
      )
    }
  }

//  private def totallyMissingFileHandler( where: String ) = RejectionHandler.newBuilder()
//    .handle { case MissingCookieRejection(cookieName) =>
//      complete(HttpResponse(StatusCodes.BadRequest, entity = "No cookies, no service!!!"))
//    }
//    .handle { case AuthorizationFailedRejection =>
//      complete(StatusCodes.Forbidden, "You're out of your depth!")
//    }
//    .handleAll[MethodRejection] { methodRejections =>
//      val names = methodRejections.map(_.supported.name)
//      complete(StatusCodes.MethodNotAllowed, s"Can't do that! Supported: ${names mkString " or "}!")
//    }
//    .handleNotFound { complete(StatusCodes.NotFound, "Not here!"+where) }
//    .result()

  val webjarsResources
      : List[FileFinder] = Nil // new FileFinder( "org.webjars", "swagger-ui" ) :: Nil

//  val faviconResources = ResourceFinder.htmlResources

//  val favicon = """favicon-\d+x\d+\.png""".r

  import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

  /**
    * The spray route for the js static files
    */
  val webjars = logRequest("webjars", Logging.InfoLevel) {
    logResult("webjars", Logging.InfoLevel) {
      pathPrefix("libs") {
//      pathPrefix("public") {
//        extractUnmatchedPath { path =>
//          path.toString() match {
//            case favicon() =>
//              getFromResourceDirectory(faviconResources.baseName)
//            case _ =>
//              complete( StatusCodes.NotFound, "Resource not found" )
//          }
//        }
//      } ~
        path("""[^/]+""".r / Remaining) { (artifactid, resource) =>
          logger.fine(
            "Looking for webjars " + artifactid + " resource " + resource
          )
          respondWithHeaders(cacheHeaders) {
            webjarsResources.find { x =>
              x.isArtifact(artifactid)
            } match {
              case Some(ff) =>
                ff.getResource(resource.toString()) match {
                  case Some(r) =>
//                  handleRejections(totallyMissingFileHandler("webjars")) {
                    logger.fine("Getting webjars resource for " + r)
                    getFromResource(r)
//                  }
                  case None =>
                    complete(
                      StatusCodes.NotFound,
                      RestMessage(
                        "Unable to find resource /webjars/" + artifactid + "/" + resource
                      )
                    )
                }
              case None =>
                complete(
                  StatusCodes.NotFound,
                  RestMessage("Unable to find resource /webjars/" + artifactid)
                )
            }
          }
        }
      }
    }
  }

}
