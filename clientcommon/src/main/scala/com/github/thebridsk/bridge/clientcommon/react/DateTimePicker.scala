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
  val onChange: js.UndefOr[Date => Unit] = js.native
  val currentDate: js.UndefOr[Date] = js.native
  val defaultCurrentDate: js.UndefOr[Date] = js.native
  val onCurrentDateChange: js.UndefOr[Date => Unit] = js.native
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
  def apply(
      name: String,
      value: Option[Date] = None,
      defaultValue: Option[Date] = None,
      onChange: Option[Date => Unit] = None,
      currentDate: Option[Date] = None,
      defaultCurrentDate: Option[Date] = None,
      onCurrentDateChange: Option[Date => Unit] = None,
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


/**
  * A date and time picker react component.
  *
  * This wrappers the DataTimePicker from react-widgets npm package.
  * https://jquense.github.io/react-widgets/api/DateTimePicker/
  *
  * Usage:
  *
  * {{{
  * case class State(
  *   played: SystemTime.Timestamp = SystemTime.currentTimeMillis(),
  * )
  *
  * def setPlayed(value: Date): Unit = {
  *   scope
  *     .modState { s =>
  *       val t =
  *         if (js.isUndefined(value) || value == null) 0 else value.getTime()
  *       s.copy(played = t)
  *     }
  *     .runNow()
  * }
  *
  * DateTimePicker(
  *   "played",
  *   defaultValue =
  *     if (state.played == 0) Some(new Date())
  *     else Some(new Date(state.played)),
  *   defaultCurrentDate = Some(new Date()),
  *   onChange = Some(setPlayed),
  *   disabled = false
  * )
  * }}}
  *
  * @see See the [[apply]] method for a description of all parameters.
  *
  */
object DateTimePicker {
  private val logger: Logger = Logger("bridge.DateTimePicker")

//  @JSGlobal("ReactWidgets.DateTimePicker")
//  @js.native
//  object ReactWidgetsDateTimePicker extends js.Object

  private val component =
    JsComponent[DateTimePickerComponentProperty, Children.None, Null](
      reactwidgets.DateTimePicker
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * DateTime picker component.
    *
    * @param name - The HTML name attribute, passed directly to the input element.
    * @param value - The current value of the DateTimePicker.
    * @param defaultValue - The default value for an uncontrolled DataTimePicker
    * @param onChange - A callback fired when the current value changes.
    * @param currentDate - Default current date at which the calendar opens. If none is provided, opens at today's date or the value date (if any).
    * @param defaultCurrentDate - The default current date for an uncontrolled DateTimePicker
    * @param onCurrentDateChange - Change event Handler that is called when the currentDate is changed. The handler is called with the currentDate object.
    * @param autoFocus - Pass focus to the DateTimePicker when it mounts.
    * @param culture - Set the culture of the DateTimePicker, passed to the configured localizer.
    * @param date - Enable the calendar component of the picker.
    * @param disabled - true if the component is disabled.
    * @param dropUp - Controls the opening direction of the DateTimePicker popup.
    * @param editFormat - A formatter to be used while the date input has focus. Useful for showing a simpler format for inputing.
    *                     For more information about formats visit the Localization page
    * @param format - A formatter used to display the date value. For more information about formats visit the Localization page
    * @param max - The maximum Date that can be selected.
    *              Max only limits selection, it doesn't constrain the date values that can be typed or pasted into the widget.
    *              If you need this behavior you can constrain values via the onChange handler.
    * @param min - The minimum Date that can be selected. Min only limits selection,
    *              it doesn't constrain the date values that can be typed or pasted into the widget.
    *              If you need this behavior you can constrain values via the onChange handler.
    * @param messages - Object hash containing display text and/or text for screen readers.
    *                   Use the messages object to localize widget text or provide custom rendering.
    *                   Fields:
    *                     dateButton?: string
    *                     timeButton?: string
    * @param open - Controls the visibility of the DateTimePicker popup. Use defaultOpen to set an initial value for uncontrolled widgets.
    * @param parse - Determines how the widget parses the typed date string into a Date object. You can provide an array of formats to try,
    *                or provide a function that returns a date to handle parsing yourself. When parse is unspecified and the format prop
    *                is a string parse will automatically use that format as its default.
    * @param placeHolder - Text to display in the input when the value is empty.
    * @param popupTransition - A Transition component from react-transition-group v2. The provided component will be used instead of the
    *  *     default SlideDownTransition for fully customizable animations. The transition component is also injected
    *  *     with a dropUp prop indicating the direction it should open.
    * @param readOnly - Controls the read-only state of the DateTimePicker.
    * @param step - The amount of minutes between each entry in the time list.
    * @param time - Enable the time list component of the picker.
    * @param containerClassName - Adds a css class to the input container element.
    *
    * @return the react component.
    *
    * @see See [[DateTimePicker$]] for usage information.
    *
    */
  def apply(
      name: String,
      value: Option[Date] = None,
      defaultValue: Option[Date] = None,
      onChange: Option[Date => Unit] = None,
      currentDate: Option[Date] = None,
      defaultCurrentDate: Option[Date] = None,
      onCurrentDateChange: Option[Date => Unit] = None,
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
      containerClassName: Option[String] = Some(
        BaseStyles.baseStyles.calendarLightDarkClass
      )
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent

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
