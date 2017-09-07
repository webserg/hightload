package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter.{UserVisitsQuery, VisitPostQuery, VisitsPostQueryParameter}
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult

import scala.util.{Failure, Success}

object VisitQueryActor {
  val name: String = "visit"

  case class VisitsQueryResult(mark: Int, visited_at: Long, place: String)

  def props: Props = Props[VisitQueryActor]

}

class VisitQueryActor(var users: Vector[Int],
                      var locations: Map[Int, Location],
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

  import scala.concurrent.ExecutionContext.Implicits.global

  def remove(num: Int, list: List[Int]) = list diff List(num)


  override def receive: Receive = {

    case user: User =>
      users = users :+ user.id

    case location: Location =>
      locations = locations + (location.id -> location)

    case q: VisitPostQuery =>
        val oldVisit = q.oldVisit
        val nlocation = q.param.location.getOrElse(oldVisit.location)
        val nuser = q.param.user.getOrElse(oldVisit.user)
        val nmark = q.param.mark.getOrElse(oldVisit.mark)
        val nvisit_at = q.param.visited_at.getOrElse(oldVisit.visited_at)
        val newVisit = Visit(oldVisit.id, nlocation, nuser, nvisit_at, nmark)
        Database.updateVisit(newVisit)

    case q: VisitsPostQueryParameter =>
      if (validateNewPostVisitQuery(q)) {
        sender() ! Some("{}")
        val nid = q.id.get
        val newVisit = Visit(nid, q.location.get, q.user.get, q.visited_at.get, q.mark.get)
        Database.updateVisit(newVisit)
      } else sender() ! None


    case queryUserVisits: UserVisitsQuery =>
      val senderAddr = sender
      if (users.isDefinedAt(queryUserVisits.id)) {
        Database.getVisitsByUser(queryUserVisits.id) onComplete {
          case Success(userVisitsRes) => senderAddr ! getUserVisitsResult(queryUserVisits, userVisitsRes)
          case Failure(t) => log.debug(t.getMessage)
        }
      } else {
        senderAddr ! None
      }
  }

  private def getUserVisitsResult(queryUserVisits: UserVisitsQuery, userVisitsRes: List[Visit]) = {
    if (userVisitsRes.nonEmpty) {
      val filtered = userVisitsRes
        .ifFilter(queryUserVisits.param.fromDate.isDefined, v => v.visited_at > queryUserVisits.param.fromDate.get)
        .ifFilter(queryUserVisits.param.toDate.isDefined, v => v.visited_at < queryUserVisits.param.toDate.get)
        .ifFilter(queryUserVisits.param.country.isDefined, v => locations(v.location).country.equalsIgnoreCase(queryUserVisits.param.country.get))
        .ifFilter(queryUserVisits.param.toDistance.isDefined, v => locations(v.location).distance < queryUserVisits.param.toDistance.get)

      val sortedRes: List[VisitsQueryResult] = filtered.sortBy(v => v.visited_at).map(v => {
        VisitsQueryResult(v.mark, v.visited_at, locations(v.location).place)
      })
      Some(sortedRes)
    } else {
      Some(List[Visit]())

    }
  }
}



