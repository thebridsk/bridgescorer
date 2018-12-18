package com.example.backend

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import com.example.data.websocket.Protocol
import akka.http.scaladsl.model.RemoteAddress
import akka.NotUsed
import akka.event.Logging
import com.example.data.MatchDuplicate
import com.example.data.Id
import com.example.data.websocket.Protocol.MonitorLeft
import com.example.data.websocket.Protocol.MonitorJoined
import com.example.data.websocket.Protocol.UpdateDuplicate
import com.example.data.websocket.Protocol.UpdateDuplicateHand
import com.example.data.websocket.Protocol.UpdateDuplicateTeam
import com.example.data.websocket.DuplexProtocol
import com.example.data.websocket.Protocol.StartMonitor
import com.example.data.websocket.Protocol.NoData
import java.util.Date
import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.Team
import java.util.Formatter
import com.example.rest.Service
import com.example.data.websocket.Protocol.StartMonitorSummary
import com.example.data.websocket.Protocol.StopMonitorSummary
import com.example.data.websocket.Protocol.StopMonitor
import com.example.data.websocket.DuplexProtocol.ErrorResponse
import com.example.data.websocket.DuplexProtocol.Response
import com.example.data.websocket.DuplexProtocol.Unsolicited
import com.example.data.websocket.Protocol.ToBrowserMessage
import akka.event.LoggingAdapter
import akka.stream.Attributes.Name
import akka.stream.Attributes
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.Future
import akka.stream.stage.GraphStage
import akka.stream.FlowShape
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler
import akka.io.Tcp
import com.example.backend.resource.Store
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import com.example.backend.resource.StoreListener
import com.example.backend.resource.ChangeContext
import com.example.backend.resource.CreateChangeContext
import com.example.backend.resource.UpdateChangeContext
import com.example.backend.resource.DeleteChangeContext
import com.example.data.VersionedInstance
import akka.http.scaladsl.model.sse.ServerSentEvent
import com.example.data.websocket.DuplexProtocol.Send

object StoreMonitor {
  sealed trait ChatEvent
  case class NewParticipant(name: String, subscriber: ActorRef) extends ChatEvent
  case class ParticipantLeft(name: String) extends ChatEvent
  case class ReceivedMessage(sender: String, message: String) extends ChatEvent
  case class SendTo( to: String, msg: DuplexProtocol.DuplexMessage ) extends ChatEvent

  case class NewParticipantSSE( name: String, id: Id.MatchDuplicate, subscriber: ActorRef) extends ChatEvent

  var testHook: Option[ (akka.actor.Actor, Any) => Unit] = None

  def setTestHook( hook: (akka.actor.Actor, Any) => Unit ) = testHook = Some(hook)
  def unsetTestHook() = testHook = None

  case class KillOneConnection()

  val counter = new AtomicLong

}

