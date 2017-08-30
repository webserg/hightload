package com.gmail.webserg.hightload

import java.time.LocalDate

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.LocationQueryActor.LocationAvgQueryResult
import com.gmail.webserg.hightload.QueryRouter._
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit

class LocationQueryActor(
                         var users: Map[Int, User], var locations: Map[Int, Location], var locationVisits: Map[Int, Map[Int, Visit]])
  extends Actor with ActorLogging {

  override def preStart() = {
    log.debug("Starting LocationActor" + self.path)
  }


  implicit class FilterHelper[A](l: List[A]) {
    def ifFilter(cond: Boolean, f: A => Boolean) = {
      if (cond) l.filter(f) else l
    }
  }

  def validateNewPostLocationQuery(q: LocationPostQueryParameter): Boolean = {
    q.id.isDefined && q.city.isDefined && q.country.isDefined && q.distance.isDefined && q.place.isDefined
  }

  override def receive: Receive = {

    case query: LocationQuery =>
      sender ! locations.get(query.id)

    case user: User =>
      users = users + (user.id -> user)

    case visit: Visit =>
      locationVisits = locationVisits + (visit.location -> (locationVisits.getOrElse(visit.location, Map()) + (visit.id -> visit)))

    case location: Location => locations + (location.id -> location)

    case query: LocationAvgQuery =>
      val locationsOpt = locationVisits.get(query.id)
      if (locationsOpt.isDefined) {
        val locs = locationsOpt.get.values.toList
        val filtered = locs
          .ifFilter(query.param.fromDate.isDefined, _.visited_at >= query.param.fromDate.get)
          .ifFilter(query.param.toDate.isDefined, _.visited_at < query.param.toDate.get)
          .ifFilter(query.param.fromAge.isDefined, v => getAge(users(v.user).birth_date) >= query.param.fromAge.get)
          .ifFilter(query.param.toAge.isDefined, v => getAge(users(v.user).birth_date) <= query.param.toAge.get)
          .ifFilter(query.param.gender.isDefined, v => users(v.user).gender.equalsIgnoreCase(query.param.gender.get))


        val marks = filtered.map(v => v.mark)
        val avgTmp = if (marks.isEmpty) 0.0 else marks.sum.toDouble / marks.length
        val avg = BigDecimal(avgTmp).setScale(5, BigDecimal.RoundingMode.HALF_UP).toDouble
        sender ! Option(LocationAvgQueryResult(avg))
      } else if (locations.get(query.id).isDefined) {
        sender ! Option(LocationAvgQueryResult(0.0))
      } else {

        sender ! None
      }

    case q: LocationPostQueryParameter =>
      if (validateNewPostLocationQuery(q)) {
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


  val SECONDS: Int = 24 * 60 * 60

  val today: LocalDate = LocalDate.now

  private def getAge(bd: Long) = {
    today.getYear - LocalDate.ofEpochDay(bd / SECONDS).getYear
  }
}


object LocationQueryActor {
  val name: String = "location"

  case class LocationAvgQueryResult(avg: Double)

  def props: Props = Props[LocationQueryActor]
}
