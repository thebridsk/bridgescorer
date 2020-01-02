
import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._
import MyReleaseVersion._

object BldBridgeHelp {

  val patternVersion = """(\d+(?:\.\d+)*(?:-SNAPSHOT)?)-.*""".r

  lazy val help = project.in(file("help")).
    settings( versionSetting: _* ).
    settings(
      scalaVersion  := verScalaVersion,

      skipGenerateImageSetting := skipGenerateImage,

      hugo := {
        val setup = hugosetup.value
        val log = streams.value.log
        val bd = new File(baseDirectory.value, "docs" )
        val targ = new File(target.value, "help" )

        val helpversion = version.value
        val shorthelpversion = helpversion match {
          case patternVersion(v) => v
          case _ => helpversion
        }

        Hugo.run(log, bd, targ, helpversion, shorthelpversion)
      },

      hugosetup := {
        {
          val testgen = new File( baseDirectory.value + "/../fullserver/target/docs" )
          val gen = new File( baseDirectory.value, "docs/static/images/gen" )
          println( s"Copy ${testgen} to ${gen}" )
          MyFileUtils.copyDirectory( testgen, gen, "png", 2 )
        }
      },

      hugoWithTest := Def.sequential( hugosetupWithTest, hugo ).value,

      hugoWithTest := Def.taskDyn {
        val log = streams.value.log
        val bd = new File(baseDirectory.value, "docs" )
        val oldtask = hugoWithTest.taskValue
        if (skipGenerateImageSetting.value && Hugo.gotGeneratedImages(log,bd)) {
          hugo
        } else {
          Def.task(oldtask.value)
        }
      }.value,

      hugosetupWithTest := Def.sequential( test in Test in BldBridgeFullServer.`bridgescorer-fullserver`, hugosetup ).value,

      clean := {
        val targ = target.value.toPath
        MyFileUtils.deleteDirectory( targ, None )
        val gen = new File( baseDirectory.value, "docs/static/images/gen" ).toPath
        MyFileUtils.deleteDirectory( gen, Some("png") )

      }
    )

  }
