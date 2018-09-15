package com.example.test.selenium.integrationtest

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.example.test.selenium.TestServer
import com.example.test.selenium.DuplicateTestFromTestDirectory2

/**
 * @author werewolf
 */
class TravisIntegrationTests extends Sequential(
  new com.example.test.selenium.TravisIntegrationTests,
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
