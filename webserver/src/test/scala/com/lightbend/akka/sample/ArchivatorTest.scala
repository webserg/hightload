package com.lightbend.akka.sample

import java.io.File
import java.nio.file.{FileSystem, Path, Paths}

import com.gmail.webserg.hightload.Archivator

object ArchivatorTest extends App{

  Archivator.unzip("C:\\git\\hightLoad\\webserver\\resources\\data.zip",
     Paths.get("C:\\git\\hightLoad\\webserver\\resources\\data"))

}
