package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles

@js.native
trait ComboboxComponentMessagesProperty extends js.Object {
  val open: js.UndefOr[String] = js.native
  val emptyList: js.UndefOr[String] = js.native
  val emptyFilter: js.UndefOr[String] = js.native
}

object ComboboxComponentMessagesProperty {
  def apply(
      msgOpen: js.UndefOr[String] = js.undefined,
      msgEmptyList: js.UndefOr[String] = js.undefined,
      msgEmptyFilter: js.UndefOr[String] = js.undefined
  ): ComboboxComponentMessagesProperty = {
    val p = js.Dynamic.literal()

    msgOpen.foreach(p.updateDynamic("open")(_))
    msgEmptyList.foreach(p.updateDynamic("emptyList")(_))
    msgEmptyFilter.foreach(p.updateDynamic("emptyFilter")(_))

//    Combobox.logger.info("ComboboxComponentMessagesProperty: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    p.asInstanceOf[ComboboxComponentMessagesProperty]
  }
}

@js.native
trait ComboboxComponentProperty extends js.Object {
  val propOne: js.UndefOr[String] = js.native
  val defaultvalue: js.UndefOr[String] = js.native
  val value: js.UndefOr[String] = js.native
  val onChange: js.UndefOr[js.Any => Unit] = js.native
  val data: js.UndefOr[js.Array[String]] = js.native
  val filter: js.UndefOr[String] = js.native
  val tabIndex: js.UndefOr[String] = js.native
  val name: js.UndefOr[String] = js.native
  val caseSensitive: js.UndefOr[Boolean] = js.native
  val messages: js.UndefOr[ComboboxComponentMessagesProperty] = js.native
  val autoFocus: js.UndefOr[Boolean] = js.native
  val busy: js.UndefOr[Boolean] = js.native
  val delay: js.UndefOr[Double] = js.native
  val dropUp: js.UndefOr[Boolean] = js.native
}

object ComboboxComponentProperty {
  def apply(
      defaultValue: js.UndefOr[String] = js.undefined,
      onChange: js.UndefOr[js.Any => Unit] = js.undefined,
      data: js.UndefOr[js.Array[String]] = js.undefined,
      filter: js.UndefOr[String] = js.undefined,
      tabIndex:  js.UndefOr[Int] = js.undefined,
      name: js.UndefOr[String] = js.undefined,
      caseSensitive: js.UndefOr[Boolean] = js.undefined,
      messages: js.UndefOr[ComboboxComponentMessagesProperty] = js.undefined,
      busy: js.UndefOr[Boolean] = js.undefined,
      value: js.UndefOr[String] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      containerClassName: js.UndefOr[String] = js.undefined
  ): ComboboxComponentProperty = {
    val p = js.Dynamic.literal()

    defaultValue.foreach(p.updateDynamic("defaultValue")(_))
    onChange.foreach(p.updateDynamic("onChange")(_))
    data.foreach(p.updateDynamic("data")(_))
    filter.foreach(p.updateDynamic("filter")(_))
    tabIndex.foreach(ti => p.updateDynamic("tabIndex")(ti.toString))
    name.foreach(p.updateDynamic("name")(_))
    caseSensitive.foreach(p.updateDynamic("caseSensitive")(_))
    messages.foreach(p.updateDynamic("messages")(_))
    busy.foreach(p.updateDynamic("busy")(_))
    value.foreach(p.updateDynamic("value")(_))
    id.foreach(p.updateDynamic("id")(_))
    containerClassName.foreach(p.updateDynamic("containerClassName")(_))

    p.asInstanceOf[ComboboxComponentProperty]
  }
}
@js.native
trait Combobox extends js.Object


/**
 * A Combobox component.
 *
 * This component has an input field and a dropdown list of suggestions.
 *
 * This wraps the Combobox component from react-widgets,
 * https://jquense.github.io/react-widgets/api/Combobox/
 *
 * Usage:
 *
 * {{{
 * def onChange( v: String ): Unit = {...}
 * val data = js.Array("Henry", "Mahitha")
 *
 * Combobox(
 *   onChange = onChange _,
 *   data = data,
 *   value = value,
 *   filter = "startsWith",
 *   name = "enterName"
 * )
 * }}}
 *
 * or
 *
 * {{{
 * def onChange( v: String ): Callback = Callback {...}
 * val data = js.Array("Ben", "Paul")
 *
 * Combobox.create(
 *   onChange = onChange _,
 *   data = data,
 *   value = value,
 *   filter = "startsWith",
 *   name = "enterName"
 * )
 * }}}
 *
 * @see See the [[apply]] or [[create]] method for a description of the parameters.
 *
 */
object Combobox {
  import Internal._

