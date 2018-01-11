package com.example.react

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import org.scalajs.dom.raw.HTMLElement
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.TagMod
import com.example.react.Utils.ExtendReactEventFromInput


object ComboboxOrInput {

  var noCombobox: Boolean = false    // used by TestChicago to not use ComboBox, causes exception in phantomjs

  def apply(callback: String=>Callback,
            defaultvalue: String = null,
            data: js.UndefOr[js.Array[String]] = js.undefined,
            filter: String = null,
            tabIndex: Int = -1,
            name: String = null,
            caseSensitive: Boolean = false,
            msgOpen: String = null,
            msgEmptyList: String = null,
            msgEmptyFilter: String = null,
            busy: Boolean = false
           ): TagMod = {

    def comboboxCB( data: js.Any ): Unit = callback(data.toString()).runNow()

    def inputCB( data: ReactEventFromInput): Callback = data.inputText( text => callback(text) )

    def v[T](value: T): Option[T] = if (value==null) None else Some(value)

    def jv[T]( value: js.UndefOr[T] ): Option[T] = if (js.isUndefined(value)) None else Some(value.get)

    if (!noCombobox) {
//      Combobox(v(defaultvalue),
//               Some(comboboxCB _),
//               jv(data),
//               v(filter),
//               v(tabIndex),
//               v(name),
//               v(caseSensitive),
//               v(msgOpen),
//               v(msgEmptyList),
//               v(msgEmptyFilter),
//               v(busy))

      // this sets the "value" field.  This causes the following warning from react:
      //
      //   Warning: You are manually calling a React.PropTypes validation function for the `value` prop on `ComboBox`.
      //   This is deprecated and will not work in production with the next major version.
      //   You may be seeing this warning due to a third-party PropTypes library.
      //   See https://fb.me/react-warning-dont-call-proptypes for details.
      Combobox(None,
               Some(comboboxCB _),
               jv(data),
               v(filter),
               v(tabIndex),
               v(name),
               v(caseSensitive),
               v(msgOpen),
               v(msgEmptyList),
               v(msgEmptyFilter),
               v(busy),
               v(defaultvalue))
    }
    else {
      <.input.text(
                    ^.name:=name,
                    ^.onChange ==> inputCB _,
                    ^.tabIndex:=tabIndex,
                    ^.value := defaultvalue)
    }
  }
}
