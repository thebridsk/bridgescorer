package com.github.thebridsk.bridge.client.pages.rubber

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.pages.BaseStyles

object RubberStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles
  def tableStyles = BaseStyles.tableStyles

  val rubStyles = new RubberStyles

}

class RubberStyles {
  import BaseStyles._

  val listPage = cls("rubListPage")
  val namesPage = cls("rubNamesPage")

  val tableNumber = cls("rubTableNumber")
  val tableLabel = cls("rubTableLabel")
  val tableGameLabel = cls("rubTableGameLabel")
  val divRubberMatch = cls("rubDivRubberMatch")
  val divRubberMatchView = cls("rubDivRubberMatchView")
  val divDetailsView = cls("rubDivDetailsView")
  val aboveLine = cls("rubAboveLine")
  val belowLine = cls("rubBelowLine")

}
