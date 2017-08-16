package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.Patterns
import com.gmail.webserg.hightload.UserDataReader.User
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

object UserFinder {
  val name = "users"

  def props: Props = Props[UserFinder]
}

class UserFinder extends Actor with ActorLogging {
  implicit val timeout = Timeout(40 millisecond)
  override def preStart() = {
    log.debug("Starting userFinder" + self.path)
    println("Starting userFinder" + self.path)
  }
  override def receive: Receive = {
    case id: Int =>
      log.debug("userFinder receive" + self.path)
      log.info("userFinder receive" + self.path)
      println("userFinder receive" + self.path)
      (context.actorSelection( "/user/"+UserLoaderActor.name + "/" + UserActor.name) ? id) to sender
  }
}
