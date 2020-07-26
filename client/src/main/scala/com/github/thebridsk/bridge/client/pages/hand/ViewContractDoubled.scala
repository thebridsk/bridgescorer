package com.github.thebridsk.bridge.client.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.InputMethod
import com.github.thebridsk.bridge.client.pages.hand.PageHandInternal.PageHandNextInput
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
object ViewContractDoubled {

  type CallbackDoubled = (ContractDoubled)=>Callback

  case class Props( current: Option[ContractDoubled], callback: CallbackDoubled,
                    nextInput: PageHandNextInput.Value,
                    visible: Boolean )

  def apply( current: Option[ContractDoubled], callback: CallbackDoubled,
             nextInput: PageHandNextInput.Value,
             visible: Boolean ) = component(Props(current,callback,nextInput,visible))  // scalafix:ok ExplicitResultTypes; ReactComponent

  private val component = ScalaComponent.builder[Props]("ViewContractDoubled")
                            .render_P( props /* (props,state,backend) */ => {
                                import HandStyles._
                                val (missingRequired,missingNotNext) = {
                                  val mr = props.current.isEmpty
                                  if (mr) {
                                    if (ComponentInputStyleButton.inputMethod == InputMethod.Guide) {
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
