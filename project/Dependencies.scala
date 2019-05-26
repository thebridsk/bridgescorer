
import sbt._
import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

// Scala.js additions, see http://www.scala-js.org/doc/project/
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

// see http://www.scala-sbt.org/0.13/docs/Organizing-Build.html

object Dependencies {
  // version numbers

  lazy val verScalaVersion = "2.12.8"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq("2.11.8", verScalaVersion)

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

  lazy val vAkkaHttpPlayJson="1.25.2"  // https://github.com/hseeberger/akka-http-json

  lazy val vSwaggerAkkaHttp = "2.0.2"  // http://mvnrepository.com/artifact/com.github.swagger-akka-http/swagger-akka-http_2.12
  lazy val vSwaggerScalaModule="2.0.4" // http://mvnrepository.com/artifact/io.swagger/swagger-scala-module_2.11
  lazy val vSwagger="2.0.8"            // http://mvnrepository.com/artifact/io.swagger.core.v3/swagger-core
  lazy val vWsRsApi="2.1.5"            // https://github.com/eclipse-ee4j/jaxrs-api
  lazy val vAkkaHttpCors = "0.4.0"     // https://github.com/lomigmegard/akka-http-cors

  lazy val vSwaggerUI = "3.22.2"       // https://www.npmjs.com/package/swagger-ui-dist
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
  lazy val vScalactic = "3.0.6"      // https://mvnrepository.com/artifact/org.scalactic/scalactic_2.12
  lazy val vScalatest = "3.0.6"      // http://mvnrepository.com/artifact/org.scalatest/scalatest_2.11
  lazy val vJunit = "4.12"           // http://mvnrepository.com/artifact/junit/junit

  lazy val vSelenium = "3.141.59"    // http://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
  lazy val vLog4js = "1.4.15"        // http://mvnrepository.com/artifact/org.webjars/log4javascript
  lazy val vScalaArm = "2.0"         // http://mvnrepository.com/artifact/com.jsuereth/scala-arm_2.11
  lazy val vScallop = "3.3.0"        // http://mvnrepository.com/artifact/org.rogach/scallop_2.11
  lazy val vSlf4j = "1.7.26"         // https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14
  lazy val vPlayJson = "2.7.3"       // https://mvnrepository.com/artifact/com.typesafe.play/play-json_2.12

  // jackson-module-scala usually updates a few days after the others are updated,
  // don't update until jackson-module-scala is updated
  lazy val vJackson = "2.9.9"        // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core

  // Selenium needs to be update to update to v23.0
  lazy val vGuavaJre = "27.1-jre"    // https://github.com/google/guava

  lazy val vWebPack = "4.32.2"          // https://www.npmjs.com/package/webpack

  lazy val vJsDom = "15.1.0"           // https://www.npmjs.com/package/jsdom
//  lazy val vExposeLoader = "0.7.3"     // https://www.npmjs.com/package/expose-loader

  // version 0.2.3 is hardcoded in sbt-scalajs-bundler
  // current is 0.2.4
  lazy val vSourceMapLoader = "0.2.4"   // https://www.npmjs.com/package/source-map-loader
  // version 1.0.7 is hardcoded in sbt-scalajs-bundler
  // current is 1.1.0
  lazy val vConcatWithSourcemaps = "1.1.0"  // https://www.npmjs.com/package/concat-with-sourcemaps
  lazy val vTerser = "4.0.0"               // https://www.npmjs.com/package/terser

  lazy val vAjv = "6.10.0"                  // https://www.npmjs.com/package/ajv


  lazy val vWebpackDevServer = "3.4.1"   // https://www.npmjs.com/package/webpack-dev-server
  lazy val vWebPackCli = "3.3.2"         // https://www.npmjs.com/package/webpack-cli

//  lazy val vFastClick = "1.0.6"       // https://www.npmjs.com/package/fastclick

