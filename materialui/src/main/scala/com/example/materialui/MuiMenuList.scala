package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait MenuListProps extends ListProps {}
object MenuListProps extends PropsFactory[MenuListProps] {

  /**
    * @param p the object that will become the properties object
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @parem component The component used for the root node. Either a string to use a DOM element or a component.
    *                   By default, it's a li when button is false and a div when button is true
    * @param dense If true, compact vertical padding designed for keyboard and mouse input will be used.
    *               Default: false
    * @param disablePadding If true, vertical padding will be removed from the list.
    *                        Default: false
    * @param subheader The content of the subheader, normally ListSubheader.
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: MenuListProps](
      props: js.UndefOr[P] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      dense: js.UndefOr[Boolean] = js.undefined,
      disablePadding: js.UndefOr[Boolean] = js.undefined,
      subheader: js.UndefOr[js.Object] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p: P = ListProps(
      props,
      classes,
      component,
      dense,
      disablePadding,
      subheader,
      additionalProps
    )
    p
  }

}

object MuiMenuList extends ComponentFactory[MenuListProps] {
  @js.native @JSImport("@material-ui/core/MenuList", JSImport.Default) private object MenuList
      extends js.Any

  protected val f = JsComponent[MenuListProps, Children.Varargs, Null](MenuList)

  /**
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @parem component The component used for the root node. Either a string to use a DOM element or a component.
    *                   By default, it's a li when button is false and a div when button is true
    * @param dense If true, compact vertical padding designed for keyboard and mouse input will be used.
    *               Default: false
    * @param disablePadding If true, vertical padding will be removed from the list.
    *                        Default: false
    * @param subheader The content of the subheader, normally ListSubheader.
    * @param children
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      dense: js.UndefOr[Boolean] = js.undefined,
      disablePadding: js.UndefOr[Boolean] = js.undefined,
      subheader: js.UndefOr[js.Object] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: MenuListProps = MenuListProps(
      classes = classes,
      component = component,
      dense = dense,
      disablePadding = disablePadding,
      subheader = subheader,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
