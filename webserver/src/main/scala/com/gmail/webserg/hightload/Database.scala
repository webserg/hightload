package com.gmail.webserg.hightload

import com.gmail.webserg.hightload.LocationDataReader.Location
import com.gmail.webserg.hightload.MyJsonProtocol._
import com.gmail.webserg.hightload.VisitDataReader.Visit
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONNumberLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Database {

  implicit object VisitReader extends BSONDocumentReader[Visit] {
    def read(bson: BSONDocument): Visit = {
      val opt: Option[Visit] = for {
        id <- bson.getAs[Int]("id")
        location <- bson.getAs[Int]("location")
        user <- bson.getAs[Int]("user")
        age <- bson.getAs[BSONNumberLike]("visited_at").map(_.toLong)
        mark <- bson.getAs[Int]("mark")
      } yield new Visit(id, location, user, age, mark)

      opt.get // the person is required (or let throw an exception)
    }
  }

  val conOpts = MongoConnectionOptions(keepAlive = true, nbChannelsPerNode = 25)
  val driver = new MongoDriver
  val connection: MongoConnection = driver.connection(List("127.0.0.1"))

  def connect(collectionName: String): BSONCollection = {
    connection("travel").collection(collectionName)
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
    val modifier = BSONDocument("$set" -> v)
    Database.connect("locations").update(selector, modifier)
  }

  def addVisit(v: Visit) = {
    Database.connect("visits").insert(v)
  }

}
