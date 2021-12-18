package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait ListProps extends AdditionalProps with StandardProps {
  val classes: js.UndefOr[js.Dictionary[String]] = js.undefined
  val component: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[Boolean] = js.undefined
  val disablePadding: js.UndefOr[Boolean] = js.undefined
  val subheader: js.UndefOr[js.Object] = js.undefined

}

object ListProps extends PropsFactory[ListProps] {

  /**
    * @param p the object that will become the properties object
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param component The component used for the root node. Either a string to use a DOM element or a component.
    *                   By default, it's a li when button is false and a div when button is true
    * @param dense If true, compact vertical padding designed for keyboard and mouse input will be used.
    *               Default: false
    * @param disablePadding If true, vertical padding will be removed from the list.
    *                        Default: false
    * @param subheader The content of the subheader, normally ListSubheader.
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: ListProps](
      props: js.UndefOr[P] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      dense: js.UndefOr[Boolean] = js.undefined,
      disablePadding: js.UndefOr[Boolean] = js.undefined,
      subheader: js.UndefOr[js.Object] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    classes.foreach(p.updateDynamic("classes")(_))
    component.foreach(p.updateDynamic("component")(_))
    dense.foreach(p.updateDynamic("dense")(_))
    disablePadding.foreach(p.updateDynamic("disablePadding")(_))
    subheader.foreach(p.updateDynamic("subheader")(_))
    className.foreach(p.updateDynamic("className")(_))

    p
  }

}

object MuiList extends ComponentFactory[ListProps] {
  @js.native @JSImport(
    "@mui/material/List",
    JSImport.Default
  ) private object MList extends js.Any

  protected val f =
    JsComponent[ListProps, Children.Varargs, Null](
      MList
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param component The component used for the root node. Either a string to use a DOM element or a component.
    *                   By default, it's a li when button is false and a div when button is true
    * @param dense If true, compact vertical padding designed for keyboard and mouse input will be used.
    *               Default: false
    * @param disablePadding If true, vertical padding will be removed from the list.
    *                        Default: false
    * @param subheader The content of the subheader, normally ListSubheader.
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    * @param children
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      dense: js.UndefOr[Boolean] = js.undefined,
      disablePadding: js.UndefOr[Boolean] = js.undefined,
      subheader: js.UndefOr[js.Object] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: ListProps = ListProps(
      classes = classes,
      component = component,
      dense = dense,
      disablePadding = disablePadding,
      subheader = subheader,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
