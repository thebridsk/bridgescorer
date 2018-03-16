package com.example.pages

import japgolly.scalajs.react.vdom.html_<^._

object BaseStylesImplicits {

  implicit class BooleanWrapper( val b: Boolean ) extends AnyVal {
    def toOption[T]( t: =>T ) = if (b) Some(t) else None
    def toList[T]( t: =>T ) = toOption(t).toList
  }

}

object BaseStyles {
  import BaseStylesImplicits._

  val baseStyles = new BaseStyles
  val rootStyles = new RootStyles
  val tableStyles = new TableStyles

  def cls( clsname: String ) = ^.className:=clsname

  /**
   * Returns a TagMod with the selected classnames in the classname attribute.  If none are selected, then
   * normal classname is returned.
   * @param selected
   * @param requiredName
   * @param required
   * @param requiredNotNext
   */
  def highlight(
      selected: Boolean = false,
      requiredName: Boolean = false,
      required: Boolean = false,
      requiredNotNext: Boolean = false
  ) = {
    val styles =
      selected.toList(baseStyles.buttonSelected):::
      requiredName.toList(baseStyles.requiredName):::
      required.toList(baseStyles.required):::
      requiredNotNext.toList(baseStyles.requiredNotNext):::
      Nil

    if (styles.isEmpty) baseStyles.normal
    else styles.toTagMod
  }
}

class BaseStyles {
  import BaseStyles._

  val fontTextLarge = cls("baseFontTextLarge")
  val fontTextNormal = cls("baseFontTextNormal")

  val defaultButton = cls("baseDefaultButton baseFontTextLarge")
  val appButton = cls("baseAppButton baseDefaultButton baseFontTextLarge")
  val nameButton = cls("baseNameButton")

  val footerButton = cls("baseFooterButton baseDefaultButton baseFontTextLarge")

  val appButton100 = cls("baseAppButton100 baseAppButton baseDefaultButton baseFontTextLarge")

  val buttonSelected = cls("baseButtonSelected")

  val checkbox = cls("baseCheckbox")
  val radioButton = cls("baseRadioButton")

  val normal = cls("baseNormal")

  val required = cls("baseRequired")

  val requiredName = cls("baseRequiredName")

  val requiredNotNext = cls("baseRequiredNotNext")

  /**
   * to gray out the entire browser page for displaying a popup
   */
  val divPopupOverlay = cls("baseDivPopupOverlay")

  /**
   * The div of the popup
   */
  val divPopup = cls("baseDivPopup baseFontTextLarge")

  val divPopupOKCancelDiv = cls("baseDivPopupOKCancelDiv baseFontTextLarge")

  val divPopupOKCancelBody = cls("baseDivPopupOKCancelBody")

  val divFooter = cls("baseDivFooter")

  val divFooterLeft = cls("baseDivFooterLeft")

  val divFooterCenter = cls("baseDivFooterCenter")

  val divFooterRight = cls("baseDivFooterRight")

  val divTextFooter = cls("baseDivFooterText baseFontTextSmall")

  val divText100 = cls("baseDivText100")

  val hideInPrint = cls("baseHideInPrint")
  val onlyInPrint = cls("baseOnlyInPrint")
  val hideInPortrait = cls("baseHideInPortrait")
  val alwaysHide = cls("baseAlwaysHide")
  val notVisible = cls("baseNotVisible")

  val testPage = cls("baseTestPage")

  val hover = cls("titleHover")

  val piechart = cls("piechart")
}

class RootStyles {
  import BaseStyles._

  val homeDiv = cls("rootHomeDiv baseFontTextLarge")

  val serverDiv = cls("rootServerDiv")

  val gameDiv = cls("rootGameDiv")

  val playButton = cls("rootPlayButton baseAppButton100 baseAppButton baseDefaultButton baseFontTextLarge")

  val testHandsDiv = cls("rootTestHandsDiv")

  val miscDiv = cls("rootMiscDiv")

  val graphqlPageDiv = cls("rootGraphQLPageDiv baseFontTextLarge")

  val aboutPageDiv = cls("rootAboutPageDiv baseFontTextLarge")

  val importsListPageDiv = cls("rootImportsListPageDiv")

  val exportPageDiv = cls("rootExportPageDiv")

  val thankYouDiv = cls("rootThankYouDiv baseFontTextLarge")

  val infoPageDiv = cls("rootInfoPageDiv baseFontTextLarge")
}

class TableStyles {
  import BaseStyles._

  val tableCellWidth1Of7 = cls("tableCellWidth1Of7")
  val tableCellWidth2Of7 = cls("tableCellWidth2Of7")
  val tableCellWidth3Of7 = cls("tableCellWidth3Of7")

  val tableFloatLeft = cls("tableFloatLeft")
  val tableFloatRight = cls("tableFloatRight")

  val tableWidthPage = cls("tableWidthPage")

}
