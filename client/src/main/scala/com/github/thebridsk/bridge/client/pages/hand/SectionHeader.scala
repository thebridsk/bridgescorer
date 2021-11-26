package com.github.thebridsk.bridge.client.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.client.pages.hand.PageHandInternal.PageHandNextInput
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.InputMethod
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.utilities.logging.Logger

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * Component( Component.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object SectionHeader {

  case class Props(
      scoringSystem: ScoringSystem,
      table: Int,
      board: Int,
      dealer: PlayerPosition,
      currentDeclarer: Option[PlayerPosition],
      north: String,
      south: String,
      east: String,
      west: String,
      currentNSVul: Vulnerability,
      currentEWVul: Vulnerability,
      callbackDeclarer: ViewDeclarer.CallbackPlayer,
      callbackNSVul: Option[ViewVulnerability.CallbackVul],
      callbackEWVul: Option[ViewVulnerability.CallbackVul],
      teamNS: Option[Team.Id],
      teamEW: Option[Team.Id],
      currentContractTricks: Option[ContractTricks],
      currentSuit: Option[ContractSuit],
      nextInput: PageHandNextInput.Value,
      callbackHonors: Option[ViewHonors.CallbackHonors],
      callbackHonorsPlayer: Option[ViewHonors.CallbackHonorsPlayer],
      currentHonors: Option[Int],
      currentHonorsPlayer: Option[PlayerPosition],
      playingDuplicate: Boolean
  )

  def apply(
      scoringSystem: ScoringSystem,
      currentDeclarer: Option[PlayerPosition],
      north: String,
      south: String,
      east: String,
      west: String,
      currentNSVul: Vulnerability,
      currentEWVul: Vulnerability,
      callbackDeclarer: ViewDeclarer.CallbackPlayer,
      callbackNSVul: Option[ViewVulnerability.CallbackVul],
      callbackEWVul: Option[ViewVulnerability.CallbackVul],
      dealer: PlayerPosition,
      table: Int = 0,
      board: Int = 0,
      teamNS: Option[Team.Id] = None,
      teamEW: Option[Team.Id] = None,
      currentContractTricks: Option[ContractTricks] = None,
      currentSuit: Option[ContractSuit] = None,
      nextInput: PageHandNextInput.Value,
      callbackHonors: Option[ViewHonors.CallbackHonors],
      callbackHonorsPlayer: Option[ViewHonors.CallbackHonorsPlayer],
      currentHonors: Option[Int],
      currentHonorsPlayer: Option[PlayerPosition],
      playingDuplicate: Boolean = false
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props(
        scoringSystem,
        table,
        board,
        dealer,
        currentDeclarer,
        north,
        south,
        east,
        west,
        currentNSVul,
        currentEWVul,
        callbackDeclarer,
        callbackNSVul,
        callbackEWVul,
        teamNS,
        teamEW,
        currentContractTricks,
        currentSuit,
        nextInput,
        callbackHonors,
        callbackHonorsPlayer,
        currentHonors,
        currentHonorsPlayer,
        playingDuplicate
      )
    )

  private val log = Logger("bridge.hand")

  private val component = ScalaComponent
    .builder[Props]("SectionHeader")
    .render_P(props => {
      import HandStyles._
      import InputMethod._
      import PageHandNextInput._
      val playingDuplicate = props.teamEW.isDefined || props.teamNS.isDefined || props.playingDuplicate
      def show(view: PageHandNextInput) = {
        val r =
          ComponentInputStyleButton.inputMethod != Prompt || view <= props.nextInput
        val shbt =
          !(props.currentContractTricks.isDefined && props.currentContractTricks.get.tricks == 0)
        r && shbt
      }
      def playerAtPosition(pos: PlayerPosition) = {
        val p = pos match {
          case North => props.north
          case South => props.south
          case East  => props.east
          case West  => props.west
        }
        log.fine(s"Dealer is ${pos.name}, player is $p")
        p
      }
      <.div(
        handStyles.sectionHeader,
        if (props.scoringSystem.isInstanceOf[Duplicate]) {
          ViewTableBoard(
            props.table,
            props.board,
            props.dealer,
            playerAtPosition(props.dealer)
          )
//                                    } else if (props.callbackHonors.isDefined) {
//                                      ViewHonors(props.currentHonors, props.currentHonorsPlayer,
//                                                 props.currentContractTricks,
//                                                 props.currentSuit,
//                                                 props.callbackHonors.get,
//                                                 props.callbackHonorsPlayer.get,
//                                                 props.nextInput)
        } else {
          <.div(
            handStyles.viewDealer,
            "Dealer:",
            <.br,
            <.span(^.dangerouslySetInnerHtml := "&nbsp;&nbsp;"),
            <.span(^.id := "Dealer", playerAtPosition(props.dealer))
          )
        },
        ViewDeclarer(
          props.currentDeclarer,
          props.north,
          props.south,
          props.east,
          props.west,
          props.callbackDeclarer,
          props.teamNS,
          props.teamEW,
          props.nextInput,
          show(InputContractBy),
          props.currentNSVul.vul,
          props.currentEWVul.vul,
          props.playingDuplicate
        )
//                                    props.scoringSystem.isInstanceOf[Duplicate] ?= ViewVulnerability( props.currentNSVul, props.currentEWVul, props.callbackNSVul, props.callbackEWVul)
      )
    })
    .build
}
