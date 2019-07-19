package com.github.thebridsk.bridge.server.test.selenium

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllSeleniumSuites extends Sequential(

  new ChicagoTest,          // this must be the first test, has test that requires no names in server
  new Chicago5Test,
  new Chicago5SimpleTest,
  new Chicago5FairTest,
  new ChicagoTestPages,
  new Duplicate5TestPages,   // this must be first duplicate test, checks stats after playing hand
  new DuplicateTestPages,
  new DuplicateTestPages2,
  new RubberTest,
  new DuplicateResultTest,
  new SwaggerTest,
  new SwaggerTest2

) with BeforeAndAfterAll {

  override
  def beforeAll() = {
    TestServer.start()
  }

  override
  def afterAll() = {
    TestServer.stop()
  }

}
