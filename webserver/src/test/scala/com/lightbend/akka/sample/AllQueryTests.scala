package com.lightbend.akka.sample

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.routing.RoundRobinPool
import com.gmail.webserg.hightload.DataLoaderActor.LoadData
import com.gmail.webserg.hightload.{DataLoaderActor, QueryRouter, WebRoute}
import org.scalatest.{Matchers, WordSpec}

class AllQueryTests extends WordSpec with Matchers with ScalatestRouteTest {
  val dataLoader: ActorRef = system.actorOf(DataLoaderActor.props, DataLoaderActor.name)
  dataLoader ! LoadData
  val queryRouter: ActorRef = system.actorOf(RoundRobinPool(5).props(QueryRouter.props), QueryRouter.name)

  val smallRoute: Route = WebRoute.createRoute(queryRouter)

  val input = scala.io.Source.fromFile("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\answers\\phase_1_get.answ")("UTF-8").getLines()

  "The service" should {
    var count = 0;
    for (answerLine <- input) {
      count = count + 1
      val answerArray = answerLine.split("\t").toList
      val query = answerArray(1)
      val statusResult: String = answerArray(2)
      val stringResult = if (answerArray.length > 3) Some(answerArray(3)) else None
      // tests:
      count + " | " + query in {
        Get(query) ~> Route.seal(smallRoute) ~> check {
          val resStatus = StatusCodes.getForKey(statusResult.toInt)
          if (resStatus.isDefined)
            status shouldEqual resStatus.get
          //          if (stringResult.isDefined)
          //            responseAs[String] shouldEqual stringResult.get
        }
      }
    }
  }


}
