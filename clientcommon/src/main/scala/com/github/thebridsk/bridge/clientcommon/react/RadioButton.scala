package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui._

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * AppButton( AppButton.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object RadioButton {
  import RadioButtonInternal._

  case class Props(
      id: String,
      text: String,
      value: Boolean,
      toggle: Callback,
      className: Option[String] = None
  )

  /**
    * @param id
    * @param text
    * @param value
    * @param toggle
    * @param attrs attributes that are applied to the enclosing label element.
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
}

object RadioButtonInternal {
  import RadioButton._

  def callback(cb: Callback): js.Function1[scala.scalajs.js.Object, Unit] =
    (event: js.Object) => cb.runNow()

  private[react] val component = ScalaComponent
    .builder[Props]("RadioButton")
    .stateless
    .noBackend
    .render_P(props => {
      import BaseStyles._
      // val ic = if (props.value) icons.RadioButtonChecked()
      //          else icons.RadioButtonUnchecked()

      // val attrs = List[TagMod](
      //   baseStyles.radioButton,
      //   ^.id := props.id,
      //   ic,
      //   " ",
      //   props.text,
      //   ^.onClick --> props.toggle,
      //   HtmlStyles.whiteSpace.nowrap,
      //   CheckBoxInternal.dataSelected := props.value
      // ) ::: props.attrs.toList

      // <.div( attrs: _* )

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
