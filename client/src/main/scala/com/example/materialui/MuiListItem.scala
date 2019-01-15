package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

class AlignItem( val value: String ) extends AnyVal
object AlignItem {
  val flexStart = new AlignItem("flex-start")
  val center = new AlignItem("center")

  val values = List(flexStart,center)
}

object MuiListItem {
    @js.native @JSImport("@material-ui/core/ListItem", JSImport.Default) private object ListItem extends js.Any

    private val f = JsComponent[js.Object, Children.Varargs, Null](ListItem)

    /**
     * @param p the object that will become the properties object
     * @param alignItems Defines the align-items style property.
     *                    Default: center
     * @param button If true, the list item will be a button (using ButtonBase).
     *                Default: false
     * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
     * @parem component The component used for the root node. Either a string to use a DOM element or a component.
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
     */
    def set(
        p: js.Object with js.Dynamic,
        alignItems: js.UndefOr[AlignItem] = js.undefined,
        button: js.UndefOr[Boolean] = js.undefined,
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        ContainerComponent: js.UndefOr[String] = js.undefined,
        ContainerProps: js.UndefOr[js.Object] = js.undefined,
        dense: js.UndefOr[Boolean] = js.undefined,
        disabled: js.UndefOr[Boolean] = js.undefined,
        disableGutters: js.UndefOr[Boolean] = js.undefined,
        divider: js.UndefOr[Boolean] = js.undefined,
        selected: js.UndefOr[Boolean] = js.undefined,
        id: js.UndefOr[String] = js.undefined
    ): js.Object with js.Dynamic = {
      alignItems.foreach(v => p.updateDynamic("alignItems")(v.value))
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

      p
    }

    /**
     * @param alignItems Defines the align-items style property.
     *                    Default: center
     * @param button If true, the list item will be a button (using ButtonBase).
     *                Default: false
     * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
     * @parem component The component used for the root node. Either a string to use a DOM element or a component.
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
     * @param children
     */
    def apply(
        alignItems: js.UndefOr[AlignItem] = js.undefined,
        button: js.UndefOr[Boolean] = js.undefined,
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        ContainerComponent: js.UndefOr[String] = js.undefined,
        ContainerProps: js.UndefOr[js.Object] = js.undefined,
        dense: js.UndefOr[Boolean] = js.undefined,
        disabled: js.UndefOr[Boolean] = js.undefined,
        disableGutters: js.UndefOr[Boolean] = js.undefined,
        divider: js.UndefOr[Boolean] = js.undefined,
        selected: js.UndefOr[Boolean] = js.undefined,
        id: js.UndefOr[String] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = set(
                  js.Dynamic.literal(),
                  alignItems,
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
                  id
              )
      val x = f(p) _
      x(children)
    }
}
