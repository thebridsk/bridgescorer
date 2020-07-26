package com.github.thebridsk.bridge.client.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.Button


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
  def apply( callback: Callback, useHandStyle: Boolean = false ) = // scalafix:ok ExplicitResultTypes; ReactComponent
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
    val Guide = Value
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

  def setInputMethod( method: InputMethod ): Unit = pInputMethod=method

  def inputMethodAsString: String = {
        pInputMethod match {
          case Original => "Original"
          case Prompt => "Prompt"
          case Guide => "Guide"
        }
      }

  val logger: Logger = Logger("bridge.ComponentInputStyleButton")

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
    def render( props: Props ) = {  // scalafix:ok ExplicitResultTypes; ReactComponent
      import HandStyles._
      Button( if (props.useHandStyle) handStyles.footerButton else baseStyles.appButton,
              "InputStyle",
              "Input Style: "+inputMethodAsString,
              ^.onClick --> nextInputStyle)
    }

    val nextInputStyle: Callback = Callback {
      pInputMethod = InputMethod.next(pInputMethod)
      logger.fine("InputMethod is "+pInputMethod)
    } >> scope.forceUpdate >> scope.props >>= { props => props.callback }
  }

  private[hand]
  val component = ScalaComponent.builder[Props]("ComponentInputStyleButton")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

