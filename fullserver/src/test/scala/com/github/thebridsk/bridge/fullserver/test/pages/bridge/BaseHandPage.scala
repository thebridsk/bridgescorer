package com.github.thebridsk.bridge.fullserver.test.pages

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.bridge.ContractSuit
import com.github.thebridsk.bridge.data.bridge.ContractDoubled
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.Made
import com.github.thebridsk.bridge.data.bridge.Down
import com.github.thebridsk.bridge.data.bridge.MadeOrDown
import com.github.thebridsk.bridge.data.bridge.Vulnerability
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.browserpages.Element
import org.scalatest.Assertion
import scala.util.matching.Regex

object BaseHandPage {

  val log: Logger = Logger(getClass.getName)

  def urlFor: String = TestServer.getAppPageUrl("duplicate/#new")

  val patternContractTricksToInt: Regex = """CT(\d)""".r
  val patternTricksToInt: Regex = """T(\d+)""".r

  val passedOutButton = "CTPassed"
  val nContractTricksButtons: Map[Int,String] = (1 to 7).map( i => i->s"CT$i" ).toMap
  val contractTricksButtons: Map[Int,String] = nContractTricksButtons + (0->passedOutButton)
  val contractSuitButtons: Map[ContractSuit,String] = "NSHDC".map( c => (ContractSuit( c.toString ),s"CS$c") ).toMap
  val doubledButtons: Map[ContractDoubled,String] = "NDR".map( c => (ContractDoubled(c.toString), s"Doubled$c") ).toMap
  val declarerButtons: Map[PlayerPosition,String] = "NEWS".map( c => (PlayerPosition(c.toString), s"Dec$c") ).toMap
  val madeDownButtons: Map[MadeOrDown,String] = Map( Made-> "made", Down->"down")
  val tricksButtons: Map[Int,String] = (1 to 13).map( i => i->s"T$i" ).toMap

  val honorsPoints: Map[Int,String] = Map( 0 ->"Honors0", 100->"Honors100", 150->"Honors150")
  val honPlayButtons: Map[PlayerPosition,String] = "NEWS".map( c => (PlayerPosition(c.toString), s"HonPlay$c") ).toMap

  val alwaysButtons: List[String] =
      "Ok"::"Cancel"::"ChangeSK"::"InputStyle"::"Clear"::
      nContractTricksButtons.values.toList:::    // Passed out is not always there
      contractSuitButtons.values.toList:::
      doubledButtons.values.toList:::
      declarerButtons.values.toList

  val patternInputStyle: Regex = """Input Style: (.*)""".r

  val validInputStyles: List[String] = "Guide"::"Prompt"::"Original"::Nil

