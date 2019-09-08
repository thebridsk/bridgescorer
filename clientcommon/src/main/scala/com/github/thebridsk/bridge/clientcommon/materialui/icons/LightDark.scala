package com.github.thebridsk.bridge.clientcommon.material.icons

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.svg_<^._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.clientcommon.pages.TitleSuits
import com.github.thebridsk.materialui.icons.MuiSvgIcon

object LightDark {
  import LightDarkInternal._

  case class Props()

  def apply(  ) = component(Props())

}

object LightDarkInternal {
  import LightDark._

  val component = ScalaComponent.builder[Props]("LightDark")
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
}
