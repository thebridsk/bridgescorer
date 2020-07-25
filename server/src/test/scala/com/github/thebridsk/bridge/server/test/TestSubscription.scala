package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.backend.Subscriptions
import akka.actor.Actor
import com.github.thebridsk.bridge.server.backend.DuplicateSubscription
import com.github.thebridsk.bridge.data.MatchDuplicate

class MySubscription extends Subscriptions {

  var registerCalled = false
  var unregisterCalled = false

  def clear = {
    registerCalled = false
    unregisterCalled = false
  }

  def register(): Unit = {
    registerCalled = true
  }

  def unregister(): Unit = {
    unregisterCalled = true
  }

}

class TestSubscription extends AnyFlatSpec with Matchers {

  val log = Logger[TestSubscription]()

  behavior of "Subscription"



  it should "register and unregister" in {
    val ms = new MySubscription

    ms.add(new DuplicateSubscription( "1", Actor.noSender, MatchDuplicate.id(1) ) )
    ms.registerCalled mustBe true
    ms.unregisterCalled mustBe false

    ms.clear

    ms.add(new DuplicateSubscription( "2", Actor.noSender, MatchDuplicate.id(2) ) )
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe false

    ms.clear

    ms.remove("1")
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe false

    ms.clear

    ms.add(new DuplicateSubscription( "2", Actor.noSender, MatchDuplicate.id(3) ) )
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe false

    ms.clear

    ms.add( ms.get("2").get.getSubscription() )
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe true

    ms.clear

    ms.add(new DuplicateSubscription( "2", Actor.noSender, MatchDuplicate.id(3) ) )
    ms.registerCalled mustBe true
    ms.unregisterCalled mustBe false

    ms.clear

    ms.remove("2")
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe true

  }
}
