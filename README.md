# Bridge Scorer Project

[![Build Status](https://travis-ci.org/thebridsk/bridgescorer.svg?branch=master)](https://travis-ci.org/thebridsk/bridgescorer)

## Releasing

To release a new version, the current branch must be `master`, the workspace must be clean.  The `release` branch must not exist.

To create the release, execute:

	sbt release

## Directory Structure

    client/
      src/
        main/
          scala/      the source for the browser application
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

To generate the server key for the HTTPS Connection run the

	cd jvm
	md key
	cd key
	..\generateServerKey.bat abcdef xyzabc
	copy example.com.p12 ..\src\main\resources\keys\

Warning: The following is **NOT** secure, the password for the private key is hardcoded in Server.scala.
This only shows how to make a server that supports HTTPS.

## Development Environment

    mkdir BridgeScorer
    cd BridgeScorer
    git clone <binBridgeScorerProject> bin
    git clone <testdataBridgeScorerProject> testdata
    mkdir git
    cd git
    git clone <utilitiesProject>
    git clone <bridgescorerProject>

The resulting directory structure is:

    BridgeScorer
      bin
      testdata
      git
        bridgescorer
        utilities
      ws

The `ws` directory is the eclipse workspace.

## Prereqs

- Java 1.8
- [Scala 2.12.2](http://www.scala-lang.org/)
- [SBT 0.13.15](http://www.scala-sbt.org/)
- [Chrome](https://www.google.com/chrome/)
- [ChromeDriver 2.29](https://sites.google.com/a/chromium.org/chromedriver/)
- [Eclipse Mars+](https://eclipse.org/)
- [Scala IDE](http://scala-ide.org/) [Update site](http://download.scala-ide.org/sdk/lithium/e46/scala211/stable/site)

## SBT Global Setup

- In the SBT install, edit the file `conf/sbtconfig.txt` and make the following changes:

  - Change `-Xmx` option to `-Xmx=4096M`.  512m is not enough.
  - Comment out `-XX:MaxPermSize=256m`.  Doesn't exist in Java 1.8 anymore.
    
- If you update SBT, you may need to clean out the `~/.sbt` directory.  Make sure you save `global.sbt`, `plugins.sbt` and any other configuration files.
- Copy the files in `setup/sbt/0.13` to `~/.sbt/0.13`.  This has a `global.sbt`, `plugins.sbt` files with plugins that are nice to have.


## Setup for Eclipse

The following steps are needed to work in eclipse.

- to generate the eclipse .project and .classpath files:

    cd BridgeScorer
    cd bridgescorer
    sbt "eclipse with-source=true" "reload plugins" "eclipse with-source=true"
    cd ../utilities
    sbt "reload plugins" "eclipse with-source=true"
    

- Import all projects into eclipse starting at the BridgeScorer directory.

- In the project-utilities add all the jars from the current SBT in `~\.sbt\boot\`.  As of Dec 2016, this was
`~\.sbt\boot\scala-2.10.6\org.scala-sbt\sbt\0.13.13`.

### Eclipse preferences:

Turn off Eclipse > Preferences > Team > Git > Projects > "Automatically ignore derived resources by adding them to .gitignore"
See https://github.com/typesafehub/sbteclipse/issues/271

## Debugging iOS

Use jsconsole from https://github.com/remy/jsconsole

### Start jsconsole

    npm install
    node .
 
### Browser to jsconsole

edit server/src/main/public/index-jsconsole.html to set the correct IP address for the jsconsole server.

on iOS Safari, go to /public/index-jsconsole.html

