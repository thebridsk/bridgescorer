

object BldVersion {

  lazy val verScalaVersion = "2.12.8"
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


  lazy val vAkka = "2.5.23"           // http://mvnrepository.com/artifact/com.typesafe.akka/akka-actor_2.11
  lazy val vAkkaHttp = "10.1.8"       // http://mvnrepository.com/artifact/com.typesafe.akka/akka-http_2.11

  lazy val vAkkaHttpPlayJson="1.27.0"  // https://github.com/hseeberger/akka-http-json

  lazy val vSwaggerAkkaHttp = "2.0.3"  // http://mvnrepository.com/artifact/com.github.swagger-akka-http/swagger-akka-http_2.12
  lazy val vSwaggerScalaModule="2.0.4" // http://mvnrepository.com/artifact/io.swagger/swagger-scala-module_2.11
  lazy val vSwagger="2.0.8"            // http://mvnrepository.com/artifact/io.swagger.core.v3/swagger-core
  lazy val vWsRsApi="2.1.5"            // https://github.com/eclipse-ee4j/jaxrs-api
  lazy val vAkkaHttpCors = "0.4.1"     // https://github.com/lomigmegard/akka-http-cors

  lazy val vSwaggerUI = "3.22.3"       // https://www.npmjs.com/package/swagger-ui-dist
  lazy val vScalajsdom = "0.9.7"       // http://mvnrepository.com/artifact/org.scala-js/scalajs-dom_sjs0.6_2.11
  lazy val vScalaJsReact = "1.4.2"     // http://mvnrepository.com/artifact/com.github.japgolly.scalajs-react/core_sjs0.6_2.11
  lazy val vScalaCss = "0.5.3"         // http://mvnrepository.com/artifact/com.github.japgolly.scalacss/core_sjs0.6_2.11

  lazy val vWebJarsReact = "16.8.6"    // http://mvnrepository.com/artifact/org.webjars/react
  lazy val vReactWidgets = "4.4.11"    // http://mvnrepository.com/artifact/org.webjars.npm/react-widgets
  lazy val vWebJarsFlux = "3.1.3"      // http://mvnrepository.com/artifact/org.webjars/flux
  lazy val vGlobalize = "1.3.0"        // https://www.npmjs.com/package/globalize
  lazy val vCldr = "4.7.0"             // https://www.npmjs.com/package/cldr
  lazy val vReactWidgetsMoment = "4.0.27"  // http://mvnrepository.com/artifact/org.webjars.npm/react-widgets-moment
  lazy val vMoment = "2.24.0"          // https://www.npmjs.com/package/moment

  lazy val vScalajsJquery = "0.9.2"  // http://mvnrepository.com/artifact/be.doeraene/scalajs-jquery_sjs0.6_2.11

  lazy val vJqueryFacade = "1.2"     // https://mvnrepository.com/artifact/org.querki/jquery-facade

  lazy val vJQuery = "3.4.1"         // https://www.npmjs.com/package/jquery

  // bug in scalatest 3.0.7 see https://github.com/scalatest/scalatest/issues/1561
  lazy val vScalactic = "3.0.8"      // https://mvnrepository.com/artifact/org.scalactic/scalactic_2.12
  lazy val vScalatest = "3.0.8"      // http://mvnrepository.com/artifact/org.scalatest/scalatest_2.11
  lazy val vJunit = "4.12"           // http://mvnrepository.com/artifact/junit/junit

  lazy val vSelenium = "3.141.59"    // http://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
  lazy val vLog4js = "1.4.15"        // http://mvnrepository.com/artifact/org.webjars/log4javascript
  lazy val vScalaArm = "2.0"         // http://mvnrepository.com/artifact/com.jsuereth/scala-arm_2.11
  lazy val vScallop = "3.3.1"        // http://mvnrepository.com/artifact/org.rogach/scallop_2.11
  lazy val vSlf4j = "1.7.26"         // https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14
  lazy val vPlayJson = "2.7.4"       // https://mvnrepository.com/artifact/com.typesafe.play/play-json_2.12

  // jackson-module-scala usually updates a few days after the others are updated,
  // don't update until jackson-module-scala is updated
  lazy val vJackson = "2.9.9"        // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core

  // Selenium needs to be update to update to v23.0
  lazy val vGuavaJre = "28.0-jre"    // https://github.com/google/guava

  lazy val vWebPack = "4.35.0"          // https://www.npmjs.com/package/webpack

  lazy val vJsDom = "15.1.1"           // https://www.npmjs.com/package/jsdom
//  lazy val vExposeLoader = "0.7.3"     // https://www.npmjs.com/package/expose-loader

  // version 0.2.3 is hardcoded in sbt-scalajs-bundler
  // current is 0.2.4
  lazy val vSourceMapLoader = "0.2.4"   // https://www.npmjs.com/package/source-map-loader
  // version 1.0.7 is hardcoded in sbt-scalajs-bundler
  // current is 1.1.0
  lazy val vConcatWithSourcemaps = "1.1.0"  // https://www.npmjs.com/package/concat-with-sourcemaps
  lazy val vTerser = "4.0.0"               // https://www.npmjs.com/package/terser

  lazy val vAjv = "6.10.0"                  // https://www.npmjs.com/package/ajv


  lazy val vWebpackDevServer = "3.7.2"   // https://www.npmjs.com/package/webpack-dev-server
  lazy val vWebPackCli = "3.3.5"         // https://www.npmjs.com/package/webpack-cli

//  lazy val vFastClick = "1.0.6"       // https://www.npmjs.com/package/fastclick

  lazy val vSangria = "1.4.2"           // https://github.com/sangria-graphql/sangria
  lazy val vSangriaPlayJson = "1.0.5"   // https://github.com/sangria-graphql/sangria-playground

  lazy val vGraphQL = "14.4.1"              // https://github.com/graphql/graphql-js
  lazy val vGraphiQL = "0.13.2"             // https://github.com/graphql/graphiql
  lazy val vGraphQLVoyager = "1.0.0-rc.27"  // https://github.com/APIs-guru/graphql-voyager
  lazy val vMaterialUIcore = "4.1.3"        // https://www.npmjs.com/package/@material-ui/core
  lazy val vMaterialUIicons = "4.2.1"        // https://www.npmjs.com/package/@material-ui/icons

}
