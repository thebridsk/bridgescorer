
import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{crossProject => _, CrossType => _, _}

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldColor {

  lazy val color = crossProject(JSPlatform, JVMPlatform).in(file("color")).
    configure(commonSettings,buildInfo("com.github.thebridsk.color.version", "VersionColor")).
    settings(
      name := "color",

      libraryDependencies ++= bridgeScorerColorDeps.value,

    ).
    jvmSettings(

    ).
    jsSettings(

      // This gets rid of the jetty check which is required for the sbt runtime
      // not the application
      //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
      //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
  //    dependencyUpdatesExclusions := moduleFilter(organization = "org.eclipse.jetty")
      dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty"),

      testOptions in Test += {
        if (inTravis) println("Not running JS tests in bridgescorer-rotation")
        Tests.Filter(s => !inTravis)
      }

    )

  lazy val colorJS: Project = color.js
  lazy val colorJVM = color.jvm

}
