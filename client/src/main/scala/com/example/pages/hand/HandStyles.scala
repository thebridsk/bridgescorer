package com.example.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import com.example.pages.BaseStyles

object HandStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles

  val handStyles = new HandStyles

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
}

