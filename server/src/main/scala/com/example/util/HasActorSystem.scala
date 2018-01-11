package com.example.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait HasActorSystem {
  implicit val actorSystem: ActorSystem
  implicit val materializer: ActorMaterializer
}
