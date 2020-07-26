package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.UndefOr

class Position(val value: String) extends AnyVal
object Position {
  val fixed = new Position("fixed")
  val absolute = new Position("absolute")
  val sticky = new Position("sticky")
  val static = new Position("static")
  val relative = new Position("relative")
  val values: List[Position] = List(fixed, absolute, sticky, static, relative)
}

@js.native
protected trait AppBarPropsPrivate extends js.Any {
  @JSName("color")
  val colorInternal: js.UndefOr[String] = js.native
  @JSName("position")
  val positionInternal: js.UndefOr[String] = js.native
}

@js.native
trait AppBarProps extends PaperProps with AppBarPropsPrivate {}
object AppBarProps extends PropsFactory[AppBarProps] {

  implicit class WrapAppBarProps(private val p: AppBarProps) extends AnyVal {
    def position: UndefOr[Position] = p.positionInternal.map(s => new Position(s))

//    def position_= (v: js.UndefOr[Position]): Unit = {
//      v.map{ vv=>p.positionInternal=vv.value; None }.
//        orElse{ p.positionInternal=js.undefined; None }
//    }

//    def position_= (v: Position) = { p.positionInternal = v.value }

    def color: UndefOr[ColorVariant] = p.colorInternal.map(s => new ColorVariant(s))

//    def color_= (v: js.UndefOr[ColorVariant]): Unit = {
//      v.map{ vv=>p.colorInternal=vv.value; None }.
//        orElse{ p.colorInternal=js.undefined; None }
//    }

  }

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
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: AppBarProps](
      props: js.UndefOr[P] = js.undefined,
      color: js.UndefOr[ColorVariant] = js.undefined,
      position: js.UndefOr[Position] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      square: js.UndefOr[Boolean] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): AppBarProps = {
    val p: P = PaperProps(
      props,
      classes = classes,
      component = component,
      elevation = elevation,
      square = square,
      className = className,
      additionalProps = additionalProps
    )

    color.foreach(v => p.updateDynamic("color")(v.value))
    position.foreach(v => p.updateDynamic("position")(v.value))

    p
  }
}

object MuiAppBar extends ComponentFactory[AppBarProps] {
  @js.native @JSImport("@material-ui/core/AppBar", JSImport.Default) private object AppBar
      extends js.Any

  protected val f = JsComponent[AppBarProps, Children.Varargs, Null](AppBar)  // scalafix:ok ExplicitResultTypes; ReactComponent

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
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      color: js.UndefOr[ColorVariant] = js.undefined,
      position: js.UndefOr[Position] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      square: js.UndefOr[Boolean] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {  // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: AppBarProps = AppBarProps(
      color = color,
      position = position,
      classes = classes,
      component = component,
      elevation = elevation,
      square = square,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
