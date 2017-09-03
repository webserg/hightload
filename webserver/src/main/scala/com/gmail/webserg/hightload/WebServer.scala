package com.gmail.webserg.hightload

import java.nio.file.{NoSuchFileException, Paths}
import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.gmail.webserg.hightload.QueryRouter._
import com.gmail.webserg.hightload.UserDataReader.UserLocation

object WebServer {

  case class WebServerProps(archiveDirName: String, dataDirName: String)

  def main(args: Array[String]) {
    if (args.length < 2) {
      println("enter dirname and archivedirname")
      return
    }
    val webServerProps = WebServerProps(args(0), args(1))
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    unZip(webServerProps)
    val actorAddresses = loadData(webServerProps, system)

    val queryRouter: ActorRef = system.actorOf(RoundRobinPool(100).props(Props(new QueryRouter(actorAddresses))), QueryRouter.name)


    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route = WebRoute.createRoute(queryRouter)

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 80)

    actorAddresses.userActor ! 1
    actorAddresses.locationActor ! 1
    actorAddresses.visitActor ! VisitQuery(860163)
    actorAddresses.visitActor ! UserVisitsQuery(12995, UserVisitsQueryParameter(toDate = Some(954028800)))

    //    println(s"Server online at http://localhost:80/\nPress RETURN to stop...")
    //    StdIn.readLine() // let it run until user presses return
    //    bindingFuture
    //      .flatMap(_.unbind()) // trigger unbinding from the port
    //      .onComplete(_ => system.terminate()) // and shutdown when done
  }


  def unZip(webServerProps: WebServerProps): Unit = {
    val out = webServerProps.dataDirName
    val in = webServerProps.archiveDirName + "data.zip"
    Archivator.unzip(in, Paths.get(out))
  }

  case class ActorAddresses(userActor: ActorRef, visitActor: ActorRef, locationActor: ActorRef, locationGetActor: ActorRef)

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
    val usersList = (for {ls <- usersFileList} yield UserDataReader.readData(ls).users).flatten
    val usersMap = usersList.map(i => i.id -> i).toMap
    val userActor = system.actorOf(Props(new UserQueryActor(usersMap)), name = UserQueryActor.name)
    system.log.debug("users loaded size = " + usersList.length)
    val visitsFileList = new java.io.File(dataDir).listFiles.filter(_.getName.startsWith("visits"))
    val visitsList = (for {ls <- visitsFileList} yield VisitDataReader.readData(ls).visits).flatten
    val visitsMap = visitsList.map(i => i.id -> i).toMap
    system.log.debug("visits loaded size = " + visitsList.length)

    val locationsFileList = new java.io.File(dataDir).listFiles.filter(_.getName.startsWith("locations"))
    val locationList = (for {ls <- locationsFileList} yield LocationDataReader.readData(ls).locations).flatten
    val locationMap = locationList.map(i => i.id -> i).toMap
    system.log.debug("locs loaded size = " + locationList.length)

    val visitActor = system.actorOf(RoundRobinPool(2).props(Props(
      new VisitQueryActor(usersList.map(v => v.id).toVector, visitsMap, locationMap,
        visitsList.groupBy(v => v.user).map(k => (k._1, k._2.map(i => i.id).toList)),
        visitsList.groupBy(v => v.location).map(k => (k._1, k._2.map(i => i.id).toList))
      ))),
      name = VisitQueryActor.name)

    val locationActor = system.actorOf(Props(
      new LocationQueryActor(usersList.map(v => v.id -> UserLocation(v.id, v.birth_date, v.gender)).toMap, locationList.map(v => v.id -> v).toMap,
        visitsList.groupBy(v => v.location).map(k => (k._1, k._2.map(i => i.id -> i).toMap))
        , generationDateTime
      )),
      name = LocationQueryActor.name)

    val locationGetActor = system.actorOf(Props(
      new LocationGetActor(locationList.map(v => v.id -> v).toMap)),
      name = LocationGetActor.name)

    ActorAddresses(userActor = userActor, visitActor = visitActor, locationActor, locationGetActor)
  }


}
