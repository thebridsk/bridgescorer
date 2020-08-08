package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsData
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairData
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByWonPct
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorBy
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsDataSummary
import com.github.thebridsk.bridge.data.duplicate.suggestion.Stat
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByWonPtsPct
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByPointsPct
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByPlayed
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.ColorBar
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationType
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationAsPlayed
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByIMP
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationMP
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationIMP
import com.github.thebridsk.bridge.clientcommon.react.Table
import com.github.thebridsk.bridge.clientcommon.react.SvgRect
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import com.github.thebridsk.bridge.clientcommon.react.Table.Column
import com.github.thebridsk.bridge.client.pages.HomePage

/**
  * Shows a pairs summary page.
  * Each match has a button that that shows that match, by going to the ScoreboardView(id) page.
  * There is also a button to create a new match, by going to the NewScoreboardView page.
  *
  * The data is obtained from the DuplicateStore object.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * ViewPairsGrid( routerCtl: BridgeRouter[DuplicatePage] )
  * </code></pre>
  *
  * @author werewolf
  */
object ViewPairsGrid {
  import ViewPairsGridInternal._

  case class Props(
      filter: ViewPlayerFilter.Filter,
      showNoDataMsg: Boolean = false
  ) {

    def getNames = filter.getNames
  }

  def apply(
      filter: ViewPlayerFilter.Filter,
      showNoDataMsg: Boolean = false
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(filter, showNoDataMsg))

}

object ViewPairsGridInternal {
  import ViewPairsGrid._
  import DuplicateStyles._

  val logger: Logger = Logger("bridge.ViewPairsGrid")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      calc: CalculationType,
      initialCalc: CalculationType,
      colorBy: ColorBy = ColorByWonPct,
      maxSize: Int = 60,
      minSize: Int = 5
  )

  /**
    * @param stat
    * @param n the number of steps
    * @return Tuple3( titlesBelow, titleMiddle, titlesAbove )
    */
  def stepTitles(stat: Stat, n: Int): (List[TagMod], TagMod, List[TagMod]) = {
    val min = stat.min
    val ave = stat.ave
    val max = stat.max

    val stepB = (ave - min) / n
    val below = (0 until n).map { i =>
      TagMod(f"${min + stepB * i}%.2f%%")
    }.toList

    val middle = TagMod(f"${ave}%.2f%%")

    val stepA = (max - ave) / n
    val above = (1 to n).map { i =>
      TagMod(f"${ave + stepA * i}%.2f%%")
    }.toList
    (below, middle, above)
  }

