package com.example.test.selenium

import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import com.example.data.util.Strings
import scala.collection.convert.ImplicitConversionsToScala._
import utils.logging.Logger
import com.example.data.bridge._
import com.example.test.util.NoResultYet
import scala.concurrent.duration._
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Eventually.PatienceConfig
import com.example.data.BoardSet
import com.example.data.Id
import org.openqa.selenium.Keys
import com.example.pages.Combobox
import com.example.pages.TextField
import com.example.pages.Element

trait DuplicateUtils {
  import org.scalatest.MustMatchers._
  import com.example.test.util.EventuallyUtils._
  import com.example.test.util.ParallelUtils._
  import com.example.pages.PageBrowser._

  import scala.language.postfixOps

  private val testlog = Logger[DuplicateUtils]

  val urlprefix = TestServer.getAppPageUrl("duplicate/")

  def findXPath( xpath: String )( implicit webDriver: Session ): Option[WebElement] = {
    val list = webDriver.findElements(By.xpath(xpath))
    if (list.isEmpty()) None
    else if (list.size() != 1) None
    else Some( list.get(0) )
  }

  def checkDrawInBoards( boards: List[String] )( implicit webDriver: WebDriver ) = {
    val p = List(Strings.half,Strings.half,Strings.half,Strings.half)
    boards.foreach { b => checkPointsInBoard(b,p) }
  }

  def checkPointsInBoard( board: String, points: List[String])( implicit webDriver: WebDriver ) = {
    val p = webDriver.findElements(By.xpath("//table[@id='Board_B"+board+"']/tbody/tr/td[9]")).toList.map { td => td.getText }
    p mustBe points
  }

  /**
   * @return the duplicate id of the match
   */
  def checkForDuplicate()(implicit webDriver: WebDriver): Option[String] = {
    val cUrl = currentUrl
    testlog.fine( "currentUrl: "+cUrl )
    testlog.fine( "prefix    : "+urlprefix)
    if (cUrl.startsWith(urlprefix)) {
      val id = cUrl.substring(urlprefix.length())
      if (id.startsWith("M")) Some( id )
      else None
    } else {
      None
    }
  }

//  def playEnterNamesAutomaticSwap(north: String, south: String, east: String, west: String )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {
//    eventually({ find(id( "Ok" ))})
//    find(id("Player_North")) match {
//      case None =>
//        enterNamesAndClickOk(north, south, east, west)
//      case Some(pNelement) =>
//        val pN = pNelement.text
//        val pS = find(id("Player_South")).get.text
//        val pE = find(id("Player_East")).get.text
//        val pW = find(id("Player_West")).get.text
//
//        checkAndMaybeSwapPlayers( pN, pS, north, south, "North" )
//        checkAndMaybeSwapPlayers( pE, pW, east, west, "East" )
//        click on id("Ok")
//    }
//
//  }

  def playEnterNames(north: String, south: String, east: String, west: String,
                     scorekeeper: PlayerPosition
                    )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {
    eventually({ find( id( "OK" ))})
    val inputs = findAllTextInputs
    if (inputs.contains("Scorekeeper")) {
      enterNamesAndClickOk(north, south, east, west,scorekeeper)
    } else if (inputs.contains("I_N")) {
      inputs("I_N").value = north
      inputs("I_S").value = south
      esc
      click on id("OK")
      selectNamesAndClickButton(north, south, east, west, "OK", scorekeeper)
    } else if (inputs.contains("I_E")) {
      inputs("I_E").value = east
      inputs("I_W").value = west
      esc
      click on id("OK")
      selectNamesAndClickButton(north, south, east, west, "OK", scorekeeper)
    } else {
      selectNamesAndClickButton(north, south, east, west, "OK", scorekeeper)
    }

  }

  def playRound(round: Int,
                north: String, south: String, east: String, west: String,
                nsteam: String, ewteam: String,
                scorekeeper: PlayerPosition,
                hands: List[PlayedHand],
                boardSet: BoardSet,
                wantInputStyleText: String = "Original" )(implicit webDriver: WebDriver): Unit = {
    InputStyleHelper.hitInputStyleButton( wantInputStyleText )
    hitRound(round)
    playEnterNames(north, south, east, west, scorekeeper)
    playRoundBoards(north, south, east, west, nsteam,ewteam, hands, boardSet)
  }

  def maybeSwapPlayers( pPlayer1: String, pPlayer2: String,
                        player1: String, player2: String,
                        mustSwap: Boolean,
                        swapButton: String )(implicit webDriver: WebDriver) = {
    if (pPlayer1==player1 && pPlayer2==player2) {
      // no need to swap, players are correct
      mustSwap mustBe false
    } else if (pPlayer1==player2 && pPlayer2==player1) {
      // swap players
      mustSwap mustBe true
      click on id(swapButton)
    } else {
      fail("NS Players don't match, on page "+pPlayer1+","+pPlayer2+" want "+player1+","+player2 )
    }
  }

