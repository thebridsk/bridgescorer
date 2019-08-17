package com.github.thebridsk.bridge.server.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.Page

trait ServerURLAddOn[+T <: Page[T]] {
  page: Page[T] =>

  def validateServerURL(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    val h1 = findElemByXPath("""//div[@id = 'ServerURLPopupDiv']/div/div/div/div/div/h1""").text
    h1 mustBe "Server URL"
    this
  }

  def clickServerURL(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ServerURL")
    this
  }

  def clickServerURLOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("PopUpOk")
    this
  }

  def getServerURLs(implicit patienceConfig: PatienceConfig, pos: Position): List[String] = {
    findElemsByXPath("""//div[@id = 'ServerURLPopupDiv']/div/div/div/div/div/ul/li""").map(_.text)
  }

  def checkServerURL(l: List[String])(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getServerURLs must contain theSameElementsAs l
  }

  def checkForValidServerURLs(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    getServerURLs.map { u =>
      u match {
        case ServerURLAddOn.patternURL() =>
          // good
        case _ =>
          fail(s"""checkForValidServerURLs: url is not valid: $u""")
      }
    }
  }
}

object ServerURLAddOn {
  val patternURL = """https?\://.*?/""".r
}
