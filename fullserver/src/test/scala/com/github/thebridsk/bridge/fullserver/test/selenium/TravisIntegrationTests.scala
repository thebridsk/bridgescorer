package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.selenium.TestServer

/**
 * @author werewolf
 */
class TravisIntegrationTests extends Sequential(
  new DuplicateTestFromTestDirectory2
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
