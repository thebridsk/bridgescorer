package com.github.thebridsk.bridge.client.pages.duplicate

import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import japgolly.scalajs.react.vdom.TagMod

object DuplicateStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles
  def tableStyles = BaseStyles.tableStyles

  val dupStyles = new DuplicateStyles

}

class DuplicateStyles {
  import BaseStyles._

  val divSummary: TagMod = cls("dupDivSummary")

  val divPageStats: TagMod = cls("dupDivPageStats")

  val divPlayerFilter: TagMod = cls("dupDivPlayerFilter")

  val divPairsGrid: TagMod = cls("dupDivPairsGrid")

  val divPlayerOpponentGrid: TagMod = cls("dupDivPlayerOpponentGrid")

  val viewPairsDetailsTable: TagMod = cls("dupViewPairsDetailsTable")

  val viewPeopleDetailsTable: TagMod = cls("dupViewPeopleDetailsTable")

  val viewPairsTable: TagMod = cls("dupViewPairsTable")

  val viewPeopleTable: TagMod = cls("dupViewPeopleTable")

  val divPairsDetailsGrid: TagMod = cls("dupDivPairsDetailsGrid")

  val tablePairsDetailsGrid: TagMod = cls("dupTablePairsDetailsGrid")

  val viewPlayerAggressiveness: TagMod = cls("dupViewPlayerAggressiveness")

  val viewPlayerContractResults: TagMod = cls("dupViewPlayerContractResults")

  val viewContractResults: TagMod = cls("dupViewContractResults")

  val viewPlayerPlaces: TagMod = cls("dupViewPlayerPlaces")

  val divNewDuplicate: TagMod = cls("dupDivNewDuplicate")

  val divSelectMatch: TagMod = cls("dupDivSelectMatch")

  val tableNewDuplicate: TagMod = cls("dupTableNewDuplicate")

  val divScoreboardPage: TagMod = cls("dupDivScoreboardPage")
  val divViewScoreboard: TagMod = cls("dupDivViewScoreboard")
  val divViewScoreboardAllButtons: TagMod = cls(
    "dupDivViewScoreboardAllButtons"
  )

  val tableViewScoreboard: TagMod = cls("dupTableViewScoreboard")
  val cellScoreboardBoardColumn: TagMod = cls("dupCellScoreboardBoardColumn")

  val divScoreboardHelp: TagMod = cls("dupDivScoreboardHelp")
  val divScoreboardDetails: TagMod = cls("dupDivScoreboardDetails")
  val divPlayerPosition: TagMod = cls("dupDivPlayerPosition")
  val tablePlayerPosition: TagMod = cls("dupTablePlayerPosition")

  val divAllTablesPage: TagMod = cls("dupDivAllTablesPage")
  val divTablePage: TagMod = cls("dupDivTablePage")
  val divTableHelp: TagMod = cls("dupDivTableHelp")
  val divTableView: TagMod = cls("dupDivTableView")
  val boardButtonInTable: TagMod = cls("dupBoardButtonInTable")

  val divBoardSetPage: TagMod = cls("dupDivBoardSetPage")
  val divBoardPage: TagMod = cls("dupDivBoardPage")
  val divBoardView: TagMod = cls("dupDivBoardView")
  val tableCellGray: TagMod = cls("dupTableCellGray")
  val divAllBoardsPage: TagMod = cls("dupDivAllBoardsPage")

  val divFinishedScoreboardsPage: TagMod = cls("dupDivFinishedScoreboardsPage")

  val divBoardSetsPage: TagMod = cls("dupDivBoardSetsPage")
  val divBoardSetView: TagMod = cls("dupDivBoardSetView")

  val divMovementsPage: TagMod = cls("dupDivMovementsPage")
  val divMovementView: TagMod = cls("dupDivMovementView")

  val divNamesPage: TagMod = cls("dupDivNamesPage")

  val divTableNamesPage: TagMod = cls("dupDivTableNamesPage")
  val inputTableNames: TagMod = cls("dupInputTableNames")

  val divDuplicateResultPage: TagMod = cls("dupDivDuplicateResultPage")
  val divDuplicateResultEditPage: TagMod = cls("dupDivDuplicateResultEditPage")

  val divSuggestionPage: TagMod = cls("dupDivSuggestionPage")

  val divEditBoardSet: TagMod = cls("dupDivEditBoardSet")
  val divEditMovement: TagMod = cls("dupDivEditMovement")
}
