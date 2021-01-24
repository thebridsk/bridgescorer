package com.github.thebridsk.bridge.server.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.thebridsk.bridge.server.backend.BridgeService
import java.util.logging.Level
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.MethodRejection
import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.DateTime
import java.util.Formatter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import com.github.thebridsk.utilities.logging.TraceLevel
import akka.http.scaladsl.model.HttpHeader
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.server.backend.DuplicateMonitorWebservice
import com.github.thebridsk.bridge.server.backend.ChicagoMonitorWebservice
import com.github.thebridsk.bridge.server.backend.RubberMonitorWebservice
import java.time.Instant
import java.time.ZoneId
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.ValidationRejection
import akka.event.LoggingAdapter
import akka.event.Logging
import com.github.thebridsk.bridge.server.backend.IndividualDuplicateMonitorWebservice

//import akka.event.LoggingAdapter

trait Service extends ImportExport {
  hasActorSystem: HasActorSystem =>

  private lazy val log: LoggingAdapter = Logging(actorSystem, classOf[Service])

  def ports: ServerPort

  //
  // To add a new resource, xxx, the following must be done:
  // 1. Create a case class called Xxx in the shared (js,jvm) project
  //    that has all the fields for the rest api in the com.github.thebridsk.bridge.data
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

  val restTypes: Seq[Class[_]] = Seq(
    classOf[RestBoardSet],
    classOf[RestChicago],
    classOf[RestNestedChicagoRound],
    classOf[RestNestedChicagoRoundHand],
    classOf[RestRubber],
    classOf[RestNestedRubberHand],
    classOf[RestLoggerConfig],
    classOf[RestDuplicate],
    classOf[RestDuplicateResult],
    classOf[RestDuplicateSummary],
    classOf[RestDuplicatePlaces],
    classOf[RestNestedBoard],
    classOf[RestNestedHand],
    classOf[RestNestedTeam],
    classOf[RestNestedPicture],
    classOf[RestNestedPictureHand],
    classOf[RestSuggestion],
    classOf[RestNames],
    classOf[RestBoardSet],
    classOf[RestMovement],
    classOf[RestIndividualMovement],
    classOf[RestIndividualDuplicate],
    classOf[RestIndividualDuplicateSummary],
    classOf[RestNestedIndividualBoard],
    classOf[RestNestedIndividualHand],
    classOf[RestNestedPictureIndividual],
    classOf[RestNestedPictureIndividualHand],
    classOf[DuplicateMonitorWebservice],
    classOf[ChicagoMonitorWebservice],
    classOf[RubberMonitorWebservice],
    classOf[ImportExport]
  )

  implicit val restService: BridgeService

  object restMovement extends RestMovement {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService: BridgeService =
      hasActorSystem.restService
  }

  object restIndividualMovement extends RestIndividualMovement {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService: BridgeService =
      hasActorSystem.restService
  }

  object restBoardSet extends RestBoardSet {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService: BridgeService =
      hasActorSystem.restService
  }
  object restChicago extends RestChicago {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService: BridgeService =
      hasActorSystem.restService
  }
  object restRubber extends RestRubber {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService: BridgeService =
      hasActorSystem.restService
  }
  val restDuplicate: RestDuplicate = new RestDuplicate {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService = hasActorSystem.restService
  }
  val restIndividualDuplicate: RestIndividualDuplicate = new RestIndividualDuplicate {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService = hasActorSystem.restService
  }
  val restDuplicateResult: RestDuplicateResult = new RestDuplicateResult {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService = hasActorSystem.restService
  }
  val restDuplicateSummary: RestDuplicateSummary = new RestDuplicateSummary {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService = hasActorSystem.restService
  }
  val restIndividualDuplicateSummary: RestIndividualDuplicateSummary = new RestIndividualDuplicateSummary {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService = hasActorSystem.restService
  }
  val restDuplicatePlaces: RestDuplicatePlaces = new RestDuplicatePlaces {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService = hasActorSystem.restService
  }
  val restSuggestion: RestSuggestion = new RestSuggestion {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService = hasActorSystem.restService
  }
  object restNames extends RestNames {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService: BridgeService =
      hasActorSystem.restService
  }
  object duplicateMonitor
      extends DuplicateMonitorWebservice(totallyMissingResourceHandler, this)
  object individualDuplicateMonitor
      extends IndividualDuplicateMonitorWebservice(totallyMissingResourceHandler, this)
  object chicagoMonitor
      extends ChicagoMonitorWebservice(totallyMissingResourceHandler, this)
  object rubberMonitor
      extends RubberMonitorWebservice(totallyMissingResourceHandler, this)

