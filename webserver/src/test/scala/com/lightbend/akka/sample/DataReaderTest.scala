package com.lightbend.akka.sample

import com.gmail.webserg.hightload.{LocationDataReader, UserDataReader, VisitDataReader}

object DataReaderTest extends App{
  val usersFileList = new java.io.File("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\").listFiles.filter(_.getName.startsWith("users"))
  val l = (for {(ls) <- usersFileList} yield UserDataReader.readData(ls).users).flatten.toList
  println(l.length)
  println(l)

  val locationsFileList = new java.io.File("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\").listFiles.filter(_.getName.startsWith("locations"))
  val listOfLocations = for {(ls) <- locationsFileList} yield LocationDataReader.readData(ls).locations
  println(listOfLocations.flatten.length)

  //  val t = UserDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\users_1.json")
//  println(UserDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\users_1.json"))
//  println(VisitDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\visits_1.json"))
//  println(LocationDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\locations_1.json"))

}
