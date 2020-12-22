import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitBranchPrompt
import sbtassembly.AssemblyPlugin.autoImport._
import com.typesafe.sbt.gzip.SbtGzip.autoImport._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import webscalajs.WebScalaJS.autoImport._
import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin
import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import scalafix.sbt.ScalafixPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._
import MyReleaseVersion._

object BldBridge {

  def actionAppHelp( state: State ) = {
    println(
      """
        |BridgeScoreKeeper sbt help
        |
        |Tasks on root project
        |  setOptimize            turn on scalac optimization in all projects
        |  setSemanticDB          turn on setSemanticDB for scalafix
        |  server                 run server with HTTP without help pages
        |  serverhelp             run server with HTTP with help pages
        |  updateCheck            check for updates in project and plugins
        |  checkForUpdates        check for dependency updates, does not check sbt plugins
        |  distribution:alltests  runs all tests
        |  standalonetests        run test cases using assembled jars, does not build jars
        |  distribution:travis    run build that is run in Travis-CI
        |  distribution:travis1   run build that is run in step 1 in Travis-CI
        |  distribution:travis2   run build that is run in step 2 in Travis-CI
        |  myrelease-with-defaults do a release using defaults
        |  bridgescalafix         run scalafix on all projects with semanticDB, runs bridgescorer-fullserver/test
        |Note, the following will fail:
        |  release with-defaults  does a release bumping the patch version
        |  release release-version 1.0.99 next-version 1.2.0-SNAPSHOT    set the release version and next version for release process
        |
        |Tasks on bridgescorekeeper project, with help pages
        |  serverlogs         run server with logs to stdout
        |  serverssl          run server with HTTPS
        |  test:serverhttps2  run server with HTTPS and HTTP 2
        |  allassembly        build big jar and test jar
        |  webassembly        build web files
        |
        |Tasks on bridgescorer-fullserver project, without help pages
        |  serverlogs         run server with logs to stdout
        |  serverssl          run server with HTTPS
        |  test:serverhttps2  run server with HTTPS and HTTP 2
        |  test:test          run selenium tests
        |
        |Tasks on bridgescorer-client and bridgescorer-clientapi
        |  fastOptJS::webpack   build the bundle
        |  fullOptJS::webpack   build the bundle
        |
        |Tasks on bridgescorer-server project, without app or help
        |  generatesslkeys    generate test ssl keys for https port
        |
        |Tasks on help project
        |  hugo               run hugo to generate help pages
        |  hugoserver         run hugo server on help docs
        |
        |Tasks on demo project
        |  generateDemo       generates the demo website at demo/target/demo
        |  publishDemo        publishes the demo website to thebridsk/bridgescorerdemo
        |                     this command is normally not needed, a release will publis
        |                     the website
        |
        |Tasks in any project
        |  test:testClass <clsname>...  run tests on specified class(es), wildcard * matches any number of characters
        |  scalafix           run scalafix, for help run scalafix --help
        |  scalafmt           run scalafmt
        |
        |Environment Variables that affect build
        |  UseBrowser <browser>        the browser to use in selenium tests, default is "chrome"
        |  SkipGenerateImage <bool>    skip generating images in help pages, default is false
        |  BuildProduction <bool>      build using full optimization
        |  ServerTestToRun <clsname>   test to run in fullserver when running test task
        |  TRAVIS_BUILD_NUMBER <n>     running in Travis-CI
        |  BuildForHelpOnly            if defined only tests that generate help images are run
        |  ReleaseFromBranch <branch>  current branch when release is executed, default "main"
        |  UseLogFilePrefix  <path>    prefix to use for logging when running tests
        |  ChromeNoSandbox <bool>      use --no-sandbox when starting chrome, default is false
        |  ParallelUtilsUseSerial <bool> use serial testing if true, default is false on windows and true on Mac and linux
        |  WebDriverDebug <bool>       turn on chromedriver or geckodriver debugging, default is false
        |
        |Environment Variables when using server in tests
        |  UseBridgeScorerURL              Override URL of server, default: http://localhost:8081
        |  UseBridgeScorerScheme           Override schema of servr URL, default: http
        |  UseBridgeScorerHost             Override host of server URL, default: localhost
        |  UseBridgeScorerPort             Override port of server URL, default: 8081
        |  UseWebsocketLogging             use websockets for client logging
        |  OptRemoteLogger <file>          the remote logger configuration to use
        |  OptBrowserRemoteLogging <name>  the name of the configuration to use for browsers
        |  OptIPadRemoteLogging <name>     the name of the configuration to use for iPads
        |
        |Notes:
        |  To test the build that runs in Travis-CI first set the environment variable TRAVIS_BUILD_NUMBER to a number,
        |  then invoke "sbt distribution:travis1" or "sbt distribution:travis2"
        |
        |  To make a release, checkout the main branch, be sure the submodules are at the correct commit and that
        |  commit has been pushed to GitHub.  Then invoke "sbt release".  The release process will create a branch
        |  called "release" from the HEAD, then modifies version.sbt, and commits this, tags it with the version number
        |  and runs "distribution:disttest".  If the build succeeds then version.sbt is updated to the next snapshot
        |  version, and committed.  The "release" branch and the version tag should be pushed to GitHub, and a Pull Request
        |  should be made from the "release" branch to the "main" branch.  The commit with the version tag will be
        |  built by Travis-CI and the bridgescorekeeper jar file is added as an artifact to the release in GitHub.
        |
        |Scalafix
        |  To run scalafix on all projects, run
        |
        |     setSemanticDB
        |     project bridgescorer
        |     scalafix
        |     test:scalafix
        |     project {utilities}utilities
        |     setSemanticDB
        |     scalafix
        |     test:scalafix
        |
        |  Note: running bridgescorekeeper/test:scalafix will cause bridgescorer-fullserver/test to execute
        |
        |Scalafmt
        |  To run scalafmt on all projects, run
        |
        |     project bridgescorer
        |     scalafmt
        |     test:scalafmt
        |     project {utilities}utilities
        |     scalafmt
        |     test:scalafmt
        |
        |""".stripMargin
    )
    state
  }

