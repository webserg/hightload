package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.DataLoaderActor.{LoadData}

class DataLoaderActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case LoadData => {
      val usersList: List[User] = UserDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\users_1.json").users
      val usersMap = usersList.map(i => i.id -> i).toMap
      val userActor = context.actorOf(Props(new UserQueryActor(usersMap)), name = UserQueryActor.name)
      val visitsList: List[Visit] = VisitDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\visits_1.json").visits
      val locationList: List[Location] = LocationDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\locations_1.json").locations
      val visitsMap = visitsList.map(i => i.id -> i).toMap
      val locationMap = locationList.map(i => i.id -> i).toMap
      val visitActor = context.actorOf(Props(
        new VisitQueryActor(usersList.map(v => v.id -> v).toMap, visitsMap, locationMap, visitsList.groupBy(v => v.user), visitsList.groupBy(v => v.location))),
        name = VisitQueryActor.name)

      val locationActor = context.actorOf(Props(
        new LocationQueryActor(usersList.map(v => v.id -> v).toMap, locationList.map(v => v.id -> v).toMap, visitsList.groupBy(v => v.location))),
        name = LocationQueryActor.name)
    }
  }

}

object DataLoaderActor {
  val name = "dataLoader"

  case object LoadData

  def props: Props = Props[DataLoaderActor]


}
