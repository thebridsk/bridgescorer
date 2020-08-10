package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.util.TestServer

/**
  * @author werewolf
  */
class TravisSeleniumSuites
    extends Sequential(
      new ChicagoTest,
      new Chicago5Test,
      new Chicago5SimpleTest,
      new Chicago5FairTest,
      new ChicagoTestPages,
      new DuplicateTestPages,
      new DuplicateTestPages2,
      new Duplicate5TestPages,
      new RubberTest,
      new DuplicateResultTest,
      new SwaggerTest
//  new SwaggerTest2
    )
    with BeforeAndAfterAll {
  override def beforeAll(): Unit = {
    TestServer.start()
  }

  override def afterAll(): Unit = {
    TestServer.stop()
  }

}
