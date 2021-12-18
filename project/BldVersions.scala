

object BldVersion {

  lazy val verScalaVersion = "2.13.7"
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


  lazy val vAkka = "2.6.17"            // https://github.com/akka/akka
  lazy val vAkkaHttp = "10.2.7"        // https://github.com/akka/akka-http

  lazy val vAkkaHttpPlayJson="1.38.2"  // https://github.com/hseeberger/akka-http-json

  lazy val vSwaggerAkkaHttp = "2.6.0"  // https://github.com/swagger-akka-http/swagger-akka-http
  lazy val vSwaggerScalaModule="2.5.2" // https://github.com/swagger-akka-http/swagger-scala-module
  lazy val vSwagger="2.1.11"           // https://github.com/swagger-api/swagger-core
  lazy val vWsRsApi="3.0.0"            // https://github.com/eclipse-ee4j/jaxrs-api
  lazy val vAkkaHttpCors = "1.1.2"     // https://github.com/lomigmegard/akka-http-cors

  lazy val vSwaggerUI = "4.1.3"        // https://www.npmjs.com/package/swagger-ui-dist
  lazy val vScalajsdom = "2.0.0"       // https://github.com/scala-js/scala-js-dom
  lazy val vScalaJsReact = "2.0.0"     // https://github.com/japgolly/scalajs-react

  lazy val vWebJarsReact = "17.0.2"    // https://www.npmjs.com/package/react
  lazy val vReactWidgets = "4.6.1"     // https://www.npmjs.com/package/react-widgets
  lazy val vWebJarsFlux = "4.0.3"      // https://www.npmjs.com/package/flux
  lazy val vReactWidgetsMoment = "4.0.30"  // https://www.npmjs.com/package/react-widgets-moment
  lazy val vMoment = "2.29.1"          // https://www.npmjs.com/package/moment
  lazy val vGlobalize = "1.6.0"               // https://www.npmjs.com/package/globalize
  lazy val vReactWidgetsGlobalize = "5.0.22"  // https://www.npmjs.com/package/react-widgets-globalize
  lazy val vCldrjs = "0.5.4"                  // https://www.npmjs.com/package/cldrjs
  lazy val vCldr = "5.7.0"                    // https://www.npmjs.com/package/cldr
  lazy val vCldrData = "36.0.0"               // https://www.npmjs.com/package/cldr-data

  lazy val vJQuery = "3.6.0"         // https://www.npmjs.com/package/jquery

  lazy val vScalactic = "3.2.10"      // https://github.com/scalatest/scalatest
  lazy val vScalatest = "3.2.10"      // https://github.com/scalatest/scalatest
  lazy val vScalatestSelenium = "3.2.10.0"  // https://github.com/scalatest/scalatestplus-selenium
  lazy val vJunit = "4.13.2"         // https://github.com/junit-team/junit4

  lazy val vSelenium = "4.1.0"         // https://github.com/SeleniumHQ/selenium

  lazy val vScallop = "4.1.0"          // https://github.com/scallop/scallop
  lazy val vSlf4j = "1.7.32"           // https://github.com/qos-ch/slf4j
  lazy val vPlayJson = "2.9.2"         // https://github.com/playframework/play-json

  // jackson-module-scala usually updates a few days after the others are updated,
  // don't update until jackson-module-scala is updated
  lazy val vJackson = "2.13.0"           // https://github.com/FasterXML/jackson-core
  lazy val vJacksonDatabind = "2.13.0"   // https://github.com/FasterXML/jackson-databind

  // Selenium needs to be update to update to v23.0
  lazy val vGuavaJre = "31.0.1-jre"    // https://github.com/google/guava

  lazy val vWebPack = "4.44.2"          // https://www.npmjs.com/package/webpack

  lazy val vJsDom = "18.1.1"           // https://www.npmjs.com/package/jsdom

  // version 0.2.3 is hardcoded in sbt-scalajs-bundler
  // current is 1.1.3   0.2.4
  lazy val vSourceMapLoader = "0.2.4"   // https://www.npmjs.com/package/source-map-loader
  // version 1.0.7 is hardcoded in sbt-scalajs-bundler
  // current is 1.1.0
  lazy val vConcatWithSourcemaps = "1.1.0"  // https://www.npmjs.com/package/concat-with-sourcemaps
  lazy val vTerser = "5.10.0"                // https://www.npmjs.com/package/terser

  lazy val vAjv = "8.8.2"                   // https://www.npmjs.com/package/ajv


  lazy val vWebpackDevServer = "3.11.0"   // https://www.npmjs.com/package/webpack-dev-server
  lazy val vWebPackCli = "3.3.12"         // https://www.npmjs.com/package/webpack-cli

  lazy val vSangria = "2.1.6"              // https://github.com/sangria-graphql/sangria
  lazy val vSangriaPlayJson = "2.0.2"      // https://github.com/sangria-graphql/sangria-play-json

  // graphql, graphiql, and graphql-voyager must be updated together.
  lazy val vGraphQL = "16.1.0"              // https://www.npmjs.com/package/graphql    co-req of graphql-voyager
  lazy val vGraphiQL = "1.5.16"             // https://www.npmjs.com/package/graphiql
  lazy val vGraphQLVoyager = "1.0.0-rc.31"  // https://www.npmjs.com/package/graphql-voyager

  lazy val vMaterialUIcore = "5.2.3"        // https://www.npmjs.com/package/@mui/material
  lazy val vMaterialUIicons = "5.2.1"       // https://www.npmjs.com/package/@mui/icons-material
  lazy val vEmotionReact = "11.7.0"         // https://www.npmjs.com/package/@emotion/react
  lazy val vEmotionStyled = "11.6.0"        // https://www.npmjs.com/package/@emotion/styled

  lazy val vPropTypes = "15.7.2"            // https://www.npmjs.com/package/prop-types

  val vRedux = "4.0.4"        // https://github.com/reduxjs/redux
  val vReduxThunk = "2.3.0"   // https://github.com/reduxjs/redux-thunk

  lazy val vJss = "10.9.0"                  // https://www.npmjs.com/package/jss

  lazy val vJsMacroTaskExecutor = "1.0.0"   // https://github.com/scala-js/scala-js-macrotask-executor
}
