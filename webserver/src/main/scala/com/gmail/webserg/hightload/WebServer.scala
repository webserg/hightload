package com.gmail.webserg.hightload

import java.nio.file.NoSuchFileException
import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.gmail.webserg.hightload.QueryRouter._

object WebServer {

  case class WebServerProps(archiveDirName: String, dataDirName: String)

  def main(args: Array[String]) {
    if (args.length < 2) {
      println("enter dirname and archivedirname")
      return
    }
    val webServerProps = WebServerProps(args(0), args(1))
    implicit val system = ActorSystem("travel")
    implicit val materializer = ActorMaterializer()
    val actorAddresses = loadData(webServerProps, system)

    val queryRouter: ActorRef = system.actorOf(RoundRobinPool(100).props(Props(new QueryRouter(actorAddresses))), QueryRouter.name)


    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route = WebRoute.createRoute(queryRouter)

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 80)

    actorAddresses.userActor ! UserQuery(1)
    actorAddresses.locationActor ! LocationQuery(1)
    actorAddresses.visitActor ! VisitQuery(860163)
    actorAddresses.visitActor ! UserVisitsQuery(12995, UserVisitsQueryParameter(toDate = Some(954028800)))
    actorAddresses.userActor ! LocationAvgQuery(1, LocationQueryParameter(gender = Some("m")))

    //    println(s"Server online at http://localhost:80/\nPress RETURN to stop...")
    //    StdIn.readLine() // let it run until user presses return
    //    bindingFuture
    //      .flatMap(_.unbind()) // trigger unbinding from the port
    //      .onComplete(_ => system.terminate()) // and shutdown when done
  }


  case class ActorAddresses(userActor: ActorRef, visitActor: ActorRef, locationActor: ActorRef)

  def loadOptionFile(webServerProps: WebServerProps, system: ActorSystem): (LocalDateTime, Boolean) = {
    val (generationDateTime, isRateRun) = try {
      val optionLines = scala.io.Source.fromFile(new java.io.File(webServerProps.archiveDirName + "options.txt"))("UTF-8").getLines()
      val generationTime = optionLines.next().toInt
      val isRateRun = optionLines.next() == "1"
      (
        LocalDateTime.ofEpochSecond(
          generationTime, 0, ZoneOffset.UTC),
        isRateRun
      )
    } catch {
      case _: NoSuchFileException =>
        system.log.debug("Options file not found! Default to (datetime.now(), true)")
        (LocalDateTime.now(ZoneOffset.UTC), true)
    }

    (generationDateTime, isRateRun)
  }

  def loadData(webServerProps: WebServerProps, system: ActorSystem): ActorAddresses = {

    val (generationDateTime, isRateRun) = loadOptionFile(webServerProps, system)
    val dataDir = webServerProps.dataDirName
    val usersFileList = new java.io.File(dataDir).listFiles.filter(_.getName.startsWith("users"))
    val usersMap = (for {ls <- usersFileList} yield UserDataReader.readData(ls).users).flatten.map(i => i.id -> i).toMap
    val locationsFileList = new java.io.File(dataDir).listFiles.filter(_.getName.startsWith("locations"))
    val userActor = system.actorOf(Props(new UserQueryActor(usersMap, generationDateTime)), name = UserQueryActor.name)
    val locationMap = (for {ls <- locationsFileList} yield LocationDataReader.readData(ls).locations).flatten.map(i => i.id -> i).toMap
    val locationActor = system.actorOf(Props(new LocationQueryActor(locationMap)), name = LocationQueryActor.name)
    val visitActor = system.actorOf(RoundRobinPool(2).props(Props(
      new VisitQueryActor(usersMap.keys.toVector, locationMap))),
      name = VisitQueryActor.name)

    ActorAddresses(userActor = userActor, visitActor = visitActor, locationActor)
  }


}
