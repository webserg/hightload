package com.gmail.webserg.hightload

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.nio.file.Path
import java.util.zip.{ZipEntry, ZipInputStream}

object Archivator {
  def unzip(zipFilePath: String, destination: Path): Unit = {

    Control.using(new FileInputStream(zipFilePath)) { fis =>
      Control.using(new ZipInputStream(fis)) { zis =>
        Stream.continually(zis.getNextEntry).takeWhile(_ != null).foreach { file =>
          if (!file.isDirectory) {
            val outPath = destination.resolve(file.getName)
            val outPathParent = outPath.getParent
            if (!outPathParent.toFile.exists()) {
              outPathParent.toFile.mkdirs()
            }
            val outFile = outPath.toFile
            Control.using(new FileOutputStream(outFile)) { out =>
              val buffer = new Array[Byte](4096)
              Stream.continually(zis.read(buffer)).takeWhile(_ != -1).foreach(out.write(buffer, 0, _))
            }
          }
        }
      }
    }
  }
}
