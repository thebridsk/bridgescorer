package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.example.pages.BaseStyles
import japgolly.scalajs.react.vdom.HtmlStyles

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * AppButton( AppButton.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ColorBar {
  import ColorBarInternal._

  case class Props( hue1: Double, minLightness1: Double, hue2: Double, minLightness2: Double, n: Int )

  def apply( hue1: Double, minLightness1: Double, hue2: Double, minLightness2: Double, n: Int ) =
    component(Props(hue1,minLightness1,hue2,minLightness2,n))

}

object ColorBarInternal {
  import ColorBar._

  private def box( hue: Double, sat: Double, lightness: Double ) = {
    <.div(
      ^.flex := "0 0 auto",
      ^.width := "20px",
      ^.height := "20px",
      ^.background := s"${HSLColor( hue, sat, lightness ).toHex}"
    )
  }

  val component = ScalaComponent.builder[Props]("ColorBar")
                            .stateless
                            .noBackend
                            .render_P( props => {
                              import BaseStyles._
                              val step1 = (1.0 - props.minLightness1)/(props.n-1)
                              val step2 = -(1.0 - props.minLightness2)/(props.n-1)

                              <.div(
                                ^.display := "flex",
                                ^.flexDirection := "row",
                                ^.flexWrap := "nowrap",
                                ^.justifyContent := "center",
                                ^.border := "none",
                                (props.minLightness1 until 1.0 by step1).map { l =>
                                  box( props.hue1, 1.0, l )
                                }.toTagMod,
                                box( props.hue1, 1.0, 1.0 ),
                                ((1.0-step1) to props.minLightness2 by step2).map { l =>
                                  box( props.hue2, 1.0, l )
                                }.toTagMod,
                              )
                            })
                            .build
}
