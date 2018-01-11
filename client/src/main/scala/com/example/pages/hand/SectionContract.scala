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
object SectionContract {

  case class Props( currentTricks: Option[ContractTricks],
                    currentSuit: Option[ContractSuit],
                    currentDoubled: Option[ContractDoubled],
                    allowPassedOut: Boolean,
                    callbackTricks: ViewContractTricks.CallbackTricks,
                    callbackSuit: ViewContractSuit.CallbackSuit,
                    callbackDoubled: ViewContractDoubled.CallbackDoubled,
                    nextInput: PageHandNextInput.Value ) {
    def missingRequired: Boolean = {
      currentTricks match {
        case Some(tricks) =>
          if (tricks.tricks == 0) !allowPassedOut
          else currentSuit.isEmpty || currentDoubled.isEmpty
        case None => true
      }
    }
  }

  def apply( currentTricks: Option[ContractTricks],
             currentSuit: Option[ContractSuit],
             currentDoubled: Option[ContractDoubled],
             allowPassedOut: Boolean,
             callbackTricks: ViewContractTricks.CallbackTricks,
             callbackSuit: ViewContractSuit.CallbackSuit,
             callbackDoubled: ViewContractDoubled.CallbackDoubled,
             nextInput: PageHandNextInput.Value) =
    component(Props(currentTricks,currentSuit,currentDoubled,allowPassedOut,callbackTricks,callbackSuit,callbackDoubled,nextInput))

  private val header = ScalaComponent.builder[Unit]("SectionHeader")
                 .render( _ =>
                   <.div(
                     <.span( "Contract:")
                   )
                 ).build

  private val component = ScalaComponent.builder[Props]("SectionContract")
                            .render_P(props => {
                                import HandStyles._
                                import InputMethod._
                                import PageHandNextInput._
                                def show( view: PageHandNextInput, ignoretricks: Boolean = false ) = {
                                  val r = ComponentInputStyleButton.inputMethod != Prompt || view <= props.nextInput
                                  val shbt = ignoretricks || !(props.currentTricks.isDefined && props.currentTricks.get.tricks == 0)
                                  r && shbt
                                }
                                <.div(
                                    handStyles.sectionContract,
                                    header(),
                                    ViewContractTricks(props.allowPassedOut,props.currentTricks,props.callbackTricks, props.nextInput,show( InputContractTricks, true)),
                                    ViewContractSuit(props.currentSuit,props.callbackSuit, props.nextInput,show( InputContractSuit)),
                                    ViewContractDoubled(props.currentDoubled,props.callbackDoubled, props.nextInput,show( InputContractDoubled))
                                )
                            })
                            .build
}
