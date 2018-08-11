//
// To only build the debug version of the client code,
// define the environment variable "OnlyBuildDebug"
//
// To specify the browser to use for tests, define the
// environment variable "UseBrowser" to the browser to use.  Default: chrome
// supported browsers: chrome, chromeheadless, safari, firefox, edge, ie
//
// To check for updates, npm and maven
//   checkForUpdates
// To build just the bundle.js and copy them to `bridgescorer-server` project
//   webassembly
//   or
//   bridgescorer-server/*:assembly::assembledMappings
// To build all assembly
//   allassembly
//   or
//   assembly test:assembly
// To run unit tests
//   distribution:mytest
// To run all tests
//   distribution:alltests
// To make a new release
//   release
// To run standalone tests using already built jar files
//   bridgescorer-server/distribution:standalonetests
//   bridgescorer-server/distribution:fvt
//   bridgescorer-server/distribution:svt
//
// When testing help screens, this will only run the test case that generates images for help
//   set BUILDFORHELPONLY=true
//   sbt webassembly

import Dependencies._

import sbtcrossproject.{crossProject, CrossType}

import sbtassembly.AssemblyPlugin.defaultShellScript

import MyEclipseTransformers._

import scala.language.postfixOps

enablePlugins(GitVersioning, GitBranchPrompt)
EclipseKeys.skipParents in ThisBuild := false

lazy val useBrowser = sys.props.get("UseBrowser").
                       orElse(sys.env.get("UseBrowser")).
                       getOrElse("chrome")

lazy val onlyBuildDebug = sys.props.get("OnlyBuildDebug").
                       orElse(sys.env.get("OnlyBuildDebug")).
                       map( s => s.toBoolean ).
                       getOrElse(false)

//
// Debugging deprecation and feature warnings
//
// Through the sbt console...
//
//    reload plugins
//    set scalacOptions ++= Seq( "-unchecked", "-deprecation", "-feature" )
//    session save
//    reload return

lazy val inTravis = sys.props.get("TRAVIS_BUILD_NUMBER").
                     orElse(sys.env.get("TRAVIS_BUILD_NUMBER")).
                     isDefined
                     
val buildForHelpOnly = sys.props.get("BUILDFORHELPONLY").
                         orElse(sys.env.get("BUILDFORHELPONLY")).
                           isDefined

val testToRunNotTravis = "com.example.test.AllSuites"
val testToRunBuildForHelpOnly = "com.example.test.selenium.DuplicateTestPages" 
val testToRunInTravis = "com.example.test.TravisAllSuites"

lazy val testToRun = if (inTravis) {
  println( s"Running in Travis CI, tests to run: ${testToRunInTravis}" )
  testToRunInTravis
} else {
  println( s"Not running in Travis CI, tests to run: ${testToRunNotTravis}" )
  if (buildForHelpOnly) {
    testToRunBuildForHelpOnly
  } else {
    testToRunNotTravis
  }
}

val moretestToRun = "com.example.test.selenium.IntegrationTests"
val travisMoretestToRun = "com.example.test.selenium.TravisIntegrationTests"
val testdataDir = "../testdata"

val imoretestToRun = "com.example.test.selenium.integrationtest.IntegrationTests"
val itravisMoretestToRun = "com.example.test.selenium.integrationtest.TravisIntegrationTests"
val itestdataDir = "./testdata"

// resolvers += Resolver.mavenLocal

import com.typesafe.sbt.SbtGit.GitKeys._

import MyReleaseVersion._

lazy val commonSettings = versionSetting ++ Seq(
  organization  := "com.example",
  scalaVersion  := verScalaVersion,
  crossScalaVersions := verCrossScalaVersions,
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature" /* , "-Xlog-implicits" */),
  EclipseKeys.withSource := true,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
  EclipseKeys.useProjectId := true
)

lazy val Distribution = config("distribution") describedAs("tasks for creating a distribution.")

// The prereqs for the integration tests,
// to run integration tests (integrationtests, moretests) without running prereqs, on command line issue:
//   set prereqintegrationtests := {}
//
// Looks like there is a bug that is preventing the above set if the key is
// defined with taskKey[Unit](desc) instead of TaskKey[Unit](name,desc).
// The error is a type error, expecting a T got a Unit.
//
val prereqintegrationtests = TaskKey[Unit]("prereqintegrationtests", "Prereqs for unit tests on the assembly.jar file.") in Distribution

val integrationtests = taskKey[Unit]("Runs integration tests on the assembly.jar file.") in Distribution

val moretests = taskKey[Unit]("Runs more tests on the assembly.jar file.") in Distribution

val travismoretests = taskKey[Unit]("Runs travis more tests on the assembly.jar file.") in Distribution

val alltests = taskKey[Unit]("Runs all tests in JS and JVM projects.") in Distribution

val travis = taskKey[Unit]("The build that is run in Travis CI.") in Distribution

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

val hugo = taskKey[Unit]("Run Hugo")
val hugosetup = taskKey[Unit]("Setup to run Hugo")
val hugoWithTest = taskKey[Unit]("Run Hugo")
val hugosetupWithTest = taskKey[Unit]("Setup to run Hugo")
val helptask = taskKey[Seq[(java.io.File, String)]]("Identifies help resources")

