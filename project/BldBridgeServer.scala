import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._
import com.typesafe.sbt.gzip.SbtGzip.autoImport._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitBranchPrompt
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import webscalajs.WebScalaJS.autoImport._
import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin
import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._
import MyReleaseVersion._

// import MyReleaseVersion._

object BldBridgeServer {

  lazy val `bridgescorer-server`: Project = project
    .in(file("server"))
    .configure(
      commonSettings,
      buildInfo("com.github.thebridsk.bridge.server.version", "VersionServer")
    )
    .dependsOn(BldBridgeShared.sharedJVM)
    .dependsOn(ProjectRef(uri("utilities"), "utilities-jvm"))
    .dependsOn(BldBridgeRotation.rotationJVM % "test")
    .dependsOn(BldColor.colorJVM % "test->compile")
    .settings(
      name := "bridgescorer-server",
      //    mainClass in Compile := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, run) := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, packageBin) := Some("com.github.thebridsk.bridge.server.Server"),
      Compile / run / fork := true,

      mainClass in Test := Some("org.scalatest.tools.Runner"),
      testOptions in Test += Tests.Filter { s =>
        if (s == "com.github.thebridsk.bridge.server.test.AllUnitTests") {
          println("Using Test:    " + s)
          true
        } else {
          println(s"Ignoring Test: $s, looking for $testToRun");
          false
        }
      },
      // baseDirectory in Test := {
      //   val x = (baseDirectory in Test).value
      //   println(s"baseDirectory in test is ${x}")
      //   x
      // },
      fork in Test := true,
      javaOptions in Test ++= Seq(
        "-Xmx4096M",
        "-DDefaultWebDriver=" + useBrowser,
        "-DSessionComplete=" + useBrowser,
        "-DSessionDirector=" + useBrowser,
        "-DSessionTable1=" + useBrowser,
        "-DSessionTable2=" + useBrowser,
        s"""-DUseProductionPage=${if (useFullOpt) "1" else "0"}"""
      ),

      // The following depend on the javax.xml.bind version
      //   "io.swagger.core.v3" % "swagger-core"
      // This must be included to prevent assembly failure because of duplicate files
      // the new library is "jakarta.xml.bind" % "jakarta.xml.bind-api"
      excludeDependencies ++= Seq(
        ExclusionRule(organization = "javax.xml.bind" ),
      ),
      libraryDependencies ++= bridgeScorerDeps.value,
      libraryDependencies ++= bridgeScorerServerDeps.value,
    )

}