abstract class BaseStoreMonitor[VId, VType <: VersionedInstance[VType,VType,VId]]( system: ActorSystem,
                                            store: Store[VId,VType],
                                            toProtocol: VType=>Protocol.ToBrowserMessage
                                          ) extends Subscriptions with Actor {
  import StoreMonitor._

  val log = Logging(system, getClass)

  protected def process( sender: String, msg: DuplexProtocol.DuplexMessage )

  protected def dispatchToAll( data: ToBrowserMessage ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAll: ${data}")
    dispatchToAll( Unsolicited(data) )
  }

  protected def dispatchToAllDuplicate( id: Id.MatchDuplicate, data: ToBrowserMessage ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAllDuplicate(${id}): ${data}")
    dispatchToFiltered( Unsolicited(data) )( DuplicateSubscription.filter(id) )
  }

  protected def dispatchToAllChicago( id: Id.MatchChicago, data: ToBrowserMessage ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAllChicago(${id}): ${data}")
    dispatchToFiltered( Unsolicited(data) )( ChicagoSubscription.filter(id) )
  }

  protected def dispatchToAllRubber( id: String, data: ToBrowserMessage ): Unit = {
    log.debug(s"BaseStoreMonitor.dispatchToAllRubber(${id}): ${data}")
    dispatchToFiltered( Unsolicited(data) )( RubberSubscription.filter(id) )
  }

  def receive: Receive = {
    case msg =>
      log.debug(s"StoreMonitor.receive $msg")
      try {
        testHook.foreach(hook => hook(this,msg))
      } catch {
        case x: Exception =>
          log.error(x, "Error in hook for message "+msg)
      }
      receiveImpl(msg)
  }

  def receiveImpl: Receive = {
    case NewParticipant(name, subscriber) =>
      log.info("("+name+"): New monitor")
      context.watch(subscriber)
      add(new Subscription( name, subscriber ))
      dispatchToAll(MonitorJoined(name, members ))

      // Debug code to terminate a session after 10 seconds
      if (false) {
        import scala.concurrent.ExecutionContext.Implicits.global
        Future {
          val sec: Long = 10
          log.info(s"""In $sec seconds will Send PoisonPill to $name $subscriber""")
          Thread.sleep(sec*1000)
          log.info(s"""Sending PoisonPill to $name $subscriber""")
          subscriber ! TerminateFlowStage.KillMsg
        }
      }
    case NewParticipantSSE(name, dupid, subscriber) =>
      log.info("("+name+"): New monitor")
      context.watch(subscriber)
      add(new Subscription( name, subscriber ))
      dispatchToAll(MonitorJoined(name, members ))
      process(name, Send(StartMonitor(dupid)) )

    case ReceivedMessage(senderid, message) =>
//      log.info("("+sender+"): Received "+message)
      process(senderid, DuplexProtocol.fromString(message))
    case msg: Protocol.ToBrowserMessage => // dispatch(msg)
      log.debug(s"Receive: ToBrowserMessage Sending to all ${msg}")
      msg match {
        case UpdateDuplicate(md) => dispatchToAllDuplicate(md.id, msg)
        case UpdateDuplicateHand(id,h) => dispatchToAllDuplicate(id, msg)
        case UpdateDuplicateTeam(id,t) => dispatchToAllDuplicate(id, msg)
        case _ =>
          dispatchToAll(msg)
      }
    case msg: DuplexProtocol.DuplexMessage => // dispatch(msg)
      log.debug(s"Receive: DuplexMessage Sending to all ${msg}")
      dispatchToAll(msg)
    case ParticipantLeft(name) =>
      log.info("("+name+"): Participant left")
      remove(name) match {
        case Some(sub) =>
          context.unwatch(sub.actor)
          sub.actor ! akka.actor.Status.Success(0)
        case None =>
      }
      dispatchToAll(MonitorLeft(name, members ))
    case SendTo(senderid,data) =>
      dispatchTo(data,senderid )
    case Terminated(subscriber) =>
      log.info("Subscriber terminated")
      // Will not happen if we got a ParticipantLeft from the subscriber
      // clean up dead subscribers, but should have been removed when `ParticipantLeft`
      context.unwatch(subscriber)
      remove(subscriber).foreach{ s =>
        log.info("("+s.id+"): Removed subscriber")
      }
    case x: KillOneConnection =>
      val ms = members
      if (ms.size > 0) {
        val sub = members(0)
        log.info(s"""Killing connection $sub""")
        remove(sub) match {
          case Some(s) =>
            log.info(s"""Sending PoisonPill to $sub""")
//            s.actor ! PoisonPill
            s.actor ! TerminateFlowStage.KillMsg
          case None =>
        }
      }
  }

}

class StoreMonitorManager[VId, VType <: VersionedInstance[VType,VType,VId]]( system: ActorSystem,
                                      store: Store[VId,VType]
                                    ) {
  val log = Logging(system, getClass)

  import StoreMonitor._

  val monitor = system.actorOf(Props( classOf[StoreMonitor], system, store ), name="StoreMonitorActor")

  // Wraps the chatActor in a sink. When the stream to this sink will be completed
  // it sends the `ParticipantLeft` message to the chatActor.
  // FIXME: here some rate-limiting should be applied to prevent single users flooding
  private def chatInSink(sender: String) = Sink.actorRef[ChatEvent](monitor, ParticipantLeft(sender))

  def monitorFlow(sender: RemoteAddress): Flow[String, Any, NotUsed] = {
    val count = StoreMonitor.counter.incrementAndGet()
    val name = sender.toString().replaceAll("\\.", "-")
    val in =
      Flow[String]
        .map{ s => {
          log.debug("Received on websocket("+sender+"): "+s)
          ReceivedMessage(sender.toString(), s)
        }}
        .to(chatInSink(sender.toString).addAttributes( Attributes(Name(s"sink-monitorFlow-$count.in-${name}"))))

    // The counter-part which is a source that will create a target ActorRef per
    // materialization where the chatActor will send its messages to.
    // This source will only buffer one element and will fail if the client doesn't read
    // messages fast enough.
    val out =
      Source.actorRef[Any](4, OverflowStrategy.fail)
        .addAttributes( Attributes(Name(s"source-monitorFlow-$count.out-${name}")))
        .mapMaterializedValue { actorref =>
          log.debug(s"Created new monitorFlow ${sender} ${actorref} ")
          monitor ! NewParticipant(sender.toString, actorref)
        }

    val f = Flow.fromSinkAndSource(in, out).via(new TerminateFlowStage(log,false))
    f
  }

  def monitorDuplicateSource( sender: RemoteAddress, id: Id.MatchDuplicate): Source[DuplexProtocol.DuplexMessage, _] = {
    Source.actorRef[DuplexProtocol.DuplexMessage](10, OverflowStrategy.fail ).
      mapMaterializedValue { x => monitor ! NewParticipantSSE(sender.toString(),id, x) }
  }
}

