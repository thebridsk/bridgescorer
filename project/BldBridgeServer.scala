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
      server := {
        (run in Compile).toTask(""" --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs""").value
      },

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

      fork in Test := true,
      javaOptions in Test ++= Seq(
        "-Xmx4096M",
        "-DDefaultWebDriver=" + useBrowser,
        "-DSessionComplete=" + useBrowser,
        "-DSessionDirector=" + useBrowser,
        "-DSessionTable1=" + useBrowser,
        "-DSessionTable2=" + useBrowser,
        testProductionPage
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

      cleanFiles += baseDirectory.value / "key",

      generatesslkeys := (Def.taskDyn {

        val keydir = "key"

        val baseDir = baseDirectory.value

        val dir = baseDir / keydir

        val pw = "abcdef"

        val serverprefix = "examplebridgescorekeeper"
        val caprefix = "examplebridgescorekeeperca"
        val trustprefix = "examplebridgescorekeepertrust"

        val serverAlias = "bsk"

        val info = BldCommonSettings.SSLKeys(
            keystore = dir / s"${serverprefix}.jks",
            keystorepass = pw,
            serveralias = serverAlias,
            keypass = pw,
            truststore = dir / s"${trustprefix}.jks"
        )

        val marker = dir / "GenerateSSLKeys.Marker.txt"

        if (marker.isFile) {
          Def.task {
            info
          }
        } else {
          Def.task {
            val x = (run in Compile).toTask(
              """ sslkey"""+
              s""" -d $keydir"""+
              """ generateselfsigned"""+
              s""" -a $serverAlias"""+
              s""" --ca $caprefix"""+
              """ --caalias ca"""+
              """ --cadname "CN=BridgeScoreKeeperCA, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US""""+
              s""" --cakeypw $pw"""+
              s""" --castorepw $pw"""+
              """ --dname "CN=BridgeScoreKeeper, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US""""+
              s""" --keypass $pw"""+
              s""" --server $serverprefix"""+
              s""" --storepass $pw"""+
              s""" --trustpw $pw"""+
              s""" --truststore $trustprefix"""+
              """ -v"""+
              """ --nginx"""+
              """ --clean"""+
              """ --addmachineip"""
            ).value
            info
          }
        }
      }).value,

      ssltests in Test := Def.sequential(
        generatesslkeys,
        (Test/testClass).toTask(" com.github.thebridsk.bridge.server.test.ssl.AllSuitesSSL")
      ).value,

      test in Test := {
        val ssl = (ssltests in Test).value
        val t = (test in Test).value
      }
    )

}
