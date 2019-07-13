
import sbt._
import Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSCrossVersion
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{crossProject => _, CrossType => _, _}
import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin.autoImport._

import BldVersion._

object BldDependencies {
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
