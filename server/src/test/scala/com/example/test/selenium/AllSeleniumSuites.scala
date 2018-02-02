package com.example.test.selenium

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllSeleniumSuites extends Sequential(
  new ChicagoTestPages,
  new ChicagoTest,
  new Chicago5Test,
  new Chicago5SimpleTest,
  new Chicago5FairTest,
  new DuplicateTestPages2,
  new Duplicate5TestPages,
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
