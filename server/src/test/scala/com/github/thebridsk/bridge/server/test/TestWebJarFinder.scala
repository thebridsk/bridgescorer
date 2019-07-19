package com.github.thebridsk.bridge.server.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest.Matchers
import com.github.thebridsk.bridge.server.webjar.FileFinder
import com.github.thebridsk.bridge.server.service.ResourceFinder

class TestWebJarFinder extends FlatSpec with MustMatchers {

  behavior of "The WebJar FileFinder class"

  val version = ResourceFinder.htmlResources.version

  it should "find the FileFinder object for react-widgets" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.isArtifact("bridgescorer-server") mustBe true
  }

  it should "not find the FileFinder object for react" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.isArtifact("react") mustBe false
  }

  it should "find the resource dist/css/react-widgets.css in FileFinder object for react-widgets" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.getResource("react-widgets/dist/css/react-widgets.css") match {
      case Some(s) =>
      case None => fail("Did not find resource dist/css/react-widgets.css")
    }
  }

  it should "not find the resource dist/css/react.css in FileFinder object for react-widgets" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.getResource("react-widgets/dist/css/widgets.css") match {
      case Some(s) => fail("Not expected to find resource dist/css/react.css")
      case None =>
    }
  }

  it should "not find the resource dist/../dist/css/react-widgets.css in FileFinder object for react-widgets" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.getResource("react-widgets/dist/../dist/css/react-widgets.css") match {
      case Some(s) => fail("Not expected to find resource dist/../dist/css/react-widgets.css")
      case None =>
    }
  }

  it should "find the resource react-widgets, dist/css/react-widgets.css in FileFinder object for react-widgets" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.findResource("bridgescorer-server","react-widgets/dist/css/react-widgets.css") match {
      case Some(s) =>
      case None => fail("Did not find resource dist/css/react-widgets.css")
    }
  }

  it should "not find the resource react-widgets, dist/css/react.css in FileFinder object for react-widgets" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.findResource("bridgescorer-server","react-widgets/dist/css/widgets.css") match {
      case Some(s) => fail("Not expected to find resource dist/css/react.css")
      case None =>
    }
  }

  it should "not find the resource react, dist/css/react-widgets.css in FileFinder object for react-widgets" in {
    val ff = new FileFinder( "com.github.thebridsk.bridge.server", "bridgescorer-server", Some(version) )
    ff.findResource("react","react-widgets/dist/css/react-widgets.css") match {
      case Some(s) => fail("Not expected to find resource react, dist/css/react-react.css")
      case None =>
    }
  }


}
