import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
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
import MyEclipseTransformers._
import MyReleaseVersion._

// import MyReleaseVersion._

object BldBridgeServer {

  lazy val `bridgescorer-server`: Project = project
    .in(file("server"))
    .configure(
      commonSettings,
      buildInfo("com.github.thebridsk.bridge.server.version", "VersionServer")
    )
    .enablePlugins(WebScalaJSBundlerPlugin)
    .dependsOn(BldBridgeShared.sharedJVM)
    .dependsOn(ProjectRef(uri("utilities"), "utilities-jvm"))
    .dependsOn(BldBridgeRotation.rotationJVM % "test")
    .dependsOn(BldBrowserPages.browserpages % "test->compile")
    .settings(
      inConfig(Test)(baseAssemblySettings): _*
    )
    .settings(
      name := "bridgescorer-server",
      EclipseKeys.classpathTransformerFactories ++= Seq(
        MyEclipseTransformers.replaceRelativePath(
          "/bridgescorer-shared",
          "/bridgescorer-sharedJVM"
        ),
        MyEclipseTransformers.replaceRelativePath(
          "/bridgescorer-rotation",
          "/bridgescorer-rotationJVM"
        )
      ),
      EclipseKeys.withSource := true,
      //    mainClass in Compile := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, run) := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, packageBin) := Some("com.github.thebridsk.bridge.server.Server"),
      Compile / run / fork := true,
      EclipseKeys.classpathTransformerFactories ++= Seq(
        addDependentRunClassFolder("target/web/classes/main"),
        removeRelativePath(
          "target\\scala-" + verScalaMajorMinor + "\\resource_managed\\main"
        )
      ),
      server := {
        (run in Compile).toTask(""" --logfile "logs/server.sbt.%d.%u.log" start --store store""").value
      },
      servertemp := {
        (run in Compile).toTask(""" --logfile "logs/server.sbt.%d.%u.log" start --store temp""").value
      },
      serverlogs := {
        (run in Compile).toTask(""" --logconsolelevel=ALL start --store store""").value
      },
      // shebang the jar file.  7z and jar will no longer see it as a valid zip file.
      //    assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript)),
      //    assemblyJarName in assembly := s"${name.value}-{version.value}",
      test in assembly := {},
      test in (Test, assembly) := {}, // { val x = assembly.value },
      assemblyJarName in (assembly) := s"${name.value}-assembly-${version.value
        .replaceAll("[\\/]", "_")}.jar",
      assemblyJarName in (Test, assembly) := s"${name.value}-test-${version.value
        .replaceAll("[\\/]", "_")}.jar",
      webassembly := { val x = (assembledMappings in assembly).value },
      assembly := {
        val log = streams.value.log
        val x = (assembly).value
        val sha = Sha256.generate(x)
        log.info(s"SHA-256: ${sha}")
        x
      },
      assembly in Test := {
        val log = streams.value.log
        val x = (assembly in Test).value
        val sha = Sha256.generate(x)
        log.info(s"SHA-256: ${sha}")
        x
      },
      mainClass in Test := Some("org.scalatest.tools.Runner"),
      EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.ManagedClasses,
      //    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oG"),
      testOptions in Test += Tests.Filter { s =>
        if (s == testToRun) {
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
        "-DDefaultWebDriver=" + useBrowser,
        "-DSessionComplete=" + useBrowser,
        "-DSessionDirector=" + useBrowser,
        "-DSessionTable1=" + useBrowser,
        "-DSessionTable2=" + useBrowser,
        s"""-DUseProductionPage=${if (useFullOpt) "1" else "0"}"""
      ),
      libraryDependencies ++= bridgeScorerDeps.value,
      libraryDependencies ++= bridgeScorerServerDeps.value,
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
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", "maven", xs @ _*)
            if (!xs.isEmpty && (xs.last endsWith ".properties")) =>
          MergeStrategy.first
        case PathList("JS_DEPENDENCIES") => MergeStrategy.rename
        //      case PathList("akka", "http", xs @ _*) => MergeStrategy.first
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyExcludedJars in (Test, assembly) := {
        val log = streams.value.log
        val ccp = (fullClasspath in (Compile, assembly)).value.map {
          _.data.getName
        }
        log.info("fullClasspath in (Compile,assembly): " + ccp)
        val cp = (fullClasspath in (Test, assembly)).value
        log.info("fullClasspath in (Test,assembly): " + ccp)
        cp filter { x =>
          val rc = ccp.contains(x.data.getName)
          log.info(
            "  " + (if (rc) "Excluding " else "Using     ") + x.data.getName
          )
          rc
        }
      },
      prereqintegrationtests := {
        val x = (assembly in Compile).value
        val y = (assembly in Test).value
      },
      integrationtests := Def
        .sequential(prereqintegrationtests in Distribution, fvt in Distribution)
        .value,
      fvt := {
        val log = streams.value.log
        def getclasspath() = {
          val targetdir = (classDirectory in Compile).value + "/../"

          val assemblyjar = BridgeServer.findFile(
            targetdir + (assemblyJarName in assembly).value,
            "-SNAPSHOT.jar"
          )
          val testjar = BridgeServer.findFile(
            targetdir + (assemblyJarName in (Test, assembly)).value,
            "-SNAPSHOT.jar"
          )

          val cp = assemblyjar + java.io.File.pathSeparator + testjar
          log.info("Classpath is " + cp)
          cp
        }
        val args = "-DUseProductionPage=1" ::
          "-DToMonitorFile=logs/atestTcpMonitorTimeWait.csv" ::
          "-DUseLogFilePrefix=logs/atest" ::
          "-DDefaultWebDriver=" + useBrowser ::
          "-cp" :: getclasspath() ::
          "org.scalatest.tools.Runner" ::
          "-oD" ::
          "-s" ::
          testToRun ::
          Nil
        val inDir = baseDirectory.value
        log.info(
          s"""Running in directory ${inDir}: java ${args.mkString(" ")}"""
        )
        val rc =
          Fork.java(ForkOptions().withWorkingDirectory(Some(inDir)), args)
        if (rc != 0) throw new RuntimeException("integration tests failed")
      },
      moretests := Def
        .sequential(prereqintegrationtests in Distribution, svt in Distribution)
        .value,
      svt := {
        val log = streams.value.log
        val (assemblyJar, testJar) = {
          val targetdir = (classDirectory in Compile).value + "/../"

          val assemblyjar = BridgeServer.findFile(
            targetdir + (assemblyJarName in assembly).value,
            "-SNAPSHOT.jar"
          )
          val testjar = BridgeServer.findFile(
            targetdir + (assemblyJarName in (Test, assembly)).value,
            "-SNAPSHOT.jar"
          )

          val cp = (assemblyjar, testjar)
          log.info("Jars are " + cp)
          cp
        }
        val cp = assemblyJar + java.io.File.pathSeparator + testJar

        val server = new BridgeServer(assemblyJar)
        server.runWithServer(
          log,
          baseDirectory.value + "/logs/itestServerInTest.%u.log"
        ) {
          val jvmargs = server.getTestDefine() :::
            "-DUseProductionPage=1" ::
            "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv" ::
            "-DUseLogFilePrefix=logs/itest" ::
            "-DTestDataDirectory=" + testdataDir ::
            "-DDefaultWebDriver=" + useBrowser ::
            "-cp" :: cp ::
            "org.scalatest.tools.Runner" ::
            "-oD" ::
            "-s" ::
            moretestToRun ::
            Nil
          val inDir = baseDirectory.value
          log.info(
            s"""Running in directory ${inDir}: java ${jvmargs.mkString(" ")}"""
          )
          BridgeServer.runjava(log, jvmargs, Some(baseDirectory.value))
        }
      },
      travismoretests := Def
        .sequential(
          prereqintegrationtests in Distribution,
          travissvt in Distribution
        )
        .value,
      travissvt := {
        val log = streams.value.log
        val (assemblyJar, testJar) = {
          val targetdir = (classDirectory in Compile).value + "/../"

          val assemblyjar = BridgeServer.findFile(
            targetdir + (assemblyJarName in assembly).value,
            "-SNAPSHOT.jar"
          )
          val testjar = BridgeServer.findFile(
            targetdir + (assemblyJarName in (Test, assembly)).value,
            "-SNAPSHOT.jar"
          )

          val cp = (assemblyjar, testjar)
          log.info("Jars are " + cp)
          cp
        }
        val cp = assemblyJar + java.io.File.pathSeparator + testJar

        val server = new BridgeServer(assemblyJar)
        server.runWithServer(
          log,
          baseDirectory.value + "/logs/itestServerInTest.%u.log"
        ) {
          val jvmargs = server.getTestDefine() :::
            "-DUseProductionPage=1" ::
            "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv" ::
            "-DUseLogFilePrefix=logs/itest" ::
            "-DTestDataDirectory=" + testdataDir ::
            "-DMatchToTest=10" ::
            "-DDefaultWebDriver=" + useBrowser ::
            "-cp" :: cp ::
            "org.scalatest.tools.Runner" ::
            "-oD" ::
            "-s" ::
            travisMoretestToRun ::
            Nil
          val inDir = baseDirectory.value
          log.info(
            s"""Running in directory ${inDir}: java ${jvmargs.mkString(" ")}"""
          )
          BridgeServer.runjava(log, jvmargs, Some(baseDirectory.value))
        }
      },
      standalonetests := Def
        .sequential(fvt in Distribution, svt in Distribution)
        .value,
      disttests := Def
        .sequential(integrationtests in Distribution, moretests in Distribution)
        .value,
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

            val targetdir = (classDirectory in Compile).value + "/../"
            val assemblyjar = (assemblyJarName in assembly).value
            val testjar = (assemblyJarName in (Test, assembly)).value

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
          disttests in Distribution,
          mypublishcopy in Distribution
        )
        .value
    )

}
