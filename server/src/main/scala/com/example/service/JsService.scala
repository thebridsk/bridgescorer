package com.example.service

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.headers.RawHeader
import com.example.webjar.FileFinder
import utils.logging.Logger
import java.util.logging.Level
import akka.event.Logging
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.CacheDirectives._
import com.example.data.RestMessage
import scala.concurrent.duration.Duration
import com.example.version.VersionServer
import scala.reflect.io.Directory
import scala.reflect.io.File
import scala.reflect.io.Path
import akka.event.LoggingAdapter
import scala.annotation.tailrec
import java.io.{File => JFile}


/**
 * HttpService for the static html and js pages.
 * The html pages are served up from the classpath at the
 * resource directory "html", and the URI is /html/...
 * The js pages are at the resource directory "js",
 * and the URI is /js/...
 */
trait JsService /* extends HttpService */ {

  val logger = Logger( getClass.getName, null )

  lazy val cacheDuration = Duration("0s")

  lazy val cacheHeaders = {
    import akka.http.scaladsl.model.headers._
    import akka.http.scaladsl.model.headers.CacheDirectives._
    val sec = cacheDuration.toSeconds
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

//  def totallyMissingFileHandler( where: String ) = RejectionHandler.newBuilder()
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

  val htmlResources = ResourceFinder.htmlResources

  {
    val res = htmlResources.baseName+"/bridgescorer-client-fastopt.js"
    val url = getClass.getClassLoader.getResource(res)
    logger.info("Found resource "+res+" at "+url.toString())
  }


  private def safeJoinPaths(base: String, path: Uri.Path, separator: Char = JFile.separatorChar): String = {
    import java.lang.StringBuilder
    @tailrec def rec(p: Uri.Path, result: StringBuilder = new StringBuilder(base)): String =
      p match {
        case Uri.Path.Empty       ⇒ result.toString
        case Uri.Path.Slash(tail) ⇒ rec(tail, result.append(separator))
        case Uri.Path.Segment(head, tail) ⇒
          if (head.indexOf('/') >= 0 || head.indexOf('\\') >= 0 || head == "..") {
            logger.warning(s"File-system path for base [${base}] and Uri.Path [${path}] contains suspicious path segment [${head}], " +
              "GET access was disallowed")
            ""
          } else rec(tail, result.append(head))
      }
    rec(if (path.startsWithSlash) path.tail else path)
  }

  /**
   * The spray route for the html static files
   */
  val html = {
    pathSingleSlash {
      redirect("/public/index.html", StatusCodes.PermanentRedirect)
    } ~
    pathPrefix("public") {
      respondWithHeaders(cacheHeaders:_*) {
        pathEndOrSingleSlash {
          redirect("/public/index.html", StatusCodes.PermanentRedirect)
        } ~
        getFromResourceDirectory(htmlResources.baseName) ~
        extractUnmatchedPath { path =>
          logger.info(s"Looking for file "+path)
          safeJoinPaths(htmlResources.baseName+"/", path, separator = '/') match {
            case ""           => reject
            case resourceName =>
              val resname = resourceName+".gz"
              logger.info(s"Looking for gzipped file as a resource "+resname)
              getFromResource(resname)
          }
        }
      }
    }
  }

}