lazy val bridgescorer: Project = project.in(file(".")).
  aggregate(sharedJVM, sharedJS, rotationJS, `bridgescorer-client`, `bridgescorer-server`, rotationJVM).
  dependsOn( `bridgescorer-server` % "test->test;compile->compile" ).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(WebScalaJSBundlerPlugin).
  settings(commonSettings: _*).
  settings(
    inConfig(Test)(baseAssemblySettings):_*
  ).
  settings(
    name := "bridgescorer",
    publish := {},
    publishLocal := {},
    resolvers += Resolver.bintrayRepo("scalaz", "releases"),

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.example.version",
    buildInfoObject := "Version2",
    buildInfoUsePackageAsPath := true,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

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

    EclipseKeys.classpathTransformerFactories ++= Seq(
      addDependentRunClassFolder("target/web/classes/main"),
//      removeRelativePath("target\\scala-"+verScalaMajorMinor+"\\resource_managed\\main")
    ),

    EclipseKeys.withSource := true,
    mainClass in Compile := Some("com.example.Server"),

    testOptions in Test := Seq(),

    test in assembly := {}, // test in (`bridgescorer-server`, Test),
    test in (Test,assembly) := {}, // { val x = assembly.value },
    
    assemblyJarName in (assembly) := s"${name.value}-server-assembly-${version.value}.jar",  // s"${name.value}-server-assembly-${version.value}.jar",
    assemblyJarName in (Test, assembly) := s"${name.value}-test-${version.value}.jar",
    
    assembly := {
      val log = streams.value.log
      val x = (assembly).value
      val sha = Sha256.generate( x )
      log.info( s"SHA-256: ${sha}" )
      x
    },

    assembly in Test := {
      val log = streams.value.log
      val x = (assembly in Test).value
      val sha = Sha256.generate( x )
      log.info( s"SHA-256: ${sha}" )
      x
    },

    helptask := {
      val depend = (hugoWithTest in help).value
      val rootdir = (target in help).value
      val helpdir = new File( rootdir, "help" )
      val prefix = rootdir.toString.length+1
      helpdir.allPaths.pair( f => Some( f.toString.substring(prefix) ) )

    },

    npmAssets := {
      helptask.value
    },

    scalaJSProjects := Seq(),
    pipelineStages in Assets := (if (onlyBuildDebug) Seq() else Seq(scalaJSProd)) ++ Seq(scalaJSDev, gzip ),

    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "maven", xs @ _*) if (!xs.isEmpty && (xs.last endsWith ".properties"))  => MergeStrategy.first
      case PathList("JS_DEPENDENCIES") => MergeStrategy.rename
      case PathList("module-info.class") => MergeStrategy.rename
