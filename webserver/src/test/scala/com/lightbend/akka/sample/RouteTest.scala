package com.lightbend.akka.sample

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.routing.RoundRobinPool
import akka.util.{ByteString, Timeout}
import com.gmail.webserg.hightload.WebServer.WebServerProps
import com.gmail.webserg.hightload.{QueryRouter, WebRoute, WebServer}
import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._


class RouteTest extends WordSpec with Matchers with ScalatestRouteTest {
  val webserverProps: WebServerProps = WebServerProps("C:\\git\\hightLoad\\", "C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\")

  val addr = WebServer.loadData(webserverProps, system)
  val queryRouter: ActorRef = system.actorOf(RoundRobinPool(25).props(Props(new QueryRouter(addr))), QueryRouter.name)

  val smallRoute: Route = WebRoute.createRoute(queryRouter)

  implicit val timeout: Timeout = 3000 millisecond


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
   "/locations/11/avg?gender=m&fromDate=1273795200&toAge=33" in {
      // tests:
      Get("/locations/11/avg?gender=m&fromDate=1273795200&toAge=33") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"avg\":4.0}"
      }
    }

   "/locations/11/avg?gender=sfsfsdfsdf" in {
      // tests:
      Get("/locations/11/avg?gender=sdfsdfsdfsd") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual "{}"
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

    "/users/809 post" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "birth_date": 616550400, "last_name": "serg", "email": "termilnodsitasen@mail.ru"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/users/809",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{}"
      }
    }

    "/users/new" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "first_name": "\u041b\u044e\u0431\u043e\u0432\u044c", "last_name": "\u0414\u0430\u043d\u043b\u0435\u043d\u043a\u0430\u044f", "gender": "f", "id": 1032, "birth_date": -680054400, "email": "udgivwev@mail.ru"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/users/new",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{}"
      }
    }

    "/users/809 post null" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "birth_date": 616550400, "last_name": null, "email": "termilnodsitasen@mail.ru"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/users/809",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual "{}"
      }
    }

    "/users/809 post wrong data" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "birth_date": 616550400, "last_name": 1, "email": "termilnodsitasen@mail.ru"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/users/809",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual "{}"
      }
    }


    "/users/256/visits" in {
      // tests:
      Get("/users/256/visits") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"visits\":[{\"mark\":3,\"visited_at\":952703235,\"place\":\"Ручей\"},{\"mark\":1,\"visited_at\":1056622577,\"place\":\"Набережная\"},{\"mark\":3,\"visited_at\":1058884526,\"place\":\"Улица\"},{\"mark\":1,\"visited_at\":1094315689,\"place\":\"Улочка\"},{\"mark\":2,\"visited_at\":1177544827,\"place\":\"Набережная\"},{\"mark\":1,\"visited_at\":1192898482,\"place\":\"Улица\"},{\"mark\":2,\"visited_at\":1246566491,\"place\":\"Площадь\"},{\"mark\":3,\"visited_at\":1279548701,\"place\":\"Набережная\"},{\"mark\":4,\"visited_at\":1301879595,\"place\":\"Пруд\"},{\"mark\":3,\"visited_at\":1319717860,\"place\":\"Лес\"}]}"
      }
    }


    "/visits/new id null" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |"id": null, "user": 256, "visited_at": 1302197249, "location": 354, "mark": 2
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/visits/new",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "/visits/new" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |"id": 100000, "user": 2, "visited_at": 1302197249, "location": 354, "mark": 2
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/visits/new",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{}"
      }
    }

    "return visit with id = 100000" in {
      // tests:
      Get("/visits/100000") ~> smallRoute ~> check {
        responseAs[String] shouldEqual "{\"location\":354,\"visited_at\":1302197249,\"id\":100000,\"mark\":2,\"user\":2}"
      }
    }


    "/users/2/visits + new" in {
      // tests:
      Get("/users/2/visits") ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        val res = responseAs[String]
        println(res)
        res.contains("1302197249") shouldBe(true)
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
        responseAs[String] shouldEqual "HTTP method not allowed, supported methods: POST, GET"
      }
    }

    "return user with id = 809" in {
      // tests:
      Get("/users/809") ~> smallRoute ~> check {
        responseAs[String] shouldEqual "{\"first_name\":\"Денис\",\"email\":\"termilnodsitasen@mail.ru\",\"id\":809,\"last_name\":\"serg\",\"birth_date\":616550400,\"gender\":\"m\"}"
      }
    }

    "return user with id = 1032" in {
      // tests:
      Get("/users/1032") ~> smallRoute ~> check {
        responseAs[String] shouldEqual "{\"first_name\":\"Любовь\",\"email\":\"udgivwev@mail.ru\",\"id\":1032,\"last_name\":\"Данленкая\",\"birth_date\":-680054400,\"gender\":\"f\"}"
      }
    }

     "return location with id = 310" in {
      // tests:
      Get("/locations/310") ~> smallRoute ~> check {
        responseAs[String] shouldEqual "{\"city\":\"Роттеринск\",\"country\":\"Норвегия\",\"id\":310,\"place\":\"Склон\",\"distance\":17}"
      }
    }

    "/locations/310 post" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "distance": 46,
           |  "place": "Фонтан",
           |  "country": "Белоруссия"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/locations/310",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{}"
      }
    }

    "/locations/310 post 2" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "distance": 46,
           |  "place": "Фонтан",
           |  "country": "Белоруссия"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/locations/310",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{}"
      }
    }



    "/locations/27 post" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "city": null,
           |  "place": "Здание"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/locations/27",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual "{}"
      }
    }

    "return location with id = 310 changed" in {
      // tests:
      Get("/locations/310") ~> smallRoute ~> check {
        responseAs[String] shouldEqual "{\"city\":\"Роттеринск\",\"country\":\"Белоруссия\",\"id\":310,\"place\":\"Фонтан\",\"distance\":46}"
      }
    }


    "/visits/new 100616" in {
      // tests:
      val jsonRequest = ByteString(
        s"""
           |{
           |  "id": 100616,
           |  "user": 1,
           |  "visited_at": 1095915810,
           |  "location": 25,
           |  "mark": 3
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = "/visits/new",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(smallRoute) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{}"
      }
    }
    "return visit with id = 100616" in {
      // tests:
      Get("/users/1") ~> smallRoute ~> check {
        val res = responseAs[String]
        println(res)
        res shouldEqual "{\"first_name\":\"Инна\",\"email\":\"iwgeodwa@list.me\",\"id\":1,\"last_name\":\"Терыкатева\",\"birth_date\":-712108800,\"gender\":\"f\"}"
      }
    }


  }

}
