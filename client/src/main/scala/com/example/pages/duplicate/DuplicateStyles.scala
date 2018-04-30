package com.example.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.example.pages.BaseStyles

object DuplicateStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles
  def tableStyles = BaseStyles.tableStyles

  val dupStyles = new DuplicateStyles

}

class DuplicateStyles {
  import BaseStyles._

  val divSummary = cls("dupDivSummary")

  val divPageStats = cls("dupDivPageStats" )

  val divPlayerFilter = cls("dupDivPlayerFilter")

  val divPairsGrid = cls("dupDivPairsGrid")

  val viewPairsDetailsTable = cls("dupViewPairsDetailsTable")

  val viewPeopleDetailsTable = cls("dupViewPeopleDetailsTable")

  val viewPairsTable = cls("dupViewPairsTable")

  val viewPeopleTable = cls("dupViewPeopleTable")

  val divPairsDetailsGrid = cls("dupDivPairsDetailsGrid")

  val tablePairsDetailsGrid = cls("dupTablePairsDetailsGrid")

  val viewPlayerContractResults = cls("dupViewPlayerContractResults")

  val viewContractResults = cls("dupViewContractResults")

  val divNewDuplicate = cls("dupDivNewDuplicate")

  val divSelectMatch = cls("dupDivSelectMatch")

  val tableNewDuplicate = cls("dupTableNewDuplicate")

  val divScoreboardPage = cls("dupDivScoreboardPage")
  val divViewScoreboard = cls("dupDivViewScoreboard")
  val tableViewScoreboard = cls("dupTableViewScoreboard")
  val cellScoreboardBoardColumn = cls("dupCellScoreboardBoardColumn")

  val divScoreboardHelp = cls("dupDivScoreboardHelp")
  val divScoreboardDetails = cls("dupDivScoreboardDetails")
  val divPlayerPosition = cls("dupDivPlayerPosition")
  val tablePlayerPosition = cls("dupTablePlayerPosition")

  val divAllTablesPage = cls("dupDivAllTablesPage")
  val divTablePage = cls("dupDivTablePage")
  val divTableHelp = cls("dupDivTableHelp")
  val divTableView = cls("dupDivTableView")
  val boardButtonInTable = cls("dupBoardButtonInTable")

  val divBoardPage = cls("dupDivBoardPage")
  val divBoardView = cls("dupDivBoardView")
  val tableCellGray = cls("dupTableCellGray")
  val divAllBoardsPage = cls("dupDivAllBoardsPage")

  val divFinishedScoreboardsPage = cls("dupDivFinishedScoreboardsPage")

  val divBoardSetsPage = cls("dupDivBoardSetsPage")
  val divBoardSetView = cls("dupDivBoardSetView")

  val divMovementsPage = cls("dupDivMovementsPage")

  val divNamesPage = cls("dupDivNamesPage")

  val divTableNamesPage = cls("dupDivTableNamesPage")
  val inputTableNames = cls("dupInputTableNames")

  val divDuplicateResultPage = cls("dupDivDuplicateResultPage")
  val divDuplicateResultEditPage = cls("dupDivDuplicateResultEditPage")

  val divSuggestionPage = cls("dupDivSuggestionPage")

}
