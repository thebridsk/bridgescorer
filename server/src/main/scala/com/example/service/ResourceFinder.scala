package com.example.service

import com.example.webjar.FileFinder
import com.example.version.VersionServer
import scala.reflect.io.Directory
import utils.logging.Logger

object ResourceFinder {
  val logger = Logger( getClass.getName )

  def htmlResources = {
    val tryServerVersion = new FileFinder( "com.example", "bridgescorer-server", Some(VersionServer.version) )
    tryServerVersion.getResource("/index.html") match {
      case Some(v) => tryServerVersion
      case None =>
        val dir = Directory("target/web/classes/main/META-INF/resources/webjars/bridgescorer-server")
        logger.warning("Looking in directory "+dir.toAbsolute)
        if (dir.exists) {
          // find dir with latest bridgescorer-client-fastopt-bundle.js
          try {
            val (found,date) = dir.dirs.map(d => {
              val f = d/"bridgescorer-client-fastopt.js"
              if (f.exists) (d,f.lastModified)
              else (d,0L)
            }).reduce((l,r) =>
                if (l._2 < r._2) r
                else l
            )
            val v = found.name
            logger.warning("Using client version "+v)
            new FileFinder( "com.example", "bridgescorer-server", Some(v) )
          } catch {
            case x: Exception => throw new IllegalStateException("Can't find the client code",x)
          }

        } else {
          throw new IllegalStateException("Can't find the client code")
        }
    }

  }

}
