package com.github.thebridsk.bridge.test.selenium.integrationtest

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.github.thebridsk.bridge.test.selenium.TestServer
import com.github.thebridsk.bridge.test.selenium.DuplicateTestFromTestDirectory2

/**
 * @author werewolf
 */
class TravisIntegrationTests extends Sequential(
  new com.github.thebridsk.bridge.test.selenium.TravisIntegrationTests,
  new HelpTest
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
