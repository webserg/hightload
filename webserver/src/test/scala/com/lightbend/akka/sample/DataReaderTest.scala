package com.lightbend.akka.sample

import com.gmail.webserg.hightload.UserDataReader

object DataReaderTest extends App{
  println(UserDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\users_1.json").users)

}
