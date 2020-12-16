package com.github.thebridsk.bridge.clientcommon.material.icons

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.svg_<^._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.clientcommon.react.PieChart

/**
  * An icon to display the Light/Dark mode of the page.
  *
  * This icon is typically used on a button that toggles between three different themes.
  * The [[nextTheme]] method is used to identify the next theme.
  *
  * Themes are:
  * - white - typically with a white background
  * - medium - typically with a gray background
  * - dark - typically with a black background
  *
  * The icon displays a piechart with three slices,
  * with the bottom slice indicating the current setting.
  *
  * The themes are defined in css.  The theme is selected by using the following css selector:
  * {{{
  * @media screen {
  *   [data-theme="medium"] {
  *     --color-bg: rgb(50,54,57);
  *   }
  * }
  * }}}
  *
  * Usage:
  * {{{
  * LightDark()
  * }}}
  */
object LightDark {
  import Internal._

  case class Props()

  /**
    * Instantiate the component
    *
    * @return the unmounted react component.
    */
  def apply() =
    component(Props()) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * The themes in toggle order.
    */
  val themes: List[String] = List("white", "medium", "dark")

  /**
    * TagMods that identify the colors to display.
    *
    * Colors are:
    * - [0] the previous theme background color
    * - [1] the current theme background color
    * - [2] the next theme background color
    */
  val colors: List[TagMod] = List(
    baseStyles.lightDarkIconPrev, // prev
    baseStyles.lightDarkIconCurrent, // current theme
    baseStyles.lightDarkIconNext // next
  )

  /**
    * Determine the next theme name
    *
    * @param current - the current theme name
    * @return the next theme name
    */
  def nextTheme(current: String): String = {
    val i = (themes.indexOf(current) + 1) % themes.length
    themes(i)
  }
  protected object Internal {

    private[icons] val component = ScalaComponent
      .builder[Props]("LightMediumDark")
      .stateless
      .noBackend
      .render_P(props =>
        PieChart.create(
          List(1, 1, 1),
          None,
          size = Some(24),
          sliceAttrs = Some(colors)
        )
      )
      .build

  }

}


