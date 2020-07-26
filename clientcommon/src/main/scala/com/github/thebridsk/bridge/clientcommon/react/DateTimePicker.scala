package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js.Date
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles

@js.native
trait DateTimePickerComponentProperty extends js.Object {
  val name: js.UndefOr[String] = js.native
  val value: js.UndefOr[Date] = js.native
  val defaultValue: js.UndefOr[Date] = js.native
  val onChange: js.UndefOr[Date=>Unit] = js.native
  val currentDate: js.UndefOr[Date] = js.native
  val defaultCurrentDate: js.UndefOr[Date] = js.native
  val onCurrentDateChange: js.UndefOr[Date=>Unit] = js.native
  val autoFocus: js.UndefOr[Boolean] = js.native
  val culture: js.UndefOr[String] = js.native
  val date: js.UndefOr[Boolean] = js.native
  val disabled: js.UndefOr[Boolean] = js.native
  val dropUp: js.UndefOr[Boolean] = js.native
  val editFormat: js.UndefOr[String] = js.native
  val format: js.UndefOr[String] = js.native
  val max: js.UndefOr[Date] = js.native
  val min: js.UndefOr[Date] = js.native
  val messages: js.UndefOr[js.Object] = js.native
  val open: js.UndefOr[js.Any] = js.native
  val parse: js.UndefOr[js.Any] = js.native
  val placeHolder: js.UndefOr[String] = js.native
  val popupTransition: js.UndefOr[String] = js.native
  val readOnly: js.UndefOr[Boolean] = js.native
  val step: js.UndefOr[Int] = js.native
  val time: js.UndefOr[Boolean] = js.native
  val containerClassName: js.UndefOr[String] = js.native
}

object DateTimePickerComponentProperty {
  def apply(name: String,
            value: Option[Date] = None,
            defaultValue: Option[Date] = None,
            onChange: Option[Date=>Unit] = None,
            currentDate: Option[Date] = None,
            defaultCurrentDate: Option[Date] = None,
            onCurrentDateChange: Option[Date=>Unit] = None,
            autoFocus: Boolean = false,
            culture: Option[String] = None,
            date: Boolean = true,
            disabled: Boolean = false,
            dropUp: Boolean = false,
            editFormat: Option[String] = None,
            format: Option[String] = None,
            max: Option[Date] = None,
            min: Option[Date] = None,
            messages: Option[js.Object] = None,
            open: Option[js.Any] = None,
            parse: Option[js.Any] = None,
            placeHolder: Option[String] = None,
            popupTransition: Option[String] = None,
            readOnly: Boolean = false,
            step: Option[Int] = None,
            time: Boolean = true,
            containerClassName: Option[String] = None
  ): DateTimePickerComponentProperty = {
    val p = js.Dynamic.literal()

    p.updateDynamic("name")(name)

    value.foreach(p.updateDynamic("value")(_))
    defaultValue.foreach(p.updateDynamic("defaultValue")(_))
    onChange.foreach(p.updateDynamic("onChange")(_))
    currentDate.foreach(p.updateDynamic("currentDate")(_))
    defaultCurrentDate.foreach(p.updateDynamic("defaultCurrentDate")(_))
    onCurrentDateChange.foreach(p.updateDynamic("onCurrentDateChange")(_))
    p.updateDynamic("autoFocus")(autoFocus)
    culture.foreach(p.updateDynamic("culture")(_))
    p.updateDynamic("date")(date)
    p.updateDynamic("disabled")(disabled)
    p.updateDynamic("dropUp")(dropUp)
    editFormat.foreach(p.updateDynamic("editFormat")(_))
    format.foreach(p.updateDynamic("format")(_))
    max.foreach(p.updateDynamic("max")(_))
    min.foreach(p.updateDynamic("min")(_))
    messages.foreach(p.updateDynamic("messages")(_))
    open.foreach(p.updateDynamic("open")(_))
    parse.foreach(p.updateDynamic("parse")(_))
    placeHolder.foreach(p.updateDynamic("placeHolder")(_))
    popupTransition.foreach(p.updateDynamic("popupTransition")(_))
    p.updateDynamic("readOnly")(readOnly)
    step.foreach(p.updateDynamic("step")(_))
    p.updateDynamic("time")(time)
    containerClassName.foreach(p.updateDynamic("containerClassName")(_))

    p.asInstanceOf[DateTimePickerComponentProperty]
  }
}

@js.native
trait DateTimePicker extends js.Object

object DateTimePicker {
  val logger: Logger = Logger("bridge.DateTimePicker")

//  @JSGlobal("ReactWidgets.DateTimePicker")
//  @js.native
//  object ReactWidgetsDateTimePicker extends js.Object

  val component = JsComponent[DateTimePickerComponentProperty, Children.None, Null](reactwidgets.DateTimePicker)  // scalafix:ok ExplicitResultTypes; ReactComponent

  def apply(name: String,
            value: Option[Date] = None,
            defaultValue: Option[Date] = None,
            onChange: Option[Date=>Unit] = None,
            currentDate: Option[Date] = None,
            defaultCurrentDate: Option[Date] = None,
            onCurrentDateChange: Option[Date=>Unit] = None,
            autoFocus: Boolean = false,
            culture: Option[String] = None,
            date: Boolean = true,
            disabled: Boolean = false,
            dropUp: Boolean = false,
            editFormat: Option[String] = None,
            format: Option[String] = None,
            max: Option[Date] = None,
            min: Option[Date] = None,
            messages: Option[js.Object] = None,
            open: Option[js.Any] = None,
            parse: Option[js.Any] = None,
            placeHolder: Option[String] = None,
            popupTransition: Option[String] = None,
            readOnly: Boolean = false,
            step: Option[Int] = None,
            time: Boolean = true,
            containerClassName: Option[String] = Some(BaseStyles.baseStyles.calendarLightDarkClass)
           ) = {  // scalafix:ok ExplicitResultTypes; ReactComponent

//    logger.info("DateTimePicker: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val props = DateTimePickerComponentProperty(
                  name,
                  value,
                  defaultValue,
                  onChange,
                  currentDate,
                  defaultCurrentDate,
                  onCurrentDateChange,
                  autoFocus,
                  culture,
                  date,
                  disabled,
                  dropUp,
                  editFormat,
                  format,
                  max,
                  min,
                  messages,
                  open,
                  parse,
                  placeHolder,
                  popupTransition,
                  readOnly,
                  step,
                  time,
                  containerClassName
                )

    component(props)
  }
}
