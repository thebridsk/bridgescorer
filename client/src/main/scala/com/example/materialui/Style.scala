package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import japgolly.scalajs.react.raw.React.Component
import japgolly.scalajs.react.component.Generic.ComponentRaw
import utils.logging.Logger

object Style {

  val log = Logger("bridge.mui.Style")

//  import { withStyles } from '@material-ui/styles'

  @js.native @JSImport("@material-ui/core/styles/withStyles", JSImport.Default)
  object WithStyle extends js.Any // with Function2[js.Object,js.Object,Function1[js.Any,TagMod]]

  log.fine("WithStyle: "+WithStyle)

//  private val f = WithStyle.asInstanceOf[Function2[js.Object,js.Object,Function1[js.Any,TagMod]]]
//  private val f = WithStyle.asInstanceOf[(js.Object,js.Object)=> js.Any => TagMod ]
  private val f = WithStyle.asInstanceOf[js.Dynamic]

  log.fine("WithStyle f: "+f)

  def withStyle(
      styles: js.Object,
      options: js.Object = js.Object()
  )(
      child: CtorType.ChildArg
  ): TagMod = {
    val p = js.Dynamic.literal()
    p.updateDynamic("styles")(styles)
    p.updateDynamic("options")(options)

    val c = f(styles,options)
    log.fine("WithStyle c: "+f)
    val x = c(child.asInstanceOf[js.Any])
    log.fine("WithStyle x: "+f)
    x.asInstanceOf[TagMod]
  }

}
