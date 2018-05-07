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

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewPlayerDoubledContractResults( ViewPlayerDoubledContractResults.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerDoubledContractResults {
  import ViewPlayerDoubledContractResultsInternal._

  case class Props( playerStats: PlayerStats, contractStats: ContractStats )

  def apply( playerStats: PlayerStats, contractStats: ContractStats ) =
    component(Props(playerStats,contractStats))

}

object ViewPlayerDoubledContractResultsInternal {
  import ViewPlayerDoubledContractResults._

  val logger = Logger("bridge.ViewPlayerDoubledContractResults")

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

  def getPlayerStatByContractType( ct: ContractType, list: List[PlayerStat] ) = {

    // ignore passed hands in total
    def fix( h: PlayerStat ) = {
      val (hp,hist) = if (h.contractType == ContractTypePassed.value) {
        (h.handsPlayed,List(CounterStat(10,h.handsPlayed)))
      } else {
        (h.handsPlayed,h.histogram)
      }
      h.copy(h.player, h.declarer, ct.value, hist, hp)
    }

    @tailrec
    def add( sum: PlayerStat, l: List[PlayerStat] ): PlayerStat = {
      if (l.isEmpty) sum
      else {
        val h = l.head
        add( sum.add(fix(h)), l.tail )
      }
    }

    if (list.isEmpty) None
    else if (ct == ContractTypeTotal) {

      val h = list.head
      val cc = add( fix(h), list.tail ).normalize

      Some(cc)

    } else {

      val l = list.filter( ps => ps.contractType == ct.value )
      if (l.isEmpty) None
      else {
        val h = l.head
        val cc = add( fix(h), l.tail ).normalize

        Some(cc)
      }
    }
  }

  val contractTypeOrderWithPlayerStats: List[ContractType] = ContractTypePartial::ContractTypeDoubledToGame::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::Nil
  def pieChartByTypeWithPlayerStats(
      list: List[PlayerStat],
      playedAs: String,
      funCalcSize: Int => Int,
      player: Option[String],
      colspan: Int = 1
  ): TagMod = {
    val dd = contractTypeOrderWithPlayerStats.map { ct =>
      list.find( ps => ps.contractType==ct.value ).map( ps => ps.handsPlayed ).getOrElse(0)
    }
    dd match {
      case List(rpartial,rdgame,rgame,rslam,rgrandslam) =>
        val sum = dd.foldLeft(0)((ac,v) => ac+v)
        val by = player.map( p => s" by ${p}" ).getOrElse("")
        TagMod(
          ^.colSpan := colspan,
          ContractTypePieChart(
            partial = rpartial,
            game = rgame,
            slam = rslam,
            grandslam = rgrandslam,
            passed = 0,
            title = Some(s"Types of hands as ${playedAs}${by}"),
            legendtitle = Left(true),
            size = funCalcSize( sum ),
            sizeInLegend = tooltipPieChartSize,
            minSize = pieChartMaxSize,
            doubledToGame = rdgame
          )
        )
      case _ =>
        TagMod( "Oops" )
    }
  }

  val contractTypeOrderWithContractStats: List[ContractType] = ContractTypePartial::ContractTypeDoubledToGame::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::Nil
  def pieChartByTypeWithContractStats(
      list: List[ContractStat],
      funCalcSize: Int => Int,
      colspan: Int = 2
  ): TagMod = {
    val dd = contractTypeOrderWithContractStats.map { ct =>
      list.find( ps => ps.contractType==ct.value ).map( ps => ps.handsPlayed ).getOrElse(0)
    }
    dd match {
      case List(rpartial,rdgame,rgame,rslam,rgrandslam) =>
        val sum = dd.foldLeft(0)((ac,v) => ac+v)
        TagMod(
          ^.colSpan := colspan,
          ContractTypePieChart(
            partial = rpartial,
            game = rgame,
            slam = rslam,
            grandslam = rgrandslam,
            passed = 0,
            title = Some(s"Types of hands"),
            legendtitle = Left(true),
            size = funCalcSize( sum ),
            sizeInLegend = tooltipPieChartSize,
            minSize = pieChartMaxSize,
            doubledToGame = rdgame
          )
        )
      case _ =>
        TagMod( "Oops" )
    }
  }

  val order: List[ContractType] = ContractTypeTotal::ContractTypePartial::ContractTypeDoubledToGame::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::Nil
  def genData(
      declarer: Option[List[PlayerStat]],
      defender: Option[List[PlayerStat]],
      total: Option[List[PlayerStat]],
      calcSizeCT: Int=>Int,
      calcSizeTotal: Int=>Int
  ) = {

    def asPlaying( b: Option[Boolean] ) = b.map( dec => if (dec) "Declarer" else "Defender" ).getOrElse("Total")

    /* *
     * @param d
     * @param b if None - totals, true - declarer, false defender
     * @return list of tuple3( List[PlayerStat], decdeftot, colspan )
     */
    def getInfo( ct: ContractType, d: Option[List[PlayerStat]], b: Option[Boolean] ) = {
      d.map { list =>
        val skip = ct == ContractTypePassed    // always skip declarer passed, same as defender passed
        (skip,getPlayerStatByContractType( ct,list ), asPlaying(b), if (ct == ContractTypePassed) 1 else b.map( _ =>1 ).getOrElse(2))
      }.toList
    }

    order.flatMap { ct =>
      ( getInfo(ct,declarer,Some(true)):::
        getInfo(ct,defender,Some(false)):::
        getInfo(ct,total,None)
      ).flatMap { entry =>
        val (skip,stat, decl,colspan) = entry
        if (skip) Nil
        else {
          stat match {
            case Some(ps) =>
              logger.fine( s"""Stat for ${decl}: ${stat}""" )
              if (ps.handsPlayed == 0) {
                TagMod( ^.colSpan := colspan)::Nil
              } else {
                val histogram = ps.histogram.filter(cs => cs.counter!=0 ).sortBy(cs=>cs.tricks)
                val title = if (ct == ContractTypePassed) s"${ps.player} in ${ct}"
                            else s"${ps.player} in ${ct} as ${decl}"
                TagMod(
                  ^.colSpan := colspan,
                  TrickPieChart(
                    histogram = histogram,
                    title = Some( title ),
                    legendtitle = Left(true),
                    size = if (ct == ContractTypeTotal) calcSizeTotal(ps.handsPlayed) else calcSizeCT(ps.handsPlayed),
                    sizeInLegend = tooltipPieChartSize,
                    minSize = pieChartMaxSize
                  )
                )::Nil
              }
            case None =>
              TagMod( ^.colSpan := colspan)::Nil
          }
        }
      }
    }
  }

  val toGame = "2N"::"2H"::"2S"::"3H"::"3S"::"3C"::"3D"::"4C"::"4D"::Nil

  def addDoubledToGame( stats: List[ContractStat] ) = {
    stats.flatMap { s =>
      if (s.contractType == ContractTypePassed.value) Nil
      else if (s.contract.contains("*")) {
        if (s.contractType == ContractTypePartial.value) {
          toGame.find( c => s.contract.startsWith(c)) match {
            case Some(ns) =>
              s.copy(contractType = ContractTypeDoubledToGame.value )::Nil
            case None =>
              s::Nil
          }
        } else {
          s::Nil
        }
      } else {
        Nil
      }
    }
  }

  /**
   * @return Tuple3(totalStats, maxHandsPlayed, maxHandsPlayedTotal)
   */
  def genTotalStats( stats: ContractStats ) = {

    def fix2( h: ContractStat ) = {
      if (h.contractType == ContractTypePassed.value) {
        val hist = List( CounterStat( 10, h.handsPlayed ) )
        h.copy(ContractTypeTotal.toString, ContractTypeTotal.value, hist, h.handsPlayed)
      } else {
        h.copy(ContractTypeTotal.toString, ContractTypeTotal.value, h.histogram, h.handsPlayed)
      }
    }

    def fix( h: ContractStat ) = {
      h.copy(h.contractType.toString(), h.contractType, h.histogram, h.handsPlayed)
    }

    @tailrec
    def add( sum: ContractStat, l: List[ContractStat], fixer: ContractStat => ContractStat ): ContractStat = {
      if (l.isEmpty) sum
      else {
        val h = l.head
        add( sum.add(fixer(h)), l.tail, fixer )
      }
    }

    val extraStats = order.filter(ct => ct != ContractTypeTotal).map( ct => ContractStat(ct.toString(), ct.value) )

    val almostAll = (extraStats:::addDoubledToGame(stats.data)).groupBy( ps => ps.contractType ).map { entry =>
      val (ct, allStats) = entry

      val h = allStats.head
      add( fix(h), allStats.tail, fix _ ).normalize

    }.toList.sortWith { (l,r) =>
      val il = order.indexWhere( ct => ct.value == l.contractType)
      val ir = order.indexWhere( ct => ct.value == r.contractType)
      il < ir
    }

    val max = almostAll.map( ps => ps.handsPlayed ).foldLeft(0)(Math.max _)

    val t = add( fix2(almostAll.head), almostAll.tail, fix2 _ ).normalize

    logger.fine(s"""TotalStats is ${almostAll}, totals is ${t}""")

    (t::almostAll, max, t.handsPlayed )
  }

  def totalsRowData(
      totalStats: List[ContractStat],
      calcSizeCT: Int => Int,
      calcSizeTotal: Int => Int
  ): List[TagMod] = {
    pieChartByTypeWithContractStats( totalStats, calcSizeTotal, 2 )::
    order.map { ct =>
      val colspan = if (ct == ContractTypePassed) 1 else 2
      totalStats.find( ps => ps.contractType==ct.value ) match {
        case Some(ps) =>
          if (ps.handsPlayed == 0) {
            TagMod(
                ^.colSpan := colspan
            )
          } else {
            val histogram = ps.histogram.filter(cs => cs.counter!=0 ).sortBy(cs=>cs.tricks)
            val hist = if (ct == ContractTypePassed) {
              histogram.map(ct => ct.copy(tricks=10))
            } else {
              histogram
            }
            TagMod(
              ^.colSpan := colspan,
              TrickPieChart(
                histogram = hist,
                title = Some( ct.toString ),
                legendtitle = Left(true),
                size = if (ct == ContractTypeTotal) calcSizeTotal(ps.handsPlayed) else calcSizeCT(ps.handsPlayed),
                sizeInLegend = tooltipPieChartSize,
                minSize = pieChartMaxSize
              )
            )
          }
        case None =>
          TagMod( ^.colSpan := colspan, "Oops" )
      }
    }
  }

  def totalsRow( stats: ContractStats ): List[TagMod] = {
    val (totalStats, maxHandsPlayed, maxHandsPlayedTotal) = genTotalStats(stats)
    totalsRowData( totalStats, calcSize(maxHandsPlayed), calcSize(maxHandsPlayedTotal) )
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
      val maxDown = Math.min( 0, props.playerStats.min )
      val maxMade = Math.max( 0, props.playerStats.max )

      val maxHandsPlayed = ( props.playerStats.declarer.map( ps => ps.handsPlayed ):::
                             props.playerStats.defender.map( ps => ps.handsPlayed )
                           ).foldLeft(0)(Math.max _)

      val players = ( props.playerStats.declarer.map( ps => ps.player ):::
                      props.playerStats.defender.map( ps => ps.player )
                    ).distinct.sorted

      val maxHandsPlayedTotal = players.flatMap { p =>
        val (declarer, defender) = props.playerStats.stats(p)
        getPlayerStatByContractType(ContractTypeTotal,declarer).toList:::getPlayerStatByContractType(ContractTypeTotal,defender).toList
      }.map( ps => ps.handsPlayed ).foldLeft(0)(Math.max _)

      def byType( list: List[PlayerStat], playedAs: String, player: Option[String], colspan: Int = 1 ) =
        pieChartByTypeWithPlayerStats( list, playedAs, calcSize(maxHandsPlayedTotal), player, colspan )


      val rows = players.map { p =>
        val (declarerNoPass, defenderWithPass) = props.playerStats.stats(p)

        val defender = defenderWithPass.filter( p => p.contractType != ContractTypePassed.value)

        val declarer = declarerNoPass

        logger.finest( s"""PlayerStats for ${p} = ${defender} ${declarer}""" )

        val data = genData(Some(declarer),Some(defender),None, calcSize(maxHandsPlayed), calcSize(maxHandsPlayedTotal) )

        TagMod(p)::byType(declarer,"Declarer",Some(p))::byType(defender,"Defender",Some(p))::data
      }

      val totRow = TagMod("Totals")::totalsRow(props.contractStats)

      val columns = Column("Player")::
                    Column(TagMod("Type by",<.br,"Declarer"))::
                    Column(TagMod("Type by",<.br,"Defender"))::
                    order.flatMap { ct =>
        val c = ct.toString()
        if (ct == ContractTypePassed) Column("Passed out")::Nil
        else Column(TagMod(c,<.br,"Declarer"))::Column(TagMod(c,<.br,"Defender"))::Nil
      }

      <.div(
        dupStyles.viewPlayerContractResults,
        Table(
          columns = columns,
          rows = rows,
          initialSort = None,
          header = None,
          footer = Some(
              <.tr(
                <.td(
                  ^.colSpan := columns.length + 1,
                  "The declarer column shows when the team that included the player got the contract,",
                  " not whether the player was actually the declarer",
                  <.br,
                  TrickPieChart.description(maxDown, maxMade, false),
                  "For the Type columns the colors are:",
                  ContractTypePieChart.description(true)
                )
              )
          ),
          additionalRows = None,
          totalRows = Some(List(totRow)),
          caption = Some("Player Doubled Stats")
        )
      )

    }

  }

  val component = ScalaComponent.builder[Props]("ViewPlayerDoubledContractResults")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

