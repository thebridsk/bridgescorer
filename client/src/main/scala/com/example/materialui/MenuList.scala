package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

object MuiMenuList {
    @js.native @JSImport("@material-ui/core/MenuList", JSImport.Default) private object MenuList extends js.Any

    private val f = JsComponent[js.Object, Children.Varargs, Null](MenuList)

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
     */
    def set(
        p: js.Object with js.Dynamic,
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        dense: js.UndefOr[Boolean] = js.undefined,
        disablePadding: js.UndefOr[Boolean] = js.undefined,
        subheader: js.UndefOr[js.Object] = js.undefined
    ): js.Object with js.Dynamic = {
      MuiList.set(
          js.Dynamic.literal(),
          classes,
          component,
          dense,
          disablePadding,
          subheader,
      )
    }

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
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        dense: js.UndefOr[Boolean] = js.undefined,
        disablePadding: js.UndefOr[Boolean] = js.undefined,
        subheader: js.UndefOr[js.Object] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = set(
                  js.Dynamic.literal(),
                  classes,
                  component,
                  dense,
                  disablePadding,
                  subheader,
              )
      val x = f(p) _
      x(children)
    }
}
