package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.util.TestServer

/**
 * @author werewolf
 */
class IntegrationTests extends Sequential(
  new DuplicateTestFromTestDirectory,
  new DuplicateTestFromTestDirectory2
) with BeforeAndAfterAll {
  override
  def beforeAll(): Unit = {
    TestServer.start()
  }

  override
  def afterAll(): Unit = {
    TestServer.stop()
  }

}
