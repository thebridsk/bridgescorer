

object BldVersion {

  lazy val verScalaVersion = "2.13.3"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq(verScalaVersion)

// The scala.js version is NOT defined in this file.
// It is defined in ProjectScala/project/plugins.sbt
//
// The version of the scala.js library
// This MUST match the version in ProjectScala/project/plugins.sbt of
//    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.11")
// And MUST match the version in ProjectScala/BridgeScorer/project/plugins.sbt of
//    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.11")
// This file is ProjectScala/BridgeScorer/build.sbt
//  lazy val vScalaJsLibrary = "0.6.13" // http://mvnrepository.com/artifact/org.scala-js/scalajs-library_2.11


  lazy val vAkka = "2.6.10"            // https://github.com/akka/akka
  lazy val vAkkaHttp = "10.2.1"        // https://github.com/akka/akka-http

  lazy val vAkkaHttpPlayJson="1.35.2"  // https://github.com/hseeberger/akka-http-json

  lazy val vSwaggerAkkaHttp = "2.2.0"  // https://github.com/swagger-akka-http/swagger-akka-http
  lazy val vSwaggerScalaModule="2.1.3" // https://github.com/swagger-akka-http/swagger-scala-module
  lazy val vSwagger="2.1.5"            // https://github.com/swagger-api/swagger-core
  lazy val vWsRsApi="2.1.6"            // https://github.com/eclipse-ee4j/jaxrs-api
  lazy val vAkkaHttpCors = "1.1.0"     // https://github.com/lomigmegard/akka-http-cors

  lazy val vSwaggerUI = "3.36.2"       // https://www.npmjs.com/package/swagger-ui-dist
  lazy val vScalajsdom = "1.1.0"       // https://github.com/scala-js/scala-js-dom
  lazy val vScalaJsReact = "1.7.6"     // https://github.com/japgolly/scalajs-react

  lazy val vWebJarsReact = "17.0.1"    // https://www.npmjs.com/package/react
  lazy val vReactWidgets = "4.6.1"     // https://www.npmjs.com/package/react-widgets
  lazy val vWebJarsFlux = "3.1.3"      // https://www.npmjs.com/package/flux
  lazy val vReactWidgetsMoment = "4.0.30"  // https://www.npmjs.com/package/react-widgets-moment
  lazy val vMoment = "2.29.1"          // https://www.npmjs.com/package/moment
  lazy val vGlobalize = "1.6.0"               // https://www.npmjs.com/package/globalize
  lazy val vReactWidgetsGlobalize = "5.0.22"  // https://www.npmjs.com/package/react-widgets-globalize
  lazy val vCldrjs = "0.5.4"                  // https://www.npmjs.com/package/cldrjs
  lazy val vCldr = "5.7.0"                    // https://www.npmjs.com/package/cldr
  lazy val vCldrData = "36.0.0"               // https://www.npmjs.com/package/cldr-data

  lazy val vJQuery = "3.5.1"         // https://www.npmjs.com/package/jquery

  lazy val vScalactic = "3.2.2"      // https://github.com/scalatest/scalatest
  lazy val vScalatest = "3.2.2"      // https://github.com/scalatest/scalatest
  lazy val vScalatestSelenium = "3.2.2.0"  // https://github.com/scalatest/scalatestplus-selenium
  lazy val vJunit = "4.13.1"         // https://github.com/junit-team/junit4

  lazy val vSelenium = "3.141.59"    // https://github.com/SeleniumHQ/selenium
  lazy val vScallop = "3.5.1"        // https://github.com/scallop/scallop
  lazy val vSlf4j = "1.7.30"         // https://github.com/qos-ch/slf4j
  lazy val vPlayJson = "2.9.1"       // https://github.com/playframework/play-json  from https://github.com/mliarakos/play-json/tree/feature/scalajs-1.0  local build

  // jackson-module-scala usually updates a few days after the others are updated,
  // don't update until jackson-module-scala is updated
  lazy val vJackson = "2.11.3"           // https://github.com/FasterXML/jackson-core
  lazy val vJacksonDatabind = "2.11.3"   // https://github.com/FasterXML/jackson-databind

  // Selenium needs to be update to update to v23.0
  lazy val vGuavaJre = "30.0-jre"    // https://github.com/google/guava

  lazy val vWebPack = "4.44.2"          // https://www.npmjs.com/package/webpack

  lazy val vJsDom = "16.4.0"           // https://www.npmjs.com/package/jsdom

  // version 0.2.3 is hardcoded in sbt-scalajs-bundler
  // current is 1.1.2   0.2.4
  lazy val vSourceMapLoader = "0.2.4"   // https://www.npmjs.com/package/source-map-loader
  // version 1.0.7 is hardcoded in sbt-scalajs-bundler
  // current is 1.1.0
  lazy val vConcatWithSourcemaps = "1.1.0"  // https://www.npmjs.com/package/concat-with-sourcemaps
  lazy val vTerser = "5.3.8"                // https://www.npmjs.com/package/terser

  lazy val vAjv = "6.12.6"                  // https://www.npmjs.com/package/ajv


  lazy val vWebpackDevServer = "3.11.0"   // https://www.npmjs.com/package/webpack-dev-server
  lazy val vWebPackCli = "3.3.12"         // https://www.npmjs.com/package/webpack-cli

  lazy val vSangria = "2.0.1"              // https://github.com/sangria-graphql/sangria
  lazy val vSangriaPlayJson = "2.0.1"      // https://github.com/sangria-graphql/sangria-play-json

  // graphql, graphiql, and graphql-voyager must be updated together.
  lazy val vGraphQL = "15.4.0"              // https://www.npmjs.com/package/graphql    co-req of graphql-voyager
  lazy val vGraphiQL = "1.0.6"              // https://www.npmjs.com/package/graphiql
  lazy val vGraphQLVoyager = "1.0.0-rc.31"  // https://www.npmjs.com/package/graphql-voyager
  lazy val vMaterialUIcore = "4.11.0"       // https://www.npmjs.com/package/@material-ui/core
  lazy val vMaterialUIicons = "4.9.1"       // https://www.npmjs.com/package/@material-ui/icons
  lazy val vPropTypes = "15.7.2"            // https://www.npmjs.com/package/prop-types
}
