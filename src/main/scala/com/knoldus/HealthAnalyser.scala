package com.knoldus

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class HealthAnalyser(path: String) {
  connector()

  def connector():Future[ActorDataStructure]= {
    implicit val timeout: Timeout = Timeout(5 second)
    val actorSystem = ActorSystem("firstActorSystem")
    val supervisor = actorSystem.actorOf(Props[Supervisor], "Supervisor")
    val result = (supervisor ? path).mapTo[ActorDataStructure]
    result


  }


}