//  case class Data[+TColor](
//    size: Int,
//    color: List[TColor],
//    value: List[Double],
//    title: Option[String] = None,
//    size2: Int = 0,
//    showX: Boolean = false
//  )

  /**
    * @param pd
    * @param sizeSt the stat for determining size of bar
    * @param colorSt the stat for determining the color of the bar
    * @param colorAbove the hue of the color to use if pd is above average
    * @param colorBelow the hue of the color to use if pd is below average
    * @param state
    * @param sizeMultiplier multiply the size to make the bar bigger. (1 - player,player, 2 - total column)
    * @return a tuple2, the data and the title.
    */
  def getData(
      pd: PairData,
      sizeSt: Stat,
      colorSt: Stat,
      colorAbove: Double,
      colorBelow: Double,
      state: State,
      sizeMultiplier: Int
  ): (TagMod, String) = {
    val (bcolor, light) = colorSt.sizeAveAsFraction(pd)
    val lightness = (1 - light) * 75.0 + 25.0
    val size = sizeSt.size(
      pd,
      state.minSize * sizeMultiplier,
      state.maxSize * sizeMultiplier
    )
    if (size < 0) {
      logger.warning(s"""Negative size, sizeSt=${sizeSt}, pd=${pd}""")
    }
    val (color, scolor) = if (bcolor) {
      // above average, green
      (
        Color.hsl(colorAbove, 100, lightness),
        f"hsl(${colorAbove},100,${lightness}%.2f)"
      )
    } else {
      // below average, red
      (
        Color.hsl(colorBelow, 100, lightness),
        f"hsl(${colorBelow},100,${lightness}%.2f)"
      )
    }

//    val debug = f"""
//                   |Debug: bcolor=${bcolor}, light=${light}%.2f, lightness=${lightness}%.2f, scolor=${scolor}
//                   |pd=${pd}
//                   |sizeSt=${sizeSt}
//                   |colorSt=${colorSt}
//                   |""".stripMargin

    val title = f"""Played ${pd.played},
                   |Won ${pd.won + pd.wonImp} (${pd.winPercent}%.2f%%),
                   |WonPoints ${pd.wonPts + pd.wonImpPts}%.2f (${pd.winPtsPercent}%.2f%%)""".stripMargin
    val titleMP = if (state.calc != CalculationIMP) {
      f""",
         |Points ${pd.points}%.0f (${pd.pointsPercent}%.2f%%)""".stripMargin
    } else {
      ""
    }
    val titleIMP = if (state.calc != CalculationMP) {
      f""",
         |IMP ${pd.avgIMP}%.2f""".stripMargin
    } else {
      ""
    }

//    (DataBar( size, List( color ), List(1.0), None, None, 20 ), title+titleMP+titleIMP)
    (
      SvgRect(
        width = size,
        height = 20,
        borderColor = Color.Black,
        slices = List(1.0),
        colors = List(color),
        chartTitle = None
      ),
      title + titleMP + titleIMP // +debug
    )

  }

  val cellX: TagMod = TagMod("X")

  def getRows(
      players: List[String],
      pds: PairsData,
      summary: PairsDataSummary,
      statColor: Stat,
      statSize: Stat,
      statTotalColor: Stat,
      statTotalSize: Stat,
      state: State
  ): List[List[TagMod]] = {
    players.map { rowplayer =>
      val data = players.map { colPlayer =>
        val d = if (rowplayer == colPlayer) {
          cellX
        } else {
          pds.get(rowplayer, colPlayer) match {
            case Some(pd) if (statSize.valueInRange(pd)) =>
              val (bar, title) =
                getData(pd, statSize, statColor, 120, 0, state, 1)
              TagMod(
                Tooltip(
                  data = bar,
                  tooltipbody = <.div(title),
                  tooltiptitle = None
                )
              )
            case _ =>
              TagMod()
          }
        }
        d
      }
      val playerTotals = summary.playerTotals
        .get(rowplayer)
        .getOrElse(
          PairData(rowplayer, "", 0, 0, 0, 0, 0, 0, None, 0, 0, 0, 0, 0, 0)
        )

      val totalDataList = if (statTotalSize.valueInRange(playerTotals)) {
        val (totalData, totalTitle) = getData(
          playerTotals,
          statTotalSize,
          statTotalColor,
          240,
          60,
          state,
          2
        )

        List(
          TagMod(
            Tooltip(
              data = totalData,
              tooltipbody = <.div(totalTitle),
              tooltiptitle = None
            )
          )
        )
      } else {
        List(TagMod())
      }

      TagMod(rowplayer) :: data ::: totalDataList
    }
  }

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    def setColorBy(colorBy: ColorBy): Callback =
      scope.modState { s => s.copy(colorBy = colorBy) }

    def setCalc(calc: CalculationType): Callback =
      scope.modState { s =>
        val colorBy = calc match {
          case CalculationAsPlayed =>
            s.colorBy
          case CalculationMP =>
            if (s.colorBy == ColorByIMP) ColorByPointsPct
            else s.colorBy
          case CalculationIMP =>
            if (s.colorBy == ColorByPointsPct) ColorByIMP
            else s.colorBy
        }
        s.copy(colorBy = colorBy, calc = calc)
      }

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

      props.filter.pairsData match {
        case Some(rawpds) if !rawpds.players.isEmpty =>
          val pds = {
            if (rawpds.calc == state.calc) rawpds
            else PairsData(rawpds.pastgames, state.calc)
          }
          val filteredNames = props.filter.selected
          val summary = new PairsDataSummary(
            pds,
            state.colorBy,
            filteredNames,
            props.filter.filterDisplayOnly,
            ColorByPlayed
          )
          val allPlayers = summary.players.sorted
          val sortedPlayers =
            props.filter.selected.getOrElse(summary.playerFilter).sorted

          def getButton(colorBy: ColorBy, id: String, text: String) = {
            AppButton(
              id,
              text,
              ^.onClick --> setColorBy(colorBy),
              BaseStyles.highlight(selected = colorBy == state.colorBy)
            )
          }

          val statColor = summary.colorStat
          val statSize = summary.extraStats.head

          val statTotalColor = summary.colorStatPlayerTotals
          val statTotalSize = summary.extraStatsPlayerTotals.head

          val n = 10
          val (titlesBelow, titleWhite, titlesAbove) = stepTitles(statColor, n)
          val (titlesTBelow, titleTWhite, titlesTAbove) =
            stepTitles(statTotalColor, n)

          val shownSortedPlayers =
            sortedPlayers.filter(p => props.filter.isPlayerShown(p))

          val columns =
            Column("Player") :: sortedPlayers.map(p => Column(p)) ::: List(
              Column("Totals")
            )

          val rows = getRows(
            shownSortedPlayers,
            pds,
            summary,
            statColor,
            statSize,
            statTotalColor,
            statTotalSize,
            state
          )

          <.div(
            dupStyles.divPairsGrid,
            Table(
              columns = columns,
              rows = rows,
              header = None,
              footer = Some(
                TagMod(
                  <.tr(
                    <.td(
                      ^.colSpan := sortedPlayers.length + 1,
                      f"Average for ${state.colorBy.name} is ${summary.colorStat.ave}%.2f"
                    ),
                    <.td(f"${summary.colorStatPlayerTotals.ave}%.2f")
                  ),
                  <.tr(
                    <.td(
                      ^.colSpan := sortedPlayers.length + 2,
                      "The width of the box is relative to the number of times the pair has played together. ",
                      <.br,
                      "The color indicates how well, dark green is well above average, light green is above average, ",
                      <.br,
                      "white is average, ",
                      "light red is below average, dark red is well below average",
                      ColorBar(
                        0,
                        25.0,
                        120,
                        25.0,
                        n,
                        titlesBelow,
                        titlesAbove,
                        titleWhite
                      ),
                      "For Totals, the width is relative to the number of times the player has played",
                      <.br,
                      "blue is above average and yellow is below average.",
                      ColorBar(
                        60,
                        25.0,
                        240,
                        25.0,
                        n,
                        titlesTBelow,
                        titlesTAbove,
                        titleTWhite
                      )
                    )
                  )
                )
              ),
              totalRows = None,
              caption = Some(
                TagMod(
                  "Results ",
                  AppButton(
                    "CalcPlayed",
                    "as played",
                    BaseStyles.highlight(selected =
                      state.calc == CalculationAsPlayed
                    ),
                    ^.onClick --> setCalc(CalculationAsPlayed)
                  ),
                  AppButton(
                    "CalcMP",
                    "by MP",
                    BaseStyles.highlight(selected =
                      state.calc == CalculationMP
                    ),
                    ^.onClick --> setCalc(CalculationMP)
                  ),
                  AppButton(
                    "CalcIMP",
                    "by IMP",
                    BaseStyles.highlight(selected =
                      state.calc == CalculationIMP
                    ),
                    ^.onClick --> setCalc(CalculationIMP)
                  ),
                  " color by ",
                  getButton(ColorByWonPct, "ColorByWonPct", "Won %"),
                  getButton(
                    ColorByWonPtsPct,
                    "ColorByWonPtsPct",
                    "WonPoints %"
                  ),
                  if (state.calc != CalculationIMP)
                    getButton(ColorByPointsPct, "ColorByPointsPct", "Points %")
                  else TagMod(),
                  if (state.calc != CalculationMP)
                    getButton(ColorByIMP, "ColorByIMP", "IMP")
                  else TagMod()
                )
              )
            )
          )
        case Some(pds) =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= "No past duplicate matches were found"
          )
        case None =>
          HomePage.loading
      }
    }
  }

  private[duplicate] val component = ScalaComponent
    .builder[Props]("ViewPairsGrid")
    .initialStateFromProps { props =>
      props.filter.pairsData match {
        case Some(pd) =>
          val anyMP = pd.pastgames.find(ds => ds.isMP).isDefined
          val anyIMP = pd.pastgames.find(ds => ds.isIMP).isDefined
          val calc: CalculationType =
            if (anyMP == anyIMP) CalculationAsPlayed
            else if (anyMP) CalculationMP
            else CalculationIMP
          State(calc = calc, initialCalc = calc)
        case None =>
          val calc: CalculationType = CalculationMP
          State(calc = calc, initialCalc = calc)
      }
    }
    .backend(new Backend(_))
    .renderBackend
    .build
}
