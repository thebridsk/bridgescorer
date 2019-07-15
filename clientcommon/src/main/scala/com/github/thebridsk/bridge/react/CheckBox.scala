package com.github.thebridsk.bridge.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.pages.BaseStyles
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
object CheckBox {
  import CheckBoxInternal._

  case class Props( id: String, text: String, value: Boolean, toggle: Callback, attrs: TagMod* )

  def apply( id: String, text: String, value: Boolean, toggle: Callback, attrs: TagMod* ) = component(Props(id,text,value,toggle,attrs:_*))

  def withKey( key: String )( id: String, text: String, value: Boolean, onclick: Callback, attrs: TagMod* ) = component.withKey(key)(Props(id,text,value,onclick,attrs:_*))
}

object CheckBoxInternal {
  import CheckBox._

  val component = ScalaComponent.builder[Props]("CheckBox")
                            .stateless
                            .noBackend
                            .render_P( props => {
                              import BaseStyles._
                              <.label(
                                HtmlStyles.whiteSpace.nowrap,
                                ^.id := "Label"+props.id,
                                baseStyles.checkbox,
                                <.input(
                                  ^.name:=props.id,
                                  ^.id:=props.id,
                                  ^.`type`:="checkbox",
                                  ^.checked := props.value,
                                  ^.onClick --> props.toggle,
                                  ^.readOnly := true,
                                  props.attrs.toTagMod
                                ),
                                " "+props.text
                              )
                            })
                            .build
}

