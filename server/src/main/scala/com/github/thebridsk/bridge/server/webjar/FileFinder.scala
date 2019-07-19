package com.github.thebridsk.bridge.server.webjar

import java.util.Properties
import com.github.thebridsk.utilities.logging.Logger

object FileFinder {
  val log = Logger[FileFinder]
}

/**
  * Helper to find files in WebJars.
  * @param groupid the group ID for the webjar
  * @param artifactid the artifact ID for the webjar
  */
class FileFinder(
    groupid: String,
    artifactid: String,
    versionoverride: Option[String] = None,
    suffix: Option[String] = None
) {

  val loader = classOf[FileFinder].getClassLoader
  val version = versionoverride match {
    case None =>
      val pin = loader.getResourceAsStream(
        "META-INF/maven/" + groupid + "/" + artifactid + "/pom.properties"
      )
      val p = new Properties
      if (pin != null) {
        p.load(pin)
      }
      p.getProperty("version", "unknown")
    case Some(v) => v
  }

  FileFinder.log.info(
    "Version of group " + groupid + " artifact " + artifactid + " is " + version + " suffix " + suffix
  )

  /**
    * determine if this is the artifact ID of the webjar
    * @param art the artifact ID of the webjar
    */
  def isArtifact(art: String) = artifactid == art

  val baseName1 = "META-INF/resources/webjars/" + artifactid + "/" + version
  val baseName = suffix.map(s => baseName1 + "/" + s).getOrElse(baseName1)

  def resourceName(res: String) =
    baseName + (if (res.startsWith("/")) res; else "/" + res)

  /**
    * Return the full resource name.  The name will start with "META-INF/resources/webjars/".
    * @param res the resource name in the webjar
    * @return None is returned if not found, Some(name) is returned if found.
    */
  def getResource(reslist: String*): Option[String] = {
    for (res <- reslist) {
      if (checkName(res)) {
        val s = resourceName(res)
        val url = loader.getResource(s)
        if (url != null) return Some(s)
        else {
          FileFinder.log.fine(
            s"Unable to find resource in classpath in resource dir ${baseName}: $s"
          )
        }
      } else {
        FileFinder.log.fine("Resource name is not valid: " + res)
      }
    }
    FileFinder.log.warning(
      s"Unable to find one of in classpath in resource dir ${baseName}: ${reslist.mkString(", ")}"
    )
    None
  }

  def checkName(res: String): Boolean = {
    val i = res.indexOf("/../")
    if (res.startsWith("../") || res.endsWith("/..") || res.indexOf("/../") >= 0)
      false
    else true
  }

  /**
    * Return the full resource name.  The name will start with "META-INF/resources/webjars/".
    * @param art the artifact ID of the webjar
    * @param res the resource name in the webjar
    * @return None is returned if not found, Some(name) is returned if found.
    */
  def findResource(art: String, res: String): Option[String] = {
    if (isArtifact(art)) getResource(res)
    else None
  }
}
