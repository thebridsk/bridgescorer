package com.github.thebridsk.bridge.test.pages

import com.github.thebridsk.utilities.logging.Logger
import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.Eventually._
import org.scalactic.source.Position
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.browserpages.PageBrowser._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import java.net.URL
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.browserpages.Page

object HelpPage {

  val log = Logger[HelpPage]()

  val directory = "target/screenshots/HelpPage"

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val cp = getCurrentPage
    log.fine(s"""Current help page is ${cp}""" )
    new HelpPage( cp )
  }

  def urlFor( helppage: String = "" ) = TestServer.getHelpPage(helppage)

  def goto( helppage: String = "" )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(helppage)
    new HelpPage(helppage)
  }

  def getCurrentPage( implicit webDriver: WebDriver, pos: Position ): String = {
    val prefix = TestServer.getHelpPage()
    eventually {
      val cur = currentUrl
      cur must startWith( prefix )
      val s = cur.drop(prefix.length())
      s
    }
  }

  /**
   * @param helppage the URI without "help/".  Example: for duplicate page use "duplicate/"
   */
  def getPageUrl( helppage: String = "" ) = {
    TestServer.getHelpPage(helppage)
  }

  val hrefvals=List( "introduction.html", "home.html", "duplicate.html", "chicago.html", "rubber.html" )
  val hrefurls=hrefvals.map( v => getPageUrl(v) )

  def gethrefs( implicit webDriver: WebDriver, pos: Position ) = {
    findAllElems[Element]( xpath("//a") ).flatMap( e => e.attribute("href") )
  }

  def getImages( implicit webDriver: WebDriver, pos: Position ) = {
    findAllElems[Element]( xpath("//img") ).flatMap( e => e.attribute("src"))
  }

  def checkImage( url: String ) = {
    val resp = HttpUtils.getHttpAllBytes( new URL(url) )
    resp.status mustBe 200
  }

  def adjustURI( uri: String ) = {
    val slashes = uri.length() - uri.replace("/","").length()
    if (slashes == 0) s"./$uri"
    else s"${"../" * slashes}$uri"
  }
}

/**
 * @param helpuri the URI without "help/".  Example: for duplicate page use "duplicate/"
 */
class HelpPage(
                val helpuri: String
              )( implicit
                   webDriver: WebDriver,
                   pageCreated: SourcePosition
              ) extends Page[HelpPage] {
  import HelpPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = {

    // <body class="" data-url="./introduction.html">
    // <body class="" data-url="../duplicate/summary.html">
    // <body class="" data-url="../../chicago/fastfair/names5.html">

    eventually {
      val base = getPageUrl(helpuri)
      val url = currentUrl
      url mustBe base
      // find( xpath(s"""//body[contains(@data-url, './${helpuri}')]""") )
      find( xpath(s"""//body[@data-url='${adjustURI(helpuri)}']""") )
    }
    this
  }

  def checkMainMenu(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {

    withClueAndScreenShot(directory, "checkMainMenu", "checking main menu") {
      eventually {
        val links = findAll( xpath("""//ul[contains(concat(' ', @class, ' '), ' topics ')]/li/a"""))
        val href = links.flatMap( e => e.attribute("href") )
        log.fine( s"""found ${href}""" )
        href must contain theSameElementsAs hrefurls
      }
    }
    this
  }

  /**
   * @param item the URI without "help/".  Example: for duplicate page use "duplicate/"
   */
  def clickMenu( item: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val b = find( xpath(s"""//ul[contains(concat(' ', @class, ' '), ' topics ')]//li[@data-nav-id = '/${item}']/a""") )
    click on b
    new HelpPage(item)
  }

  def clickDuplicate(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    clickMenu( "duplicate.html" )
  }

  /**
   * @param helppage the URI without "help/".  Example: for duplicate page use "duplicate/"
   */
  def checkPage( helppage: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val url = getPageUrl(helppage)
    val curl = currentUrl
    log.fine( s"""Current URL ${curl}, looking for ${url}""" )
    curl mustBe url
    this
  }

}
