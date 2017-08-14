package com.gmail.webserg.hightload

import java.nio.file.Paths

import akka.actor.{Actor, Props}
import com.gmail.webserg.hightload.ArchivatorActor.StartArchivator

object ArchivatorActor {

  val name = "dataArchivator"

  val in = "C:\\git\\hightLoad\\webserver\\resources\\data.zip"
  val out = "C:\\git\\hightLoad\\webserver\\resources\\data"

  case object StartArchivator

  case object DataArchived

  def props: Props = Props[ArchivatorActor]
}

class ArchivatorActor extends Actor {

  import ArchivatorActor._

  def receive = {
    case StartArchivator => {
      Archivator.unzip(in, Paths.get(out))
      sender ! DataArchived
    }
  }

}
