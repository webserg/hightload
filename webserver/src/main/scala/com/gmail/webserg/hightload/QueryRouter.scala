package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onSuccess}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.gmail.webserg.hightload.QueryRouter._
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object QueryRouter {
  val name = "users"

  case class UserQuery(id: Int)

  case class VisitQuery(id: Int)

  case class LocationQuery(id: Int)

  case class VisitsQueryParameter(fromDate: Option[Long], toDate: Option[Long],
                                  country: Option[String], toDistance: Option[Int])

  case class UserPostQueryParameter(
                                     id: Option[Int],
                                     first_name: Option[String],
                                     last_name: Option[String],
                                     birth_date: Option[Long],
                                     gender: Option[String],
                                     email: Option[String])

  case class VisitsPostQueryParameter(
                                     id: Option[Int],
                                     location: Option[Int],
                                     user: Option[Int],
                                     visited_at: Option[Long],
                                     mark: Option[Int])

  case class LocationPostQueryParameter(
                                     id: Option[Int],
                                     place: Option[String],
                                     country: Option[String],
                                     city: Option[String],
                                     distance: Option[Int])


  case class LocationQueryParameter(fromDate: Option[Long], toDate: Option[Long],
                                    fromAge: Option[Int], toAge: Option[Int], gender: Option[String])

  case class VisitsQuery(id: Int, param: VisitsQueryParameter)

  case class UserPostQuery(id: Int, param: UserPostQueryParameter)
  case class VisitPostQuery(id: Int, param: VisitsPostQueryParameter)
  case class LocationPostQuery(id: Int, param: LocationPostQueryParameter)

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
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + UserQueryActor.name) ? q.id) to sender
    case q: UserPostQuery =>
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + UserQueryActor.name) ? q) to sender
    case q: UserPostQueryParameter =>
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + UserQueryActor.name) ? q) to sender
    case q: User =>
      context.actorSelection("/user/" + DataLoaderActor.name + "/" + LocationQueryActor.name) ? q
      context.actorSelection("/user/" + DataLoaderActor.name + "/" + VisitQueryActor.name) ? q
    case q: VisitsPostQueryParameter =>
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + VisitQueryActor.name) ? q) to sender
    case q: Visit =>
      context.actorSelection("/user/" + DataLoaderActor.name + "/" + LocationQueryActor.name) ? q
      context.actorSelection("/user/" + DataLoaderActor.name + "/" + UserQueryActor.name) ? q
    case q: VisitQuery =>
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + VisitQueryActor.name) ? q.id) to sender
    case q: VisitsQuery =>
      log.debug(q + "")
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + VisitQueryActor.name) ? q) to sender
    case q: LocationAvgQuery =>
      log.debug(q + "")
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + LocationQueryActor.name) ? q) to sender
    case q: LocationQuery =>
      (context.actorSelection("/user/" + DataLoaderActor.name + "/" + LocationQueryActor.name) ? q) to sender
  }
}
