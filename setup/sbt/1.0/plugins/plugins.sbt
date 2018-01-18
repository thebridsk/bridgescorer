
val vSbtDependencyGraph = "0.9.0"  // https://github.com/jrudolph/sbt-dependency-graph
val vSbtUpdates         = "0.3.3"  // https://github.com/rtimush/sbt-updates

// resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % vSbtDependencyGraph)
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % vSbtUpdates)