  val apphelp = Command.command(
    "apphelp",
    "show help for app tasks",
    "show help for app tasks"
  )(actionAppHelp _) // "shows help for sbt tasks for BridgeScoreKeeper"

  val setSemanticDB = Command.command(
    "setSemanticDB",
    "turn on setSemanticDB",
    "turn on setSemanticDB"
  ) { state: State =>
    val extracted = Project extract state
    import extracted._
    println("Turning on SemanticDB")
    appendWithoutSession(
      semanticdbEnabled in ThisBuild := true,
      // structure.allProjectRefs.map{ p =>
      //   semanticdbEnabled in p := true
      // },
      state
    )
  }

  def init = inThisBuild(
    List(
      scalaVersion := verScalaVersion,
      scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(verScalaVersion),
      semanticdbEnabled := false,
      semanticdbVersion := scalafixSemanticdb.revision,

      Global / excludeLintKeys ++= Set(
        scalafixScalaBinaryVersion,
        cleanKeepGlobs in BldBridgeClient.`bridgescorer-client`,
        cleanKeepGlobs in BldBridgeClientApi.`bridgescorer-clientapi`,
      )
    )

  )

  val setOptimize = Command.command(
    "setOptimize",
    "turn on scalac optimization for all projects",
    "turn on scalac optimization for all projects"
  )( turnOnOptimize _)

  def turnOnOptimize( state: State ) = {
    val extracted = Project extract state
    import extracted._
    println("Turning on optimization in all projects")
    appendWithoutSession(
      structure.allProjectRefs.map{ p =>
        scalacOptions in p ++= Seq(
          "-opt:l:method",
          // "-opt:l:inline",
          // "-opt-inline-from:**"
        )
      },
      state
    )
  }

