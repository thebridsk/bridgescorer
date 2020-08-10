package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.Table.Column
import com.github.thebridsk.bridge.clientcommon.react.Table
import DuplicateStyles._
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStat
import PlayerComparisonStat.{Competitive, PassedOut, SameSide}
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStat.StatType

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

  case class Props(stats: PlayerComparisonStats)

  def apply(
      stats: PlayerComparisonStats
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(stats))

}

object ViewPlayerAggressivenessInternal {
  import ViewPlayerAggressiveness._

  val logger: Logger = Logger("bridge.ViewPlayerAggressiveness")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State()

  val pieChartMaxSize = 100
  val tooltipPieChartSize = 150

  def calcSize(max: Int)(handsPlayed: Int): Int = {
    (handsPlayed.toDouble / max * (pieChartMaxSize - 5)).toInt + 5
  }

  def maxOf(values: Int*): Int = {
    values.foldLeft(0) { (ac, v) =>
      Math.max(ac, v)
    }
  }

  /**
    * Returns totals from the list of comparisons.
    * @return ( maxsizesame, maxsizeComp, statsSame, statsComp)
    */
  def totals(list: List[PlayerComparisonStat]): (
      Int,
      Int,
      Int,
      PlayerComparisonStat,
      PlayerComparisonStat,
      PlayerComparisonStat
  ) = {
    val max = list.foldLeft(
      (
        0,
        0,
        0,
        PlayerComparisonStat.zero("Totals", SameSide),
        PlayerComparisonStat.zero("Totals", Competitive),
        PlayerComparisonStat.zero("Totals", PassedOut)
      )
    ) { (ac, v) =>
      val t = maxOf(
        v.aggressivebad + v.aggressivegood + v.aggressiveneutral,
        v.passivebad + v.passivegood + v.passiveneutral
      )
      (
        Math.max(ac._1, if (v.stattype == SameSide) t else 0),
        Math.max(ac._2, if (v.stattype == Competitive) t else 0),
        Math.max(ac._3, if (v.stattype == PassedOut) t else 0),
        if (v.stattype == SameSide) ac._4.add(v.forTotals) else ac._4,
        if (v.stattype == Competitive) ac._5.add(v.forTotals) else ac._5,
        if (v.stattype == PassedOut) ac._6.add(v.forTotals) else ac._6
      )
    }
    max
  }

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

      val columns = Column("Player") ::
        Column("Aggressive") :: Column("Passive") ::
        Column("Competitive Aggressive") :: Column("Competitive Passive") ::
        Column("Passed Out Aggressive") :: Column("Passed Out Passive") ::
        Nil

      val (
        maxsizesame,
        maxsizeopp,
        maxsizepass,
        totalsSame,
        totalsComp,
        totalsPass
      ) =
        totals(props.stats.data)

      def sizeCalcSame(total: Int) = calcSize(maxsizesame)(total)
      def sizeCalcOpp(total: Int) = calcSize(maxsizeopp)(total)
      def sizeCalcPass(total: Int) = calcSize(maxsizepass)(total)

      def sizeCalc(total: Int, stattype: StatType) = {
        stattype match {
          case SameSide    => sizeCalcSame(total)
          case Competitive => sizeCalcOpp(total)
          case PassedOut   => sizeCalcPass(total)
        }
      }

      val players = props.stats.data.map(pcs => pcs.player).distinct.sorted

      def findStat(player: String, stattype: StatType) = {
        props.stats.data.find { pcs =>
          pcs.player == player && pcs.stattype == stattype
        }
      }

      /**
        * Returns three piecharts, aggressive, passive, neutral
        */
      def getPiecharts(
          player: String,
          stattype: StatType,
          sizeCalculation: (Int, StatType) => Int
      ): List[TagMod] = {
        findStat(player, stattype) match {
          case Some(pcs) =>
            getPiechartsFromStats(pcs, stattype, sizeCalculation)
          case None =>
            List(TagMod(), TagMod())
        }
      }

