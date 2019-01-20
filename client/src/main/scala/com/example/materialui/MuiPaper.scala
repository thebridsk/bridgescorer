package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait PaperProps extends AdditionalProps {

  var classes: js.UndefOr[js.Object] = js.native
  var component: js.UndefOr[String] = js.native
  var elevation: js.UndefOr[Double] = js.native
  var square: js.UndefOr[Boolean] = js.native

}
object PaperProps extends PropsFactory[PaperProps] {

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
     * @param additionalProps a dictionary of additional properties
     */
    def apply[P <: PaperProps](
        props: js.UndefOr[P] = js.undefined,
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        elevation: js.UndefOr[Double] = js.undefined,
        square: js.UndefOr[Boolean] = js.undefined,

        additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
    ): P = {
      val p = get(props,additionalProps)
      classes.foreach( p.updateDynamic("classes")(_))
      component.foreach( p.updateDynamic("component")(_))
      elevation.foreach( p.updateDynamic("elevation")(_))
      square.foreach( p.updateDynamic("square")(_))

      p
    }
}

object MuiPaper extends ComponentFactory[PaperProps] {
    @js.native @JSImport("@material-ui/core/Paper", JSImport.Default) private object Paper extends js.Any

    protected val f = JsComponent[PaperProps, Children.Varargs, Null](Paper)

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
     * @param additionalProps a dictionary of additional properties
     */
    def apply(
        classes: js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        elevation: js.UndefOr[Double] = js.undefined,
        square: js.UndefOr[Boolean] = js.undefined,

        additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p: PaperProps = PaperProps(
                  classes = classes,
                  component = component,
                  elevation = elevation,
                  square = square,
                  additionalProps = additionalProps
              )
      val x = f(p) _
      x(children)
    }
}