  def checkAndMaybeSwapPlayers( pPlayer1: String, pPlayer2: String,
                                player1: String, player2: String,
                                swapButton: String )(implicit webDriver: WebDriver) = {
    if (pPlayer1==player1 && pPlayer2==player2) {
      // no need to swap, players are correct
    } else if (pPlayer1==player2 && pPlayer2==player1) {
      // swap players
      click on id(swapButton)
    } else {
      fail("Players don't match, on page "+pPlayer1+","+pPlayer2+" want "+player1+","+player2 )
    }
  }

  /**
   * @param id the board id, starts with "Board_B"
   */
  def boardButtonIdToNumber( id: String ) = {
    if (id.startsWith("Board_B")) {
      id.drop(7).toInt
    }
    else 0
  }

  /**
   * @param id the board id, starts with "Board_B"
   */
  def boardIdToNumber( id: String ) = {
    if (id.startsWith("B")) {
      id.drop(1).toInt
    }
    else 0
  }

  /**
   * play the hands in a round.
   * The page #duplicate/M2/table/2/round/1/game must be displayed
   */
  def playRoundBoards(north: String, south: String, east: String, west: String,
                nsteam: String, ewteam: String,
                hands: List[PlayedHand],
                boardSet: BoardSet
               )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {
    eventually({ find(id("Game")) })
    val buttons = findAll(xpath("//button") )

    val boardButtons = buttons.toList.flatMap { we => we.id match {
      case Some(id) if (id.startsWith("Board_B")) => id::Nil
      case _ => Nil
    }}
    boardButtons.length mustBe hands.length

    (boardButtons, hands).zipped.foreach { (buttonid,hand) =>
      val boardid = boardButtonIdToNumber(buttonid)
      val board = boardSet.boards.find(b => b.id == boardid).get
      val nsvul = board.nsVul
      val ewvul = board.ewVul
      playHand(buttonid, north, south, east, west, nsteam,ewteam,nsvul,ewvul, hand, boardSet)
    }
    click on id("Game")
    eventually({ find(id("AllBoards"))})

  }

