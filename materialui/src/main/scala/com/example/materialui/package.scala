package com.github.thebridsk

import org.scalajs.dom.raw.Element
import scala.scalajs.js
import scala.scalajs.js._
import org.scalajs.dom.raw.Node
import japgolly.scalajs.react.raw.React.Component

package object materialui {
  type AnchorElementFn = () => Element
  type AnchorElement = Element | AnchorElementFn

  type ContainerFnNode = () => Node
  type ContainerFnComp = () => Component[_, _]
  type Container = Node | Component[_, _] | ContainerFnNode | ContainerFnComp
}
