package com.gmail.webserg.hightload

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, onSuccess, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.LocationQueryActor.LocationAvgQueryResult
import com.gmail.webserg.hightload.MyJsonProtocol._
import com.gmail.webserg.hightload.QueryRouter._
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult

import scala.concurrent.Future
import scala.concurrent.duration._

object WebRoute {
  implicit val timeout: Timeout = 3000 millisecond


  def createRoute(queryRouter: ActorRef): Route = {
    post {
      pathPrefix("users" / IntNumber) { id =>
        entity(as[String]) {
          q =>
            if (q.contains("null")) {
              complete(StatusCodes.BadRequest, "{}")
            } else {

              entity(as[UserPostQueryParameter]) {
                queryParam =>

                  val maybeItem: Future[Option[String]] = (queryRouter ? UserPostQuery(id, queryParam)).mapTo[Option[String]]

                  onSuccess(maybeItem) {
                    case None => complete(StatusCodes.NotFound)
                    case Some(item) => {
                      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "{}"))
                    }
                  }
              }
            }
        }
      }
    } ~
      post {
        pathPrefix("users" / "new") {
          entity(as[UserPostQueryParameter]) {
            queryParam =>
              val maybeItem: Future[Option[User]] = (queryRouter ? queryParam).mapTo[Option[User]]

              onSuccess(maybeItem) {
                case None => complete(StatusCodes.NotFound)
                case Some(item) => {
                  queryRouter ! User
                  complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "{}"))
                }
              }
          }
        }
      } ~
      post {
        pathPrefix("visits" / IntNumber) { id =>
          entity(as[String]) {
            q =>
              if (q.contains("null")) {
                complete(StatusCodes.BadRequest, "{}")
              } else {

                entity(as[VisitsPostQueryParameter]) {
                  queryParam =>
                    val maybeItem: Future[Option[String]] = (queryRouter ? VisitPostQuery(id, queryParam)).mapTo[Option[String]]

                    onSuccess(maybeItem) {
                      case None => complete(StatusCodes.NotFound)
                      case Some(item) => {
                        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "{}"))
                      }
                    }
                }
              }
          }
        }
      } ~
      post {
        pathPrefix("visits" / "new") {
          entity(as[String]) {
            q =>
              if (q.contains("null")) {
                complete(StatusCodes.BadRequest, "{}")
              } else {

                entity(as[VisitsPostQueryParameter]) {
                  queryParam =>
                    val maybeItem: Future[Option[Visit]] = (queryRouter ? queryParam).mapTo[Option[Visit]]

                    onSuccess(maybeItem) {
                      case None => complete(StatusCodes.BadRequest)
                      case Some(item) => {
                        queryRouter ! Visit
                        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "{}"))
                      }
                    }
                }
              }
          }
        }
      } ~
      post {
        pathPrefix("locations" / IntNumber) { id =>
          entity(as[String]) {
            q =>
              if (q.contains("null")) {
                complete(StatusCodes.BadRequest, "{}")
              } else {

                entity(as[LocationPostQueryParameter]) {
                  queryParam =>
                    val maybeItem: Future[Option[Location]] = (queryRouter ? LocationPostQuery(id, queryParam)).mapTo[Option[Location]]

                    onSuccess(maybeItem) {
                      case None => complete(StatusCodes.BadRequest)
                      case Some(item) => {
                        queryRouter ! Location
                        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "{}"))
                      }
                    }
                }
              }
          }
        }
      } ~
      post {
        pathPrefix("locations" / "new") {

          entity(as[LocationPostQueryParameter]) {
            queryParam =>
              val maybeItem: Future[Option[Location]] = (queryRouter ? queryParam).mapTo[Option[Location]]

              onSuccess(maybeItem) {
                case None => complete(StatusCodes.BadRequest)
                case Some(item) => {
                  queryRouter ! Location
                  complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "{}"))
                }
              }
          }
        }
      } ~
      get {
        pathPrefix("users" / IntNumber / "visits") { id =>
          parameters('fromDate.as[Long].?, 'toDate.as[Long].?, 'country.as[String].?, 'toDistance.as[Int].?).as(UserVisitsQueryParameter) {
            visitQueryParam =>
              // there might be no item for a given id
              val maybeItem: Future[Option[List[VisitsQueryResult]]] = (queryRouter ? UserVisitsQuery(id, visitQueryParam)).mapTo[Option[List[VisitsQueryResult]]]

              onSuccess(maybeItem) {
                case None => complete(StatusCodes.NotFound)
                case Some(item) => {
                  complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, LocationDataWriter.writeData(item).toString()))
                }
              }
          }
        }
      } ~
      pathPrefix("users" / IntNumber / "visits") { id =>
        parameters('fromDate.as[String].?, 'toDate.as[String].?, 'country.as[String].?, 'toDistance.as[String].?) {
          (a, b, c, d) => complete(StatusCodes.BadRequest)
        }
      } ~
      pathPrefix("users" / IntNumber) { id =>
        val maybeItem: Future[Option[User]] = (queryRouter ? UserQuery(id)).mapTo[Option[User]]

        onSuccess(maybeItem) {
          case None => complete(StatusCodes.NotFound)
          case Some(item) => {
            import MyJsonProtocol._
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, userFormat.write(item).toString()))
          }
        }
      } ~
      pathPrefix("visits" / IntNumber) { id =>
        val maybeItem: Future[Option[Visit]] = (queryRouter ? VisitQuery(id)).mapTo[Option[Visit]]

        onSuccess(maybeItem) {
          case None => complete(StatusCodes.NotFound)
          case Some(item) => {
            import MyJsonProtocol._
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, visitFormat.write(item).toString()))
          }
        }
      } ~
      pathPrefix("locations" / IntNumber / "avg") { id =>
        parameters('fromDate.as[Long].?, 'toDate.as[Long].?, 'fromAge.as[Int].?, 'toAge.as[Int].?, 'gender.as[String].?).as(LocationQueryParameter) {
          param =>
            val maybeItem: Future[Option[LocationAvgQueryResult]] = (queryRouter ? LocationAvgQuery(id, param)).mapTo[Option[LocationAvgQueryResult]]

            onSuccess(maybeItem) {
              case None => complete(StatusCodes.NotFound)
              case Some(item) => {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, MyJsonProtocol.locationQueryResultFormat.write(item).toString()))
              }
            }
        }
      } ~
      pathPrefix("locations" / IntNumber / "avg") { id =>
        parameters('fromDate.as[String].?, 'toDate.as[String].?, 'fromAge.as[String].?, 'toAge.as[String].?, 'gender.as[String].?) {
          (a, b, c, d, e) => complete(StatusCodes.BadRequest)
        }
      } ~
      pathPrefix("locations" / IntNumber) { id =>
        val maybeItem: Future[Option[Location]] = (queryRouter ? LocationQuery(id)).mapTo[Option[Location]]

        onSuccess(maybeItem) {
          case None => complete(StatusCodes.NotFound)
          case Some(item) => {
            import MyJsonProtocol._
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, locationFormat.write(item).toString()))
          }
        }
      }

  }


}
