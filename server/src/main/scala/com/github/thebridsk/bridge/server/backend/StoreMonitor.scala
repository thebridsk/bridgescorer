package com.github.thebridsk.bridge.server.backend

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import com.github.thebridsk.bridge.data.websocket.Protocol
import akka.http.scaladsl.model.RemoteAddress
import akka.NotUsed
import akka.event.Logging
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.MonitorLeft
import com.github.thebridsk.bridge.data.websocket.Protocol.MonitorJoined
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateTeam
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorChicago
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorRubber
import com.github.thebridsk.bridge.data.websocket.Protocol.NoData
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.server.rest.Service
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorSummary
import com.github.thebridsk.bridge.data.websocket.Protocol.StopMonitorSummary
import com.github.thebridsk.bridge.data.websocket.Protocol.StopMonitorDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.StopMonitorChicago
import com.github.thebridsk.bridge.data.websocket.Protocol.StopMonitorRubber
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.ErrorResponse
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.Response
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.Unsolicited
import com.github.thebridsk.bridge.data.websocket.Protocol.ToBrowserMessage
import akka.event.LoggingAdapter
import akka.stream.Attributes.Name
import akka.stream.Attributes
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Store
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.github.thebridsk.bridge.server.backend.resource.StoreListener
import com.github.thebridsk.bridge.server.backend.resource.ChangeContext
import com.github.thebridsk.bridge.server.backend.resource.CreateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.UpdateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.DeleteChangeContext
import com.github.thebridsk.bridge.data.VersionedInstance
import akka.http.scaladsl.model.sse.ServerSentEvent
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.Send
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicago
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateRubber
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.server.backend.StoreMonitor.ChatEvent
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicagoRound
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicagoHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateRubberHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePicture
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePictures
import akka.stream.CompletionStrategy
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicatePicture
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicatePictures
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicateHand
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorIndividualDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.StopMonitorIndividualDuplicate

object StoreMonitor {
  sealed trait ChatEvent
  case class NewParticipant(name: String, subscriber: ActorRef)
      extends ChatEvent
  case class ParticipantLeft(name: String) extends ChatEvent
  case class ReceivedMessage(sender: String, message: String) extends ChatEvent
  case class SendTo(to: String, msg: DuplexProtocol.DuplexMessage)
      extends ChatEvent

  case class NewParticipantSSEDuplicate(
      name: String,
      id: MatchDuplicate.Id,
      subscriber: ActorRef
  ) extends ChatEvent
  case class NewParticipantSSEIndividualDuplicate(
      name: String,
      id: IndividualDuplicate.Id,
      subscriber: ActorRef
  ) extends ChatEvent
  case class NewParticipantSSEChicago(
      name: String,
      id: MatchChicago.Id,
      subscriber: ActorRef
  ) extends ChatEvent
  case class NewParticipantSSERubber(
      name: String,
      id: MatchRubber.Id,
      subscriber: ActorRef
  ) extends ChatEvent

  var testHook: Option[(akka.actor.Actor, Any) => Unit] = None

  def setTestHook(hook: (akka.actor.Actor, Any) => Unit): Unit =
    testHook = Some(hook)
  def unsetTestHook(): Unit = testHook = None

  case class KillOneConnection()

  val counter = new AtomicLong

}

