package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation.JSName
import japgolly.scalajs.react._
import scala.scalajs.js.UndefOr
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js.annotation.JSGlobal

@js.native
trait DropdownListComponentMessagesProperty extends js.Object {
  val open: js.UndefOr[String] = js.native
  val emptyList: js.UndefOr[String] = js.native
  val emptyFilter: js.UndefOr[String] = js.native
  val filterPlaceholder: js.UndefOr[String] = js.native
  val createOption: js.UndefOr[String|js.Function1[js.UndefOr[js.Object],String]]
}

object DropdownListComponentMessagesProperty {
  def apply( msgOpen: Option[String] = None,
             msgEmptyList: Option[String] = None,
             msgEmptyFilter: Option[String] = None): DropdownListComponentMessagesProperty = {
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
  val defaultvalue: js.UndefOr[String|js.Object] = js.native
  val value: js.UndefOr[String|js.Object] = js.native
  val onChange: js.UndefOr[js.Function2[js.UndefOr[js.Any],js.UndefOr[DropdownListMetadata],Unit]] = js.native
  val data: js.UndefOr[js.Array[String|js.Object]] = js.native
  val tabIndex: js.UndefOr[String] = js.native
  val name: js.UndefOr[String] = js.native
  val messages: js.UndefOr[DropdownListComponentMessagesProperty] = js.native
  val busy: js.UndefOr[Boolean] = js.native
  val delay: js.UndefOr[Double] = js.native
  val dropUp: js.UndefOr[Boolean] = js.native
  val allowCreate: js.UndefOr[Boolean|String] = js.native
  val disabled: js.UndefOr[Boolean] = js.native
  val placeholder: js.UndefOr[String] = js.native
  val valueField: js.UndefOr[String] = js.native
  val textField: js.UndefOr[String] = js.native
}

object DropdownListComponentProperty {
  def apply(defaultValue: Option[String|js.Object] = None,
            onChange: Option[js.Any=>Unit] = None,
            data: Option[js.Array[String|js.Object]] = None,
            tabIndex: Option[Int] = None,
            name: Option[String] = None,
            messages: Option[DropdownListComponentMessagesProperty] = None,
            busy: Option[Boolean] = None,
            value: Option[String|js.Object] = None,
            id: Option[String] = None,
            containerClassName: Option[String] = None,
            allowCreate: Option[Boolean|String] = None,
            disabled: Option[Boolean] = None,
            placeholder: Option[String] = None,
            valueField: Option[String] = None,
            textField: Option[String] = None
  ): DropdownListComponentProperty = {
    val p = js.Dynamic.literal()

    defaultValue.foreach(v => p.updateDynamic("defaultValue")(v.asInstanceOf[js.Any]))
    onChange.foreach(p.updateDynamic("onChange")(_))
    data.foreach(p.updateDynamic("data")(_))
    tabIndex.foreach( ti => p.updateDynamic("tabIndex")(ti.toString))
    name.foreach(p.updateDynamic("name")(_))
    messages.foreach(p.updateDynamic("messages")(_))
    busy.foreach(p.updateDynamic("busy")(_))
    value.foreach(v => p.updateDynamic("value")(v.asInstanceOf[js.Any]))
    id.foreach(p.updateDynamic("id")(_))
    containerClassName.foreach(p.updateDynamic("containerClassName")(_))
    allowCreate.foreach(v=>p.updateDynamic("allowCreate")(v.asInstanceOf[js.Any]))
    disabled.foreach(p.updateDynamic("disabled")(_))
    placeholder.foreach(p.updateDynamic("placeholder")(_))
    valueField.foreach(p.updateDynamic("valueField")(_))
    textField.foreach(p.updateDynamic("textField")(_))

    p.asInstanceOf[DropdownListComponentProperty]
  }
}

@js.native
trait DropdownList extends js.Object

object DropdownList {
  val logger = Logger("bridge.DropdownList")

//  @JSGlobal("ReactWidgets.DropdownList")
//  @js.native
//  object ReactWidgetsDropdownList extends js.Object

  val component = JsComponent[DropdownListComponentProperty, Children.None, Null](reactwidgets.DropdownList)

  def apply(defaultValue: Option[String|js.Object] = None,
            onChange: Option[js.Any=>Unit] = None,
            data: Option[js.Array[String|js.Object]] = None,
            tabIndex: Option[Int] = None,
            name: Option[String] = None,
            messages: Option[DropdownListComponentMessagesProperty] = None,
            busy: Option[Boolean] = None,
            value: Option[String|js.Object] = None,
            id: Option[String] = None,
            containerClassName: Option[String] = None,
            allowCreate: Option[Boolean|String] = None,
            disabled: Option[Boolean] = None,
            placeholder: Option[String] = None,
            valueField: Option[String] = None,
            textField: Option[String] = None
  ) = {

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
