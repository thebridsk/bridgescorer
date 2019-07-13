package com.github.thebridsk.bridge.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.pages.hand.PageHandInternal.PageHandNextInput
import com.github.thebridsk.bridge.pages.hand.ComponentInputStyleButton.InputMethod
import com.github.thebridsk.bridge.react.Utils._

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
object ViewTricks {

  type CallbackTricks = (Int)=>Callback

  case class Props( current: Option[Int],
                    contractTricks: Option[ContractTricks],
                    madeOrDown: Option[MadeOrDown],
                    callback: CallbackTricks,
                    nextInput: PageHandNextInput.Value,
                    visible: Boolean ) {
    def missingRequired = {
      contractTricks match {
        case Some(ct) =>
          if (ct.tricks == 0) false
          else madeOrDown.isEmpty || current.isEmpty ||
               !BridgeHand.getTricksRange(madeOrDown.get, ct).contains(current.get)
        case None => true
      }
    }

  }

  def apply( current: Option[Int],
             contractTricks: Option[ContractTricks],
             madeOrDown: Option[MadeOrDown],
             callback: CallbackTricks,
             nextInput: PageHandNextInput.Value,
             visible: Boolean ) =
    component(Props(current,contractTricks,madeOrDown,callback,nextInput,visible))

  private val component = ScalaComponent.builder[Props]("ViewTricks")
                            .render_P( props => {
                                import HandStyles._
                                val (missingRequired,missingNotNext) = {
                                  val mr = props.missingRequired
                                  if (mr) {
                                    if (ComponentInputStyleButton.inputMethod == InputMethod.Yellow) {
                                      val ni = props.nextInput == PageHandNextInput.InputResultTricks
                                      (ni,!ni)
                                    } else {
                                      (true,false)
                                    }
                                  } else {
                                    (false,false)
                                  }
                                }
                                def isSelected( d: Int ) = props.current.isDefined && props.current.get == d
                                def getButton(tricks: Int) =
                                    <.button( ^.`type` := "button",
                                              ^.onClick --> props.callback(tricks),
                                              HandStyles.highlight(
                                                  selected = isSelected(tricks),
                                                  required = missingRequired,
                                                  requiredNotNext = missingNotNext
                                              ),
                                              ^.id:="T"+tricks,
                                              tricks.toString
                                            )

                                val ct = props.contractTricks.getOrElse(PassedOut)
                                if (ct.tricks == 0) {
                                  <.span
                                } else {
                                  val range = props.madeOrDown match {
                                    case Some(mord) => BridgeHand.getTricksRange(mord, ct)
                                    case None => 0 until 0
                                  }
                                  val start = range.start
                                  val mid = start +(range.length+1)/2
                                  <.div(
                                    handStyles.viewTricks,
                                    <.div(
                                      !props.visible ?= handStyles.notVisible,
                                      range.map( i =>
                                        if (i==mid) {
                                          List[TagMod]( <.br, getButton(i) )
                                        } else {
                                          List( getButton(i) )
                                        }
                                      ).flatten.toTagMod
                                    )
                                  )
                                }

                            })
                            .build
}
