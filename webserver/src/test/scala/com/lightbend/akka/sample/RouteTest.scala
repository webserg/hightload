package com.lightbend.akka.sample

import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import akka.actor.ActorRef
import akka.routing.RoundRobinPool
import com.gmail.webserg.hightload.DataLoaderActor.{LoadData}
import com.gmail.webserg.hightload.{DataLoaderActor, QueryRouter, WebRoute}

class RouteTest extends WordSpec with Matchers with ScalatestRouteTest {

  val dataLoader: ActorRef = system.actorOf(DataLoaderActor.props, DataLoaderActor.name)
  dataLoader ! LoadData
  val queryRouter: ActorRef = system.actorOf(RoundRobinPool(5).props(QueryRouter.props), QueryRouter.name)

  val smallRoute: Route = WebRoute.createRoute(queryRouter)

  "The service" should {

    "return user with id = 1" in {
      // tests:
      Get("/users/1") ~> smallRoute ~> check {
        responseAs[String] shouldEqual "{\"first_name\":\"Инна\",\"email\":\"iwgeodwa@list.me\",\"id\":1,\"last_name\":\"Терыкатева\",\"birth_date\":-712108800,\"gender\":\"f\"}"
      }
    }

    "return user bad" in {
      // tests:
      Get("/users/bad") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return user -1" in {
      // tests:
      Get("/users/-1") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "The requested resource could not be found."
      }
    }

    "/users/217/visits?toDistance=adeefcecbedeececafafebfbbdabedda" in {
      // tests:
      Get("/users/217/visits?toDistance=adeefcecbedeececafafebfbbdabedda") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.BadRequest

      }
    }

    "/users/137/visits?country=%D0%95%D0%B3%D0%B8%D0%BF%D0%B5%D1%82" in {
      // tests:
      Get("/users/137/visits?country=%D0%95%D0%B3%D0%B8%D0%BF%D0%B5%D1%82") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"visits\":[{\"mark\":2,\"visited_at\":993060501,\"place\":\"Ручей\"}]}"
      }
    }

    "/users/1140/visits?toDistance=21&toDate=1323302400&country=%D0%AD%D1%81%D1%82%D0%BE%D0%BD%D0%B8%D1%8F2" in {
      // tests:
      Get("/users/1140/visits?toDistance=21&toDate=1323302400&country=%D0%AD%D1%81%D1%82%D0%BE%D0%BD%D0%B8%D1%8F") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return list of visits for user 1057" in {
      // tests:
      Get("/users/1057/visits") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "/users/1052/visits?country=%D0%90%D1%80%D0%BC%D0%B5%D0%BD%D0%B8%D1%8F&fromDate=1575417600" in {
      // tests:
      Get("/users/1052/visits?country=%D0%90%D1%80%D0%BC%D0%B5%D0%BD%D0%B8%D1%8F&fromDate=1575417600") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "/users/561/visits?fromDate=1150675200" in {
      // tests:
      Get("/users/561/visits?fromDate=1150675200") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"visits\":[{\"mark\":1,\"visited_at\":1295142610,\"place\":\"Ресторан\"},{\"mark\":1,\"visited_at\":1330864126,\"place\":\"Замок\"}]}"
      }
    }

    "/users/587/visits?toDistance=91&toDate=1041897600&country=%D0%9D%D0%B8%D0%B4%D0%B5%D1%80%D0%BB%D0%B0%D0%BD%D0%B4%D1%8B" in {
      // tests:
      Get("/users/587/visits?toDistance=91&toDate=1041897600&country=%D0%9D%D0%B8%D0%B4%D0%B5%D1%80%D0%BB%D0%B0%D0%BD%D0%B4%D1%8B") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"visits\":[]}"
      }
    }

    "/locations/93/avg?fromDate=1566086400" in {
      // tests:
      Get("/locations/93/avg?fromDate=1566086400") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"avg\":0.0}"
      }
    }

    "/locations/442/avg?toDate=1546819200&toAge=70&fromAge=28" in {
      // tests:
      Get("/locations/442/avg?toDate=1546819200&toAge=70&fromAge=28") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"avg\":2.33333}"
      }
    }

    "/locations/463/avg?fromAge=21" in {
      // tests:
      Get("/locations/463/avg?fromAge=21") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"avg\":2.4}"
      }
    }

    "/locations/735/avg?fromAge=16" in {
      // tests:
      Get("/locations/735/avg?fromAge=16") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"avg\":2.0}"
      }
    }

    "leave GET requests to other paths unhandled" in {
      // tests:
      Get("/kermit") ~> smallRoute ~> check {
        handled shouldBe false
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      // tests:
      Put() ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.MethodNotAllowed
        responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
      }
    }
  }

}