abstract class BaseStoreMonitor[VId <: Comparable[
  VId
], VType <: VersionedInstance[
  VType,
  VType,
  VId
]](
    system: ActorSystem,
    store: Store[VId, VType],
    toProtocol: VType => Protocol.ToBrowserMessage
) extends Subscriptions
    with Actor {
  import StoreMonitor._

  val log: LoggingAdapter = Logging(system, getClass)

  def process(sender: String, msg: DuplexProtocol.DuplexMessage): Any = {
    msg match {
      case DuplexProtocol.Send(data) =>
        log.info("Processing " + msg)
        processProtocolMessage(sender, -1, data, true)
//        val resp = processProtocolMessage(sender,-1,data,true)
//        dispatchTo(sender,resp)
      case DuplexProtocol.Request(data, seq, ack) =>
        log.info("Processing " + msg)
        processProtocolMessage(sender, seq, data, ack).foreach(r =>
          dispatchTo(r, sender)
        )
      case DuplexProtocol.LogEntryS(s) => {
        val le =
          DuplexProtocol.fromString(s).asInstanceOf[DuplexProtocol.LogEntryV2]
        logFromBrowser(sender, le)
      }
//        dispatchTo(sender, DuplexProtocol.Response(NoData(),-1))
      case le: DuplexProtocol.LogEntryV2 => {
        logFromBrowser(sender, le)
      }
//        dispatchTo(sender, DuplexProtocol.Response(NoData(),-1))
      case er: ErrorResponse =>
        log.warning("Unknown request " + er)
      case r: Response =>
        log.warning("Unknown request " + r)
      case u: Unsolicited =>
        log.warning("Unknown request " + u)

      case k: DuplexProtocol.Complete =>
        log.warning("Unknown request " + k)
      case k: DuplexProtocol.Fail =>
        log.warning("Unknown request " + k)

//      case _ =>
//        log.warning("Unknown request "+msg)
//        dispatchTo(sender, DuplexProtocol.Response(NoData(),-1))
    }
  }

  def logFromBrowser(ips: String, e: DuplexProtocol.LogEntryV2): Unit = {
    Service.logFromBrowser(ips, "ws", e)
  }

  def processProtocolMessage(
      sender: String,
      seq: Int,
      msg: Protocol.ToServerMessage,
      ack: Boolean
  ): Future[DuplexProtocol.DuplexMessage]

  protected def dispatchToAll(data: ToBrowserMessage): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAll: ${data}")
    dispatchToAll(Unsolicited(data))
  }

  protected def dispatchToAllDuplicate(
      id: MatchDuplicate.Id,
      data: ToBrowserMessage
  ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAllDuplicate(${id}): ${data}")
    dispatchToFiltered(Unsolicited(data))(DuplicateSubscription.filter(id))
  }

  protected def dispatchToAllIndividualDuplicate(
      id: IndividualDuplicate.Id,
      data: ToBrowserMessage
  ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAllIndividualDuplicate(${id}): ${data}")
    dispatchToFiltered(Unsolicited(data))(IndividualDuplicateSubscription.filter(id))
  }

  protected def dispatchToAllChicago(
      id: MatchChicago.Id,
      data: ToBrowserMessage
  ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAllChicago(${id}): ${data}")
    dispatchToFiltered(Unsolicited(data))(ChicagoSubscription.filter(id))
  }

  protected def dispatchToAllRubber(
      id: MatchRubber.Id,
      data: ToBrowserMessage
  ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAllRubber(${id}): ${data}")
    dispatchToFiltered(Unsolicited(data))(RubberSubscription.filter(id))
  }

  def receive: Receive = {
    case msg =>
      log.debug(s"StoreMonitor.receive $msg")
      try {
        testHook.foreach(hook => hook(this, msg))
      } catch {
        case x: Exception =>
          log.error(x, "Error in hook for message " + msg)
      }
      receiveImpl(msg)
  }

  def receiveImpl: Receive = {
    case NewParticipant(name, subscriber) =>
      log.info("(" + name + "): New monitor")
      context.watch(subscriber)
      add(new Subscription(name, subscriber))
      dispatchToAll(MonitorJoined(name, members))

      // Debug code to terminate a session after 10 seconds
      if (false) {
        import scala.concurrent.ExecutionContext.Implicits.global
        Future {
          val sec: Long = 10
          log.info(
            s"""In $sec seconds will Send PoisonPill to $name $subscriber"""
          )
          Thread.sleep(sec * 1000)
          log.info(s"""Sending complete to $name $subscriber""")
          subscriber ! DuplexProtocol.Complete("Testing termination")
        }
      }
    case NewParticipantSSEDuplicate(name, dupid, subscriber) =>
      log.info("(" + name + "): New monitor")
      context.watch(subscriber)
      add(new Subscription(name, subscriber))
      dispatchToAll(MonitorJoined(name, members))
      process(name, Send(StartMonitorDuplicate(dupid)))
    case NewParticipantSSEIndividualDuplicate(name, dupid, subscriber) =>
      log.info("(" + name + "): New monitor")
      context.watch(subscriber)
      add(new Subscription(name, subscriber))
      dispatchToAll(MonitorJoined(name, members))
      process(name, Send(StartMonitorIndividualDuplicate(dupid)))
    case NewParticipantSSEChicago(name, dupid, subscriber) =>
      log.info("(" + name + "): New monitor")
      context.watch(subscriber)
      add(new Subscription(name, subscriber))
      dispatchToAll(MonitorJoined(name, members))
      process(name, Send(StartMonitorChicago(dupid)))
    case NewParticipantSSERubber(name, dupid, subscriber) =>
      log.info("(" + name + "): New monitor")
      context.watch(subscriber)
      add(new Subscription(name, subscriber))
      dispatchToAll(MonitorJoined(name, members))
      process(name, Send(StartMonitorRubber(dupid)))

    case ReceivedMessage(senderid, message) =>
//      log.info("("+sender+"): Received "+message)
      process(senderid, DuplexProtocol.fromString(message))
    case msg: Protocol.ToBrowserMessage => // dispatch(msg)
      log.debug(s"Receive: ToBrowserMessage Sending to all ${msg}")
      msg match {
        case UpdateDuplicate(md)        => dispatchToAllDuplicate(md.id, msg)
        case UpdateIndividualDuplicate(md) => dispatchToAllIndividualDuplicate(md.id, msg)
        case UpdateDuplicateHand(id, h) => dispatchToAllDuplicate(id, msg)
        case UpdateDuplicateTeam(id, t) => dispatchToAllDuplicate(id, msg)
        case UpdateChicago(md)          => dispatchToAllChicago(md.id, msg)
        case UpdateRubber(md)           => dispatchToAllRubber(md.id, msg)
        case _ =>
          dispatchToAll(msg)
      }
    case msg: DuplexProtocol.DuplexMessage => // dispatch(msg)
      log.debug(s"Receive: DuplexMessage Sending to all ${msg}")
      dispatchToAll(msg)
    case ParticipantLeft(name) =>
      log.info("(" + name + "): Participant left")
      remove(name) match {
        case Some(sub) =>
          context.unwatch(sub.actor)
          sub.actor ! DuplexProtocol.Complete() // akka.actor.Status.Success(0)
        case None =>
      }
      dispatchToAll(MonitorLeft(name, members))
    case SendTo(senderid, data) =>
      dispatchTo(data, senderid)
    case Terminated(subscriber) =>
      log.info("Subscriber terminated")
      // Will not happen if we got a ParticipantLeft from the subscriber
      // clean up dead subscribers, but should have been removed when `ParticipantLeft`
      context.unwatch(subscriber)
      remove(subscriber).foreach { s =>
        log.info("(" + s.id + "): Removed subscriber")
      }
    case x: KillOneConnection =>
      val ms = members
      if (ms.size > 0) {
        val sub = members(0)
        log.info(s"""Killing connection $sub""")
        remove(sub) match {
          case Some(s) =>
            log.info(s"""Sending complete to $sub""")
//            s.actor ! PoisonPill
            s.actor ! DuplexProtocol.Complete("")
          case None =>
        }
      }
  }

  val listener = new Listener(log, self)

  override def register(): Unit = store.addListener(listener)
  override def unregister(): Unit = store.removeListener(listener)

  protected def futureError(
      msg: String,
      seq: Int
  ): Future[DuplexProtocol.DuplexMessage] =
    Future {
      DuplexProtocol.ErrorResponse(msg, seq)
    }
}

