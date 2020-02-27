package com.knoldus

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * This class triggers the complete system after particular duration
 * @param path Location where all log file are
 */
class HealthAnalyser(path: String) {



   private implicit val timeout: Timeout = Timeout(5 second)
   private val actorSystem = ActorSystem("firstActorSystem")
   private val supervisor = actorSystem.actorOf(Props[Supervisor].withDispatcher("fixed-thread-poo"), "Supervisor")
   actorSystem.scheduler.schedule(0 milliseconds, 50 milliseconds, supervisor, path)








}
