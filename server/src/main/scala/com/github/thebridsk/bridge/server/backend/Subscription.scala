package com.github.thebridsk.bridge.server.backend

import akka.actor.ActorRef
import com.github.thebridsk.bridge.data.Id
import akka.actor.Actor
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.DuplexMessage
import com.github.thebridsk.utilities.logging.Logger

class Subscription(val id: String, val actor: ActorRef) {

  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit =
    actor ! message

  def getSubscription() = new Subscription(id, actor)

  def needsStoreMonitored = false
}

class DuplicateSubscription(
    id: String,
    actor: ActorRef,
    val matchid: Id.MatchDuplicate
) extends Subscription(id, actor) {

  def this(subscription: Subscription, matchid: Id.MatchDuplicate) =
    this(subscription.id, subscription.actor, matchid)

  def isMatch(mid: Id.MatchDuplicate) = matchid == mid

  override def needsStoreMonitored = true
}

object DuplicateSubscription {

  def apply(id: String, actor: ActorRef, matchid: Id.MatchDuplicate) =
    new DuplicateSubscription(id, actor, matchid)

  def unapply(
      obj: DuplicateSubscription
  ): Option[(String, ActorRef, Id.MatchDuplicate)] = {
    Some((obj.id, obj.actor, obj.matchid))
  }

  def filter(id: Id.MatchDuplicate)(sub: Subscription) = {
    sub match {
      case DuplicateSubscription(sid, actor, mid) if (mid == id) => true
      case _                                                     => false
    }
  }

}

class ChicagoSubscription(
    id: String,
    actor: ActorRef,
    val matchid: Id.MatchChicago
) extends Subscription(id, actor) {

  def this(subscription: Subscription, matchid: Id.MatchDuplicate) =
    this(subscription.id, subscription.actor, matchid)

  def isMatch(mid: Id.MatchChicago) = matchid == mid

  override def needsStoreMonitored = true
}

object ChicagoSubscription {

  def apply(id: String, actor: ActorRef, matchid: Id.MatchChicago) =
    new ChicagoSubscription(id, actor, matchid)

  def unapply(
      obj: ChicagoSubscription
  ): Option[(String, ActorRef, Id.MatchChicago)] = {
    Some((obj.id, obj.actor, obj.matchid))
  }

  def filter(id: Id.MatchChicago)(sub: Subscription) = {
    sub match {
      case ChicagoSubscription(sid, actor, mid) if (mid == id) => true
      case _                                                   => false
    }
  }

}

class RubberSubscription(id: String, actor: ActorRef, val matchid: String)
    extends Subscription(id, actor) {

  def this(subscription: Subscription, matchid: Id.MatchDuplicate) =
    this(subscription.id, subscription.actor, matchid)

  def isMatch(mid: String) = matchid == mid

  override def needsStoreMonitored = true
}

object RubberSubscription {

  def apply(id: String, actor: ActorRef, matchid: String) =
    new RubberSubscription(id, actor, matchid)

  def unapply(obj: RubberSubscription): Option[(String, ActorRef, String)] = {
    Some((obj.id, obj.actor, obj.matchid))
  }

  def filter(id: String)(sub: Subscription) = {
    sub match {
      case RubberSubscription(sid, actor, mid) if (mid == id) => true
      case _                                                  => false
    }
  }

}

abstract class Subscriptions {
  import Subscriptions._

  private var subscriptions = Map[String, Subscription]()

  @volatile
  private var storeCounter = 0

  def register(): Unit
  def unregister(): Unit

  private def checkRegister[R](f: => R) = {
    val oldCount = storeCounter
    val r = f
    val newCount = storeCounter
    if (newCount == 0) {
      if (oldCount > 0) unregister()
    } else if (newCount > 0) {
      if (oldCount == 0) register()
    }
    r
  }

  /**
    * Add a new subscription.  This will replace an existing subscription with the same ID.
    * @return the old subscription or None if there was no old
    */
  def add(sub: Subscription) = checkRegister {
    val old = subscriptions.get(sub.id)
    subscriptions += (sub.id -> sub)
    old match {
      case Some(oldsub) =>
        if (oldsub.needsStoreMonitored) storeCounter = storeCounter - 1
      case None =>
    }
    if (sub.needsStoreMonitored) storeCounter = storeCounter + 1
    old
  }

  /**
    * @return the old subscription or None if there was no old
    */
  def remove(id: String): Option[Subscription] = checkRegister {
    val old = subscriptions.get(id)
    subscriptions -= id
    old match {
      case Some(oldsub) =>
        if (oldsub.needsStoreMonitored) storeCounter = storeCounter - 1
      case None =>
    }
    old
  }

  def remove(actor: ActorRef): Iterable[Subscription] = {
    val removed = subscriptions.values.filter(s => s.actor == actor)
    removed.foreach { oldsub =>
      remove(oldsub.id)
    }
    removed
  }

  def size = subscriptions.size

  def members = subscriptions.keySet.toList

  def get(id: String) = subscriptions.get(id)

  def dispatchTo(message: DuplexMessage, id: String)(
      implicit sender: ActorRef = Actor.noSender
  ) = {
    get(id) match {
      case Some(sub) => sub ! message
      case None =>
        throw new IllegalArgumentException(
          s"Subscription with id ${id} not found"
        )
    }
  }

  def dispatchToAll(
      message: DuplexMessage
  )(implicit sender: ActorRef = Actor.noSender): Unit = {
    log.fine("Sending " + message)
    subscriptions.values.foreach { s =>
      log.fine(s"Sending to ${s.id} ${s.actor} ${message}")
      s ! message
    }
  }

  def dispatchToFiltered(message: DuplexMessage)(
      filt: Subscription => Boolean
  )(implicit sender: ActorRef = Actor.noSender) = {
    subscriptions.values.filter(filt).foreach(s => s ! message)
  }

  def filter(filt: Subscription => Boolean) = {
    subscriptions.values.filter(filt)
  }

  def getDuplicate = subscriptions.values.flatMap { sub =>
    sub match {
      case s: DuplicateSubscription => s :: Nil
      case _                        => Nil
    }
  }

  def getChicago = subscriptions.values.flatMap { sub =>
    sub match {
      case s: ChicagoSubscription => s :: Nil
      case _                      => Nil
    }
  }

  def getRubber = subscriptions.values.flatMap { sub =>
    sub match {
      case s: RubberSubscription => s :: Nil
      case _                     => Nil
    }
  }
}

object Subscriptions {
  val log = Logger[Subscriptions]()
}
