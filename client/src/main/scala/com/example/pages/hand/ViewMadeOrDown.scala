package com.example.pages.hand

import scala.scalajs.js
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
object ViewMadeOrDown {

  type CallbackMade = (MadeOrDown)=>Callback

  case class Props( current: Option[MadeOrDown], callback: CallbackMade,
                    nextInput: PageHandNextInput.Value,
                    visible: Boolean )

  def apply( current: Option[MadeOrDown], callback: CallbackMade,
             nextInput: PageHandNextInput.Value,
             visible: Boolean ) = component(Props(current,callback,nextInput,visible))

  private val component = ScalaComponent.builder[Props]("ViewMadeOrDown")
                            .render_P( props => {
                                import HandStyles._
                                val (missingRequired,missingNotNext) = {
                                  val mr = props.current.isEmpty
                                  if (mr) {
                                    if (ComponentInputStyleButton.inputMethod == InputMethod.Yellow) {
                                      val ni = props.nextInput == PageHandNextInput.InputResultMadeOrDown
                                      (ni,!ni)
                                    } else {
                                      (true,false)
                                    }
                                  } else {
                                    (false,false)
                                  }
                                }

                                def isSelected( d: MadeOrDown ) = props.current.isDefined && props.current.get == d
                                def getButton(text: String, madeOrDown: MadeOrDown) =
                                    <.button( ^.`type` := "button",
                                              ^.onClick --> props.callback(madeOrDown),
                                              HandStyles.highlight(
                                                  selected = isSelected(madeOrDown),
                                                  required = missingRequired,
                                                  requiredNotNext = missingNotNext
                                              ),
                                              ^.id:=madeOrDown.forScore,
                                              text
                                            )
                                <.div( handStyles.viewMadeOrDown, !props.visible ?= handStyles.notVisible,
                                    getButton("Made", Made),
                                    <.br(),
                                    getButton("Down", Down)
                                    )

                            })
                            .build
}

