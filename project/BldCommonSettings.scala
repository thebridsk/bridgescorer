import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
//import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{crossProject => _, CrossType => _, _}
import org.scalajs.sbtplugin.ScalaJSCrossVersion
// import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
// import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

import BldVersion._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
import MyReleaseVersion._
import XTimestamp._

/**
  * The idea comes from https://github.com/japgolly/scalajs-react/blob/master/project/Build.scala
  *
  * To use,
  *    val x = project.
  *                configure(commonSettings,noTests)
  */
object BldCommonSettings {

  // stuff to have dependencies on other projects
  lazy val utilities = RootProject(file("utilities"))

  lazy val useBrowser = sys.props
    .get("UseBrowser")
    .orElse(sys.env.get("UseBrowser"))
    .getOrElse("chrome")

  lazy val skipGenerateImage = sys.props
    .get("skipGenerateImage")
    .orElse(sys.env.get("skipGenerateImage"))
    .map(s => s.toBoolean)
    .getOrElse(false)

  lazy val onlyBuildDebug = sys.props
    .get("OnlyBuildDebug")
    .orElse(sys.env.get("OnlyBuildDebug"))
    .map(s => s.toBoolean)
    .getOrElse(false)

  lazy val useFullOpt = sys.props
    .get("UseFullOpt")
    .orElse(sys.env.get("UseFullOpt"))
    .map(s => s.toBoolean)
    .getOrElse(false)

  lazy val serverTestToRun =
    sys.props.get("ServerTestToRun").orElse(sys.env.get("ServerTestToRun"))

//
// Debugging deprecation and feature warnings
//
// Through the sbt console...
//
//    reload plugins
//    set scalacOptions ++= Seq( "-unchecked", "-deprecation", "-feature" )
//    session save
//    reload return

  lazy val inTravis = sys.props
    .get("TRAVIS_BUILD_NUMBER")
    .orElse(sys.env.get("TRAVIS_BUILD_NUMBER"))
    .isDefined

  val buildForHelpOnly = sys.props
    .get("BUILDFORHELPONLY")
    .orElse(sys.env.get("BUILDFORHELPONLY"))
    .isDefined

  val testCaseToRun =
    sys.props.get("TESTCASETORUN").orElse(sys.env.get("TESTCASETORUN"))

  val testToRunNotTravis = "com.github.thebridsk.bridge.test.AllSuites"
  val testToRunBuildForHelpOnly = "com.github.thebridsk.bridge.test.selenium.DuplicateTestPages"
  val testToRunInTravis = "com.github.thebridsk.bridge.test.TravisAllSuites"

  lazy val testToRun = serverTestToRun.getOrElse(
    if (inTravis) {
      println(s"Running in Travis CI, tests to run: ${testToRunInTravis}")
      testToRunInTravis
    } else {
      val t = testCaseToRun.getOrElse(
        if (buildForHelpOnly) {
          testToRunBuildForHelpOnly
        } else {
          testToRunNotTravis
        }
      )
      println(s"Not running in Travis CI, tests to run: ${t}")
      t
    }
  )

  val moretestToRun = "com.github.thebridsk.bridge.test.selenium.IntegrationTests"
  val travisMoretestToRun = "com.github.thebridsk.bridge.test.selenium.TravisIntegrationTests"
  val testdataDir = "../testdata"

  val imoretestToRun =
    "com.github.thebridsk.bridge.test.selenium.integrationtest.IntegrationTests"
  val itravisMoretestToRun =
    "com.github.thebridsk.bridge.test.selenium.integrationtest.TravisIntegrationTests"
  val itestdataDir = "./testdata"


  lazy val bridgescorerAllProjects = ScopeFilter(
    inAggregates(BldBridge.bridgescorer, includeRoot = true)
  )

  lazy val utilitiesAllProjects = ScopeFilter(
    inAggregates(utilities, includeRoot = false)
  )

  lazy val bridgescorerAllCompileAndTestConfigurations = ScopeFilter(
    inAggregates(BldBridge.bridgescorer, includeRoot = false),
    inConfigurations(Compile,Test)
  )

  lazy val utilitiesAllCompileAndTestConfigurations = ScopeFilter(
    inAggregates(utilities, includeRoot = false),
    inConfigurations(Compile,Test)
  )

  lazy val `utilities-js` = ProjectRef( uri("utilities"), "utilities-js" )
  lazy val `utilities-jvm` = ProjectRef( uri("utilities"), "utilities-jvm" )

  lazy val Distribution = config("distribution") describedAs("tasks for creating a distribution.")

  // The prereqs for the integration tests,
  // to run integration tests (integrationtests, moretests) without running prereqs, on command line issue:
  //   set prereqintegrationtests := {}
  //
  // Looks like there is a bug that is preventing the above set if the key is
  // defined with taskKey[Unit](desc) instead of TaskKey[Unit](name,desc).
  // The error is a type error, expecting a T got a Unit.
  //
  val prereqintegrationtests = taskKey[Unit]( "Prereqs for unit tests on the assembly.jar file.") in Distribution

  val integrationtests = taskKey[Unit]("Runs integration tests on the assembly.jar file.") in Distribution

  val moretests = taskKey[Unit]("Runs more tests on the assembly.jar file.") in Distribution

  val travismoretests = taskKey[Unit]("Runs travis more tests on the assembly.jar file.") in Distribution

  val alltests = taskKey[Unit]("Runs all tests in JS and JVM projects.") in Distribution