//      case PathList("akka", "http", xs @ _*) => MergeStrategy.first
      case PathList("META-INF", "resources", "webjars", "bridgescorer", version, "lib", "bridgescorer-server", rest @ _*) => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    assemblyMergeStrategy in (Test, assembly) := {
      case PathList("META-INF", "maven", xs @ _*) if (!xs.isEmpty && (xs.last endsWith ".properties"))  => MergeStrategy.first
      case PathList("JS_DEPENDENCIES") => MergeStrategy.rename
      case PathList("module-info.class") => MergeStrategy.rename
//      case PathList("akka", "http", xs @ _*) => MergeStrategy.first
      case PathList("META-INF", "resources", "webjars", "bridgescorer", version, "lib", "bridgescorer-server", rest @ _*) => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    assemblyExcludedJars in (Test,assembly) := { 
      val log = streams.value.log
      val ccp = (fullClasspath in (Compile,assembly)).value.map { _.data.getName }
      log.info("fullClasspath in (Compile,assembly): "+ccp)
      val cp = (fullClasspath in (Test,assembly)).value
      log.info("fullClasspath in (Test,assembly): "+ccp)
      cp filter { x => 
        val rc = ccp.contains(x.data.getName)
        log.info("  "+(if (rc) "Excluding " else "Using     ")+x.data.getName)
        rc
      }
    },
    
    mainClass in Test := Some("org.scalatest.tools.Runner"),

    allassembly := {
      val x = assembly.value
      val y = (assembly in Test).value
    },
    // want to run bridgescorer-server/*:assembly::assembledMappings
    webassembly := { val x = (assembledMappings in assembly).value },

    dependencyUpdates := {
      val x = dependencyUpdates.value
      val z = dependencyUpdates.all(utilitiesAllProjects).value
    },

    clean := {
      val c = clean.value
      val ch = (clean in help).value
    },
    

    prereqintegrationtests := {
      val x = (assembly in Compile).value
      val y = (assembly in Test).value
    },

    integrationtests := Def.sequential( prereqintegrationtests in Distribution, fvt in Distribution).value,

    fvt := {
      val log = streams.value.log
      def getclasspath() = {
        val targetdir = (classDirectory in Compile).value+"/../"

        val assemblyjar = BridgeServer.findFile( targetdir+(assemblyJarName in assembly).value, "-SNAPSHOT.jar" )
        val testjar = BridgeServer.findFile( targetdir+(assemblyJarName in (Test,assembly)).value, "-SNAPSHOT.jar" )

        val cp = assemblyjar+java.io.File.pathSeparator+testjar
        log.info( "Classpath is "+cp )
        cp
      }
      val args = "-DUseProductionPage=1"::
                 "-DToMonitorFile=logs/atestTcpMonitorTimeWait.csv"::
                 "-DUseLogFilePrefix=logs/atest"::
                 "-DDefaultWebDriver="+useBrowser::
                 "-cp"::getclasspath()::
                 "org.scalatest.tools.Runner"::
                 "-oD"::
                 "-s"::
                 testToRun::
                 Nil
      val inDir = baseDirectory.value
      log.info( s"""Running in directory ${inDir}: java ${args.mkString(" ")}""" )
      val rc = Fork.java( ForkOptions().withWorkingDirectory( Some(inDir) ), args )
      if (rc != 0) throw new RuntimeException("integration tests failed")
    },

    moretests := Def.sequential( prereqintegrationtests in Distribution, svt in Distribution).value,

    svt := {
      val log = streams.value.log
      val (assemblyJar, testJar) = {
        val targetdir = (classDirectory in Compile).value+"/../"

        val assemblyjar = BridgeServer.findFile( targetdir+(assemblyJarName in assembly).value, "-SNAPSHOT.jar" )
        val testjar = BridgeServer.findFile( targetdir+(assemblyJarName in (Test,assembly)).value, "-SNAPSHOT.jar" )

        val cp = (assemblyjar, testjar)
        log.info( "Jars are "+cp )
        cp
      }
      val cp = assemblyJar+java.io.File.pathSeparator+testJar

      val server = new BridgeServer(assemblyJar)
      server.runWithServer(log, baseDirectory.value+"/logs/itestServerInTest.%u.log") {
        val jvmargs = server.getTestDefine():::
                                   "-DUseProductionPage=1"::
                                   "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv"::
                                   "-DUseLogFilePrefix=logs/itest"::
                                   "-DTestDataDirectory="+itestdataDir::
                                   "-DDefaultWebDriver="+useBrowser::
                                   "-cp"::cp::
                                   "org.scalatest.tools.Runner"::
                                   "-oD"::
                                   "-s"::
                                   imoretestToRun::
                                   Nil
        val inDir = baseDirectory.value
        log.info( s"""Running in directory ${inDir}: java ${jvmargs.mkString(" ")}""" )
        BridgeServer.runjava( log, jvmargs, 
                              Some(baseDirectory.value) )
      }
    },

    travismoretests := Def.sequential( prereqintegrationtests in Distribution, travissvt in Distribution).value,

    travissvt := {
      val log = streams.value.log
      val (assemblyJar, testJar) = {
        val targetdir = (classDirectory in Compile).value+"/../"

        val assemblyjar = BridgeServer.findFile( targetdir+(assemblyJarName in assembly).value, "-SNAPSHOT.jar" )
        val testjar = BridgeServer.findFile( targetdir+(assemblyJarName in (Test,assembly)).value, "-SNAPSHOT.jar" )

        val cp = (assemblyjar, testjar)
        log.info( "Jars are "+cp )
        cp
      }
      val cp = assemblyJar+java.io.File.pathSeparator+testJar

      val server = new BridgeServer(assemblyJar)
      server.runWithServer(log, baseDirectory.value+"/logs/itestServerInTest.%u.log") {
        val jvmargs = server.getTestDefine():::
                                   "-DUseProductionPage=1"::
                                   "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv"::
                                   "-DUseLogFilePrefix=logs/itest"::
                                   "-DTestDataDirectory="+itestdataDir::
                                   "-DMatchToTest=10"::
                                   "-DDefaultWebDriver="+useBrowser::
                                   "-cp"::cp::
                                   "org.scalatest.tools.Runner"::
                                   "-oD"::
                                   "-s"::
                                   itravisMoretestToRun::
                                   Nil
        val inDir = baseDirectory.value
        log.info( s"""Running in directory ${inDir}: java ${jvmargs.mkString(" ")}""" )
        BridgeServer.runjava( log, jvmargs, 
                              Some(baseDirectory.value) )
      }
    },

    standalonetests := Def.sequential( fvt in Distribution, svt in Distribution ).value,

    disttests := Def.sequential(integrationtests in Distribution, moretests in Distribution).value,

    publishdir := {
      // returns an Option[File]

      val log = streams.value.log

      import java.io.File
      sys.props("user.home") match {
        case homedir if (homedir!=null) =>
          val configfile = new File( homedir, "bridgescorer/config.properties") 
          if (configfile.exists()) {
            import java.util.Properties
            import java.io.InputStreamReader
            import java.io.FileInputStream
            val props = new Properties
            props.load( new InputStreamReader( new FileInputStream( configfile) , "UTF8") )
            val dd = props.getProperty("DistributionDirectory")
            if (dd != null) {
              import java.io.File

              val distdir = dd.replace('\\','/')
              val f = new File(distdir)
              if (f.isDirectory()) {
                log.info( "Publishing to "+f )
                Some(f)
              } else {
                throw new RuntimeException( "DistributionDirectory directory does not exist: "+f )
                None
             }
            } else {
              throw new RuntimeException( "DistributionDirectory property does not exist in file ~/bridgescorer/config.properties" )
              None
            }
          } else {
            throw new RuntimeException( "file ~/bridgescorer/config.properties does not exist" )
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

          log.info( "Publishing to "+distdir )

          val targetdir = (classDirectory in Compile).value+"/../"
          val assemblyjar = (assemblyJarName in assembly).value
          val testjar = (assemblyJarName in (Test,assembly)).value

          val sourceassemblyjar = new File( targetdir,assemblyjar )
          val targetassemblyjar = new File( distdir, assemblyjar )
          val sourcetestjar = new File( targetdir,testjar )
          val targettestjar = new File( distdir, testjar )

          IO.listFiles(distdir, GlobFilter("*.jar")).foreach { jar => {
            log.info( "Moving jar to save: "+jar )
            IO.move( jar, new File( new File( jar.getParentFile, "save" ), jar.getName ) ) 
          }}

          log.info("Publishing "+assemblyjar+" to "+distdir)
          Files.copy( sourceassemblyjar.toPath, targetassemblyjar.toPath, StandardCopyOption.REPLACE_EXISTING )
          log.info("Publishing "+testjar+" to "+distdir )
          Files.copy( sourcetestjar.toPath, targettestjar.toPath, StandardCopyOption.REPLACE_EXISTING )

          log.info( "Published to "+distdir )
        case None =>
          throw new RuntimeException("DistributionDirectory is not set")
      }

    },

    mypublish := Def.sequential(
                         disttests in Distribution, 
                         mypublishcopy in Distribution
                        ).value

  )

val bridgescorerAllProjects = ScopeFilter(
     inAggregates(bridgescorer, includeRoot = true)
   )

val utilitiesAllProjects = ScopeFilter(
     inAggregates(utilities, includeRoot = false)
   )

val bridgescorerAllCompileAndTestConfigurations = ScopeFilter(
     inAggregates(bridgescorer, includeRoot = false),
     inConfigurations(Compile,Test)
   )

val utilitiesAllCompileAndTestConfigurations = ScopeFilter(
     inAggregates(utilities, includeRoot = false),
     inConfigurations(Compile,Test)
   )

checkForUpdates := Def.sequential (
  MyNpm.checkForNpmUpdates in Compile in `bridgescorer-client`,
  MyNpm.checkForNpmUpdates in Test in `bridgescorer-client`,
//  MyNpm.checkForNpmUpdates.all(bridgescorerAllProjects),
  dependencyUpdates.all(bridgescorerAllProjects),
  dependencyUpdates
).value

lazy val `bridgescorer-shared` = crossProject(JSPlatform, JVMPlatform).in(file("shared")).
  enablePlugins(BuildInfoPlugin).
  settings(commonSettings: _*).
  settings(
    name := "bridgescorer-shared",
    resolvers += Resolver.bintrayRepo("scalaz", "releases"),

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.example.version",
    buildInfoObject := "VersionShared",
    buildInfoUsePackageAsPath := true,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

    libraryDependencies ++= bridgeScorerDeps.value,
    libraryDependencies ++= bridgeScorerSharedDeps.value,

    EclipseKeys.classpathTransformerFactories ++= Seq(
      MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-main-scala", "shared-src-main-scala"),
      MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-test-scala", "shared-src-test-scala"),
      MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-main-scala-"+verScalaMajorMinor, "shared-src-main-scala-"+verScalaMajorMinor),
      MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-test-scala-"+verScalaMajorMinor, "shared-src-test-scala-"+verScalaMajorMinor)
    ),
    EclipseKeys.projectTransformerFactories ++= Seq(
      MyEclipseTransformers.fixLinkName("-shared-shared-src-main-scala", "shared-src-main-scala"),
      MyEclipseTransformers.fixLinkName("-shared-shared-src-test-scala", "shared-src-test-scala"),
      MyEclipseTransformers.fixLinkName("-shared-shared-src-main-scala-"+verScalaMajorMinor, "shared-src-main-scala-"+verScalaMajorMinor),
      MyEclipseTransformers.fixLinkName("-shared-shared-src-test-scala-"+verScalaMajorMinor, "shared-src-test-scala-"+verScalaMajorMinor)
    )

  ).
  jvmSettings(
    libraryDependencies ++= bridgeScorerSharedJVMDeps.value
  ).
  jsSettings(

    // This gets rid of the jetty check which is required for the sbt runtime
    // not the application
    //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
    //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
//    dependencyUpdatesExclusions := moduleFilter(organization = "org.eclipse.jetty")
    dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty")
    
  )

lazy val sharedJS: Project = `bridgescorer-shared`.js.
  dependsOn( ProjectRef( uri("utilities"), "utilities-js" ))

lazy val sharedJVM = `bridgescorer-shared`.jvm.
  dependsOn( ProjectRef( uri("utilities"), "utilities-jvm" ))

lazy val `bridgescorer-rotation` = crossProject(JSPlatform, JVMPlatform).in(file("rotation")).
  enablePlugins(BuildInfoPlugin).
  settings(commonSettings: _*).
  settings(
    name := "bridgescorer-rotation",
    resolvers += Resolver.bintrayRepo("scalaz", "releases"),

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.example.version",
    buildInfoObject := "VersionRotation",
    buildInfoUsePackageAsPath := true,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

    libraryDependencies ++= bridgeScorerRotationDeps.value,

    EclipseKeys.classpathTransformerFactories ++= Seq(
      MyEclipseTransformers.fixLinkedNameFromClasspath("-rotation-shared-src-main-scala", "shared-src-main-scala"),
      MyEclipseTransformers.fixLinkedNameFromClasspath("-rotation-shared-src-test-scala", "shared-src-test-scala"),
      MyEclipseTransformers.fixLinkedNameFromClasspath("-rotation-shared-src-main-scala-"+verScalaMajorMinor, "shared-src-main-scala-"+verScalaMajorMinor),
      MyEclipseTransformers.fixLinkedNameFromClasspath("-rotation-shared-src-test-scala-"+verScalaMajorMinor, "shared-src-test-scala-"+verScalaMajorMinor)
    ),
    EclipseKeys.projectTransformerFactories ++= Seq(
      MyEclipseTransformers.fixLinkName("-rotation-shared-src-main-scala", "shared-src-main-scala"),
      MyEclipseTransformers.fixLinkName("-rotation-shared-src-test-scala", "shared-src-test-scala"),
      MyEclipseTransformers.fixLinkName("-rotation-shared-src-main-scala-"+verScalaMajorMinor, "shared-src-main-scala-"+verScalaMajorMinor),
      MyEclipseTransformers.fixLinkName("-rotation-shared-src-test-scala-"+verScalaMajorMinor, "shared-src-test-scala-"+verScalaMajorMinor)
    )

  ).
  jvmSettings(

  ).
  jsSettings(

    // This gets rid of the jetty check which is required for the sbt runtime
    // not the application
    //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
    //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
//    dependencyUpdatesExclusions := moduleFilter(organization = "org.eclipse.jetty")
    dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty"),
    
    testOptions in Test += {
      if (inTravis) println("Not running JS tests in bridgescorer-rotation")
      Tests.Filter(s => !inTravis)
    }
    
  )

lazy val rotationJS: Project = `bridgescorer-rotation`.js
lazy val rotationJVM = `bridgescorer-rotation`.jvm

// stuff to have dependencies on other projects 
lazy val utilities = RootProject(file("utilities"))

lazy val `bridgescorer-client` = project.in(file("client")).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(ScalaJSPlugin).
  enablePlugins(ScalaJSBundlerPlugin).
  dependsOn( sharedJS, rotationJS ).
  dependsOn( ProjectRef( uri("utilities"), "utilities-js" )).
  settings(commonSettings: _*).
  settings(
    name := "bridgescorer-client",
    EclipseKeys.classpathTransformerFactories ++= Seq(
      MyEclipseTransformers.replaceRelativePath("/bridgescorer-shared", "/bridgescorer-sharedJS"),
      MyEclipseTransformers.replaceRelativePath("/bridgescorer-rotation", "/bridgescorer-rotationJS")
    ),

    version in webpack := vWebPack,
    webpackCliVersion := vWebPackCli,
    version in startWebpackDevServer := vWebpackDevServer,
    version in installJsdom := vJsDom,
    
    scalaJSUseMainModuleInitializer := true,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.example.version",
    buildInfoObject := "VersionClient",
    buildInfoUsePackageAsPath := true,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

    // This gets rid of the jetty check which is required for the sbt runtime
    // not the application
    //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
    //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
//    dependencyUpdatesExclusions := moduleFilter(organization = "org.eclipse.jetty"),
    dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty"),

//    test in assembly := {},

//    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oU"),

//    testOptions in Test += Tests.Filter(s => { println("TestOption: "+s); true}),
    // no tests, npm stuff not working properly with tests
    //   https://github.com/scalacenter/scalajs-bundler/issues/83
//    testOptions in Test += Tests.Filter(s => { println("TestOption: "+s); false}),
    testOptions in Test += Tests.Filter(s => { 
      if (s == "com.example.test.AllUnitTests") {
        println("Using Test:    "+s) 
        true
      } else {
        println("Ignoring Test: "+s); 
        false
      }
    }),

// Indicate that unit tests will access the DOM
    version in webpack := vWebPack,
    version in installJsdom := vJsDom,

// warning: value requiresDOM in object AutoImport is deprecated (since 0.6.20): 
//   Requesting a DOM-enabled JS env with `jsDependencies += RuntimeDOM` 
//   or `requiresDOM := true` will not be supported in Scala.js 1.x. 
//   Instead, explicitly select a suitable JS with `jsEnv`, 
//   e.g., `jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv`.
//     requiresDOM in Test := true,
//     ^    
    
    requiresDOM in Test := true,
//    jsDependencies += RuntimeDOM,      // for testing in node.js and scala.js 0.6.13 https://www.scala-js.org/news/2016/10/17/announcing-scalajs-0.6.13/

    // this is for SBT 1.0
    // 11/18/17, 12/4/17 currently does not work, looks like JSDOM is not loaded
    // see https://github.com/scalacenter/scalajs-bundler/issues/181
    // error is navigator undefined
//    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,

// Compile tests to JS using fast-optimisation
//    scalaJSStage in Test := FastOptStage,

    libraryDependencies ++= bridgeScorerDeps.value,
    libraryDependencies ++= bridgeScorerClientDeps.value,

    // Resolve the required JS dependencies from NPM
    npmDependencies in Compile ++= bridgeScorerNpmDeps,
    npmDependencies in Test ++= bridgeScorerTestNpmDeps,

    // Add a dependency to the expose-loader (which will expose react to the global namespace)
    npmDevDependencies in Compile ++= bridgeScorerDevNpmDeps,
    npmDevDependencies in Test ++= bridgeScorerDevNpmDeps,

    // Use a custom config file to export the JS dependencies to the global namespace,
    // as expected by the scalajs-react facade
//    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
//    webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js"),
    webpackBundlingMode := BundlingMode.LibraryOnly("bridgeLib"),

    // React.JS itself
    // Note the JS filename. Can be react.js, react.min.js, react-with-addons.js, or react-with-addons.min.js.
    // Test requires react-with-addons
//    jsDependencies ++= bridgeScorerJsDeps,

//    unmanagedResources in Compile += (baseDirectory in ThisBuild).value / "BridgeScorer" / "nodejs" / "build" / "bridge" / "bridgedep.js",
//    jsDependencies in Compile += ProvidedJS / "bridgedep.js",
//    jsDependencies += ProvidedJS / "bridge/bridgedep.js",

//    crossTarget in (Compile,npmUpdate) := crossTarget.value / "scalajs-bundler" / "main" / "js" / "js",
//    crossTarget in (Test,npmUpdate) := crossTarget.value / "scalajs-bundler" / "test" / "js" / "js",

    skip in packageJSDependencies := false,

//    artifactPath in (Compile, fullOptJS) :=             (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "bridgescorer-js-opt.js",
//    artifactPath in (Compile, fastOptJS) :=             (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "bridgescorer-js-fastopt.js",
//    artifactPath in (Compile, packageJSDependencies) := (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "bridgescorer-js-jsdeps.js",
//    artifactPath in (Compile, packageScalaJSLauncher) := (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "scalajs-launcher.js",

    assemblyMergeStrategy in assembly := {
      case "JS_DEPENDENCIES"                            => MergeStrategy.concat
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  ).
  settings(
    inConfig(Compile)(MyNpm.myNpmSettings),
    inConfig(Test)(MyNpm.myNpmSettings)
  )

lazy val help = project.in(file("help")).
  settings(
    hugo := {
      val setup = hugosetup.value
      val log = streams.value.log
      val bd = new File(baseDirectory.value, "docs" )
      val targ = new File(target.value, "help" )
      Hugo.run(log, bd, targ)
    },
    
    hugosetup := {
      {
        val testgen = new File( baseDirectory.value+"/../server/target/docs" )
        val gen = new File( baseDirectory.value, "docs/static/images/gen" )
        println( s"Copy ${testgen} to ${gen}" )
        MyFileUtils.copyDirectory( testgen, gen, "png", 2 )
      }
    },
    
    hugoWithTest := Def.sequential( hugosetupWithTest, hugo ).value,

    hugosetupWithTest := Def.sequential( test in Test in `bridgescorer-server`, hugosetup ).value,

    clean := {
      val targ = target.value.toPath
      MyFileUtils.deleteDirectory( targ, None )
      val gen = new File( baseDirectory.value, "docs/static/images/gen" ).toPath
      MyFileUtils.deleteDirectory( gen, Some("png") )
      
    }
  )

lazy val `bridgescorer-server`: Project = project.in(file("server")).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(WebScalaJSBundlerPlugin).
  dependsOn( sharedJVM ).
  dependsOn( ProjectRef( uri("utilities"), "utilities-jvm" )).
  dependsOn( rotationJVM % "test" ).
  settings(
    inConfig(Test)(baseAssemblySettings):_*
  ).
  settings(commonSettings: _*).
  settings(
    name := "bridgescorer-server",
    EclipseKeys.classpathTransformerFactories ++= Seq(
      MyEclipseTransformers.replaceRelativePath("/bridgescorer-shared", "/bridgescorer-sharedJVM"),
      MyEclipseTransformers.replaceRelativePath("/bridgescorer-rotation", "/bridgescorer-rotationJVM")
    ),
    EclipseKeys.withSource := true,
    mainClass in Compile := Some("com.example.Server"),
    EclipseKeys.classpathTransformerFactories ++= Seq(
      addDependentRunClassFolder("target/web/classes/main"),
      removeRelativePath("target\\scala-"+verScalaMajorMinor+"\\resource_managed\\main")
    ),

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.example.version",
    buildInfoObject := "VersionServer",
    buildInfoUsePackageAsPath := true,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

    // shebang the jar file.  7z and jar will no longer see it as a valid zip file.
//    assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript)),
//    assemblyJarName in assembly := s"${name.value}-{version.value}",

    test in assembly := {},
    test in (Test,assembly) := {}, // { val x = assembly.value },
    
    assemblyJarName in (assembly) := s"${name.value}-assembly-${version.value}.jar",
    assemblyJarName in (Test, assembly) := s"${name.value}-test-${version.value}.jar",
    
    webassembly := { val x = (assembledMappings in assembly).value },
    
    
    assembly := {
      val log = streams.value.log
      val x = (assembly).value
      val sha = Sha256.generate( x )
      log.info( s"SHA-256: ${sha}" )
      x
    },
    
    assembly in Test := {
      val log = streams.value.log
      val x = (assembly in Test).value
      val sha = Sha256.generate( x )
      log.info( s"SHA-256: ${sha}" )
      x
    },

    mainClass in Test := Some("org.scalatest.tools.Runner"),

    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.ManagedClasses,

//    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oG"),

    testOptions in Test += Tests.Filter(s => { 
      if (s == testToRun) {
        println("Using Test:    "+s) 
        true
      } else {
        println("Ignoring Test: "+s); 
        false
      }
    }),

    baseDirectory in Test := {
      val x = (baseDirectory in Test).value
      println(s"baseDirectory in test is ${x}")
      x
    },
    fork in Test := true,
    javaOptions in Test ++= Seq(
      "-DDefaultWebDriver="+useBrowser,
      "-DSessionComplete="+useBrowser,
      "-DSessionDirector="+useBrowser,
      "-DSessionTable1="+useBrowser,
      "-DSessionTable2="+useBrowser
    ),

    libraryDependencies ++= bridgeScorerDeps.value,
    libraryDependencies ++= bridgeScorerServerDeps.value,

    bridgeScorerNpmAssets(`bridgescorer-client`),

//    helptask := {
//      val depend = (hugo in help).value
//      val rootdir = (target in help).value
//      val helpdir = new File( rootdir, "help" )
//      val prefix = rootdir.toString.length+1
//      helpdir.allPaths.pair( f => Some( f.toString.substring(prefix) ) )
//    },

    npmAssets := {
      val x = npmAssets.value
//      val h = helptask.value
      x // ++h
    },

    scalaJSProjects := Seq(`bridgescorer-client`),
    pipelineStages in Assets := (if (onlyBuildDebug) Seq() else Seq(scalaJSProd)) ++ Seq(scalaJSDev, gzip ),
    //(scalaJSPipeline),

//    pipelineStages := Seq(digest, gzip),

//    (resourceGenerators in Compile) <+= 
//      (resourceManaged in Compile, baseDirectory in ThisBuild) map { (dir,base) =>
//        IO.copy(
//          Seq( 
//           ( base / "client" / "target" / "js" / "js" / "bridgescorer-js-opt.js" , dir / "js" / "bridgescorer-js-opt.js"),
//           ( base / "client" / "target" / "js" / "js" / "bridgescorer-js-fastopt.js", dir / "js" / "bridgescorer-js-fastopt.js"),
//           ( base / "client" / "target" / "js" / "js" / "bridgescorer-js-jsdeps.js", dir / "js" / "bridgescorer-js-jsdeps.js")
////           ( base / "client" / "target" / "js" / "js" / "scalajs-launcher.js", dir / "js" / "scalajs-launcher.js")
//          ),
//          /* overwrite */ false, /* preserveLastModified */ false )
//        Seq( 
//          dir / "js" / "bridgescorer-js-opt.js",
//          dir / "js" / "bridgescorer-js-fastopt.js",
//          dir / "js" / "bridgescorer-js-jsdeps.js"
////          dir / "js" / "scalajs-launcher.js"
//        )

//      },

//    (resourceGenerators in Compile) <+=
//      (resourceManaged in Compile, baseDirectory in ThisBuild) map { (dir,base) =>
//        IO.copyDirectory(
//          base / "client" / "src" / "main" / "resources",
//          dir,
//          /* overwrite */ false, /* preserveLastModified */ false )
//        (PathFinder( dir / "web" ) ***).get
//      },

//    unmanagedClasspath in Runtime += baseDirectory.value / ".." / ".." / "BridgeScorer" / "js" / "src" / "main" / "resources",
//    unmanagedClasspath in Runtime += baseDirectory.value / ".." / ".." / "BridgeScorer" / "js" / "target" / "js",
//    unmanagedClasspath in Test += baseDirectory.value / ".." / ".." / "BridgeScorer" / "js" / "src" / "main" / "resources",
//    unmanagedClasspath in Test += baseDirectory.value / ".." / ".." / "BridgeScorer" / "js" / "target" / "js"

//    watchSources <++= (watchSources in BridgeScorerJS)

//    assemblyExcludedJars in assembly := { 
//      val cp = (fullClasspath in assembly).value
//      cp filter {_.data.getName == "compile-0.1.0.jar"}
//    },

    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "maven", xs @ _*) if (!xs.isEmpty && (xs.last endsWith ".properties"))  => MergeStrategy.first
      case PathList("JS_DEPENDENCIES") => MergeStrategy.rename
//      case PathList("akka", "http", xs @ _*) => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    assemblyExcludedJars in (Test,assembly) := { 
      val log = streams.value.log
      val ccp = (fullClasspath in (Compile,assembly)).value.map { _.data.getName }
      log.info("fullClasspath in (Compile,assembly): "+ccp)
      val cp = (fullClasspath in (Test,assembly)).value
      log.info("fullClasspath in (Test,assembly): "+ccp)
      cp filter { x => 
        val rc = ccp.contains(x.data.getName)
        log.info("  "+(if (rc) "Excluding " else "Using     ")+x.data.getName)
        rc
      }
    },

// the following does not work.  Need to figure out how to set assemblyMergeStrategy in (Test,assembly)
//    inConfig(Test)(
//    (assemblyMergeStrategy in assembly) := {
//      case PathList("META-INF", "maven", xs @ _*) if (!xs.isEmpty && (xs.last endsWith ".properties"))  => MergeStrategy.first
//      case x =>
//        val oldStrategy = (assemblyMergeStrategy in assembly).value
//        oldStrategy(x)
//    }),

    prereqintegrationtests := Def.sequential(
//      test in Test in `bridgescorer-server`,
//      hugo in help, 
      assembly in Compile, 
      assembly in Test
    ).value,

    prereqintegrationtests := {
      val x = (assembly in Compile).value 
      val y = (assembly in Test).value
    },

    integrationtests := Def.sequential( prereqintegrationtests in Distribution, fvt in Distribution).value,

    fvt := {
      val log = streams.value.log
      def getclasspath() = {
        val targetdir = (classDirectory in Compile).value+"/../"

        val assemblyjar = BridgeServer.findFile( targetdir+(assemblyJarName in assembly).value, "-SNAPSHOT.jar" )
        val testjar = BridgeServer.findFile( targetdir+(assemblyJarName in (Test,assembly)).value, "-SNAPSHOT.jar" )

        val cp = assemblyjar+java.io.File.pathSeparator+testjar
        log.info( "Classpath is "+cp )
        cp
      }
      val args = "-DUseProductionPage=1"::
                 "-DToMonitorFile=logs/atestTcpMonitorTimeWait.csv"::
                 "-DUseLogFilePrefix=logs/atest"::
                 "-DDefaultWebDriver="+useBrowser::
                 "-cp"::getclasspath()::
                 "org.scalatest.tools.Runner"::
                 "-oD"::
                 "-s"::
                 testToRun::
                 Nil
      val inDir = baseDirectory.value
      log.info( s"""Running in directory ${inDir}: java ${args.mkString(" ")}""" )
      val rc = Fork.java( ForkOptions().withWorkingDirectory( Some(inDir) ), args )
      if (rc != 0) throw new RuntimeException("integration tests failed")
    },

    moretests := Def.sequential( prereqintegrationtests in Distribution, svt in Distribution).value,

    svt := {
      val log = streams.value.log
      val (assemblyJar, testJar) = {
        val targetdir = (classDirectory in Compile).value+"/../"

        val assemblyjar = BridgeServer.findFile( targetdir+(assemblyJarName in assembly).value, "-SNAPSHOT.jar" )
        val testjar = BridgeServer.findFile( targetdir+(assemblyJarName in (Test,assembly)).value, "-SNAPSHOT.jar" )

        val cp = (assemblyjar, testjar)
        log.info( "Jars are "+cp )
        cp
      }
      val cp = assemblyJar+java.io.File.pathSeparator+testJar

      val server = new BridgeServer(assemblyJar)
      server.runWithServer(log, baseDirectory.value+"/logs/itestServerInTest.%u.log") {
        val jvmargs = server.getTestDefine():::
                                   "-DUseProductionPage=1"::
                                   "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv"::
                                   "-DUseLogFilePrefix=logs/itest"::
                                   "-DTestDataDirectory="+testdataDir::
                                   "-DDefaultWebDriver="+useBrowser::
                                   "-cp"::cp::
                                   "org.scalatest.tools.Runner"::
                                   "-oD"::
                                   "-s"::
                                   moretestToRun::
                                   Nil
        val inDir = baseDirectory.value
        log.info( s"""Running in directory ${inDir}: java ${jvmargs.mkString(" ")}""" )
        BridgeServer.runjava( log, jvmargs, 
                              Some(baseDirectory.value) )
      }
    },

    travismoretests := Def.sequential( prereqintegrationtests in Distribution, travissvt in Distribution).value,

    travissvt := {
      val log = streams.value.log
      val (assemblyJar, testJar) = {
        val targetdir = (classDirectory in Compile).value+"/../"

        val assemblyjar = BridgeServer.findFile( targetdir+(assemblyJarName in assembly).value, "-SNAPSHOT.jar" )
        val testjar = BridgeServer.findFile( targetdir+(assemblyJarName in (Test,assembly)).value, "-SNAPSHOT.jar" )

        val cp = (assemblyjar, testjar)
        log.info( "Jars are "+cp )
        cp
      }
      val cp = assemblyJar+java.io.File.pathSeparator+testJar

      val server = new BridgeServer(assemblyJar)
      server.runWithServer(log, baseDirectory.value+"/logs/itestServerInTest.%u.log") {
        val jvmargs = server.getTestDefine():::
                                   "-DUseProductionPage=1"::
                                   "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv"::
                                   "-DUseLogFilePrefix=logs/itest"::
                                   "-DTestDataDirectory="+testdataDir::
                                   "-DMatchToTest=10"::
                                   "-DDefaultWebDriver="+useBrowser::
                                   "-cp"::cp::
                                   "org.scalatest.tools.Runner"::
                                   "-oD"::
                                   "-s"::
                                   travisMoretestToRun::
                                   Nil
        val inDir = baseDirectory.value
        log.info( s"""Running in directory ${inDir}: java ${jvmargs.mkString(" ")}""" )
        BridgeServer.runjava( log, jvmargs, 
                              Some(baseDirectory.value) )
      }
    },

    standalonetests := Def.sequential( fvt in Distribution, svt in Distribution ).value,

    disttests := Def.sequential(integrationtests in Distribution, moretests in Distribution).value,

    publishdir := {
      // returns an Option[File]

      val log = streams.value.log

      import java.io.File
      sys.props("user.home") match {
        case homedir if (homedir!=null) =>
          val configfile = new File( homedir, "bridgescorer/config.properties") 
          if (configfile.exists()) {
            import java.util.Properties
            import java.io.InputStreamReader
            import java.io.FileInputStream
            val props = new Properties
            props.load( new InputStreamReader( new FileInputStream( configfile) , "UTF8") )
            val dd = props.getProperty("DistributionDirectory")
            if (dd != null) {
              import java.io.File

              val distdir = dd.replace('\\','/')
              val f = new File(distdir)
              if (f.isDirectory()) {
                log.info( "Publishing to "+f )
                Some(f)
              } else {
                throw new RuntimeException( "DistributionDirectory directory does not exist: "+f )
              	None
             }
            } else {
              throw new RuntimeException( "DistributionDirectory property does not exist in file ~/bridgescorer/config.properties" )
              None
            }
          } else {
            throw new RuntimeException( "file ~/bridgescorer/config.properties does not exist" )
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

          log.info( "Publishing to "+distdir )

          val targetdir = (classDirectory in Compile).value+"/../"
          val assemblyjar = (assemblyJarName in assembly).value
          val testjar = (assemblyJarName in (Test,assembly)).value

          val sourceassemblyjar = new File( targetdir,assemblyjar )
          val targetassemblyjar = new File( distdir, assemblyjar )
          val sourcetestjar = new File( targetdir,testjar )
          val targettestjar = new File( distdir, testjar )

          IO.listFiles(distdir, GlobFilter("*.jar")).foreach { jar => {
            log.info( "Moving jar to save: "+jar )
            IO.move( jar, new File( new File( jar.getParentFile, "save" ), jar.getName ) ) 
          }}

          log.info("Publishing "+assemblyjar+" to "+distdir)
          Files.copy( sourceassemblyjar.toPath, targetassemblyjar.toPath, StandardCopyOption.REPLACE_EXISTING )
          log.info("Publishing "+testjar+" to "+distdir )
          Files.copy( sourcetestjar.toPath, targettestjar.toPath, StandardCopyOption.REPLACE_EXISTING )

          log.info( "Published to "+distdir )
        case None =>
          throw new RuntimeException("DistributionDirectory is not set")
      }

    },

    mypublish := Def.sequential(
                         disttests in Distribution, 
                         mypublishcopy in Distribution
                        ).value
  )

alltests := Def.sequential(
                       mydist in Distribution in utilities,
                       fastOptJS in Compile in `bridgescorer-client`,
                       fullOptJS in Compile in `bridgescorer-client`,
                       fastOptJS in Test in `bridgescorer-client`,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
                       test in Test in rotationJVM,
                       test in Test in rotationJS,
                       test in Test in `bridgescorer-client`,
                       test in Test in `bridgescorer-server`,
//                       hugo in help,
                       disttests in Distribution in `bridgescorer`
                      ).value

travis := Def.sequential(
                       travis in Distribution in utilities,
                       fastOptJS in Compile in `bridgescorer-client`,
                       fullOptJS in Compile in `bridgescorer-client`,
                       fastOptJS in Test in `bridgescorer-client`,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
                       test in Test in rotationJVM,
                       test in Test in rotationJS,
                       test in Test in `bridgescorer-client`,
                       test in Test in `bridgescorer-server`,
//                       hugo in help,
                       travismoretests in Distribution in `bridgescorer`
                      ).value

mydist := Def.sequential(
                       clean.all(bridgescorerAllProjects),
                       mydist in Distribution in utilities,
                       fastOptJS in Compile in `bridgescorer-client`,
                       fullOptJS in Compile in `bridgescorer-client`,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
//                       assembly in `bridgescorer-client`,
//                       assembly in Test in `bridgescorer-client`,
                       test in Test in rotationJVM,
                       test in Test in rotationJS,
                       test in Test in `bridgescorer-client`,
                       test in Test in `bridgescorer-server`,
//                       hugo in help,
                       mypublish in Distribution in `bridgescorer`
                      ).value

myclean := Def.sequential(
                       clean.all(bridgescorerAllProjects)
                      ).value

mytest := Def.sequential(
//                       fastOptJS in Compile in `bridgescorer-client`,
//                       fullOptJS in Compile in `bridgescorer-client`,
                       allassembly,
//                       packageJSDependencies in Compile in `bridgescorer-client`,
                       test in Test in `bridgescorer-client`,
                       test in Test in `bridgescorer-server`
                      ).value


lazy val releaseCheck = { st: State =>
  Project.extract(st).runTask(publishdir in `bridgescorer`, st) match {
    case (newst,Some(dir)) =>
      if (!dir.isDirectory()) throw new RuntimeException("failed check for release, DistributionDirectory does not exist: "+dir)
      newst
    case (newst, None) =>
      throw new RuntimeException("failed check for release, DistributionDirectory not defined")
      newst
    case _ =>
      throw new RuntimeException("failed check for release, unknown error")
      st
  }
}


import ReleaseTransformations.{ setReleaseVersion=>_, setNextVersion=>_, _ }

val publishRelease = ReleaseStep(
  check  = releaseCheck,                                       // upfront check
  action = releaseStepTaskAggregated(mydist in Distribution in bridgescorer) // publish release notes
)

releaseUseGlobalVersion := false

//
// need to update release tag and comment
//
releaseTagName := "v"+git.baseVersion.value

releaseTagComment := s"Releasing ${git.baseVersion.value}"

releaseCommitMessage := s"Setting version to ${git.baseVersion.value}"

// import MyReleaseVersion._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  gitMakeReleaseBranch,
  inquireVersions,                        // : ReleaseStep
//  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  recalculateVersion,                     // : ReleaseStep
  publishRelease,                         // : ReleaseStep, custom
  setNextVersion,                         // : ReleaseStep
  commitNextVersion                       // : ReleaseStep
//  gitMergeReleaseMaster,
//  recalculateVersion,                     // : ReleaseStep
//  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)
