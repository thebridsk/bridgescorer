package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import scala.scalajs.js.|
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger

@js.native
trait DropdownListComponentMessagesProperty extends js.Object {
  val open: js.UndefOr[String] = js.native
  val emptyList: js.UndefOr[String] = js.native
  val emptyFilter: js.UndefOr[String] = js.native
  val filterPlaceholder: js.UndefOr[String] = js.native
  val createOption: js.UndefOr[
    String | js.Function1[js.UndefOr[js.Object], String]
  ]
}

object DropdownListComponentMessagesProperty {
  def apply(
      msgOpen: Option[String] = None,
      msgEmptyList: Option[String] = None,
      msgEmptyFilter: Option[String] = None
  ): DropdownListComponentMessagesProperty = {
    val p = js.Dynamic.literal()

    msgOpen.foreach(p.updateDynamic("open")(_))
    msgEmptyList.foreach(p.updateDynamic("emptyList")(_))
    msgEmptyFilter.foreach(p.updateDynamic("emptyFilter")(_))

//    DropdownList.logger.info("DropdownListComponentMessagesProperty: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    p.asInstanceOf[DropdownListComponentMessagesProperty]
  }
}

@js.native
trait DropdownListMetadata extends js.Object {
  val lastValue: js.UndefOr[js.Any] = js.native
  val searchTerm: js.UndefOr[String] = js.native
  val originalEvent: js.UndefOr[js.Object] = js.native
}

@js.native
trait DropdownListComponentProperty extends js.Object {
  val defaultvalue: js.UndefOr[String | js.Object] = js.native
  val value: js.UndefOr[String | js.Object] = js.native
  val onChange: js.UndefOr[
    js.Function2[js.UndefOr[js.Any], js.UndefOr[DropdownListMetadata], Unit]
  ] = js.native
  val data: js.UndefOr[js.Array[String | js.Object]] = js.native
  val tabIndex: js.UndefOr[String] = js.native
  val name: js.UndefOr[String] = js.native
  val messages: js.UndefOr[DropdownListComponentMessagesProperty] = js.native
  val busy: js.UndefOr[Boolean] = js.native
  val delay: js.UndefOr[Double] = js.native
  val dropUp: js.UndefOr[Boolean] = js.native
  val allowCreate: js.UndefOr[Boolean | String] = js.native
  val disabled: js.UndefOr[Boolean] = js.native
  val placeholder: js.UndefOr[String] = js.native
  val valueField: js.UndefOr[String] = js.native
  val textField: js.UndefOr[String] = js.native
}

object DropdownListComponentProperty {
  def apply(
      defaultValue: Option[String | js.Object] = None,
      onChange: Option[js.Any => Unit] = None,
      data: Option[js.Array[String | js.Object]] = None,
      tabIndex: Option[Int] = None,
      name: Option[String] = None,
      messages: Option[DropdownListComponentMessagesProperty] = None,
      busy: Option[Boolean] = None,
      value: Option[String | js.Object] = None,
      id: Option[String] = None,
      containerClassName: Option[String] = None,
      allowCreate: Option[Boolean | String] = None,
      disabled: Option[Boolean] = None,
      placeholder: Option[String] = None,
      valueField: Option[String] = None,
      textField: Option[String] = None
  ): DropdownListComponentProperty = {
    val p = js.Dynamic.literal()

    defaultValue.foreach(v =>
      p.updateDynamic("defaultValue")(v.asInstanceOf[js.Any])
    )
    onChange.foreach(p.updateDynamic("onChange")(_))
    data.foreach(p.updateDynamic("data")(_))
    tabIndex.foreach(ti => p.updateDynamic("tabIndex")(ti.toString))
    name.foreach(p.updateDynamic("name")(_))
    messages.foreach(p.updateDynamic("messages")(_))
    busy.foreach(p.updateDynamic("busy")(_))
    value.foreach(v => p.updateDynamic("value")(v.asInstanceOf[js.Any]))
    id.foreach(p.updateDynamic("id")(_))
    containerClassName.foreach(p.updateDynamic("containerClassName")(_))
    allowCreate.foreach(v =>
      p.updateDynamic("allowCreate")(v.asInstanceOf[js.Any])
    )
    disabled.foreach(p.updateDynamic("disabled")(_))
    placeholder.foreach(p.updateDynamic("placeholder")(_))
    valueField.foreach(p.updateDynamic("valueField")(_))
    textField.foreach(p.updateDynamic("textField")(_))

    p.asInstanceOf[DropdownListComponentProperty]
  }
}

