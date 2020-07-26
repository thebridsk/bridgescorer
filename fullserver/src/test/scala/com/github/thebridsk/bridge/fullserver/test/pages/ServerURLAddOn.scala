package com.github.thebridsk.bridge.fullserver.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.Page
import org.scalatest.Assertion
import scala.util.matching.Regex

trait ServerURLAddOn[+T <: Page[T]] {
  page: Page[T] =>

  def validateServerURL(implicit patienceConfig: PatienceConfig, pos: Position): T = eventually {
    val h1 = findElemByXPath("""//div[@id = 'ServerURLPopupDiv']/div/div/div/div/div/h1""").text
    h1 mustBe "Server URL"
    this.asInstanceOf[T]
  }

  def clickServerURL(implicit patienceConfig: PatienceConfig, pos: Position): T = {
    clickButton("ServerURL")
    this.asInstanceOf[T]
  }

  def clickServerURLOK(implicit patienceConfig: PatienceConfig, pos: Position): T = {
    clickButton("PopUpOk")
    this.asInstanceOf[T]
  }

  def getServerURLs(implicit patienceConfig: PatienceConfig, pos: Position): List[String] = {
    findElemsByXPath("""//div[@id = 'ServerURLPopupDiv']/div/div/div/div/div/ul/li""").map(_.text)
  }

  def checkServerURL(l: List[String])(implicit patienceConfig: PatienceConfig, pos: Position): Assertion = {
    getServerURLs must contain theSameElementsAs l
  }

  def checkForValidServerURLs(implicit patienceConfig: PatienceConfig, pos: Position): List[Unit] = eventually {
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
  val patternURL: Regex = """https?\://.*?/""".r
}