  implicit class WrapState( val state: State ) extends AnyVal {
    def runAggregated[T]( key: TaskKey[T] ) = {
      releaseStepTaskAggregated(key)(state)
    }
    def run[T]( key: TaskKey[T] ) = {
      releaseStepTask(key)(state)
    }
    def runWithInput[T](key: InputKey[T], input: String = "") = {
      releaseStepInputTask(key,input)
    }
    def run( command: String ) = {
      releaseStepCommandAndRemaining(command)(state)
    }
    def run(command: Command, input: String = "") = {
      releaseStepCommand(command, input)(state)
    }
  }

  val updateCheck = Command.command(
    "updateCheck",
    "Check for updates",
    "Check for updates"
  ) { state =>
    state
      .run(checkForUpdates)
      .run("reload plugins")
      .run(dependencyUpdates)
      .run("reload return")
    // state
      .run( "project {utilities}utilities" )
      .run( "reload plugins")
      .run( dependencyUpdates )
//      .run("reload return")
    state
  }

  val bridge_scalafix = Command.command(
    "bridgescalafix",
    "Run scalafix on all projects",
    "Run scalafix on all projects, will run setSemanticDB, will run bridgescorer-fullserver/test"
  ) { state =>
    state
      .run(setSemanticDB)
      .run("scalafix")
      .run( "project {utilities}utilities" )
      .run(setSemanticDB)
      .run("scalafix")
    state
  }

  lazy val releaseOptimize = ReleaseStep(
    action = turnOnOptimize
  )

  lazy val releaseCheck = { st: State =>
    Project.extract(st).runTask(publishdir, st) match {
      case (newst, Some(dir)) =>
        if (!dir.isDirectory())
          throw new RuntimeException(
            "failed check for release, DistributionDirectory does not exist: " + dir
          )
        newst
      case (newst, None) =>
        throw new RuntimeException(
          "failed check for release, DistributionDirectory not defined"
        )
        newst
      case _ =>
        throw new RuntimeException("failed check for release, unknown error")
        st
    }
  }

  import ReleaseTransformations.{setReleaseVersion => _, setNextVersion => _, _}

  lazy val publishRelease = ReleaseStep(
    check = releaseCheck, // upfront check
    action = releaseStepTaskAggregated(mydist in Distribution in bridgescorer) // publish release notes
  )

