package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

class ToolbarVariant(val value: String) extends AnyVal
object ToolbarVariant {
  val regular = new Position("regular")
  val dense = new Position("dense")

  val values = List( regular, dense )
}

@js.native
trait ToolbarProps extends js.Object {
  val classes: js.UndefOr[js.Object] = js.native
  val disableGutters: js.UndefOr[Boolean] = js.native
  val variant: js.UndefOr[ToolbarVariant] = js.native
}
object ToolbarProps {

    /**
     * @param p the object that will become the properties object
     * @param classes Override or extend the styles applied to the component.
     *                 See CSS API below for more details.
     * @param disableGutters If true, disables gutter padding.
     *                        Default: false
     * @param variant The variant to use.
     *                Default: regular
     */
    def apply(
        p: js.Object with js.Dynamic = js.Dynamic.literal(),
        classes: js.UndefOr[js.Object] = js.undefined,
        disableGutters: js.UndefOr[Boolean] = js.undefined,
        variant: js.UndefOr[ToolbarVariant] = js.undefined
    ): ToolbarProps = {

      classes.foreach( p.updateDynamic("classes")(_))
      disableGutters.foreach( p.updateDynamic("disableGutters")(_))
      variant.foreach( v => p.updateDynamic("variant")(v.value))

      p.asInstanceOf[ToolbarProps]
    }
}

object MuiToolbar {
    @js.native @JSImport("@material-ui/core/Toolbar", JSImport.Default) private object Toolbar extends js.Any

    private val f = JsComponent[ToolbarProps, Children.Varargs, Null](Toolbar)

    /**
     * @param classes Override or extend the styles applied to the component.
     *                 See CSS API below for more details.
     * @param disableGutters If true, disables gutter padding.
     *                        Default: false
     * @param variant The variant to use.
     *                Default: regular
     */
    def apply(
        classes: js.UndefOr[js.Object] = js.undefined,
        disableGutters: js.UndefOr[Boolean] = js.undefined,
        variant: js.UndefOr[ToolbarVariant] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = ToolbarProps(
                  js.Dynamic.literal(),
                  classes,
                  disableGutters,
                  variant
              )
      val x = f(p) _
      x(children)
    }
}
