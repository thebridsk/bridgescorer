import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
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

object BldBridgeClient {

  val clientUnitTests = "com.example.test.MyTest" ::
    "com.example.test.TestDuplicateStore" ::
    "com.example.test.TestLogFilter" ::
    "com.example.test.TestSerialize" ::
    "com.example.test.TestColor" ::
    Nil

  lazy val `bridgescorer-client` = project
    .in(file("client"))
    .configure(
      commonSettings,
      buildInfo("com.example.version", "VersionClient")
    )
    .enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(
      BldBridgeShared.sharedJS,
      BldBridgeRotation.rotationJS,
      BldBridgeMaterialUI.materialui
    )
    .dependsOn(`utilities-js`)
    .settings(
      name := "bridgescorer-client",
      EclipseKeys.classpathTransformerFactories ++= Seq(
        MyEclipseTransformers.replaceRelativePath(
          "/bridgescorer-shared",
          "/bridgescorer-sharedJS"
        ),
        MyEclipseTransformers.replaceRelativePath(
          "/bridgescorer-rotation",
          "/bridgescorer-rotationJS"
        )
      ),
      version in webpack := vWebPack,
      webpackCliVersion := vWebPackCli,
      version in startWebpackDevServer := vWebpackDevServer,
      version in installJsdom := vJsDom,
//    scalaJSUseMainModuleInitializer := true,

      // This gets rid of the jetty check which is required for the sbt runtime
      // not the application
      //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
      //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
      dependencyUpdatesFilter -= moduleFilter(
        organization = "org.eclipse.jetty"
      ),
//    test in assembly := {},

//    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oU"),

//    testOptions in Test += Tests.Filter(s => { println("TestOption: "+s); true}),
      // no tests, npm stuff not working properly with tests
      //   https://github.com/scalacenter/scalajs-bundler/issues/83
//    testOptions in Test += Tests.Filter(s => { println("TestOption: "+s); false}),
      testOptions in Test += Tests.Filter(s => {
//      if (s == "com.example.test.AllUnitTests") {
        if (clientUnitTests.contains(s)) {
          println("Using Test:    " + s)
          true
        } else {
          println("Ignoring Test: " + s);
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
      requireJsDomEnv in Test := true,
//    requiresDOM in Test := true,
//    jsDependencies += RuntimeDOM,      // for testing in node.js and scala.js 0.6.13 https://www.scala-js.org/news/2016/10/17/announcing-scalajs-0.6.13/

      // this is for SBT 1.0
      // 11/18/17, 12/4/17 currently does not work, looks like JSDOM is not loaded
      // see https://github.com/scalacenter/scalajs-bundler/issues/181
      // error is navigator undefined
//    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,

// Compile tests to JS using fast-optimisation
//    scalaJSStage in Test := FastOptStage,
      if (useFullOpt) {
        scalaJSStage in Test := FullOptStage
      } else {
        scalaJSStage in Test := FastOptStage
      },
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
