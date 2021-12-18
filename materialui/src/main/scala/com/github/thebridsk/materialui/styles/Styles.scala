package com.github.thebridsk.materialui.styles

import scala.scalajs.js
import scala.scalajs.js.|
import com.github.thebridsk.materialui.AdditionalProps
import com.github.thebridsk.color.Color
import com.github.thebridsk.materialui.PropsFactory
import scala.language.implicitConversions
import japgolly.scalajs.react._

object Types {
  type PColor = String

}

import Types._

@js.native
trait PaletteColor extends AdditionalProps {
  var light: js.UndefOr[PColor] = js.native
  var main: PColor = js.native
  var dark: js.UndefOr[PColor] = js.native
  var contrastText: js.UndefOr[PColor] = js.native
}

object StyleImplicits {
  implicit def colorToPColor(color: Color): PColor = color.toRGBColor.toAttrValue
  implicit def colorToUPColor(color: Color): js.UndefOr[PColor] = color.toRGBColor.toAttrValue
  implicit def colorToUPCPColor(color: Color): js.UndefOr[PaletteColor|PColor] = color.toRGBColor.toAttrValue
  implicit def dictToUDict[T](dict: js.Dictionary[T]): js.UndefOr[js.Dictionary[js.Any]] = dict.asInstanceOf[js.UndefOr[js.Dictionary[js.Any]]]
  implicit def dictStringToUDict(dict: js.Dictionary[String]): js.UndefOr[js.Dictionary[js.Any]] = dict.asInstanceOf[js.UndefOr[js.Dictionary[js.Any]]]
}

object PaletteColor extends PropsFactory[PaletteColor] {

  def apply(
    main: PColor,
    light: js.UndefOr[PColor] = js.undefined,
    dark: js.UndefOr[PColor] = js.undefined,
    contrastText: js.UndefOr[PColor] = js.undefined
  ): PaletteColor = {
    val p = get[PaletteColor](props = js.undefined)
    p.updateDynamic("main")(main)
    light.foreach(v => p.updateDynamic("light")(v))
    dark.foreach(v => p.updateDynamic("dark")(v))
    contrastText.foreach(v => p.updateDynamic("contrastText")(v))
    p
  }

}

@js.native
trait PaletteTonalOffset extends AdditionalProps {
  var light: Double
  var dark: Double
}

@js.native
trait MColor extends AdditionalProps {
}

object MColor {
  implicit def toUndef(color: MColor): js.UndefOr[PaletteColor|MColor] = {
    color.asInstanceOf[js.UndefOr[PaletteColor|MColor]]
  }

}

@js.native
@js.annotation.JSImport("@mui/material/colors", js.annotation.JSImport.Default)
object MuiColor extends js.Any {
  val red: MColor = js.native
  val pink: MColor = js.native
  val purple: MColor = js.native
  val deepPurple: MColor = js.native
  val indigo: MColor = js.native
  val blue: MColor = js.native
  val lightBlue: MColor = js.native
  val cyan: MColor = js.native
  val teal: MColor = js.native
  val green: MColor = js.native
  val lightGreen: MColor = js.native
  val lime: MColor = js.native
  val yellow: MColor = js.native
  val amber: MColor = js.native
  val orange: MColor = js.native
  val deepOrange: MColor = js.native
  val brown: MColor = js.native
  val grey: MColor = js.native
  val blueGrey: MColor = js.native

}

@js.native
trait Palette extends AdditionalProps {
  var primary: js.UndefOr[PaletteColor|MColor] = js.native
  var secondary: js.UndefOr[PaletteColor|MColor] = js.native
  var error: js.UndefOr[PaletteColor|MColor] = js.native
  var warning: js.UndefOr[PaletteColor|MColor] = js.native
  var info: js.UndefOr[PaletteColor|MColor] = js.native
  var success: js.UndefOr[PaletteColor|MColor] = js.native
  var contrastThreshold: js.UndefOr[Double] = js.native
  var tonalOffset: js.UndefOr[Double|PaletteTonalOffset] = js.native
  var mode: js.UndefOr[String] = js.native
}

object Palette extends PropsFactory[Palette] {

  def apply(
    primary: js.UndefOr[PaletteColor|MColor] = js.undefined,
    secondary: js.UndefOr[PaletteColor|MColor] = js.undefined,
    error: js.UndefOr[PaletteColor|MColor] = js.undefined,
    warning: js.UndefOr[PaletteColor|MColor] = js.undefined,
    info: js.UndefOr[PaletteColor|MColor] = js.undefined,
    success: js.UndefOr[PaletteColor|MColor] = js.undefined,
    contrastThreshold: js.UndefOr[Double] = js.undefined,
    tonalOffset: js.UndefOr[Double|PaletteTonalOffset] = js.undefined,
    mode: js.UndefOr[String] = js.undefined,
    additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): Palette = {
    val p = get[Palette](props = js.undefined, additionalProps = additionalProps)
    primary.foreach(v => p.updateDynamic("primary")(v.asInstanceOf[js.Any]))
    secondary.foreach(v => p.updateDynamic("secondary")(v.asInstanceOf[js.Any]))
    error.foreach(v => p.updateDynamic("error")(v.asInstanceOf[js.Any]))
    warning.foreach(v => p.updateDynamic("warning")(v.asInstanceOf[js.Any]))
    info.foreach(v => p.updateDynamic("info")(v.asInstanceOf[js.Any]))
    success.foreach(v => p.updateDynamic("success")(v.asInstanceOf[js.Any]))
    tonalOffset.foreach(v => p.updateDynamic("tonalOffset")(v.asInstanceOf[js.Any]))
    contrastThreshold.foreach(v => p.updateDynamic("contrastThreshold")(v))
    mode.foreach(v => p.updateDynamic("mode")(v))
    p
  }

}

