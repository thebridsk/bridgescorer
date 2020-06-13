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

object BldBridgeScoreKeeper {

  val cleanAssemblyCache = taskKey[Unit]("Cleans assembly cache of old builds") in Distribution

  lazy val bridgescorekeeper: Project = project
    .in(file("bridgescorekeeper"))
    .configure( commonSettings, buildInfo("com.github.thebridsk.bridge.bridgescorer.version", "VersionBridgeScorer"))
    .dependsOn(BldBridgeFullServer.`bridgescorer-fullserver` % "test->test;compile->compile")
    .dependsOn(ProjectRef(uri("utilities"), "utilities-jvm"))
    .enablePlugins(WebScalaJSBundlerPlugin)
    .settings(
      inConfig(Test)(baseAssemblySettings): _*
    )
    .settings(
      name := "bridgescorekeeper",
      publish := {},
      publishLocal := {},
//    mainClass in Compile := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, run) := Some("com.github.thebridsk.bridge.server.Server"),
      mainClass in (Compile, packageBin) := Some("com.github.thebridsk.bridge.server.Server"),
      fork := true,

      // testOptions in Test := Seq(),

      serverhelp := {
        (run in Compile).toTask(""" --logfile "../server/logs/serverhelp.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs""").value
      },
      serverssl in Test := {
        (run in Compile).toTask(""" --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs --certificate ../server/key/examplebridgescorekeeper.p12 --certpassword abcdef --https 8443""").value
      },
      serverhttps2 in Test := {
        (runMain in Test).toTask(""" com.github.thebridsk.bridge.server.Server --logfile "../server/logs/server.sbt.%d.%u.log" start --cache 0s --store ../server/store --diagnostics ../server/logs --certificate ../server/key/examplebridgescorekeeper.p12 --certpassword abcdef --https 8443 --http2""").value
      },

      serverlogs := {
        (run in Compile).toTask(""" --logconsolelevel=ALL start --cache 0s --store ../server/store""").value
      },

      test in assembly := {}, // test in (`bridgescorer-fullserver`, Test),
      test in (Test, assembly) := {}, // { val x = assembly.value },
      testOptions in Test += Tests.Filter { s =>
        if (s == "com.github.thebridsk.bridge.test.selenium.integrationtest.TravisIntegrationTests") {
          println("Using Test:    " + s)
          true
        } else {
          println(s"Ignoring Test: $s, looking for $testToRun");
          false
        }
      },

      assemblyJarName in (assembly) := s"${name.value}-server-${version.value
        .replaceAll("[\\/]", "_")}.jar",
      assemblyJarName in (Test, assembly) := s"${name.value}-test-${version.value
        .replaceAll("[\\/]", "_")}.jar",
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
      helptask := {
        val depend = (hugoWithTest in BldBridgeHelp.help).value
        val rootdir = (target in BldBridgeHelp.help).value
        val helpdir = new File(rootdir, "help")
        val prefix = rootdir.toString.length + 1
        helpdir.allPaths.pair(f => Some(f.toString.substring(prefix)))

      },
      npmAssets := {
        helptask.value
      },
