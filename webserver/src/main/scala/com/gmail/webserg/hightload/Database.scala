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

  def connect(collectionName:String): BSONCollection = {
    connection("travel").collection(collectionName)
  }

  def getUser(id: Int): Future[Option[User]] = {
    val query = BSONDocument("id" -> id)
    Database.connect("users").find(query).one[User]
  }

  def getLocation(id: Int): Future[Option[Location]] = {
    val query = BSONDocument("id" -> id)
    Database.connect("locations").find(query).one[Location]
  }

  def getVisit(id: Int): Future[Option[Visit]] = {
    val query = BSONDocument("id" -> id)
    Database.connect("visits").find(query).one[Visit]
  }

  def getVisitsByUser(id: Int): Future[List[Visit]] = {
    val query = BSONDocument("user" -> id)
    Database.connect("visits").find(query).cursor[Visit]().collect[List]()
  }

  def getVisitsByLocation(id: Int): Future[List[Visit]] = {
    val query = BSONDocument("location" -> id)
    Database.connect("visits").find(query).cursor[Visit]().collect[List]()
  }

  def updateUser(u: User) = {
    val selector = BSONDocument("id" -> u.id)
    var userBson = userBsonWriter.write(u)
    val modifier = BSONDocument(
      "$set" -> userBson
    )
    Database.connect("users").update(selector, modifier)
  }

  def addUser(u: User) = {
    Database.connect("users").insert(u)
  }

  def addLoc(u: Location) = {
    Database.connect("locations").insert(u)
  }

  def updateVisit(v: Visit) = {
    val selector = BSONDocument("id" -> v.id)
//    var bson = visitBsonWriter.write(v)
    val modifier = BSONDocument(
      "$set" -> v
    )
    Database.connect("visits").update(selector, modifier)
  }

  def updateLoc(v: Location) = {
    val selector = BSONDocument("id" -> v.id)
    val modifier = BSONDocument("$set" -> v )
    Database.connect("locations").update(selector, modifier)
  }

  def addVisit(v: Visit) = {
    Database.connect("visits").insert(v)
  }

}
