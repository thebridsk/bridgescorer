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

object BldBridgeFullServer {

  lazy val `bridgescorer-fullserver`: Project = project
    .in(file("fullserver"))
    .configure(
      commonSettings
    )
    .enablePlugins(WebScalaJSBundlerPlugin)
    .dependsOn(BldBridgeServer.`bridgescorer-server` % "compile->compile;test->test")
    .dependsOn(BldBrowserPages.browserpages % "test->compile")
    .settings(
      name := "bridgescorer-fullserver",
      //    mainClass in Compile := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, run) := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, packageBin) := Some("com.github.thebridsk.bridge.server.Server"),
      Compile / run / fork := true,
      server := {
        (run in Compile).toTask(""" --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs""").value
      },
      serverssl := {
        (run in Compile).toTask(""" --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs --certificate ../server/key/examplebridgescorekeeper.jks --certpassword abcdef --https 8443 --cacert ../server/key/examplebridgescorekeeperca.crt""").value
      },
      serverhttps2 in Test := {
        (runMain in Test).toTask(""" com.github.thebridsk.bridge.server.Server --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs --certificate ../server/key/examplebridgescorekeeper.jks --certpassword abcdef --https 8443 --http2 --cacert ../server/key/examplebridgescorekeeperca.crt""").value
      },
      serverhttp2 in Test := {
        (runMain in Test).toTask(""" com.github.thebridsk.bridge.server.Server --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs --http2""").value
      },
      servertemp := {
        (run in Compile).toTask(""" --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/temp --diagnostics ../server/logs""").value
      },
      serverlogs := {
        (run in Compile).toTask(""" --logconsolelevel=ALL start --cache 0s --store ../server/store""").value
      },
      // shebang the jar file.  7z and jar will no longer see it as a valid zip file.
      //    assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript)),
      //    assemblyJarName in assembly := s"${name.value}-{version.value}",

      mainClass in Test := Some("org.scalatest.tools.Runner"),
      testOptions in Test += Tests.Filter { s =>
        if (s == testToRun) {
          println("Using Test:    " + s)
          true
        } else {
          println(s"Ignoring Test: $s, looking for $testToRun");
          false
        }
      },

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
      // parallelExecution in Test := false,

      // The following depend on the javax.xml.bind version
      //   "io.swagger.core.v3" % "swagger-core"
      // This must be included to prevent assembly failure because of duplicate files
      // the new library is "jakarta.xml.bind" % "jakarta.xml.bind-api"
      excludeDependencies ++= Seq(
        ExclusionRule(organization = "javax.xml.bind" ),
      ),
      libraryDependencies ++= bridgeScorerDeps.value,
      libraryDependencies ++= bridgeScorerFullServerDeps.value,
      bridgeScorerNpmAssets(BldBridgeClientApi.`bridgescorer-clientapi`),
      scalaJSProjects := Seq(BldBridgeClient.`bridgescorer-client`, BldBridgeClientApi.`bridgescorer-clientapi`),
      pipelineStages in Assets := {
        if (onlyBuildDebug) {
          Seq(scalaJSDev, gzip),
        } else if (useFullOpt) {
          Seq(scalaJSProd, gzip)
        } else {
          Seq(scalaJSProd, scalaJSDev, gzip),
        }
      },
      pipelineStages in Test in Assets := {
        if (onlyBuildDebug) {
          Seq(scalaJSDev, gzip),
        } else if (useFullOpt) {
          Seq(scalaJSProd, gzip)
        } else {
          Seq(scalaJSProd, scalaJSDev, gzip),
        }
      },
    )
}