  import com.github.thebridsk.browserpages.PageBrowser._
  def getInputStyle(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Option[String] = {
    find(id("InputStyle")).text match {
      case patternInputStyle(s) => Some(s)
      case _ => None
    }
  }

  /**
   * @param style the input style, valid values are Guide, Prompt, Original
   * @return Some(style) if successful, otherwise returns current input style
   */
  def setInputStyle( want: String )( implicit webDriver: WebDriver, pos: Position ): Option[String] = {
    if (!validInputStyles.contains(want)) fail(s"""Specified style, ${want} is not valid, must be one of ${validInputStyles}""")
    val stop = Some(want)
    var last: Option[String] = None
    for ( i <- 1 to 3 ) {
      last = getInputStyle
      if (last == stop) return stop
      click on id("InputStyle")
//      Thread.sleep(100L)
    }
    last
  }
}

abstract class BaseHandPage[T <: Page[T]]( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[T] {
  baseself: T =>

  import BaseHandPage._


  def validate(implicit patienceConfig: PatienceConfig, pos: Position): BaseHandPage[T] with T = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    eventually {
//      val but = findButton("Ok")
      val but = findButtons(alwaysButtons: _*)
    }

    this
  }

  def clickContractTrick( i: Int )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = {
    val b = contractTricksButtons(i)
    try {
      clickButton(b)
    } catch {
      case x: Exception =>
        log.warning(s"Did not find button $b, looking for $i")
        throw x
    }
  }

  def clickContractSuit( suit: ContractSuit )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( contractSuitButtons(suit) )

  def clickContractDoubled( doubled: ContractDoubled )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( doubledButtons(doubled) )

  def clickDeclarer( dec: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( declarerButtons(dec) )

  def clickMadeOrDown( madeOrDown: MadeOrDown )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( madeDownButtons(madeOrDown))

  def clickTricks( i: Int )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( tricksButtons(i) )

  def clickHonorsPlayer( dec: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( honPlayButtons(dec) )

  def clickHonors( i: Int )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( honorsPoints(i) )

  def clickOk(implicit patienceConfig: PatienceConfig, pos: Position): Page.AnyPage = {
    val ok = findElemById("Ok")
    scrollToElement(ok)
    ok.enter
//    clickButton( "Ok" )
    GenericPage.current
  }

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position): Page.AnyPage = {
    clickButton( "Cancel" )
    GenericPage.current
  }

  def clickClear(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( "Clear" )

  def clickChangeScorekeeper(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( "ChangeSK" )

  def clickInputStyle(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickButton( "InputStyle" )

  def isOkEnabled(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    getButton("Ok").isEnabled
  }

  def enterPassedOutContract(implicit patienceConfig: PatienceConfig, pos: Position): this.type = clickContractTrick(0)

  /**
   * Enter the contract.
   * @param contractTricks
   * @param contractSuit
   * @param contractDoubled
   * @param declarer
   * @param madeOrDown
   * @param tricks
   * @param honors if None, honor points are not entered
   * @param honorsPlayer if None, honors player is not entered
   * @param patienceConfig
   * @param pos
   * @return this
   */
  def enterContract(
        contractTricks: Int,
        contractSuit: ContractSuit,
        contractDoubled: ContractDoubled,
        declarer: PlayerPosition,
        madeOrDown: MadeOrDown,
        tricks: Int,
        honors: Option[Int] = None,
        honorsPlayer: Option[PlayerPosition] = None
      )(implicit
          patienceConfig: PatienceConfig,
          pos: Position
      ): this.type = logMethod0(s"""Entering contract ${contractTricks} ${contractSuit.suit} ${contractDoubled.doubled} by ${declarer.pos} ${madeOrDown.made} ${tricks} ${honors} ${honorsPlayer}""") {

    clickContractTrick(contractTricks)
    if (contractTricks != 0) {
      clickContractSuit(contractSuit)
      clickContractDoubled(contractDoubled)
      clickDeclarer(declarer)
      honors.foreach( h => clickHonors(h) )
      honorsPlayer.foreach( p => clickHonorsPlayer(p))
      clickMadeOrDown(madeOrDown)
      if (tricks > 0) clickTricks(tricks)
    }
    this
  }

  def isContractTricksVisible(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    find(cssSelector("""div.handViewContractTricks""")).isDisplayed
  }

  def isContractSuitVisible(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    find(cssSelector("""div.handViewContractSuit""")).isDisplayed
  }

  def isContractDoubledVisible(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    find(cssSelector("""div.handViewContractDoubled""")).isDisplayed
  }

  def isDeclarerEnabled(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    getButton("DecN").isEnabled
  }

  def isMadeOrDownVisible(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    find(cssSelector("""div.handViewMadeOrDown""")).isDisplayed
  }

  def isTricksVisible(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    find(cssSelector("""div.handViewTricksInner""")).isDisplayed
  }

  def isHonorsPointsVisible(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    find(cssSelector("""div.handViewHonorsPoints""")).isDisplayed
  }

  def isHonorPlayersVisible(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = eventually {
    find(cssSelector("""div.handViewHonorsPlayers""")).isDisplayed
  }

  def isRubberBridge(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    findOption(cssSelector("""div.handViewHonors""")).isEmpty
  }

  def isDuplicateBridge(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    findOption(cssSelector("""div.handViewTableBoard""")).isEmpty
  }

  def isChicagoBridge(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    !isDuplicateBridge && !isRubberBridge
  }

  /**
   * Get the selected contract tricks.
   * @return None if nothing is selected, Some(0) if passed out, Some(n) n is number of tricks
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedContractTricks(implicit patienceConfig: PatienceConfig, pos: Position): Option[Int] = {
    getSelected( "ContractTricks", contractTricksButtons, "CT" )
  }

//  def getSelectedContractTricks(implicit patienceConfig: PatienceConfig, pos: Position): Option[Int] = {
//    getSelected( "ContractTricks", nContractTricksButtons ) match {
//      case Some(s) => Some(s)
//      case None =>
//        try {
//          if (findButton(passedOutButton).attribute("class").map{ s => s.indexOf("baseButtonSelected")>0 }.getOrElse(false)) Some(0)
//          else None
//        } catch {
//          case x: TestFailedException => None
//        }
//    }
//
//  }

  /**
   * Get the selected contract suit.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedContractSuit(implicit patienceConfig: PatienceConfig, pos: Position): Option[ContractSuit] = {
    getSelected( "Suit", contractSuitButtons, "CS")
  }

  /**
   * Get the selected contract doubled.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedContractDoubled(implicit patienceConfig: PatienceConfig, pos: Position): Option[ContractDoubled] = {
    getSelected( "Doubled", doubledButtons, "Doubled" )
  }

  /**
   * Get the selected declarer.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedDeclarer(implicit patienceConfig: PatienceConfig, pos: Position): Option[PlayerPosition] = {
    getSelected( "Declarer", declarerButtons, "Dec" )
  }

  /**
   * Get the selected made or down.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedMadeOrDown(implicit patienceConfig: PatienceConfig, pos: Position): Option[MadeOrDown] = {
    val x = """//button[@id='made' or @id='down'][contains(concat(' ', @class, ' '), ' baseButtonSelected ')]"""
    val selected = findElemsByXPath(x)
    selected.size mustBe 1
    val sel = selected.head.attribute("id").get

    madeDownButtons.find( e => e._2==sel ).map(e => e._1)

//    getSelected( "MadeOrDown", madeDownButtons )
  }

  def findTrickButtons(implicit patienceConfig: PatienceConfig, pos: Position): List[Int] = {
    val x = s"""//button[starts-with( @id, 'T' )]"""

    val tbuttons = findElemsByXPath(x).map(e => e.attribute("id").getOrElse("<Unknown>"))

    tricksButtons.flatMap(e => if (tbuttons.contains(e._2)) e._1::Nil else Nil ).toList

//    findElemsByXPath("//button").flatMap(b => b.attribute("id") match {
//      case Some(patternTricksToInt(i)) =>
//        i.toInt::Nil
//      case _ =>
//        Nil
//    })
  }

  /**
   * Get the selected trick.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedTricks(implicit patienceConfig: PatienceConfig, pos: Position): Option[Int] = {
    getSelected("Tricks", tricksButtons, "T" )
//    val selected = findElemsByXPath("//button").flatMap(b => b.attribute("id") match {
//      case Some(patternTricksToInt(i)) if (b.attribute("class").map{ s => s.indexOf("baseButtonSelected")>0 }.getOrElse(false)) =>
//        i.toInt::Nil
//      case _ =>
//        Nil
//    })
//    if (selected.size > 1) fail(s"${pos.line}: more than on trick is selected: ${selected}")
//    selected.headOption
  }

  /**
   * Get the selected made or down.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedHonors(implicit patienceConfig: PatienceConfig, pos: Position): Option[Int] = {
    getSelected( "Honors", honorsPoints, "Honors" )
  }

  /**
   * Get the selected made or down.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedHonorsPlayer(implicit patienceConfig: PatienceConfig, pos: Position): Option[PlayerPosition] = {
    getSelected( "HonorsPlayer", honPlayButtons, "HonPlay" )
  }

  private def getSelected[T]( name: String, map: Map[T,String], prefix: String ): Option[T] = logMethod(s"getSelected($name)"){

    val x = s"""//button[starts-with( @id, '${prefix}' ) and contains(concat(' ', @class, ' '), ' baseButtonSelected ')]"""

    val elems = findElemsByXPath(x)

    if (elems.isEmpty) None
    else {
      elems.size mustBe 1
      val id = elems.head.attribute("id").get
      log.fine(s"Looking for button with id ${id} in ${map}")
      map.find( e => e._2==id).map(e => e._1)
    }

//    val buttons = findButtons( map.values.toList: _* )
//
//    buttons.flatMap{ case (id,we) => if (we.attribute("class").map{ s => s.indexOf("baseButtonSelected")>0 }.getOrElse(false)) id::Nil else Nil }.toList match {
//      case Nil =>
//        None
//      case List( s ) =>
//        val suits = map.flatMap{ e => if (e._2 == s) e._1::Nil else Nil }.toList
//        if (suits.size != 1) fail(s"Did not find ${name} object for ${suits}")
//        suits.headOption
//      case l =>
//        fail(s"Multiple ${name} are selected ${l}")
//    }
  }

  def getScore(implicit patienceConfig: PatienceConfig, pos: Position): (String,String,String) = logMethod("getScore"){
    eventually {
      findElemsByXPath( "//div[contains(concat(' ', @class, ' '), ' handSectionScore ')]/div" ) match {
        case List( r1, r2, r3 ) => ( r1.text, r2.text, r3.text )
        case List( r1, r3 ) =>
          findElemsByXPath( "//div[contains(concat(' ', @class, ' '), ' contractAndResult ')]" ) match {
            case List( r2 ) => ( r1.text, r2.text, r3.text )
            case x => ( r1.text, "", r3.text )
          }

        case x => fail(s"Did not get exactly three elements: $x")
      }
    }
  }

  def getInputStyle(implicit patienceConfig: PatienceConfig, pos: Position): Option[String] =
    BaseHandPage.getInputStyle

  /**
   * @param style the input style, valid values are Guide, Prompt, Original
   * @return Some(style) if successful, otherwise returns current input style
   */
  def setInputStyle( style: String ): Option[String] =
    BaseHandPage.setInputStyle(style)

  /**
   * Validate that all the selected buttons match the arguments.
   * If any of the arguments is None, then none of those buttons must be selected.
   * @param contractTricks
   * @param contractSuit
   * @param contractDoubled
   * @param declarer
   * @param madeOrDown
   * @param tricks
   * @param checkHonors if true check for honors
   * @param honors
   * @param honorsPlayer
   * @param patienceConfig
   * @param pos
   */
  def validateContract(
        contractTricks: Option[Int],
        contractSuit: Option[ContractSuit],
        contractDoubled: Option[ContractDoubled],
        declarer: Option[PlayerPosition],
        madeOrDown: Option[MadeOrDown],
        tricks: Option[Int],
        checkHonors: Boolean = false,
        honors: Option[Int] = None,
        honorsPlayer: Option[PlayerPosition] = None
      )(implicit
          patienceConfig: PatienceConfig,
          pos: Position
      ): this.type = {

    log.info(s"""validate contract ${contractTricks} ${contractSuit} ${contractDoubled} by ${declarer.get.pos} ${madeOrDown} ${tricks} ${honors} ${honorsPlayer}""")

    val x = convertToAnyMustWrapper(declarer)

    def compare[T]( value: Option[T], selected: =>Option[T] ) = {
      selected match {
        case Some(s) =>
          if (value.isDefined) s mustBe value.get
          else fail(s"${pos.line}: selected is $s, expecting $value")
        case None =>
          if (value.isDefined) fail(s"${pos.line}: selected is None, expecting $value")
      }
    }

    compare( contractTricks, getSelectedContractTricks )

    if (contractTricks.getOrElse(1) != 0) {
      compare( contractSuit, getSelectedContractSuit )
      compare( contractDoubled, getSelectedContractDoubled )
      compare( declarer, getSelectedDeclarer )
      compare( madeOrDown, getSelectedMadeOrDown )
      compare( tricks, getSelectedTricks )
      if (checkHonors) {
        compare( honors, getSelectedHonors )
        compare( honorsPlayer, getSelectedHonorsPlayer )
      }
    }
    this
  }

  def enterContractAndOk(
      contractTricks: Int,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      madeOrDown: MadeOrDown,
      tricks: Int,
      vul: Vulnerability,
      validate: Boolean = true
    )(implicit
        patienceConfig: PatienceConfig,
        pos: Position
    ): Page.AnyPage = {
    enterContract(contractTricks,contractSuit,contractDoubled,declarer,madeOrDown,tricks)
    if (validate) validateContract(Some(contractTricks),Some(contractSuit),Some(contractDoubled),Some(declarer),Some(madeOrDown),Some(tricks))
    isOkEnabled mustBe true
    clickOk
  }

  def findVulnerableElement(
                    loc: PlayerPosition
                  )(implicit
                     patienceConfig: PatienceConfig,
                     pos: Position
                  ): Element = {
    val e = find(xpath(s"""//span[@id = '${loc.name}']/span[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')]"""))
    e
  }

  def isVulnerable(
                    loc: PlayerPosition
                  )(implicit
                     patienceConfig: PatienceConfig,
                     pos: Position
                  ): Boolean = {
    val e = findVulnerableElement(loc)
    e.containsClass("handVulnerable")
  }

  def getVulnerable(
                    loc: PlayerPosition
                  )(implicit
                     patienceConfig: PatienceConfig,
                     pos: Position
                  ): Boolean = {
    eventually {
      val e = findVulnerableElement(loc)
      e.containsClass("handVulnerable")
    }
  }

  val buttonPlayerText: Regex = """(.*?) [vV]ul""".r

  def getName(
               loc: PlayerPosition
             )(implicit
                patienceConfig: PatienceConfig,
                pos: Position
             ): String = {
    find(xpath(s"""//span[@id = '${loc.name}']""")).text match {
      case buttonPlayerText(name) => name
      case s =>
        fail(s"Player button text is bad: ${s}")
    }
  }

  /**
   * Get the name and vulnerability of the person sitting at the specifiec location.
   * @param loc
   * @param patienceConfig
   * @param pos
   * @return a string that contains both the name and vulnerability.  Syntax:
   *    name sp vul
   * Where:
   *   name is the players name
   *   sp is a space
   *   vul is "vul" for not vulnerable, and "Vul" for vulnerable.
   */
  def getNameAndVul(
                     loc: PlayerPosition
                   )(implicit
                      patienceConfig: PatienceConfig,
                      pos: Position
                   ): String = {
    find(xpath(s"""//span[@id = '${loc.name}']""")).text
  }

  def checkDealer( name: String )(implicit patienceConfig: PatienceConfig, pos: Position): BaseHandPage[T] with T = {
    getElemById("Dealer").text mustBe name.trim
    this
  }

  def checkVulnerable( loc: PlayerPosition, vul: Vulnerability )(implicit patienceConfig: PatienceConfig, pos: Position): Assertion = {
    withClue(s"Checking vulnerability of ${loc} for ${vul}") {
      getVulnerable(loc) mustBe vul.vul
    }
  }
}
