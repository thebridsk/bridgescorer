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
object ViewContractDoubled {

  type CallbackDoubled = (ContractDoubled)=>Callback

  case class Props( current: Option[ContractDoubled], callback: CallbackDoubled,
                    nextInput: PageHandNextInput.Value,
                    visible: Boolean )

  def apply( current: Option[ContractDoubled], callback: CallbackDoubled,
             nextInput: PageHandNextInput.Value,
             visible: Boolean ) = component(Props(current,callback,nextInput,visible))

  private val component = ScalaComponent.builder[Props]("ViewContractDoubled")
                            .render_P( props /* (props,state,backend) */ => {
                                import HandStyles._
                                val (missingRequired,missingNotNext) = {
                                  val mr = props.current.isEmpty
                                  if (mr) {
                                    if (ComponentInputStyleButton.inputMethod == InputMethod.Yellow) {
                                      val ni = props.nextInput == PageHandNextInput.InputContractDoubled
                                      (ni,!ni)
                                    } else {
                                      (true,false)
                                    }
                                  } else {
                                    (false,false)
                                  }
                                }

                                def isSelected( d: ContractDoubled ) = props.current.isDefined && props.current.get == d
                                def getButton(text: String, contractDoubled: ContractDoubled) =
                                    <.button( ^.`type` := "button",
                                              ^.id:="Doubled"+contractDoubled.doubled,
                                              ^.onClick --> props.callback(contractDoubled),
                                              HandStyles.highlight(
                                                  selected = isSelected(contractDoubled),
                                                  required = missingRequired,
                                                  requiredNotNext = missingNotNext
                                              ),
                                              text
                                            )
                                <.div( handStyles.viewContractDoubled, !props.visible ?= handStyles.notVisible,
                                    getButton("Not doubled", NotDoubled),
                                    <.br,
                                    getButton("Doubled", Doubled),
                                    <.br,
                                    getButton("Redoubled", Redoubled)
                                    )
                            })
                            .build
}