@js.native
trait DropdownList extends js.Object

/**
  * Dropdown list react component.
  *
  * A wrapper for the DropdownList component in react-widgets npm package.
  * https://jquense.github.io/react-widgets/api/DropdownList/
  *
  * Usage:
  *
  * {{{
  * case class State(
  *   val currentValue: String = ""
  * )
  *
  * def onChange(newvalue: js.Any): Unit = {
  *   scope.modState(_.copy(currentValue = newvalue.toString))
  * }
  *
  * val data: js.Array(...)
  *
  * DropdownList(
  *   value = state.currentValue,
  *   data = data,
  *   onChange = onChange _
  * )
  *
  * }}}
  *
  * @see See [[apply]] for a description of parameters.
  */
object DropdownList {
  private val logger: Logger = Logger("bridge.DropdownList")

//  @JSGlobal("ReactWidgets.DropdownList")
//  @js.native
//  object ReactWidgetsDropdownList extends js.Object

  private val component =
    JsComponent[DropdownListComponentProperty, Children.None, Null](
      reactwidgets.DropdownList
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * Instantiate the react component
    *
    * @param defaultValue - The initial value of the value field.
    * @param onChange - A callback fired when the current value changes.
    * @param data - An array of possible values for the DropdownList.
    *               Tip: When data is an array of objects consider
    *               specifying textField and valueField as well.
    *               Default value is the empty array.
    * @param tabIndex - the tabindex attribute of the input field.
    * @param name - The name attribute of the input field.
    * @param messages - Custom messages.
    * @param busy - Controls the loading/busy spinner visibility. Presentational only!
    *               Useful for providing visual feedback while data is being loaded.
    *               Default value is false.
    * @param value - Controls the current value of the DropdownList.
    * @param id - The id attribute of the input field.
    * @param containerClassName - Adds a css class to the input container element.
    * @param allowCreate - Enables the list option creation UI. onFilter will only
    *                      the UI when actively filtering for a list item.
    *                      Default value is false.
    * @param disabled - true if the input field is disabled.  Default value is false.
    * @param placeholder - Text to display in the input when the value is empty.
    * @param valueField - A property name that provides the value of the data items.
    *                     This value is used to uniquely distinigush items from others
    *                     in the data list.
    *                     Generally, valueField points to an Id field, or other unique identifier.
    *                     When not provided, the referential identity of each data item is used.
    * @param textField - A property name, or accessor function, that provides the text
    *                    content of the data items. The DropdownList will filter data
    *                    based on this value as well as use it as the default display
    *                    value for list items and selected values.
    *
    * @return the unmounted react component
    *
    * @see See [[DropdownList]] for usage information.
    */
  def apply(
      defaultValue: Option[String | js.Object] = None,
      onChange: Option[js.Any => Unit] = None,
      data: Option[js.Array[String | js.Object]] = None,
      tabIndex: Option[Int] = None,
      name: Option[String] = None,
      messages: Option[DropdownListComponentMessagesProperty] = None,
      busy: Option[Boolean] = None,
      value: Option[String | js.Object] = None,
      id: Option[String] = None,
      containerClassName: Option[String] = None,
      allowCreate: Option[Boolean | String] = None,
      disabled: Option[Boolean] = None,
      placeholder: Option[String] = None,
      valueField: Option[String] = None,
      textField: Option[String] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent

//    logger.info("DropdownList: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val props = DropdownListComponentProperty(
      defaultValue,
      onChange,
      data,
      tabIndex,
      name,
      messages,
      busy,
      value,
      id,
      containerClassName,
      allowCreate,
      disabled,
      placeholder,
      valueField,
      textField
    )

    component(props)
  }
}