  lazy val bridgescorer: Project = project
    .in(file("."))
    .aggregate(
      BldBridgeShared.sharedJVM,
      BldBridgeShared.sharedJS,
      BldBridgeRotation.rotationJS,
      BldMaterialUI.materialui,
      BldBridgeClientCommon.`bridgescorer-clientcommon`,
      BldBridgeClient.`bridgescorer-client`,
      BldBridgeClientApi.`bridgescorer-clientapi`,
      BldBridgeServer.`bridgescorer-server`,
      BldBridgeFullServer.`bridgescorer-fullserver`,
      BldBridgeRotation.rotationJVM,
      BldBrowserPages.browserpages,
      BldColor.colorJS,
      BldColor.colorJVM,
      BldBridgeScoreKeeper.bridgescorekeeper,
      BldBridgeDemo.demo
    )
    .configure(commonSettings)
    .settings(
      name := "bridgescorer",
      publish := {},
      publishLocal := {},
      scalaVersion := verScalaVersion,

      aggregate in assembly := false,
      aggregate in webassembly := false,
      aggregate in integrationtests in Distribution := false,
      aggregate in prereqintegrationtests in Distribution := false,
      aggregate in disttests in Distribution := false,
      aggregate in fvt in Distribution := false,
      aggregate in svt in Distribution := false,
      aggregate in moretests in Distribution := false,
      aggregate in travissvt in Distribution := false,
      aggregate in travismoretests in Distribution := false,
      aggregate in integrationtests in Distribution := false,
      aggregate in mypublish in Distribution := false,
      aggregate in mypublishcopy in Distribution := false,
      aggregate in server := false,
      aggregate in serverlogs := false,
      aggregate in testClass := false,

      server := { (server in BldBridgeFullServer.`bridgescorer-fullserver`).value },
      serverhelp := { (serverhelp in BldBridgeScoreKeeper.bridgescorekeeper).value },
      serverlogs := { (serverlogs in BldBridgeScoreKeeper.bridgescorekeeper).value },

      commands ++= Seq( apphelp, setOptimize, updateCheck, releaseWithDefaults, setSemanticDB, bridge_scalafix )
    ).
    settings(
      checkForUpdates := Def
        .sequential(
          MyNpm.checkForNpmUpdates in Compile in BldBridgeClient.`bridgescorer-client`,
          MyNpm.checkForNpmUpdates in Test in BldBridgeClient.`bridgescorer-client`,
          MyNpm.checkForNpmUpdates in Compile in BldBridgeClientApi.`bridgescorer-clientapi`,
          // MyNpm.checkForNpmUpdates in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          dependencyUpdates.all(allProjects),
          dependencyUpdates,
          dependencyUpdates in utilities
        )
        .value,
      alltests := Def
        .sequential(
          mydist in Distribution in utilities,
          mydistnoclean,
          disttests in Distribution in BldBridgeScoreKeeper.bridgescorekeeper,
          generateDemo in BldBridgeDemo.demo,
          test in Test in BldBridgeDemo.demo,
        )
        .value,
      travis := Def
        .sequential(
          travis1,
          travismoretests in Distribution in BldBridgeScoreKeeper.bridgescorekeeper,
          generateDemo in BldBridgeDemo.demo,
          test in Test in BldBridgeDemo.demo,
        )
        .value,
      travis1p := {
          // val x1 = (travis in Distribution in utilities).value
          val x2 = (test in Test in BldBridgeRotation.rotationJVM).value
          val x3 = (test in Test in BldBridgeRotation.rotationJS).value
          val x4 = (test in Test in BldColor.colorJVM).value
          val x5 = (test in Test in BldBridgeShared.sharedJVM).value
          val x6 = (test in Test in BldBridgeShared.sharedJS).value
          val x7 = (test in Test in BldBridgeClientCommon.`bridgescorer-clientcommon`).value
          val x8 = (test in Test in BldBridgeClient.`bridgescorer-client`).value
          // val x9 = (test in Test in BldBridgeClientApi.`bridgescorer-clientapi`).value
          val x10 = (test in Test in BldBridgeServer.`bridgescorer-server`).value
          val x11 = (test in Test in BldBridgeFullServer.`bridgescorer-fullserver`).value
      },
      travis1 := Def
        .sequential(
          travis in Distribution in utilities,
          travis1p,
        ).value,
      travis2 := Def
        .sequential(
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`,
          generateSwagger in BldBridgeFullServer.`bridgescorer-fullserver`,
          prereqintegrationtests in BldBridgeScoreKeeper.bridgescorekeeper,
          generateDemo in BldBridgeDemo.demo,
          test in Test in BldBridgeDemo.demo,
          travismoretests in Distribution in BldBridgeScoreKeeper.bridgescorekeeper,
        )
        .value,
      mydistnoclean := {
          // val x1 = (mydist in Distribution in utilities).value
          val x2 = (test in Test in BldBrowserPages.browserpages).value
          val x3 = (fastOptJS in Compile in BldBridgeClient.`bridgescorer-client`).value
          val x4 = (fullOptJS in Compile in BldBridgeClient.`bridgescorer-client`).value
          val x5 = travis1p.value
        },
      mydist := Def
        .sequential(
          myclean,
          alltests,
          mypublish in Distribution,
        )
        .value,
      myclean := {
        val x = clean.all(allProjects).value
      },

      mytest := Def
        .sequential(
          allassembly in BldBridgeScoreKeeper.bridgescorekeeper,
          test in Test in BldColor.colorJVM,
          test in Test in BldBridgeClientCommon.`bridgescorer-clientcommon`,
          test in Test in BldBridgeClient.`bridgescorer-client`,
          // test in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          test in Test in BldBridgeServer.`bridgescorer-server`,
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`,
          test in Test in BldBridgeScoreKeeper.bridgescorekeeper,
        )
        .value,
      releaseUseGlobalVersion := false,
//
// need to update release tag and comment
//
      releaseTagName := getTagFromVersion( git.baseVersion.value ),
      releaseTagComment := s"Releasing ${git.baseVersion.value}",
      releaseCommitMessage := s"Setting version to ${git.baseVersion.value}",
      releaseNextCommitMessage := s"Setting version to ${git.baseVersion.value}",

      // This release process will only work if the command "release with-defaults" or
      // "myrelease-with-defaults" is used.
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        gitMakeReleaseBranch,
        inquireVersions,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        recalculateVersion,
        releaseOptimize,
        publishRelease,  // runs a clean build and test
        setNextVersion,
        commitNextVersion,
        gitPushReleaseBranch,
        gitPushReleaseTag
      ),

