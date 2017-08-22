package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.gmail.webserg.hightload.QueryRouter._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object QueryRouter {
  val name = "users"

  case class UserQuery(id: Int)

  case class VisitQuery(id: Int)

  case class LocationQuery(id: Int)

  case class VisitsQueryParameter(fromDate: Option[Long], toDate: Option[Long],
                                  country: Option[String], toDistance: Option[Int])

  case class LocationQueryParameter(fromDate: Option[Long], toDate: Option[Long],
                                    fromAge: Option[Int], toAge: Option[Int], gender: Option[String])

  case class VisitsQuery(id: Int, param: VisitsQueryParameter)

  case class LocationAvgQuery(id: Int, param: LocationQueryParameter)

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
    case q: VisitQuery =>
      log.debug("QueryRouter" + q + self.path)
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + VisitQueryActor.name) ? q.id) to sender
    case q: VisitsQuery =>
      log.debug(q + "")
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + VisitQueryActor.name) ? q) to sender
    case q: LocationAvgQuery =>
      log.debug(q + "")
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + LocationQueryActor.name) ? q) to sender
    case q:LocationQuery =>
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + LocationQueryActor.name) ? q) to sender
  }
}
