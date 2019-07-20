package com.github.thebridsk.bridge.server.test.selenium

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential

/**
 * @author werewolf
 */
class IntegrationTests extends Sequential(
  new DuplicateTestFromTestDirectory,
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