      publishdir := {
        // returns an Option[File]

        val log = streams.value.log

        import java.io.File
        sys.props("user.home") match {
          case homedir if (homedir != null) =>
            val configfile = new File(homedir, "bridgescorer/config.properties")
            if (configfile.exists()) {
              import java.util.Properties
              import java.io.InputStreamReader
              import java.io.FileInputStream
              val props = new Properties
              props.load(
                new InputStreamReader(new FileInputStream(configfile), "UTF8")
              )
              val dd = props.getProperty("DistributionDirectory")
              if (dd != null) {
                import java.io.File

                val distdir = dd.replace('\\', '/')
                val f = new File(distdir)
                if (f.isDirectory()) {
                  log.info("Publishing to " + f)
                  Some(f)
                } else {
                  throw new RuntimeException(
                    "DistributionDirectory directory does not exist: " + f
                  )
                  None
                }
              } else {
                throw new RuntimeException(
                  "DistributionDirectory property does not exist in file ~/bridgescorer/config.properties"
                )
                None
              }
            } else {
              throw new RuntimeException(
                "file ~/bridgescorer/config.properties does not exist"
              )
              None
            }
          case _ =>
            throw new RuntimeException("Home directory not set")
            None
        }
      },
      mypublishcopy := {

        val log = streams.value.log

        val dd = publishdir.value
        dd match {
          case Some(distdir) =>
            import java.nio.file.Path
            import java.nio.file.StandardCopyOption
            import java.nio.file.Files

            log.info("Publishing to " + distdir)

            val targetdir = (crossTarget in BldBridgeScoreKeeper.bridgescorekeeper in Compile).value
            val assemblyjar = (assemblyJarName in BldBridgeScoreKeeper.bridgescorekeeper in assembly).value
            val testjar = (assemblyJarName in BldBridgeScoreKeeper.bridgescorekeeper in (Test, assembly)).value

            val sourceassemblyjar = new File(targetdir, assemblyjar)
            val targetassemblyjar = new File(distdir, assemblyjar)
            val sourcetestjar = new File(targetdir, testjar)
            val targettestjar = new File(distdir, testjar)

            IO.listFiles(distdir, GlobFilter("*.jar")).foreach { jar =>
              {
                log.info("Moving jar to save: " + jar)
                IO.move(
                  jar,
                  new File(new File(jar.getParentFile, "save"), jar.getName)
                )
              }
            }

            log.info("Publishing " + assemblyjar + " to " + distdir)
            Files.copy(
              sourceassemblyjar.toPath,
              targetassemblyjar.toPath,
              StandardCopyOption.REPLACE_EXISTING
            )
            log.info("Publishing " + testjar + " to " + distdir)
            Files.copy(
              sourcetestjar.toPath,
              targettestjar.toPath,
              StandardCopyOption.REPLACE_EXISTING
            )

            log.info("Published to " + distdir)
          case None =>
            throw new RuntimeException("DistributionDirectory is not set")
        }

      },
      mypublish := (mypublishcopy in Distribution).value

    )
    .enablePlugins(GitVersioning, GitBranchPrompt)

}
