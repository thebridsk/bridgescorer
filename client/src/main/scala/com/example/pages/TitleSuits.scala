package com.example.pages

import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import japgolly.scalajs.react.vdom.HtmlStyles
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import BaseStyles._

object TitleSuits {

  def suits = {
    MuiTypography(
        variant = TextVariant.h6,
        color = TextColor.inherit,
        classes = js.Dictionary( "root" -> "homePageTitle")
    )(
        <.span(
          <.span(
              HtmlStyles.color.black,
              rootStyles.headerSuitSize,
              ^.dangerouslySetInnerHtml := " &spades;"
          ),
          <.span(
              HtmlStyles.color.red,
              rootStyles.headerSuitSize,
              ^.dangerouslySetInnerHtml := " &hearts;"
          ),
          <.span(
              HtmlStyles.color.red,
              rootStyles.headerSuitSize,
              ^.dangerouslySetInnerHtml := " &diams;"
          ),
          <.span(
              HtmlStyles.color.black,
              rootStyles.headerSuitSize,
              ^.dangerouslySetInnerHtml := " &clubs;"
          ),
        )
    ),

  }
}
