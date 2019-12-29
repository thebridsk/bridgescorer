//
// To only build the debug version of the client code,
// define the environment variable "OnlyBuildDebug"
//
// To specify the browser to use for tests, define the
// environment variable "UseBrowser" to the browser to use.  Default: chrome
// supported browsers: chrome, chromeheadless, safari, firefox, edge, ie
//
// To check for updates, npm and maven
//   checkForUpdates
// To build just the bundle.js and copy them to `bridgescorer-server` project
//   webassembly
//   or
//   bridgescorer-server/*:assembly::assembledMappings
// To build all assembly
//   allassembly
//   or
//   assembly test:assembly
// To run unit tests
//   distribution:mytest
// To run all tests
//   distribution:alltests
// To make a new release
//   release
// To run standalone tests using already built jar files
//   bridgescorer-server/distribution:standalonetests
//   bridgescorer-server/distribution:fvt
//   bridgescorer-server/distribution:svt
//
// When testing help screens, this will only run the test case that generates images for help
//   set BUILDFORHELPONLY=true
//   sbt webassembly
//
// to get times for tasks,
//    set JAVA_OPTS=-Dsbt.task.timings=true

bloopExportJarClassifiers in Global := Some(Set("sources"))

lazy val browserpages: Project = BldBrowserPages.browserpages

lazy val sharedJS: Project = BldBridgeShared.sharedJS
lazy val sharedJVM = BldBridgeShared.sharedJVM

lazy val rotationJS: Project = BldBridgeRotation.rotationJS
lazy val rotationJVM = BldBridgeRotation.rotationJVM

lazy val colorJS: Project = BldColor.colorJS
lazy val colorJVM: Project = BldColor.colorJVM

lazy val materialui = BldMaterialUI.materialui
lazy val `bridgescorer-clientcommon` = BldBridgeClientCommon.`bridgescorer-clientcommon`
lazy val `bridgescorer-clientapi` = BldBridgeClientApi.`bridgescorer-clientapi`
lazy val `bridgescorer-client` = BldBridgeClient.`bridgescorer-client`

lazy val help = BldBridgeHelp.help

lazy val `bridgescorer-server`: Project = BldBridgeServer.`bridgescorer-server`

lazy val bridgescorer: Project = BldBridge.bridgescorer
lazy val bridgescorekeeper: Project = BldBridgeScoreKeeper.bridgescorekeeper
