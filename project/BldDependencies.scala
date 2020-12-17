
import sbt._
import Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSCrossVersion
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
      // "com.jsuereth" %% "scala-arm" % vScalaArm withSources()
      ))

  val utilitiesDeps = Def.setting(scalatestDeps.value ++ Seq(
      lScallop,
      lJunit
      ))

  val bridgeScorerRotationDeps = Def.setting(scalatestDeps.value)

  val bridgeScorerColorDeps = Def.setting(scalatestDeps.value)

  val bridgeScorerDeps = Def.setting(scalatestDeps.value ++ Seq(
      ))

  val jacksonDatabind = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind",
  ).map( _ % vJacksonDatabind withSources())

  val jacksons = Seq(
    "com.fasterxml.jackson.core" % "jackson-core",
    "com.fasterxml.jackson.core" % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
 ).map( _ % vJackson withSources()) ++ jacksonDatabind

 val morejacksons = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml",
    "com.fasterxml.jackson.module" %% "jackson-module-scala",
    "com.fasterxml.jackson.module" % "jackson-module-paranamer"
  ).map(_ % vJackson withSources())

  val browserPagesDeps = Def.setting(scalatestDeps.value ++ Seq(
    "org.seleniumhq.selenium" %   "selenium-java" % vSelenium withSources(),

    "org.scalatest" %%% "scalatest" % vScalatest withSources(),
    "org.scalatestplus" %%% "selenium-3-141" % vScalatestSelenium withSources(),
    "org.scalactic" %%% "scalactic" % vScalactic withSources(),

// Test
      lJunit,

      ))

  val bridgeScorerServerDeps = Def.setting(morejacksons ++ jacksons ++ Seq(

      "com.typesafe.akka"   %% "akka-actor"              % vAkka withSources(),
      // "com.typesafe.akka"   %% "akka-actor-typed"        % vAkka withSources(),
      "com.typesafe.akka"   %% "akka-stream"             % vAkka withSources(),
      "com.typesafe.akka"   %% "akka-slf4j"              % vAkka withSources(),
      "com.typesafe.akka"   %% "akka-http"               % vAkkaHttp withSources(),
      "com.typesafe.akka"   %% "akka-http-caching"       % vAkkaHttp withSources(),
      // add http2 support to test only for now.  annoying if cert from trusted CA is not used
      "com.typesafe.akka"   %% "akka-http2-support"      % vAkkaHttp % "test" withSources(),
      "de.heikoseeberger"   %% "akka-http-play-json"     % vAkkaHttpPlayJson withSources(),

      "com.github.swagger-akka-http" %%  "swagger-scala-module" % vSwaggerScalaModule withSources(),
      "com.github.swagger-akka-http" %% "swagger-akka-http" % vSwaggerAkkaHttp withSources(),
      "ch.megard" %% "akka-http-cors" % vAkkaHttpCors withSources(),

      "org.sangria-graphql" %% "sangria" % vSangria withSources(),
      // "org.sangria-graphql" %% "sangria-relay" % vSangria withSources(),
      "org.sangria-graphql" %% "sangria-play-json" % vSangriaPlayJson withSources(),

      "io.swagger.core.v3" % "swagger-core" % vSwagger withSources(),
      "io.swagger.core.v3" % "swagger-annotations" % vSwagger withSources(),
      "io.swagger.core.v3" % "swagger-models" % vSwagger withSources(),
      "io.swagger.core.v3" % "swagger-jaxrs2" % vSwagger withSources(),

      "jakarta.ws.rs" % "jakarta.ws.rs-api" % vWsRsApi withSources(),

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

      ))

  val bridgeScorerFullServerDeps = Def.setting(morejacksons ++ jacksons ++ Seq(

// Test
    lJunit,

    "com.typesafe.akka"   %% "akka-http-xml"           % vAkkaHttp % "test" withSources(),

    "com.typesafe.akka"       %%  "akka-testkit"  % vAkka      % "test" withSources(),
    "com.typesafe.akka"       %%  "akka-stream-testkit"  % vAkka      % "test" withSources(),

    "com.typesafe.akka"       %% "akka-http-testkit" % vAkkaHttp % "test" withSources(),

    "org.seleniumhq.selenium" %   "selenium-java" % vSelenium  % "test" withSources(),

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


  val reduxDeps = Def.setting(Seq(

    "com.github.japgolly.scalajs-react" %%% "core"          % vScalaJsReact withSources(),
    "com.github.japgolly.scalajs-react" %%% "extra"         % vScalaJsReact withSources(),

    "org.scalatest" %%% "scalatest" % vScalatest withSources(),
    "org.scalactic" %%% "scalactic" % vScalactic withSources(),

    "com.github.japgolly.scalajs-react" %%% "test" % vScalaJsReact % "test" withSources()
    ))

  val clientcommonDeps = Def.setting(scalatestDeps.value ++ Seq(

    "com.github.japgolly.scalajs-react" %%% "core"          % vScalaJsReact withSources(),
    "com.github.japgolly.scalajs-react" %%% "extra"         % vScalaJsReact withSources(),

    "com.github.japgolly.scalajs-react" %%% "test" % vScalaJsReact % "test" withSources()
    ))

  val bridgeScorerClientDeps = Def.setting(Seq(

      "org.scala-js" %%%  "scalajs-dom"    % vScalajsdom withSources(),
      "io.swagger.core.v3" % "swagger-annotations" % vSwagger withSources(),

      "com.github.japgolly.scalajs-react" %%% "core"          % vScalaJsReact withSources(),
      "com.github.japgolly.scalajs-react" %%% "extra"         % vScalaJsReact withSources(),

      "com.github.japgolly.scalajs-react" %%% "test" % vScalaJsReact % "test" withSources()
      ))

  val bridgeScorerClientApiDeps = Def.setting(Seq(

    "org.scala-js" %%%  "scalajs-dom"    % vScalajsdom withSources(),
    "io.swagger.core.v3" % "swagger-annotations" % vSwagger withSources(),

    "com.github.japgolly.scalajs-react" %%% "core"          % vScalaJsReact withSources(),
    "com.github.japgolly.scalajs-react" %%% "extra"         % vScalaJsReact withSources(),

    "com.github.japgolly.scalajs-react" %%% "test" % vScalaJsReact % "test" withSources()
    ))

  val bridgeScorerDemoDeps = Seq()

  val bridgeScorerNpmDeps = Seq(
      "react" -> vWebJarsReact,
      "react-dom" -> vWebJarsReact,
      "flux" -> vWebJarsFlux,
      "react-widgets" -> vReactWidgets,
      "react-widgets-moment" -> vReactWidgetsMoment,
      "moment" -> vMoment,
      // "globalize" -> vGlobalize,
      // "react-widgets-globalize" -> vReactWidgetsGlobalize,
      // "cldrjs" -> vCldrjs,
      // "cldr" -> vCldr,
      // "cldr-data" -> vCldrData,
      "@material-ui/core" -> vMaterialUIcore,
      "@material-ui/icons" -> vMaterialUIicons
  )

  val bridgeScorerClientApiNpmDeps = Seq(
      "react" -> vWebJarsReact,
      "react-dom" -> vWebJarsReact,
      "flux" -> vWebJarsFlux,
      "react-widgets" -> vReactWidgets,
      "react-widgets-moment" -> vReactWidgetsMoment,
      "moment" -> vMoment,
      // "globalize" -> vGlobalize,
      // "react-widgets-globalize" -> vReactWidgetsGlobalize,
      // "cldrjs" -> vCldrjs,
      // // "cldr" -> vCldr,
      // "cldr-data" -> vCldrData,
      "swagger-ui-dist" -> vSwaggerUI,
      "graphql-voyager" -> vGraphQLVoyager,
      "graphql" -> vGraphQL,
      "graphiql" -> vGraphiQL,
      "@material-ui/core" -> vMaterialUIcore,
      "@material-ui/icons" -> vMaterialUIicons,
      "prop-types" -> vPropTypes
  )

    // this is for SBT 1.0
    // 11/18/17, 12/4/17 currently does not work, looks like JSDOM is not loaded
    // see https://github.com/scalacenter/scalajs-bundler/issues/181
    // error is navigator undefined

  val bridgeScorerTestNpmDeps = Seq(
      "jsdom" -> vJsDom,
      "jquery" -> vJQuery,
  )

  val bridgeScorerClientApiTestNpmDeps = Seq(
      "jsdom" -> vJsDom,
      "jquery" -> vJQuery,
  )

  val bridgeScorerDevNpmDeps = Seq(
      "webpack" -> vWebPack,
      "jquery" -> vJQuery,
//      "source-map-loader" -> vSourceMapLoader,
//      "concat-with-sourcemaps" -> vConcatWithSourcemaps,
      "terser" -> vTerser,
      "ajv" -> vAjv
  )


  val bridgeScorerClientApiDevNpmDeps = Seq(
      "webpack" -> vWebPack,
      "jquery" -> vJQuery,
//      "source-map-loader" -> vSourceMapLoader,
//      "concat-with-sourcemaps" -> vConcatWithSourcemaps,
      "terser" -> vTerser,
      "ajv" -> vAjv,
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

  )

  val reduxTestNpmDeps = Seq(
    "redux" -> vRedux,
    "redux-thunk" -> vReduxThunk
  )
}
