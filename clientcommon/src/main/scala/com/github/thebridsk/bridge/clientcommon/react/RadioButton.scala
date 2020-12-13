package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui._

/**
  * A radio button react component.
  *
  * Usage:
  *
  * {{{
  * val callback: Callback = ...
  *
  * RadioButton(
  *   id = "radio",
  *   text = "label",
  *   value = false,
  *   toggle = callback
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the parameters.
  *
  * @author werewolf
  */
object RadioButton {
  import Internal._

  case class Props(
      id: String,
      text: String,
      value: Boolean,
      toggle: Callback,
      className: Option[String] = None
  )

  /**
    * Instantiate the react component.
    *
    * @param id - the id attribute on the radio button input field.
    * @param text - the label for the radio button.
    * @param value - true indicates the button is selected.
    * @param toggle - a callback that is called when the radio button is selected.
    * @param className - a class to add to the root label element.
    *
    * @return the unmounted react component
    */
  def apply(
      id: String,
      text: String,
      value: Boolean,
      toggle: Callback,
      className: Option[String] = None
  ) =
    component(
      Props(id, text, value, toggle, className)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * Instantiate the react component with a key.
    *
    * @param key - key to add to the component.
    * @param id - the id attribute on the radio button input field.
    * @param text - the label for the radio button.
    * @param value - true indicates the button is selected.
    * @param toggle - a callback that is called when the radio button is selected.
    * @param className - a class to add to the root label element.
    *
    * @return the unmounted react component
    */
  def withKey(key: String)(
      id: String,
      text: String,
      value: Boolean,
      onclick: Callback,
      className: Option[String] = None
  ) =
    component.withKey(key)(
      Props(id, text, value, onclick, className)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    def callback(cb: Callback): js.Function1[scala.scalajs.js.Object, Unit] =
      (event: js.Object) => cb.runNow()

    private[react] val component = ScalaComponent
      .builder[Props]("RadioButton")
      .stateless
      .noBackend
      .render_P(props => {
        import BaseStyles._

        MuiFormControlLabel(
          checked = props.value,
          control = MuiRadio(
            checked = props.value,
            onChange = callback(props.toggle),
            name = props.id,
            id = props.id
          )(),
          label = <.span(props.text),
          className =
            s"${baseStyles.baseRadioButton}${props.className.map(c => s" $c").getOrElse("")}"
        )()

        // <.label(
        //   baseStyles.radioButton,
        //   ^.id:=props.id,
        //   <.input(
        //     ^.`type`:="radio",
        //     ^.name:=props.id,
        //     ^.id:="Input_"+props.id,
        //     ^.value:=props.id,
        //     ^.checked:=props.value,
        //     ^.onChange --> props.toggle,
        //   ),
        //   ic,
        //   " "+props.text,
        //   props.attrs.toTagMod
        // )
      })
      .build
  }

}
