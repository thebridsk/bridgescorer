package com.github.thebridsk.bridge.test.pages

import com.github.thebridsk.bridge.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._

object GenericPage {

  def current( implicit webDriver: WebDriver, pageCreated: SourcePosition ) = {
    new GenericPage
  }

  def goto( url: String )( implicit webDriver: WebDriver, pageCreated: SourcePosition ) = {
    webDriver.get(url)
    new GenericPage
  }
}

class GenericPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[GenericPage] {

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = this

}
