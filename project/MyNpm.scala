

import sbt._
import sbt.Keys._

import scalajsbundler.Npm
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

object MyNpm {



  val checkForNpmUpdates = taskKey[Unit]("Check for NPM dependency updates")

  val myNpmSettings = Seq(
    checkForNpmUpdates := {
      val npmDirectory = (npmUpdate).value
      val log = streams.value.log
      log.debug("npmDirectory is "+npmDirectory)

      Npm.run( "outdated" )(npmDirectory, log)
    }
  )
}
