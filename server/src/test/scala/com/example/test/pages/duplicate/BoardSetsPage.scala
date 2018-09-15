package com.example.test.pages.duplicate

import com.example.test.pages.Page
import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.test.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.test.util.HttpUtils
import com.example.data.BoardSet
import com.example.data.Movement
import java.net.URL
import com.example.test.util.HttpUtils.ResponseFromHttp

object BoardSetsPage {

  val log = Logger[BoardSetsPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new BoardSetsPage( getCurrentBoard )
  }

  def urlFor( boardset: Option[String] = None ) = TestServer.getAppPageUrl("duplicate/boardsets"+boardset.map{ bs => s"/${bs}"}.getOrElse(""))
  def restUrlFor( boardset: String ) = TestServer.getUrl(s"/v1/rest/boardsets/${boardset}")

  def goto(boardset: String = null)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val bs = Option(boardset)
    go to urlFor(bs)
    new BoardSetsPage(bs)
  }

  val boardsets = "ArmonkBoards"::"StandardBoards"::Nil

  def getBoardSet( boardset: String ): Option[BoardSet] = {
    import com.example.rest.UtilsPlayJson._
    val ResponseFromHttp(status,loc,ce,bs,cd) = HttpUtils.getHttpObject[BoardSet](restUrlFor(boardset))
    bs
  }

  def getCurrentBoard( implicit webDriver: WebDriver, pageCreated: SourcePosition ): Option[String] = {
    val prefix = urlFor()
    eventually {
      val cur = currentUrl
      cur must startWith( prefix )
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
                   )(
                     implicit webDriver: WebDriver,
                     pageCreated: SourcePosition
                   ) extends Page[BoardSetsPage] {
  import BoardSetsPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor(boardset)

    findButtons( "OK"::boardsets: _* )
    this
  }}

  def click( bs: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!boardsets.contains(bs)) log.warning(s"Unknown boardset bs")

    clickButton(bs)
    new BoardSetsPage(Option(bs))
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("OK")
    new ListDuplicatePage(None)
  }

}
