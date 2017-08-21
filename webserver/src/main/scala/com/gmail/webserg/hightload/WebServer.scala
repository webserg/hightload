package com.gmail.webserg.hightload

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.gmail.webserg.hightload.UserDataReader.User
import com.lightbend.akka.sample.AkkaQuickstart.system
import com.lightbend.akka.sample.Printer

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.gmail.webserg.hightload.DataLoaderActor.{LoadUsers, LoadVisitors}
import com.gmail.webserg.hightload.QueryRouter.{UserQuery, VisitsQuery}
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult

import scala.concurrent.duration._
import scala.io.StdIn
import scala.concurrent.Future
import scala.io.StdIn

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val dataArchivator: ActorRef = system.actorOf(ArchivatorActor.props, ArchivatorActor.name)
    dataArchivator ! ArchivatorActor.StartArchivator
    //    val dataLoader : ActorRef = system.actorOf(DataLoaderActor.props, "dataLoader")


    val dataLoader: ActorRef = system.actorOf(DataLoaderActor.props, DataLoaderActor.name)
    dataLoader ! LoadUsers
    dataLoader ! LoadVisitors
    val queryRouter: ActorRef = system.actorOf(RoundRobinPool(5).props(QueryRouter.props), QueryRouter.name)


    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher


    val route = {
        get {
          pathPrefix("users" / IntNumber / "visits") { id =>
            // there might be no item for a given id
            implicit val timeout: Timeout = 60 millisecond
            val maybeItem: Future[Option[List[VisitsQueryResult]]] = (queryRouter ? VisitsQuery(id)).mapTo[Option[List[VisitsQueryResult]]]

            onSuccess(maybeItem) {
              case None => complete(StatusCodes.NotFound)
              case Some(item) => {
                                  complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, LocationDataWriter.writeData(item).toString()))
//                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "bala"))
              }
            }
          } ~
          pathPrefix("users" / IntNumber) { id =>
            // there might be no item for a given id
            implicit val timeout: Timeout = 60 millisecond
            val maybeItem: Future[Option[User]] = (queryRouter ? UserQuery(id)).mapTo[Option[User]]

            onSuccess(maybeItem) {
              case None => complete(StatusCodes.NotFound)
              case Some(item) => {
                print("gdfgdfgdfgd" + item.email)
                import MyJsonProtocol._

                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, userFormat.write(item).toString()))
              }
            }
          }

      }
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
