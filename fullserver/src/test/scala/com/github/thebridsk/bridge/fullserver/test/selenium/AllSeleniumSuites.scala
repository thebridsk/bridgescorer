package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.util.TestServer

/**
  * @author werewolf
  */
class AllSeleniumSuites
    extends Sequential(
      new ChicagoTest, // this must be the first test, has test that requires no names in server
      new Chicago5Test,
      new Chicago5SimpleTest,
      new Chicago5FairTest,
      new ChicagoTestPages,
      new ChicagoDemoTestPages,
      new Duplicate5TestPages, // this must be first duplicate test, checks stats after playing hand
      new DuplicateTestPages,
      new DuplicateTestPages2,
      new RubberTest,
      new DuplicateResultTest,
      new IndividualTest,
      new SwaggerTest,
      new SwaggerTest2
    )
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    TestServer.start()
  }

  override def afterAll(): Unit = {
    TestServer.stop()
  }

}
