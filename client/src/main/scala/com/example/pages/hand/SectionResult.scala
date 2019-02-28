package com.example.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.data.bridge.ContractTricks
import com.example.data.bridge._
import com.example.pages.hand.PageHandInternal.PageHandNextInput
import com.example.pages.hand.ComponentInputStyleButton.InputMethod
import com.example.react.Utils._

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
object SectionResult {

  case class Props( madeOrDown: Option[MadeOrDown],
                    tricks: Option[Int],
                    contractTricks: Option[ContractTricks],
                    contractSuit: Option[ContractSuit],
                    contractDoubled: Option[ContractDoubled],
                    declarer: Option[PlayerPosition],
                    currentHonors: Option[Int],
                    currentHonorsPlayer: Option[PlayerPosition],
                    north: String,
                    south: String,
                    east: String,
                    west: String,
                    callbackMadeOrDown: ViewMadeOrDown.CallbackMade,
                    callbackTricks: ViewTricks.CallbackTricks,
                    callbackHonors: Option[ViewHonors.CallbackHonors],
                    callbackHonorsPlayer: Option[ViewHonors.CallbackHonorsPlayer],
                    nextInput: PageHandNextInput.Value ) {
    def missingRequired = {
      contractTricks match {
        case Some(ct) =>
          if (ct.tricks == 0) false
          else madeOrDown.isEmpty || tricks.isEmpty ||
               !BridgeHand.getTricksRange(madeOrDown.get, ct).contains(tricks.get)
        case None => true
      }
    }
  }

  def apply( madeOrDown: Option[MadeOrDown],
             tricks: Option[Int],
             contractTricks: Option[ContractTricks],
             contractSuit: Option[ContractSuit],
             contractDoubled: Option[ContractDoubled],
             declarer: Option[PlayerPosition],
             currentHonors: Option[Int],
             currentHonorsPlayer: Option[PlayerPosition],
             north: String,
             south: String,
             east: String,
             west: String,
             callbackMadeOrDown: ViewMadeOrDown.CallbackMade,
             callbackTricks: ViewTricks.CallbackTricks,
             callbackHonors: Option[ViewHonors.CallbackHonors],
             callbackHonorsPlayer: Option[ViewHonors.CallbackHonorsPlayer],
             nextInput: PageHandNextInput.Value ) =
    component(Props(madeOrDown,tricks,contractTricks,contractSuit,contractDoubled,declarer,
                    currentHonors, currentHonorsPlayer,
                    north,south,east,west,
                    callbackMadeOrDown,callbackTricks,
                    callbackHonors, callbackHonorsPlayer,
                    nextInput))

  def header = ScalaComponent.builder[Unit]("SectionHeader")
                 .render( _ =>
                   <.div(
                     <.span( "Result:")
                   )
                 ).build

  private val component = ScalaComponent.builder[Props]("SectionResult")
                            .render_P( props => {
                                import HandStyles._
                                val showButtons = props.contractTricks.getOrElse(PassedOut).tricks != 0 &&
                                                  props.contractSuit.isDefined &&
                                                  props.contractDoubled.isDefined &&
                                                  props.declarer.isDefined
                                import InputMethod._
                                import PageHandNextInput._
                                def show( view: PageHandNextInput ) = {
                                  ComponentInputStyleButton.inputMethod != Prompt || view <= props.nextInput
                                }
                                <.div( handStyles.sectionResult,
                                  props.callbackHonors.whenDefined ( x =>
                                    ViewHonors(props.currentHonors, props.currentHonorsPlayer,
                                               props.contractTricks,
                                               props.contractSuit,
                                               props.north, props.south, props.east, props.west,
                                               props.callbackHonors.get,
                                               props.callbackHonorsPlayer.get,
                                               props.nextInput)
                                  ),
                                  <.div(
                                    handStyles.sectionResultInner,
                                    header(),
                                    <.div(
                                      showButtons ?= ViewMadeOrDown(props.madeOrDown,props.callbackMadeOrDown, props.nextInput, show(InputResultMadeOrDown)),
                                      showButtons ?= ViewTricks(props.tricks,props.contractTricks,props.madeOrDown,props.callbackTricks, props.nextInput, show(InputResultTricks))
                                    )
                                  )
                                )
                            })
                            .build
}
