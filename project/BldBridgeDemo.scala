
import sbt._
import Keys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitBranchPrompt
import sbtcrossproject.{crossProject, CrossType}
import BldDependencies._
import BldCommonSettings._
import sbtassembly.AssemblyPlugin.autoImport._
import com.typesafe.sbt.git.JGit
import collection.JavaConverters._
import org.eclipse.jgit.transport.RefSpec
import scala.sys.process.ProcessLogger

object BldBridgeDemo {

  val remoteName = "origin"
  val pagesBranch = "gh-pages"

  val demoTargetDir = settingKey[File]("The demo directory.")

  lazy val demo: Project = project.in(file("demo"))
    .configure( commonSettings )
    .dependsOn(BldBridgeScoreKeeper.bridgescorekeeper % "test->test" )
    .dependsOn(BldBridgeServer.`bridgescorer-server`)
    .dependsOn(ProjectRef(uri("utilities"), "utilities-jvm"))
    .settings(
      name := "bridgedemo",

      mainClass in Compile := Some("com.github.thebridsk.bridgedemo.Publish"),

      libraryDependencies ++= bridgeScorerDemoDeps,

      demoTargetDir := baseDirectory.value / "target" / "demo",

      generateDemo := Def.taskDyn {
        val jar = (crossTarget in BldBridgeScoreKeeper.bridgescorekeeper).value / (assemblyJarName in assembly in BldBridgeScoreKeeper.bridgescorekeeper).value
        val target = demoTargetDir.value
        // val jar = (assembly in BldBridgeScoreKeeper.bridgescorekeeper).value
        Def.task {
          (run in Compile).toTask(s""" $jar $target""").value
        }
      }.value,

      publishDemo := {
        // val x = generateDemo.value
        import collection.JavaConverters._

        val log = streams.value.log

        val rootDir = baseDirectory.value / ".."
        val rootGit = JGit(rootDir)

        val remotes = rootGit.porcelain.remoteList.call
        val rootURL = (remotes.asScala.find( rc => rc.getName == "origin" ).flatMap { remoteConfig =>
          log.info(s"Remote is ${remoteConfig.getName}, URIs:")
          val uris = remoteConfig.getURIs.asScala
          uris.foreach { uri =>
            log.info(s"  URI: ${uri}")
          }

          if (uris.length == 1) {
            Some(uris.head)
          } else {
            None
          }

        }.getOrElse {
          sys.error(s"Unable to determine the remote URL for ${rootDir}")
        }).toASCIIString
        log.info( s"Root URL: ${rootURL}, class is ${rootURL.getClass.getName}")

        val demoURL = rootURL.replace("bridgescorer.git", "bridgescorerdemo.git")
        log.info( s"demoURL: ${demoURL}")
        val demoDir = demoTargetDir.value
        log.info( s"demoDir: ${demoDir}")

        if ( !demoDir.isDirectory
             || !new File(demoDir,"public").isDirectory
             || !new File(demoDir,"help").isDirectory
             || !new File(demoDir,"index.html").isFile
        ) {
          sys.error("Did not find files in demo directory")
        }

        try {
          import org.eclipse.jgit.api.{Git => PGit}
          val git = PGit.init().setDirectory( demoDir ).call()
          val demoGit = JGit(demoDir)

          import org.eclipse.jgit.transport.URIish
          val dUrl = new URIish(demoURL)
          val remote = demoGit.porcelain.remoteAdd().setName(remoteName).setUri(dUrl).call()
          log.info(s"remote is ${remote.getName}, pushURIs=${remote.getPushURIs.asScala}, URIs=${remote.getURIs.asScala}")

          val branch = demoGit.porcelain.checkout().setOrphan(true).setName( pagesBranch ).call()
          log.info(s"branch is $branch")
          val added = demoGit.porcelain.add().addFilepattern("help").addFilepattern("public").addFilepattern("index.html").call()
          log.info(s"added is $added")

          val commit = demoGit.porcelain.commit().setMessage("publish demo").call()
          log.info(s"commit is $commit")

          // // this requires a CredentialProvider
          // val push = demoGit.porcelain.push().setRemote(remoteName).setRefSpecs(new RefSpec(pagesBranch)).call()
          // push.asScala.foreach { pr =>
          //   println(s"push result $pr")
          // }

          val gitcmd = sbtrelease.Git.mkVcs( demoDir )
          gitcmd.cmd( "push", "--force", remoteName, pagesBranch) !! toProcessLogger(log)



        } catch {
          case x: Exception =>
            throw new RuntimeException( s"Error publishing demo: $x", x)
        }
      },

      fork in run := true,
      fork in Test := true,

      javaOptions in Test ++= Seq(
        "-Xmx4096M",
        "-DDefaultWebDriver=" + useBrowser,
      )
    )

  private def toProcessLogger(log: sbt.internal.util.ManagedLogger): ProcessLogger = new ProcessLogger {
    override def err(s: => String): Unit = log.info(s)
    override def out(s: => String): Unit = log.info(s)
    override def buffer[T](f: => T): T = log.buffer(f)
  }

}
