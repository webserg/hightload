package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.DataLoaderActor.LoadData

class DataLoaderActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case LoadData => {
      val usersFileList = new java.io.File(DataLoaderActor.dataDir).listFiles.filter(_.getName.startsWith("users"))
      val usersList = (for {ls <- usersFileList} yield UserDataReader.readData(ls).users).flatten
      val usersMap = usersList.map(i => i.id -> i).toMap
      val userActor = context.actorOf(Props(new UserQueryActor(usersMap)), name = UserQueryActor.name)
      log.debug("users loaded size = " + usersList.length)
      val visitsFileList = new java.io.File(DataLoaderActor.dataDir).listFiles.filter(_.getName.startsWith("visits"))
      val visitsList = (for {ls <- visitsFileList} yield VisitDataReader.readData(ls).visits).flatten
      val visitsMap = visitsList.map(i => i.id -> i).toMap
      log.debug("visits loaded size = " + visitsList.length)

      val locationsFileList = new java.io.File(DataLoaderActor.dataDir).listFiles.filter(_.getName.startsWith("locations"))
      val locationList = (for {ls <- locationsFileList} yield LocationDataReader.readData(ls).locations).flatten
      val locationMap = locationList.map(i => i.id -> i).toMap
      log.debug("locs loaded size = " + locationList.length)

      val visitActor = context.actorOf(Props(
        new VisitQueryActor(usersList.map(v => v.id -> v).toMap, visitsMap, locationMap,
          visitsList.groupBy(v => v.user).map(k => (k._1, k._2.map(i => i.id -> i).toMap)),
          visitsList.groupBy(v => v.location).map(k => (k._1, k._2.map(i => i.id -> i).toMap))
        )),
        name = VisitQueryActor.name)

      val locationActor = context.actorOf(Props(
        new LocationQueryActor(usersList.map(v => v.id -> v).toMap, locationList.map(v => v.id -> v).toMap,
          visitsList.groupBy(v => v.location).map(k => (k._1, k._2.map(i => i.id -> i).toMap))

        )),
        name = LocationQueryActor.name)
    }
  }

}

object DataLoaderActor {
  val name = "dataLoader"

  case object LoadData

  //  val dataDir = ArchivatorActor.dirName + "/data/data/"
  val dataDir: String = ArchivatorActor.dirName

  def props: Props = Props[DataLoaderActor]


}
