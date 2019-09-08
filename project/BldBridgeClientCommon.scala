
import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldBridgeClientCommon {

  lazy val `bridgescorer-clientcommon` = project.in(file("clientcommon"))
    .configure( commonSettings, noTests(true) )
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(
      BldBridgeShared.sharedJS,
      BldMaterialUI.materialui,
      BldColor.colorJS,
      `utilities-js`
    )
    .settings(
      name := "bridgescorer-clientcommon",
      libraryDependencies ++= clientcommonDeps.value,
      testOptions in Test += Tests.Filter(s => {
//        if (s == "com.github.thebridsk.bridge.clientcommon.test.AllCommonUnitTests") {
          if (s == "com.github.thebridsk.bridge.clientcommon.test.TestColor") {
            println("Using Test:    " + s)
            true
          } else {
            println("Ignoring Test: " + s);
            false
          }
        }),
    )

}