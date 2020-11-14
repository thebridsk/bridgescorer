package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import japgolly.scalajs.react.vdom.TagMod

object ChicagoStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles
  def tableStyles = BaseStyles.tableStyles

  val chiStyles = new ChicagoStyles

}

class ChicagoStyles {
  import BaseStyles._

  val chicagoListPage: TagMod = cls("chiChicagoListPage")
  val chicagoSummaryPage: TagMod = cls("chiChicagoSummaryPage")
  val chicagoFastSummaryPage: TagMod = cls("chiChicagoFastSummaryPage")

  val divPageFive: TagMod = cls("chiDivPageFive")
  val divPageSelectSittingOut: TagMod = cls("chiDivPageSelectSittingOut")
  val divPageSelectPairs: TagMod = cls("chiDivPageSelectPairs")
  val divPageSelectPos: TagMod = cls("chiDivPageSelectPos")
  val viewPlayersVeryFirstRound: TagMod = cls("chiViewPlayersVeryFirstRound")
  val viewPlayersSecondRound: TagMod = cls("chiViewPlayersSecondRound")
  val viewPlayersThirdRound: TagMod = cls("chiViewPlayersThirdRound")
  val viewPlayersFourthRound: TagMod = cls("chiViewPlayersFourthRound")

  val divPageQuintet: TagMod = cls("chiDivPageQuintet")
  val viewShowNewPosition: TagMod = cls("chiViewShowNewPosition")

  val divEditNamesPage: TagMod = cls("chiDivEditNamesPage")
}
