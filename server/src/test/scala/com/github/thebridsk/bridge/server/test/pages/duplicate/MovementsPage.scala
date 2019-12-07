package com.github.thebridsk.bridge.server.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import java.net.URL
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp

object MovementsPage {

  val log = Logger[MovementsPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new MovementsPage( getCurrentMovement )
  }

  def urlFor( movement: Option[String] = None ) = TestServer.getAppPageUrl("duplicate/movements"+movement.map{ m => s"/${m}"}.getOrElse(""))
  def restUrlFor( movement: String ) = TestServer.getUrl(s"/v1/rest/movements/${movement}")

  def goto( movement: String = null )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val mov = Option(movement)
    go to urlFor(Option(movement))
    new MovementsPage(mov)
  }

  val movements = "2TablesArmonk"::"Howell3TableNoRelay"::"Mitchell3Table"::Nil

  def getMovement( movement: String ): Option[Movement] = {
    import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
    val ResponseFromHttp(status,loc,ce,mov,cd) = HttpUtils.getHttpObject[Movement](restUrlFor(movement))
    mov
  }

  def getBoardsFromMovement( movement: String ): List[Int] = {
    getMovement(movement) match {
      case Some(mov) =>
        mov.hands.flatMap( hit => hit.boards ).distinct.sorted
      case None =>
        fail(s"Unable to get movement $movement")
    }
  }

  def getRoundsFromMovement( movement: String ): List[Int] = {
    getMovement(movement) match {
      case Some(mov) =>
        mov.hands.map( hit => hit.round ).distinct.sorted
      case None =>
        fail(s"Unable to get movement $movement")
    }
  }

  def getCurrentMovement( implicit webDriver: WebDriver, pos: Position ): Option[String] = {
    val prefix = urlFor()
    eventually {
      val cur = currentUrl
      cur must startWith( prefix )
      if (cur == prefix) None
      else {
        val s = prefix.drop(cur.length())
        if (s.charAt(0) == '/') Some(s.drop(1))
        else fail(s"${pos.line} URL is not valid for MovementsPage: $cur")
      }
    }
  }
}

class MovementsPage(
                     movement: Option[String] = None
                   )( implicit
                       webDriver: WebDriver,
                       pageCreated: SourcePosition
                   ) extends Page[MovementsPage] {
  import MovementsPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {
    currentUrl mustBe urlFor( movement )

    findButtons( "OK"::movements: _* )
    this
  }}

  def click( mov: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!movements.contains(mov)) log.warning(s"Unknown movement mov")

    clickButton( mov )
    new MovementsPage(Option(mov))
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("OK")
    new ListDuplicatePage(None)
  }

}
