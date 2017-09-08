package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter._

class LocationQueryActor(var locations: Map[Int, Location])
  extends Actor with ActorLogging {

  override def preStart() = {
    log.debug("Starting LocationActor" + self.path)
  }


  def remove(num: Int, list: List[Int]) = list diff List(num)

  override def receive: Receive = {

    case query: LocationQuery =>
      sender ! locations.get(query.id)


    case q: LocationPostQueryParameter =>
      if (LocationQueryActor.validateNewPostLocationQuery(q)) {
        val loc = locations.get(q.id.get)
        if (loc.isEmpty) {
          sender ! Some("{}")
          val nid = q.id.get
          val ncountry = q.country.get
          val ncity = q.city.get
          val nplace = q.place.get
          val ndist = q.distance.get
          val newLoc = Location(nid, nplace, ncountry, ncity, ndist)
          locations = locations + (nid -> newLoc)
          context.actorSelection("/user/" + QueryRouter.name) ! newLoc
        }
      }
      else sender() ! None

    case q: LocationPostQuery =>
      val loc = locations.get(q.id)
      if (loc.isDefined && q.param.id.isEmpty) {
        sender ! Some("{}")
        val oldLoc = loc.get
        val nid = q.id
        val ncountry = q.param.country.getOrElse(oldLoc.country)
        val ncity = q.param.city.getOrElse(oldLoc.city)
        val nplace = q.param.place.getOrElse(oldLoc.place)
        val ndist = q.param.distance.getOrElse(oldLoc.distance)
        val newLoc = Location(nid, nplace, ncountry, ncity, ndist)
        locations = locations + (nid -> newLoc)
        context.actorSelection("/user/" + QueryRouter.name) ! newLoc
      }
      else sender() ! None
  }


}


object LocationQueryActor {
  val name: String = "location"

  case class LocationAvgQueryResult(avg: Double)

  def props: Props = Props[LocationQueryActor]

  def validateNewPostLocationQuery(q: LocationPostQueryParameter): Boolean = {
    q.id.isDefined && q.city.isDefined && q.country.isDefined && q.distance.isDefined && q.place.isDefined
  }
}
