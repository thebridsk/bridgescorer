import sbt._
import sbtrelease.Versions
import sbtrelease.Utilities._

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.{
  setReleaseVersion => _,
  setNextVersion => _,
  _
}

import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.SbtGit.git

import sbt.Keys._
import sbtrelease.Vcs
import sbt.SettingKey

import sys.process.ProcessLogger

/**
  * <code><pre>
  * import ReleaseTransformations.{ setReleaseVersion=>_, setNextVersion=>_, _ }
  * import MyReleaseVersion._
  *
  * val root = project.
  *   settings( versionSetting )
  *
  * enablePlugins(GitVersioning)
  *
  * releaseUseGlobalVersion := false
  * releaseTagName := "v"+git.baseVersion.value
  * releaseTagComment := s"Releasing ${git.baseVersion.value}"
  * releaseCommitMessage := s"Setting version to ${git.baseVersion.value}"
  * releaseNextCommitMessage := s"Setting version to ${git.baseVersion.value}"
  *
  * releaseProcess := Seq[ReleaseStep](
  *     checkSnapshotDependencies,
  *     inquireVersions,
  *     setReleaseVersion,
  *     commitReleaseVersion,
  *     tagRelease,
  *     recalculateVersion,
  *     ...    // clean build test assembly
  *     setNextVersion,
  *     commitNextVersion,
  *     recalculateVersion,
  *     pushChanges
  *   )
  *
  * </pre></code>
  */
object MyReleaseVersion {

  //
  // Need to have my own setVersion function because git.baseVersion is being written to version.sbt, not version
  //

  //releaseUseGlobalVersion := false

  private val releaseBranch = "release"

  private val releaseFromBranch = sys.props
    .get("ReleaseFromBranch")
    .getOrElse(sys.env.get("ReleaseFromBranch").getOrElse("master"))

  private val globalVersionString = "git.baseVersion in ThisBuild := \"%s\""
  private val versionString = "git.baseVersion := \"%s\""
  private def setVersion(selectVersion: Versions => String): ReleaseStep = { st: State =>
      val vs = st
        .get(ReleaseKeys.versions)
        .getOrElse(
          sys.error(
            "No versions are set! Was this release part executed before inquireVersions?"
          )
        )
      val selected = selectVersion(vs)

      st.log.info("Setting version to '%s'." format selected)
      import sbtrelease.Utilities._
      val useGlobal = st.extract.get(releaseUseGlobalVersion)
      val versionStr = (if (useGlobal) globalVersionString else versionString) format selected
      writeVersion(st, versionStr)

      reapply(
        Seq(
          if (useGlobal) git.baseVersion := selected
          else git.baseVersion := selected
        ),
        st
      )
  }

  private def writeVersion(st: State, versionString: String) {
    import sbtrelease.Utilities._
    val file = st.extract.get(releaseVersionFile)
    IO.writeLines(file, Seq(versionString))
  }

  val versionSetting = Seq(
    version := {
      val v = version.value
      val n = name.value
      val headCommit = gitHeadCommit.value
      val curBranch = gitCurrentBranch.value
      val uncommittedChanges = gitUncommittedChanges.value

      // hack to determine if this is a JS project
//        val isScalaJSx = SettingKey[Boolean]("scalaJSUseMainModuleInitializer").?.value.isDefined

      val crossVersion = Keys.crossVersion.value
      val isScalaJS = crossVersion == org.scalajs.sbtplugin.ScalaJSCrossVersion.binary

//        val isScalaJS = isScalaJSProject.value
      val js = if (isScalaJS) " JS" else ""
//        println(s"""Original version for ${n+js}: ${v}""")
      val v1 =
        if (v contains headCommit.getOrElse("Unknown")) v
        else
          v + "-" + headCommit.getOrElse("Unknown") + (if (uncommittedChanges)
                                                         "-SNAPSHOT"
                                                       else "")
//        println("v1 is "+v1+" in "+ n+js)
      val v2 =
        if (v1.endsWith(curBranch) || curBranch == releaseBranch) v1
        else if (headCommit.map(_ == curBranch).getOrElse(false))
          v1 + "-DANGER-HEAD"
        else v1 + "-" + curBranch
//        println("v2 is "+v2+" in "+ n+js)
      val v3 = v2.replaceAll("[\\/]", "_")
      println(
        s"Version is $v3 in $n$js"
        // + " $crossVersion ${org.scalajs.sbtplugin.ScalaJSCrossVersion.binary}"
      )
      v3
    },
    isSnapshot := {
      val ver = version.value
      snapshotVersion = ver.contains("-SNAPSHOT") || ver.contains("DANGER-HEAD")
      snapshotVersion
    }
  )

