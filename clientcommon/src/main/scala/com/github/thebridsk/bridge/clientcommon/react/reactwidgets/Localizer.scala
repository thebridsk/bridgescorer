package com.github.thebridsk.bridge.clientcommon.react.reactwidgets

import com.github.thebridsk.bridge.clientcommon.react.reactwidgets.globalize.Moment
import com.github.thebridsk.bridge.clientcommon.react.reactwidgets.globalize.ReactWidgetsMoment


object Localizer {
  def initLocalizer(locale: String): Unit = {
    // Globalize.locale(locale)
    // ReactWidgetsGlobalize()

    Moment.locale(locale)
    ReactWidgetsMoment()

  }
}
