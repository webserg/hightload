package com.lightbend.akka.sample

import akka.util.Timeout
import com.gmail.webserg.hightload.Database

import scala.util.{Failure, Success}
import scala.concurrent.duration._
object DatabaseTest extends App {
  implicit val timeout: Timeout = 10 millisecond
  // query the database
  val ticker = Database.findTicker(5000)

//  // use a simple for comprehension, to make
//  // working with futures easier.
//  Database.findTicker("5000") onComplete{
//      case Success(bson) => println(bson.getOrElse("nothing"))
//      case Failure(t) => println("NOne" + t.getMessage)
//    }

}

