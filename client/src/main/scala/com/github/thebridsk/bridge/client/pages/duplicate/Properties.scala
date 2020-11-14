package com.github.thebridsk.bridge.client.pages.duplicate

import com.github.thebridsk.bridge.client.pages.Pixels
import com.github.thebridsk.bridge.client.Bridge

object Properties {

  val viewportContent: String = {
    val elem = Bridge.getElement("metaViewport")
    elem.getAttribute("content")
  }

  val defaultScoreboardWidth: Int =
    Pixels.getWidthWithBoarder("DefaultScoreboard")
  val defaultNumberOfBoards = 18
  val defaultScoreboardBoardWidth: Int =
    Pixels.getWidthWithBoarder("DefaultScoreboardBoard")
  val defaultScoreboardPlayersWidth: Int =
    Pixels.getWidthWithBoarder("DefaultScoreboardPlayers")
  val defaultScoreboardNamesWidthWithoutName: Int =
    Pixels.getPaddingBorder("DefaultScoreboardNames")
  val defaultScoreboardTitleBoards: Int =
    Pixels.getWidthWithBoarder("DefaultScoreboardTitleBoards")

  def getScoreboardWidth(boards: Int, names: String*): Int = {
    val nameLen =
      Pixels.maxLength(names: _*) + defaultScoreboardNamesWidthWithoutName
    val playerlen = Math.max(defaultScoreboardPlayersWidth, nameLen)

    defaultScoreboardWidth -
      defaultScoreboardTitleBoards -
      defaultScoreboardPlayersWidth +
      playerlen +
      boards * defaultScoreboardBoardWidth
  }

  def setViewportContent(boards: Int, names: String*): Unit = {
    val w = getScoreboardWidth(boards, names: _*)
    setViewportContentWidth(w)
  }

  def setViewportContentWidth(pixels: Int): Unit = {
    val nc = viewportContent.replace("width=device-width", s"width=${pixels}")
    val elem = Bridge.getElement("metaViewport")
    elem.setAttribute("content", nc)
  }

  def restoreViewportContentWidth(): Unit = {
    val elem = Bridge.getElement("metaViewport")
    elem.setAttribute("content", viewportContent)
  }

}
