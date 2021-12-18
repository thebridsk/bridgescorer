//
// to get times for tasks,
//    set JAVA_OPTS=-Dsbt.task.timings=true

bloopExportJarClassifiers in Global := Some(Set("sources"))

suppressSbtShellNotification := true

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
lazy val `bridgescorer-clienttest` = BldBridgeClientTest.`bridgescorer-clienttest`
lazy val `bridgescorer-client` = BldBridgeClient.`bridgescorer-client`

lazy val help = BldBridgeHelp.help

lazy val `bridgescorer-server`: Project = BldBridgeServer.`bridgescorer-server`
lazy val `bridgescorer-fullserver`: Project = BldBridgeFullServer.`bridgescorer-fullserver`

lazy val bridgescorekeeper: Project = BldBridgeScoreKeeper.bridgescorekeeper

lazy val bridgescorer: Project = BldBridge.bridgescorer
lazy val bridgeDemo: Project = BldBridgeDemo.demo

BldBridge.init