      def getPiechartsFromStats(
          pcs: PlayerComparisonStat,
          stattype: StatType,
          sizeCalculation: (Int, StatType) => Int
      ): List[TagMod] = {
        val totalagg =
          pcs.aggressivebad + pcs.aggressivegood + pcs.aggressiveneutral
        val totalpas = pcs.passivebad + pcs.passivegood + pcs.passiveneutral
        List(
          if (totalagg == 0) {
            TagMod()
          } else {
            TagMod(
              ComparisonPieChart(
                good = pcs.aggressivegood,
                bad = pcs.aggressivebad,
                neutral = pcs.aggressiveneutral,
                title = Some(
                  TagMod(
                    pcs.player,
                    " ",
                    stattype match {
                      case SameSide    => "Aggressive"
                      case Competitive => "Competitive Aggressive"
                      case PassedOut   => "Passed Out Aggressive"
                    }
                  )
                ),
                legendtitle = Left(true),
                size = sizeCalculation(totalagg, stattype),
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
                title = Some(
                  TagMod(
                    pcs.player,
                    " ",
                    stattype match {
                      case SameSide    => "Passive"
                      case Competitive => "Competitive Passive"
                      case PassedOut   => "Passed Out Passive"
                    }
                  )
                ),
                legendtitle = Left(true),
                size = sizeCalculation(totalpas, stattype),
                sizeInLegend = tooltipPieChartSize,
                minSize = pieChartMaxSize
              )
            )
          }
        )
      }

      val rows = players.map { player =>
        val playedSame = getPiecharts(player, SameSide, sizeCalc)
        val playedOpp = getPiecharts(player, Competitive, sizeCalc)
        val playedPass = getPiecharts(player, PassedOut, sizeCalc)
        TagMod(player) :: playedSame ::: playedOpp ::: playedPass
      }

      val totMaxSame = maxOf(
        totalsSame.aggressivebad + totalsSame.aggressivegood + totalsSame.aggressiveneutral,
        totalsSame.passivebad + totalsSame.passivegood + totalsSame.passiveneutral
      )
      val totMaxComp = maxOf(
        totalsComp.aggressivebad + totalsComp.aggressivegood + totalsComp.aggressiveneutral,
        totalsComp.passivebad + totalsComp.passivegood + totalsComp.passiveneutral
      )
      val totMaxPass = maxOf(
        totalsPass.aggressivebad + totalsPass.aggressivegood + totalsPass.aggressiveneutral,
        totalsPass.passivebad + totalsPass.passivegood + totalsPass.passiveneutral
      )

      def sizeCalcTotalSame(total: Int) = calcSize(totMaxSame)(total)
      def sizeCalcTotalOpp(total: Int) = calcSize(totMaxComp)(total)
      def sizeCalcTotalPass(total: Int) = calcSize(totMaxPass)(total)

      def sizeCalcTotal(total: Int, stattype: StatType) = {
        stattype match {
          case SameSide    => sizeCalcTotalSame(total)
          case Competitive => sizeCalcTotalOpp(total)
          case PassedOut   => sizeCalcTotalPass(total)
        }
      }

      val totrows = List(
        TagMod("Total") ::
          getPiechartsFromStats(totalsSame, SameSide, sizeCalcTotal) :::
          getPiechartsFromStats(totalsComp, Competitive, sizeCalcTotal) :::
          getPiechartsFromStats(totalsPass, PassedOut, sizeCalcTotal)
      )

      <.div(
        dupStyles.viewPlayerAggressiveness,
        Table(
          header = Some(
            <.tr(
              <.th(),
              <.th(^.colSpan := 6, "Bidding")
            )
          ),
          columns = columns,
          rows = rows,
          initialSort = None,
          footer = Some(
            <.tr(
              <.td(
                ^.colSpan := columns.length + 1,
                ComparisonPieChart.description
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

  private[duplicate] val component = ScalaComponent
    .builder[Props]("ViewPlayerAggressiveness")
    .initialStateFromProps { props =>
      State()
    }
    .backend(new Backend(_))
    .renderBackend
    .build
}
