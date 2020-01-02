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

import BldDependencies._
import BldCommonSettings._
import BldVersion._
import MyReleaseVersion._

object BldBridge {

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
      BldBridgeScoreKeeper.bridgescorekeeper
    )
    .settings(
      name := "bridgescorer",
      publish := {},
      publishLocal := {},

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
    ).
    settings(
      checkForUpdates := Def
        .sequential(
          MyNpm.checkForNpmUpdates in Compile in BldBridgeClient.`bridgescorer-client`,
          MyNpm.checkForNpmUpdates in Test in BldBridgeClient.`bridgescorer-client`,
          MyNpm.checkForNpmUpdates in Compile in BldBridgeClientApi.`bridgescorer-clientapi`,
//          MyNpm.checkForNpmUpdates in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          //  MyNpm.checkForNpmUpdates.all(bridgescorerAllProjects),
          dependencyUpdates.all(bridgescorerAllProjects),
          dependencyUpdates
        )
        .value,
      alltests := Def
        .sequential(
          mydist in Distribution in utilities,
          test in Test in BldBrowserPages.browserpages,
//                       fastOptJS in Compile in `bridgescorer-client`,
//                       fullOptJS in Compile in `bridgescorer-client`,
//                       fastOptJS in Test in `bridgescorer-client`,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
          test in Test in BldBridgeRotation.rotationJVM,
          test in Test in BldBridgeRotation.rotationJS,
          test in Test in BldColor.colorJVM,
          test in Test in BldBridgeClientCommon.`bridgescorer-clientcommon`,
          test in Test in BldBridgeClient.`bridgescorer-client`,
//          test in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          test in Test in BldBridgeServer.`bridgescorer-server`,
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`,
//                       hugo in help,
          disttests in Distribution in BldBridgeScoreKeeper.bridgescorekeeper,
        )
        .value,
      travis := Def
        .sequential(
          travis in Distribution in utilities,
//                       fastOptJS in Compile in `bridgescorer-client`,
//                       fullOptJS in Compile in `bridgescorer-client`,
//                       fastOptJS in Test in `bridgescorer-client`,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
          test in Test in BldBridgeRotation.rotationJVM,
          test in Test in BldBridgeRotation.rotationJS,
          test in Test in BldColor.colorJVM,
          test in Test in BldBridgeClientCommon.`bridgescorer-clientcommon`,
          test in Test in BldBridgeClient.`bridgescorer-client`,
//          test in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          test in Test in BldBridgeServer.`bridgescorer-server`,
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`,
//                       hugo in help,
          travismoretests in Distribution in BldBridgeScoreKeeper.bridgescorekeeper,
        )
        .value,
      travis1 := Def
        .sequential(
          travis in Distribution in utilities,
//                       fastOptJS in Compile in `bridgescorer-client`,
//                       fullOptJS in Compile in `bridgescorer-client`,
//                       fastOptJS in Test in `bridgescorer-client`,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
          test in Test in BldBridgeRotation.rotationJVM,
          test in Test in BldBridgeRotation.rotationJS,
          test in Test in BldColor.colorJVM,
          test in Test in BldBridgeClientCommon.`bridgescorer-clientcommon`,
          test in Test in BldBridgeClient.`bridgescorer-client`,
//          test in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          test in Test in BldBridgeServer.`bridgescorer-server`,
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`
        )
        .value,
      travis2 := Def
        .sequential(
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`,
          travismoretests in Distribution in BldBridgeScoreKeeper.bridgescorekeeper,
        )
        .value,
      mydist := Def
        .sequential(
          clean.all(bridgescorerAllProjects),
          clean.all(utilitiesAllProjects),
          mydist in Distribution in utilities,
          fastOptJS in Compile in BldBridgeClient.`bridgescorer-client`,
          fullOptJS in Compile in BldBridgeClient.`bridgescorer-client`,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
//                       assembly in `bridgescorer-client`,
//                       assembly in Test in `bridgescorer-client`,
          test in Test in BldBridgeRotation.rotationJVM,
          test in Test in BldBridgeRotation.rotationJS,
          test in Test in BldColor.colorJVM,
          test in Test in BldBridgeClientCommon.`bridgescorer-clientcommon`,
          test in Test in BldBridgeClient.`bridgescorer-client`,
//          test in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          test in Test in BldBridgeServer.`bridgescorer-server`,
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`,
//                       hugo in help,
          mypublish in Distribution,
        )
        .value,
      myclean := Def
        .sequential(
          clean.all(bridgescorerAllProjects),
          clean.all(utilitiesAllProjects)
        )
        .value,
      mytest := Def
        .sequential(
//                       fastOptJS in Compile in `bridgescorer-client`,
//                       fullOptJS in Compile in `bridgescorer-client`,
          allassembly in BldBridgeScoreKeeper.bridgescorekeeper,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
          test in Test in BldColor.colorJVM,
          test in Test in BldBridgeClientCommon.`bridgescorer-clientcommon`,
          test in Test in BldBridgeClient.`bridgescorer-client`,
//          test in Test in BldBridgeClientApi.`bridgescorer-clientapi`,
          test in Test in BldBridgeServer.`bridgescorer-server`,
          test in Test in BldBridgeFullServer.`bridgescorer-fullserver`,
          test in Test in BldBridgeScoreKeeper.bridgescorekeeper,
        )
        .value,
      releaseUseGlobalVersion := false,
//
// need to update release tag and comment
//
      releaseTagName := "v" + git.baseVersion.value,
      releaseTagComment := s"Releasing ${git.baseVersion.value}",
      releaseCommitMessage := s"Setting version to ${git.baseVersion.value}",
      releaseNextCommitMessage := s"Setting version to ${git.baseVersion.value}",
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies, // : ReleaseStep
        gitMakeReleaseBranch,
        inquireVersions, // : ReleaseStep
      //  runTest,                                // : ReleaseStep
        setReleaseVersion, // : ReleaseStep
        commitReleaseVersion, // : ReleaseStep, performs the initial git checks
        tagRelease, // : ReleaseStep
        recalculateVersion, // : ReleaseStep
        publishRelease, // : ReleaseStep, custom
        setNextVersion, // : ReleaseStep
        commitNextVersion // : ReleaseStep
      //  gitMergeReleaseMaster,
      //  recalculateVersion,                     // : ReleaseStep
      //  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
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
      mypublish := Def
        .sequential(
          disttests in BldBridgeScoreKeeper.bridgescorekeeper in Distribution,
          mypublishcopy in Distribution
        )
        .value


    )
    .enablePlugins(GitVersioning, GitBranchPrompt)

}
