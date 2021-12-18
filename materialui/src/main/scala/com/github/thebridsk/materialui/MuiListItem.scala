package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.UndefOr

class AlignItem(val value: String) extends AnyVal
object AlignItem {
  val flexStart = new AlignItem("flex-start")
  val center = new AlignItem("center")

  val values: List[AlignItem] = List(flexStart, center)
}

@js.native
protected trait ListItemPropsPrivate extends js.Any {
  @JSName("alignItems")
  val alignItemsInternal: js.UndefOr[String] = js.native
}

@js.native
trait ListItemProps
    extends AdditionalProps
    with ListItemPropsPrivate
    with StandardProps {
  // val alignItem: js.UndefOr[AlignItem] = js.native
  val autoFocus: js.UndefOr[Boolean] = js.native
  val button: js.UndefOr[Boolean] = js.native
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  val component: js.UndefOr[String] = js.native
  val ContainerComponent: js.UndefOr[String] = js.native
  val ContainerProps: js.UndefOr[js.Object] = js.native
  val dense: js.UndefOr[Boolean] = js.native
  val disabled: js.UndefOr[Boolean] = js.native
  val disableGutters: js.UndefOr[Boolean] = js.native
  val divider: js.UndefOr[Boolean] = js.native
  val selected: js.UndefOr[Boolean] = js.native
  val id: js.UndefOr[String] = js.native
}

object ListItemProps extends PropsFactory[ListItemProps] {

  implicit class WrapListItemProps(private val p: ListItemProps)
      extends AnyVal {
    def alignItems: UndefOr[AlignItem] =
      p.alignItemsInternal.map(s => new AlignItem(s))

//    def alignItems_= (v: js.UndefOr[AlignItem]): Unit = {
//      v.map{ vv=>p.alignItemsInternal=vv.value; None }.
//        orElse{ p.alignItemsInternal=js.undefined; None }
//    }

  }

  /**
    * @param p the object that will become the properties object
    * @param alignItems Defines the align-items style property.
    *                    Default: center
    * @param autoFocus If true, the list item will be focused during the first mount. Focus will also be triggered
    *                  if the value changes from false to true.
    * @param button If true, the list item will be a button (using ButtonBase).
    *                Default: false
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param component The component used for the root node. Either a string to use a DOM element or a component.
    *                   By default, it's a li when button is false and a div when button is true
    * @param ContainerComponent The container component used when a ListItemSecondaryAction is rendered.
    *                            Default: "li"
    * @param ContainerProps Properties applied to the container element when the component is used to
    *                        display a ListItemSecondaryAction
    * @param dense If true, compact vertical padding designed for keyboard and mouse input will be used.
    *               Default: false
    * @param disabled If true, the list item will be disabled.
    *                  Default: false
    * @param disableGutters If true, the left and right padding is removed.
    *                        Default: false
    * @param divider If true, a 1px light border is added to the bottom of the list item.
    *                 Default: false
    * @param selected Use to apply selected styling.
    *                  Default: false
    * @param id the value of the id attribute
    * @param className css class name to add to element
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: ListItemProps](
      props: js.UndefOr[P] = js.undefined,
      alignItems: js.UndefOr[AlignItem] = js.undefined,
      autoFocus: js.UndefOr[Boolean] = js.undefined,
      button: js.UndefOr[Boolean] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      ContainerComponent: js.UndefOr[String] = js.undefined,
      ContainerProps: js.UndefOr[js.Object] = js.undefined,
      dense: js.UndefOr[Boolean] = js.undefined,
      disabled: js.UndefOr[Boolean] = js.undefined,
      disableGutters: js.UndefOr[Boolean] = js.undefined,
      divider: js.UndefOr[Boolean] = js.undefined,
      selected: js.UndefOr[Boolean] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      onClick: js.UndefOr[(ReactEvent) => Unit] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps, onClick)

    alignItems.foreach(v => p.updateDynamic("alignItems")(v.value))
    autoFocus.foreach(p.updateDynamic("autoFocus")(_))
    button.foreach(p.updateDynamic("button")(_))
    classes.foreach(p.updateDynamic("classes")(_))
    component.foreach(p.updateDynamic("component")(_))
    ContainerComponent.foreach(p.updateDynamic("ContainerComponent")(_))
    ContainerProps.foreach(p.updateDynamic("ContainerProps")(_))
    dense.foreach(p.updateDynamic("dense")(_))
    disabled.foreach(p.updateDynamic("disabled")(_))
    disableGutters.foreach(p.updateDynamic("disableGutters")(_))
    divider.foreach(p.updateDynamic("divider")(_))
    selected.foreach(p.updateDynamic("selected")(_))
    id.foreach(p.updateDynamic("id")(_))
    className.foreach(p.updateDynamic("className")(_))

    p
  }

}

object MuiListItem extends ComponentFactory[ListItemProps] {
  @js.native @JSImport(
    "@mui/material/ListItem",
    JSImport.Default
  ) private object ListItem extends js.Any

  protected val f =
    JsComponent[ListItemProps, Children.Varargs, Null](
      ListItem
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param alignItems Defines the align-items style property.
    *                    Default: center
    * @param autoFocus If true, the list item will be focused during the first mount. Focus will also be triggered
    *                  if the value changes from false to true.
    * @param button If true, the list item will be a button (using ButtonBase).
    *                Default: false
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param component The component used for the root node. Either a string to use a DOM element or a component.
    *                   By default, it's a li when button is false and a div when button is true
    * @param ContainerComponent The container component used when a ListItemSecondaryAction is rendered.
    *                            Default: "li"
    * @param ContainerProps Properties applied to the container element when the component is used to
    *                        display a ListItemSecondaryAction
    * @param dense If true, compact vertical padding designed for keyboard and mouse input will be used.
    *               Default: false
    * @param disabled If true, the list item will be disabled.
    *                  Default: false
    * @param disableGutters If true, the left and right padding is removed.
    *                        Default: false
    * @param divider If true, a 1px light border is added to the bottom of the list item.
    *                 Default: false
    * @param selected Use to apply selected styling.
    *                  Default: false
    * @param id the id attribute value of the element
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    * @param onClick the click handler
    * @param children
    */
  def apply(
      alignItems: js.UndefOr[AlignItem] = js.undefined,
      autoFocus: js.UndefOr[Boolean] = js.undefined,
      button: js.UndefOr[Boolean] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      ContainerComponent: js.UndefOr[String] = js.undefined,
      ContainerProps: js.UndefOr[js.Object] = js.undefined,
      dense: js.UndefOr[Boolean] = js.undefined,
      disabled: js.UndefOr[Boolean] = js.undefined,
      disableGutters: js.UndefOr[Boolean] = js.undefined,
      divider: js.UndefOr[Boolean] = js.undefined,
      selected: js.UndefOr[Boolean] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      onClick: js.UndefOr[(ReactEvent) => Unit] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: ListItemProps = ListItemProps(
      alignItems = alignItems,
      autoFocus = autoFocus,
      button = button,
      classes = classes,
      component = component,
      ContainerComponent = ContainerComponent,
      ContainerProps = ContainerProps,
      dense = dense,
      disabled = disabled,
      disableGutters = disableGutters,
      divider = divider,
      selected = selected,
      id = id,
      className = className,
      onClick = onClick,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
