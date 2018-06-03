package com.example.service

import com.example.webjar.FileFinder
import com.example.version.VersionServer
import scala.reflect.io.Directory
import utils.logging.Logger

object ResourceFinder {
  val logger = Logger( getClass.getName )

  def htmlResources = {
    val tryServerVersion = new FileFinder( "com.example", "bridgescorer-server", Some(VersionServer.version) )
    tryServerVersion.getResource("/bridgescorer-client-fastopt.js") match {
      case Some(v) => tryServerVersion
      case None =>
        val dirs = Directory("target/web/classes/main/META-INF/resources/webjars/bridgescorer-server")::
//                   Directory("target/web/classes/main/META-INF/resources/webjars/bridgescorer")::
                   Nil
        val tdir = dirs.flatMap { dir =>
          logger.warning("Looking in directory "+dir.toAbsolute)
          if (dir.exists) {
            try {
              val (found,date) = dir.dirs.map(d => {
                val f = d/"bridgescorer-client-fastopt.js.gz"
                if (f.exists) (d,f.lastModified)
                else (d,0L)
              }).reduce((l,r) =>
                  if (l._2 < r._2) r
                  else l
              )
              val v = found.name
              logger.warning("Using client version "+v)
              new FileFinder( "com.example", "bridgescorer-server", Some(v) )::Nil
            } catch {
              case x: Exception => throw new IllegalStateException("Can't find the client code",x)
            }
          } else {
            Nil
          }
        }
        if (tdir.length == 0) {
          logger.warning( "Unable to find client resource" )
          throw new IllegalStateException( "Unable to find client resource" )
        } else if (tdir.length == 1) tdir.head
        else {
          logger.warning( "found multiple client resources: "+tdir )
          throw new IllegalStateException( "found multiple client resources" )
        }
    }

  }

  def helpResources = {
    val version = VersionServer.version
    val shortversion = try {
      Some( version.split("-").head )
    } catch {
      case x: NoSuchElementException =>
        None
    }
    val tryServerVersion = new FileFinder( "com.example", "bridgescorer", Some(VersionServer.version) )
    tryServerVersion.getResource("/help/index.html") match {
      case Some(v) => tryServerVersion
      case None =>
        val r = if (shortversion.isDefined) {
          val tryServerVersion = new FileFinder( "com.example", "bridgescorer", shortversion )
          tryServerVersion.getResource("/help/index.html") match {
            case Some(v) => Some(tryServerVersion)
            case None => None
          }
        } else {
          None
        }
        r match {
          case Some(ff) => ff
          case None =>
            val dirs = Directory("target/web/classes/main/META-INF/resources/webjars/bridgescorer")::
                       Nil
            val tdir = dirs.flatMap { dir =>
              logger.warning("Looking in directory "+dir.toAbsolute)
              if (dir.exists) {
                try {
                  val (found,date) = dir.dirs.map(d => {
                    val f = d/"help"/"index.html"
                    if (f.exists) (d,f.lastModified)
                    else (d,0L)
                  }).reduce((l,r) =>
                      if (l._2 < r._2) r
                      else l
                  )
                  val v = found.name
                  logger.warning("Using client version "+v)
                  new FileFinder( "com.example", "bridgescorer", Some(v) )::Nil
                } catch {
                  case x: Exception => throw new IllegalStateException("Can't find the help code",x)
                }
              } else {
                Nil
              }
            }
            if (tdir.length == 0) {
              logger.warning( "Unable to find help resource" )
              throw new IllegalStateException( "Unable to find help resource" )
            } else if (tdir.length == 1) tdir.head
            else {
              logger.warning( "found multiple help resources: "+tdir )
              throw new IllegalStateException( "found multiple help resources" )
            }
        }
    }

  }

}