  private var snapshotVersion = false

  /**
    * Is the current version a SNAPSHOT version.
    * This is ONLY valid after version setting is used in build.
    */
  def isSnapshotVersion = snapshotVersion

  lazy val setReleaseVersion: ReleaseStep = setVersion(_._1)
  lazy val setNextVersion: ReleaseStep = setVersion(_._2)

  //
  // release step to recalculate the version
  // Updates:
  //   formattedShaVersion
  //   formattedDateVersion
  //   isSnapshot
  //   version
  def recalculateVersion: ReleaseStep = { st: State =>
    reapply(
      Seq(
        formattedShaVersion in ThisBuild := {
          val base = git.baseVersion.?.value
          val suffix =
            git.makeUncommittedSignifierSuffix(
              git.gitUncommittedChanges.value,
              git.uncommittedSignifier.value
            )
          git.gitHeadCommit.value map { sha =>
            git.defaultFormatShaVersion(base, sha, suffix)
          }
        },
        formattedDateVersion in ThisBuild := {
          val base = git.baseVersion.?.value
          git.defaultFormatDateVersion(base, new java.util.Date)
        },
        isSnapshot in ThisBuild := {
          git.gitCurrentTags.value.isEmpty || git.gitUncommittedChanges.value
        },
        version := {
          val overrideVersion =
            git.overrideVersion(git.versionProperty.value)
          val uncommittedSuffix =
            git.makeUncommittedSignifierSuffix(
              git.gitUncommittedChanges.value,
              git.uncommittedSignifier.value
            )
          val releaseVersion =
            git.releaseVersion(
              git.gitCurrentTags.value,
              git.gitTagToVersionNumber.value,
              uncommittedSuffix
            )
          val describedVersion =
            git.flaggedOptional(
              git.useGitDescribe.value,
              git.describeVersion(
                git.gitDescribedVersion.value,
                uncommittedSuffix
              )
            )
          val datedVersion = formattedDateVersion.value
          val commitVersion = formattedShaVersion.value
          //Now we fall through the potential version numbers...
          git.makeVersion(
            Seq(
              overrideVersion,
              releaseVersion,
              describedVersion,
              commitVersion
            )
          ) getOrElse datedVersion // For when git isn't there at all.
        }
      ),
      st
    )

  }

  private def toProcessLogger(st: State): ProcessLogger = new ProcessLogger {
    override def err(s: => String): Unit = st.log.info(s)
    override def out(s: => String): Unit = st.log.info(s)
    override def buffer[T](f: => T): T = st.log.buffer(f)
  }

  private def vcs(st: State): Vcs = {
    st.extract
      .get(releaseVcs)
      .getOrElse(
        sys.error(
          "Aborting release. Working directory is not a repository of a recognized VCS."
        )
      )
  }

