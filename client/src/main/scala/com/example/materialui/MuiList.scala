package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait ListProps extends AdditionalProps {
    var classes: js.UndefOr[js.Object] = js.undefined
    var component: js.UndefOr[String] = js.undefined
    var dense: js.UndefOr[Boolean] = js.undefined
    var disablePadding: js.UndefOr[Boolean] = js.undefined
    var subheader: js.UndefOr[js.Object] = js.undefined

}

object ListProps extends PropsFactory[ListProps] {

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
    def apply[P <: ListProps](
        props: js.UndefOr[P] = js.undefined,
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        dense: js.UndefOr[Boolean] = js.undefined,
        disablePadding: js.UndefOr[Boolean] = js.undefined,
        subheader: js.UndefOr[js.Object] = js.undefined,

        additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
    ): P = {
      val p = get(props,additionalProps)

      classes.foreach( p.updateDynamic("classes")(_))
      component.foreach( p.updateDynamic("component")(_))
      dense.foreach( p.updateDynamic("dense")(_))
      disablePadding.foreach( p.updateDynamic("disablePadding")(_))
      subheader.foreach( p.updateDynamic("subheader")(_))

      p
    }

}


object MuiList extends ComponentFactory[ListProps] {
    @js.native @JSImport("@material-ui/core/List", JSImport.Default) private object MList extends js.Any

    protected val f = JsComponent[ListProps, Children.Varargs, Null](MList)

    /**
     * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
     * @parem component The component used for the root node. Either a string to use a DOM element or a component.
     *                   By default, it's a li when button is false and a div when button is true
     * @param dense If true, compact vertical padding designed for keyboard and mouse input will be used.
     *               Default: false
     * @param disablePadding If true, vertical padding will be removed from the list.
     *                        Default: false
     * @param subheader The content of the subheader, normally ListSubheader.
     * @param additionalProps a dictionary of additional properties
     * @param children
     */
    def apply(
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        dense: js.UndefOr[Boolean] = js.undefined,
        disablePadding: js.UndefOr[Boolean] = js.undefined,
        subheader: js.UndefOr[js.Object] = js.undefined,

        additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p: ListProps = ListProps(
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
