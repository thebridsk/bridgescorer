
import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{crossProject => _, CrossType => _, _}

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldBridgeShared {

  lazy val `bridgescorer-shared` = crossProject(JSPlatform, JVMPlatform).in(file("shared")).
    configure(commonSettings,buildInfo("com.github.thebridsk.bridge.data.version", "VersionShared")).
    settings(
      name := "bridgescorer-shared",
      resolvers += Resolver.bintrayRepo("scalaz", "releases"),

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
  //   jvmConfigure( _.dependsOn( `utilities-jvm` ) ).
    jsSettings(

      // This gets rid of the jetty check which is required for the sbt runtime
      // not the application
      //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
      //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
  //    dependencyUpdatesExclusions := moduleFilter(organization = "org.eclipse.jetty")
      dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty")

    ) // .
  //  jvmConfigure( _.dependsOn( `utilities-jvm` ) )

  lazy val sharedJS: Project = `bridgescorer-shared`.js.
    dependsOn( `utilities-js` )

  lazy val sharedJVM = `bridgescorer-shared`.jvm.
    dependsOn( `utilities-jvm` )

}
