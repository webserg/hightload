package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter.{UserVisitsQuery, VisitPostQuery, VisitsPostQueryParameter}
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult

import scala.collection.parallel.ParIterable
import scala.collection.parallel.immutable.ParSeq

class VisitQueryActor(var users: Map[Int, User],
                      var visits: Map[Int, Visit],
                      var locations: Map[Int, Location],
                      var userVisits: Map[Int, Map[Int, Visit]],
                      var locationVisits: Map[Int, Map[Int, Visit]])
  extends Actor with ActorLogging {

  override def preStart() = {
    log.debug("Starting QueryRouter" + self.path)
  }


  def validateNewPostVisitQuery(q: VisitsPostQueryParameter): Boolean = {
    q.id.isDefined && q.user.isDefined && q.location.isDefined && q.visited_at.isDefined && q.mark.isDefined &&
      users.get(q.user.get).isDefined && locations.get(q.location.get).isDefined

  }

  implicit class FilterHelper[A](l: ParIterable[A]) {
    def ifFilter(cond: Boolean, f: A => Boolean) = {
      if (cond) l.filter(f) else l
    }
  }


  override def receive: Receive = {
    case id: Int =>
      sender ! visits.get(id)

    case user: User =>
      users = users + (user.id -> user)

    case location: Location =>
      locations = locations + (location.id -> location)

    case q: VisitPostQuery =>
      val visit = visits.get(q.id)
      if (visit.isDefined) {
        val oldVisit = visit.get
        val nlocation = q.param.location.getOrElse(oldVisit.location)
        val nuser = q.param.user.getOrElse(oldVisit.user)
        val nmark = q.param.mark.getOrElse(oldVisit.mark)
        val nvisit_at = q.param.visited_at.getOrElse(oldVisit.visited_at)
        val newVisit = Visit(oldVisit.id, nlocation, nuser, nvisit_at, nmark)
        visits = visits + (q.id -> newVisit)
        userVisits.getOrElse(nuser, Map()) + (newVisit.id -> newVisit)
        locationVisits.getOrElse(newVisit.location, Map()) + (newVisit.id -> newVisit)
        sender() ! Some(newVisit)

      } else sender() ! None

    case q: VisitsPostQueryParameter =>
      if (validateNewPostVisitQuery(q)) {
        val nid = q.id.get
        val newVisit = Visit(nid, q.location.get, q.user.get, q.visited_at.get, q.mark.get)
        visits = visits + (nid -> newVisit)
        userVisits = userVisits + (newVisit.user -> (userVisits.getOrElse(newVisit.user, Map()) + (newVisit.id -> newVisit)))
        locationVisits.getOrElse(newVisit.location, Map()) + (newVisit.id -> newVisit)
        sender() ! Some(newVisit)

      } else sender() ! None


    case queryUserVisits: UserVisitsQuery =>

      val userVisitsRes = userVisits.get(queryUserVisits.id)
      if (userVisitsRes.isDefined) {

        val filtered = userVisitsRes.get.values.par
          .ifFilter(queryUserVisits.param.fromDate.isDefined, _.visited_at >= queryUserVisits.param.fromDate.get)
          .ifFilter(queryUserVisits.param.toDate.isDefined, _.visited_at < queryUserVisits.param.toDate.get)
          .ifFilter(queryUserVisits.param.country.isDefined, v => locations(v.location).country.equalsIgnoreCase(queryUserVisits.param.country.get))
          .ifFilter(queryUserVisits.param.toDistance.isDefined, v => locations(v.location).distance < queryUserVisits.param.toDistance.get)

        val sortedRes: List[VisitsQueryResult] = filtered.toList.sortBy(v => v.visited_at).map(v => {
          VisitsQueryResult(v.mark, v.visited_at, locations(v.location).place)
        }).toList
        sender ! Some(sortedRes)
      } else if (users.get(queryUserVisits.id).isDefined) {
        sender ! Some(List[Visit]())
      } else {
        sender ! None
      }
  }
}

object VisitQueryActor {
  val name: String = "visit"

  case class VisitsQueryResult(mark: Int, visited_at: Long, place: String)

  def props: Props = Props[VisitQueryActor]
}
