package com.github.thebridsk.bridge.react

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.react.Utils.ExtendReactEventFromInput


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
            busy: Boolean = false,
            id: String = null,
           ): TagMod = {

    def comboboxCB( data: js.Any ): Unit = callback(data.toString()).runNow()

    def inputCB( data: ReactEventFromInput): Callback = data.inputText( text => callback(text) )

    def v[T](value: T): Option[T] = if (value==null) None else Some(value)

    def jv[T]( value: js.UndefOr[T] ): Option[T] = if (js.isUndefined(value)) None else Some(value.get)

    if (!noCombobox) {
      Combobox(None,
               Some(comboboxCB _),
               jv(data),
               v(filter),
               if (tabIndex== -1) None else v(tabIndex),
               v(name),
               v(caseSensitive),
               v(msgOpen),
               v(msgEmptyList),
               v(msgEmptyFilter),
               v(busy),
               v(defaultvalue),
               v(id))
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
