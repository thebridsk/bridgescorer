package com.github.thebridsk.bridge.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.pages.BaseStyles
import com.github.thebridsk.bridge.pages.BaseStylesImplicits

object HandStyles {
  import BaseStylesImplicits._

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles

  val handStyles = new HandStyles

  /**
   * Returns a TagMod with the selected classnames in the classname attribute.  If none are selected, then
   * normal classname is returned.
   * @param selected
   * @param required
   * @param requiredNotNext
   */
  def highlight(
      selected: Boolean = false,
      required: Boolean = false,
      requiredNotNext: Boolean = false
  ) = {
    val styles =
      selected.toList(handStyles.buttonSelected):::
      required.toList(handStyles.required):::
      requiredNotNext.toList(handStyles.requiredNotNext):::
      Nil

    if (styles.isEmpty) baseStyles.normal
    else styles.toTagMod
  }

}

class HandStyles {
  import BaseStyles._

  val required = baseStyles.required
  val requiredNotNext = baseStyles.requiredNotNext
  val buttonSelected = baseStyles.buttonSelected
  val notVisible = baseStyles.notVisible

  val footerButton = cls("handFooterButton")

  val pageHand = cls("handPageHand")

  val sectionContract = cls("handSectionContract")
  val viewContractTricks = cls("handViewContractTricks")
  val contractTricksButton0 = cls("handContractTricksButton0")

  val viewContractSuit = cls("handViewContractSuit")
  val suitInButton = cls("handSuitInButton")
  val viewContractDoubled = cls("handViewContractDoubled")

  val sectionHeader = cls("handSectionHeader")
  val viewDealer = cls("handViewDealer")
  val viewDeclarer = cls("handViewDeclarer")
  val viewTableBoard = cls("handViewTableBoard")
  val vulnerable = cls("handVulnerable")
  val notVulnerable = cls("handNotVulnerable")

  val sectionResult = cls("handSectionResult")
  val sectionResultInner = cls("handSectionResultInner")
  val viewHonors = cls("handViewHonors")
  val viewMadeOrDown = cls("handViewMadeOrDown")
  val viewTricks = cls("handViewTricks")

  val viewVulnerability = cls("handViewVulnerability")

  val sectionScore = cls("handSectionScore")

  val titleDeclarerDup = cls("titleDeclarerDup")
  val contractAndResult = cls("contractAndResult")

  val playDuplicate = cls("playDuplicate")
  val playChicago = cls("playChicago")
  val playRubber = cls("playRubber")

}

