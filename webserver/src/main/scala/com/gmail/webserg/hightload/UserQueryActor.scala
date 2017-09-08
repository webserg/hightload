package com.gmail.webserg.hightload

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.QueryRouter.{LocationAvgQuery, LocationQuery, UserPostQuery, UserPostQueryParameter}
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
sealed class UserQueryActor(var users: Map[Int, User],
                            val generationDateTime: LocalDateTime) extends Actor with ActorLogging {

  implicit val timeout: Timeout = 3000 millisecond


  implicit class FilterHelper[A](l: List[A]) {
    def ifFilter(cond: Boolean, f: A => Boolean) = {
      if (cond) l.filter(f) else l
    }
  }

  override def receive: Receive = {
    case id: Int =>
      sender ! users.get(id)

    case query: LocationAvgQuery =>
      val senderAddr = sender
      Database.getVisitsByLocation(query.id) onComplete {
        case Success(userVisitsRes) =>
          if (userVisitsRes.nonEmpty) {
            senderAddr ! getLocAvgResult(query, userVisitsRes)
          } else {
            val maybeItem: Future[Option[Location]] = (context.actorSelection("/user/" + QueryRouter.name) ? LocationQuery(query.id)).mapTo[Option[Location]]
            maybeItem.onComplete {
              case Success(Some(_)) => senderAddr ! Option(LocationQueryActor.LocationAvgQueryResult(0.0))
              case _ => complete(StatusCodes.NotFound)
            }
          }
        case Failure(t) => log.debug(t.getMessage)
      }

    case q: UserPostQuery =>
      val user = users.get(q.id)
      if (user.isDefined) {
        sender() ! Some("{}")
        val oldUser = user.get
        val nfirst_name = q.param.first_name.getOrElse(oldUser.first_name)
        val nlast_name = q.param.last_name.getOrElse(oldUser.last_name)
        val nbirth_date = q.param.birth_date.getOrElse(oldUser.birth_date)
        val ngender = q.param.gender.getOrElse(oldUser.gender)
        val nemail = q.param.email.getOrElse(oldUser.email)
        val newUser = User(oldUser.id, nfirst_name, nlast_name, nbirth_date, ngender, nemail)
        users = users + (q.id -> newUser)
        context.actorSelection("/user/" + QueryRouter.name) ! newUser

      } else sender() ! None

    case q: UserPostQueryParameter =>
      if (UserQueryActor.validateNewPostUserQuery(q)) {
        val user = users.get(q.id.get)
        if (user.isEmpty) {
          sender() ! Some("{}")
          val nid = q.id.get
          val nfirst_name = q.first_name.get
          val nlast_name = q.last_name.get
          val nbirth_date = q.birth_date.get
          val ngender = q.gender.get
          val nemail = q.email.get
          val newUser = User(nid, nfirst_name, nlast_name, nbirth_date, ngender, nemail)
          users = users + (nid -> newUser)
          context.actorSelection("/user/" + QueryRouter.name) ! newUser

        } else sender() ! None

      }

  }


  private def getLocAvgResult(query: LocationAvgQuery, userVisitsRes: List[Visit]) = {
    val filtered = userVisitsRes
      .ifFilter(query.param.fromDate.isDefined, _.visited_at > query.param.fromDate.get)
      .ifFilter(query.param.toDate.isDefined, _.visited_at < query.param.toDate.get)
      .ifFilter(query.param.fromAge.isDefined, v => UserQueryActor.getAge(generationDateTime, users(v.user).birth_date) >= query.param.fromAge.get)
      .ifFilter(query.param.toAge.isDefined, v => UserQueryActor.getAge(generationDateTime, users(v.user).birth_date) < query.param.toAge.get)
      .ifFilter(query.param.gender.isDefined, v => users(v.user).gender.equalsIgnoreCase(query.param.gender.get))


    val marks = filtered.map(_.mark)
    val avgTmp = if (marks.isEmpty) 0.0 else marks.sum.toDouble / marks.length
    val avg = BigDecimal(avgTmp).setScale(5, BigDecimal.RoundingMode.HALF_UP).toDouble
    Option(LocationQueryActor.LocationAvgQueryResult(avg))
  }

}

object UserQueryActor {
  val name: String = "user"

  def props: Props = Props[UserQueryActor]

  def validateNewPostUserQuery(q: UserPostQueryParameter) = {
    q.id.isDefined && q.first_name.isDefined && q.last_name.isDefined && q.birth_date.isDefined && q.email.isDefined && q.gender.isDefined
  }

  val SECONDS: Int = 24 * 60 * 60

  val today: LocalDate = LocalDate.now

  import java.time.temporal.ChronoUnit.YEARS

  def getAge(generationDateTime: LocalDateTime, bd: Long): Int =
    LocalDateTime.ofEpochSecond(bd, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt
}

