//
// To only build the debug version of the client code,
// define the environment variable "OnlyBuildDebug"
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

import Dependencies._
import sbtassembly.AssemblyPlugin.defaultShellScript

import MyEclipseTransformers._

import scala.language.postfixOps

enablePlugins(GitVersioning, GitBranchPrompt)
EclipseKeys.skipParents in ThisBuild := false

val onlyBuildDebug = sys.props.get("OnlyBuildDebug").
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


val testToRun = "com.example.test.AllSuites"
//val testToRun = "com.example.test.MyServiceSpec"

val moretestToRun = "com.example.test.selenium.IntegrationTests"
val testdataDir = "../testdata"

// resolvers += Resolver.mavenLocal

import com.typesafe.sbt.SbtGit.GitKeys._

lazy val commonSettings = versionSetting ++ Seq(
  organization  := "com.example",
  scalaVersion  := verScalaVersion,
  crossScalaVersions := verCrossScalaVersions,
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature" /* , "-Xlog-implicits" */),
  EclipseKeys.withSource := true,
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

val alltests = taskKey[Unit]("Runs all tests in JS and JVM projects.") in Distribution

val disttests = taskKey[Unit]("Runs unit tests and more tests on the assembly.jar file.") in Distribution

val publishdir = taskKey[Option[java.io.File]]("The target directory for mypublishcopy") in Distribution

val mypublishcopy = taskKey[Unit]("Copy the published artifacts") in Distribution

val mypublish = taskKey[Unit]("Publish by copying") in Distribution

val myclean = taskKey[Unit]("clean") in Distribution

val mytest = taskKey[Unit]("build and test it") in Distribution

val mydist = taskKey[Unit]("Make a build for distribution") in Distribution

val fvt = taskKey[Unit]("Run test cases using assembled jars, does not build jars") in Distribution

val svt = taskKey[Unit]("Run test cases using assembled jars, does not build jars") in Distribution

val standalonetests = taskKey[Unit]("Run test cases using assembled jars, does not build jars") in Distribution

val allassembly = taskKey[Unit]("Build assembly and test assembly")

val webassembly = taskKey[Unit]("Build web application")

val checkForUpdates = taskKey[Unit]("Check for updates")

lazy val bridgescorer = project.in(file(".")).
  aggregate(sharedJVM, sharedJS, rotationJS, `bridgescorer-client`, `bridgescorer-server`, rotationJVM).
  settings(commonSettings: _*).
  settings(
    name := "bridgescorer",
    publish := {},
    publishLocal := {},
    resolvers += Resolver.bintrayRepo("scalaz", "releases"),

    aggregate in assembly := false,

    assembly := {
      val x = (compile in (rotationJVM,Compile)).value
      (assembly in `bridgescorer-server`).value 
    },
    assembly in Test := { (assembly in (`bridgescorer-server`,Test)).value },

    allassembly := {
      val x = assembly.value
      val y = (assembly in Test).value
    },
    // want to run bridgescorer-server/*:assembly::assembledMappings
    webassembly := { val x = (assembledMappings in (`bridgescorer-server`,assembly)).value },

    dependencyUpdates := {
      val x = dependencyUpdates.value
      val z = dependencyUpdates.all(utilitiesAllProjects).value
    }

  )

val bridgescorerAllProjects = ScopeFilter(
     inAggregates(bridgescorer, includeRoot = false)
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

lazy val `bridgescorer-shared` = crossProject.in(file("shared")).
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

lazy val `bridgescorer-rotation` = crossProject.in(file("rotation")).
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
    dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty")
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
      if (s != "") {
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
    webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js"),
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

lazy val `bridgescorer-server` = project.in(file("server")).
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
    mainClass in Test := Some("org.scalatest.tools.Runner"),

    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.ManagedClasses,

//    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oG"),
//    testOptions in Test += Tests.Filter(s => { println("TestOption: "+s); s.endsWith("AllSuites") }),
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
      "-DDefaultWebDriver=chrome",
      "-DSessionComplete=chrome",
      "-DSessionDirector=chrome",
      "-DSessionTable1=chrome",
      "-DSessionTable2=chrome"
    ),

    libraryDependencies ++= bridgeScorerDeps.value,
    libraryDependencies ++= bridgeScorerServerDeps.value,

    bridgeScorerNpmAssets(`bridgescorer-client`),

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

        val cp = assemblyjar+";"+testjar
        log.info( "Classpath is "+cp )
        cp
      }
      val args = "-DUseProductionPage=1"::
                 "-DToMonitorFile=logs/atestTcpMonitorTimeWait.csv"::
                 "-DUseLogFilePrefix=logs/atest"::
                 "-cp"::getclasspath()::
                 "org.scalatest.tools.Runner"::
                 "-o"::
                 "-s"::
                 testToRun::
                 Nil
      val inDir = baseDirectory.value
      log.info( s"""Running in directory ${inDir}: java ${args.mkString(" ")}""" )
      val rc = Fork.java( ForkOptions().withWorkingDirectory( Some(inDir) ), args )
      if (rc != 0) sys.error("integration tests failed")
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
      val cp = assemblyJar+";"+testJar

      val server = new BridgeServer(assemblyJar)
      server.runWithServer(log, baseDirectory.value+"/logs/itestServerInTest.%u.log") {
        val jvmargs = server.getTestDefine():::
                                   "-DUseProductionPage=1"::
                                   "-DToMonitorFile=logs/itestTcpMonitorTimeWait.csv"::
                                   "-DUseLogFilePrefix=logs/itest"::
                                   "-DTestDataDirectory="+testdataDir::
                                   "-cp"::cp::
                                   "org.scalatest.tools.Runner"::
                                   "-o"::
                                   "-s"::
                                   moretestToRun::
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
                sys.error( "DistributionDirectory directory does not exist: "+f )
              	None
             }
            } else {
              sys.error( "DistributionDirectory property does not exist in file ~/bridgescorer/config.properties" )
              None
            }
          } else {
            sys.error( "file ~/bridgescorer/config.properties does not exist" )
            None
          }
        case _ =>
          sys.error("Home directory not set")
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
          sys.error("DistributionDirectory is not set")
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
                       disttests in Distribution in `bridgescorer-server`
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
                       mypublish in Distribution in `bridgescorer-server`
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
  Project.extract(st).runTask(publishdir in `bridgescorer-server`, st) match {
    case (newst,Some(dir)) =>
      if (!dir.isDirectory()) sys.error("failed check for release, DistributionDirectory does not exist: "+dir)
      newst
    case (newst, None) =>
      sys.error("failed check for release, DistributionDirectory not defined")
      newst
    case _ =>
      sys.error("failed check for release, unknown error")
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

import MyReleaseVersion._

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
  commitNextVersion,                      // : ReleaseStep
  gitMergeReleaseMaster,
  recalculateVersion,                     // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)
