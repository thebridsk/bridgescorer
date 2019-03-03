package com.example.pages.chicagos

import japgolly.scalajs.react.vdom.html_<^._
import com.example.pages.BaseStyles

object ChicagoStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles
  def tableStyles = BaseStyles.tableStyles

  val chiStyles = new ChicagoStyles

}

class ChicagoStyles {
  import BaseStyles._

  val chicagoListPage = cls("chiChicagoListPage")
  val chicagoSummaryPage = cls("chiChicagoSummaryPage")
  val chicagoFastSummaryPage = cls("chiChicagoFastSummaryPage")

  val divPageFive = cls("chiDivPageFive")
  val divPageSelectSittingOut = cls("chiDivPageSelectSittingOut")
  val divPageSelectPairs = cls("chiDivPageSelectPairs")
  val divPageSelectPos = cls("chiDivPageSelectPos")
  val viewPlayersVeryFirstRound = cls("chiViewPlayersVeryFirstRound")
  val viewPlayersSecondRound = cls("chiViewPlayersSecondRound")
  val viewPlayersThirdRound = cls("chiViewPlayersThirdRound")
  val viewPlayersFourthRound = cls("chiViewPlayersFourthRound")

  val divPageQuintet = cls("chiDivPageQuintet")
  val viewShowNewPosition = cls("chiViewShowNewPosition")

}
