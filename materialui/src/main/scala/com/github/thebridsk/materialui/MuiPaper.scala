package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait PaperProps extends AdditionalProps with StandardProps {

  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  val component: js.UndefOr[String] = js.native
  val elevation: js.UndefOr[Double] = js.native
  val square: js.UndefOr[Boolean] = js.native
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
    * @param className css class name to add to element
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: PaperProps](
      props: js.UndefOr[P] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      square: js.UndefOr[Boolean] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps, onClick)
    classes.foreach(p.updateDynamic("classes")(_))
    component.foreach(p.updateDynamic("component")(_))
    elevation.foreach(p.updateDynamic("elevation")(_))
    square.foreach(p.updateDynamic("square")(_))
    className.map(p.updateDynamic("className")(_))
    p
  }
}

object MuiPaper extends ComponentFactory[PaperProps] {
  @js.native @JSImport(
    "@material-ui/core/Paper",
    JSImport.Default
  ) private object Paper extends js.Any

  protected val f =
    JsComponent[PaperProps, Children.Varargs, Null](
      Paper
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

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
    * @param className css class name to add to element
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      square: js.UndefOr[Boolean] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: PaperProps = PaperProps(
      classes = classes,
      component = component,
      elevation = elevation,
      square = square,
      onClick = onClick,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
