package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{ask, pipe}
import akka.routing.Broadcast
import akka.util.Timeout
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter._
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.WebServer.ActorAddresses

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object QueryRouter {
  val name = "queryRouter"

  case class UserQuery(id: Int)

  case class VisitQuery(id: Int)

  case class LocationQuery(id: Int)

  case class UserVisitsQueryParameter(fromDate: Option[Long] = None, toDate: Option[Long] = None,
                                      country: Option[String] = None, toDistance: Option[Int] = None)

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

  case class UserVisitsQuery(id: Int, param: UserVisitsQueryParameter)

  case class UserPostQuery(id: Int, param: UserPostQueryParameter)

  case class VisitPostQuery(id: Int, param: VisitsPostQueryParameter)

  case class LocationPostQuery(id: Int, param: LocationPostQueryParameter)

  case class LocationAvgQuery(id: Int, param: LocationQueryParameter)

  def props: Props = Props[QueryRouter]
}

class QueryRouter(addr: ActorAddresses) extends Actor with ActorLogging {
  implicit val timeout = Timeout(3000 millisecond)

  override def preStart() = {
    log.debug("Starting QueryRouter" + self.path)
  }


  override def receive: Receive = {
    case q: UserQuery =>
      (addr.userActor ? q.id) to sender

    case q: UserPostQuery =>
      (addr.userActor ? q) to sender

    case q: UserPostQueryParameter =>
      (addr.userActor ? q) to sender

    case q: User =>
      addr.locationActor ! Broadcast(q)
      addr.visitActor ! Broadcast(q)

    case q: VisitsPostQueryParameter =>
      (addr.visitActor ? Broadcast(q)) to sender

    case q: VisitPostQuery =>
      (addr.visitActor ? Broadcast(q)) to sender

    case q: Visit =>
      addr.locationActor ! Broadcast(q)

    case q: VisitQuery =>
      (addr.visitActor ? q.id) to sender

    case q: UserVisitsQuery =>
      (addr.visitActor ? q) to sender

    case q: LocationAvgQuery =>
      (addr.locationActor ? q) to sender

    case q: LocationQuery =>
      (addr.locationGetActor ? q) to sender

    case q: LocationPostQueryParameter =>
      (addr.locationActor ? q) to sender

    case q: LocationPostQuery =>
      (addr.locationActor ? q) to sender

    case q: Location =>
      addr.visitActor ! Broadcast(q)
      addr.locationGetActor ! q
  }
}
