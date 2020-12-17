package com.github.thebridsk.bridge.clientcommon.pages

import japgolly.scalajs.react.vdom.html_<^._

object BaseStylesImplicits {

  implicit class BooleanWrapper(private val b: Boolean) extends AnyVal {
    def toOption[T](t: => T): Option[T] = if (b) Some(t) else None
    def toList[T](t: => T): List[T] = toOption(t).toList
  }

}

object BaseStyles {
  import BaseStylesImplicits._

  val baseStyles = new BaseStyles
  val baseStyles2 = new BaseStyles2
  val rootStyles = new RootStyles
  val rootStyles2 = new RootStyles2
  val tableStyles = new TableStyles

  def cls(clsname: String): TagMod = ^.className := clsname

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
      requiredNotNext: Boolean = false,
      color2: Boolean = false,
      color3: Boolean = false
  ): TagMod = {
    val styles =
      color2.toList(baseStyles.baseColor2) :::
        color3.toList(baseStyles.baseColor3) :::
        selected.toList(baseStyles.buttonSelected) :::
        requiredName.toList(baseStyles.requiredName) :::
        required.toList(baseStyles.required) :::
        requiredNotNext.toList(baseStyles.requiredNotNext) :::
        Nil

    if (styles.isEmpty) baseStyles.normal
    else styles.toTagMod
  }
}

class BaseStyles2 {
  import BaseStyles._

  val clsSelectPosition: TagMod = cls("selectPosition")
  val clsEnterNames: TagMod = cls("enterNames")
}

class BaseStyles {
  import BaseStyles._

  val fontTextLarge: TagMod = cls("baseFontTextLarge")
  val fontTextNormal: TagMod = cls("baseFontTextNormal")

  val baseColor2: TagMod = cls("baseColor2")
  val baseColor3: TagMod = cls("baseColor3")

  val defaultButton: TagMod = cls("baseDefaultButton baseFontTextLarge")
  val appButton: TagMod = cls(
    "baseAppButton baseDefaultButton baseFontTextLarge"
  )
  val nameButton: TagMod = cls("baseNameButton")

  val footerButton: TagMod = cls(
    "baseFooterButton baseDefaultButton baseFontTextLarge"
  )

  val appButton100: TagMod = cls(
    "baseAppButton100 baseAppButton baseDefaultButton baseFontTextLarge"
  )

  val baseButtonSelected = "baseButtonSelected"
  val buttonSelected: TagMod = cls(baseButtonSelected)

  val baseCheckbox = "baseCheckbox"
  val checkbox: TagMod = cls(baseCheckbox)
  val baseRadioButton = "baseRadioButton"
  val radioButton: TagMod = cls(baseRadioButton)

  val baseNormal = "baseNormal"
  val normal: TagMod = cls(baseNormal)

  val baseRequired = "baseRequired"
  val required: TagMod = cls(baseRequired)

  val baseRequiredName = "baseRequiredName"
  val requiredName: TagMod = cls(baseRequiredName)

  val baseRequiredNotNext = "baseRequiredNotNext"
  val requiredNotNext: TagMod = cls(baseRequiredNotNext)

  /**
    * to gray out the entire browser page for displaying a popup
    */
  val divPopupOverlay: TagMod = cls("baseDivPopupOverlay")

  /**
    * The div of the popup
    */
  val divPopup: TagMod = cls("baseDivPopup baseFontTextLarge")

  val divPopupOKCancelDiv: TagMod = cls(
    "baseDivPopupOKCancelDiv baseFontTextLarge"
  )

  val divPopupOKCancelBody: TagMod = cls("baseDivPopupOKCancelBody")

  val divFlexBreak: TagMod = cls("baseDivFlexBreak")

  val divFooter: TagMod = cls("baseDivFooter")

  val divFooterLeft: TagMod = cls("baseDivFooterLeft")

  val divFooterCenter: TagMod = cls("baseDivFooterCenter")

