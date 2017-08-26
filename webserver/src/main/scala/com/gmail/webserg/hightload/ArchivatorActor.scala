package com.gmail.webserg.hightload

import java.nio.file.Paths

import akka.actor.{Actor, Props}
import com.gmail.webserg.hightload.DataLoaderActor.LoadData

object ArchivatorActor {

  val dirName = "/tmp/hightLoad/data/"
  val archivedirName = "/tmp/data/"
//    val dirName = "C:\\git\\hightLoad\\data\\"
//    val archivedirName = "C:\\git\\hightLoad\\"

  val name = "dataArchivator"

  val in = archivedirName + "data.zip"
  val out = dirName

  case object StartArchivator

  case object DataArchived

  def props: Props = Props[ArchivatorActor]
}

class ArchivatorActor extends Actor {

  import ArchivatorActor._

  def receive = {
    case StartArchivator => {
      Archivator.unzip(in, Paths.get(out))
      context.actorSelection("/user/" + DataLoaderActor.name) ! LoadData
    }
  }

}
