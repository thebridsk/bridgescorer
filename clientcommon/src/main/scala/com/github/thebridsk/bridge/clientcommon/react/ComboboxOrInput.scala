package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.clientcommon.react.Utils.ExtendReactEventFromInput
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.utilities.logging.Logger

object ComboboxOrInput {

  val log: Logger = Logger["bridge.ComboboxOrInput"]()

  var noCombobox: Boolean =
    false // used by TestChicago to not use ComboBox, causes exception in phantomjs

  def apply(
      callback: String => Callback,
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
      containerClassName: String = null
  ): TagMod = {

    def comboboxCB(data: js.Any): Unit = {
      val s = data.toString()
      log.fine(s"ComboboxOrInput.comboboxCB data=$s")
      callback(s).runNow()
    }

    def inputCB(data: ReactEventFromInput): Callback =
      data.inputText(text => callback(text))

    def jv[T](value: js.UndefOr[T]): Option[T] =
      if (js.isUndefined(value)) None else Some(value.get)

    if (!noCombobox) {
      Combobox(
        None,
        Some(comboboxCB _),
        jv(data),
        Option(filter),
        if (tabIndex < 0) None else Some(tabIndex),
        Option(name),
        Option(caseSensitive),
        Option(msgOpen),
        Option(msgEmptyList),
        Option(msgEmptyFilter),
        Option(busy),
        Option(defaultvalue),
        Option(id),
        Option(containerClassName).orElse(
          Some(BaseStyles.baseStyles.comboboxLightDarkClass)
        )
      )
    } else {
      <.input.text(
        ^.name := name,
        ^.onChange ==> inputCB _,
        ^.tabIndex := tabIndex,
        ^.value := defaultvalue
      )
    }
  }
}
