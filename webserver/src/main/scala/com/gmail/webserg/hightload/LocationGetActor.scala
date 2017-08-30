package com.gmail.webserg.hightload

import java.time.LocalDate

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.LocationQueryActor.LocationAvgQueryResult
import com.gmail.webserg.hightload.QueryRouter._
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit

class LocationGetActor(var locations: Map[Int, Location])
  extends Actor with ActorLogging {

  override def preStart() = {
    log.debug("Starting LocationActor" + self.path)
  }

  override def receive: Receive = {

    case query: LocationQuery =>
      sender ! locations.get(query.id)

    case location: Location =>
      locations = locations + (location.id -> location)

  }

}


object LocationGetActor {
  val name: String = "getLocation"

  case class LocationAvgQueryResult(avg: Double)

  def props: Props = Props[LocationGetActor]
}
