
import sbt._
import Keys._

import sbtcrossproject.CrossProject
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
// import org.scalajs.sbtplugin.ScalaJSPlugin
// import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{crossProject => _, CrossType => _, _}

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldBridgeShared {

  lazy val `bridgescorer-shared` = CrossProject("shared", file("shared"))(JSPlatform, JVMPlatform).
    configure(commonSettings,buildInfo("com.github.thebridsk.bridge.data.version", "VersionShared")).
    settings(
      name := "bridgescorer-shared",
      resolvers += Resolver.bintrayRepo("scalaz", "releases"),

      libraryDependencies ++= bridgeScorerDeps.value,
      libraryDependencies ++= bridgeScorerSharedDeps.value,

    ).
    jvmSettings(
      libraryDependencies ++= bridgeScorerSharedJVMDeps.value
    ).
    jsSettings(

      // This gets rid of the jetty check which is required for the sbt runtime
      // not the application
      //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
      //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
      dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty")

    )

  lazy val sharedJS: Project = `bridgescorer-shared`.js.
    dependsOn( `utilities-js` )

  lazy val sharedJVM = `bridgescorer-shared`.jvm.
    dependsOn( `utilities-jvm` )

}
