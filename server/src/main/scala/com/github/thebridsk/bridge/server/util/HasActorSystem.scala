package com.github.thebridsk.bridge.server.util

import akka.actor.ActorSystem

trait HasActorSystem {
  implicit val actorSystem: ActorSystem
}
