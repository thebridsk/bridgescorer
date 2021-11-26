package com.github.thebridsk.bridge.clientcommon.react.reactwidgets

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

package object globalize {

    // @js.native
    // trait CldrData extends js.Object {

    //   def entireSupplemental(): js.Object = js.native
    //   def entireMainFor( ): js.Object = js.native
    // }

    // @js.native
    // trait Globalize extends js.Object {
    //   def locale( loc: String ): Unit = js.native
    //   def load(): Unit = js.native
    //   def loadTimeZone(): Unit = js.native
    // }

    // @JSImport("globalize", JSImport.Namespace)
    // @js.native
    // object Globalize extends Globalize

    // @JSImport("react-widgets-globalize", JSImport.Namespace)
    // @js.native
    // object ReactWidgetsGlobalize extends js.Any {
    //   def apply(): Unit = js.native
    // }

    @js.native
    trait Moment extends js.Object {
      def locale(loc: String): Unit = js.native
    }

    @JSImport("moment", JSImport.Namespace)
    @js.native
    object Moment extends Moment

    @JSImport("react-widgets-moment", JSImport.Default)
    @js.native
    object ReactWidgetsMoment extends js.Any {
      def apply(): Unit = js.native
    }

}
