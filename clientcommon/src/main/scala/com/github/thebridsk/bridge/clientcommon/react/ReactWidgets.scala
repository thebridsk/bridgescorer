package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

//@js.native
//trait ReactWidgets extends js.Object {
//
//  val Combobox: Combobox = js.native
//
//}
//
//@JSImport("react-widgets", JSImport.Namespace, "ReactWidgets")
//@js.native
//object ReactWidgets extends ReactWidgets

package reactwidgets {

  @JSImport("react-widgets/lib/Combobox", JSImport.Namespace, "Combobox")
  @js.native
  object Combobox extends Combobox

  @JSImport("react-widgets/lib/DateTimePicker", JSImport.Namespace, "DateTimePicker")
  @js.native
  object DateTimePicker extends DateTimePicker


  package globalize {

  //  @js.native
  //  trait Globalize extends js.Object {
  //    def locale( loc: String ): Unit = js.native
  //  }
  //
  //  @JSImport("globalize", JSImport.Namespace, "Globalize")
  //  @js.native
  //  object Globalize extends Globalize


  //  @js.native
  //  trait ReactWidgetsGlobalize extends js.Object {
  //    def globalizeLocalizer(): Unit = js.native
  //  }
  //
  //  @JSImport("react-widgets-globalize", JSImport.Namespace)
  //  @js.native
  //  object ReactWidgetsGlobalize extends ReactWidgetsGlobalize

    @js.native
    trait Moment extends js.Object {
      def locale( loc: String ): Unit = js.native
    }

    @JSImport("moment", JSImport.Namespace )
    @js.native
    object Moment extends Moment

    @JSImport("react-widgets-moment", JSImport.Default)
    @js.native
    object ReactWidgetsMoment extends js.Any {
      def apply(): Unit = js.native
    }

  }


}
