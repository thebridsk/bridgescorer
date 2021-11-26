package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.color.Color
import com.github.thebridsk.color.Colors

/**
  * A Color bar component.
  *
  * A color bar contains 1 or more squares,
  * the colors in the square are in a sequence.
  *
  * The sequences can be created in the HSL space with a constant hue.
  * In this case there are two hues, one for the left the other for the right.
  *
  * To use, just code the following:
  *
  * {{{
  * ColorBar( AppButton.Props( ... ) )
  * }}}
  *
  * @author werewolf
  */
object ColorBar {
  import Internal._

  /**
    * Props for ColorBar
    * @constructor
    * @param leftColors list of colors for left, may be empty list
    * @param rightColors list of colors for right, may be empty list
    * @param middle the optional middle color
    * @param leftTitles the titles of the left boxes, if specified, must have leftColors.length titles.  None means no titles.
    * @param rightTitles the titles of the right boxes, if specified, must have rightColors.length titles.  None means no titles.
    * @param whiteTitle the title of the white box.  None means no title.
    */
  case class Props(
      leftColors: Seq[Color],
      rightColors: Seq[Color],
      middle: Option[Color],
      leftTitles: Option[Seq[TagMod]] = None,
      rightTitles: Option[Seq[TagMod]] = None,
      whiteTitle: Option[TagMod] = None
  ) {}

  object Props {

    /**
      * Create a props object for a color bar with the specified sequences defined by
      * the hue, min lightness, number of boxes, direction of sequence.
      * An optional middle box may also be specified.
      * Flyover titles may also be specified for each box.
      *
      * The first four parameters are for the left color boxes,
      * the second four are for the right color boxes.
      * @param hue1
      * @param minLightness1 the minimum lightness [0 - 100]
      * @param n1 the number of boxes for hue1
      * @param darkToLight1 left boxes should be dark to light if true.
      * @param hue2
      * @param minLightness2 the minimum lightness [0 - 100]
      * @param n2 the number of boxes for hue2
      * @param darkToLight2 right boxes should be dark to light if true.
      * @param middle the optional middle color
      * @param titles1 the titles of the left boxes, if specified, must have leftColors.length titles.  None means no titles.
      * @param titles2 the titles of the right boxes, if specified, must have rightColors.length titles.  None means no titles.
      * @param whiteTitle the title of the white box.  None means no title.
      *
      * @return the Props object.
      */
    def create(
        hue1: Double,
        minLightness1: Double,
        n1: Int,
        darkToLight1: Boolean,
        hue2: Double,
        minLightness2: Double,
        n2: Int,
        darkToLight2: Boolean,
        middle: Option[Color],
        titles1: Option[Seq[TagMod]] = None,
        titles2: Option[Seq[TagMod]] = None,
        whiteTitle: Option[TagMod] = None
    ): Props = {
      new Props(
        Colors.colorsExcludeEnd(hue1, minLightness1, n1, darkToLight1),
        Colors.colorsExcludeEnd(hue2, minLightness2, n2, darkToLight2),
        middle,
        titles1,
        titles2,
        whiteTitle
      )
    }
  }

