package com.gmail.webserg.hightload

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.LocationQueryActor.LocationAvgQueryResult
import com.gmail.webserg.hightload.QueryRouter._
import com.gmail.webserg.hightload.UserDataReader.{User, UserLocation}
import com.gmail.webserg.hightload.VisitDataReader.Visit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class LocationQueryActor(
                          var users: Map[Int, UserLocation],
                          val generationDateTime: LocalDateTime)
  extends Actor with ActorLogging {

  override def preStart() = {
    log.debug("Starting LocationActor" + self.path)
  }


  implicit class FilterHelper[A](l: List[A]) {
    def ifFilter(cond: Boolean, f: A => Boolean) = {
      if (cond) l.filter(f) else l
    }
  }


  def remove(num: Int, list: List[Int]) = list diff List(num)

  override def receive: Receive = {

    case user: User =>
      users = users + (user.id -> UserLocation(user.id, user.birth_date, user.gender))


    case query: LocationAvgQuery =>
      val senderAddr = sender
      Database.getVisitsByLocation(query.id) onComplete {
        case Success(userVisitsRes) => senderAddr ! getLocAvgResult(query, userVisitsRes)
        case Failure(t) => log.debug(t.getMessage)
      }

    case q: LocationPostQueryParameter =>
      val nid = q.id.get
      val ncountry = q.country.get
      val ncity = q.city.get
      val nplace = q.place.get
      val ndist = q.distance.get
      val newLoc = Location(nid, nplace, ncountry, ncity, ndist)
      Database.addLoc(newLoc)
      context.actorSelection("/user/" + QueryRouter.name) ! newLoc

    case q: LocationPostQuery =>
      val oldLoc = q.oldLoc
      val nid = q.id
      val ncountry = q.param.country.getOrElse(oldLoc.country)
      val ncity = q.param.city.getOrElse(oldLoc.city)
      val nplace = q.param.place.getOrElse(oldLoc.place)
      val ndist = q.param.distance.getOrElse(oldLoc.distance)
      val newLoc = Location(nid, nplace, ncountry, ncity, ndist)
      Database.updateLoc(newLoc)
      context.actorSelection("/user/" + QueryRouter.name) ! newLoc
  }


  private def getLocAvgResult(query: LocationAvgQuery, userVisitsRes: List[Visit]) = {
    if (userVisitsRes.nonEmpty) {
      val filtered = userVisitsRes
        .ifFilter(query.param.fromDate.isDefined, _.visited_at > query.param.fromDate.get)
        .ifFilter(query.param.toDate.isDefined, _.visited_at < query.param.toDate.get)
        .ifFilter(query.param.fromAge.isDefined, v => getAge(generationDateTime, users(v.user).birth_date) >= query.param.fromAge.get)
        .ifFilter(query.param.toAge.isDefined, v => getAge(generationDateTime, users(v.user).birth_date) < query.param.toAge.get)
        .ifFilter(query.param.gender.isDefined, v => users(v.user).gender.equalsIgnoreCase(query.param.gender.get))


      val marks = filtered.map(_.mark)
      val avgTmp = if (marks.isEmpty) 0.0 else marks.sum.toDouble / marks.length
      val avg = BigDecimal(avgTmp).setScale(5, BigDecimal.RoundingMode.HALF_UP).toDouble
      Option(LocationAvgQueryResult(avg))
    } else {
      Database.getLocation(query.id) onComplete {
        case Success(locs) => Option(LocationAvgQueryResult(0.0))
        case Failure(t) =>  None
      }
    }
  }

  val SECONDS: Int = 24 * 60 * 60

  val today: LocalDate = LocalDate.now

  import java.time.temporal.ChronoUnit.YEARS

  def getAge(generationDateTime: LocalDateTime, bd: Long): Int =
    LocalDateTime.ofEpochSecond(bd, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt

  //  private def getAge(bd: Long) = {
  //    today.getYear - LocalDate.ofEpochDay(bd / SECONDS).getYear
  //  }
}


object LocationQueryActor {
  val name: String = "location"

  case class LocationAvgQueryResult(avg: Double)

  def props: Props = Props[LocationQueryActor]

  def validateNewPostLocationQuery(q: LocationPostQueryParameter): Boolean = {
    q.id.isDefined && q.city.isDefined && q.country.isDefined && q.distance.isDefined && q.place.isDefined
  }
}
