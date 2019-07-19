package com.github.thebridsk.bridge.client.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.client.pages.hand.PageHandInternal.PageHandNextInput
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.InputMethod
import com.github.thebridsk.bridge.clientcommon.react.Utils._

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
//                    north: String,
//                    south: String,
//                    east: String,
//                    west: String,
                    callbackMadeOrDown: ViewMadeOrDown.CallbackMade,
                    callbackTricks: ViewTricks.CallbackTricks,
                    callbackHonors: Option[ViewHonors.CallbackHonors],
                    callbackHonorsPlayer: Option[ViewHonors.CallbackHonorsPlayer],
                    nextInput: PageHandNextInput.Value,
                    contract: Contract
  ) {
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
//             north: String,
//             south: String,
//             east: String,
//             west: String,
             callbackMadeOrDown: ViewMadeOrDown.CallbackMade,
             callbackTricks: ViewTricks.CallbackTricks,
             callbackHonors: Option[ViewHonors.CallbackHonors],
             callbackHonorsPlayer: Option[ViewHonors.CallbackHonorsPlayer],
             nextInput: PageHandNextInput.Value,
             contract: Contract
           ) =
    component(Props(madeOrDown,tricks,contractTricks,contractSuit,contractDoubled,declarer,
                    currentHonors, currentHonorsPlayer,
//                    north,south,east,west,
                    callbackMadeOrDown,callbackTricks,
                    callbackHonors, callbackHonorsPlayer,
                    nextInput, contract))

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
                                               props.contract.north, props.contract.south, props.contract.east, props.contract.west,
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
                                  ),
                                  props.callbackHonors.isEmpty ?= {

                                    props.contract.scorer match {
                                      case Some(Right(score)) /* Duplicate */ =>
                                        <.div( score.contractAndResultAsString, handStyles.contractAndResult )

                                      case _ => TagMod()
                                    }
                                }
                                )
                            })
                            .build
}