class StoreMonitorManager[VId <: Comparable[VId], VType <: VersionedInstance[
  VType,
  VType,
  VId
]](
    system: ActorSystem,
    store: Store[VId, VType],
    storeMonitorClass: Class[_],
    newParticipant: (String, VId, ActorRef) => ChatEvent,
    service: Service
) {
  val log: LoggingAdapter = Logging(system, getClass)

  import StoreMonitor._

  val monitor: ActorRef = system.actorOf(
    Props(storeMonitorClass, system, store, service),
    name = s"${storeMonitorClass.getSimpleName}Actor"
  )

  // Wraps the chatActor in a sink. When the stream to this sink will be completed
  // it sends the `ParticipantLeft` message to the chatActor.
  // FIXME: here some rate-limiting should be applied to prevent single users flooding
  private def chatInSink(sender: String) =
    Sink.actorRef[ChatEvent](
      monitor,
      ParticipantLeft(sender),
      { ex: Throwable =>
        ParticipantLeft(sender)
      }
    )

  val completionMatcher: PartialFunction[Any, CompletionStrategy] = {
    case x: DuplexProtocol.Complete => CompletionStrategy.immediately
  }
  val failureMatcher: PartialFunction[Any, Exception] = {
    case x: DuplexProtocol.Fail => x.ex
  }

  def monitorFlow(
      sender: RemoteAddress
  ): Flow[String, DuplexProtocol.DuplexMessage, NotUsed] = {
    val count = StoreMonitor.counter.incrementAndGet()
    val name = sender.toString().replaceAll("\\.", "-")
    val in =
      Flow[String]
        .map { s =>
          {
            log.debug("Received on websocket(" + sender + "): " + s)
            ReceivedMessage(sender.toString(), s)
          }
        }
        .to(
          chatInSink(sender.toString).addAttributes(
            Attributes(Name(s"sink-monitorFlow-$count.in-${name}"))
          )
        )

    // The counter-part which is a source that will create a target ActorRef per
    // materialization where the chatActor will send its messages to.
    // This source will only buffer one element and will fail if the client doesn't read
    // messages fast enough.
    val out =
      Source
        .actorRef[DuplexProtocol.DuplexMessage](
          completionMatcher = completionMatcher,
          failureMatcher = failureMatcher,
          bufferSize = 4,
          overflowStrategy = OverflowStrategy.fail
        )
        .addAttributes(
          Attributes(Name(s"source-monitorFlow-$count.out-${name}"))
        )
        .mapMaterializedValue { actorref =>
          log.debug(s"Created new monitorFlow ${sender} ${actorref} ")
          monitor ! NewParticipant(sender.toString, actorref)
        }

    // val f =
    Flow.fromSinkAndSource(in, out) // .via(new TerminateFlowStage(log, false))
    // f
  }

  def monitorMatch(
      sender: RemoteAddress,
      id: VId
  ): Source[ServerSentEvent, Any] = {
    sseSource(
      sender,
      Source
        .actorRef[DuplexProtocol.DuplexMessage](
          completionMatcher = completionMatcher,
          failureMatcher = failureMatcher,
          10,
          OverflowStrategy.fail
        )
        .mapMaterializedValue { x =>
          monitor ! newParticipant(sender.toString(), id, x)
        }
    )
  }

  def sseSource(
      sender: RemoteAddress,
      source: Source[DuplexProtocol.DuplexMessage, _]
  ): Source[ServerSentEvent, Any] = {
    source
      .map(msg => ServerSentEvent(DuplexProtocol.toString(msg)))
      .keepAlive(10.second, () => ServerSentEvent.heartbeat)
      .map { s =>
        val ip = sender.toIP match {
          case Some(x) => s"""${x.ip}:${x.port}"""
          case None    => "Unknown"
        }
        log.debug(s"""Sending ServerSentEvent to ${ip}: ${s}""")
        s
      }
  }

}

// class TerminateFlowStage(log: LoggingAdapter, abort: Boolean = true)
//     extends GraphStage[FlowShape[DuplexProtocol.DuplexMessage, DuplexProtocol.DuplexMessage]] {
//   val in = Inlet[DuplexProtocol.DuplexMessage]("TerminateFlowStage.in")
//   val out = Outlet[DuplexProtocol.DuplexMessage]("TerminateFlowStage.out")
//   override val shape = FlowShape.of(in, out)

//   override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
//     new GraphStageLogic(shape) {
//       import TerminateFlowStage._

//       setHandlers(
//         in,
//         out,
//         new InHandler with OutHandler {
//           override def onPull(): Unit = { pull(in) }

//           override def onPush(): Unit = {
//             val chunk = grab(in)
//             log.info(s"""TerminateFlowStage.onPush: ${chunk}""")
//             chunk match {
//               case k: KillMsg =>
//                 if (abort) {
//                   log.info(s"""Sending abort really completeStage""")
// //                push( out, Tcp.Abort )
// //                failStage(new RuntimeException("Flow terminated by TerminateFlowStage"))
//                   completeStage()
//                 } else {
//                   log.info(s"""Sending completeStage""")
//                   completeStage()
//                 }
//               case dm: DuplexProtocol.DuplexMessage =>
//                 push(out, dm)
//             }
//           }
//         }
//       )
//     }
// }