  /**
   * Play a hand.
   * The starting and ending page must be
   * #duplicate/M2/table/2/round/1/game or #duplicate/M2/table/2/round/1/board/n
   */
  def playHand( board: String, north: String, south: String, east: String, west: String,
                nsteam: String, ewteam: String, nsvul: Boolean, ewvul: Boolean,
                hand: PlayedHand,
                boardSet: BoardSet
              )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position) = {
    eventually {click on id(board)}
//    eventuallySome{ find(id(board)) } match {
//      case Some(el) => click on el
//      case None => fail("Did not find board button: "+board )
//    }

//    eventuallySome{ find(id("Game")) }
//    val buttons = webDriver.findElements( By.xpath("//button") )
//    val handButtons = buttons.toList.filter { we => Option(we.getAttribute("id")) match {
//      case Some(id) if (id.startsWith("Hand_T")) => true
//      case _ => false
//    }}.map { we => we.getAttribute("id") }
//    handButtons.length mustBe 1
//    click on id(handButtons.head)
//    println("Board button id is "+board+" boardset is "+boardSet )
    val boardid = Id.boardNumberToId( boardButtonIdToNumber(board) )
    val bid = Id.boardIdToBoardNumber(boardid).toInt
    val dealer = boardSet.boards.find(b => b.id == bid) match {
      case Some(bis) =>
        bis.dealer match {
          case "N" => Some(north)
          case "S" => Some(south)
          case "E" => Some(east)
          case "W" => Some(west)
          case _ => None
        }
      case None => None
    }

    dealer.foreach( d => testlog.info("Dealer for board "+board+": "+d) )
    enterHand(hand,north,south,east,west, nsteam,ewteam,nsvul,ewvul, dealer)
    eventually({ find(id("Game"))})
  }

  case class PlayedHand( contractTricks: ContractTricks,
                         contractSuit: ContractSuit,
                         contractDoubled: ContractDoubled,
                         declarer: PlayerPosition,
                         madeOrDown: MadeOrDown,
                         tricks: Int
                       )

  def enterHand( hand: PlayedHand,
                 north: String, south: String, east: String, west: String,
                 nsteam: String, ewteam: String,
                 nsvul: Boolean, ewvul: Boolean,
                 dealer: Option[String]
               )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position) = {
    eventually{ find(id("Ok")) }

    dealer.foreach( dealerName => eventually {
      val t = find(id("Dealer")).text
      testlog.info("Found dealer "+t+" looking for "+dealerName)
      t
    } mustBe dealerName )

    find(id("VerifySectionHeader")).text mustBe "Bridge Scorer:"

    implicit val isPrompt = eventually { find(id("InputStyle" )).text.contains("Prompt") }
//    implicit val isPrompt = (eventuallySome {
//      find(id("InputStyle")) match {
//        case Some(b) => Some(b.text.contains("Prompt"))
//        case None => None
//      }
//    }).getOrElse(false)

    def waitAndClick( buttonId: String )(implicit dowait: Boolean) = {
      if (dowait) {
        eventuallySome( {
          val b = find(id(buttonId))
          if (b.isEnabled && b.isDisplayed) Some(b)
          else None
        })
      }
      click on id(buttonId)
    }

    def buttonText( player: String, team: String, vul: Boolean, pos: String ) = {
      val svul = if (vul) "Vul" else "vul"
      s"""$player ($team) $svul $pos"""
    }

    hand.contractTricks match {
      case ContractTricks(0) =>
        waitAndClick("CTPassed")
      case _ =>
        waitAndClick("CT"+hand.contractTricks.tricks)
        waitAndClick("CS"+hand.contractSuit.suit)
        waitAndClick("Doubled"+hand.contractDoubled.doubled)
        eventually({

          find(id("DecN")).text mustBe buttonText(north,nsteam,nsvul,"North")
          find(id("DecS")).text mustBe buttonText(south,nsteam,nsvul,"South")
          find(id("DecE")).text mustBe buttonText(east,ewteam,ewvul,"East")
          find(id("DecW")).text mustBe buttonText(west,ewteam,ewvul,"West")

        })
        waitAndClick("Dec"+hand.declarer.pos)
        waitAndClick(hand.madeOrDown.forScore)
        waitAndClick("T"+hand.tricks)
    }
    tcpSleep(2)
    waitAndClick("Ok")(true)
  }

  def hitRound( round: Int )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position) = {
    eventually { click on find(id("Round_"+round)) }
//    val rid = "Round_"+round
//    eventuallySome{ find(id(rid)) } match {
//      case Some(el) => click on el
//      case None => fail("Did not find round button: "+rid)
//    }
  }

  /**
   * Enter the player names and click the specified button.
   * Must be on page #duplicate/M1/table/1/round/3/teams
   */
  def enterNamesAndClickButton( nsPlayer1: String, nsPlayer2: String,
                                ewPlayer1: String, ewPlayer2: String,
                                button: String,
                                scorekeeper: PlayerPosition
                              )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    val (scorekeeperName, partner) = scorekeeper match {
      case North => (nsPlayer1,South)
      case South => (nsPlayer2,North)
      case East => (ewPlayer1,West)
      case West => (ewPlayer2,East)
    }

    val inputScorekeeper = eventually {
      val e = find(name("Scorekeeper"))
      e.attribute("type").get mustBe "text"
      new Combobox(e)
    }
    inputScorekeeper.value = scorekeeperName

    esc

    tcpSleep(1)

    eventually( click on id(s"SK_${scorekeeper.pos}") )

    eventually( click on id("OK") )

    val inputs1 = eventually( {
       val map = findAllInputs
       val List(a,b,c) = (North::South::East::West::Nil).filter(p=>p!=scorekeeper).map(p=>s"I_${p.pos}").toList
       map.keySet must contain allOf (a,b,c)
       map
    } )

    tcpSleep(2)

    find(id(s"Player_${scorekeeper.pos}")).text mustBe scorekeeperName

    inputs1.foreach { entry => {
      val (name, input) = entry
      val t = new TextField(input)
      name match {
        case "I_N" => t.value=nsPlayer1
        case "I_S" => t.value=nsPlayer2
        case "I_E" => t.value=ewPlayer1
        case "I_W" => t.value=ewPlayer2
        case x => testlog.fine( "Found an input field with name "+x )
      }
    }}

