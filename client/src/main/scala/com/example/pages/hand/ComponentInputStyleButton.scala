package com.example.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import utils.logging.Logger
import com.example.react.Button

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ComponentInputStyleButton( ComponentInputStyleButton.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ComponentInputStyleButton {

  case class Props( callback: Callback, useHandStyle: Boolean = false )

  /**
   * @param callback called when the button is hit
   */
  def apply( callback: Callback, useHandStyle: Boolean = false ) =
    component( Props( callback, useHandStyle ) )

  abstract class MyEnumeration extends Enumeration {
    def next( v: Value ): Value = {
      val it = values.iterator
      var last: Value = null
      while (it.hasNext) {
        last = it.next()
        if (last == v) {
          if (it.hasNext) return it.next()
          else return first
        }
      }
      first
    }

    def first = values.firstKey
  }

  /**
   * The input methods.
   *
   * The first one is the default input method.
   */
  object InputMethod extends MyEnumeration {
    type InputMethod = Value
    /**
     * Same as original method, but one set of buttons is blue to prompt for next
     */
    val Yellow = Value
    /**
     * Only show buttons from previous input and current prompt
     */
    val Prompt = Value
    /**
     *  Original input method, yellow for where input is needed
     */
    val Original = Value

  }

  private var pInputMethod = InputMethod.first

  def inputMethod = pInputMethod

  import InputMethod._

  def setInputMethod( method: InputMethod ) = pInputMethod=method

  def inputMethodAsString = {
        pInputMethod match {
          case Original => "Original"
          case Prompt => "Prompt"
          case Yellow => "Yellow"
        }
      }

  val logger = Logger("bridge.ComponentInputStyleButton")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props ) = {
      import HandStyles._
      Button( if (props.useHandStyle) handStyles.footerButton else baseStyles.appButton,
              "InputStyle",
              "Input Style: "+inputMethodAsString,
              ^.onClick --> nextInputStyle())
    }

    def nextInputStyle() = Callback {
      pInputMethod = InputMethod.next(pInputMethod)
      logger.fine("InputMethod is "+pInputMethod)
    } >> scope.forceUpdate >> scope.props >>= { props => props.callback }
  }

  val component = ScalaComponent.builder[Props]("ComponentInputStyleButton")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

