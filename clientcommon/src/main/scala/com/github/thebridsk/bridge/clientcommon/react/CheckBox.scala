package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui._

import japgolly.scalajs.react.vdom.Attr

/**
  * A checkbox component.
  *
  * To use, just code the following:
  *
  * {{{
  * val cb = Callback {}
  * val value: Boolean = ...
  *
  * CheckBox("id", "label", value, cb)
  * }}}
  *
  * @see See [[apply]] method for a description of the arguments.
  *
  * @author werewolf
  */
object CheckBox {
  import Internal._

  case class Props(id: String, text: String, value: Boolean, toggle: Callback)

  /**
    * Instantiate the component.
    *
    * @param id     - the id attribute on the input field
    * @param text   - the label for the checkbox
    * @param value  - the state of the checkbox
    * @param toggle - the callback when the input field is clicked
    * @return the react component.
    */
  def apply(id: String, text: String, value: Boolean, toggle: Callback) =
    component(
      Props(id, text, value, toggle)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * Instantiate the component with a key.
    *
    * @param key    - the key to put on the component
    * @param id     - the id attribute on the input field
    * @param text   - the label for the checkbox
    * @param value  - the state of the checkbox
    * @param toggle - the callback when the input field is clicked
    * @return the react component.
    */
  def withKey(
      key: String
  )(id: String, text: String, value: Boolean, onclick: Callback) =
    component.withKey(key)(
      Props(id, text, value, onclick)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  object Internal {

    def callback(cb: Callback): js.Function1[scala.scalajs.js.Object, Unit] =
      (event: js.Object) => cb.runNow()

    val dataSelected: Attr[Boolean] = VdomAttr[Boolean]("data-selected")

    private[react] val component = ScalaComponent
      .builder[Props]("CheckBox")
      .stateless
      .noBackend
      .render_P { props =>
        import BaseStyles._

        MuiFormControlLabel(
          checked = props.value,
          control = MuiCheckbox(
            checked = props.value,
            onChange = callback(props.toggle),
            name = props.id,
            id = props.id
          )(),
          label = <.span(props.text),
          className = baseStyles.baseCheckbox
        )()
      }
      .build
  }

}
