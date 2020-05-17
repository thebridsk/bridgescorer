import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldBridgeClientApi {

  val clientUnitTests =
    "com.github.thebridsk.bridge.clientapi.test.TestColor" ::
    Nil

  lazy val `bridgescorer-clientapi` = project
    .in(file("clientapi"))
    .configure(
      commonSettings,
      noTests(true),
      buildInfo("com.github.thebridsk.bridge.clientapi.version", "VersionClient")
    )
    .enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(
      BldBridgeShared.sharedJS,
      BldBridgeRotation.rotationJS,
      BldBridgeClientCommon.`bridgescorer-clientcommon`,
      BldMaterialUI.materialui
    )
    .dependsOn(`utilities-js`)
    .settings(
      name := "bridgescorer-clientapi",
      version in webpack := vWebPack,
      webpackCliVersion := vWebPackCli,
      version in startWebpackDevServer := vWebpackDevServer,
      // version in installJsdom := vJsDom,
      mainClass := Some("com.github.thebridsk.bridge.clientapi.BridgeApi"),
      scalaJSUseMainModuleInitializer := true,

      // This gets rid of the jetty check which is required for the sbt runtime
      // not the application
      //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
      //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
      dependencyUpdatesFilter -= moduleFilter(
        organization = "org.eclipse.jetty"
      ),

      cleanKeepGlobs /* in Compile */ ++= Seq(
        Glob((crossTarget in npmUpdate in Compile).value, "node_modules") / **,
      ),

      scalaJSLinkerConfig in fastOptJS ~= (_.withSourceMap(false)),

//    test in assembly := {},

//    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oU"),

//    testOptions in Test += Tests.Filter(s => { println("TestOption: "+s); true}),
      // no tests, npm stuff not working properly with tests
      //   https://github.com/scalacenter/scalajs-bundler/issues/83
//    testOptions in Test += Tests.Filter(s => { println("TestOption: "+s); false}),
      testOptions in Test += Tests.Filter(s => {
        if (s == "xxcom.github.thebridsk.bridge.clientapi.test.AllUnitTests") {
//        if (clientUnitTests.contains(s)) {
          println("Using Test:    " + s)
          true
        } else {
          println("Ignoring Test: " + s);
          false
        }
      }),
// Indicate that unit tests will access the DOM
      version in webpack := vWebPack,
      // version in installJsdom := vJsDom,

      // requireJsDomEnv in Test := true,

      // this is for SBT 1.0
      // 11/18/17, 12/4/17 currently does not work, looks like JSDOM is not loaded
      // see https://github.com/scalacenter/scalajs-bundler/issues/181
      // error is navigator undefined
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,

// Compile tests to JS using fast-optimisation
//    scalaJSStage in Test := FastOptStage,
      if (useFullOpt) {
        scalaJSStage in Test := FullOptStage
      } else {
        scalaJSStage in Test := FastOptStage
      },
      libraryDependencies ++= bridgeScorerDeps.value,
      libraryDependencies ++= bridgeScorerClientApiDeps.value,
      // Resolve the required JS dependencies from NPM
      npmDependencies in Compile ++= bridgeScorerClientApiNpmDeps,
      npmDependencies in Test ++= bridgeScorerClientApiTestNpmDeps,
      // Add a dependency to the expose-loader (which will expose react to the global namespace)
      npmDevDependencies in Compile ++= bridgeScorerClientApiDevNpmDeps,
      npmDevDependencies in Test ++= bridgeScorerClientApiDevNpmDeps,
      // Use a custom config file to export the JS dependencies to the global namespace,
      // as expected by the scalajs-react facade
      // webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
      webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js"),
      webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack.prod.config.js"),
      // webpackBundlingMode := BundlingMode.LibraryAndApplication(),
      webpackBundlingMode := BundlingMode.LibraryOnly("bridgeLib"),
      // webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly("bridgeLib"),
      // webpackBundlingMode in fullOptJS := BundlingMode.LibraryOnly("bridgeLib"),
      // React.JS itself
      // Note the JS filename. Can be react.js, react.min.js, react-with-addons.js, or react-with-addons.min.js.
      // Test requires react-with-addons
//    jsDependencies ++= bridgeScorerJsDeps,

//    unmanagedResources in Compile += (baseDirectory in ThisBuild).value / "BridgeScorer" / "nodejs" / "build" / "bridge" / "bridgedep.js",
//    jsDependencies in Compile += ProvidedJS / "bridgedep.js",
//    jsDependencies += ProvidedJS / "bridge/bridgedep.js",

//    crossTarget in (Compile,npmUpdate) := crossTarget.value / "scalajs-bundler" / "main" / "js" / "js",
//    crossTarget in (Test,npmUpdate) := crossTarget.value / "scalajs-bundler" / "test" / "js" / "js",
//      skip in packageJSDependencies := false,
//    artifactPath in (Compile, fullOptJS) :=             (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "bridgescorer-js-opt.js",
//    artifactPath in (Compile, fastOptJS) :=             (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "bridgescorer-js-fastopt.js",
//    artifactPath in (Compile, packageJSDependencies) := (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "bridgescorer-js-jsdeps.js",
//    artifactPath in (Compile, packageScalaJSLauncher) := (baseDirectory in ThisBuild).value / "client" / "target" / "js" / "js" / "scalajs-launcher.js",
      assemblyMergeStrategy in assembly := {
        case "JS_DEPENDENCIES" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      }
    )
    .settings(
      inConfig(Compile)(MyNpm.myNpmSettings),
      inConfig(Test)(MyNpm.myNpmSettings)
    )

}
