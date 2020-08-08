package com.github.thebridsk.bridge.client.pages.hand

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
object SectionContract {

  case class Props(
      currentTricks: Option[ContractTricks],
      currentSuit: Option[ContractSuit],
      currentDoubled: Option[ContractDoubled],
      allowPassedOut: Boolean,
      callbackTricks: ViewContractTricks.CallbackTricks,
      callbackSuit: ViewContractSuit.CallbackSuit,
      callbackDoubled: ViewContractDoubled.CallbackDoubled,
      nextInput: PageHandNextInput.Value,
      showContractHeader: Boolean
  ) {
    def missingRequired: Boolean = {
      currentTricks match {
        case Some(tricks) =>
          if (tricks.tricks == 0) !allowPassedOut
          else currentSuit.isEmpty || currentDoubled.isEmpty
        case None => true
      }
    }
  }

  def apply(
      currentTricks: Option[ContractTricks],
      currentSuit: Option[ContractSuit],
      currentDoubled: Option[ContractDoubled],
      allowPassedOut: Boolean,
      callbackTricks: ViewContractTricks.CallbackTricks,
      callbackSuit: ViewContractSuit.CallbackSuit,
      callbackDoubled: ViewContractDoubled.CallbackDoubled,
      nextInput: PageHandNextInput.Value,
      showContractHeader: Boolean
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props(
        currentTricks,
        currentSuit,
        currentDoubled,
        allowPassedOut,
        callbackTricks,
        callbackSuit,
        callbackDoubled,
        nextInput,
        showContractHeader
      )
    )

  private val header = ScalaComponent
    .builder[Props]("SectionHeader")
    .render_P(props =>
      <.div(
        props.showContractHeader ?= <.span("Contract:")
      )
    )
    .build

  private val component = ScalaComponent
    .builder[Props]("SectionContract")
    .render_P(props => {
      import HandStyles._
      import InputMethod._
      import PageHandNextInput._
      def show(view: PageHandNextInput, ignoretricks: Boolean = false) = {
        val r =
          ComponentInputStyleButton.inputMethod != Prompt || view <= props.nextInput
        val shbt =
          ignoretricks || !(props.currentTricks.isDefined && props.currentTricks.get.tricks == 0)
        r && shbt
      }
      <.div(
        handStyles.sectionContract,
        header(props),
        ViewContractTricks(
          props.allowPassedOut,
          props.currentTricks,
          props.callbackTricks,
          props.nextInput,
          show(InputContractTricks, true)
        ),
        ViewContractSuit(
          props.currentSuit,
          props.currentTricks,
          props.callbackSuit,
          props.nextInput,
          show(InputContractSuit)
        ),
        ViewContractDoubled(
          props.currentDoubled,
          props.callbackDoubled,
          props.nextInput,
          show(InputContractDoubled)
        )
      )
    })
    .build
}
