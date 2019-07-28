package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Js
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait AdditionalProps extends js.Object {

  /** Reads a field of this object. */
  @JSBracketAccess
  def selectDynamic(name: String): Dynamic = js.native

  /** Writes a field of this object. */
  @JSBracketAccess
  def updateDynamic(name: String)(value: js.Any): Unit = js.native

}

object AdditionalProps {

  implicit class WrapProps[T <: AdditionalProps](val props: T) extends AnyVal {
    def add(additionalProps: js.UndefOr[js.Dictionary[js.Any]]) = {
      additionalProps.foreach { ap =>
        ap.foreach { e =>
          val (key, value) = e
          props.updateDynamic(key)(value)
        }
      }
      props
    }
  }
}

trait PropsFactory[Props <: js.Object with AdditionalProps] {

  def apply[P <: Props](): Props = {
    js.Dynamic.literal().asInstanceOf[P]
  }

  def get[P <: Props](
      props: js.UndefOr[P] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = props.getOrElse(apply[P]())
    p.add(additionalProps).asInstanceOf[P]
  }

}

trait ComponentFactory[Props <: js.Object with AdditionalProps] {

  protected val f: Js.Component[Props, Null, CtorType.PropsAndChildren]

  def apply(
      props: Props
  )(
      children: CtorType.ChildArg*
  ) = {
    val x = f(props) _
    x(children)
  }

}

trait FnComponentFactory[Props <: js.Object with AdditionalProps] {

  protected val f: JsFnComponent.Component[Props, CtorType.PropsAndChildren]

  def apply(
      props: Props
  )(
      children: CtorType.ChildArg*
  ) = {
    val x = f(props) _
    x(children)
  }

}
