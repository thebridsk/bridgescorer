package com.example.rest

import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.example.util.HasActorSystem
import com.example.backend.BridgeService
import scala.reflect.runtime.universe._
import akka.actor.ActorRefFactory
import akka.event.Logging._
import utils.logging.Logging
import java.util.logging.Level
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.MethodRejection
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.example.backend.MonitorWebservice
import java.util.Date
import client.LogA
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.DateTime
import java.util.Formatter
import utils.logging.Logger
import com.example.data.websocket.DuplexProtocol
import utils.logging.TraceLevel
import akka.http.scaladsl.model.HttpHeader
import com.example.data.RestMessage
import com.example.backend.DuplicateMonitorWebservice
import com.example.backend.ChicagoMonitorWebservice
import com.example.backend.RubberMonitorWebservice

//import akka.event.LoggingAdapter


trait Service extends ImportExport {
  hasActorSystem: HasActorSystem =>

  def ports: ServerPort

  //
  // To add a new resource, xxx, the following must be done:
  // 1. Create a case class called Xxx in the shared (js,jvm) project
  //    that has all the fields for the rest api in the com.example.data
  //    package
  // 2. To the JsonSupport.scala file add an implicit jsonFormat&lt.n>(Xxx)
  //    line to the file, like the others.
  // 3. Create a trait that extends HttpService called RestXxx
  //    a. add a route def that has the spray route for the resource
  //    b. follow the format of other rest resource files
  // 4. to the restTypes variable, add a typeOf[RestXxx] for the resource.
  // 5. add a new val called restXxx that instantiates the class
  // 6. in the definition for routeRest, add a ~restXxx.route
  //

  val restTypes: Seq[Class[_]]
                = Seq(classOf[RestBoardSet],
                      classOf[RestChicago],
                      classOf[RestRubber],
                      classOf[RestLoggerConfig],
                      classOf[RestDuplicate],
                      classOf[RestDuplicateResult],
                      classOf[RestDuplicateSummary],
                      classOf[RestNestedBoard],
                      classOf[RestNestedHand],
                      classOf[RestNestedTeam],
                      classOf[RestSuggestion],
                      classOf[RestNames],
                      classOf[RestBoardSet],
                      classOf[RestMovement],
                      classOf[DuplicateMonitorWebservice],
                      classOf[ChicagoMonitorWebservice],
                      classOf[RubberMonitorWebservice],
                      classOf[ImportExport]
                     )

  implicit val restService: BridgeService

  object restMovement extends RestMovement {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  object restBoardSet extends RestBoardSet {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  object restChicago extends RestChicago {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  object restRubber extends RestRubber {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  val restDuplicate = new RestDuplicate {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  val restDuplicateResult = new RestDuplicateResult {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  val restDuplicateSummary = new RestDuplicateSummary {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  val restSuggestion = new RestSuggestion {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  object restNames extends RestNames {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
  }
  object duplicateMonitor extends DuplicateMonitorWebservice(totallyMissingResourceHandler)
  object chicagoMonitor extends ChicagoMonitorWebservice(totallyMissingResourceHandler)
  object rubberMonitor extends RubberMonitorWebservice(totallyMissingResourceHandler)

  object restLoggerConfig extends RestLoggerConfig {
    override implicit lazy val actorSystem: ActorSystem = hasActorSystem.actorSystem
    override implicit lazy val materializer: ActorMaterializer = hasActorSystem.materializer
    override implicit lazy val restService = hasActorSystem.restService
    val ports = hasActorSystem.ports
  }

  import UtilsPlayJson._

  /**
   * Handler for converting rejections into HttpResponse
   */
  def totallyMissingResourceHandler = RejectionHandler.newBuilder()
    .handle { case MalformedRequestContentRejection(errorMsg, ex) =>
//      logger.warning("Oops: "+errorMsg, ex)
      complete( StatusCodes.BadRequest, RestMessage(errorMsg))
    }
    .handleAll[MethodRejection] { rejections =>
      val methods = rejections map (_.supported)
      lazy val names = methods map (_.name) mkString ", "

      respondWithHeader(Allow(methods)) {
        options {
          complete(StatusCodes.OK, "")
        } ~
        complete(StatusCodes.MethodNotAllowed, s"HTTP method not allowed, supported methods: $names!")
      }
    }
    .handleNotFound { complete(StatusCodes.NotFound, RestMessage("Not found, code=service")) }
    .result()


  def rejectionHandler =
    RejectionHandler.newBuilder().handleAll[MethodRejection] { rejections =>
      val methods = rejections map (_.supported)
      lazy val names = methods map (_.name) mkString ", "

      respondWithHeader(Allow(methods)) {
        options {
          complete(s"Supported methods : $names.")
        } ~
        complete(StatusCodes.MethodNotAllowed, s"HTTP method not allowed, supported methods: $names!")
      }
    }
    .result()

  val routeRest =
    respondWithHeaders(`Cache-Control`( `no-cache`, `no-store`, `must-revalidate`),
                     RawHeader("Pragma","no-cache"),
                     Expires(DateTime(0))    // RawHeader("Expires","0")
      ) {
      pathPrefix("v1") {
        pathPrefix("rest") {
          logRequestResult("Service.routeRest") {
            handleRejections(totallyMissingResourceHandler) {
              restLoggerConfig.route ~
              restBoardSet.route ~ restMovement.route ~ restChicago.route ~
              restDuplicate.route ~ restDuplicateResult.route ~ restDuplicateSummary.route ~
              restNames.route ~ restRubber.route ~ restSuggestion.route // ~ restMyLogging.route
            }
          }
        } ~
        duplicateMonitor.route ~
        chicagoMonitor.route ~
        rubberMonitor.route ~
        importExportRoute
      }
    }
}

object Service {

  val log = Logger[Service]
  val clientlog = Logger[client.LogA]

  private val format = new java.text.SimpleDateFormat("HH:mm:ss.SSS")

  def logStringFromBrowser( ips: String, msg: String ): Unit = {
    clientlog.info(s"ClientLog($ips) $msg")
  }

  def formatMsg( msg: String, args: String* ) = {
    if (args.length == 0) msg
    else {
      val b = new java.lang.StringBuilder()
      val f = new Formatter(b)
      val a = args.asInstanceOf[Object]
      f.format(msg, args:_*)
      b.toString()
    }
  }

  def toLevel( level: String ) = {
    level match {
      case "E" => Level.SEVERE
      case "W" => Level.WARNING
      case "I" => Level.INFO
      case "C" => Level.CONFIG
      case "1" => Level.FINE
      case "2" => Level.FINER
      case "3" => Level.FINEST
      case "O" => TraceLevel.STDOUT
      case "R" => TraceLevel.STDERR
      case _ => Level.WARNING
    }

  }

  def logFromBrowser( ips: String, src: String, e: DuplexProtocol.LogEntryV2 ): Unit = {
    val format = new java.text.SimpleDateFormat("HH:mm:ss.SSS")
    val ts = format.format(new Date(e.timestamp.toLong))
    val level = e.level
    val position = e.position
    val url = e.url
    val message = e.message
    val cause = e.cause
    val args = e.args
    val msg = formatMsg(message,args:_*)
    val lev = toLevel(level)
    val clientid = e.clientid.getOrElse("")
    clientlog.log(lev,s"$ts ClientLog($ips,$clientid) $src $level $position $url $msg $cause")
  }

  def extractHostPort: HttpHeader => Option[String] = {
    case h: `Host` => Some(h.host.address()+":"+h.port)
    case x         => None
  }

}
