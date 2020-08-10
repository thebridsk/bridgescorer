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
object ViewContractSuit {

  type CallbackSuit = (ContractSuit) => Callback

  case class Props(
      current: Option[ContractSuit],
      contractTricks: Option[ContractTricks],
      callback: CallbackSuit,
      nextInput: PageHandNextInput.Value,
      visible: Boolean
  )

  def apply(
      current: Option[ContractSuit],
      contractTricks: Option[ContractTricks],
      callback: CallbackSuit,
      nextInput: PageHandNextInput.Value,
      visible: Boolean
  ) =
    component(
      Props(current, contractTricks, callback, nextInput, visible)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  private val component = ScalaComponent
    .builder[Props]("ViewContractSuit")
    .render_P(props => {
      import HandStyles._
      val (missingRequired, missingNotNext) = {
        val mr = props.current.isEmpty
        if (mr) {
          if (ComponentInputStyleButton.inputMethod == InputMethod.Guide) {
            val ni = props.nextInput == PageHandNextInput.InputContractSuit
            (ni, !ni)
          } else {
            (true, false)
          }
        } else {
          (false, false)
        }
      }

      def isSelected(s: ContractSuit) =
        props.current.isDefined && props.current.get == s
      def getIcon(icon: Option[String], color: Option[String]) = {
        icon match {
          case Some(s) =>
            color match {
              case Some(c) =>
                <.span(
                  ^.color := c,
                  handStyles.suitInButton,
                  ^.dangerouslySetInnerHtml := s + " "
                )
              case None =>
                <.span(s)
            }
          case _ =>
            EmptyVdom
        }
      }
      def getButton(
          text: String,
          texts: String,
          contractSuit: ContractSuit,
          icon: Option[String],
          color: Option[String]
      ) =
        <.button(
          ^.`type` := "button",
          ^.onClick --> props.callback(contractSuit),
          ^.id := "CS" + contractSuit.suit,
          HandStyles.highlight(
            selected = isSelected(contractSuit),
            required = missingRequired,
            requiredNotNext = missingNotNext
          ),
          getIcon(icon, color),
          <.span(
            if (props.contractTricks.map(ct => ct.tricks).getOrElse(0) == 1)
              text
            else texts
          )
        )
      <.div(
        handStyles.viewContractSuit,
        !props.visible ?= baseStyles.notVisible,
        getButton("No Trump", "No Trump", NoTrump, None, None),
        <.br,
        getButton("Spade", "Spades", Spades, Some("&spades;"), Some("black")),
        getButton("Heart", "Hearts", Hearts, Some("&hearts;"), Some("red")),
        <.br,
        getButton(
          "Diamond",
          "Diamonds",
          Diamonds,
          Some("&diams;"),
          Some("red")
        ),
        getButton("Club", "Clubs", Clubs, Some("&clubs;"), Some("black"))
      )
    })
    .build
}
