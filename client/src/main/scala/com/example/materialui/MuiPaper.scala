package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait PaperProps extends js.Object {

  val classes: js.UndefOr[js.Object] = js.native
  val component: js.UndefOr[String] = js.native
  val elevation: js.UndefOr[Double] = js.native
  val square: js.UndefOr[Boolean] = js.native
}
object PaperProps {

    /**
     * @param p the object that will become the properties object
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
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        elevation: js.UndefOr[Double] = js.undefined,
        square: js.UndefOr[Boolean] = js.undefined,
    ): ModalProps = {
      classes.foreach(p.updateDynamic("classes")(_))
      component.foreach(p.updateDynamic("classes")(_))
      elevation.foreach(p.updateDynamic("classes")(_))
      square.foreach(p.updateDynamic("classes")(_))

      p.asInstanceOf[ModalProps]
    }
}

object MuiPaper {
    @js.native @JSImport("@material-ui/core/Paper", JSImport.Default) private object Paper extends js.Any

    private val f = JsComponent[js.Object, Children.Varargs, Null](Paper)

    /**
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
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        elevation: js.UndefOr[Double] = js.undefined,
        square: js.UndefOr[Boolean] = js.undefined,
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = PaperProps(
                  classes = classes,
                  component = component,
                  elevation = elevation,
                  square = square,
              )
      val x = f(p) _
      x(children)
    }
}