//    pressKeys(Keys.chord(Keys.ENTER))
    tcpSleep(2)

    testNames(nsPlayer1, nsPlayer2, ewPlayer1, ewPlayer2,scorekeeper)

    click on id(button)
  }

  /**
   * Select the player names and click the specified button.
   * Must be on page #duplicate/M1/table/1/round/3/teams
   */
  def selectNamesAndClickButton( nsPlayer1: String, nsPlayer2: String,
                                 ewPlayer1: String, ewPlayer2: String,
                                 button: String,
                                 scorekeeper: PlayerPosition
                               )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    val (scorekeeperName, partner) = scorekeeper match {
      case North => (nsPlayer1,South)
      case South => (nsPlayer2,North)
      case East => (ewPlayer1,West)
      case West => (ewPlayer2,East)
    }

    eventually( click on id(s"P_$scorekeeperName") )

    eventually( click on find(id(s"SK_${scorekeeper.pos}")) )

    eventually( click on id("OK") )

    val inputs1 = List("Player_N", "Player_S", "Player_E", "Player_W").map( p => {
      val e = find(id(p))
      ( p, e)
    }).toMap

    tcpSleep(2)

    var swap = false

    inputs1.foreach { entry => {
      val (name, input) = entry
      val t = input.text
      name match {
        case "Player_N" => swap = swap || (North!=scorekeeper&&North!=partner&&t != nsPlayer1)
        case "Player_S" => swap = swap || (South!=scorekeeper&&South!=partner&&t != nsPlayer2)
        case "Player_E" => swap = swap || (East!=scorekeeper&&East!=partner&&t != ewPlayer1)
        case "Player_W" => swap = swap || (West!=scorekeeper&&West!=partner&&t != ewPlayer2)
        case x => testlog.fine( "Found an input field with name "+x )
      }
    }}

    if (swap) {
      tcpSleep(2)
      click on id("Swap_left")
    }
//    pressKeys(Keys.chord(Keys.ENTER))
    tcpSleep(2)

    testNamesNoInput(nsPlayer1, nsPlayer2, ewPlayer1, ewPlayer2)

    click on id(button)
  }

  /**
   * Enter the player names.
   * Must be on page #duplicate/M1/table/1/round/3/teams
   */
  def enterNamesAndClickOk( nsPlayer1: String, nsPlayer2: String,
                            ewPlayer1: String, ewPlayer2: String,
                            scorekeeper: PlayerPosition
                          )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    enterNamesAndClickButton(nsPlayer1, nsPlayer2, ewPlayer1, ewPlayer2, "OK", scorekeeper)
  }

  def testNames( nsPlayer1: String, nsPlayer2: String,
                 ewPlayer1: String, ewPlayer2: String,
                 scorekeeper: PlayerPosition
               )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    val (scorekeeperName, partner) = scorekeeper match {
      case North => (nsPlayer1,South)
      case South => (nsPlayer2,North)
      case East => (ewPlayer1,West)
      case West => (ewPlayer2,East)
    }

    find(id(s"Player_${scorekeeper.pos}")).text mustBe scorekeeperName

    eventually({
      val inputs = findAll(xpath("//input"))
      inputs.size mustBe 3
      inputs.foreach { input => {
        val t = new TextField(input)
        input.attribute("name") match {
          case Some("I_N") => t.value mustBe nsPlayer1
          case Some("I_S") => t.value mustBe nsPlayer2
          case Some("I_E") => t.value mustBe ewPlayer1
          case Some("I_W") => t.value mustBe ewPlayer2
          case x => testlog.fine( "Found an input field with name "+x )
        }
      }}
    })
  }

  def testNamesNoInput( nsPlayer1: String, nsPlayer2: String,
                        ewPlayer1: String, ewPlayer2: String
                      )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {
    eventually({
      val inputs1 = List("Player_N", "Player_S", "Player_E", "Player_W").map( p => {
        val e = find(id(p))
        ( p, e)
      }).toMap

      inputs1.foreach { entry => {
        val (name, input) = entry
        val t = input.text
        name match {
          case "Player_N" => t mustBe nsPlayer1
          case "Player_S" => t mustBe nsPlayer2
          case "Player_E" => t mustBe ewPlayer1
          case "Player_W" => t mustBe ewPlayer2
          case x => testlog.fine( "Found a field with name "+x )
        }
      }}
    })
  }

  /**
   * Enter the player names.
   * Must be on page #duplicate/M1/table/1/round/3/teams
   */
  def enterAndResetNames( nsPlayer1: String, nsPlayer2: String,
                          ewPlayer1: String, ewPlayer2: String,
                          scorekeeper: PlayerPosition
                )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    enterNamesAndClickButton(nsPlayer1, nsPlayer2, ewPlayer1, ewPlayer2, "Reset", scorekeeper)

    eventually{
      val e = new TextField(find(id("Scorekeeper")))
      e.text mustBe "text"
      e.value mustBe ""
    }

  }

  def checkUrl( url: String )(implicit webDriver: WebDriver) = {
    currentUrl == url
  }

  def checkUrlThrowsNoResultYet( url: String)(implicit webDriver: WebDriver) = {
    if (!checkUrl(url)) throw new NoResultYet
    true
  }

  def clickButton( id: String, map: Map[String,Element] )(implicit webDriver: WebDriver ): Unit = {
    map.get(id) match {
      case Some(e) => click on e
      case None => fail( "Did not find the '"+id+"' button")
    }
  }

}

object DuplicateUtils extends DuplicateUtils

