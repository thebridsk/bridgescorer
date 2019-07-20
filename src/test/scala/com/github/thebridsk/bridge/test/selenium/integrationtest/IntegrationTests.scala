package com.github.thebridsk.bridge.test.selenium.integrationtest

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.bridge.server.test.selenium.DuplicateTestFromTestDirectory
import com.github.thebridsk.bridge.server.test.selenium.DuplicateTestFromTestDirectory2

/**
 * @author werewolf
 */
class IntegrationTests extends Sequential(
  new com.github.thebridsk.bridge.server.test.selenium.IntegrationTests,
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
