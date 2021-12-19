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

object BldBridgeClientTest {

  val clientUnitTests =
//    "com.github.thebridsk.bridge.clientapi.test.TestColor" ::
    Nil

  lazy val `bridgescorer-clienttest` = Project("clienttest", file("clienttest"))
    .configure(
      commonSettings,
      noTests(true),
      buildInfo("com.github.thebridsk.bridge.clienttest.version", "VersionClientTest")
    )
    .enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(
      BldBridgeShared.sharedJS,
      BldBridgeRotation.rotationJS,
      BldBridgeClientCommon.`bridgescorer-clientcommon`,
      BldMaterialUI.materialui,
      BldColor.colorJS,
      BldRedux.redux
    )
    .dependsOn(`utilities-js`)
    .settings(
      name := "bridgescorer-clienttest",
      version in webpack := vWebPack,
      webpackCliVersion := vWebPackCli,
      version in startWebpackDevServer := vWebpackDevServer,
      // version in installJsdom := vJsDom,
      // requireJsDomEnv in Test := true,
      mainClass in (Compile, run) := Some("com.github.thebridsk.bridge.clientapi.BridgeTest"),
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

      // scalaJSLinkerConfig in fastOptJS ~= (_.withSourceMap(false)),

//    testOptions in Test += Tests.Filter(s => { println("Using Test: "+s); true}),
      testOptions in Test += Tests.Filter(s => {
//        if (s == "xxcom.github.thebridsk.bridge.clientapi.test.AllUnitTests") {
        if (clientUnitTests.contains(s)) {
          println("Using Test:    " + s)
          true
        } else {
          println("Ignoring Test: " + s);
          false
        }
      }),

      // this is for SBT 1.0
      // 11/18/17, 12/4/17 currently does not work, looks like JSDOM is not loaded
      // see https://github.com/scalacenter/scalajs-bundler/issues/181
      // error is navigator undefined
      // jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,

      // Compile tests to JS using fast-optimisation
      if (buildProduction) {
        scalaJSStage := FullOptStage
      } else {
        scalaJSStage := FastOptStage
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
      webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js"),
      webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack.dev.config.js"),

      webpackEmitSourceMaps in fullOptJS := false,

      webpackBundlingMode := BundlingMode.LibraryOnly("bridgeLib"),

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
