package com.github.thebridsk.bridge.clientapi.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.ColorBar
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.color.NamedColor
import com.github.thebridsk.bridge.clientcommon.color.Color
import com.github.thebridsk.bridge.clientcommon.color.Colors
import com.github.thebridsk.bridge.clientcommon.color.Gray
import com.github.thebridsk.bridge.clientcommon.color.RGBPercentColor
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ColorPage( ColorPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ColorPage {
  import ColorPageInternal._

  case class Props( router: BridgeRouter[AppPage])

  def apply( router: BridgeRouter[AppPage]) = component(Props(router))

}

object ColorPageInternal {
  import ColorPage._

  val log = Logger("bridge.ColorPage")

  val defaultColor1 = "rgb(255,0,0,100%)"
  val defaultColor2 = "hsl(240,100%,50%,100%)"

  /*
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  /**
   * @param n the number of boxes
   * @param minLightness the minimum l from hsl, 0 to 100
   * @param maxLightness the maximum l from hsl, 0 to 100
   * @param huestep the steps to take to get all the hue values.  0 to 360 by huestep
   * @param saturation the saturation, 0 to 100
   */
  case class State(
      n: String = "11",
      minLightness: String = "0.0",
      maxLightness: String = "100.0",
      huestep: String = "30",
      saturation: String = "100.0",

      color1: String = defaultColor1,
      color2: String = defaultColor2,
      n2: String = "11",
      ng: String = "11"
  ) {

    def withN( v: String ) = copy(n=v)
    def withHueStep( v: String ) = copy(huestep=v)
    def withMinLightness( v: String ) = copy(minLightness=v)
    def withMaxLightness( v: String ) = copy(maxLightness=v)
    def withSaturation( v: String ) = copy(saturation=v)

    def withColor1( v: String ) = copy(color1=v)
    def withColor2( v: String ) = copy(color2=v)
    def withN2( v: String ) = copy(n2=v)
    def withNG( v: String ) = copy(ng=v)
  }

  def parseInt( s: String, default: Int, min: Int = 0 ) = {
    try {
      val v = s.toInt
      Math.max(v,min)
    } catch {
      case x: Exception =>
        default
    }
  }

  def parseDouble( s: String, default: Double ) = {
    try {
      s.toDouble
    } catch {
      case x: Exception =>
        default
    }
  }

  def parseColor( s: String, default: String ) = {
    try {
      Color(s)
    } catch {
      case x: IllegalArgumentException =>
        Color(default)
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setSaturation( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withSaturation(s) }
    }

    def setHueStep( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withHueStep(s) }
    }

    def setN( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withN(s) }
    }

    def setN2( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withN2(s) }
    }

    def setNG( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withNG(s) }
    }

    def setColor1( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withColor1(s) }
    }

    def setColor2( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withColor2(s) }
    }

    def setMinLightness( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withMinLightness(s) }
    }

    def setMaxLightness( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withMaxLightness(s) }
    }

    def render( props: Props, state: State ) = {
      <.div(
        baseStyles.divColorPage,
        RootBridgeAppBar(
            Seq(MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      " Color",
                    )
                )
            ),
            None,
            props.router
        )(),
        <.div(
          <.div(
            <.ul(
              <.li(
                <.label(
                  "Hue step",
                  <.input( ^.`type`:="number",
                           ^.name:="Hue",
                           ^.onChange ==> setHueStep,
                           ^.value := state.huestep
                  )
                )
              ),
              <.li(
                <.label(
                  "N",
                  <.input( ^.`type`:="number",
                           ^.name:="N",
                           ^.onChange ==> setN,
                           ^.value := state.n
                  )
                )
              ),
              <.li(
                <.label(
                  "Saturation",
                  <.input( ^.`type`:="number",
                           ^.name:="Saturation",
                           ^.onChange ==> setSaturation,
                           ^.value := state.saturation
                  )
                )
              ),
              <.li(
                <.label(
                  "Min Lightness",
                  <.input( ^.`type`:="number",
                           ^.name:="minLightness",
                           ^.onChange ==> setMinLightness,
                           ^.value := state.minLightness
                  )
                )
              ),
              <.li(
                <.label(
                  "Max Lightness",
                  <.input( ^.`type`:="number",
                           ^.name:="minLightness",
                           ^.onChange ==> setMaxLightness,
                           ^.value := state.maxLightness
                  )
                )
              )
            ),
            <.div(
              <.table(
                <.thead(
                  <.tr(
                    <.th("Hue"),
                    <.th("Color bar")
                  )
                ),
                <.tbody(
                  (0 to 360 by parseInt(state.huestep,30)).map { hue =>
                    val colors = Colors.colors(
                                                hue,
                                                parseDouble(state.minLightness,0.0),
                                                parseInt(state.n,11),
                                                false,
                                                parseDouble(state.maxLightness,100.0),
                                                parseDouble(state.saturation,100)
                                              )

                    <.tr(
                      <.td(hue.toString()),
                      <.td(
                        ColorBar.simple(
                            colors,
                            Some( colors.map( c => TagMod( s"${c.toAttrValue}" ) ).toList )
                        )
                      )
                    )
                  }.toTagMod
                )
              )
            ),
          ),
          <.div(
            <.ul(
              <.li(
                <.label(
                  "N",
                  <.input( ^.`type`:="number",
                           ^.name:="N2",
                           ^.onChange ==> setN2,
                           ^.value := state.n2
                  )
                )
              ),
              <.li(
                <.label(
                  "Color1",
                  <.input( ^.`type`:="text",
                           ^.name:="Color1",
                           ^.onChange ==> setColor1,
                           ^.value := state.color1
                  )
                )
              ),
              <.li(
                <.label(
                  "Color2",
                  <.input( ^.`type`:="text",
                           ^.name:="Color2",
                           ^.onChange ==> setColor2,
                           ^.value := state.color2
                  )
                )
              )
            ),
            <.div(
              {
                val color1 = parseColor(state.color1, defaultColor1)
                val color2 = parseColor(state.color2, defaultColor2)
                val colorsRGB = Colors.colorsRGB( color1.toRGBPercentColor, color2.toRGBPercentColor, parseInt(state.n2,11) )
                val colorsHSL = Colors.colorsHSL( color1.toHSLColor, color2.toHSLColor, parseInt(state.n2,11) )
                TagMod(
                  <.table(
                    <.thead(
                      <.tr(
                        <.th("By"),
                        <.th("Color bar")
                      )
                    ),
                    <.tbody(
                      <.tr(
                        <.td("RGB"),
                        <.td(
                          {
                            ColorBar.simple(
                              colorsRGB,
                              Some( colorsRGB.map( c => TagMod( s"${c.toAttrValue}", <.br, s"${c.toHSLColor.toAttrValue}" ) ).toList )
                            )
                          }
                        )
                      ),
                      <.tr(
                        <.td("HSL"),
                        <.td(
                          {
                            ColorBar.simple(
                              colorsHSL,
                              Some( colorsHSL.map( c => TagMod( s"${c.toRGBPercentColor.toAttrValue}", <.br, s"${c.toAttrValue}" ) ).toList )
                            )
                          }
                        )
                      )
                    )
                  ),
                  <.table(
                    <.thead(
                      <.tr(
                        <.th( ^.rowSpan := 2, "N" ),
                        <.th( ^.colSpan := 2, "RGB" ),
                        <.th( ^.colSpan := 2, "HSL" ),
                      ),
                      <.tr(
                        <.th( "RGB ColorBar" ),
                        <.th( "HSL ColorBar" ),
                        <.th( "RGB ColorBar" ),
                        <.th( "HSL ColorBar" ),
                      )
                    ),
                    <.tbody(
                      colorsRGB.zip( colorsHSL ).zipWithIndex.map { case ((rgb,hsl),n) =>
                        <.tr(
                          <.td( s"${n+1}" ),
                          <.td(rgb.toAttrValue),
                          <.td(hsl.toRGBPercentColor.toAttrValue),
                          <.td(rgb.toHSLColor.toAttrValue),
                          <.td(hsl.toAttrValue),
                        )
                      }.toTagMod
                    )
                  )
                )
              }
            ),
          ),
          <.div(
            <.ul(
              <.li(
                <.label(
                  "N",
                  <.input( ^.`type`:="number",
                           ^.name:="NG",
                           ^.onChange ==> setNG,
                           ^.value := state.ng
                  )
                )
              )
            ),
            <.div(
              {
                val colors = (0 to 100 by 100/(parseInt(state.ng,11,2)-1)).map { v =>
                  Gray(v)
                }
                val titles = colors.map { g => TagMod(f"${g.gray}%.2f") }
                val colors2 = (0 to 100 by 100/(parseInt(state.ng,11,2)-1)).map { v =>
                  Color.grayscale(v)
                }
                val titles2 = colors2.map { g => TagMod(f"${g.r}%.2f") }
                TagMod(
                  ColorBar.simple(colors, Some(titles)),
                  ColorBar.simple(colors2, Some(titles2))
                )
              }
            )
          ),
          <.div(
            <.table(
              <.thead(
                <.tr(
                  <.th( "hue (deg)" ),
                  <.th( "saturation (%)" ),
                  <.th( ^.colSpan:=5, "lightness (%)" )
                )
              ),
              <.tbody(
                NamedColor.namedColors.toList.map { case (name,hex) =>
                  val rgb = Color(hex).toRGBColor
                  val hsl = rgb.toHSLColor
                  (name, rgb, hsl)
                }.groupBy { case (name, rgb, hsl) =>
                  if (hsl.hue<0) hsl.hue+360
                  else hsl.hue
                }.map { case (hue, list) =>
                  val bySat = list.groupBy { case (name, rgb, hsl) =>
                    f"${hsl.saturation}%.2f"
                  }.map { case (sat, satlist) =>
                    (sat, satlist.sortWith((l,r)=>l._3.lightness>r._3.lightness))
                  }.toList.sortBy( e => e._2.head._3.saturation )
                  (hue,bySat)
                }.toList.sortBy( e => e._1 ).map { case (hue, satmap) =>
                  TagMod(
                    <.tr(
                      <.td( f"$hue%.2fdeg" )
                    ),
                    satmap.map { case (sat, list) =>
                      <.tr(
                        <.td(),
                        <.td(sat+"%"),
                        list.map { case (name,rgb,hsl) =>
                          val colors = Color.named(name)::rgb::hsl::Nil
                          val titles = colors.map( c => TagMod( c.toAttrValue ) )
                          <.td(
                            f"${name} ${hsl.lightness}%.2f%%",
                            ColorBar.simple(colors,Some(titles))
                          )
                        }.toTagMod
                      )
                    }.toTagMod
                  )
                }.toTagMod
              )
            )
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ColorPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