  val travis = taskKey[Unit]("The build that is run in Travis CI.") in Distribution

  val travis1 = taskKey[Unit]("The build that is run in Travis CI.") in Distribution

  val travis2 = taskKey[Unit]("The build that is run in Travis CI.") in Distribution

  val disttests = taskKey[Unit]("Runs unit tests and more tests on the assembly.jar file.") in Distribution

  val publishdir = taskKey[Option[java.io.File]]("The target directory for mypublishcopy") in Distribution

  val mypublishcopy = taskKey[Unit]("Copy the published artifacts") in Distribution

  val mypublish = taskKey[Unit]("Publish by copying") in Distribution

  val myclean = taskKey[Unit]("clean") in Distribution

  val mytest = taskKey[Unit]("build and test it") in Distribution

  val mydist = taskKey[Unit]("Make a build for distribution") in Distribution

  val fvt = taskKey[Unit]("Run test cases using assembled jars, does not build jars") in Distribution

  val svt = taskKey[Unit]("Run test cases using assembled jars, does not build jars") in Distribution

  val travissvt = taskKey[Unit]("Run test cases that Travis CI uses using assembled jars, does not build jars") in Distribution

  val standalonetests = taskKey[Unit]("Run test cases using assembled jars, does not build jars") in Distribution

  val allassembly = taskKey[Unit]("Build assembly and test assembly")

  val webassembly = taskKey[Unit]("Build web application")

  val checkForUpdates = taskKey[Unit]("Check for updates")

  val checkProject = taskKey[Unit]("Check the project type")

  val skipGenerateImageSetting = settingKey[Boolean]("if true images generation is skipped if they already exist")

  val hugo = taskKey[Unit]("Run Hugo")
  val hugosetup = taskKey[Unit]("Setup to run Hugo")
  val hugoWithTest = taskKey[Unit]("Run Hugo")
  val hugosetupWithTest = taskKey[Unit]("Setup to run Hugo")
  val helptask = taskKey[Seq[(java.io.File, String)]]("Identifies help resources")

  val patternSourceDir = """^[0-9a-f]{20}$""".r
  val patternFastopt = """-fastopt[.-]|-jsconsole[.-]""".r


  /**
    * Add common settings to a project.
    *
    * Sets:
    *   scala version
    *   scalac options
    *   test options
    */
  def commonSettings: Project => Project =
    _.settings(versionSetting).settings(
      scalaVersion := verScalaVersion,
      crossScalaVersions := verCrossScalaVersions,
      scalacOptions := Seq(
        "-unchecked",
        "-deprecation",
        "-encoding",
        "utf8",
        "-feature",
//        "-Xlog-implicits",
      ),
      EclipseKeys.withSource := true,
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      EclipseKeys.useProjectId := true,

//       checkProject := {
// //        val p = crossProject.value
// //        println(s"Project of of type $p" )
//       }
    )

  /**
    * Add if project has no test cases
    */
  def noTests(js: Boolean): Project => Project = { proj =>
    val p = proj.settings(
      test in Test := {},
      testOnly in Test := {},
      testQuick in Test := {}
    )
    if (js) {
      p.settings(
        fastOptJS in Test := Attributed(
          artifactPath.in(fastOptJS).in(Test).value
        )(AttributeMap.empty),
        fullOptJS in Test := Attributed(
          artifactPath.in(fullOptJS).in(Test).value
        )(AttributeMap.empty)
      )
    } else {
      p
    }
  }

  /**
    * Add if project should not be published
    */
  def noPublish: Project => Project =
    _.settings(
      publishTo := Some(
        Resolver
          .file("Unused transient repository", target.value / "fakepublish")
      ),
      publishArtifact := false,
      publishLocal := {},
      // publishLocalSigned := {}, // doesn't work
      // publishSigned := {}, // doesn't work
      packagedArtifacts := Map.empty // doesn't work - https://github.com/sbt/sbt-pgp/issues/42
    )

  lazy val buildInfoCommonSettings = Seq(
    // this replaces
    //
    //     buildInfoOptions += BuildInfoOption.BuildTime
    //
    // This uses a constant timestamp if it is a snapshot build
    // to mitigate a long build time.
    buildInfoKeys ++= Seq[BuildInfoKey](
      BuildInfoKey.action("builtAtString") {
        string(isSnapshotVersion)
      },
      BuildInfoKey.action("builtAtMillis") {
        millis(isSnapshotVersion)
      }
    )
  )

  def buildInfo(pack: String, cls: String): Project => Project =
    _.enablePlugins(BuildInfoPlugin)
      .settings(
        buildInfoKeys := Seq[BuildInfoKey](
          name,
          version,
          scalaVersion,
          sbtVersion
        ),
        buildInfoPackage := pack,
        buildInfoObject := cls,
        buildInfoUsePackageAsPath := true,
        //    buildInfoOptions += BuildInfoOption.BuildTime,
        buildInfoOptions += BuildInfoOption.ToJson
      )
      .settings(buildInfoCommonSettings)

  /**
    * Add command aliases
    *
    * Usage:
    *
    * val x = project.
    *           configure( addCommandAlias( "cmd" -> "test:compile", "cc" -> ";clean;compile" ))
    *
    */
  def addCommandAliases(m: (String, String)*)(proj: Project) = {
    val s = m.map(p => addCommandAlias(p._1, p._2)).reduce(_ ++ _)
    proj.settings(s: _*)
  }

}
