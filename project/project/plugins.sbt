
// To check the versions of the plugins, issue the following command:
//
//    sbt "reload plugins" dependencyUpdates
//

val vLog4j = "1.7.26"              // https://github.com/qos-ch/slf4j

val vSbtEclipse = "5.2.4"          // https://github.com/typesafehub/sbteclipse

// The following is needed to get rid of the message
//   SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
// when sbt is started.

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % vLog4j

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % vSbtEclipse)
