
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

        val rootdir = target.value
        val helpdir = new File(rootdir, "help")
        val prefix = rootdir.toString.length + 1
        helpdir.allPaths.pair(f => Some(f.toString.substring(prefix)))

      },

      hugoserver := {
        val setup = hugosetup.value
        val log = streams.value.log
        val bd = new File(baseDirectory.value, "docs" )

        val helpversion = version.value
        val shorthelpversion = helpversion match {
          case patternVersion(v) => v
          case _ => helpversion
        }

        log.info( "Running hugo" )

        Hugo.runServer(log, bd, helpversion, shorthelpversion)
      },

      hugosetup := {
        {
          val testgen = new File( baseDirectory.value, "/../fullserver/target/docs" ).getCanonicalFile
          val gen = new File( baseDirectory.value, "docs/static/images/gen" )
          val log = streams.value.log
          log.info( s"Copy ${testgen} to ${gen}" )
          MyFileUtils.copyDirectory(testgen, gen, 2)(MyFileUtils.onlyCopy("png"))
        }
      },

      hugoWithTest := Def.sequential( hugosetupWithTest, hugo ).value,

      hugoWithTest := (Def.taskDyn {
        val log = streams.value.log
        log.info( "Running hugoWithTest" )
        val bd = new File(baseDirectory.value, "docs" )
        if (skipGenerateImageSetting.value && Hugo.gotGeneratedImages(log,bd)) {
          Def.task {
            val h = hugo.value
            h
          }
        } else {
          val hugoTest = hugoWithTest.taskValue
          Def.task {
            val h = hugoTest.value
            h
          }
        }
      }).value,

      hugosetupWithTest := Def.sequential( test in Test in BldBridgeFullServer.`bridgescorer-fullserver`, hugosetup ).value,

      clean := {
        val targ = target.value.toPath
        MyFileUtils.deleteDirectory( targ, None )
        val gen = new File( baseDirectory.value, "docs/static/images/gen" ).toPath
        MyFileUtils.deleteDirectory( gen, Some("png") )

      }
    )

  }
