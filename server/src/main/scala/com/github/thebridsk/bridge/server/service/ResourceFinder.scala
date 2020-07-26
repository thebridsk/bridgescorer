package com.github.thebridsk.bridge.server.service

import com.github.thebridsk.bridge.server.webjar.FileFinder
import com.github.thebridsk.bridge.server.version.VersionServer
import scala.reflect.io.Directory
import com.github.thebridsk.utilities.logging.Logger
import scala.reflect.io.File

object ResourceFinder {
  val logger: Logger = Logger(getClass.getName)

  /**
    * Validate if the component, version, and suffix contains the client code.
    * The client code is looked for in the following resource:
    *
    *   META-INF/resources/webjars/<component>/<version>[/<suffix>]
    *
    * @param component bridgescorer or bridgescorer-fullserver
    * @param version the version string
    * @param suffix a suffix
    * @return an optional FileFinder.  None is return if this is not valid.
    */
  def validateServerVersion(
      component: String,
      version: String,
      suffix: Option[String]
  ): Option[(FileFinder, String)] = {
    val tryServerVersion =
      new FileFinder("com.github.thebridsk.bridge.server", component, Some(version), suffix)
    tryServerVersion.getResource(
      "/bridgescorer-client-opt.js.gz",
      "/bridgescorer-client-opt.js",
      "/bridgescorer-client-fastopt.js.gz",
      "/bridgescorer-client-fastopt.js"
    ) match {
      case None    => None
      case Some(v) => Some((tryServerVersion, v))
    }
  }

  /**
    * Validate if the component, version, and suffix contains the client code.
    * The client code is looked for in the following resource:
    *
    *   META-INF/resources/webjars/<component>/<version>[/<suffix>]
    *
    * @param component bridgescorer or bridgescorer-fullserver
    * @param version the version string
    * @param suffix a suffix
    * @return an optional FileFinder.  None is return if this is not valid.
    */
  def validateServerVersionWithHelp(
      component: String,
      version: String,
      suffix: Option[String]
  ): Option[(FileFinder, String)] = {
    val tryServerVersion =
      new FileFinder("com.github.thebridsk.bridge.server", component, Some(version), suffix)
    tryServerVersion.getResource(
      "/index.html"
    ) match {
      case None    => None
      case Some(v) => Some((tryServerVersion, v))
    }
  }

  private val patternVersion = """(.*?)-[0-9a-fA-F]+""".r
  def baseVersion(ver: String): Option[String] = {
    ver match {
      case patternVersion(version) => Some(version)
      case _                       => None
    }
  }

  def searchOnVersion(
      component: String,
      suffix: Option[String],
      validate: (String, String, Option[String]) => Option[(FileFinder, String)]
  ): Option[FileFinder] = {

    (baseVersion(VersionServer.version).map { ver =>
      validate(component, ver, suffix) match {
        case Some((ff, f)) =>
          logger.info(s"For $component $suffix found $f")
          Some(ff)
        case None =>
          None
      }
    }) match {
      case Some(Some(f)) => Some(f)
      case _ =>
        validate(component, VersionServer.version, suffix) match {
          case Some((ff, f)) =>
            logger.info(s"For $component $suffix found $f")
            Some(ff)
          case None =>
            val targetDir = Directory(
              s"target/web/classes/main/META-INF/resources/webjars/$component"
            )
            logger.warning("Looking in directory " + targetDir.toAbsolute)

            val x =
              if (targetDir.exists) {
                val tdir = targetDir.dirs.flatMap { dir =>
                  validate(component, dir.toFile.name, suffix) match {
                    case None => Nil
                    case Some((ff, f)) =>
                      val resAsFile = File(s"target/web/classes/main/$f")
                      if (resAsFile.isFile) {
                        logger.info(
                          s"For $component suffix $suffix found $resAsFile"
                        )
                        Some((ff, resAsFile.lastModified)) :: Nil
                      } else {
                        logger.warning(s"Could not find resource $resAsFile")
                        Nil
                      }
                  }
                }.toList
                tdir
              } else {
                None :: Nil
              }
            val (resultDir, lastmod) =
              x.foldLeft((None: Option[FileFinder], 0L)) { (ac, v) =>
                if (ac._1.isDefined) {
                  if (v.isDefined) {
                    if (ac._2 < v.get._2) (Some(v.get._1), v.get._2)
                    else ac
                  } else {
                    ac
                  }
                } else {
                  if (v.isDefined) (Some(v.get._1), v.get._2)
                  else ac
                }
              }
            resultDir.map(f => logger.info(s"Using resource ${f.baseName}")).getOrElse(logger.info(s"Did not find resources for component ${component}, suffix ${suffix}"))
            resultDir
        }
    }
  }

  def htmlResources: FileFinder = {
    // must look for bridgescorer-fullserver resources also to find client code

    searchOnVersion("bridgescorer-fullserver", None, validateServerVersion) match {
      case Some(f) =>
        logger.info(s"Found client at ${f.baseName}")
        f
      case None =>
        searchOnVersion(
          "bridgescorekeeper",
          Some("lib/bridgescorer-fullserver"),
          validateServerVersion
        ) match {
          case Some(f) =>
            logger.info(s"Found client at ${f.baseName}")
            f
          case None =>
            val curDir = File(".").toCanonical.toAbsolute
            val dirName = curDir.name
            logger.warning(s"Unable to find client code, running in ${curDir}")
            if (dirName == "server") {
              new FileFinder("com.github.thebridsk.bridge.server", "bridgescorer-fullserver")
            } else {
              throw new IllegalStateException("Unable to find client code")
            }
        }
    }

  }

  def helpResources: FileFinder = {

    searchOnVersion("bridgescorekeeper", Some("help"), validateServerVersionWithHelp) match {
      case Some(f) =>
        logger.info(s"Found help at ${f.baseName}")
        f
      case None =>
        throw new IllegalStateException("Unable to find help resource")
    }

  }

}
