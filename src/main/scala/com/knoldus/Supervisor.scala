package com.knoldus

import java.io.File

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorKilledException, ActorRef, DeathPactException, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.{ask, pipe}
import akka.routing.RoundRobinPool
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class Supervisor extends Actor {

  override def receive: Receive = {
    case path: String =>
      val master = context.actorOf(RoundRobinPool(5).props(Props[FileMiner]).withDispatcher("fixed-thread-pool"), "master")
      val fileList: List[String] = fileFinder(path)
      val listOfFuture: List[Future[ActorDataStructure]] = futureListFinder(master, fileList, List())
      Future.sequence(listOfFuture).map(_.foldLeft(ActorDataStructure(0, 0, 0)) { (acc, ele) => futureResultFinder(acc, ele) }).pipeTo(sender)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 10 seconds) {
    case _: ActorKilledException => Stop
    case _: DeathPactException => Stop
    case _: Exception => Escalate
  }

  def futureListFinder(master: ActorRef, fileList: List[String], futureList: List[Future[ActorDataStructure]]): List[Future[ActorDataStructure]] = {
    implicit val timeout: Timeout = Timeout(5 second)
    fileList match {
      case Nil => futureList
      case first :: rest =>
        val futureLogfound = (master ? first).mapTo[ActorDataStructure]
        futureListFinder(master, rest, futureLogfound :: futureList)
    }
  }

  def futureResultFinder(accumelator: ActorDataStructure, element: ActorDataStructure): ActorDataStructure = {
    ActorDataStructure(accumelator.countError + element.countError, accumelator.countWarnings + element.countWarnings, accumelator.countInfo + element.countInfo)
  }

  def fileFinder(path: String): List[String] = {
    val filePointer = new File(path)
    filePointer.listFiles.toList.map(_.toString)
  }


}
