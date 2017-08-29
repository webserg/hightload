package com.lightbend.akka.sample

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.routing.RoundRobinPool
import com.gmail.webserg.hightload.WebServer.WebServerProps
import com.gmail.webserg.hightload.{QueryRouter, WebRoute, WebServer}
import org.scalatest.{Matchers, WordSpec}

class AllQueryTests extends WordSpec with Matchers with ScalatestRouteTest {

  val webserverProps: WebServerProps = WebServerProps("C:\\git\\hightLoad\\", "C:\\git\\hightLoad\\data\\")
  val addr = WebServer.loadData(webserverProps, system)
  val queryRouter: ActorRef = system.actorOf(RoundRobinPool(25).props(Props(new QueryRouter(addr))), QueryRouter.name)

  val smallRoute: Route = WebRoute.createRoute(queryRouter)

  val input = scala.io.Source.fromFile("C:\\tmp\\data\\data\\FULL\\answers\\phase_1_get.answ")("UTF-8").getLines()

  "The service" should {
    var count = 0
    "warnup in" in {
      Get("/visits/860163") ~> Route.seal(smallRoute)~> check {
        val resStatus = StatusCodes.getForKey(200)
        if (resStatus.isDefined)
          status shouldEqual resStatus.get
        //          if (stringResult.isDefined)
        //            responseAs[String] shouldEqual stringResult.get
      }
    }


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