  private implicit class WrapState( val st: State ) extends AnyVal {

    /**
      * Throws exception if status code is non zero
      * @param args
      * @return the output
      */
    def git( args: String* ): String = {
      val cmd = vcs(st)
      st.log.info(s"Running git ${args.mkString(" ")}")
      try {
        val stdout = cmd.cmd(args: _*) !! toProcessLogger(st)
        st.log.info(stdout)
        stdout
      } catch {
        case x: RuntimeException =>
          st.log.error(s"git error: ${x}")
          throw x
      }

    }

    /**
      * Throws exception if status code is non zero
      * @param args
      * @return the output
      */
    def gitExpectError( args: String* ): String = {
      val cmd = vcs(st)
      st.log.info(s"Running git ${args.mkString(" ")}")
      try {
        val stdout = cmd.cmd(args: _*) !! toProcessLogger(st)
        st.log.error(s"Expecting error, got: ${stdout}")
        stdout
      } catch {
        case x: RuntimeException =>
          st.log.info(s"git error, expected: ${x}")
          throw x
      }

    }

    def runTask[T]( key: TaskKey[T] ): T = {
      val extracted = Project.extract(st)
      extracted.runTask(key,st)._2
    }

    // def run[T]( key: TaskKey[T] ) = {
    //   releaseStepTask(key)(st)
    // }

    def run( command: String ) = {
      releaseStepCommandAndRemaining(command)(st)
    }
  }

  def gitStatus: ReleaseStep = { st: State =>
    st.git("status")
    recalculateVersion.action(st)
  }

  def gitMakeReleaseBranch: ReleaseStep = ReleaseStep(
    // action
    { st: State =>
      st.git("checkout", "-B", releaseBranch)
      recalculateVersion.action(st)
    },
    // check
    { st: State =>
      val extracted = Project.extract(st)
      val currentBranch = extracted.get(gitCurrentBranch)
      if (currentBranch != releaseFromBranch)
        sys.error(
          s"""Must be on ${releaseFromBranch} branch to release, use ReleaseFromBranch=${currentBranch} env var"""
        )
      (try {
        val commitid = st.gitExpectError("rev-parse", "-q", "--verify", releaseBranch)
        Some(s"Branch $releaseBranch should not exist, found at commit id: $commitid")
      } catch {
        case x: RuntimeException =>
          st.log.debug(s"Branch $releaseBranch does not exist")
          None
      }).map( e => scala.sys.error(e))
      st
    }
  )

  def gitMergeReleaseMaster: ReleaseStep = ReleaseStep(
    // action
    { st: State =>
      st.git("checkout", "master")
      st.git("merge", releaseBranch)
      st.git("branch", "--delete", "--force", releaseBranch)
      recalculateVersion.action(st)
    },
    // check
    { st: State =>
      st
    }
  )

  def gitPushReleaseBranch: ReleaseStep = ReleaseStep(
    // action
    { st: State =>
      st.log.debug( s"Running: git push -u origin $releaseBranch")
      st.git("push", "-u", "origin", releaseBranch)
      st
    },
    // check
    { st: State =>
      st
    }
  )

  def getTagFromVersion( v: String ) = s"v$v"

  /**
    * A release step to push the tag to GitHub.
    *
    * The command myrelease-with-defaults must be used to do a release.
    * This command issues the command "release with-defaults"
    *
    */
  def gitPushReleaseTag: ReleaseStep = ReleaseStep(
    // action
    { st: State =>
      val vs = st
        .get(ReleaseKeys.versions)
        .getOrElse(
          sys.error(
            "No versions are set! Was this release executed with release with-defaults?"
          )
        )
      val tagName = getTagFromVersion(vs._1)
      st.log.debug( s"Running: git push origin $tagName")
      st.git("push", "origin", tagName)
      st
    },
    // check
    { st: State =>
      val vs = inquireVersions.action(st)   // ugly hack
        .get(ReleaseKeys.versions)
        .getOrElse(
          sys.error(
            "No versions are set! Was this release executed with release with-defaults?"
          )
        )
      val tagName = getTagFromVersion(vs._1)
      (try {
        val commitid = st.gitExpectError("rev-parse", "-q", "--verify", tagName)
        Some(s"Tag $tagName should not exist, found at commit id: $commitid")
      } catch {
        case x: RuntimeException =>
          st.log.debug(s"Tag $tagName does not exist")
          None
      }).map( e => scala.sys.error(e))

      st
    }
  )

  val releaseWithDefaults = Command.command(
    "myrelease-with-defaults",
    "run the 'release with-defaults' command",
    "run the 'release with-defaults' command"
  ) { st =>
    st.run( "release with-defaults" )
  }

}
