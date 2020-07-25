package com.github.thebridsk.bridge.client.pages.duplicate


import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairData
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorBy
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsDataSummary
import com.github.thebridsk.bridge.clientcommon.react.Table.Column
import com.github.thebridsk.bridge.clientcommon.react.Table
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
 * ViewPairsMadeDownGrid( routerCtl: BridgeRouter[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPairsMadeDownGrid {
  import ViewPairsMadeDownGridInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false) {

    def getNames = filter.getNames
  }

  def apply( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false ) =
    component(Props( filter, showNoDataMsg))

}

object ViewPairsMadeDownGridInternal {
  import ViewPairsMadeDownGrid._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewPairsMadeDownGrid")

  def size( v: Int, vmin: Double, vmax: Double, sizemin: Int, sizemax: Int ): Int = {
    if (vmax == vmin) sizemax
    else ((v.toDouble-vmin)*(sizemax-sizemin)/(vmax-vmin) + sizemin).toInt
  }

  val Black = Color.rgb( 0, 0, 0 )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( tooltipChartSize: Int = 150,
                    maxSize: Int = 100,
                    minSize: Int = 5
                  )

  object ColorByPlayedResults extends ColorBy {
    val name = "Played"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  object ColorByDeclarerResults extends ColorBy {
    val name = "Declared"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.declarer }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  object ColorByDefendedResults extends ColorBy {
    val name = "Defended"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.defended }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  object ColorByPassedResults extends ColorBy {
    val name = "Passed"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.passed }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  val cellX = TagMod("X")
  val cellEmpty = TagMod()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def render( props: Props, state: State ) = {

      props.filter.pairsData match {
        case Some(pds) if !pds.players.isEmpty =>
          val summary = new PairsDataSummary( pds, ColorByDeclarerResults, props.filter.selected, props.filter.filterDisplayOnly, ColorByDefendedResults, ColorByPlayedResults, ColorByPassedResults )

          val statDec = summary.colorStat
          val statDef = summary.extraStats.head
          val statPlayed = summary.extraStats.tail.head
          val statPassed = summary.extraStats.tail.tail.head

          val statTotalDec = summary.colorStatPlayerTotals
          val statTotalDef = summary.extraStatsPlayerTotals.head
          val statTotalPlayed = summary.extraStatsPlayerTotals.tail.head
          val statTotalPassed = summary.extraStatsPlayerTotals.tail.tail.head

          val vMin = Math.min( statDec.min, Math.min( statDef.min, statPassed.min ) )
          val vMax = Math.max( statDec.max, Math.max( statDef.max, statPassed.max ) )
          val vTotalMin = Math.min( statTotalDec.min, Math.min( statTotalDef.min, statTotalPassed.min ) )
          val vTotalMax = Math.max( statTotalDec.max, Math.max( statTotalDef.max, statTotalPassed.max ) )

          val allPlayers = summary.playerTotals.filter { e =>
            e._2.details.map( d => d.total>0 ).getOrElse(false)
          }.map( e => e._1 ).toList.sorted
          val sortedPlayers = props.filter.selected.map( l => l ).getOrElse(allPlayers)

          def cellAll( pd: PairData, isTotals: Boolean ): TagMod = {
            val (vmin,vmax) = if (isTotals) (statTotalPlayed.min,statTotalPlayed.max) else (statPlayed.min,statPlayed.max)
            pd.details match {
              case Some(det) =>
                val sizeAll = size(det.total, vmin, vmax, state.minSize, state.maxSize)
                MadeDownPieChart(
                  made = det.made,
                  down = det.down,
                  allowedMade = det.allowedMade,
                  tookDown = det.tookDown,
                  passed = det.passed,
                  title = Some(s"${pd.player2} - ${pd.player1}"),
                  legendtitle = Left(true),
                  size = if (det.total == 0) -sizeAll else sizeAll,
                  sizeInLegend = state.maxSize,
                  minSize = state.maxSize
                )
              case None =>
                cellEmpty
            }
          }

          val shownSorted = sortedPlayers.filter( e => props.filter.isPlayerShown(e) )

          val columns = Column("Player")::shownSorted.map( p => Column(p) ):::List(Column("Totals"))

          val rows = shownSorted.map { rowPlayer =>

            val cells = shownSorted.map { colPlayer =>
              if (colPlayer == rowPlayer) cellX
              else {
                summary.get(rowPlayer, colPlayer) match {
                  case Some(pd) =>
                    cellAll( pd, false )
                  case None =>
                    cellEmpty
                }
              }
            }

            val pdTotal = summary.playerTotals.get(rowPlayer).getOrElse(PairData(rowPlayer,"",0,0,0,0,0,0,None,0,0,0,0,0,0))
            val totalCell = List( cellAll(pdTotal,true) )

            TagMod(rowPlayer)::cells:::totalCell
          }

          <.div(
            dupStyles.divPairsDetailsGrid,
            Table(
              columns = columns,
              rows = rows,
              initialSort = None,
              additionalRows = None,
              totalRows = None,
              header = None,
              caption = Some(TagMod(
                "Hand Results"
              )),
              footer = Some(TagMod(
                <.tr(
                  <.td(
                    ^.colSpan:=sortedPlayers.length+2,
                    MadeDownPieChart.description
                  )
                )
              ))
            )
          )

        case Some(pds) =>
          <.div(
            dupStyles.divPairsDetailsGrid,
            props.showNoDataMsg ?= "No past duplicate matches were found"
          )
        case None =>
          HomePage.loading
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPairsMadeDownGrid")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