  lazy val vSangria = "1.4.2"           // https://github.com/sangria-graphql/sangria
  lazy val vSangriaPlayJson = "1.0.5"   // https://github.com/sangria-graphql/sangria-playground

  lazy val vGraphQL = "14.3.1"              // https://github.com/graphql/graphql-js
  lazy val vGraphiQL = "0.13.0"             // https://github.com/graphql/graphiql
  lazy val vGraphQLVoyager = "1.0.0-rc.27"  // https://github.com/APIs-guru/graphql-voyager
  lazy val vMaterialUIcore = "4.0.0"        // https://www.npmjs.com/package/@material-ui/core
  lazy val vMaterialUIicons = "4.0.0"        // https://www.npmjs.com/package/@material-ui/icons

  // libraries


  val lScallop   = "org.rogach"    %%  "scallop"   % vScallop withSources()
  val lJunit = "junit"         %   "junit"     % vJunit  % "test" withSources()


  // projects

  // to use %%% you must be in a Def.setting
  // see https://github.com/vmunier/play-with-scalajs-example/issues/20
  // the use of the variable then needs to use bridgeScorerDeps.value

  val scalatestDeps = Def.setting(Seq(
      "org.scalatest" %%% "scalatest" % vScalatest % "test" withSources(),
      "org.scalactic" %%% "scalactic" % vScalactic % "test" withSources()
      ))

  val loggingDeps = Def.setting(scalatestDeps.value ++ Seq(
      "com.jsuereth" %% "scala-arm" % vScalaArm withSources()
      ))

  val utilitiesDeps = Def.setting(scalatestDeps.value ++ Seq(
      lScallop,
      lJunit
      ))

  val bridgeScorerRotationDeps = Def.setting(scalatestDeps.value)

  val bridgeScorerDeps = Def.setting(scalatestDeps.value ++ Seq(
      ))

  val pagesDeps = Def.setting(scalatestDeps.value ++ Seq(

      "org.seleniumhq.selenium" %   "selenium-java" % vSelenium withSources(),
      "org.seleniumhq.selenium" %   "selenium-api"  % vSelenium withSources(),
//      "org.seleniumhq.selenium" %   "selenium-support" % vSelenium withSources(),
//      "org.seleniumhq.selenium" %   "selenium-remote-driver" % vSelenium withSources(),
//      "org.seleniumhq.selenium" % "htmlunit-driver" % "2.34.0" withSources(),
// Test
      lJunit

      ))

  val jacksons = Seq(
    "com.fasterxml.jackson.core" % "jackson-core",
    "com.fasterxml.jackson.core" % "jackson-annotations",
    "com.fasterxml.jackson.core" % "jackson-databind",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
 ).map( _ % vJackson withSources())

