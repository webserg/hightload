package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter.VisitsQuery
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult

class VisitQueryActor(val users: Map[Int, User],
                      val visits: Map[Int, Visit],
                      val location: Map[Int, Location],
                      val userVisits: Map[Int, List[Visit]],
                      val locationVisits: Map[Int, List[Visit]])
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case id: Int =>
      sender ! visits.get(id)

    case queryUserVisits: VisitsQuery =>

      val userVisitsRes = userVisits.get(queryUserVisits.id)
      if (userVisitsRes.isDefined) {
        val allRes: List[Visit] = userVisitsRes.get
        implicit class FilterHelper[A](l: List[A]) {
          def ifFilter(cond: Boolean, f: A => Boolean) = {
            if (cond) l.filter(f) else l
          }
        }

        val filtered = allRes
          .ifFilter(queryUserVisits.param.fromDate.isDefined, _.visited_at >= queryUserVisits.param.fromDate.get)
          .ifFilter(queryUserVisits.param.toDate.isDefined, _.visited_at < queryUserVisits.param.toDate.get)
          .ifFilter(queryUserVisits.param.country.isDefined, v => location(v.location).country.equalsIgnoreCase(queryUserVisits.param.country.get))
          .ifFilter(queryUserVisits.param.toDistance.isDefined, v => location(v.location).distance < queryUserVisits.param.toDistance.get)

        val sortedRes: List[VisitsQueryResult] = filtered.sortBy(v => v.visited_at).map(v => {
          VisitsQueryResult(v.mark, v.visited_at, location(v.location).place)
        })
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
