

import sbt._
import sbt.Keys._

import scalajsbundler.Npm
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import java.lang.RuntimeException

object MyNpm {



  val checkForNpmUpdates = taskKey[Unit]("Check for NPM dependency updates")

  val myNpmSettings = Seq(
    checkForNpmUpdates := {
      val npmDirectory = (npmUpdate).value
      val log = streams.value.log
      log.debug("npmDirectory is "+npmDirectory)

      try {
        Npm.run( "outdated" )(npmDirectory, log)
      } catch {
        case x: RuntimeException =>
          // hack alert
          // eat RuntimeExceptions that say Non-zero exit code.
          if (!x.getMessage.startsWith("Non-zero exit code: ")) {
            throw x
          }
      }
    }
  )
}
