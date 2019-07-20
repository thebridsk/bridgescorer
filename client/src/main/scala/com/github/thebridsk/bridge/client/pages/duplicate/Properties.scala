package com.github.thebridsk.bridge.client.pages.duplicate

import com.github.thebridsk.bridge.client.pages.Pixels
import com.github.thebridsk.bridge.client.Bridge

object Properties {

  val viewportContent = {
    val elem = Bridge.getElement("metaViewport")
    elem.getAttribute("content")
  }

  val defaultScoreboardWidth = Pixels.getWidthWithBoarder("DefaultScoreboard")
  val defaultNumberOfBoards = 18
  val defaultScoreboardBoardWidth = Pixels.getWidthWithBoarder("DefaultScoreboardBoard")
  val defaultScoreboardPlayersWidth = Pixels.getWidthWithBoarder("DefaultScoreboardPlayers")
  val defaultScoreboardNamesWidthWithoutName = Pixels.getPaddingBorder("DefaultScoreboardNames")
  val defaultScoreboardTitleBoards = Pixels.getWidthWithBoarder("DefaultScoreboardTitleBoards")

  def getScoreboardWidth( boards: Int, names: String* ) = {
    val nameLen = Pixels.maxLength(names:_*)+defaultScoreboardNamesWidthWithoutName
    val playerlen = Math.max(defaultScoreboardPlayersWidth, nameLen)

    defaultScoreboardWidth-
        defaultScoreboardTitleBoards-
        defaultScoreboardPlayersWidth+
        playerlen+
        boards*defaultScoreboardBoardWidth
  }

  def setViewportContent( boards: Int, names: String* ) = {
    val w = getScoreboardWidth(boards, names:_*)
    setViewportContentWidth(w)
  }

  def setViewportContentWidth( pixels: Int ) = {
    val nc = viewportContent.replace("width=device-width", s"width=${pixels}px")
    val elem = Bridge.getElement("metaViewport")
    elem.setAttribute("content",nc)
  }

  def restoreViewportContentWidth() = {
    val elem = Bridge.getElement("metaViewport")
    elem.setAttribute("content",viewportContent)
  }

}
