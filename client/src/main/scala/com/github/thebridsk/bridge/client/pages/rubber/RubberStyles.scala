package com.github.thebridsk.bridge.client.pages.rubber

import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import japgolly.scalajs.react.vdom.TagMod

object RubberStyles {

  def baseStyles = BaseStyles.baseStyles
  def rootStyles = BaseStyles.rootStyles
  def tableStyles = BaseStyles.tableStyles

  val rubStyles = new RubberStyles

}

class RubberStyles {
  import BaseStyles._

  val listPage: TagMod = cls("rubListPage")
  val namesPage: TagMod = cls("rubNamesPage")

  val tableNumber: TagMod = cls("rubTableNumber")
  val tableLabel: TagMod = cls("rubTableLabel")
  val tableGameLabel: TagMod = cls("rubTableGameLabel")
  val divRubberMatch: TagMod = cls("rubDivRubberMatch")
  val divRubberMatchView: TagMod = cls("rubDivRubberMatchView")
  val divDetailsView: TagMod = cls("rubDivDetailsView")
  val aboveLine: TagMod = cls("rubAboveLine")
  val belowLine: TagMod = cls("rubBelowLine")

}