class TerminateFlowStage(
    log: LoggingAdapter,
    abort: Boolean = true)
  extends GraphStage[FlowShape[Any, Any]]
{
  val in = Inlet[Any]("TerminateFlowStage.in")
  val out = Outlet[Any]("TerminateFlowStage.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      import TerminateFlowStage._

      setHandlers(in, out, new InHandler with OutHandler {
        override def onPull(): Unit = { pull(in) }

        override def onPush(): Unit = {
          val chunk = grab(in)
          log.info(s"""TerminateFlowStage.onPush: ${chunk}""")
          chunk match {
            case dm: DuplexProtocol.DuplexMessage =>
              push( out, dm )
            case `KillMsg` =>
              if (abort) {
                log.info(s"""Sending abort really completeStage""")
//                push( out, Tcp.Abort )
//                failStage(new RuntimeException("Flow terminated by TerminateFlowStage"))
                completeStage()
              } else {
                log.info(s"""Sending completeStage""")
                completeStage()
              }
          }
        }
      })
  }
}

object TerminateFlowStage {
  val KillMsg = "Kill"
}

class StoreMonitor(system: ActorSystem,
                   store: Store[Id.MatchDuplicate,MatchDuplicate]
                  ) extends BaseStoreMonitor[Id.MatchDuplicate,MatchDuplicate](system, store, Protocol.UpdateDuplicate(_)) {

  override def process( sender: String, msg: DuplexProtocol.DuplexMessage ) = {
    msg match {
      case DuplexProtocol.Send(data) =>
        log.info("Processing "+msg)
        processProtocolMessage(sender,-1,data,true)
//        val resp = processProtocolMessage(sender,-1,data,true)
//        dispatchTo(sender,resp)
      case DuplexProtocol.Request(data, seq, ack) =>
        log.info("Processing "+msg)
        val resp = processProtocolMessage(sender,seq,data,ack)
        dispatchTo(resp, sender )
      case DuplexProtocol.LogEntryS(s) =>
        {
          val le = DuplexProtocol.fromString(s).asInstanceOf[DuplexProtocol.LogEntryV2]
          logFromBrowser(sender,le)
        }
//        dispatchTo(sender, DuplexProtocol.Response(NoData(),-1))
      case le: DuplexProtocol.LogEntryV2 =>
        {
          logFromBrowser(sender,le)
        }
//        dispatchTo(sender, DuplexProtocol.Response(NoData(),-1))
      case er: ErrorResponse =>
        log.warning("Unknown request "+er)
      case r: Response =>
        log.warning("Unknown request "+r)
      case u: Unsolicited =>
        log.warning("Unknown request "+u)

//      case _ =>
//        log.warning("Unknown request "+msg)
//        dispatchTo(sender, DuplexProtocol.Response(NoData(),-1))
    }
  }

  def logFromBrowser( ips: String, e: DuplexProtocol.LogEntryV2 ): Unit = {
    Service.logFromBrowser(ips, "ws", e)
  }

  def processProtocolMessage( sender: String, seq: Int, msg: Protocol.ToServerMessage, ack: Boolean ): DuplexProtocol.DuplexMessage = {
    import BridgeNestedResources._
    log.info("ProcessProtocolMessage("+seq+") "+msg)
    val resp = msg match {
      case UpdateDuplicate( dup ) =>
        log.warning("Updating the MatchDuplicate object is not supported")
        DuplexProtocol.ErrorResponse("Updating the MatchDuplicate object is not supported", seq)
      case UpdateDuplicateHand( dupid, hand ) =>
        Await.result( store.select(dupid).resourceBoards.select(hand.board).resourceHands.select(hand.id).update(hand), 30.seconds )
        dispatchToAllDuplicate(dupid,UpdateDuplicateHand(dupid,hand))
        DuplexProtocol.Response(if (ack) UpdateDuplicateHand( dupid, hand ) else NoData(),seq)
      case UpdateDuplicateTeam( dupid, team ) =>
        Await.result( store.select(dupid).resourceTeams.select(team.id).update(team), 30.seconds )
        dispatchToAllDuplicate(dupid,UpdateDuplicateTeam(dupid,team))
        DuplexProtocol.Response(if (ack) UpdateDuplicateTeam( dupid, team ) else NoData(), seq)
      case StartMonitor(dupid: Id.MatchDuplicate ) =>
        get(sender) match {
          case Some(sub) =>
            add( new DuplicateSubscription( sub, dupid ) )
          case None =>
        }
        Await.result( store.read(dupid), 30.seconds) match {
          case Right(dup) =>
            if (ack) {
              log.info("Sending MatchDuplicate to "+sender+": "+dup)
              dispatchTo(DuplexProtocol.Unsolicited( UpdateDuplicate(dup)), sender)
              DuplexProtocol.Response( NoData(), seq )
            } else {
              DuplexProtocol.Response( UpdateDuplicate( dup ), seq )
            }
          case _ =>
            DuplexProtocol.Response(NoData(),seq)
        }
      case StopMonitor(dupid) =>
        log.info(s"StopMonitor ${dupid}")
        get(sender) match {
          case Some(sub) =>
            add( sub.getSubscription() )
          case None =>
        }
        DuplexProtocol.Response(NoData(),seq)
      case NoData(_) =>
        log.debug("No data")
        DuplexProtocol.Response(NoData(),seq)
      case StartMonitorSummary(_) =>
        log.warning("StartMonitorSummary not implemented")
        DuplexProtocol.ErrorResponse("Unknown request", seq)
      case StopMonitorSummary(_) =>
        log.warning("StopMonitorSummary not implemented")
        DuplexProtocol.ErrorResponse("Unknown request", seq)

//      case _ =>
//        log.warning("Unknown request "+msg)
//        DuplexProtocol.ErrorResponse("Unknown request", seq)
    }
    log.info("ProcessProtocolMessage Response("+seq+") "+resp)
    resp
  }

  val listener = new Listener( log, self )

  override def register() = store.addListener(listener)
  override def unregister() = store.removeListener(listener)

}

