
// To check the versions of the plugins, issue the following command:
//
//    sbt "reload plugins" dependencyUpdates
//

name := "project-bridgescorer"

scalaVersion := "2.12.11"

val vLog4j = "1.7.30"              // https://github.com/qos-ch/slf4j
val vJGit = "5.8.0.202006091008-r" // https://github.com/eclipse/jgit

val vCrossProject = "1.0.0"        // https://github.com/portable-scala/sbt-crossproject
// scala-react does not compile on scalajs 1.0 yet
val vScalaJSDefault = "1.1.1"      // http://www.scala-js.org/
val vEnvJsDomNodejs = "1.1.0"      // https://github.com/scala-js/scala-js-env-jsdom-nodejs

val sbtScalaJsBundlerSuffix = if (vScalaJSDefault.startsWith("0.6.")) "-sjs06" else ""  // "" - for ScalaJS 1.0 "-sjs06" for ScalaJS 0.6

val vSbtAssembly = "0.15.0"        // https://github.com/sbt/sbt-assembly
val vSbtGit = "1.0.0"              // https://github.com/sbt/sbt-git
val vSbtSCoverage = "1.6.1"        // https://github.com/scoverage/sbt-scoverage
val vSbtBuildInfo = "0.9.0"        // https://github.com/sbt/sbt-buildinfo
// v1.0.12 causes java.lang.NoSuchMethodError: sbtrelease.ReleaseStateTransformations$.commitNextVersion()Lsbtrelease/ReleasePlugin$autoImport$ReleaseStep;
val vSbtRelease = "1.0.13"         // https://github.com/sbt/sbt-release
val vSbtDependencyGraph = "0.9.2"  // https://github.com/jrudolph/sbt-dependency-graph
val vSbtUpdates = "0.5.1"          // https://github.com/rtimush/sbt-updates

val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse(vScalaJSDefault)

val vSbtGzip = "1.0.2"             // https://github.com/sbt/sbt-gzip
val vSbtScalaJsBundler = "0.18.0"  // https://github.com/scalacenter/scalajs-bundler

// not used:

val vSbtRevolver = "0.8.0"         // https://github.com/spray/sbt-revolver
val vSbtUniDoc = "0.3.3"           // https://github.com/sbt/sbt-unidoc
val vSbtDigest = "1.1.1"           // https://github.com/sbt/sbt-digest
val vSbtScalaFmt="2.4.0"           // https://github.com/scalameta/sbt-scalafmt
val vBloop = "1.4.3"               // https://github.com/scalacenter/bloop

// The following is needed to get rid of the message
//   SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
// when sbt is started.

libraryDependencies ++= Seq(
//  "org.scala-js" %% "scalajs-env-jsdom-nodejs" % vEnvJsDomNodejs withSources(),
  "org.slf4j" % "slf4j-log4j12" % vLog4j withSources()
)

// Unfortunately this causes an exception to be logged to the console from sbt-git plugin
// because it can't find git.
// update jgit plugin to avoid exception

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % vJGit withSources()
     exclude("javax.jms", "jms")
     exclude("com.sun.jdmk", "jmxtools")
     exclude("com.sun.jmx", "jmxri")
)

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % vCrossProject withSources())
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion withSources())

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % vSbtAssembly withSources())
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % vSbtGit withSources())
addSbtPlugin("org.scoverage" % "sbt-scoverage" % vSbtSCoverage withSources())
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % vSbtBuildInfo withSources())
addSbtPlugin("com.github.gseitz" % "sbt-release" % vSbtRelease withSources())
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % vSbtUpdates withSources())
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % vSbtDependencyGraph withSources())  // must be updated for sbt 1.0

scalacOptions ++= Seq( "-unchecked", "-deprecation" )

scalacOptions ++= Seq( "-unchecked", "-feature", "-deprecation" )

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % vSbtGzip withSources())

addSbtPlugin("ch.epfl.scala" % s"sbt-web-scalajs-bundler${sbtScalaJsBundlerSuffix}" % vSbtScalaJsBundler withSources())
addSbtPlugin("ch.epfl.scala" % s"sbt-scalajs-bundler${sbtScalaJsBundlerSuffix}" % vSbtScalaJsBundler withSources())
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % vSbtScalaFmt withSources())
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % vBloop withSources())
