

object BldVersion {

  lazy val verScalaVersion = "2.12.9"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq("2.12.8", verScalaVersion)

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


  lazy val vAkka = "2.5.25"           // https://github.com/akka/akka
  lazy val vAkkaHttp = "10.1.9"       // https://github.com/akka/akka-http

  lazy val vAkkaHttpPlayJson="1.27.0"  // https://github.com/hseeberger/akka-http-json

  lazy val vSwaggerAkkaHttp = "2.0.3"  // https://github.com/swagger-akka-http/swagger-akka-http
  lazy val vSwaggerScalaModule="2.0.4" // https://github.com/swagger-api/swagger-scala-module
  lazy val vSwagger="2.0.9"            // https://github.com/swagger-api/swagger-core
  lazy val vWsRsApi="2.1.5"            // https://github.com/eclipse-ee4j/jaxrs-api
  lazy val vAkkaHttpCors = "0.4.1"     // https://github.com/lomigmegard/akka-http-cors

  lazy val vSwaggerUI = "3.23.6"       // https://www.npmjs.com/package/swagger-ui-dist
  lazy val vScalajsdom = "0.9.7"       // https://github.com/scala-js/scala-js-dom
  lazy val vScalaJsReact = "1.4.2"     // https://github.com/japgolly/scalajs-react

  lazy val vWebJarsReact = "16.9.0"    // https://www.npmjs.com/package/react
  lazy val vReactWidgets = "4.4.11"    // https://www.npmjs.com/package/react-widgets
  lazy val vWebJarsFlux = "3.1.3"      // https://www.npmjs.com/package/flux
  lazy val vCldr = "4.7.0"             // https://www.npmjs.com/package/cldr
  lazy val vReactWidgetsMoment = "4.0.27"  // https://www.npmjs.com/package/react-widgets-moment
  lazy val vMoment = "2.24.0"          // https://www.npmjs.com/package/moment

  lazy val vJqueryFacade = "1.2"     // https://github.com/jducoeur/jquery-facade

  lazy val vJQuery = "3.4.1"         // https://www.npmjs.com/package/jquery

  lazy val vScalactic = "3.0.8"      // https://github.com/scalatest/scalatest
  lazy val vScalatest = "3.0.8"      // https://github.com/scalatest/scalatest
  lazy val vJunit = "4.12"           // https://github.com/junit-team/junit4

  lazy val vSelenium = "3.141.59"    // https://github.com/SeleniumHQ/selenium
  lazy val vScalaArm = "2.0"         // https://github.com/jsuereth/scala-arm
  lazy val vScallop = "3.3.1"        // https://github.com/scallop/scallop
  lazy val vSlf4j = "1.7.28"         // https://github.com/qos-ch/slf4j
  lazy val vPlayJson = "2.7.4"       // https://github.com/playframework/play-json

  // jackson-module-scala usually updates a few days after the others are updated,
  // don't update until jackson-module-scala is updated
  lazy val vJackson = "2.9.9"           // https://github.com/FasterXML/jackson-core
  // v2.9.9.2 causes NPE in getting swagger.yaml
  // https://github.com/FasterXML/jackson-databind/commit/dd4c5acb321a6aa9ca230aa505266fb2dd2f90ff
  lazy val vJacksonDatabind = "2.9.9.3"   // https://github.com/FasterXML/jackson-databind

  // Selenium needs to be update to update to v23.0
  lazy val vGuavaJre = "28.1-jre"    // https://github.com/google/guava

  lazy val vWebPack = "4.39.3"          // https://www.npmjs.com/package/webpack

  lazy val vJsDom = "15.1.1"           // https://www.npmjs.com/package/jsdom

  // version 0.2.3 is hardcoded in sbt-scalajs-bundler
  // current is 0.2.4
  lazy val vSourceMapLoader = "0.2.4"   // https://www.npmjs.com/package/source-map-loader
  // version 1.0.7 is hardcoded in sbt-scalajs-bundler
  // current is 1.1.0
  lazy val vConcatWithSourcemaps = "1.1.0"  // https://www.npmjs.com/package/concat-with-sourcemaps
  lazy val vTerser = "4.2.1"               // https://www.npmjs.com/package/terser

  lazy val vAjv = "6.10.2"                  // https://www.npmjs.com/package/ajv


  lazy val vWebpackDevServer = "3.8.0"   // https://www.npmjs.com/package/webpack-dev-server
  lazy val vWebPackCli = "3.3.7"         // https://www.npmjs.com/package/webpack-cli

  lazy val vSangria = "1.4.2"           // https://github.com/sangria-graphql/sangria
  lazy val vSangriaPlayJson = "1.0.5"   // https://github.com/sangria-graphql/sangria-playground

  lazy val vGraphQL = "14.5.4"              // https://github.com/graphql/graphql-js
  lazy val vGraphiQL = "0.14.2"             // https://github.com/graphql/graphiql
  lazy val vGraphQLVoyager = "1.0.0-rc.27"  // https://github.com/APIs-guru/graphql-voyager
  lazy val vMaterialUIcore = "4.3.3"        // https://www.npmjs.com/package/@material-ui/core
  lazy val vMaterialUIicons = "4.2.1"        // https://www.npmjs.com/package/@material-ui/icons

}
