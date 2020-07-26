package com.github.thebridsk.bridge.clientcommon.pages

import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import japgolly.scalajs.react.vdom.HtmlStyles
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import BaseStyles._

object TitleSuits {

  val suitspan =  // scalafix:ok ExplicitResultTypes; React
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

  val suits = {  // scalafix:ok ExplicitResultTypes; ReactComponent
    MuiTypography(
        variant = TextVariant.h6,
        color = TextColor.inherit,
        classes = js.Dictionary( "root" -> "homePageTitle")
    )(
      suitspan
    )
  }
}
