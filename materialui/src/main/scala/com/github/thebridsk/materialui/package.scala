package com.github.thebridsk

import org.scalajs.dom.Element
import scala.scalajs.js
import scala.scalajs.js._
import org.scalajs.dom.Node
import japgolly.scalajs.react.facade.React.Component
import japgolly.scalajs.react.component.Generic.UnmountedRaw
import japgolly.scalajs.react.vdom.VdomNode

package object materialui {
  type AnchorElementFn = () => Element
  type AnchorElement = Element | AnchorElementFn

  type ContainerFnNode = () => Node
  type ContainerFnComp = () => Component[_, _]
  type Container = Node | Component[_, _] | ContainerFnNode | ContainerFnComp

  import scala.language.implicitConversions
  implicit def toUndef(v: UnmountedRaw): js.UndefOr[VdomNode] = {
    val r: VdomNode = v
    r
  }

  implicit def toFunction1(
      f: js.Object => Unit
  ): js.UndefOr[js.Function1[js.Object, Unit]] = f

}
