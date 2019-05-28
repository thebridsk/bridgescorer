
import sbt._
import sbtrelease.Versions
import sbtrelease.Utilities._

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.{ setReleaseVersion=>_, setNextVersion=>_, _ }

import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.SbtGit.git

import sbt.Keys._
import sbtrelease.Vcs
import sbt.SettingKey

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

  private val releaseFromBranch = sys.props.get("RELEASEFROMBRANCH").getOrElse( sys.env.get("RELEASEFROMBRANCH").getOrElse("master"))

  private val globalVersionString = "git.baseVersion in ThisBuild := \"%s\""
  private val versionString = "git.baseVersion := \"%s\""
  private def setVersion(selectVersion: Versions => String): ReleaseStep =  { st: State =>
    val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
    val selected = selectVersion(vs)

    st.log.info("Setting version to '%s'." format selected)
    import sbtrelease.Utilities._
    val useGlobal = st.extract.get(releaseUseGlobalVersion)
    val versionStr = (if (useGlobal) globalVersionString else versionString) format selected
    writeVersion(st, versionStr)

    reapply(Seq(
      if (useGlobal) git.baseVersion := selected
      else git.baseVersion := selected
    ), st)
  }

  private def writeVersion(st: State, versionString: String) {
    import sbtrelease.Utilities._
    val file = st.extract.get(releaseVersionFile)
    IO.writeLines(file, Seq(versionString))
  }

  import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.isScalaJSProject

  val versionSetting =   Seq(
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
        val v1 = if (v contains headCommit.getOrElse("Unknown")) v
                 else v+"-"+headCommit.getOrElse("Unknown")+(if (uncommittedChanges) "-SNAPSHOT" else "")
//        println("v1 is "+v1+" in "+ n+js)
        val v2 = if (v1.endsWith(curBranch) || curBranch == releaseBranch) v1
                 else if ( headCommit.map( _ == curBranch).getOrElse(false) ) v1+"-DANGER-HEAD"
                 else v1+ "-"+curBranch
//        println("v2 is "+v2+" in "+ n+js)
        val v3 = v2.replaceAll("[\\/]", "_")
        println(s"Version is $v3 in $n$js"
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

      reapply(Seq(
          formattedShaVersion in ThisBuild := {
            val base = git.baseVersion.?.value
            val suffix =
              git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
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
              git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
            val releaseVersion =
              git.releaseVersion(git.gitCurrentTags.value, git.gitTagToVersionNumber.value, uncommittedSuffix)
            val describedVersion =
              git.flaggedOptional(git.useGitDescribe.value, git.describeVersion(git.gitDescribedVersion.value, uncommittedSuffix))
            val datedVersion = formattedDateVersion.value
            val commitVersion = formattedShaVersion.value
            //Now we fall through the potential version numbers...
            git.makeVersion(Seq(
               overrideVersion,
               releaseVersion,
               describedVersion,
               commitVersion
            )) getOrElse datedVersion // For when git isn't there at all.
          }
      ), st)

  }

  private def vcs(st: State): Vcs = {
    st.extract.get(releaseVcs).getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))
  }

  def gitStatus: ReleaseStep = { st: State =>
    val gitcmd = vcs(st)
    gitcmd.cmd("status").lineStream.foreach( line => st.log.info(s"""Git: ${line}""") )
    recalculateVersion.action(st)
  }

  def gitMakeReleaseBranch: ReleaseStep = ReleaseStep(
    // action
    { st: State =>
      val gitcmd = vcs(st)
      gitcmd.cmd("checkout","-B",releaseBranch).lineStream.foreach( line => st.log.info(s"""gitMakeReleaseBranch: ${line}""") )
      recalculateVersion.action(st)
    },
    // check
    { st: State =>
      val extracted = Project.extract(st)
      val currentBranch = extracted.get(gitCurrentBranch)
      if (currentBranch != releaseFromBranch) sys.error(s"""Must be on ${releaseFromBranch} branch to release, use RELEASEFROMBRANCH=${currentBranch} env var""")
      st
    }
  )

  def gitMergeReleaseMaster: ReleaseStep = ReleaseStep(
    // action
    { st: State =>
      val gitcmd = vcs(st)
      gitcmd.cmd("checkout","master").lineStream.foreach( line => st.log.info(s"""gitMergeReleaseMaster checkout master: ${line}""") )
      gitcmd.cmd("merge",releaseBranch).lineStream.foreach( line => st.log.info(s"""gitMergeReleaseMaster merge ${releaseBranch}: ${line}""") )
      gitcmd.cmd("branch","--delete", "--force", releaseBranch).lineStream.foreach( line => st.log.info(s"""gitMergeReleaseMaster branch -d ${releaseBranch}: ${line}""") )
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
      val gitcmd = vcs(st)
      gitcmd.cmd("push", "-u", "origin", releaseBranch).lineStream.foreach( line => st.log.info(s"""gitMergeReleaseMaster push -u origin ${releaseBranch}: ${line}""") )
      st
    },
    // check
    { st: State =>
      st
    }
  )
}
