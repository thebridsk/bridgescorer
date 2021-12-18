package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait TextFieldProps extends AdditionalProps with StandardProps {
}

object TextFieldProps extends PropsFactory[TextFieldProps] {

  /**
    * @param p the object that will become the properties object
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: TextFieldProps](
      props: js.UndefOr[P] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    p
  }

}

object MuiTextField extends ComponentFactory[TextFieldProps] {
  @js.native @JSImport(
    "@mui/material/TextField",
    JSImport.Default
  ) private object MTextField extends js.Any

  protected val f =
    JsComponent[TextFieldProps, Children.Varargs, Null](
      MTextField
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: TextFieldProps = TextFieldProps(
      additionalProps = additionalProps,
    )
    val x = f(p) _
    x(children)
  }
}
