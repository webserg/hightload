package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter.{UserVisitsQuery, VisitPostQuery, VisitsPostQueryParameter}
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult


class VisitQueryActor(var users: Vector[Int],
                      var visits: Map[Int, Visit],
                      var locations: Map[Int, Location],
                      var userVisits: Map[Int, List[Int]],
                      )
  extends Actor with ActorLogging {

  override def preStart() = {
    log.debug("Starting QueryRouter" + self.path)
  }


  def validateNewPostVisitQuery(q: VisitsPostQueryParameter): Boolean = {
    q.id.isDefined && q.user.isDefined && q.location.isDefined && q.visited_at.isDefined && q.mark.isDefined &&
      users.isDefinedAt(q.user.get) && locations.get(q.location.get).isDefined

  }

  implicit class FilterHelper[A](l: List[A]) {
    def ifFilter(cond: Boolean, f: A => Boolean) = {
      if (cond) l.filter(f) else l
    }
  }

  def remove(num: Int, list: List[Int]) = list diff List(num)


  override def receive: Receive = {
    case id: Int =>
      sender ! visits.get(id)

    case user: User =>
      users = users :+ user.id

    case location: Location =>
      locations = locations + (location.id -> location)

    case q: VisitPostQuery =>
      val visit = visits.get(q.id)
      if (visit.isDefined) {
        sender() ! Some("{}")
        val oldVisit = visit.get
        val nlocation = q.param.location.getOrElse(oldVisit.location)
        val nuser = q.param.user.getOrElse(oldVisit.user)
        val nmark = q.param.mark.getOrElse(oldVisit.mark)
        val nvisit_at = q.param.visited_at.getOrElse(oldVisit.visited_at)
        val newVisit = Visit(oldVisit.id, nlocation, nuser, nvisit_at, nmark)
        visits = visits + (q.id -> newVisit)
        if (oldVisit.user != newVisit.user) {
          userVisits = userVisits + (oldVisit.user -> remove(newVisit.id, userVisits.getOrElse(oldVisit.user, List())))
          userVisits = userVisits + (newVisit.user -> (newVisit.id :: userVisits.getOrElse(nuser, List())))
        }
        context.actorSelection("/user/" + QueryRouter.name) ! newVisit

      } else sender() ! None

    case q: VisitsPostQueryParameter =>
      if (validateNewPostVisitQuery(q)) {
        sender() ! Some("{}")
        val nid = q.id.get
        val newVisit = Visit(nid, q.location.get, q.user.get, q.visited_at.get, q.mark.get)
        visits = visits + (nid -> newVisit)
        userVisits = userVisits + (newVisit.user -> (newVisit.id :: userVisits.getOrElse(newVisit.user, List())))
        context.actorSelection("/user/" + QueryRouter.name) ! newVisit

      } else sender() ! None


    case queryUserVisits: UserVisitsQuery =>

      val userVisitsRes = userVisits.get(queryUserVisits.id)
      if (userVisitsRes.isDefined) {

        val filtered = userVisitsRes.get
          .ifFilter(queryUserVisits.param.fromDate.isDefined, v => visits(v).visited_at > queryUserVisits.param.fromDate.get)
          .ifFilter(queryUserVisits.param.toDate.isDefined, v => visits(v).visited_at < queryUserVisits.param.toDate.get)
          .ifFilter(queryUserVisits.param.country.isDefined, v => locations(visits(v).location).country.equalsIgnoreCase(queryUserVisits.param.country.get))
          .ifFilter(queryUserVisits.param.toDistance.isDefined, v => locations(visits(v).location).distance < queryUserVisits.param.toDistance.get)

        val sortedRes: List[VisitsQueryResult] = filtered.sortBy(v => visits(v).visited_at).map(v => {
          val vv = visits(v)
          VisitsQueryResult(vv.mark, vv.visited_at, locations(vv.location).place)
        })
        sender ! Some(sortedRes)
      } else if (users.isDefinedAt(queryUserVisits.id)) {
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
