package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Js
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

  implicit class WrapProps[T <: AdditionalProps](private val props: T)
      extends AnyVal {
    def add(additionalProps: js.UndefOr[js.Dictionary[js.Any]]): T = {
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
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined
  ): P = {
    val p = props.getOrElse(apply[P]())
    p.add(additionalProps)
    onClick.foreach(p.updateDynamic("onClick")(_))
    p.asInstanceOf[P]
  }

}

trait ComponentFactory[Props <: js.Object with AdditionalProps] {

  protected val f: Js.Component[Props, Null, CtorType.PropsAndChildren]

  def apply(
      props: Props
  )(
      children: CtorType.ChildArg*
  ): Js.UnmountedWithRawType[Props, Null, Js.RawMounted[Props, Null]] = {
    val x = f(props) _
    x(children)
  }

}

trait ComponentNoChildrenFactory[Props <: js.Object with AdditionalProps] {

  protected val f: Js.Component[Props, Null, CtorType.Props]

  def create(
      props: Props
  ): Js.UnmountedWithRawType[Props, Null, Js.RawMounted[Props, Null]] = {
    f(props)
  }

}

trait FnComponentFactory[Props <: js.Object with AdditionalProps] {

  protected val f: JsFnComponent.Component[Props, CtorType.PropsAndChildren]

  def apply(
      props: Props
  )(
      children: CtorType.ChildArg*
  ): JsFnComponent.Unmounted[Props] = {
    val x = f(props) _
    x(children)
  }

}
