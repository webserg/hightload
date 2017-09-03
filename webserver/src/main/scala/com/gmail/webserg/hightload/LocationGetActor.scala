package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter._

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

  def props: Props = Props[LocationGetActor]
}
