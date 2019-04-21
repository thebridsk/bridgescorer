# Bridge Scorer Project

[![Build Status](https://travis-ci.org/thebridsk/bridgescorer.svg?branch=master)](https://travis-ci.org/thebridsk/bridgescorer)
[![GitHub release](https://img.shields.io/github/release/thebridsk/bridgescorer.svg)](https://github.com/thebridsk/bridgescorer/releases/latest)
[![ZenHub](https://img.shields.io/badge/Managed_with-ZenHub-5e60ba.svg)](https://app.zenhub.com/workspace/o/thebridsk/bridgescorer/boards)

## Demo

See the demo of the BridgeScorer [here](https://thebridsk.github.io/bridgescorerdemo/public/demo.html).

## Contributing

Use pull requests

## Releasing

To release a new version, the current branch must be `master`, the workspace must be clean.  The `release` branch must not exist.

To create the release, execute:

	sbt release

Then push the release branch and make a pull request.  Once the [Travis CI](https://travis-ci.org/thebridsk/bridgescorer) build finishes merge the pull request, and then push the tag that was created with the `sbt release` command.

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

	cd jvm
	md key
	cd key
	..\generateServerKey.bat abcdef xyzabc
	copy example.com.p12 ..\src\main\resources\keys\

Warning: The above is **NOT** secure, the password for the private key is hardcoded in Server.scala.
This only shows how to make a server that supports HTTPS.

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
- [Scala 2.12.8](http://www.scala-lang.org/)
- [SBT 1.2.7](http://www.scala-sbt.org/)
- [Chrome](https://www.google.com/chrome/)
- [ChromeDriver](https://sites.google.com/a/chromium.org/chromedriver/), be sure to match the version of Chrome.
- [Hugo](https://gohugo.io/), at least version 0.52

Optional:
- [Eclipse Oxygen](https://eclipse.org/)
- [Scala IDE](http://scala-ide.org/) [Update site](http://download.scala-ide.org/sdk/lithium/e46/scala211/stable/site)

## SBT Global Setup

- In the SBT install, edit the file `conf/sbtconfig.txt` and make the following changes:

  - Change `-Xmx` option to `-Xmx=4096M`.  512m is not enough.
  - Comment out `-XX:MaxPermSize=256m`.  Doesn't exist in Java 1.8 anymore.
    
- If you update SBT, you may need to clean out the `~/.sbt` directory.  Make sure you save `global.sbt`, `plugins.sbt` and any other configuration files.
- Optionally copy the files in `setup/sbt/1.0` to `~/.sbt/1.0`.  This has a `plugins.sbt` file with plugins that are nice to have.


## Setup for Eclipse

The following steps are needed to work in eclipse.  Note: the eclipse will need the [Scala IDE](http://scala-ide.org/) plugin installed.

- to generate the eclipse .project and .classpath files:

```
cd BridgeScorer/git/bridgescorer
sbt allassembly
sbt updateClassifiers "eclipse with-source=true" "reload plugins" updateClassifiers "eclipse with-source=true"
cd utilities
sbt "reload plugins" updateClassifiers "eclipse with-source=true"
```
    
- Import all projects except for one into eclipse starting at the BridgeScorer directory.  The one to not import into eclipse is one of the `utilities-shared` project, the one that has a directory path that ends in `js`.

### Eclipse preferences:

Turn off Eclipse > Preferences > Team > Git > Projects > "Automatically ignore derived resources by adding them to .gitignore"
See https://github.com/typesafehub/sbteclipse/issues/271

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

