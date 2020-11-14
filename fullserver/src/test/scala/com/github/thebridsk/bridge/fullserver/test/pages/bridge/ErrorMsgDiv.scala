package com.github.thebridsk.bridge.fullserver.test.pages.bridge

import com.github.thebridsk.browserpages.Page
import org.scalatest.concurrent.Eventually._
import org.scalactic.source.Position
import com.github.thebridsk.browserpages.PageBrowser._
import org.openqa.selenium.WebDriver
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.source.SourcePosition

trait ErrorMsgDiv[T <: Page[T]] {
  self: T =>

  implicit val webDriver: WebDriver

  def checkErrorMsg(
      s: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): T =
    eventually {
      val e = find(cssSelector("div#ErrorMsg p"))
      e.text mustBe s
      self
    }

}

class PageWithErrorMsg(implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[PageWithErrorMsg]
    with ErrorMsgDiv[PageWithErrorMsg] {

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): PageWithErrorMsg = this

}
