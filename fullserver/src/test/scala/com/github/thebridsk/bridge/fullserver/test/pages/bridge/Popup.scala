package com.github.thebridsk.bridge.fullserver.test.pages.bridge

import org.scalactic.source.Position
import com.github.thebridsk.browserpages.PageBrowser
import org.scalatest.concurrent.Eventually._
import org.openqa.selenium.WebDriver
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.browserpages.Page

object Popup {
  val buttonPopUpCancel = "PopUpCancel"
  val buttonPopUpOK = "PopUpOk"

}

trait Popup[+T <: Page[T]] {
  import PageBrowser._
  import Matchers._
  import Popup._

  implicit val webDriver: WebDriver

  def isPopupDisplayed(implicit pos: Position): Boolean = {
    try {
      find(id("popup")).isDisplayed
    } catch {
      case x: NoSuchElementException => false
    }
  }

  def validatePopup(
      visible: Boolean = true
  )(implicit patienceConfig: PatienceConfig, pos: Position): T = {
    eventually {
      withClue("Looking for popup") {
        isPopupDisplayed mustBe visible
      }
    }
    this.asInstanceOf[T]
  }

  def clickPopUpOk(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): T = {
    click on id(buttonPopUpOK)
    this.asInstanceOf[T]
  }

  def clickPopUpCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): T = {
    click on id(buttonPopUpCancel)
    this.asInstanceOf[T]
  }

  def getPopUpText(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): String = {
    find(className("baseDivPopupOKCancelBody")).text
  }

}
