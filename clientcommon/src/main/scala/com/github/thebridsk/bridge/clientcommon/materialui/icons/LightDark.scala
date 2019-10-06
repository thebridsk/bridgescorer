package com.github.thebridsk.bridge.clientcommon.material.icons

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.svg_<^._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.clientcommon.pages.TitleSuits
import com.github.thebridsk.materialui.icons.MuiSvgIcon
import com.github.thebridsk.bridge.clientcommon.react.PieChart

object LightDark {
  import LightDarkInternal._

  case class Props()

  def apply(  ) = component(Props())

  val themes = List("white","medium","dark")

  val colors = List(
                     baseStyles.lightDarkIcon3,  // prev
                     baseStyles.lightDarkIcon1,  // current theme
                     baseStyles.lightDarkIcon2   // next
                   )

  def nextTheme( current: String ) = {
    val i = (themes.indexOf(current)+1)%themes.length
    themes(i)
  }

}

object LightDarkInternal {
  import LightDark._

  val componentold = ScalaComponent.builder[Props]("LightDark")
                            .stateless
                            .noBackend
                            .render_P( props =>
                              MuiSvgIcon(
                                viewBox = "-1.1 -1.1 2.2 2.2"
                              )(
                                <.path(
                                  baseStyles.lightDarkIcon1,
                                  ^.d := f"M 0 1" +
                                         f" A 1 1 0 1 1 0 -1" +
                                          " L 0 0"
                                ),
                                <.path(
                                  baseStyles.lightDarkIcon2,
                                  ^.d := f"M 0 -1" +
                                         f" A 1 1 0 1 1 0 1" +
                                          " L 0 0"
                                )

                              )
                            )
                            .build

  val component = ScalaComponent.builder[Props]("LightMediumDark")
                            .stateless
                            .noBackend
                            .render_P( props =>
                              PieChart.create(
                                List(1,1,1),
                                None,
                                size = Some(24),
                                sliceAttrs = Some( colors )
                              )
                            )
                            .build

}
