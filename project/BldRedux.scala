
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

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldRedux {

  lazy val redux = project.in(file("redux")).
    configure( commonSettings ).
    enablePlugins(ScalaJSPlugin).
    enablePlugins(ScalaJSBundlerPlugin).
    settings(
      libraryDependencies ++= reduxDeps.value,

      npmDependencies in Test ++= reduxTestNpmDeps,

      scalacOptions in Test += "-P:scalajs:sjsDefinedByDefault",

      version in webpack := vWebPack,
      webpackCliVersion := vWebPackCli,
      version in startWebpackDevServer := vWebpackDevServer,
      version in installJsdom := vJsDom,

      cleanKeepGlobs /* in Compile */ ++= Seq(
        Glob((crossTarget in npmUpdate in Compile).value, "node_modules") / **,
      ),

      cleanKeepGlobs /* in Test */ ++= Seq(
        Glob((crossTarget in npmUpdate in Test).value, "node_modules") / **
      ),

    )

}
