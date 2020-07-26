package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui._

import japgolly.scalajs.react.vdom.Attr

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
object CheckBox {
  import CheckBoxInternal._

  case class Props( id: String, text: String, value: Boolean, toggle: Callback )

  def apply( id: String, text: String, value: Boolean, toggle: Callback ) = component(Props(id,text,value,toggle))  // scalafix:ok ExplicitResultTypes; ReactComponent

  def withKey( key: String )( id: String, text: String, value: Boolean, onclick: Callback ) = component.withKey(key)(Props(id,text,value,onclick))  // scalafix:ok ExplicitResultTypes; ReactComponent
}

object CheckBoxInternal {
  import CheckBox._

  def callback( cb: Callback ): js.Function1[scala.scalajs.js.Object,Unit] = ( event: js.Object ) => cb.runNow()

  val dataSelected: Attr[Boolean] = VdomAttr[Boolean]("data-selected")

  private[react]
  val component = ScalaComponent.builder[Props]("CheckBox")
                            .stateless
                            .noBackend
                            .render_P { props =>
                              import BaseStyles._
                              // val ic = if (props.value) icons.CheckBox()
                              //          else icons.CheckBoxOutlineBlank()

                              // val attrs = List[TagMod](
                              //   baseStyles.checkbox,
                              //   ^.id := props.id,
                              //   ic,
                              //   " ",
                              //   props.text,
                              //   ^.onClick --> props.toggle,
                              //   HtmlStyles.whiteSpace.nowrap,
                              //   dataSelected := props.value
                              // ) ::: props.attrs.toList

                              // <.div( attrs: _* ),

                              MuiFormControlLabel(
                                checked = props.value,
                                control = MuiCheckbox(
                                  checked = props.value,
                                  onChange = callback( props.toggle ),
                                  name = props.id,
                                  id = props.id
                                )(),
                                label = <.span( props.text ),
                                className = baseStyles.baseCheckbox
                              )()


                              // <.label(
                              //   HtmlStyles.whiteSpace.nowrap,
                              //   ^.id := props.id,
                              //   baseStyles.checkbox,
                              //   <.input(
                              //     ^.name:=props.id,
                              //     ^.id:="Input_"+props.id,
                              //     ^.`type`:="checkbox",
                              //     ^.checked := props.value,
                              //     ^.onClick --> props.toggle,
                              //     ^.readOnly := true,
                              //     props.attrs.toTagMod
                              //   ),
                              //   ic,
                              //   " "+props.text
                              // )
                            }
                            .build
}