//      includeFilter in gzip := "*.html" || "*.css" || "*.js",
      scalaJSProjects := Seq(),
      pipelineStages in Assets := (if (onlyBuildDebug) Seq()
                                   else
                                     Seq(scalaJSProd)) ++ Seq(scalaJSDev, gzip),

      cleanAssemblyCache in assembly := {
        cleanCacheDir((streams in assemblyOption).value)
      },

      cleanAssemblyCache in (Test, assembly) := {
        cleanCacheDir((streams in (Test,assemblyOption)).value)
      },

      assembly := (assembly dependsOn (cleanAssemblyCache in assembly)).value,
      assembly in Test := ((assembly in Test) dependsOn (cleanAssemblyCache in (Test,assembly))).value,

      assemblyMergeStrategy in assembly := {
        case PathList(
            "META-INF",
            "resources",
            "webjars",
            "bridgescorer",
            xs @ _*
            )
            if (!xs.isEmpty && patternFastopt.findFirstIn(xs.last).isDefined) =>
          MergeStrategy.discard
        case PathList(
            "META-INF",
            "resources",
            "webjars",
            "bridgescorer-fullserver",
            xs @ _*
            )
            if (!xs.isEmpty && patternFastopt.findFirstIn(xs.last).isDefined) =>
          MergeStrategy.discard
        case PathList(
            "META-INF",
            "resources",
            "webjars",
            "bridgescorer-fullserver",
            ver,
            dir,
            xs @ _*
            )
            if (!xs.isEmpty && patternSourceDir.pattern
              .matcher(dir)
              .matches && (xs.last endsWith ".scala")) =>
          MergeStrategy.discard
        case PathList("META-INF", "maven", xs @ _*)
            if (!xs.isEmpty && (xs.last endsWith ".properties")) =>
          MergeStrategy.first
        case PathList("JS_DEPENDENCIES") =>
          MergeStrategy.rename
        case PathList("module-info.class") =>
          MergeStrategy.rename
        case PathList("license") =>
          MergeStrategy.rename
        case PathList("META-INF", "versions", "9", "module-info.class") =>
          MergeStrategy.rename
        case PathList("META-INF", "versions", "9") =>
          // selenium jar files have a file with this name,
          // that causes unzip errors with files in that directory
          MergeStrategy.discard
//      case PathList("akka", "http", xs @ _*) => MergeStrategy.first
        case PathList(
            "META-INF",
            "resources",
            "webjars",
            "bridgescorer",
            version,
            "lib",
            "bridgescorer-fullserver",
            rest @ _*
            ) =>
          MergeStrategy.discard
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyMergeStrategy in (Test, assembly) := {
        case PathList("META-INF", "maven", xs @ _*)
            if (!xs.isEmpty && (xs.last endsWith ".properties")) =>
          MergeStrategy.first
        case PathList("JS_DEPENDENCIES")   => MergeStrategy.rename
        case PathList("module-info.class") => MergeStrategy.rename
        case PathList("license", xs @ _*) => MergeStrategy.discard
        case PathList("META-INF", "versions", "9", "module-info.class") =>
          MergeStrategy.rename
        case PathList("META-INF", "versions", "9") =>
          // selenium jar files have a file with this name,
          // that causes unzip errors with files in that directory
          MergeStrategy.discard
//      case PathList("akka", "http", xs @ _*) => MergeStrategy.first
        case PathList(
            "META-INF",
            "resources",
            "webjars",
            "bridgescorer",
            version,
            "lib",
            "bridgescorer-fullserver",
            rest @ _*
            ) =>
          MergeStrategy.discard
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
      mainClass in Test := Some("org.scalatest.tools.Runner"),
      allassembly := {
        val x = assembly.value
        val y = (assembly in Test).value
      },
      // want to run bridgescorekeeper/*:assembly::assembledMappings
      webassembly := { val x = (assembledMappings in assembly).value },
      dependencyUpdates := {
        val x = dependencyUpdates.value
        val z = dependencyUpdates.all(utilitiesAllProjects).value
      },
      clean := {
        val c = clean.value
        val ch = (clean in BldBridgeHelp.help).value
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
          val (projjar,testjar) = BridgeServer.findBridgeJars(
                                    (crossTarget in Compile).value,
                                    (assemblyJarName in assembly).value,
                                    (assemblyJarName in (Test, assembly)).value
                                  )
          val cp = projjar + java.io.File.pathSeparator + testjar
          log.info("Classpath is " + cp)
          cp
        }
        val args =
          "-Xmx4096M" ::
          "-DUseProductionPage=1" ::
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
        log.info(s"""Running in directory ${inDir}: java ${args
          .mkString(" ")}""")
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
          val cp = BridgeServer.findBridgeJars(
                                  (crossTarget in Compile).value,
                                  (assemblyJarName in assembly).value,
                                  (assemblyJarName in (Test, assembly)).value
                                 )
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
            "-Xmx4096M" ::
            "-DUseProductionPage=1" ::
            "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv" ::
            "-DUseLogFilePrefix=logs/itest" ::
            "-DTestDataDirectory=" + itestdataDir ::
            "-DDefaultWebDriver=" + useBrowser ::
            "-cp" :: cp ::
            "org.scalatest.tools.Runner" ::
            "-oD" ::
            "-s" ::
            imoretestToRun ::
            Nil
          val inDir = baseDirectory.value
          log.info(s"""Running in directory ${inDir}: java ${jvmargs
            .mkString(" ")}""")
          BridgeServer.runjava(log, jvmargs, Some(baseDirectory.value))
        }
      },
      nsvt := {
        val log = streams.value.log
        import complete.DefaultParsers._
        val args: Seq[String] = spaceDelimited("<n>").parsed

        val n = if (args.isEmpty) 1
                else if (args.length == 1) {
                  try {
                    args(0).toInt
                  } catch {
                    case _: NumberFormatException =>
                      throw new Error(s"Must specify an integer: ${args}")
                    case x: Exception =>
                      throw new Error(s"Unexpected exception parsing ${args}", x)
                  }
                } else {
                  throw new Error(s"Too many arguments specified: ${args}")
                }
        val ntimes = if (n<1) 1 else n

        val (assemblyJar, testJar) = {
          val cp = BridgeServer.findBridgeJars(
                                  (crossTarget in Compile).value,
                                  (assemblyJarName in assembly).value,
                                  (assemblyJarName in (Test, assembly)).value
                                 )
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
            "-Xmx4096M" ::
            "-DUseProductionPage=1" ::
            "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv" ::
            "-DUseLogFilePrefix=logs/itest" ::
            "-DTestDataDirectory=" + itestdataDir ::
            "-DDefaultWebDriver=" + useBrowser ::
            "-cp" :: cp ::
            "org.scalatest.tools.Runner" ::
            "-oD" ::
            "-s" ::
            imoretestToRun ::
            Nil
          val inDir = baseDirectory.value
          log.info(s"""Running ${ntimes} times in directory ${inDir}: java ${jvmargs
            .mkString(" ")}""")
          for (i <- 1 to ntimes) BridgeServer.runjava(log, jvmargs, Some(baseDirectory.value))
        }
      },

      travismoretests := Def
        .sequential(
          prereqintegrationtests in Distribution,
          travissvt in Distribution,
          ssltests in Distribution
        )
        .value,
      travissvt := {
        val log = streams.value.log
        val (assemblyJar, testJar) = {
          val cp = BridgeServer.findBridgeJars(
                                  (crossTarget in Compile).value,
                                  (assemblyJarName in assembly).value,
                                  (assemblyJarName in (Test, assembly)).value
                                 )
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
            "-Xmx4096M" ::
            "-DUseProductionPage=1" ::
            "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv" ::
            "-DUseLogFilePrefix=logs/itest" ::
            "-DTestDataDirectory=" + itestdataDir ::
            "-DMatchToTest=10" ::
            "-DDefaultWebDriver=" + useBrowser ::
            "-cp" :: cp ::
            "org.scalatest.tools.Runner" ::
            "-oD" ::
            "-s" ::
            itravisMoretestToRun ::
            Nil
          val inDir = baseDirectory.value
          log.info(s"""Running in directory ${inDir}: java ${jvmargs
            .mkString(" ")}""")
          BridgeServer.runjava(log, jvmargs, Some(baseDirectory.value))
        }
      },
      standalonetests := Def
        .sequential(fvt in Distribution, svt in Distribution)
        .value,
      disttests := Def
        .sequential(integrationtests in Distribution, moretests in Distribution, ssltests in Distribution)
        .value,

      onlyssltests in Distribution := {
        val log = streams.value.log
        val (assemblyJar, testJar) = {
          val cp = BridgeServer.findBridgeJars(
                                  (crossTarget in Compile).value,
                                  (assemblyJarName in assembly).value,
                                  (assemblyJarName in (Test, assembly)).value
                                 )
          log.info("Jars are " + cp)
          cp
        }
        val cp = assemblyJar + java.io.File.pathSeparator + testJar

        val server = new BridgeServer(assemblyJar)
        val jvmargs = server.getTestDefine() :::
          "-Xmx4096M" ::
          "-DUseProductionPage=1" ::
          "-DToMonitorFile=logs/ssltestTcpMonitorTimeWait.csv" ::
          "-DUseLogFilePrefix=logs/ssltest" ::
          "-DTestDataDirectory=" + itestdataDir ::
          "-DDefaultWebDriver=" + useBrowser ::
          "-cp" :: cp ::
          "org.scalatest.tools.Runner" ::
          "-oD" ::
          "-s" ::
          ssltestToRun ::
          Nil
        val inDir = baseDirectory.value

        val env = Map( "BridgeScoreKeeperJar" -> assemblyJar )
        BridgeServer.runjava(log, jvmargs, Some(baseDirectory.value), Some(env))

      },

      ssltests in Distribution := Def
        .sequential( prereqintegrationtests in Distribution, onlyssltests in Distribution )
        .value,
    )

    def cleanCacheDir( stream: sbt.Keys.TaskStreams ) = {

      // the following is a hack.  The assembly caches the jar files,
      // but it doesn't erase old ones.  This means for bridgescorer and
      // bridgescorer-fullserver we get multiple version in the cache which
      // end up in the assembly.jar file.

      println(s"Cleaning assembly cache for bridgescorer, cache directory is ${stream.cacheDirectory}")

      val files = ( (stream.cacheDirectory ** "webjars" / "bridgescorekeeper") +++ (stream.cacheDirectory ** "webjars" / "bridgescorer-fullserver")).get
      println(s"Deleting bridgescorer cached files in assembly, directories to delete ${files.mkString("\n  ","\n  ","\n")}")
      files.foreach { f => deleteDir(f) }

    }

    def deleteDir( dir: File ): Unit = {
      //          println(s"Deleting ${dir}")
      if (dir.isDirectory) {
        dir.listFiles.foreach { f =>
          deleteDir(f)
        }
      }
      (1 to 3).find { i =>
        val rc = dir.delete
        if (!rc) {
          Thread.sleep( 500L )
        }
        rc
      }.getOrElse {
        println(s"Error deleting ${dir}")
      }
    }

}
