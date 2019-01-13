package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

class Position(val value: String) extends AnyVal
object Position {
  val fixed = new Position("fixed")
  val absolute = new Position("absolute")
  val sticky = new Position("sticky")
  val static = new Position("static")
  val relative = new Position("relative")
  val values = List( fixed, absolute, sticky, static, relative )
}

@js.native
trait AppBarProps extends PaperProps {
  val color: js.UndefOr[ColorVariant] = js.native
  val position: js.UndefOr[Position] = js.native
}
object AppBarProps {

    /**
     * @param p the object that will become the properties object
     * @param color The color of the component. It supports those theme colors
     *               that make sense for this component.
     *               Default: primary
     * @param position The positioning type. The behavior of the different options
     *                  is described in the MDN web docs. Note: sticky is not
     *                  universally supported and will fall back to static when
     *                  unavailable.  Default: fixed
     * @param classes Override or extend the styles applied to the component.
     *                 See CSS API below for more details.
     * @param component The component used for the root node. Either a
     *                   string to use a DOM element or a component.
     *                   Default: div
     * @param elevation Shadow depth, corresponds to dp in the spec.
     *                   It's accepting values between 0 and 24 inclusive.
     *                   Default 2
     * @param square If true, rounded corners are disabled.
     *                Default: false
     */
    def apply(
        p: js.Object with js.Dynamic = js.Dynamic.literal(),
        color: js.UndefOr[ColorVariant] = js.undefined,
        position: js.UndefOr[Position] = js.undefined,

        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        elevation: js.UndefOr[Double] = js.undefined,
        square: js.UndefOr[Boolean] = js.undefined,
    ): AppBarProps = {
      val abp = PaperProps(p,classes,component,elevation,square)

      color.foreach( v => p.updateDynamic("color")(v.value))
      position.foreach( v => p.updateDynamic("position")(v.value))

      p.asInstanceOf[AppBarProps]
    }
}

object MuiAppBar {
    @js.native @JSImport("@material-ui/core/AppBar", JSImport.Default) private object AppBar extends js.Any

    private val f = JsComponent[AppBarProps, Children.Varargs, Null](AppBar)

    /**
     * @param color The color of the component. It supports those theme colors
     *               that make sense for this component.
     *               Default: primary
     * @param position The positioning type. The behavior of the different options
     *                  is described in the MDN web docs. Note: sticky is not
     *                  universally supported and will fall back to static when
     *                  unavailable.  Default: fixed
     * @param classes Override or extend the styles applied to the component.
     *                 See CSS API below for more details.
     * @param component The component used for the root node. Either a
     *                   string to use a DOM element or a component.
     *                   Default: div
     * @param elevation Shadow depth, corresponds to dp in the spec.
     *                   It's accepting values between 0 and 24 inclusive.
     *                   Default 2
     * @param square If true, rounded corners are disabled.
     *                Default: false
     */
    def apply(
        color: js.UndefOr[ColorVariant] = js.undefined,
        position: js.UndefOr[Position] = js.undefined,

        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        elevation: js.UndefOr[Double] = js.undefined,
        square: js.UndefOr[Boolean] = js.undefined,
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = AppBarProps(
                  color = color,
                  position = position,
                  classes = classes,
                  component = component,
                  elevation = elevation,
                  square = square,
              )
      val x = f(p) _
      x(children)
    }
}