  /**
    * ColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
    * No flyover text on the color bar.
    *
    * @param hue1 the hue on the left
    * @param minLightness1 the minimum lightness [0 - 100]
    * @param hue2 the hue on the right
    * @param minLightness2 the minimum lightness [0 - 100]
    * @param n the number of boxes for hue1 and hue2
    *
    * @return the Props object.
    */
  def apply(
      hue1: Double,
      minLightness1: Double,
      hue2: Double,
      minLightness2: Double,
      n: Int
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props.create(
        hue1,
        minLightness1,
        n,
        true,
        hue2,
        minLightness2,
        n,
        false,
        Some(Color.White),
        None,
        None,
        None
      )
    )
  }

  /**
    * ColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
    * Titles will show as flyover text.  No title for the middle white box.
    *
    * @param hue1 the hue on the left
    * @param minLightness1 the minimum lightness [0 - 100]
    * @param hue2 the hue on the right
    * @param minLightness2 the minimum lightness [0 - 100]
    * @param n the number of boxes for hue1 and hue2
    * @param titles1 the titles of the left boxes, must have n titles.
    * @param titles2 the titles of the right boxes, must have n titles.
    *
    * @return the Props object.
    */
  def apply(
      hue1: Double,
      minLightness1: Double,
      hue2: Double,
      minLightness2: Double,
      n: Int,
      titles1: Seq[TagMod],
      titles2: Seq[TagMod]
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props.create(
        hue1,
        minLightness1,
        n,
        true,
        hue2,
        minLightness2,
        n,
        false,
        Some(Color.White),
        Option(titles1),
        Option(titles2),
        None
      )
    )
  }

  /**
    * ColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
    * Titles will show as flyover text.
    *
    * @param hue1 the hue on the left
    * @param minLightness1 the minimum lightness [0 - 100]
    * @param hue2 the hue on the right
    * @param minLightness2 the minimum lightness [0 - 100]
    * @param n the number of boxes for hue1 and hue2
    * @param titles1 the titles of the left boxes, must have n titles.
    * @param titles2 the titles of the right boxes, must have n titles.
    * @param whiteTitle
    *
    * @return the Props object.
    */
  def apply(
      hue1: Double,
      minLightness1: Double,
      hue2: Double,
      minLightness2: Double,
      n: Int,
      titles1: Seq[TagMod],
      titles2: Seq[TagMod],
      whiteTitle: TagMod
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props.create(
        hue1,
        minLightness1,
        n,
        true,
        hue2,
        minLightness2,
        n,
        false,
        Some(Color.White),
        Option(titles1),
        Option(titles2),
        Option(whiteTitle)
      )
    )
  }

  /**
    * Create a color bar with the specified sequences defined by
    * the hue, min lightness, number of boxes, direction of sequence.
    * An optional middle box may also be specified.
    * Flyover titles may also be specified for each box.
    *
    * The first four parameters are for the left color boxes,
    * the second four are for the right color boxes.
    *
    * @param hue1 the hue on the left
    * @param minLightness1 the minimum lightness [0 - 100]
    * @param n1 the number of boxes for hue1
    * @param darkToLight1 left boxes should be dark to light if true.
    * @param hue2 the hue on the right
    * @param minLightness2 the minimum lightness [0 - 100]
    * @param n2 the number of boxes for hue2
    * @param darkToLight2 right boxes should be dark to light if true.
    * @param middle the optional middle color
    * @param titles1 the titles of the left boxes, if specified, must have leftColors.length titles.  None means no titles.
    * @param titles2 the titles of the right boxes, if specified, must have rightColors.length titles.  None means no titles.
    * @param whiteTitle the title of the white box.  None means no title.
    *
    * @return the react component.
    */
  def apply(
      hue1: Double,
      minLightness1: Double,
      n1: Int,
      darkToLight1: Boolean,
      hue2: Double,
      minLightness2: Double,
      n2: Int,
      darkToLight2: Boolean,
      middle: Option[Color],
      titles1: Option[Seq[TagMod]] = None,
      titles2: Option[Seq[TagMod]] = None,
      whiteTitle: Option[TagMod] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props.create(
        hue1,
        minLightness1,
        n1,
        darkToLight1,
        hue2,
        minLightness2,
        n2,
        darkToLight2,
        middle,
        titles1,
        titles2,
        whiteTitle
      )
    )
  }

  /**
    * A color bar with the specified colors on the left, middle, and right.
    *
    * @param leftColors list of colors for left of middle
    * @param rightColors list of colors for right of middle
    * @param middle the optional middle color
    * @param leftTitles the titles of the left boxes, if specified, must have leftColors.length titles.
    * @param rightTitles the titles of the right boxes, if specified, must have rightColors.length titles.
    * @param whiteTitle the title of the white box.
    *
    * @return the react component.
    */
  def create(
      leftColors: Seq[Color],
      rightColors: Seq[Color],
      middle: Option[Color],
      leftTitles: Option[Seq[TagMod]] = None,
      rightTitles: Option[Seq[TagMod]] = None,
      whiteTitle: Option[TagMod] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props(
        leftColors,
        rightColors,
        middle,
        leftTitles,
        rightTitles,
        whiteTitle
      )
    )
  }

  /**
    * Simple ColorBar with the specified colors and flyover titles.
    *
    * @param colors the colors for the squares.
    * @param titles optional, the titles for the squares.
    *
    * @return the react component.
    */
  def simple(
      colors: Seq[Color],
      titles: Option[Seq[TagMod]] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(colors, Nil, None, titles, None, None))
  }

  protected object Internal {

    import BaseStyles._
    import com.github.thebridsk.color.Colors._
    import ReactColor._

    private def box(color: Color, title: Option[TagMod]): VdomNode = {
      Tooltip(
        <.div(
          ^.flex := "0 0 auto",
          ^.width := "20px",
          ^.height := "20px",
          ^.backgroundColor := color
        ),
        title
      )
    }

    private def bar(
        hue: Double,
        minLightness: Double,
        n: Int,
        darkToLight: Boolean,
        titles: Option[List[TagMod]]
    ): TagMod = {
      val cols = colorsExcludeEnd(hue, minLightness, n)
      val c = if (darkToLight) cols else cols.reverse
      bar(c, titles)
    }

    private def bar(cols: Seq[Color], titles: Option[Seq[TagMod]]): TagMod = {
      val ts =
        titles.map(t => t.map(ti => Some(ti))).getOrElse(cols.map(cols => None))
      cols
        .zip(ts)
        .map { entry =>
          val (cc, t) = entry
          box(cc, t)
        }
        .toTagMod
    }

    private[react] val component = ScalaComponent
      .builder[Props]("ColorBar")
      .stateless
      .noBackend
      .render_P(props => {

        <.div(
          baseStyles.colorbar,
          ^.display := "flex",
          ^.flexDirection := "row",
          ^.flexWrap := "nowrap",
          ^.justifyContent := "center",
          ^.border := "none",
          bar(props.leftColors, props.leftTitles),
          props.middle.whenDefined(c => box(c, props.whiteTitle)),
          bar(props.rightColors, props.rightTitles)
        )
      })
      .build
  }

}