  /**
    * Instantiate the component.
    *
    * @param defaultValue - default value if the component is uncontrolled.
    * @param onChange - A callback fired when the current value changes.
    * @param data - An array of possible values for the Combobox.
    *               Tip: When data is an array of objects consider specifying textField and valueField as well
    * @param filter - Enable and customize filtering behavior for the Combobox. Specify one of the built-in
    *                 methods ("startsWith" "endsWith" "contains") or provide a function that returns true or false
    *                 for each passed in item (analogous to the array.filter builtin)
    *                 You can explicitly disable filtering by setting filter to false.
    * @param tabIndex - the tabIndex attribute value for the input element.
    * @param name - the name attribute value for the input element.
    * @param caseSensitive - use case sensitive compare in filter.
    * @param msgOpen - message to display??
    * @param msgEmptyList - message to display if data is empty.
    * @param msgEmptyFilter - message to display if no entry in data hits the filter.
    * @param busy - Controls the loading/busy spinner visibility. Presentational only! Useful for providing visual feedback while data is being loaded.
    * @param value - Controls the current value of the Combobox.
    * @param id - the id attribute value for the input element.
    * @param containerClassName - Adds a css class to the input container element.
    * @return the react component
    */
  def apply(
      defaultValue: js.UndefOr[String] = js.undefined,
      onChange: js.UndefOr[js.Any => Unit] = js.undefined,
      data: js.UndefOr[js.Array[String]] = js.undefined,
      filter: js.UndefOr[String] = js.undefined,
      tabIndex: js.UndefOr[Int] = js.undefined,
      name: js.UndefOr[String] = js.undefined,
      caseSensitive: js.UndefOr[Boolean] = js.undefined,
      msgOpen: js.UndefOr[String] = js.undefined,
      msgEmptyList: js.UndefOr[String] = js.undefined,
      msgEmptyFilter: js.UndefOr[String] = js.undefined,
      busy: js.UndefOr[Boolean] = js.undefined,
      value: js.UndefOr[String] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      containerClassName: js.UndefOr[String] = js.undefined
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent

//    logger.info("Combobox: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val messages: js.UndefOr[ComboboxComponentMessagesProperty] =
      if (
        msgOpen.isDefined || msgEmptyList.isDefined || msgEmptyFilter.isDefined
      ) {
        ComboboxComponentMessagesProperty(
          msgOpen,
          msgEmptyList,
          msgEmptyFilter
        )
      } else {
        js.undefined
      }
    val props = ComboboxComponentProperty(
      defaultValue,
      onChange,
      data,
      filter,
      tabIndex,
      name,
      caseSensitive,
      messages,
      busy,
      value,
      id,
      containerClassName
    )

    component(props)
  }

  /**
    * Instantiate the component.
    *
    * @param defaultValue - default value if the component is uncontrolled.
    *
    * @param callback - A callback fired when the current value changes.
    * @param value - Controls the current value of the Combobox.
    * @param data - An array of possible values for the Combobox.
    *               Tip: When data is an array of objects consider specifying textField and valueField as well
    * @param filter - Enable and customize filtering behavior for the Combobox. Specify one of the built-in
    *                 methods ("startsWith" "endsWith" "contains") or provide a function that returns true or false
    *                 for each passed in item (analogous to the array.filter builtin)
    *                 You can explicitly disable filtering by setting filter to false.
    * @param tabIndex - the tabIndex attribute value for the input element.
    * @param name - the name attribute value for the input element.
    * @param caseSensitive - use case sensitive compare in filter.
    * @param msgOpen - message to display??
    * @param msgEmptyList - message to display if data is empty.
    * @param msgEmptyFilter - message to display if no entry in data hits the filter.
    * @param busy - Controls the loading/busy spinner visibility. Presentational only! Useful for providing visual feedback while data is being loaded.
    * @param id - the id attribute value for the input element.
    * @param containerClassName - Adds a css class to the input container element.
    * @return the react component
    */
  def create(
      callback: js.UndefOr[String => Callback] = js.undefined,
      value: js.UndefOr[String] = js.undefined,
      data: js.UndefOr[js.Array[String]] = js.undefined,
      filter: js.UndefOr[String] = js.undefined,
      tabIndex: js.UndefOr[Int] = js.undefined,
      name: js.UndefOr[String] = js.undefined,
      caseSensitive: js.UndefOr[Boolean] = js.undefined,
      msgOpen: js.UndefOr[String] = js.undefined,
      msgEmptyList: js.UndefOr[String] = js.undefined,
      msgEmptyFilter: js.UndefOr[String] = js.undefined,
      busy: js.UndefOr[Boolean] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      containerClassName: js.UndefOr[String] = js.undefined
  ) = {  // scalafix:ok ExplicitResultTypes; ReactComponent

    def comboboxCB(data: js.Any): Unit = {
      val s = data.toString()
      logger.fine(s"Combobox.comboboxCB data=$s")
      callback.foreach(cb => cb(s).runNow())
    }

    Combobox(
      onChange = comboboxCB _,
      data = data,
      filter = filter,
      tabIndex = tabIndex,
      name = name,
      caseSensitive = caseSensitive,
      msgOpen = msgOpen,
      msgEmptyList = msgEmptyList,
      msgEmptyFilter = msgEmptyFilter,
      busy = busy,
      value = value,
      id = id,
      containerClassName = containerClassName.orElse(BaseStyles.baseStyles.comboboxLightDarkClass)
    )
  }

  protected object Internal {
    val logger: Logger = Logger("bridge.Combobox")

    // @JSGlobal("ReactWidgets.Combobox")
    // @js.native
    // object ReactWidgetsCombobox extends js.Object

    val component = JsComponent[ComboboxComponentProperty, Children.None, Null](
      reactwidgets.Combobox
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  }
}
