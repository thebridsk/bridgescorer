package com.example.pages.hand

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
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
object ViewContractSuit {

  type CallbackSuit = (ContractSuit)=>Callback

  case class Props( current: Option[ContractSuit], callback: CallbackSuit,
                    nextInput: PageHandNextInput.Value,
                    visible: Boolean )

  def apply( current: Option[ContractSuit], callback: CallbackSuit,
             nextInput: PageHandNextInput.Value,
             visible: Boolean ) = component(Props(current,callback,nextInput,visible))

  private val component = ScalaComponent.builder[Props]("ViewContractSuit")
                            .render_P(props => {
                                import HandStyles._
                                val (missingRequired,missingNotNext) = {
                                  val mr = props.current.isEmpty
                                  if (mr) {
                                    if (ComponentInputStyleButton.inputMethod == InputMethod.Yellow) {
                                      val ni = props.nextInput == PageHandNextInput.InputContractSuit
                                      (ni,!ni)
                                    } else {
                                      (true,false)
                                    }
                                  } else {
                                    (false,false)
                                  }
                                }

                                def isSelected( s: ContractSuit ) = props.current.isDefined && props.current.get == s
                                def getIcon( icon: Option[String], color: Option[String] ) = {
                                  icon match {
                                    case Some(s) =>
                                      color match {
                                        case Some(c) =>
                                          <.span( ^.color := c, handStyles.suitInButton, ^.dangerouslySetInnerHtml:=s+" ")
                                        case None =>
                                          <.span( s )
                                      }
                                    case _ =>
                                      EmptyVdom
                                  }
                                }
                                def getButton(text: String,
                                              contractSuit: ContractSuit,
                                              icon: Option[String],
                                              color: Option[String]) =
                                    <.button( ^.`type` := "button",
                                              ^.onClick --> props.callback(contractSuit),
                                              ^.id:="CS"+contractSuit.suit,
                                              HandStyles.highlight(
                                                  selected = isSelected(contractSuit),
                                                  required = missingRequired,
                                                  requiredNotNext = missingNotNext
                                              ),
                                              getIcon(icon,color),
                                              text
                                            )
                                <.div( handStyles.viewContractSuit, !props.visible ?= baseStyles.notVisible,
                                    getButton("No Trump", NoTrump, None, None),
                                    <.br,
                                    getButton("Spades", Spades, Some("&spades;"), Some("black")),
                                    getButton("Hearts", Hearts, Some("&hearts;"), Some("red")),
                                    <.br,
                                    getButton("Diamonds", Diamonds, Some("&diams;"), Some("red")),
                                    getButton("Clubs", Clubs, Some("&clubs;"), Some("black"))
                                    )
                            })
                            .build
}

