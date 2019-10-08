package com.github.thebridsk.bridge.client.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
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
object ViewContractTricks {

  type CallbackTricks = (ContractTricks)=>Callback

  case class Props( allowPassedOut: Boolean,
                    current: Option[ContractTricks],
                    callback: CallbackTricks,
                    nextInput: PageHandNextInput.Value,
                    visible: Boolean )

  def apply( allowPassedOut: Boolean,
             current: Option[ContractTricks],
             callback: CallbackTricks,
             nextInput: PageHandNextInput.Value,
             visible: Boolean ) =
    component(Props(allowPassedOut,current,callback,nextInput,visible))

  private val component = ScalaComponent.builder[Props]("ViewContractTricks")
                            .render_P( props => {
                              import HandStyles._
                              val (missingRequired,missingNotNext) = {
                                val mr = props.current.isEmpty || (props.current.get.tricks == 0 && !props.allowPassedOut)
                                if (mr) {
                                  if (ComponentInputStyleButton.inputMethod == InputMethod.Guide) {
                                    val ni = props.nextInput == PageHandNextInput.InputContractTricks
                                    (ni,!ni)
                                  } else {
                                    (true,false)
                                  }
                                } else {
                                  (false,false)
                                }
                              }

                              def isSelected( i: Int ) = props.current.isDefined && props.current.get.tricks == i
                              def getButton(i: Int) = {
                                val text = if (i>0) i.toString; else "Passed"
                                <.button( i==0?=handStyles.contractTricksButton0,
                                          ^.`type` := "button",
                                          ^.onClick --> props.callback(i),
                                          HandStyles.highlight(
                                              selected = isSelected(i),
                                              required = missingRequired,
                                              requiredNotNext = missingNotNext
                                          ),
                                          ^.id:="CT"+text,
                                          text
                                        )
                              }
                              <.div( handStyles.viewContractTricks, !props.visible ?= baseStyles.notVisible,
                                (1 to 7).map( i =>
                                  if (i==4) {
                                    List[TagMod](
                                        <.br,
                                        getButton(i)
                                        )
                                  } else {
                                    List(getButton(i))
                                  }

                                ).flatten.toTagMod,
                                props.allowPassedOut ?= <.br,
                                props.allowPassedOut ?= getButton(0)
                              )
                            })
                            .build
}

