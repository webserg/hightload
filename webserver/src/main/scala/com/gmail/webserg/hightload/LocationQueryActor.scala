package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.LocationQueryActor.LocationAvgQueryResult
import com.gmail.webserg.hightload.QueryRouter.{LocationAvgQuery, LocationQuery}
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit

class LocationQueryActor(var users: Map[Int, User], var locations: Map[Int, Location], var locationVisits: Map[Int, Map[Int, Visit]])
  extends Actor with ActorLogging {

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
        implicit class FilterHelper[A](l: List[A]) {
          def ifFilter(cond: Boolean, f: A => Boolean) = {
            if (cond) l.filter(f) else l
          }
        }
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
  }

  val SECONDS: Int = 24 * 60 * 60

  import java.time.LocalDate

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