  val divFooterRight: TagMod = cls("baseDivFooterRight")

  val divTextFooter: TagMod = cls("baseDivFooterText baseFontTextSmall")

  val divText100: TagMod = cls("baseDivText100")

  val hideInPrint: TagMod = cls("baseHideInPrint")
  val onlyInPrint: TagMod = cls("baseOnlyInPrint")
  val hideInPortrait: TagMod = cls("baseHideInPortrait")
  val alwaysHide: TagMod = cls("baseAlwaysHide")
  val notVisible: TagMod = cls("baseNotVisible")
  val collapse: TagMod = cls("baseCollapse")

  val testPage: TagMod = cls("baseTestPage")

  val piechart: TagMod = cls("piechart")
  val piechartzero: TagMod = cls("piechartzero")

  val svgrect: TagMod = cls("svgrect")

  val colorbar: TagMod = cls("colorbar")

  val tableComponent: TagMod = cls("tableComponent")

  val withTooltipBox: TagMod = cls("withTooltipBox")
  val tooltipContent: TagMod = cls("tooltipContent")
  val tooltipTitle: TagMod = cls("tooltipTitle")
  val tooltipBody: TagMod = cls("tooltipBody")

  val divColorPage: TagMod = cls("divColorPage")

  val divGraphiql: TagMod = cls("divGraphiql")

  val divAppBar: TagMod = cls("divAppBar")

  val appBarTitle: TagMod = cls("appBarTitle")
  val appBarTitleWhenFullscreen: TagMod = cls("appBarTitleWhenFullscreen")

  val lightDarkIconCurrent: TagMod = cls("lightDarkIconCurrent")
  val lightDarkIconNext: TagMod = cls("lightDarkIconNext")
  val lightDarkIconPrev: TagMod = cls("lightDarkIconPrev")

  val comboboxLightDarkClass = "comboboxLightDark"
  val calendarLightDarkClass = "calendarLightDark"
}

class RootStyles2 {
  import BaseStyles._

  val defaultButton: TagMod = cls(
    "baseDefaultButton"
  )
  val clsSelectionButton: TagMod = cls(
    "baseFooterButton baseDefaultButton baseFontTextLarge"
  )

}

class RootStyles {
  import BaseStyles._

  val logDiv: TagMod = cls("rootLogDiv baseFontTextLarge")

  val homeDiv: TagMod = cls("rootHomeDiv baseFontTextLarge")

  val serverDiv: TagMod = cls("rootServerDiv")

  val gameDiv: TagMod = cls("rootGameDiv")

  val playButton: TagMod = cls(
    "rootPlayButton baseAppButton100 baseAppButton baseDefaultButton baseFontTextLarge"
  )

  val testHandsDiv: TagMod = cls("rootTestHandsDiv")

  val miscDiv: TagMod = cls("rootMiscDiv")

  val graphqlPageDiv: TagMod = cls("rootGraphQLPageDiv baseFontTextLarge")

  val aboutPageDiv: TagMod = cls("rootAboutPageDiv baseFontTextLarge")

  val importsListPageDiv: TagMod = cls("rootImportsListPageDiv")

  val exportPageDiv: TagMod = cls("rootExportPageDiv")

  val thankYouDiv: TagMod = cls("rootThankYouDiv baseFontTextLarge")

  val infoPageDiv: TagMod = cls("rootInfoPageDiv baseFontTextLarge")

  val headerSuitSize: TagMod = cls("headerSuitSize")
}

class TableStyles {
  import BaseStyles._

  val tableCellWidth1Of7: TagMod = cls("tableCellWidth1Of7")
  val tableCellWidth2Of7: TagMod = cls("tableCellWidth2Of7")
  val tableCellWidth3Of7: TagMod = cls("tableCellWidth3Of7")

  val tableFloatLeft: TagMod = cls("tableFloatLeft")
  val tableFloatRight: TagMod = cls("tableFloatRight")

  val tableWidthPage: TagMod = cls("tableWidthPage")

}
