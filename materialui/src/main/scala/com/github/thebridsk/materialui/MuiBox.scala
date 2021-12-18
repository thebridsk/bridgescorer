package com.github.thebridsk.materialui

import scala.scalajs.js
import japgolly.scalajs.react._

@js.native
trait BoxProps extends AdditionalProps with StandardProps {
  val component: js.UndefOr[String] = js.native
}
object BoxProps extends PropsFactory[BoxProps] {

  /**
    * @param p the object that will become the properties object
    * @param component The component used for the root node. Either a
    *                   string to use a DOM element or a component.
    *                   Default: div
    * @param sx      The system prop that allows defining system overrides as well as additional CSS styles.
    * @param className css class name to add to element
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: BoxProps](
      props: js.UndefOr[P] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      sx: js.UndefOr[js.Dictionary[js.Any]] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps, onClick, sx)
    component.foreach(p.updateDynamic("component")(_))
    className.map(p.updateDynamic("className")(_))
    p
  }
}

object MuiBox extends ComponentFactory[BoxProps] {
  @js.native @js.annotation.JSImport(
    "@mui/material/Box",
    js.annotation.JSImport.Default
  ) private object Box extends js.Any

  protected val f =
    JsComponent[BoxProps, Children.Varargs, Null](
      Box
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param component The component used for the root node. Either a
    *                   string to use a DOM element or a component.
    *                   Default: div
    * @param sx      The system prop that allows defining system overrides as well as additional CSS styles.
    * @param className css class name to add to element
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      component: js.UndefOr[String] = js.undefined,
      sx: js.UndefOr[js.Dictionary[js.Any]] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: BoxProps = BoxProps(
      component = component,
      sx = sx,
      onClick = onClick,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
