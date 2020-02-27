package com.knoldus

import java.io.File
import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor.{Actor, ActorKilledException, ActorRef, DeathPactException, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.{ask, pipe}
import akka.routing.RoundRobinPool
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * This actor class controls complete opration of system
 */
class Supervisor extends Actor with akka.actor.ActorLogging{

  private val poolSize=5
  private val maxNrOfRetries=5
  private val withinTimeRange=10 seconds

  override def receive: Receive = {

    case path: String =>
      val master = context.actorOf(RoundRobinPool(poolSize).props(Props[FileMiner]))
      val fileList: List[String] = fileFinder(path)
      val listOfFuture: List[Future[ActorDataStructure]] = futureListFinder(master, fileList, List())
      val res=Future.sequence(listOfFuture).map(_.foldLeft(ActorDataStructure(0, 0, 0)) { (acc, ele) => futureResultFinder(acc, ele) })

      val errors=res.map(messageStructure=> messageStructure.countError/fileList.length).pipeTo(sender())
      res.map(res=>log.info(s"$res AverageOfErrors : $errors"))


  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries, withinTimeRange) {
    case _: ActorKilledException => Stop
    case _: DeathPactException => Stop
    case _: Exception => Escalate
  }

  /**
   * This function makes List which contains the future log result master actor.
   * @param master Router.
   * @param fileList All log files in provided path.
   * @param futureList Result of every log files.
   * @return List which contain result.
   */
  def futureListFinder(master: ActorRef, fileList: List[String], futureList: List[Future[ActorDataStructure]]): List[Future[ActorDataStructure]] = {
    implicit val timeout: Timeout = Timeout(5 second)
    fileList match {
      case Nil => futureList
      case first :: rest =>
        val futureLogfound = (master ? first).mapTo[ActorDataStructure]
        futureListFinder(master, rest, futureLogfound :: futureList)
    }
  }

  /**
   * This function adds result of every log file
   * @param accumelator Accumulator
   * @param element Result of every log file
   * @return Sum of result
   */
  def futureResultFinder(accumelator: ActorDataStructure, element: ActorDataStructure): ActorDataStructure = {
    ActorDataStructure(accumelator.countError + element.countError, accumelator.countWarnings
      + element.countWarnings, accumelator.countInfo + element.countInfo)
  }

  /**
   * It finds all file present in given directory
   * @param path Path of provided directory
   * @return List of files in that directory
   */
  def fileFinder(path: String): List[String] = {
    val filePointer = new File(path)
    filePointer.listFiles.toList.map(_.toString)
  }


}
