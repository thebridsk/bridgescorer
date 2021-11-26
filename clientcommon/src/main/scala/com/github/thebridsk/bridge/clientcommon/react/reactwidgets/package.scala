package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

package object reactwidgets {

  @JSImport("react-widgets/lib/Combobox", JSImport.Namespace, "Combobox")
  @js.native
  object Combobox extends Combobox

  @JSImport(
    "react-widgets/lib/DropdownList",
    JSImport.Namespace,
    "DropdownList"
  )
  @js.native
  object DropdownList extends DropdownList

  @JSImport(
    "react-widgets/lib/DateTimePicker",
    JSImport.Namespace,
    "DateTimePicker"
  )
  @js.native
  object DateTimePicker extends DateTimePicker

}