@js.native
trait Typography extends AdditionalProps

@js.native
trait Spacing extends AdditionalProps

@js.native
trait Breakpoints extends AdditionalProps

@js.native
trait ZIndex extends AdditionalProps

@js.native
trait Transitions extends AdditionalProps


@js.native
trait Components extends AdditionalProps

@js.native
trait Theme extends AdditionalProps {
  var palette: js.UndefOr[Palette] = js.native
  var typography: js.UndefOr[Typography] = js.native
  var spacing: js.UndefOr[Spacing] = js.native
  var breakpoints: js.UndefOr[Breakpoints] = js.native
  var zIndex: js.UndefOr[ZIndex] = js.native
  var transitions: js.UndefOr[Transitions] = js.native
  var components: js.UndefOr[Components] = js.native
}

object Theme extends PropsFactory[Theme] {

  def apply(
    palette: js.UndefOr[Palette] = js.undefined,
    typography: js.UndefOr[Typography] = js.undefined,
    spacing: js.UndefOr[Spacing] = js.undefined,
    breakpoints: js.UndefOr[Breakpoints] = js.undefined,
    zIndex: js.UndefOr[ZIndex] = js.undefined,
    transitions: js.UndefOr[Transitions] = js.undefined,
    components: js.UndefOr[Components] = js.undefined,
    additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): Theme = {
    val p = get[Theme](props = js.undefined, additionalProps = additionalProps)
    palette.foreach(v => p.updateDynamic("palette")(v))
    typography.foreach(v => p.updateDynamic("typography")(v))
    spacing.foreach(v => p.updateDynamic("spacing")(v))
    breakpoints.foreach(v => p.updateDynamic("breakpoints")(v))
    zIndex.foreach(v => p.updateDynamic("zIndex")(v))
    transitions.foreach(v => p.updateDynamic("transitions")(v))
    components.foreach(v => p.updateDynamic("components")(v))
    p
  }

}

object Styles {
  @js.native
  @js.annotation.JSImport("@mui/material/styles", "createTheme")
  def createTheme(theme: js.Object, args: js.UndefOr[js.Object]*): Theme = js.native
  @js.native
  @js.annotation.JSImport("@mui/material/styles", "styled")
  def styled(component: js.Object): Theme => js.Object = js.native
  @js.native
  @js.annotation.JSImport("@mui/material/styles", "useTheme")
  def useTheme(): Theme = js.native
  @js.native
  @js.annotation.JSImport("@mui/styles", "withTheme")
  def withTheme(component: js.Any): js.Object = js.native
  @js.native
  @js.annotation.JSImport("@mui/material/styles", "ThemeProvider")
  val ThemeProvider: js.Object = js.native
}

object ReactStyles {

  def withTheme[P <: js.Object, C <: Children, S <: js.Object](
    raw: P => facade.React.Element
  )(
    implicit ctorType1: CtorType.Summoner[P, C]
  ) = {

    val c = Styles.withTheme(raw)

    JsComponent[P, C, S](c)
  }

  def addTheme[P <: js.Object](p: P): P = {
    val d = p.asInstanceOf[js.Dynamic]
    val theme = Styles.useTheme()
    d.updateDynamic("theme")(theme)
    p
  }

  // def withTheme(
  //   raw: P => facade.React.Element
  // )(
  //   implicit ctorType1: CtorType.Summoner[P, C]
  // ) = {

  //   def fnComp: js.Function1[P, facade.React.Element] = { props =>
  //     val d = props.asInstanceOf[js.Dynamic]
  //     val theme = Styles.useTheme()
  //     d.updateDynamic("theme")(theme)
  //     raw(props)
  //   }

  //   // JsComponent[P, C, S](fnComp.asInstanceOf[js.Any])
  //   JsFnComponent[P, C](fnComp.asInstanceOf[js.Any])
  // }
}

// object ReactStyles {

//   def withTheme[P <: js.Object, C <: Children](
//     raw: P => VdomElement
//   )(
//       implicit ctorType1: CtorType.Summoner[P, C]
//   ) = {

//     def fnComp: js.Function1[P, VdomElement] = { props =>
//       val d = props.asInstanceOf[js.Dynamic]
//       val theme = Styles.useTheme()
//       d.updateDynamic("theme")(theme)
//       raw(props)
//     }

//     JsFnComponent.fromScala(fnComp)
//   }
// }

