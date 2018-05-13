package com.example.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.data.duplicate.stats.PlayerStats
import com.example.data.duplicate.stats.ContractStats
import com.example.data.duplicate.stats.ContractType
import com.example.data.duplicate.stats.PlayerStat
import com.example.data.duplicate.stats.CounterStat
import com.example.data.duplicate.stats.ContractTypePassed
import com.example.data.duplicate.stats.ContractTypePartial
import com.example.data.duplicate.stats.ContractTypeGame
import com.example.data.duplicate.stats.ContractTypeSlam
import com.example.data.duplicate.stats.ContractTypeGrandSlam
import com.example.data.duplicate.stats.ContractType
import scala.annotation.tailrec
import com.example.data.duplicate.stats.ContractTypeTotal
import utils.logging.Logger
import com.example.react.Table.Column
import com.example.react.Table
import com.example.data.duplicate.stats.ContractStat
import DuplicateStyles._
import com.example.data.duplicate.stats.ContractTypeDoubledToGame
import com.example.data.duplicate.stats.PlayerComparisonStats
import com.example.data.duplicate.stats.PlayerComparisonStat

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewPlayerAggressiveness( ViewPlayerAggressiveness.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerAggressiveness {
  import ViewPlayerAggressivenessInternal._

  case class Props( stats: PlayerComparisonStats )

  def apply( stats: PlayerComparisonStats ) =
    component(Props(stats))

}

object ViewPlayerAggressivenessInternal {
  import ViewPlayerAggressiveness._

  val logger = Logger("bridge.ViewPlayerAggressiveness")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  val pieChartMaxSize = 100
  val tooltipPieChartSize = 150

  def calcSize(max: Int)( handsPlayed: Int ) = {
    (handsPlayed.toDouble/max*(pieChartMaxSize-5)).toInt + 5
  }

  def maxOf( values: Int* ): Int = {
    values.foldLeft(0) { (ac,v) => Math.max(ac,v) }
  }

  /**
   * Returns totals from the list of comparisons.
   * @return ( maxsizesame, maxsizeComp, statsSame, statsComp)
   */
  def totals( list: List[PlayerComparisonStat] ): (Int, Int, PlayerComparisonStat, PlayerComparisonStat) = {
    val max = list.foldLeft((0, 0, PlayerComparisonStat.zero("Totals", true), PlayerComparisonStat.zero("Totals", false))) { (ac,v) =>
      val t = maxOf( v.aggressivebad+v.aggressivegood+v.aggressiveneutral, v.passivebad+v.passivegood+v.passiveneutral )
      (
        Math.max(ac._1, if (v.sameside) t else 0),
        Math.max(ac._2, if (v.sameside) 0 else t),
        if (v.sameside) ac._3.add(v.forTotals) else ac._3,
        if (!v.sameside) ac._4.add(v.forTotals) else ac._4,
      )
    }
    max
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

      val columns = Column("Player")::
                    Column("Aggressive")::Column("Passive")::
                    Column("Competitive Aggressive")::Column("Competitive Passive")::
                    Nil

      val ( maxsizesame, maxsizeopp, totalsSame, totalsComp) =
            totals(props.stats.data)

      def sizeCalcSame( total: Int ) = calcSize(maxsizesame)(total)
      def sizeCalcOpp( total: Int ) = calcSize(maxsizeopp)(total)

      def sizeCalc( total: Int, sameside: Boolean ) = {
        if (sameside) sizeCalcSame(total)
        else sizeCalcOpp(total)
      }

      val players = props.stats.data.map( pcs => pcs.player ).distinct.sorted

      def findStat( player: String, sameside: Boolean ) = {
        props.stats.data.find { pcs =>
          pcs.player == player && pcs.sameside == sameside
        }
      }

      /**
       * Returns three piecharts, aggressive, passive, neutral
       */
      def getPiecharts( player: String, sameside: Boolean, sizeCalculation: (Int,Boolean)=>Int ): List[TagMod] = {
        findStat(player, sameside) match {
          case Some(pcs) =>
            getPiechartsFromStats(pcs,sameside,sizeCalculation)
          case None =>
            List( TagMod(), TagMod() )
        }
      }

      def getPiechartsFromStats( pcs: PlayerComparisonStat, sameside: Boolean, sizeCalculation: (Int,Boolean)=>Int ): List[TagMod] = {
        val totalagg = pcs.aggressivebad+pcs.aggressivegood+pcs.aggressiveneutral
        val totalpas = pcs.passivebad+pcs.passivegood+pcs.passiveneutral
        List(
          if (totalagg == 0) {
            TagMod()
          } else {
            TagMod(
              ComparisonPieChart(
                  good = pcs.aggressivegood,
                  bad = pcs.aggressivebad,
                  neutral = pcs.aggressiveneutral,
                  title = Some( TagMod( pcs.player, " ", if (sameside) "Aggressive" else "Competitive Aggressive" ) ),
                  legendtitle = Left(true),
                  size = sizeCalculation(totalagg,sameside),
                  sizeInLegend = tooltipPieChartSize,
                  minSize = pieChartMaxSize
              )
            )
          },
          if (totalpas == 0) {
            TagMod()
          } else {
            TagMod(
              ComparisonPieChart(
                  good = pcs.passivegood,
                  bad = pcs.passivebad,
                  neutral = pcs.passiveneutral,
                  title = Some( TagMod( pcs.player, " ", if (sameside) "Passive" else "Competitive Passive" ) ),
                  legendtitle = Left(true),
                  size = sizeCalculation(totalpas,sameside),
                  sizeInLegend = tooltipPieChartSize,
                  minSize = pieChartMaxSize
              )
            )
          }
        )
      }

      val rows = players.map { player =>
        val playedSame = getPiecharts( player, true, sizeCalc )
        val playedOpp = getPiecharts( player, false, sizeCalc )
        TagMod(player)::playedSame:::playedOpp
      }

      val totMaxSame = maxOf( totalsSame.aggressivebad+totalsSame.aggressivegood+totalsSame.aggressiveneutral, totalsSame.passivebad+totalsSame.passivegood+totalsSame.passiveneutral )
      val totMaxComp = maxOf( totalsComp.aggressivebad+totalsComp.aggressivegood+totalsComp.aggressiveneutral, totalsComp.passivebad+totalsComp.passivegood+totalsComp.passiveneutral )

      def sizeCalcTotalSame( total: Int ) = calcSize(totMaxSame)(total)
      def sizeCalcTotalOpp( total: Int ) = calcSize(totMaxComp)(total)

      def sizeCalcTotal( total: Int, sameside: Boolean ) = {
        if (sameside) sizeCalcTotalSame(total)
        else sizeCalcTotalOpp(total)
      }

      val totrows = List(
        TagMod( "Total" )::
        getPiechartsFromStats(totalsSame, true, sizeCalcTotal):::
        getPiechartsFromStats(totalsComp, false, sizeCalcTotal)
      )

      <.div(
        dupStyles.viewPlayerAggressiveness,
        Table(
          header = Some(
              <.tr(
                <.th(),
                <.th( ^.colSpan := 6, "Bidding"),
              )
          ),
          columns = columns,
          rows = rows,
          initialSort = None,
          footer = Some(
              <.tr(
                <.td(
                  ^.colSpan := columns.length + 1,
                  ComparisonPieChart.description,
                )
              )
          ),
          additionalRows = None,
          totalRows = Some(totrows),
          caption = Some("Player Aggressiveness Stats")
        )
      )

    }

  }

  val component = ScalaComponent.builder[Props]("ViewPlayerAggressiveness")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

