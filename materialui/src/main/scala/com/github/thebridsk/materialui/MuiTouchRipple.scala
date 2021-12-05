package com.github.thebridsk.materialui

import scala.scalajs.js
@js.native
trait TouchRippleProps extends AdditionalProps {

  /**
    * If true, the ripple starts at the center of the
    * component rather than at the point of interaction.
    */
  val center: js.UndefOr[Boolean] = js.native

  /**
    * Override or extend the styles applied to the
    * component. See CSS API below for more details.
    */
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
}

object TouchRippleProps extends PropsFactory[TouchRippleProps] {

  /**
    * create a TouchRipple object
    *
    * @param center If true, the ripple starts at the center of the
    *               component rather than at the point of interaction.
    * @param classes Override or extend the styles applied to the
    *                component. See CSS API below for more details.
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: TouchRippleProps](
      props: js.UndefOr[P] = js.undefined,
      center: js.UndefOr[Boolean] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    center.foreach(p.updateDynamic("center")(_))
    classes.foreach(p.updateDynamic("classes")(_))

    p
  }
}

// object MuiTouchRipple extends ComponentFactory[TouchRippleProps] {
//   @js.native @JSImport("@mui/material/TouchRipple", JSImport.Default) private object TouchRipple
//       extends js.Any

//   protected val f =
//     JsComponent[TouchRippleProps, Children.Varargs, Null](TouchRipple)

//   /**
//     *
//     * @param center If true, the ripple starts at the center of the
//     *               component rather than at the point of interaction.
//     * @param classes Override or extend the styles applied to the
//     *                component. See CSS API below for more details.
//     * @param additionalProps a dictionary of additional properties
//     */
//   def apply(
//       center: js.UndefOr[Boolean] = js.undefined,
//       classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
//       additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
//   )(
//       children: CtorType.ChildArg*
//   ) = {
//     val p: TouchRippleProps = TouchRippleProps(
//       center = center,
//       classes = classes,
//       additionalProps = additionalProps
//     )
//     val x = f(p) _
//     x(children)
//   }
// }
