package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.example.pages.BaseStyles
import japgolly.scalajs.react.vdom.HtmlStyles
import org.scalajs.dom.ext.Color

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
object FixedColorBar {
  import FixedColorBarInternal._

  case class Props( colors: List[Color],
                    titles: Option[List[String]] = None
                  )

  /**
   * FixedColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
   * No flyover text on the color bar.
   * @param hue1
   * @param minLightness1
   * @param hue2
   * @param minLightness2
   * @param n the number of boxes for hue1 and hue2
   */
  def apply(
      colors: List[Color],
      titles: Option[List[String]] = None
  ) = {
    component(Props(colors,titles))
  }

}

object FixedColorBarInternal {
  import FixedColorBar._
  import Utils._

  import BaseStyles._

  def bar( cols: List[Color], titles: Option[List[String]] ) = {
    val ts = titles.map( t => t.map( ti => Some(ti) ) ).getOrElse( cols.map( c => None ) )
    cols.zip( ts ).map { entry =>
      val (cc,t) = entry
      ColorBarInternal.box(cc,t)
    }.toTagMod
  }

  val component = ScalaComponent.builder[Props]("FixedColorBar")
                            .stateless
                            .noBackend
                            .render_P( props => {

                              <.div(
                                baseStyles.colorbar,
                                ^.display := "flex",
                                ^.flexDirection := "row",
                                ^.flexWrap := "nowrap",
                                ^.justifyContent := "center",
                                ^.border := "none",
                                bar( props.colors, props.titles ),
                              )
                            })
                            .build
}
