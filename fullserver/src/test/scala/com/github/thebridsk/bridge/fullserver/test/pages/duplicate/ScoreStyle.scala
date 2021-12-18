package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.Eventually._
import org.scalactic.source.Position
import org.scalatest.Assertions._
import com.github.thebridsk.browserpages.PageBrowser._

sealed trait ScoreStyle {
  val name: String
  override def toString() = getClass.getSimpleName
}

sealed trait ScoreStylePlayed extends ScoreStyle

object ScoreStyleIMP extends ScoreStyle {
  val name = "International Match Points"
}
object ScoreStyleMP extends ScoreStyle { val name = "Match Points" }
object ScoreStylePlayedMultiple extends ScoreStylePlayed {
  val name = "Played Scoring Method"
}
object ScoreStylePlayedMP extends ScoreStylePlayed {
  val name = "Played Scoring Method: MP"
}
object ScoreStylePlayedIMP extends ScoreStylePlayed {
  val name = "Played Scoring Method: IMP"
}
object ScoreStylePlayedUnknown extends ScoreStylePlayed {
  val name = "Played Scoring Method: Unknown"
}

object ScoreStyle {
  val validScoreStyles: List[ScoreStyle] =
    ScoreStyleMP ::
      ScoreStyleIMP ::
      ScoreStylePlayedMultiple ::
      ScoreStylePlayedMP ::
      ScoreStylePlayedIMP ::
      ScoreStylePlayedUnknown ::
      Nil

  /**
    * @param style the input style, valid values are from ScoreStyle sealed trait
    * @return Some(style) if successful, otherwise returns current input style
    */
  def setScoreStyle(
      style: ScoreStyle
  )(implicit webDriver: WebDriver, pos: Position): Option[ScoreStyle] = {
    if (!validScoreStyles.contains(style))
      fail(
        s"""Specified style, ${style} is not valid, must be one of ${validScoreStyles
          .mkString(", ")}"""
      )
    val stop = Some(style)
    var last: Option[ScoreStyle] = None
    for (i <- 1 to 3) {
      last = getScoreStyle
      if (last == stop) return stop
      click on id("ScoreStyle")
    }
    last
  }

  def getScoreStyle(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): Option[ScoreStyle] = {
    val cur = find(id("ScoreStyle")).text
    validScoreStyles.find(s => s.name == cur)
  }

}
