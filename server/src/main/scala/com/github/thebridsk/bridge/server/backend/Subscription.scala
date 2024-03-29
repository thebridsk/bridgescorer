package com.github.thebridsk.bridge.server.backend

import akka.actor.ActorRef
import akka.actor.Actor
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.DuplexMessage
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.IndividualDuplicate

class Subscription(val id: String, val actor: ActorRef) {

  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit =
    actor ! message

  def getSubscription() = new Subscription(id, actor)

  def needsStoreMonitored = false
}

class DuplicateSubscription(
    id: String,
    actor: ActorRef,
    val matchid: MatchDuplicate.Id
) extends Subscription(id, actor) {

  def this(subscription: Subscription, matchid: MatchDuplicate.Id) =
    this(subscription.id, subscription.actor, matchid)

  def isMatch(mid: MatchDuplicate.Id): Boolean = matchid == mid

  override def needsStoreMonitored = true
}

object DuplicateSubscription {

  def apply(id: String, actor: ActorRef, matchid: MatchDuplicate.Id) =
    new DuplicateSubscription(id, actor, matchid)

  def unapply(
      obj: DuplicateSubscription
  ): Option[(String, ActorRef, MatchDuplicate.Id)] = {
    Some((obj.id, obj.actor, obj.matchid))
  }

  def filter(id: MatchDuplicate.Id)(sub: Subscription): Boolean = {
    sub match {
      case DuplicateSubscription(sid, actor, mid) if (mid == id) => true
      case _                                                     => false
    }
  }

}

class IndividualDuplicateSubscription(
    id: String,
    actor: ActorRef,
    val matchid: IndividualDuplicate.Id
) extends Subscription(id, actor) {

  def this(subscription: Subscription, matchid: IndividualDuplicate.Id) =
    this(subscription.id, subscription.actor, matchid)

  def isMatch(mid: IndividualDuplicate.Id): Boolean = matchid == mid

  override def needsStoreMonitored = true
}

object IndividualDuplicateSubscription {

  def apply(id: String, actor: ActorRef, matchid: IndividualDuplicate.Id) =
    new IndividualDuplicateSubscription(id, actor, matchid)

  def unapply(
      obj: IndividualDuplicateSubscription
  ): Option[(String, ActorRef, IndividualDuplicate.Id)] = {
    Some((obj.id, obj.actor, obj.matchid))
  }

  def filter(id: IndividualDuplicate.Id)(sub: Subscription): Boolean = {
    sub match {
      case IndividualDuplicateSubscription(sid, actor, mid) if (mid == id) => true
      case _                                                               => false
    }
  }

}

class ChicagoSubscription(
    id: String,
    actor: ActorRef,
    val matchid: MatchChicago.Id
) extends Subscription(id, actor) {

  def this(subscription: Subscription, matchid: MatchChicago.Id) =
    this(subscription.id, subscription.actor, matchid)

  def isMatch(mid: MatchChicago.Id): Boolean = matchid == mid

  override def needsStoreMonitored = true
}

object ChicagoSubscription {

  def apply(id: String, actor: ActorRef, matchid: MatchChicago.Id) =
    new ChicagoSubscription(id, actor, matchid)

  def unapply(
      obj: ChicagoSubscription
  ): Option[(String, ActorRef, MatchChicago.Id)] = {
    Some((obj.id, obj.actor, obj.matchid))
  }

  def filter(id: MatchChicago.Id)(sub: Subscription): Boolean = {
    sub match {
      case ChicagoSubscription(sid, actor, mid) if (mid == id) => true
      case _                                                   => false
    }
  }

}

class RubberSubscription(
    id: String,
    actor: ActorRef,
    val matchid: MatchRubber.Id
) extends Subscription(id, actor) {

  def this(subscription: Subscription, matchid: MatchRubber.Id) =
    this(subscription.id, subscription.actor, matchid)

  def isMatch(mid: String): Boolean = matchid == mid

  override def needsStoreMonitored = true
}

object RubberSubscription {

  def apply(id: String, actor: ActorRef, matchid: MatchRubber.Id) =
    new RubberSubscription(id, actor, matchid)

  def unapply(
      obj: RubberSubscription
  ): Option[(String, ActorRef, MatchRubber.Id)] = {
    Some((obj.id, obj.actor, obj.matchid))
  }

  def filter(id: MatchRubber.Id)(sub: Subscription): Boolean = {
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
  def add(sub: Subscription): Option[Subscription] =
    checkRegister {
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
  def remove(id: String): Option[Subscription] =
    checkRegister {
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

  def get(id: String): Option[Subscription] = subscriptions.get(id)

  def dispatchTo(message: DuplexMessage, id: String)(implicit
      sender: ActorRef = Actor.noSender
  ): Unit = {
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
  )(implicit sender: ActorRef = Actor.noSender): Unit = {
    subscriptions.values.filter(filt).foreach(s => s ! message)
  }

  def filter(filt: Subscription => Boolean): Iterable[Subscription] = {
    subscriptions.values.filter(filt)
  }

  def getDuplicate: Iterable[DuplicateSubscription] =
    subscriptions.values.flatMap { sub =>
      sub match {
        case s: DuplicateSubscription => s :: Nil
        case _                        => Nil
      }
    }

  def getChicago: Iterable[ChicagoSubscription] =
    subscriptions.values.flatMap { sub =>
      sub match {
        case s: ChicagoSubscription => s :: Nil
        case _                      => Nil
      }
    }

  def getRubber: Iterable[RubberSubscription] =
    subscriptions.values.flatMap { sub =>
      sub match {
        case s: RubberSubscription => s :: Nil
        case _                     => Nil
      }
    }
}

object Subscriptions {
  val log: Logger = Logger[Subscriptions]()
}
