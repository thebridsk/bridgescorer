package com.github.thebridsk.bridge.client.pages.individual.styles

import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import japgolly.scalajs.react.vdom.TagMod

object IndividualStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles
  def tableStyles = BaseStyles.tableStyles

  val dupStyles = new IndividualStyles

}

/**
  * Maps CSS style classnames to scala variables.
  */
class IndividualStyles {
  import BaseStyles._

  val pageSummary: TagMod = cls("pageSummary")

  val pageScoreboard: TagMod = cls("pageScoreboard")
  val viewScoreboard: TagMod = cls("viewScoreboard")
  val viewScoreboardHelp: TagMod = cls("viewScoreboardHelp")
  val viewScoreboardDetails: TagMod = cls("viewScoreboardDetails")
  val viewPlayerMatchResult: TagMod = cls("viewPlayerMatchResult")

  val pageNewDuplicate: TagMod = cls("pageNewDuplicate")

  val pageTable: TagMod = cls("pageTable")
  val viewTable: TagMod = cls("viewTable")
  val pageAllTables: TagMod = cls("pageAllTables")
  val pageTableNames: TagMod = cls("pageTableNames")

  val pageMovements: TagMod = cls("pageMovements")
}
