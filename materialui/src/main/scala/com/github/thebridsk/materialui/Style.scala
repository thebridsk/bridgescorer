package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.annotation._
// import com.github.thebridsk.utilities.logging.Logger

class Style(styles: (String, js.Dictionary[js.Any])*) {

  def asJS: js.Dictionary[js.Dictionary[js.Any]] = {
    val r = js.Dictionary[js.Dictionary[js.Any]](styles: _*)
    r
  }
}

object Style {

//  private val log = Logger("bridge.mui.Style")

  def apply(styles: (String, js.Dictionary[js.Any])*): Style = {
    new Style(styles: _*)
  }

//  import { withStyles } from '@material-ui/styles'

  @js.native @JSImport("@material-ui/core/styles/withStyles", JSImport.Default)
  private object WithStyle
      extends js.Any // with Function2[js.Object,js.Object,Function1[js.Any,VdomNode]]

//  log.fine("WithStyle: "+WithStyle)

//  private val f = WithStyle.asInstanceOf[Function2[js.Object,js.Object,Function1[js.Any,TagMod]]]
//  private val f = WithStyle.asInstanceOf[(js.Object,js.Object)=> js.Any => TagMod ]
  private val f = WithStyle.asInstanceOf[js.Dynamic]

//  log.fine("WithStyle f: "+f)

  /**
    * Does NOT work
    */
  def withStyle(
      styles: Style,
      options: js.Object = js.Object()
  )(
      child: js.Object => CtorType.ChildArg
  ): VdomNode = {
//    val p = js.Dynamic.literal()
//    p.updateDynamic("styles")(styles.asJS)
//    p.updateDynamic("options")(options)

    val c = f(styles.asJS, options)
//    log.fine("WithStyle c: "+c)
    val x = c(child.asInstanceOf[js.Any])
//    log.fine("WithStyle x: "+x)
    x.asInstanceOf[VdomNode]
  }

}
