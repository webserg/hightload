package com.lightbend.akka.sample

import akka.util.Timeout
import com.gmail.webserg.hightload.Database
import com.gmail.webserg.hightload.UserDataReader.User
import reactivemongo.bson.{BSONDocumentReader, Macros}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object DatabaseTest extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 10 millisecond

  implicit def personReader: BSONDocumentReader[User] = Macros.reader[User]

  Database.getUser(1) onComplete {
    case Success(bson) => {
      println(bson.get)
    }
    case Failure(t) => println("NOne" + t.getMessage)
  }

  Database.getVisitsByUser(12995) onComplete {
    case Success(bson) => {
      println(bson)
    }
    case Failure(t) => println("NOne" + t.getMessage)
  }

}

