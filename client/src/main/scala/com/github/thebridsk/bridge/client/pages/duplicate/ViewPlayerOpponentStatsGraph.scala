package com.github.thebridsk.bridge.client.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.ColorBar
import com.github.thebridsk.bridge.clientcommon.react.PieChart
import com.github.thebridsk.bridge.clientcommon.react.Table
import com.github.thebridsk.bridge.clientcommon.react.SvgRect
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import com.github.thebridsk.bridge.clientcommon.react.Table.Column
import com.github.thebridsk.bridge.data.duplicate.stats.PlayersOpponentsStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerOpponentStat
import com.github.thebridsk.bridge.data.duplicate.stats.Statistic
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
 * ViewPlayerOpponentStatsGraph( routerCtl: BridgeRouter[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerOpponentStatsGraph {
  import ViewPlayerOpponentStatsGraphInternal._

  case class Props( stats: Option[PlayersOpponentsStats], showNoDataMsg: Boolean = false) {
  }

  def apply( stats: Option[PlayersOpponentsStats], showNoDataMsg: Boolean = false ) =
    component(Props( stats, showNoDataMsg))

}

object ViewPlayerOpponentStatsGraphInternal {
  import ViewPlayerOpponentStatsGraph._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewPlayerOpponentStatsGraph")

  sealed abstract class ColorBy( val name: String ) {
    def value(po: PlayerOpponentStat): Double
  }

  object ColorByPctMP extends ColorBy("PctMP") {
    def value(po: PlayerOpponentStat) = po.wonMP*100.0/po.totalMP
  }

  object ColorByTotalMP extends ColorBy("TotalMP") {
    def value(po: PlayerOpponentStat) = po.totalMP
  }

  object ColorByPlayed extends ColorBy("Played") {
    def value(po: PlayerOpponentStat) = po.matchesPlayed
  }

  object ColorByPctBeat extends ColorBy("PctBeat") {
    def value(po: PlayerOpponentStat) = po.matchesBeat*100.0/po.matchesPlayed
  }

  object ColorByPctTied extends ColorBy("PctTied") {
    def value(po: PlayerOpponentStat) = po.matchesTied*100.0/po.matchesPlayed
  }

  object ColorByPctLost extends ColorBy("PctLost") {
    def value(po: PlayerOpponentStat) = (po.matchesPlayed-po.matchesTied-po.matchesBeat)*100.0/po.matchesPlayed
  }

  class ColorStat( colorBy: ColorBy ) extends Statistic(colorBy.name) {
     def add(po: PlayerOpponentStat): Unit = {
       add( colorBy.value(po), 1)
     }

  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
                    colorBy: ColorBy = ColorByPctMP,
                    maxSize: Int = 60,
                    minSize: Int = 5
                  )

  /**
   * @param stat
   * @param n the number of steps
   * @return Tuple3( titlesBelow, titleMiddle, titlesAbove )
   */
  def stepTitles( stat: ColorStat, n: Int ) = {
    val min = stat.min
    val ave = stat.ave
    val max = stat.max

    val stepB = (ave-min)/n
    val below = (0 until n).map { i =>
      TagMod( f"${min+stepB*i}%.2f%%" )
    }.toList

    val middle = TagMod( f"${ave}%.2f%%" )

    val stepA = (max-ave)/n
    val above = (1 to n).map { i =>
      TagMod( f"${ave+stepA*i}%.2f%%" )
    }.toList
    (below,middle,above)
  }

  val pieChartMaxSize = 100
  val tooltipPieChartSize = 150

  def getData(
      po: PlayerOpponentStat,
      sizeSt: ColorStat,
      sizeMPSt: ColorStat,
      state: State,
      sizeMultiplier: Double
  ): TagMod = {
    if (po.matchesPlayed==0 && po.totalMP == 0) {
      TagMod()
    } else {
      TagMod(
          <.div(
              ComparisonPieChart(
                  good = po.matchesBeat,
                  bad = po.matchesPlayed-po.matchesBeat-po.matchesTied,
                  neutral = po.matchesTied,
                  title = Some( TagMod( "Matches ", po.player, " vs ", po.opponent)),
                  legendtitle = Left(true),
                  size = (sizeSt.scale(po.matchesPlayed, state.minSize, state.maxSize)*sizeMultiplier).toInt,
                  sizeInLegend = tooltipPieChartSize,
                  minSize = pieChartMaxSize
              ),
              ComparisonPieChart(
                  good = po.wonMP,
                  bad = po.totalMP-po.wonMP,
                  neutral = 0,
                  title = Some( TagMod( "MP ", po.player, " vs ", po.opponent)),
                  legendtitle = Left(true),
                  size = (sizeMPSt.scale(po.totalMP, state.minSize, state.maxSize)*sizeMultiplier).toInt,
                  sizeInLegend = tooltipPieChartSize,
                  minSize = pieChartMaxSize
              )
          )
      )
    }
  }

  val cellX = TagMod( "X" )

  def getRows(
      playersOpponentsStats: PlayersOpponentsStats,
      statSize: ColorStat,
      statSizeMP: ColorStat,
      statTotalSize: ColorStat,
      statTotalSizeMP: ColorStat,
      state: State
  ) = {
    playersOpponentsStats.players.map { rowPlayer =>
      TagMod(rowPlayer.player) ::
      playersOpponentsStats.players.map { colPlayer =>
        if (rowPlayer.player == colPlayer.player) {
          cellX
        } else {
          rowPlayer.getPlayer(colPlayer.player) match {
            case Some(pos) =>
              getData(pos, statSize, statSizeMP, state, 1)
            case None =>
              TagMod()
          }
        }
      } :::
      List(getData(rowPlayer.playerTotal().copy(opponent="all"),statTotalSize,statTotalSizeMP,state,1.5))
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def render( props: Props, state: State ) = {

      props.stats match {
        case Some(posUnsorted) if !posUnsorted.players.isEmpty =>
          val pos = posUnsorted.sort()

          val statSize = new ColorStat( ColorByPlayed)
          val statSizeMP = new ColorStat( ColorByTotalMP)

          val statTotalSize = new ColorStat( ColorByPlayed)
          val statTotalSizeMP = new ColorStat( ColorByTotalMP)

          pos.players.foreach { p =>
            p.opponents.foreach { o =>
              statSize.add(o)
              statSizeMP.add(o)
            }
            val t = p.playerTotal()
            statTotalSize.add(t)
            statTotalSizeMP.add(t)
          }

          logger.fine(s"""statSize=$statSize""")
          logger.fine(s"""statSizeMP=$statSizeMP""")
          logger.fine(s"""statTotalSize=$statTotalSize""")
          logger.fine(s"""statTotalSizeMP=$statTotalSizeMP""")

          val columns = Column("Player")::pos.players.map( p => Column(p.player) ):::List( Column( "Totals" ) )

          val rows = getRows(
              pos,
              statSize,
              statSizeMP,
              statTotalSize,
              statTotalSizeMP,
              state
          )

          <.div(
            dupStyles.divPlayerOpponentGrid,
            Table(
              columns = columns,
              rows = rows,
              header = None,
              footer = Some(
                TagMod(
                  <.tr(
                    <.td(
                      ^.colSpan:=pos.players.length+2,
                      "The first circle in a cell is the match results against the opponent.",
                      " Green, ",
                      PieChart( 15, 1.0::Nil, Some(ComparisonPieChart.ColorGood::Nil), attrs=Some(^.display := "inline-block") ),
                      ", indicates the opponent was beaten, gray, ",
                      PieChart( 15, 1.0::Nil, Some(ComparisonPieChart.ColorNeutral::Nil), attrs=Some(^.display := "inline-block") ),
                      ", indicates a tie, and red, ",
                      PieChart( 15, 1.0::Nil, Some(ComparisonPieChart.ColorBad::Nil), attrs=Some(^.display := "inline-block") ),
                      ", indicates losing.",
                      <.br,
                      "The second circle is the match point results against the opponent.",
                      " Green, ",
                      PieChart( 15, 1.0::Nil, Some(ComparisonPieChart.ColorGood::Nil), attrs=Some(^.display := "inline-block") ),
                      ", indicates the match points won, while red, ",
                      PieChart( 15, 1.0::Nil, Some(ComparisonPieChart.ColorBad::Nil), attrs=Some(^.display := "inline-block") ),
                      ", indicates match points lost.",

                    ),
                  ),
                )
              ),
              totalRows = None,
              caption = Some(
                TagMod(
                  "Player Opponent Stats",
                )
              )
            )
          )
        case Some(pos) =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= "No past player opponent statistics were found"
          )
        case None =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= HomePage.loading
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPlayerOpponentStatsGraph")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

