package com.example.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest.Matchers
import utils.logging.Logger
import java.util.logging.Level
import com.example.backend.BridgeServiceInMemory
import com.example.backend.Subscriptions
import akka.actor.Actor
import com.example.backend.Subscription
import com.example.backend.DuplicateSubscription

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

class TestSubscription extends FlatSpec with MustMatchers {

  val log = Logger[TestSubscription]

  behavior of "Subscription"



  it should "register and unregister" in {
    val ms = new MySubscription

    ms.add(new DuplicateSubscription( "1", Actor.noSender, "M1" ) )
    ms.registerCalled mustBe true
    ms.unregisterCalled mustBe false

    ms.clear

    ms.add(new DuplicateSubscription( "2", Actor.noSender, "M2" ) )
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe false

    ms.clear

    ms.remove("1")
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe false

    ms.clear

    ms.add(new DuplicateSubscription( "2", Actor.noSender, "M3" ) )
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe false

    ms.clear

    ms.add( ms.get("2").get.getSubscription() )
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe true

    ms.clear

    ms.add(new DuplicateSubscription( "2", Actor.noSender, "M3" ) )
    ms.registerCalled mustBe true
    ms.unregisterCalled mustBe false

    ms.clear

    ms.remove("2")
    ms.registerCalled mustBe false
    ms.unregisterCalled mustBe true

  }
}
