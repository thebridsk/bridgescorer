
import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldBridgeClientCommon {

  lazy val `bridgescorer-clientcommon` = project.in(file("clientcommon"))
    .configure( commonSettings )
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(
      BldBridgeShared.sharedJS,
      BldBridgeMaterialUI.materialui,
      `utilities-js`
    )
    .settings(
      name := "bridgescorer-clientcommon",
      libraryDependencies ++= clientcommonDeps.value,
      testOptions in Test += Tests.Filter(s => {
//        if (s == "com.github.thebridsk.bridge.test.AllCommonUnitTests") {
          if (s == "com.github.thebridsk.bridge.test.TestColor") {
            println("Using Test:    " + s)
            true
          } else {
            println("Ignoring Test: " + s);
            false
          }
        }),
    )

}