 val morejacksons = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml",
    "com.fasterxml.jackson.module" %% "jackson-module-scala",
    "com.fasterxml.jackson.module" % "jackson-module-paranamer"
  ).map(_ % vJackson withSources())

  val bridgeScorerServerDeps = Def.setting(morejacksons ++ jacksons ++ Seq(

      "com.typesafe.akka"   %%  "akka-actor"    % vAkka withSources(),
//      "com.typesafe.akka"   %%  "akka-contrib"  % vAkka withSources(),   // needed for logging

      "com.typesafe.akka"   %% "akka-stream"             % vAkka withSources(),

//      "com.typesafe.akka"   %% "akka-http-core"          % vAkkaHttp withSources(),
      "com.typesafe.akka"   %% "akka-http"               % vAkkaHttp withSources(),
      "com.typesafe.akka"   %% "akka-http-caching"       % vAkkaHttp withSources(),
//      "com.typesafe.akka"   %% "akka-http2-support"      % vAkkaHttp % "test" withSources(),
      "de.heikoseeberger"   %% "akka-http-play-json"     % vAkkaHttpPlayJson withSources(),

//      (
      "com.github.swagger-akka-http" %%  "swagger-scala-module" % vSwaggerScalaModule withSources(),
//      ).exclude("com.google.code.findbugs","jsr305"),
      "com.github.swagger-akka-http" %% "swagger-akka-http" % vSwaggerAkkaHttp withSources(),
      "ch.megard" %% "akka-http-cors" % vAkkaHttpCors withSources(),

      "org.sangria-graphql" %% "sangria" % vSangria withSources(),
      "org.sangria-graphql" %% "sangria-relay" % vSangria withSources(),
      "org.sangria-graphql" %% "sangria-play-json" % vSangriaPlayJson withSources(),

      "io.swagger.core.v3" % "swagger-core" % vSwagger withSources(),
      "io.swagger.core.v3" % "swagger-annotations" % vSwagger withSources(),
      "io.swagger.core.v3" % "swagger-models" % vSwagger withSources(),
      "io.swagger.core.v3" % "swagger-jaxrs2" % vSwagger withSources(),

      "jakarta.ws.rs" % "jakarta.ws.rs-api" % vWsRsApi withSources(),

//      "org.webjars.npm" % "react-widgets" % vReactWidgets,

      // webjar is included by sbt-web
//      "org.webjars" % "swagger-ui" % vSwaggerUI % "provided",
      "com.jsuereth" %% "scala-arm" % vScalaArm withSources(),
      lScallop,
      "org.slf4j" % "slf4j-jdk14" % vSlf4j withSources(),

      // This is required to make integrations test work.  Selenium 3.2.0 uses guava 21.0
      "com.google.guava" % "guava" % vGuavaJre withSources(),
// Test
      lJunit,

      "com.typesafe.akka"   %% "akka-http-xml"           % vAkkaHttp % "test" withSources(),

      "com.typesafe.akka"       %%  "akka-testkit"  % vAkka      % "test" withSources(),
      "com.typesafe.akka"       %%  "akka-stream-testkit"  % vAkka      % "test" withSources(),

      "com.typesafe.akka"       %% "akka-http-testkit" % vAkkaHttp % "test" withSources(),

      "org.seleniumhq.selenium" %   "selenium-java" % vSelenium  % "test" withSources(),
//      "org.seleniumhq.selenium" %   "selenium-api"  % vSelenium  % "test" withSources(),
//      "org.seleniumhq.selenium" %   "selenium-support" % vSelenium  % "test" withSources()
//      "org.seleniumhq.selenium" % "htmlunit-driver" % "2.34.0" % "test" withSources(),

      ))

  val bridgeScorerSharedDeps = Def.setting(Seq(
      "com.typesafe.play" %%% "play-json" % vPlayJson withSources(),
      "io.swagger.core.v3" % "swagger-annotations" % vSwagger withSources(),
      "org.scalactic" %%% "scalactic" % vScalactic withSources()
      ))


  val bridgeScorerSharedJVMDeps = Def.setting( jacksons )

  val materialUiDeps = Def.setting(Seq(

      "com.github.japgolly.scalajs-react" %%% "core"          % vScalaJsReact withSources(),
      "com.github.japgolly.scalajs-react" %%% "extra"         % vScalaJsReact withSources(),

      "com.github.japgolly.scalajs-react" %%% "test" % vScalaJsReact % "test" withSources()
      ))

  val bridgeScorerClientDeps = Def.setting(Seq(

      "org.scala-js" %%%  "scalajs-dom"    % vScalajsdom withSources(),
      "io.swagger.core.v3" % "swagger-annotations" % vSwagger withSources(),

      "com.github.japgolly.scalajs-react" %%% "core"          % vScalaJsReact withSources(),
//      "com.github.japgolly.scalajs-react" %%% "ext-scalaz71"  % vScalaJsReact withSources(),
      "com.github.japgolly.scalajs-react" %%% "extra"         % vScalaJsReact withSources(),
//      "com.github.japgolly.scalacss"      %%% "core"          % vScalaCss withSources(),
//      "com.github.japgolly.scalacss"      %%% "ext-react"     % vScalaCss withSources(),

//      "be.doeraene"  %%%  "scalajs-jquery" % vScalajsJquery % "test" withSources(),
      "org.querki" %%% "jquery-facade" % vJqueryFacade % "test" withSources(),

      "com.github.japgolly.scalajs-react" %%% "test" % vScalaJsReact % "test" withSources()
      ))

  val bridgeScorerNpmDeps = Seq(
      "react" -> vWebJarsReact,
      "react-dom" -> vWebJarsReact,
      "flux" -> vWebJarsFlux,
      "react-widgets" -> vReactWidgets,
      "react-widgets-moment" -> vReactWidgetsMoment,
      "moment" -> vMoment,
      "swagger-ui-dist" -> vSwaggerUI,
//      "fastclick" -> vFastClick,
      "graphql-voyager" -> vGraphQLVoyager,
      "graphql" -> vGraphQL,
      "graphiql" -> vGraphiQL,
      "@material-ui/core" -> vMaterialUIcore,
      "@material-ui/icons" -> vMaterialUIicons
  )

    // this is for SBT 1.0
    // 11/18/17, 12/4/17 currently does not work, looks like JSDOM is not loaded
    // see https://github.com/scalacenter/scalajs-bundler/issues/181
    // error is navigator undefined

  val bridgeScorerTestNpmDeps = Seq(
      "jsdom" -> vJsDom,
      "jquery" -> vJQuery,
  )

  val bridgeScorerDevNpmDeps = Seq(
      "webpack" -> vWebPack,
//      "source-map-loader" -> vSourceMapLoader,
//      "concat-with-sourcemaps" -> vConcatWithSourcemaps,
      "terser" -> vTerser,
      "ajv" -> vAjv
//      "expose-loader" -> vExposeLoader
  )

  def bridgeScorerNpmAssets(client: ProjectReference) = {
    npmAssets ++= NpmAssets.ofProject(client) { nodeModules =>
      (nodeModules / "react-widgets" / "dist" / "css" ).allPaths +++
      (nodeModules / "react-widgets" / "dist" / "fonts" ).allPaths +++
      (nodeModules / "react-widgets" / "dist" / "img" ).allPaths +++
      (nodeModules / "graphql-voyager" / "dist" ) **(
          new ExactFilter("voyager.worker.js") |
          new ExactFilter("voyager.css")
          ) +++
      (nodeModules / "graphiql" ) **(
          new ExactFilter("graphiql.css")
          ) +++
      nodeModules / "swagger-ui-dist" ** (
          new PatternFilter("""swagger-ui.*""".r.pattern) |
          new ExactFilter("index.html")
          )
    }.value
  }

  val bridgeScorerJsDeps = Seq(
//      "org.webjars.npm" % "react" % vWebJarsReact / "react-with-addons.js"  minified "react-with-addons.min.js" commonJSName "React",
//      "org.webjars.npm" % "react-dom" % vWebJarsReact / "react-dom.js"          minified "react-dom.min.js"         dependsOn "react-with-addons.js" commonJSName "ReactDOM",
//      "org.webjars.npm" % "react-dom" % vWebJarsReact / "react-dom-server.js"   minified "react-dom-server.min.js"  dependsOn "react-dom.js" commonJSName "ReactDOMServer",
//      "org.webjars.npm" % "flux" % vWebJarsFlux / "Flux.js" minified "Flux.min.js" commonJSName "flux",
//      "org.webjars" % "log4javascript" % vLog4js / "js/log4javascript_uncompressed.js" % "compile",
//      "org.webjars" % "log4javascript" % vLog4js / "js/stubs/log4javascript_uncompressed.js" % "test",
//      "org.webjars" % "log4javascript" % vLog4js / "js/log4javascript.js",

//      "org.webjars.npm" % "react-widgets" % vReactWidgets / "react-widgets.js", // commonJSName "ReactWidgets",

      )
}
