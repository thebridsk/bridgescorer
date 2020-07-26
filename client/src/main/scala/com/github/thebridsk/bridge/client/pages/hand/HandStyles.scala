package com.github.thebridsk.bridge.client.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.pages.BaseStylesImplicits

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
  ): TagMod = {
    val styles =
      selected.toList(handStyles.buttonSelected):::
      required.toList(handStyles.required):::
      requiredNotNext.toList(handStyles.requiredNotNext):::
      Nil

    if (styles.isEmpty) baseStyles.normal
    else styles.toTagMod
  }

  /**
   * Returns the classname with the selected classnames in the classname attribute.  If none are selected, then
   * normal classname is returned.
   * @param selected
   * @param required
   * @param requiredNotNext
   */
  def highlightClass(
      selected: Boolean = false,
      required: Boolean = false,
      requiredNotNext: Boolean = false
  ): String = {
    val styles =
      selected.toList(baseStyles.baseButtonSelected):::
      required.toList(baseStyles.baseRequired):::
      requiredNotNext.toList(baseStyles.baseRequiredNotNext):::
      Nil

    if (styles.isEmpty) baseStyles.baseNormal
    else styles.mkString(" ")
  }

}

class HandStyles {
  import BaseStyles._

  val required = baseStyles.required
  val requiredNotNext = baseStyles.requiredNotNext
  val buttonSelected = baseStyles.buttonSelected
  val notVisible = baseStyles.notVisible

  val footerButton: TagMod = cls("handFooterButton")

  val pageHand: TagMod = cls("handPageHand")

  val sectionContract: TagMod = cls("handSectionContract")
  val viewContractTricks: TagMod = cls("handViewContractTricks")
  val contractTricksButton0: TagMod = cls("handContractTricksButton0")

  val viewContractSuit: TagMod = cls("handViewContractSuit")
  val suitInButton: TagMod = cls("handSuitInButton")
  val viewContractDoubled: TagMod = cls("handViewContractDoubled")

  val sectionHeader: TagMod = cls("handSectionHeader")
  val viewDealer: TagMod = cls("handViewDealer")
  val viewDeclarer: TagMod = cls("handViewDeclarer")
  val viewTableBoard: TagMod = cls("handViewTableBoard")
  val vulnerable: TagMod = cls("handVulnerable")
  val notVulnerable: TagMod = cls("handNotVulnerable")

  val sectionResult: TagMod = cls("handSectionResult")
  val sectionResultInner: TagMod = cls("handSectionResultInner")
  val viewHonors: TagMod = cls("handViewHonors")
  val viewMadeOrDown: TagMod = cls("handViewMadeOrDown")
  val viewTricks: TagMod = cls("handViewTricks")

  val viewVulnerability: TagMod = cls("handViewVulnerability")

  val sectionScore: TagMod = cls("handSectionScore")

  val titleDeclarerDup: TagMod = cls("titleDeclarerDup")
  val contractAndResult: TagMod = cls("contractAndResult")

  val playDuplicate: TagMod = cls("playDuplicate")
  val playChicago: TagMod = cls("playChicago")
  val playRubber: TagMod = cls("playRubber")

}

