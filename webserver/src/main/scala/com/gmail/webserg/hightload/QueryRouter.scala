package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.Patterns
import com.gmail.webserg.hightload.UserDataReader.User
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.gmail.webserg.hightload.QueryRouter.{UserQuery, VisitsQuery}

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

object QueryRouter {
  val name = "users"
  case class UserQuery(id:Int)
  case class VisitsQuery(id:Int)
  def props: Props = Props[QueryRouter]
}

class QueryRouter extends Actor with ActorLogging {
  implicit val timeout = Timeout(10 millisecond)

  override def preStart() = {
    log.debug("Starting QueryRouter" + self.path)
  }

  override def receive: Receive = {
    case q: UserQuery =>
      log.debug("QueryRouter" + q + self.path)
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + UserQueryActor.name) ? q.id) to sender
    case q: VisitsQuery =>
      log.debug(q + "")
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + VisitQueryActor.name) ? q) to sender
  }
}
