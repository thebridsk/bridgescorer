import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldBrowserPages {

  lazy val browserpages: Project = project
    .in(file("browserpages"))
    .configure(
      commonSettings
    )
    .dependsOn(ProjectRef(uri("utilities"), "utilities-jvm"))
    .settings(
      name := "session",
      mainClass in Compile := None,
      Compile / run / fork := true,
      mainClass in Test := Some("org.scalatest.tools.Runner"),
      fork in Test := true,
      javaOptions in Test ++= Seq(
        "-DDefaultWebDriver=" + useBrowser,
        "-DSessionComplete=" + useBrowser,
        "-DSessionDirector=" + useBrowser,
        "-DSessionTable1=" + useBrowser,
        "-DSessionTable2=" + useBrowser,
        s"""-DUseProductionPage=${if (useFullOpt) "1" else "0"}"""
      ),
      libraryDependencies ++= browserPagesDeps.value,
    )
}
