package com.gmail.webserg.hightload
import spray.json._

object UserProtocol extends DefaultJsonProtocol {
  import DataReader._
  implicit def namedListFormat[A :JsonFormat]: RootJsonFormat[NamedList[A]] = jsonFormat1(NamedList.apply[A])
  implicit def userListFormat = jsonFormat6(User)
}

object DataReader {

  case class User(id: Int, first_name: String, last_name: String, birth_date: Long, gender: String, email: String)
  case class NamedList[A](users: List[A])

  def readData(in: String): NamedList[User] = {
    import UserProtocol._
    val input = scala.io.Source.fromFile(in)("UTF-8").mkString.parseJson
    input.convertTo[NamedList[User]]
  }

}
