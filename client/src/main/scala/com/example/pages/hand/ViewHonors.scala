package com.example.pages.hand

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.data.bridge.ContractTricks
import com.example.data.bridge._
import com.example.pages.hand.ComponentInputStyleButton.InputMethod
import com.example.pages.hand.PageHandInternal.PageHandNextInput
import com.example.react.Utils._
import com.example.react.RadioButton

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
object ViewHonors {

  type CallbackHonors = (Int)=>Callback
  type CallbackHonorsPlayer = (Option[PlayerPosition])=>Callback

  case class Props( currentHonors: Option[Int], currentPlayer: Option[PlayerPosition],
                    currentContractTricks: Option[ContractTricks],
                    currentSuit: Option[ContractSuit],
                    north: String,
                    south: String,
                    east: String,
                    west: String,
                    callback: CallbackHonors,
                    callbackPlayer: CallbackHonorsPlayer,
                    nextInput: PageHandNextInput.Value)

  def apply( currentHonors: Option[Int], currentPlayer: Option[PlayerPosition],
             currentContractTricks: Option[ContractTricks],
             currentSuit: Option[ContractSuit],
             north: String,
             south: String,
             east: String,
             west: String,
             callback: CallbackHonors,
             callbackPlayer: CallbackHonorsPlayer,
             nextInput: PageHandNextInput.Value ) = component(Props(currentHonors,currentPlayer,currentContractTricks,currentSuit,
                                                                    north,south,east,west,
                                                                    callback,callbackPlayer,nextInput))

  private val component = ScalaComponent.builder[Props]("ViewHonors")
                            .render_P( props /* (props,state,backend) */ => {
                                import HandStyles._
                                val (hMissingRequired,hMissingNotNext) = {
                                  val mr = props.currentHonors.isEmpty
                                  if (mr) {
                                    if (ComponentInputStyleButton.inputMethod == InputMethod.Yellow) {
                                      val ni = props.nextInput == PageHandNextInput.InputHonors
                                      (ni,!ni)
                                    } else {
                                      (true,false)
                                    }
                                  } else {
                                    (false,false)
                                  }
                                }
                                val (hpMissingRequired,hpMissingNotNext) = {
                                  val mr = props.currentPlayer.isEmpty
                                  if (mr) {
                                    if (ComponentInputStyleButton.inputMethod == InputMethod.Yellow) {
                                      val ni = props.nextInput == PageHandNextInput.InputHonorsPlayer
                                      (ni,!ni)
                                    } else {
                                      (true,false)
                                    }
                                  } else {
                                    (false,false)
                                  }
                                }

                                def show() = {
                                  val r = ComponentInputStyleButton.inputMethod != InputMethod.Prompt || PageHandNextInput.InputHonors <= props.nextInput
                                  val shbt = props.currentContractTricks.isDefined && props.currentContractTricks.get.tricks > 0 &&
                                             props.currentSuit.isDefined
                                  r && shbt
                                }

                                def showP() = {
                                  val r = ComponentInputStyleButton.inputMethod != InputMethod.Prompt || PageHandNextInput.InputHonorsPlayer <= props.nextInput
                                  val shbt = props.currentContractTricks.isDefined && props.currentContractTricks.get.tricks > 0 &&
                                             props.currentSuit.isDefined
                                  r && shbt
                                }

                                def isSelected( d: Int ) = props.currentHonors.isDefined && props.currentHonors.get == d
                                def isPlayerSelected( d: PlayerPosition ) = props.currentPlayer.isDefined && props.currentPlayer.get == d

                                def getButton( honors: Int ) =
                                    <.button( ^.`type` := "button",
                                              ^.id:="Honors"+honors,
                                              ^.onClick --> props.callback(honors),
                                              HandStyles.highlight(
                                                  selected = isSelected(honors),
                                                  required = hMissingRequired,
                                                  requiredNotNext = hMissingNotNext
                                              ),
                                              honors.toString()
                                            )
                                def getPlayerButton(honors: PlayerPosition, name: String) =
                                    RadioButton( "HonPlay"+honors.pos, name,
                                                 isPlayerSelected(honors),
                                                 props.callbackPlayer(Some(honors)),
                                                 HandStyles.highlight(
                                                     selected = isPlayerSelected(honors),
                                                     required = hpMissingRequired,
                                                     requiredNotNext = hpMissingNotNext
                                                 )
                                               )

                                val notrump = props.currentSuit.isDefined && props.currentSuit.get == NoTrump
                                <.div( handStyles.viewHonors,
                                  <.div( "Honors:" ),
                                  <.div(
                                      !show() ?= handStyles.notVisible,
                                      getButton(0),
                                      !notrump ?= getButton(100),
                                      getButton(150)
                                  ),
                                  <.div(
                                      !(props.currentHonors.getOrElse(0)>0 && showP()) ?= handStyles.notVisible,
                                      getPlayerButton( North, props.north ),
                                      getPlayerButton( South, props.south ),
                                      getPlayerButton( East, props.east ),
                                      getPlayerButton( West, props.west )
                                  )
                                )
                            })
                            .build
}
