package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.QueryRouter.VisitsQuery

class VisitQueryActor(val visits: Map[Int, Visit],
                      val location: Map[Int, Location],
                      val userVisits: Map[Int, List[Visit]],
                      val locationVisits: Map[Int, List[Visit]])
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case "lol" =>     sender ! "sdfsdfdf"
    case queryUserVisits: VisitsQuery =>

      val userVisitsRes = userVisits.get(queryUserVisits.id)
      if (userVisitsRes.isDefined) {
        val res: List[VisitsQueryResult] = userVisitsRes.get.sortBy(v => v.visited_at).map(v => {
          VisitsQueryResult(v.mark, v.visited_at, location(v.location).place)
        })
        sender ! Option(res)
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
