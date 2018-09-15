package com.example.test.selenium.integrationtest

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Sequential
import com.example.test.selenium.TestServer
import com.example.test.selenium.DuplicateTestFromTestDirectory
import com.example.test.selenium.DuplicateTestFromTestDirectory2

/**
 * @author werewolf
 */
class IntegrationTests extends Sequential(
  new com.example.test.selenium.IntegrationTests,
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
