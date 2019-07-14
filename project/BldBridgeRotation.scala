
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

object BldBridgeRotation {

  lazy val `bridgescorer-rotation` = crossProject(JSPlatform, JVMPlatform).in(file("rotation")).
    configure(commonSettings,buildInfo("com.github.thebridsk.bridge.version", "VersionRotation")).
    settings(
      name := "bridgescorer-rotation",
      resolvers += Resolver.bintrayRepo("scalaz", "releases"),

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

}
