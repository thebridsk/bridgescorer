# Bridge Scorer Project

[![Build Status](https://travis-ci.com/thebridsk/bridgescorer.svg?branch=main)](https://travis-ci.com/thebridsk/bridgescorer)
[![GitHub release](https://img.shields.io/github/release/thebridsk/bridgescorer.svg)](https://github.com/thebridsk/bridgescorer/releases/latest)
[![ZenHub](https://img.shields.io/badge/Managed_with-ZenHub-5e60ba.svg)](https://app.zenhub.com/workspace/o/thebridsk/bridgescorer/boards)

## Demo

See the demo of the BridgeScorer [here](https://thebridsk.github.io/bridgescorerdemo/public/demo.html).

## Contributing

Use pull requests.  [Travis CI](https://travis-ci.com/thebridsk/bridgescorer) is used to test all pull requests prior to merging.

## Releasing

To release a new version, the current branch must be `main`, the workspace must be clean.  The `release` branch must not exist.

To create the release, execute:

	sbt release

Then push the release branch and make a pull request.  Once the [Travis CI](https://travis-ci.com/thebridsk/bridgescorer) build finishes merge the pull request, and then push the tag that was created with the `sbt release` command.

## Directory Structure

    client/
      src/
        main/
          scala/      the source for the browser application
    help/             the help project from https://github.com/thebridsk/bridgescorerdocs
    rotation/
      shared/
        src/
          main/
            scala/    a utility to help with bridge table rotations
    server/
      src/
        main/
          public/     static content for web application
          scala/      the server source code
          resources/  resources for the server
        test
          scala/      test code
      store/          persistent store for dev environment, git ignored
      testdata/       test data for integration tests
    shared/
      shared/
        src/
          main/
            scala/    shared code between web application and server
    utilities/        the utilities project from https://github.com/thebridsk/utilities
    project/
      Dependencies.scala            dependencies for all projects
      MyEclipseTransformers.scala   sbt transformers for eclipse
      MyReleaseVersion.scala        my version support for sbt-release
      plugins.sbt                   plugins needed by this project
      Server.scala                  support for running server in tests
    build.sbt         build file
    version.sbt       version file, maintained by build.sbt and sbt-release plugin
    setup/
      sbt/            files for global configuration of sbt
    launchers/        eclipse launchers for development environment

## Server Key for HTTPS Connection

To generate the server key for the HTTPS Connection run the following:

    md key
    cd key
    ..\generateServerKey.bat abcdef abcdef

Warning: The above is **NOT** secure, the password for the private key trivial.
This only shows how to make a server that supports HTTPS.

With this key, the following options will enable https and http2 when starting the server:

    --certificate key/example.com.p12 --certpassword abcdef --https 8443 --http2

The sbt build target `bridgescorer-server/serverssl` can be used to start the server with https support and `bridgescorer-server/serverhttp2` can be used to start the server with https and http2 support.  Note that the akka-http2-support library is added in the test scope, as is the serverhttp2 target.

## Development Environment

This project uses nested submodules.  The easiest way to clone is to use `clone --recurse-submodules`.

    mkdir BridgeScorer
    cd BridgeScorer
    mkdir git
    cd git
    git clone --recurse-submodules https://github.com/thebridsk/bridgescorer

The resulting directory structure is:

    BridgeScorer
      git
        bridgescorer
      ws

The `ws` directory is the eclipse workspace.

## Prereqs

- Java 1.8
- [Scala 2.12.10](http://www.scala-lang.org/)
- [SBT 1.3.0](http://www.scala-sbt.org/)
- [Chrome](https://www.google.com/chrome/)
- [ChromeDriver](https://sites.google.com/a/chromium.org/chromedriver/), be sure to match the version of Chrome.
- [Hugo](https://gohugo.io/), at least version 0.52

Optional:
- [Visual Studio Code](https://code.visualstudio.com/)
- [Scala Metals](https://scalameta.org/metals/)

## SBT Global Setup

- In the SBT install, edit the file `conf/sbtconfig.txt` and make the following changes:

  - Change `-Xmx` option to `-Xmx=4096M`.  512m is not enough.

- If you update SBT, you may need to clean out the `~/.sbt` directory.  Make sure you save `global.sbt`, `plugins.sbt` and any other configuration files.
- Optionally copy the files in `setup/sbt/1.0` to `~/.sbt/1.0`.  This has a `plugins.sbt` file with plugins that are nice to have.

## Setup for VSCode

Install the scalametals extension.  Add sbt, Scala, Java, chromedriver, hugo to the path when starting VSCode.

## Debugging iOS (Oct 28, 2017)

Use jsconsole from https://github.com/remy/jsconsole

### Start jsconsole

    git clone https://github.com/remy/jsconsole
    cd jsconsole
    npm install
    node .

### Browser to jsconsole

edit server/src/main/public/index-jsconsole.html to set the correct IP address for the jsconsole server.

on iOS Safari, go to /public/index-jsconsole.html

