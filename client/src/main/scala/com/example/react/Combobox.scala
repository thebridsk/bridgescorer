package com.example.react

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import org.scalajs.dom.raw.HTMLElement
import japgolly.scalajs.react._
import scala.scalajs.js.UndefOr
import utils.logging.Logger
import scala.scalajs.js.annotation.JSGlobal

@js.native
trait ComboboxComponentMessagesProperty extends js.Object {
  val open: js.UndefOr[String] = js.native
  val emptyList: js.UndefOr[String] = js.native
  val emptyFilter: js.UndefOr[String] = js.native
}

object ComboboxComponentMessagesProperty {
  def apply( msgOpen: Option[String] = None,
             msgEmptyList: Option[String] = None,
             msgEmptyFilter: Option[String] = None): ComboboxComponentMessagesProperty = {
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
  val onChange: js.UndefOr[js.Any=>Unit] = js.native
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
  def apply(defaultValue: Option[String] = None,
            onChange: Option[js.Any=>Unit] = None,
            data: Option[js.Array[String]] = None,
            filter: Option[String] = None,
            tabIndex: Option[Int] = None,
            name: Option[String] = None,
            caseSensitive: Option[Boolean] = None,
            messages: Option[ComboboxComponentMessagesProperty] = None,
            busy: Option[Boolean] = None,
            value: Option[String] = None,
            id: Option[String] = None ): ComboboxComponentProperty = {
    val p = js.Dynamic.literal()

    defaultValue.foreach(p.updateDynamic("defaultValue")(_))
    onChange.foreach(p.updateDynamic("onChange")(_))
    data.foreach(p.updateDynamic("data")(_))
    filter.foreach(p.updateDynamic("filter")(_))
    tabIndex.foreach( ti => p.updateDynamic("tabIndex")(ti.toString))
    name.foreach(p.updateDynamic("name")(_))
    caseSensitive.foreach(p.updateDynamic("caseSensitive")(_))
    messages.foreach(p.updateDynamic("messages")(_))
    busy.foreach(p.updateDynamic("busy")(_))
    value.foreach(p.updateDynamic("value")(_))
    id.foreach(p.updateDynamic("id")(_))

    p.asInstanceOf[ComboboxComponentProperty]
  }
}

@js.native
trait Combobox extends js.Object

object Combobox {
  val logger = Logger("bridge.Combobox")

//  @JSGlobal("ReactWidgets.Combobox")
//  @js.native
//  object ReactWidgetsCombobox extends js.Object

  val component = JsComponent[ComboboxComponentProperty, Children.None, Null](reactwidgets.Combobox)

  def apply(defaultValue: Option[String] = None,
            onChange: Option[js.Any=>Unit] = None,
            data: Option[js.Array[String]] = None,
            filter: Option[String] = None,
            tabIndex: Option[Int] = None,
            name: Option[String] = None,
            caseSensitive: Option[Boolean] = None,
            msgOpen: Option[String] = None,
            msgEmptyList: Option[String] = None,
            msgEmptyFilter: Option[String] = None,
            busy: Option[Boolean] = None,
            value: Option[String] = None,
            id: Option[String] = None ) = {

//    logger.info("Combobox: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val messages = if (msgOpen.isDefined || msgEmptyList.isDefined || msgEmptyFilter.isDefined) {
      Some(ComboboxComponentMessagesProperty(msgOpen,msgEmptyList,msgEmptyFilter))
    } else {
      None
    }
    val props = ComboboxComponentProperty(defaultValue,onChange,data,filter,tabIndex,name,caseSensitive,messages,busy,value,id)

    component(props)
  }
}
