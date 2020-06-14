package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui.icons.MuiIcons
import japgolly.scalajs.react.vdom.HtmlStyles

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

  case class Props( id: String, text: String, value: Boolean, toggle: Callback, attrs: TagMod* )

  /**
   * @param id
   * @param text
   * @param value
   * @param toggle
   * @param attrs attributes that are applied to the enclosing label element.
   */
  def apply( id: String, text: String, value: Boolean, toggle: Callback, attrs: TagMod* ) = component(Props(id,text,value,toggle,attrs:_*))

  def withKey( key: String )( id: String, text: String, value: Boolean, onclick: Callback, attrs: TagMod* ) = component.withKey(key)(Props(id,text,value,onclick,attrs:_*))
}

object RadioButtonInternal {
  import RadioButton._

  val component = ScalaComponent.builder[Props]("RadioButton")
                            .stateless
                            .noBackend
                            .render_P( props => {
                              import BaseStyles._
                              val ic = if (props.value) MuiIcons.RadioButtonChecked()
                                       else MuiIcons.RadioButtonUnchecked()

                              val attrs = List[TagMod](
                                baseStyles.radioButton,
                                ^.id := props.id,
                                ic,
                                " ",
                                props.text,
                                ^.onClick --> props.toggle,
                                HtmlStyles.whiteSpace.nowrap,
                                CheckBoxInternal.dataSelected := props.value
                              ) ::: props.attrs.toList

                              <.div( attrs: _* )

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

