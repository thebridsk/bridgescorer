package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait MenuItemProps extends ListItemProps {
}
object MenuItemProps extends PropsFactory[MenuItemProps] {

  /**
    * @param props the object that will become the properties object
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
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: MenuItemProps](
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
    val p: P = ListItemProps(
      props,
      alignItems,
      autoFocus,
      button,
      classes,
      component,
      ContainerComponent,
      ContainerProps,
      dense,
      disabled,
      disableGutters,
      divider,
      selected,
      id,
      className,
      onClick,
      additionalProps
    )

    onClick.map(p.updateDynamic("onClick")(_))

    p
  }
}

object MuiMenuItem extends ComponentFactory[MenuItemProps] {
  @js.native @JSImport(
    "@material-ui/core/MenuItem",
    JSImport.Default
  ) private object MenuItem extends js.Any

  protected val f =
    JsComponent[MenuItemProps, Children.Varargs, Null](
      MenuItem
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param props the object that will become the properties object
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
    * @param onClick
    * @param additionalProps a dictionary of additional properties
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
    val p: MenuItemProps = MenuItemProps(
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
      onClick = onClick,
      className = className,
      id = id,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