class Listener( log: LoggingAdapter, actor: ActorRef ) extends StoreListener {

  override def create( context: ChangeContext ): Unit = update(context)
  override def update( context: ChangeContext ): Unit = {
    val changes = context.changes
    log.debug( "StoreMonitor.StoreListener "+changes.mkString("\n", "\n", ""))
    changes.headOption match {
      case Some(fcd) =>
        ((fcd match {
          case cc: CreateChangeContext => Some(cc.newValue)
          case uc: UpdateChangeContext => Some(uc.newValue)
          case dc: DeleteChangeContext => None   // can't happen here
        }) match {
          case Some(fdata) => fdata match {
            case md: MatchDuplicate => Some(md.id)
            case b: Board => None
            case h: DuplicateHand => None
          }
          case None => None
        }) match {
          case Some(mdid) =>
            (context.getSpecificChange() match {
              case Some(cd) => cd match {
                case cc: CreateChangeContext => Some(cc.newValue)
                case uc: UpdateChangeContext => Some(uc.newValue)
                case dc: DeleteChangeContext => None   // can't happen here
              }
              case None => None
            }) match {
              case Some(data) => data match {
                case md: MatchDuplicate => actor ! UpdateDuplicate(md)
                case b: Board =>
                case h: DuplicateHand => actor ! UpdateDuplicateHand(mdid,h)
                case t: Team => actor ! UpdateDuplicateTeam(mdid,t)
              }
              case None =>
            }
          case None =>
        }


      case None =>
    }
  }
  override def delete( context: ChangeContext ): Unit = {}
}
