package com.gmail.webserg.hightload

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.gmail.webserg.hightload.DataLoaderActor.LoadData

import scala.io.StdIn

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val dataArchivator: ActorRef = system.actorOf(ArchivatorActor.props, ArchivatorActor.name)
    dataArchivator ! ArchivatorActor.StartArchivator
    val dataLoader: ActorRef = system.actorOf(DataLoaderActor.props, DataLoaderActor.name)

    val queryRouter: ActorRef = system.actorOf(RoundRobinPool(15).props(QueryRouter.props), QueryRouter.name)


    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route = WebRoute.createRoute(queryRouter)

    val bindingFuture = Http().bindAndHandle(route, "localhost", 80)

    println(s"Server online at http://localhost:80/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
