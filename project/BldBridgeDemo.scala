
import sbt._
import Keys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitBranchPrompt
import sbtcrossproject.{crossProject, CrossType}
import BldDependencies._
import BldCommonSettings._
import sbtassembly.AssemblyPlugin.autoImport._

object BldBridgeDemo {

  lazy val demo: Project = project.in(file("demo"))
    .configure( commonSettings )
    .dependsOn(BldBridgeScoreKeeper.bridgescorekeeper % "test->test" )
    .dependsOn(BldBridgeServer.`bridgescorer-server`)
    .dependsOn(ProjectRef(uri("utilities"), "utilities-jvm"))
    .settings(
      name := "bridgedemo",

      mainClass in Compile := Some("com.github.thebridsk.bridgedemo.Publish"),

      libraryDependencies ++= bridgeScorerDemoDeps,

      generateDemo := Def.taskDyn {
        val jar = (crossTarget in BldBridgeScoreKeeper.bridgescorekeeper).value / (assemblyJarName in assembly in BldBridgeScoreKeeper.bridgescorekeeper).value
        val target = baseDirectory.value / "target" / "demo"
        // val jar = (assembly in BldBridgeScoreKeeper.bridgescorekeeper).value
        Def.task {
          (run in Compile).toTask(s""" $jar $target""").value
        }
      }.value,

      fork in run := true,
      fork in Test := true,
    )

}
