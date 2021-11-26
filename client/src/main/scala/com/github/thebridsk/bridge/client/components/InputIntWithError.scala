package com.github.thebridsk.bridge.client.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.Utils._

/**
  * A component that displays an input field, with a label and optional error messages.
  *
  * To use, just code the following:
  *
  * {{{
  * case class State(
  *   nboards: Int = 0
  * )
  *
  * def onChange(n: Int) = scope.modState(_.copy(nboards = n))
  *
  * def validate(n: Int) = {
  *   if (n <= 0) Left("Enter a positive number")
  *   else Right(n)
  * }
  *
  * InputIntWithError(
  *   label = "Number of boards",
  *   name = "nboards",
  *   value = state.nboards,
  *   onChange = onChange,
  *   validate = validate
  * )
  * }}}
  *
  * @see See [[apply]] for a description of all arguments.
  *
  * @author werewolf
  */
object InputIntWithError {
  import Internal._

  case class Props(
    label: String,
    name: String,
    value: Int,
    onChange: Int => Callback,
    validate: Int => Either[String,Int],
    notNumberError: String
  )

  /**
    * Instantiate the component
    *
    * @param label - the label for the input field
    * @param name - the `name` attribute on the `input` element
    * @param value - the `value` attribute on the `input` element
    * @param onChange - callback when the value is changed
    * @param validate - an input value validation routine.
    *                   Returns Left(error) or Right(int).
    *                   Default: all values are good.
    * @param notNumberError - error message to display if not an int.
    *                         Default: "Enter a valid integer"
    *
    * @return the unmounted react component
    *
    * @see [[InputIntWithError]] for usage.
    */
  def apply(
    label: String,
    name: String,
    value: Int,
    onChange: Int => Callback,
    validate: Int => Either[String,Int] = v => Right(v),
    notNumberError: String = "Enter a valid integer"
  ) =
    component(Props(label, name, value, onChange, validate, notNumberError)) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State(
      error: Option[String] = None
    ) {

      def setMsg(error: String) = copy(error = Some(error))
      def clrMsg() = copy(error = None)
    }

    class Backend(scope: BackendScope[Props, State]) {
      def setValue(v: ReactEventFromInput) = v.inputText { text =>
        scope.modState { (state,props) =>
          try {
            val n = text.trim.toInt
            props.validate(n) match {
              case Left(error) =>
                state.setMsg(error)
              case Right(value) =>
                props.onChange(value).runNow()
                state.clrMsg()
            }
          } catch {
            case x: NumberFormatException =>
              state.setMsg(props.notNumberError)
          }
        }

      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.label(
          props.label,
          <.input(
            ^.`type` := "number",
            ^.name := props.name,
            ^.onChange ==> setValue _,
            ^.value := props.value.toString
          ),
          state.error.whenDefined { msg =>
            <.span(s" $msg")
          }
        )
      }
    }

    val component = ScalaComponent
      .builder[Props]("InputIntWithError")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
