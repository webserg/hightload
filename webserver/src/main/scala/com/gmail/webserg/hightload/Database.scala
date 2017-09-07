package com.gmail.webserg.hightload

import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.MyJsonProtocol._
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.VisitDataReader.Visit
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Database {

  val driver = new MongoDriver
  val connection = driver.connection(List("127.0.0.1"))

  val db = connection("travel")

  val users = connect("users")
  val locations = connect("locations")
  val visits = connect("visits")

  def connect(collectionName:String): BSONCollection = {
    db.collection(collectionName)
  }

  def getUser(id: Int): Future[Option[User]] = {
    val query = BSONDocument("id" -> id)
    Database.users.find(query).one[User]
  }

  def getLocation(id: Int)= {
    val query = BSONDocument("id" -> id)
    Database.locations.find(query).one[Location]
  }

  def getVisit(id: Int)= {
    val query = BSONDocument("id" -> id)
    Database.visits.find(query).one[Visit]
  }

  def getVisitsByUser(id: Int): Future[List[Visit]] = {
    val query = BSONDocument("user" -> id)
    Database.visits.find(query).cursor[Visit]().collect[List]()
  }

  def getVisitsByLocation(id: Int): Future[List[Visit]] = {
    val query = BSONDocument("location" -> id)
    Database.visits.find(query).cursor[Visit]().collect[List]()
  }

  def updateUser(u: User) = {
    val selector = BSONDocument("id" -> u.id)
    var userBson = userBsonWriter.write(u)
    val modifier = BSONDocument(
      "$set" -> userBson
    )
    Database.users.update(selector, modifier)
  }

  def addUser(u: User) = {
    Database.users.insert(u)
  }

  def addLoc(u: Location) = {
    Database.locations.insert(u)
  }

  def updateVisit(v: Visit) = {
    val selector = BSONDocument("id" -> v.id)
//    var bson = visitBsonWriter.write(v)
    val modifier = BSONDocument(
      "$set" -> v
    )
    Database.visits.update(selector, modifier)
  }

  def updateLoc(v: Location) = {
    val selector = BSONDocument("id" -> v.id)
    val modifier = BSONDocument("$set" -> v )
    Database.locations.update(selector, modifier)
  }

  def addVisit(v: Visit) = {
    Database.visits.insert(v)
  }

}
