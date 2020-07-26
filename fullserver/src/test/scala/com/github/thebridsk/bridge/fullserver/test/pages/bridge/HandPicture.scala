package com.github.thebridsk.bridge.fullserver.test.pages.bridge

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import org.scalatest.concurrent.Eventually._
import org.scalactic.source.Position
import org.openqa.selenium.WebDriver
import com.github.thebridsk.browserpages.PageBrowser
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.source.SourcePosition
import scala.reflect.io.File
import com.github.thebridsk.browserpages.Element
import org.scalatest.Assertion
import scala.util.matching.Regex


object HandPicture {
  val log: Logger = Logger[HandPicture[_]]()

  /** the input field for take picture button */
  val namePicture = "picture"

  /** the id of button to show picture button */
  val showPicture = "ShowPicture"

  /** the id of button to delete picture button */
  val deletePicture = "RemovePicture"

  /** the id of the ok button on picture popup */
  val okPicture = ".playDuplicate #popup #PopUpOk"
  val okPicture2 = ".dupDivBoardPage #popup #PopUpOk"

  //
  // when picture is stored on server
  //
  //   src="/v1/rest/duplicates/M15/pictures/B1/hands/T1"
  //
  // or on return from taking picture
  //
  //   src="<img src="data:image/jpeg;base64,/9j/4TicRX...agds+asasdf=="
  //
  val patternImageSrcServer: Regex = """(?:https?://[^/]+)?/v1/rest(/.*)""".r
  val patternImageSrcBrowser: Regex = """data:image/([^;]+);base64,[a-zA-z0-9/+]+=?=?""".r

}

trait HandPicture[ +T <: Page[T] ] extends Popup[T] {

  import PageBrowser._
  import Matchers._
  import HandPicture._

  implicit val webDriver: WebDriver

  val page: T = this.asInstanceOf[T]

  def getOkButton: Element = findOption( cssSelector(okPicture) ).getOrElse( find(cssSelector(okPicture2)))

  /**
   * Validate a page with only one show picture button
   * @param showingPicture whether picture is showing or not, default is false
   */
  def validatePicture( showingPicture: Boolean = false )(implicit patienceConfig: PatienceConfig, pos: Position): T = eventually {

    withClue(s"Validating page with a picture button, showingPicture=${showingPicture}") {
      isPictureDisplayed mustBe showingPicture
      if (showingPicture) {
        val e = getOkButton
        e.isDisplayed mustBe true
        validateImageUrl
      } else {
        val e = find( name(namePicture))
        e.scrollToElement
        find( name(namePicture) ).isEnabled mustBe true
      }
    }
    page
  }

  /**
   * Validate a page with potentially multiple show picture buttons.
   * If the picture popup is being displayed, then the IDs are not checked.
   * @param showingPicture whether picture is showing or not, default is false
   * @param ids a list of picture ids of buttons that are visible.  Default is Nil
   *            All the picture ids will get a prefix of "ShowPicture_"
   */
  def validatePictures( showingPicture: Boolean = false, ids: List[String] = Nil, notShowingIds: List[String] = Nil )(implicit patienceConfig: PatienceConfig, pos: Position): T = eventually {

    withClue(s"Validating page with potentially multiple picture buttons, showingPicture=${showingPicture}") {
      isPictureDisplayed mustBe showingPicture

      if (showingPicture) {
        val e = getOkButton
        e.isDisplayed mustBe true
        validateImageUrl
      } else {
        ids.foreach { pid =>
          find( id(s"${showPicture}_${pid}") ).isDisplayed mustBe true
        }
        notShowingIds.foreach { pid =>
          findAll( id(s"${showPicture}_${pid}") ).isEmpty mustBe true
        }
      }
    }
    page
  }

  def getImageUrl( implicit pos: Position ): Option[String] = {
    val img = find( cssSelector("""div#HandPicture > img"""))
    img.attribute("src")
  }

  /**
   * Check to see if the image src is valid.
   * Should only be called when the popup is visible
   */
  def checkImageUrl( serverUrl: Boolean )( implicit patienceConfig: PatienceConfig, pos: Position ): T = eventually {
    getImageUrl match {
        case Some(url) =>
          if (serverUrl) {
            url match {
              case patternImageSrcServer(rest) =>
              case _ =>
                fail(s"Did not find an img src value for server, called from ${pos.line}: ${url}")
            }
          } else {
            url match {
              case patternImageSrcBrowser(subtype) =>
              case _ =>
                fail(s"Did not find an img src value for browser, called from ${pos.line}: ${url}")
            }
          }
        case None =>
          fail(s"Did not find an img src value, called from ${pos.line}")
    }
    page
  }

  /**
   * Check to see if the image src is valid.
   * Should only be called when the popup is visible
   */
  def validateImageUrl( implicit patienceConfig: PatienceConfig, pos: Position ): T = eventually {
    getImageUrl match {
        case Some(url) =>
          url match {
            case patternImageSrcServer(rest) =>
            case patternImageSrcBrowser(subtype) =>
            case _ =>
              fail(s"Did not find an img src value, called from ${pos.line}: ${url.substring(0,50)}")
          }
        case None =>
          fail(s"Did not find an img src value, called from ${pos.line}")
    }
    page
  }

  /**
   * @param serverUri the server URI to look for.  Syntax: /duplicates/M15/pictures/B1/hands/T1
   *
   */
  def checkDuplicateServerUrl( serverUri: String )( implicit patienceConfig: PatienceConfig, pos: Position ): T = eventually {
    getImageUrl.map { url =>
      url match {
        case patternImageSrcServer(rest) =>
          withClue( s"Checking image URL for server") {
            rest mustBe serverUri
          }
        case _ =>
          fail(s"Did not find an img src value for server, called from ${pos.line}: ${url}")
      }
    }.orElse( fail(s"Did not find an img src value, called from ${pos.line}") )
    page
  }

  def clickTakePicture( implicit pos: Position ): T = {
    find( name(namePicture) ).click
    page
  }

  def selectPictureFile( file: File )( implicit pos: Position ): T = {
    val upload = find(name(namePicture))
    upload.sendKeys(file.toString);
    page
  }

  def clickShowPicture( implicit pos: Position ): T = {
    find( id(showPicture) ).click
    page
  }

  /**
   * @param bid the id of the button.  The bid will be prefixed with "ShowPicture_"
   */
  def clickShowPicture( bid: String )( implicit pos: Position ): T = {
    find( id(s"${showPicture}_${bid}") ).click
    page
  }

  def clickDeletePicture( implicit pos: Position ): T = {
    find( id(deletePicture) ).click
    page
  }

  def clickOkPicture( implicit pos: Position ): T = {
    getOkButton.click
    page
  }

  def isPictureDisplayed( implicit pos: Position ): Boolean = {
    getOkButton.isDisplayed
  }

  def isShowDisplayed( implicit pos: Position ): Boolean = {
    find( id(showPicture)).isDisplayed
  }

  def isDeleteDisplayed( implicit pos: Position ): Boolean = {
    find( id(deletePicture)).isDisplayed
  }

  def checkShowDisplayed( displayed: Boolean )( implicit patienceConfig: PatienceConfig, pos: Position ): Assertion = eventually {
    isShowDisplayed mustBe displayed
  }

  def checkDeleteDisplayed( displayed: Boolean )( implicit patienceConfig: PatienceConfig, pos: Position ): Assertion = eventually {
    isDeleteDisplayed mustBe displayed
  }

}
