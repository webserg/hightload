package com.gmail.webserg.hightload

import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import com.gmail.webserg.hightload.VisitQueryActor.VisitsQueryResult
import spray.json._

case class LocationsList[A](locations: List[A])
case class UserList[A](users: List[A])

case class VisitsList[A](visits: List[A])




object MyJsonProtocol extends DefaultJsonProtocol {


  implicit def locationListFormat[A: JsonFormat]: RootJsonFormat[LocationsList[A]] = jsonFormat1(LocationsList.apply[A])

  implicit def userListFormat[A: JsonFormat]: RootJsonFormat[UserList[A]] = jsonFormat1(UserList.apply[A])

  implicit def visitListFormat[A: JsonFormat]: RootJsonFormat[VisitsList[A]] = jsonFormat1(VisitsList.apply[A])

  implicit val locationFormat: RootJsonFormat[Location] = jsonFormat5(Location)

  implicit val userFormat: RootJsonFormat[User] = jsonFormat6(User)

  implicit val visitFormat: RootJsonFormat[Visit] = jsonFormat5(Visit)

  implicit val visitQueryResultFormat: RootJsonFormat[VisitsQueryResult] = jsonFormat3(VisitsQueryResult)

}

object UserDataReader {

  final case class User(id: Int, first_name: String, last_name: String, birth_date: Long, gender: String, email: String)

  def readData(in: String): UserList[User] = {
    import MyJsonProtocol._
    val input = scala.io.Source.fromFile(in)("UTF-8").mkString.parseJson
    input.convertTo[UserList[User]]
  }

}

object LocationDataReader {

  final case class Location(id: Int, place: String, country: String, city: String, distance: Int)

  def readData(in: String): LocationsList[Location] = {
    import MyJsonProtocol._
    val input = scala.io.Source.fromFile(in)("UTF-8").mkString.parseJson
    input.convertTo[LocationsList[Location]]
  }

}

object LocationDataWriter {



  def writeData(list: List[VisitsQueryResult]): JsValue = {
    import MyJsonProtocol._
    VisitsList(list).toJson
  }

}

object VisitDataReader {

  final case class Visit(id: Int, location: Int, user: Int, visited_at: Long, mark: Int)

  def readData(in: String): VisitsList[Visit] = {
    import MyJsonProtocol._
    val input = scala.io.Source.fromFile(in)("UTF-8").mkString.parseJson
    input.convertTo[VisitsList[Visit]]
  }


}