class DuplicateStoreMonitor(
    system: ActorSystem,
    store: Store[MatchDuplicate.Id, MatchDuplicate],
    service: Service
) extends BaseStoreMonitor[MatchDuplicate.Id, MatchDuplicate](
      system,
      store,
      Protocol.UpdateDuplicate(_)
    ) {

  def processProtocolMessage(
      sender: String,
      seq: Int,
      msg: Protocol.ToServerMessage,
      ack: Boolean
  ): Future[DuplexProtocol.DuplexMessage] = {
    import BridgeNestedResources._
    log.info("ProcessProtocolMessage(" + seq + ") " + msg)
    val resp: Future[DuplexProtocol.DuplexMessage] = msg match {
      case UpdateDuplicate(dup) =>
        log.debug("Updating the MatchDuplicate object is not supported")
        futureError("Updating the MatchDuplicate object is not supported", seq)
      case UpdateDuplicateHand(dupid, hand) =>
        log.debug(s"UpdateDuplicateHand ${dupid} ${hand}")
        store
          .select(dupid)
          .resourceBoards
          .select(hand.board)
          .resourceHands
          .select(hand.id)
          .update(hand)
          .map { rh =>
            dispatchToAllDuplicate(dupid, UpdateDuplicateHand(dupid, hand))
            DuplexProtocol.Response(
              if (ack) UpdateDuplicateHand(dupid, hand) else NoData(),
              seq
            )
          }
      case UpdateDuplicateTeam(dupid, team) =>
        log.debug(s"UpdateDuplicateTeam ${dupid} ${team}")
        store.select(dupid).resourceTeams.select(team.id).update(team).map {
          rt =>
            dispatchToAllDuplicate(dupid, UpdateDuplicateTeam(dupid, team))
            DuplexProtocol.Response(
              if (ack) UpdateDuplicateTeam(dupid, team) else NoData(),
              seq
            )
        }
      case UpdateDuplicatePicture(dupid, boardid, handid, picture) =>
        log.debug(s"UpdateDuplicatePicture ${dupid} ${boardid} ${picture}")
        futureError("Use REST API", seq)
      case UpdateDuplicatePictures(dupid, pictures) =>
        log.debug(s"UpdateDuplicatePictures ${dupid} ${pictures}")
        futureError("Use REST API", seq)
      case StartMonitorDuplicate(dupid: MatchDuplicate.Id) =>
        log.info(s"StartMonitorDuplicate ${dupid}")
        val subid = get(sender) match {
          case Some(sub) =>
            add(new DuplicateSubscription(sub, dupid))
            Some(sub)
          case None =>
            None
        }
        store.read(dupid).map { rd =>
          val resp = rd match {
            case Right(dup) =>
              if (ack) {
                log.info("Sending MatchDuplicate to " + sender + ": " + dup)
                dispatchTo(
                  DuplexProtocol.Unsolicited(UpdateDuplicate(dup)),
                  sender
                )
                DuplexProtocol.Response(NoData(), seq)
              } else {
                DuplexProtocol.Response(UpdateDuplicate(dup), seq)
              }
            case _ =>
              DuplexProtocol.Response(NoData(), seq)
          }
          service.restDuplicate.nestedPictures.getAllPictures(dupid).foreach {
            ridp =>
              ridp match {
                case Right(idp) =>
                  val ldp = idp.toList
                  if (!ldp.isEmpty) {
                    val data = UpdateDuplicatePictures(dupid, ldp)
                    dispatchTo(DuplexProtocol.Unsolicited(data), sender)
                  }
                case Left(err) =>
                  log.warning("Error getting all pictures in monitor: ${err}")
              }
          }
          resp
        }
      case StopMonitorDuplicate(dupid) =>
        log.info(s"StopMonitorDuplicate ${dupid}")
        get(sender) match {
          case Some(sub) =>
            add(sub.getSubscription())
          case None =>
        }
        Future(DuplexProtocol.Response(NoData(), seq))

      case UpdateIndividualDuplicate(dup) =>
        log.warning("UpdateIndividualDuplicate not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicateHand(dupid, hand) =>
        log.warning("UpdateIndividualDuplicateHand not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicatePicture(dupid, boardid, handid, picture) =>
        log.warning("UpdateIndividualDuplicatePicture not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicatePictures(dupid, pictures) =>
        log.warning("UpdateIndividualDuplicatePictures not implemented")
        futureError("Unknown request", seq)
      case StartMonitorIndividualDuplicate(dupid: IndividualDuplicate.Id) =>
        log.warning("StartMonitorIndividualDuplicate not implemented")
        futureError("Unknown request", seq)
      case StopMonitorIndividualDuplicate(dupid) =>
        log.warning("StopMonitorIndividualDuplicate not implemented")
        futureError("Unknown request", seq)

      case NoData(_) =>
        log.debug("No data")
        Future(DuplexProtocol.Response(NoData(), seq))
      case StartMonitorSummary(_) =>
        log.info("StartMonitorSummary not implemented")
        futureError("Unknown request", seq)
      case StopMonitorSummary(_) =>
        log.info("StopMonitorSummary not implemented")
        futureError("Unknown request", seq)

      case StartMonitorChicago(_) =>
        log.warning("StartMonitorChicago not implemented")
        futureError("Unknown request", seq)
      case StopMonitorChicago(_) =>
        log.warning("StopMonitorChicago not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicago =>
        log.warning("UpcateChicago not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicagoRound =>
        log.warning("UpcateChicagoRound not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicagoHand =>
        log.warning("UpcateChicagoHand not implemented")
        futureError("Unknown request", seq)

      case StartMonitorRubber(_) =>
        log.warning("StartMonitorRubber not implemented")
        futureError("Unknown request", seq)
      case StopMonitorRubber(_) =>
        log.warning("StopMonitorRubber not implemented")
        futureError("Unknown request", seq)
      case x: UpdateRubber =>
        log.warning("UpdateRubber not implemented")
        futureError("Unknown request", seq)
      case x: UpdateRubberHand =>
        log.warning("UpdateRubber not implemented")
        futureError("Unknown request", seq)

//      case _ =>
//        log.warning("Unknown request "+msg)
//        futureError("Unknown request", seq)
    }

    resp.map { r =>
      log.info("ProcessProtocolMessage Response(" + seq + ") " + r); r
    }
  }

}

class IndividualDuplicateStoreMonitor(
    system: ActorSystem,
    store: Store[IndividualDuplicate.Id, IndividualDuplicate],
    service: Service
) extends BaseStoreMonitor[IndividualDuplicate.Id, IndividualDuplicate](
      system,
      store,
      Protocol.UpdateIndividualDuplicate(_)
    ) {

  def processProtocolMessage(
      sender: String,
      seq: Int,
      msg: Protocol.ToServerMessage,
      ack: Boolean
  ): Future[DuplexProtocol.DuplexMessage] = {
    import BridgeNestedResources._
    log.info("ProcessProtocolMessage(" + seq + ") " + msg)
    val resp: Future[DuplexProtocol.DuplexMessage] = msg match {
      case UpdateDuplicate(dup) =>
        log.warning("UpdateDuplicate not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicateHand(dupid, hand) =>
        log.warning("UpdateDuplicateHand not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicateTeam(dupid, team) =>
        log.warning("UpdateDuplicateTeam not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicatePicture(dupid, boardid, handid, picture) =>
        log.warning("UpdateDuplicatePicture not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicatePictures(dupid, pictures) =>
        log.warning("UpdateDuplicatePictures not implemented")
        futureError("Unknown request", seq)
      case StartMonitorDuplicate(dupid: MatchDuplicate.Id) =>
        log.warning("StartMonitorDuplicate not implemented")
        futureError("Unknown request", seq)
      case StopMonitorDuplicate(dupid) =>
        log.warning("StopMonitorDuplicate not implemented")
        futureError("Unknown request", seq)

      case UpdateIndividualDuplicate(dup) =>
        log.debug("Updating the IndividualDuplicate object is not supported")
        futureError("Updating the IndividualDuplicate object is not supported", seq)
      case UpdateIndividualDuplicateHand(dupid, hand) =>
        log.debug(s"UpdateIndividualDuplicateHand ${dupid} ${hand}")
        store
          .select(dupid)
          .resourceBoards
          .select(hand.board)
          .resourceHands
          .select(hand.id)
          .update(hand)
          .map { rh =>
            dispatchToAllIndividualDuplicate(dupid, UpdateIndividualDuplicateHand(dupid, hand))
            DuplexProtocol.Response(
              if (ack) UpdateIndividualDuplicateHand(dupid, hand) else NoData(),
              seq
            )
          }
      case UpdateIndividualDuplicatePicture(dupid, boardid, handid, picture) =>
        log.debug(s"UpdateDuplicatePicture ${dupid} ${boardid} ${picture}")
        futureError("Use REST API", seq)
      case UpdateIndividualDuplicatePictures(dupid, pictures) =>
        log.debug(s"UpdateDuplicatePictures ${dupid} ${pictures}")
        futureError("Use REST API", seq)
      case StartMonitorIndividualDuplicate(dupid: IndividualDuplicate.Id) =>
        log.info(s"StartMonitorIndividualDuplicate ${dupid}")
        val subid = get(sender) match {
          case Some(sub) =>
            add(new IndividualDuplicateSubscription(sub, dupid))
            Some(sub)
          case None =>
            None
        }
        store.read(dupid).map { rd =>
          val resp = rd match {
            case Right(dup) =>
              if (ack) {
                log.info("Sending MatchDuplicate to " + sender + ": " + dup)
                dispatchTo(
                  DuplexProtocol.Unsolicited(UpdateIndividualDuplicate(dup)),
                  sender
                )
                DuplexProtocol.Response(NoData(), seq)
              } else {
                DuplexProtocol.Response(UpdateIndividualDuplicate(dup), seq)
              }
            case _ =>
              DuplexProtocol.Response(NoData(), seq)
          }
          service.restIndividualDuplicate.nestedPictures.getAllPictures(dupid).foreach {
            ridp =>
              ridp match {
                case Right(idp) =>
                  val ldp = idp.toList
                  if (!ldp.isEmpty) {
                    val data = UpdateIndividualDuplicatePictures(dupid, ldp)
                    dispatchTo(DuplexProtocol.Unsolicited(data), sender)
                  }
                case Left(err) =>
                  log.warning("Error getting all pictures in monitor: ${err}")
              }
          }
          resp
        }
      case StopMonitorIndividualDuplicate(dupid) =>
        log.info(s"StopMonitorIndividualDuplicate ${dupid}")
        get(sender) match {
          case Some(sub) =>
            add(sub.getSubscription())
          case None =>
        }
        Future(DuplexProtocol.Response(NoData(), seq))

      case NoData(_) =>
        log.debug("No data")
        Future(DuplexProtocol.Response(NoData(), seq))
      case StartMonitorSummary(_) =>
        log.info("StartMonitorSummary not implemented")
        futureError("Unknown request", seq)
      case StopMonitorSummary(_) =>
        log.info("StopMonitorSummary not implemented")
        futureError("Unknown request", seq)

      case StartMonitorChicago(_) =>
        log.warning("StartMonitorChicago not implemented")
        futureError("Unknown request", seq)
      case StopMonitorChicago(_) =>
        log.warning("StopMonitorChicago not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicago =>
        log.warning("UpcateChicago not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicagoRound =>
        log.warning("UpcateChicagoRound not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicagoHand =>
        log.warning("UpcateChicagoHand not implemented")
        futureError("Unknown request", seq)

      case StartMonitorRubber(_) =>
        log.warning("StartMonitorRubber not implemented")
        futureError("Unknown request", seq)
      case StopMonitorRubber(_) =>
        log.warning("StopMonitorRubber not implemented")
        futureError("Unknown request", seq)
      case x: UpdateRubber =>
        log.warning("UpcateRubber not implemented")
        futureError("Unknown request", seq)
      case x: UpdateRubberHand =>
        log.warning("UpcateRubber not implemented")
        futureError("Unknown request", seq)

//      case _ =>
//        log.warning("Unknown request "+msg)
//        futureError("Unknown request", seq)
    }

    resp.map { r =>
      log.info("ProcessProtocolMessage Response(" + seq + ") " + r); r
    }
  }

}

class ChicagoStoreMonitor(
    system: ActorSystem,
    store: Store[MatchChicago.Id, MatchChicago],
    service: Service
) extends BaseStoreMonitor[MatchChicago.Id, MatchChicago](
      system,
      store,
      Protocol.UpdateChicago(_)
    ) {

  def processProtocolMessage(
      sender: String,
      seq: Int,
      msg: Protocol.ToServerMessage,
      ack: Boolean
  ): Future[DuplexProtocol.DuplexMessage] = {
    import BridgeNestedResources._
    log.info("ProcessProtocolMessage(" + seq + ") " + msg)
    val resp: Future[DuplexProtocol.DuplexMessage] = msg match {
      case UpdateDuplicate(dup) =>
        log.warning("Updating the MatchDuplicate object is not supported")
        futureError("Updating the MatchDuplicate object is not supported", seq)
      case UpdateDuplicateHand(dupid, hand) =>
        log.warning("UpdateDuplicateHand not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicateTeam(dupid, team) =>
        log.warning("UpdateDuplicateTeam not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicatePicture(dupid, boardid, handid, picture) =>
        log.warning("UpdateDuplicatePicture not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicatePictures(dupid, pictures) =>
        log.debug(s"UpdateDuplicatePictures not implemented")
        futureError("Unknown request", seq)
      case StartMonitorDuplicate(dupid: MatchDuplicate.Id) =>
        log.warning("StartMonitorDuplicate not implemented")
        futureError("Unknown request", seq)
      case StopMonitorDuplicate(dupid) =>
        log.warning("StopMonitorDuplicate not implemented")
        futureError("Unknown request", seq)

      case UpdateIndividualDuplicate(dup) =>
        log.warning("UpdateIndividualDuplicate not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicateHand(dupid, hand) =>
        log.warning("UpdateIndividualDuplicateHand not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicatePicture(dupid, boardid, handid, picture) =>
        log.warning("UpdateIndividualDuplicatePicture not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicatePictures(dupid, pictures) =>
        log.warning("UpdateIndividualDuplicatePictures not implemented")
        futureError("Unknown request", seq)
      case StartMonitorIndividualDuplicate(dupid: IndividualDuplicate.Id) =>
        log.warning("StartMonitorIndividualDuplicate not implemented")
        futureError("Unknown request", seq)
      case StopMonitorIndividualDuplicate(dupid) =>
        log.warning("StopMonitorIndividualDuplicate not implemented")
        futureError("Unknown request", seq)

      case NoData(_) =>
        log.debug("No data")
        Future(DuplexProtocol.Response(NoData(), seq))
      case StartMonitorSummary(_) =>
        log.warning("StartMonitorSummary not implemented")
        futureError("Unknown request", seq)
      case StopMonitorSummary(_) =>
        log.warning("StopMonitorSummary not implemented")
        futureError("Unknown request", seq)

      case StartMonitorChicago(dupid: MatchChicago.Id) =>
        log.info(s"StartMonitorChicago ${dupid}")
        get(sender) match {
          case Some(sub) =>
            add(new ChicagoSubscription(sub, dupid))
          case None =>
        }
        store.read(dupid).map { rchi =>
          rchi match {
            case Right(dup) =>
              if (ack) {
                log.info("Sending MatchChicago to " + sender + ": " + dup)
                dispatchTo(
                  DuplexProtocol.Unsolicited(UpdateChicago(dup)),
                  sender
                )
                DuplexProtocol.Response(NoData(), seq)
              } else {
                DuplexProtocol.Response(UpdateChicago(dup), seq)
              }
            case _ =>
              DuplexProtocol.Response(NoData(), seq)
          }
        }
      case StopMonitorChicago(dupid) =>
        log.info(s"StopMonitorChicago ${dupid}")
        get(sender) match {
          case Some(sub) =>
            add(sub.getSubscription())
          case None =>
        }
        Future(DuplexProtocol.Response(NoData(), seq))
      case UpdateChicago(chi) =>
        log.info(s"UpdateChicago ${chi}")
        store.select(chi.id).update(chi).map { rchi =>
          dispatchToAllChicago(chi.id, UpdateChicago(chi))
          DuplexProtocol
            .Response(if (ack) UpdateChicago(chi) else NoData(), seq)
        }
      case UpdateChicagoRound(chiid, round) =>
        log.info(s"UpdateChicagoRound ${chiid} ${round}")
        store.select(chiid).resourceRounds.select(round.id).update(round).map {
          r =>
            dispatchToAllChicago(chiid, UpdateChicagoRound(chiid, round))
            DuplexProtocol.Response(
              if (ack) UpdateChicagoRound(chiid, round) else NoData(),
              seq
            )
        }
      case UpdateChicagoHand(chiid, rid, hand) =>
        log.info(s"UpdateChicagoHand ${chiid} ${rid} ${hand}")
        store
          .select(chiid)
          .resourceRounds
          .select(rid)
          .resourceHands
          .select(hand.id)
          .update(hand)
          .map { h =>
            dispatchToAllChicago(chiid, UpdateChicagoHand(chiid, rid, hand))
            DuplexProtocol.Response(
              if (ack) UpdateChicagoHand(chiid, rid, hand) else NoData(),
              seq
            )
          }

      case StartMonitorRubber(_) =>
        log.warning("StartMonitorRubber not implemented")
        futureError("Unknown request", seq)
      case StopMonitorRubber(_) =>
        log.warning("StopMonitorRubber not implemented")
        futureError("Unknown request", seq)
      case x: UpdateRubber =>
        log.warning("UpcateRubber not implemented")
        futureError("Unknown request", seq)
      case x: UpdateRubberHand =>
        log.warning("UpcateRubber not implemented")
        futureError("Unknown request", seq)

//      case _ =>
//        log.warning("Unknown request "+msg)
//        futureError("Unknown request", seq)
    }
    resp.map { r =>
      log.info("ProcessProtocolMessage Response(" + seq + ") " + r); r
    }
  }

}

class RubberStoreMonitor(
    system: ActorSystem,
    store: Store[MatchRubber.Id, MatchRubber],
    service: Service
) extends BaseStoreMonitor[MatchRubber.Id, MatchRubber](
      system,
      store,
      Protocol.UpdateRubber(_)
    ) {

  def processProtocolMessage(
      sender: String,
      seq: Int,
      msg: Protocol.ToServerMessage,
      ack: Boolean
  ): Future[DuplexProtocol.DuplexMessage] = {
    import BridgeNestedResources._
    log.info("ProcessProtocolMessage(" + seq + ") " + msg)
    val resp: Future[DuplexProtocol.DuplexMessage] = msg match {
      case UpdateDuplicate(dup) =>
        log.warning("Updating the MatchDuplicate object is not supported")
        futureError("Updating the MatchDuplicate object is not supported", seq)
      case UpdateDuplicateHand(dupid, hand) =>
        log.warning("UpdateDuplicateHand not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicateTeam(dupid, team) =>
        log.warning("UpdateDuplicateTeam not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicatePicture(dupid, boardid, handid, picture) =>
        log.warning("UpdateDuplicatePicture not implemented")
        futureError("Unknown request", seq)
      case UpdateDuplicatePictures(dupid, pictures) =>
        log.debug(s"UpdateDuplicatePictures not implemented")
        futureError("Unknown request", seq)
      case StartMonitorDuplicate(dupid: MatchDuplicate.Id) =>
        log.warning("StartMonitorDuplicate not implemented")
        futureError("Unknown request", seq)
      case StopMonitorDuplicate(dupid) =>
        log.warning("StopMonitorDuplicate not implemented")
        futureError("Unknown request", seq)

      case UpdateIndividualDuplicate(dup) =>
        log.warning("UpdateIndividualDuplicate not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicateHand(dupid, hand) =>
        log.warning("UpdateIndividualDuplicateHand not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicatePicture(dupid, boardid, handid, picture) =>
        log.warning("UpdateIndividualDuplicatePicture not implemented")
        futureError("Unknown request", seq)
      case UpdateIndividualDuplicatePictures(dupid, pictures) =>
        log.warning("UpdateIndividualDuplicatePictures not implemented")
        futureError("Unknown request", seq)
      case StartMonitorIndividualDuplicate(dupid: IndividualDuplicate.Id) =>
        log.warning("StartMonitorIndividualDuplicate not implemented")
        futureError("Unknown request", seq)
      case StopMonitorIndividualDuplicate(dupid) =>
        log.warning("StopMonitorIndividualDuplicate not implemented")
        futureError("Unknown request", seq)

      case NoData(_) =>
        log.debug("No data")
        Future(DuplexProtocol.Response(NoData(), seq))
      case StartMonitorSummary(_) =>
        log.warning("StartMonitorSummary not implemented")
        futureError("Unknown request", seq)
      case StopMonitorSummary(_) =>
        log.warning("StopMonitorSummary not implemented")
        futureError("Unknown request", seq)

      case StartMonitorChicago(_) =>
        log.warning("StartMonitorChicago not implemented")
        futureError("Unknown request", seq)
      case StopMonitorChicago(_) =>
        log.warning("StopMonitorChicago not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicago =>
        log.warning("UpcateChicago not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicagoRound =>
        log.warning("UpcateChicagoRound not implemented")
        futureError("Unknown request", seq)
      case x: UpdateChicagoHand =>
        log.warning("UpcateChicagoHand not implemented")
        futureError("Unknown request", seq)

      case StartMonitorRubber(dupid) =>
        log.info(s"StartMonitorRubber ${dupid.id}")
        get(sender) match {
          case Some(sub) =>
            add(new RubberSubscription(sub, dupid))
          case None =>
        }
        store.read(dupid).map { rr =>
          rr match {
            case Right(dup) =>
              if (ack) {
                log.info("Sending MatchRubber to " + sender + ": " + dup)
                dispatchTo(
                  DuplexProtocol.Unsolicited(UpdateRubber(dup)),
                  sender
                )
                DuplexProtocol.Response(NoData(), seq)
              } else {
                DuplexProtocol.Response(UpdateRubber(dup), seq)
              }
            case _ =>
              DuplexProtocol.Response(NoData(), seq)
          }
        }
      case StopMonitorRubber(dupid) =>
        log.info(s"StopMonitorRubber ${dupid}")
        get(sender) match {
          case Some(sub) =>
            add(sub.getSubscription())
          case None =>
        }
        Future(DuplexProtocol.Response(NoData(), seq))
      case UpdateRubber(rub) =>
        log.info(s"UpdateRubber ${rub}")
        store.select(rub.id).update(rub).map { rr =>
          dispatchToAllRubber(rub.id, UpdateRubber(rub))
          DuplexProtocol.Response(if (ack) UpdateRubber(rub) else NoData(), seq)
        }
      case UpdateRubberHand(rid, hand) =>
        log.info(s"UpdateRubberHand ${rid} ${hand}")
        store.select(rid).resourceHands.select(hand.id).update(hand).map { rh =>
          dispatchToAllRubber(rid, UpdateRubberHand(rid, hand))
          DuplexProtocol
            .Response(if (ack) UpdateRubberHand(rid, hand) else NoData(), seq)
        }

//      case _ =>
//        log.warning("Unknown request "+msg)
//        futureError("Unknown request", seq)
    }
    resp.map { r =>
      log.info("ProcessProtocolMessage Response(" + seq + ") " + r); r
    }
  }

}

class Listener(log: LoggingAdapter, actor: ActorRef) extends StoreListener {
  import com.github.thebridsk.bridge.server.backend.resource.ChangeContextData
  private def getDuplicateId(
      ccd: ChangeContextData
  ): Option[MatchDuplicate.Id] = {
    (ccd match {
      case cc: CreateChangeContext => Some(cc.newValue)
      case uc: UpdateChangeContext => Some(uc.newValue)
      case dc: DeleteChangeContext => None // can't happen here
    }) match {
      case Some(fdata) =>
        fdata match {
          case md: MatchDuplicate           => Some(md.id)
          case b: Board                     => None
          case h: DuplicateHand             => None
          case pict: UpdateDuplicatePicture => Some(pict.dupid)
          case _                            => None
        }
      case None => None
    }
  }
  private def getIndividualDuplicateId(
      ccd: ChangeContextData
  ): Option[IndividualDuplicate.Id] = {
    (ccd match {
      case cc: CreateChangeContext => Some(cc.newValue)
      case uc: UpdateChangeContext => Some(uc.newValue)
      case dc: DeleteChangeContext => None // can't happen here
    }) match {
      case Some(fdata) =>
        fdata match {
          case md: IndividualDuplicate      => Some(md.id)
          case b: Board                     => None
          case h: DuplicateHand             => None
          case pict: UpdateIndividualDuplicatePicture => Some(pict.dupid)
          case _                            => None
        }
      case None => None
    }
  }
  private def getChicagoRound(ccd: ChangeContextData): Option[String] = {
    (ccd match {
      case cc: CreateChangeContext => Some(cc.newValue)
      case uc: UpdateChangeContext => Some(uc.newValue)
      case dc: DeleteChangeContext => None // can't happen here
    }) match {
      case Some(fdata) =>
        fdata match {
          case r: Round => Some(r.id)
          case _        => None
        }
      case None => None
    }
  }
  private def getChicagoId(ccd: ChangeContextData): Option[MatchChicago.Id] = {
    (ccd match {
      case cc: CreateChangeContext => Some(cc.newValue)
      case uc: UpdateChangeContext => Some(uc.newValue)
      case dc: DeleteChangeContext => None // can't happen here
    }) match {
      case Some(fdata) =>
        fdata match {
          case mc: MatchChicago => Some(mc.id)
          case _                => None
        }
      case None => None
    }
  }
  private def getRubberId(ccd: ChangeContextData): Option[MatchRubber.Id] = {
    (ccd match {
      case cc: CreateChangeContext => Some(cc.newValue)
      case uc: UpdateChangeContext => Some(uc.newValue)
      case dc: DeleteChangeContext => None // can't happen here
    }) match {
      case Some(fdata) =>
        fdata match {
          case mr: MatchRubber => Some(mr.id)
          case _               => None
        }
      case None => None
    }
  }

  override def create(context: ChangeContext): Unit = update(context)
  override def update(context: ChangeContext): Unit = {
    val changes = context.changes
    log.debug("StoreMonitor.StoreListener " + changes.mkString("\n", "\n", ""))
    (context.getSpecificChange() match {
      case Some(scd) =>
        scd match {
          case cc: CreateChangeContext =>
            log.debug(
              s"StoreMonitor.Listener.update: got a CreateChangeContext"
            )
            Some(cc.newValue)
          case uc: UpdateChangeContext =>
            log.debug(
              s"StoreMonitor.Listener.update: got a UpdateChangeContext"
            )
            Some(uc.newValue)
          case dc: DeleteChangeContext =>
            None // can't happen here, only create and delete
        }
      case None =>
    }) match {
      case Some(v) =>
        v match {
          case md: MatchDuplicate =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateDuplicate($md)")
            actor ! UpdateDuplicate(md)
          case b: Board =>
          // log.debug(s"StoreMonitor.Listener.update: Ignoring $b")
          case pict: UpdateDuplicatePicture =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with $pict")
            actor ! pict
          case h: DuplicateHand =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateDuplicateHand($h)")
            getDuplicateId(changes.head).foreach { mdid =>
              actor ! UpdateDuplicateHand(mdid, h)
            }
          case t: Team =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateDuplicateTeam($t)")
            getDuplicateId(changes.head).foreach { mdid =>
              actor ! UpdateDuplicateTeam(mdid, t)
            }
          case md: IndividualDuplicate =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateDuplicate($md)")
            actor ! UpdateIndividualDuplicate(md)
          case b: IndividualBoard =>
          // log.debug(s"StoreMonitor.Listener.update: Ignoring $b")
          case pict: UpdateIndividualDuplicatePicture =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with $pict")
            actor ! pict
          case h: IndividualDuplicateHand =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateDuplicateHand($h)")
            getIndividualDuplicateId(changes.head).foreach { mdid =>
              actor ! UpdateIndividualDuplicateHand(mdid, h)
            }
          case mc: MatchChicago =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateChicago($mc)")
            actor ! UpdateChicago(mc)
          case cr: Round =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateChicagoRound($cr)")
            getChicagoId(changes.head).foreach { mdid =>
              actor ! UpdateChicagoRound(mdid, cr)
            }
          case ch: Hand =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateChicagoHand($ch)")
            getChicagoRound(changes.tail.head) match {
              case Some(rid) =>
                getChicagoId(changes.head) match {
                  case Some(mcid) =>
                    actor ! UpdateChicagoHand(mcid, rid, ch)
                  case None =>
                }
              case None =>
            }
            val mcid = getChicagoId(changes.head)
          case mr: MatchRubber =>
            // log.debug(s"StoreMonitor.Listener.update: notifying actor with UpdateRubber($mr)")
            actor ! UpdateRubber(mr)
          case x =>
        }
      case None =>
    }
  }

  override def delete(context: ChangeContext): Unit = {}

}