  object restLoggerConfig extends RestLoggerConfig {
    implicit override lazy val actorSystem: ActorSystem =
      hasActorSystem.actorSystem
    implicit override lazy val restService: BridgeService =
      hasActorSystem.restService
    val ports = hasActorSystem.ports
  }

  import UtilsPlayJson._

  /**
    * Handler for converting rejections into HttpResponse
    */
  def totallyMissingResourceHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(errorMsg, ex) =>
          log.error(ex, s"Oops MalformedRequestContentRejection: ${errorMsg}")
          complete(StatusCodes.BadRequest, RestMessage(errorMsg))
      }
      .handle {
        case ValidationRejection(msg, ex) =>
          ex.map(log.error(_, s"Oops ValidationRejection: ${msg}")).
             getOrElse(log.error(s"Oops ValidationRejection: ${msg}"))
          complete(StatusCodes.BadRequest, "That wasn't valid! " + msg)
      }
      .handleAll[MethodRejection] { rejections =>
        val methods = rejections map (_.supported)
        lazy val names = methods map (_.name) mkString ", "

        respondWithHeader(Allow(methods)) {
          options {
            complete(StatusCodes.OK, "")
          } ~
            complete(
              StatusCodes.MethodNotAllowed,
              s"HTTP method not allowed, supported methods: $names!"
            )
        }
      }
      .handleNotFound {
        complete(StatusCodes.NotFound, RestMessage("Not found, code=service"))
      }
      .result()

  def rejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handleAll[MethodRejection] { rejections =>
        val methods = rejections map (_.supported)
        lazy val names = methods map (_.name) mkString ", "

        respondWithHeader(Allow(methods)) {
          options {
            complete(s"Supported methods : $names.")
          } ~
            complete(
              StatusCodes.MethodNotAllowed,
              s"HTTP method not allowed, supported methods: $names!"
            )
        }
      }
      .result()

  val routeRest: Route =
    respondWithHeaders(
      `Cache-Control`(`no-cache`, `no-store`, `must-revalidate`),
      RawHeader("Pragma", "no-cache"),
      Expires(DateTime(0)) // RawHeader("Expires","0")
    ) {
      pathPrefix("v1") {
        pathPrefix("rest") {
          logRequestResult("Service.routeRest") {
            handleRejections(totallyMissingResourceHandler) {
              restLoggerConfig.route ~
                restBoardSet.route ~ restMovement.route ~ restChicago.route ~
                restDuplicate.route ~ restDuplicateResult.route ~ restDuplicateSummary.route ~
                restIndividualDuplicateSummary.route ~
                restNames.route ~ restRubber.route ~ restSuggestion.route ~
                restDuplicatePlaces.route ~ restIndividualMovement.route ~
                restIndividualDuplicate.route // ~ restMyLogging.route
            }
          }
        } ~
          duplicateMonitor.route ~
          individualDuplicateMonitor.route ~
          chicagoMonitor.route ~
          rubberMonitor.route ~
          importExportRoute
      }
    }
}

object Service {

  val log: Logger = Logger[Service]()
  val clientlog: Logger = Logger[client.LogA]()

  private val format = java.time.format.DateTimeFormatter
    .ofPattern("HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

  def logStringFromBrowser(ips: String, msg: String): Unit = {
    clientlog.info(s"ClientLog($ips) $msg")
  }

  def formatMsg(msg: String, args: String*): String = {
    if (args.length == 0) msg
    else {
      val b = new java.lang.StringBuilder()
      val f = new Formatter(b)
      val a = args.asInstanceOf[Object]
      f.format(msg, args: _*)
      b.toString()
    }
  }

  def toLevel(level: String): Level = {
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
      case _   => Level.WARNING
    }

  }

  def logFromBrowser(
      ips: String,
      src: String,
      e: DuplexProtocol.LogEntryV2
  ): Unit = {
    val ts = format.format(Instant.ofEpochMilli(e.timestamp.toLong))
    val level = e.level
    val position = e.position
    val url = e.url
    val message = e.message
    val cause = e.cause
    val args = e.args
    val msg = formatMsg(message, args: _*)
    val lev = toLevel(level)
    val clientid = e.clientid.getOrElse("")
    clientlog.log(
      lev,
      s"$ts ClientLog($ips,$clientid) $src $level $position $url $msg $cause"
    )
  }

  def extractHostPort: HttpHeader => Option[String] = {
    case h: `Host` => Some(h.host.address() + ":" + h.port)
    case x         => None
  }

}
