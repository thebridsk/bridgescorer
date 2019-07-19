package com.github.thebridsk.bridge.server.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait HasActorSystem {
  implicit val actorSystem: ActorSystem
  implicit val materializer: ActorMaterializer
}
