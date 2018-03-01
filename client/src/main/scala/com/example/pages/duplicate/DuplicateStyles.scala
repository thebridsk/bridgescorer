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

  val spanTopButtons = cls("dupSpanTopButtons")

  val tableSummary = cls("dupTableSummary")

  val divPeopleSummary = cls("dupDivPeopleSummary")

  val tablePeopleSummary = cls("dupTablePeopleSummary")

  val divPlayerFilter = cls("dupDivPlayerFilter")

  val divPairsGrid = cls("dupDivPairsGrid")

  val tablePairsGrid = cls("dupTablePairsGrid")

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

  val divBoardSetPage = cls("dupDivBoardSetPage")
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
