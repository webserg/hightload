package com.lightbend.akka.sample

import com.gmail.webserg.hightload.{LocationDataReader, UserDataReader, VisitDataReader}

object DataReaderTest extends App{
  println(UserDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\users_1.json"))
  println(VisitDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\visits_1.json"))
  println(LocationDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\locations_1.json"))

}
