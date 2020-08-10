package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import java.net.URL

object BoardSetsPage {

  val log: Logger = Logger[BoardSetsPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardSetsPage = {
    new BoardSetsPage(getCurrentBoard)
  }

  def urlFor(boardset: Option[String] = None): String =
    TestServer.getAppPageUrl(
      "duplicate/boardsets" + boardset.map { bs => s"/${bs}" }.getOrElse("")
    )
  def restUrlFor(boardset: String): URL =
    TestServer.getUrl(s"/v1/rest/boardsets/${boardset}")

  def goto(boardset: String = null)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardSetsPage = {
    val bs = Option(boardset)
    go to urlFor(bs)
    new BoardSetsPage(bs)
  }

  val boardsets: List[String] = "ArmonkBoards" :: "StandardBoards" :: Nil

  def getBoardSet(boardset: String): Option[BoardSet] = {
    import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
    val ResponseFromHttp(status, loc, ce, bs, cd) =
      HttpUtils.getHttpObject[BoardSet](restUrlFor(boardset))
    bs
  }

  def getCurrentBoard(implicit
      webDriver: WebDriver,
      pageCreated: SourcePosition
  ): Option[String] = {
    val prefix = urlFor()
    eventually {
      val cur = currentUrl
      cur must startWith(prefix)
      if (cur == prefix) None
      else {
        val s = prefix.drop(cur.length())
        if (s.charAt(0) == '/') Some(s.drop(1))
        else fail("URL is not valid for BoardSetPage: $cur")
      }
    }
  }

}

class BoardSetsPage(
    boardset: Option[String] = None
)(implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[BoardSetsPage] {
  import BoardSetsPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardSetsPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor(boardset)

        findButtons("OK" :: boardsets: _*)
        this
      }
    }

  def click(
      bs: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardSetsPage = {
    if (!boardsets.contains(bs)) log.warning(s"Unknown boardset bs")

    clickButton(bs)
    new BoardSetsPage(Option(bs))
  }

  def clickOK(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    clickButton("OK")
    new ListDuplicatePage(None)
  }

}
