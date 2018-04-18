package com.example.pages.duplicate


import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.data.SystemTime
import com.example.routes.BridgeRouter
import com.example.react.AppButton
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.example.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.example.rest2.RestClientDuplicateSummary
import com.example.bridge.store.DuplicateSummaryStore
import com.example.data.duplicate.suggestion.PairsData
import com.example.react.Utils._
import com.example.pages.BaseStyles
import com.example.data.duplicate.stats.DuplicateStats
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import com.example.data.rest.JsonSupport
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.example.react.PopupOkCancel
import com.example.react.PieChartTable
import com.example.react.PieChartTable.Column
import com.example.color.Color
import com.example.react.PieChartTable.Row
import com.example.data.duplicate.stats.PlayerStats
import com.example.react.ColorBar
import com.example.react.PieChartTable.Data
import com.example.data.duplicate.stats.ContractStats
import com.example.data.duplicate.stats.ContractStat
import scala.annotation.tailrec
import com.example.data.duplicate.stats.ContractTypePassed
import com.example.data.duplicate.stats.ContractTypePartial
import com.example.data.duplicate.stats.ContractTypeGame
import com.example.data.duplicate.stats.ContractTypeSlam
import com.example.data.duplicate.stats.ContractTypeGrandSlam
import com.example.data.duplicate.stats.ContractType
import com.example.data.duplicate.stats.PlayerStat
import com.example.data.duplicate.stats.ContractTypeTotal
import com.example.data.duplicate.stats.CounterStat
import com.example.data.bridge.ContractTricks
import com.example.react.PieChartTable.Cell
import com.example.react.PieChartTable.DataPieChart
import com.example.react.PieChartTable.DataTagMod
import scala.language.postfixOps
import com.example.react.PieChart

/**
 * Shows a summary page of all duplicate matches from the database.
 * Each match has a button that that shows that match, by going to the ScoreboardView(id) page.
 * There is also a button to create a new match, by going to the NewScoreboardView page.
 *
 * The data is obtained from the DuplicateStore object.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageStats( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object PageStats {
  import PageStatsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage] )

  def apply( routerCtl: BridgeRouter[DuplicatePage] ) = component(Props(routerCtl))

}

object PageStatsInternal {
  import PageStats._
  import DuplicateStyles._
  import scala.concurrent.ExecutionContext.Implicits.global
  import JsonSupport._

  val logger = Logger("bridge.PageStats")

  case class StatResult( duplicatestats: DuplicateStats )

  implicit val StatResultReads = Json.reads[StatResult]

  object GraphQLMethods {

    def duplicateStats() = {

      val vars = None
      val query =
         """{
           |  duplicatestats {
           |    playerStats {
           |      declarer {
           |        player
           |        declarer
           |        contractType
           |        handsPlayed
           |        histogram {
           |          tricks, counter
           |        }
           |      }
           |      defender {
           |        player
           |        declarer
           |        contractType
           |        handsPlayed
           |        histogram {
           |          tricks, counter
           |        }
           |      }
           |      min
           |      max
           |    }
           |    contractStats {
           |      data {
           |        contract
           |        contractType
           |        histogram {
           |          tricks, counter
           |        }
           |        handsPlayed
           |      }
           |      min
           |      max
           |    }
           |  }
           |}
           |""".stripMargin
      val operation = None

      GraphQLClient.request(query, vars, operation ).map { resp =>
        resp.data match {
          case Some( d: JsObject ) =>
            Json.fromJson[StatResult](d) match {
              case JsSuccess( t, path ) =>
                Right(t)
              case err: JsError =>
                logger.warning( s"Error processing return data from duplicate stats: ${JsError.toJson(err)}" )
                Left("Error processing returned data")
            }
          case _ =>
            logger.warning( s"Error on Imports list: ${resp}")
            Left("Internal error")
        }
      }.recover {
        case x: Exception =>
          logger.warning( s"Error on Imports list", x)
          Left("Internal error")
      }

    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( stats: Option[DuplicateStats] = None, msg: Option[TagMod] = None, aggregateDouble: Boolean = false )

  /**
   * zero data, shows as a small black square.
   */
  val zeroData = DataPieChart(-5,Nil,Nil,None)
  val zeroList = List[Data]()
  val emptyCell = Cell( zeroList )

  val suitSortOrder = "PNZSHDC"

  val colorTypePartial: Color = Color.hsl( 60, 100, 50.0 )  // yellow
  val colorTypeGame: Color = Color.hsl( 30, 100, 50.0 )  // orange
  val colorTypeSlam: Color = Color.hsl( 300, 100, 50.0 ) // purple
  val colorTypeGrandSlam = Color.Cyan
  val colorTypePassed = Color.Blue

  val ctColors = colorTypePartial::colorTypeGame::colorTypeSlam::colorTypeGrandSlam::colorTypePassed::Nil
  val typeOrder: List[ContractType] = ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypePassed::Nil

  val colorsMapForType = typeOrder.zip(ctColors).toMap

  val allMadeColors = for (
                         hue <- 120::150::180::Nil;
                         lightness <- 75::50::25::Nil
                      ) yield {
                        Color.hsl(hue,100,lightness)
                      }

  val allDownColors = for (
                         hue <- 0::330::30::300::60::Nil;
                         lightness <- 25::50::75::Nil
                      ) yield {
                        Color.hsl(hue,100,lightness)
                      }

  def trickTitle( title: String, colors: Color* ) = {
    <.li(
      PieChart( 15, colors.map(c=>1.0).toList, colors.toList ),
      " ",
      title
    )
  }

  def stats( prefix: String, n: Int, detail: List[TagMod], colors: List[Color], handsPlayed: Int ) = {
    if (n == 1) {
      detail.toTagMod
    } else if (n > 0) {
      TagMod(
        trickTitle( f"${prefix}: ${n} (${100.0*n/handsPlayed}%.2f%%)", colors:_* ),
        <.ul(
          detail.toTagMod
        )
      )
    } else {
      TagMod()
    }
  }

  /**
   * @param pre prefix that identifies what is being displayed
   * @param histogram the histogram that is being displayed
   * @param handsPlayed the number of hands that were played that are counted in the histogram
   * @param totalHandsPlayed the total number of hands played, including hands not in histogram.
   * @param colorMap mapping tricks to a color
   */
  def getTitle(
      pre: String,
      histogram: List[CounterStat],
      handsPlayed: Int,
      totalHandsPlayed: Option[Int],
      colorMap: Int=>Color,
      madeColors: List[Color],
      downColors: List[Color],
      passedColor: Color
  ) = {

    val (made,down,passed,smade,sdown) = histogram.foldLeft((0,0,0,List[TagMod](),List[TagMod]())) { (ac,v) =>
      val percent: Double = 100.0 * v.counter / handsPlayed
      val s = if (v.tricks < 0)        trickTitle( f"Down ${-v.tricks}: ${v.counter} (${percent}%.2f%%)", colorMap(v.tricks) )
              else if (v.tricks == 0)  trickTitle( f"Made   : ${v.counter} (${percent}%.2f%%)", colorMap(v.tricks) )
              else if (v.tricks == 10) TagMod()
              else                     trickTitle( f"Made +${v.tricks}: ${v.counter} (${percent}%.2f%%)", colorMap(v.tricks) )
      if (v.tricks == 10)    (ac._1,          ac._2,           ac._3+v.counter, ac._4,          ac._5 )
      else if (v.tricks < 0) (ac._1,          ac._2+v.counter, ac._3,           ac._4,          s::ac._5 )
      else                   (ac._1+v.counter,ac._2,           ac._3,           ac._4:::s::Nil, ac._5 )
    }

    <.ul(
      <.li( pre ),
      <.li(
        f"Total: ${handsPlayed}",
        totalHandsPlayed.filter(t=>t!=0).map( t => TagMod( f" (${100.0*handsPlayed/t}%.2f%%)" )).getOrElse(TagMod(""))
      ),
      stats( "Made", made, smade, madeColors, handsPlayed ),
      stats( "Down", down, sdown, downColors, handsPlayed ),
      if (passed > 0) {
        TagMod(
          trickTitle( f"Passed Out: ${passed} (${100.0*passed/handsPlayed}%.2f%%)", passedColor ),
        )
      } else {
        TagMod()
      }
    )
  }


  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend( scope: BackendScope[Props, State]) {

    val cancel = scope.modState( s => s.copy(msg = None) )

    def displayPlayer( stats: PlayerStats, contractStats: ContractStats ) = {

      val numberDown = Math.max( 0, -stats.min )
      val numberMade = Math.max( 0, stats.max+1 )

      val titleDown = if (numberDown > 0) {
        (-numberDown until 0).map( i => s"Down ${-i}" ).toList
      } else {
        List()
      }
      val titleMade = (0 until numberMade).map( i => if (i==0) "Made" else s"Made ${i}" ).toList

      val downColors: Seq[Color] = allDownColors.take(numberDown) // ColorBar.colors( 0, 25.0, numberDown, false )
      val madeColors: Seq[Color] = allMadeColors.take(numberMade) // ColorBar.colors( 120, 25.0, numberMade, false )

      def colorMap( i: Int ) = {
        if (i == 10) colorTypePassed
        else if (i < 0) downColors( -i-1 )
        else madeColors( i )
      }

      val maxHandsPlayed = (stats.declarer.map( ps => ps.handsPlayed ):::stats.defender.map( ps => ps.handsPlayed )).foldLeft(0)(Math.max _)

      val pieChartMaxSize = 100
      val pieChartMaxSizePlusPadding = 105

      val tooltipPieChartSize = 150

      def calcSize(max: Int)( handsPlayed: Int ) = {
        (handsPlayed.toDouble/max*(pieChartMaxSize-5)).toInt + 5
      }

      val players = (stats.declarer.map( ps => ps.player ):::stats.defender.map( ps => ps.player )).distinct.sorted

      def getCT( ct: ContractType, list: List[PlayerStat] ) = {

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

      val maxHandsPlayedTotal = players.flatMap { p =>
        val (declarer, defender) = stats.stats(p)
        getCT(ContractTypeTotal,declarer).toList:::getCT(ContractTypeTotal,defender).toList
      }.map( ps => ps.handsPlayed ).foldLeft(0)(Math.max _)

      val order: List[ContractType] = ContractTypePassed::ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypeTotal::Nil

      def byType( list: List[PlayerStat], playedAs: String, passedout: PlayerStat, colspan: Int = 1 ) = {
        val or: List[ContractType] = ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::Nil
        val dd = or.zipWithIndex.map { entry =>
          val (ct,i) = entry
          ( ct, list.find( ps => ps.contractType==ct.value ).map( ps => ps.handsPlayed ).getOrElse(0).toDouble, colorsMapForType(ct) )
        }
        val sum = dd.foldLeft(0.0)((ac,v) => ac+v._2) + passedout.handsPlayed
        val (cts,values,cols) = dd.unzip3
//        val title = s"Types of hands as ${playedAs}"+dd.map { entry =>
//          val (ct,value, col) = entry
//          f"${ct.toString()}: ${value} (${100.0*value/sum}%.2f%%)"
//        }.mkString("\n  ","\n  ","\n  ")+f"${ContractTypePassed.toString()}: ${passedout.handsPlayed} (${100.0*passedout.handsPlayed/sum}%.2f%%)"
        val title = <.ul(
          <.li(s"Types of hands as ${playedAs}"),
          dd.flatMap { entry =>
            val (ct,value, col) = entry
            if (value == 0) Nil
            else trickTitle( f"${ct.toString()}: ${value} (${100.0*value/sum}%.2f%%)", col )::Nil
          }.toTagMod,
          if (passedout.handsPlayed == 0) TagMod()
          else trickTitle( f"${ContractTypePassed.toString()}: ${passedout.handsPlayed} (${100.0*passedout.handsPlayed/sum}%.2f%%)", colorTypePassed )
        )
        DataPieChart(
            calcSize(maxHandsPlayedTotal)( sum.toInt ),
            cols:::(colorTypePassed::Nil),
            values:::(passedout.handsPlayed.toDouble::Nil)
        ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding ).
           withColSpan( colspan )
      }

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
            (getCT( ct,list ), asPlaying(b), if (ct == ContractTypePassed) 1 else b.map( _ =>1 ).getOrElse(2))
          }.toList
        }

        order.flatMap { ct =>
          ( getInfo(ct,declarer,Some(true)):::
            getInfo(ct,defender,Some(false)):::
            getInfo(ct,total,None)
          ).map { entry =>
            val (stat, decl,colspan) = entry
            stat match {
              case Some(ps) =>
                logger.fine( s"""Stat for ${decl}: ${stat}""" )
                if (ps.handsPlayed == 0) {
                  Cell(zeroList).withColSpan(colspan)
                } else {
                  val histogram = ps.histogram.filter(cs => cs.counter!=0 ).sortBy(cs=>cs.tricks)
                  val (cols,vals) = histogram.map(cs => (colorMap(cs.tricks), cs.counter.toDouble)).unzip
                  if (ct == ContractTypePassed) {
                    val title = s"${ps.player} in ${ct}\nPassed: ${ps.handsPlayed}"
                    DataPieChart(
                      calcSizeCT(ps.handsPlayed ),
                      colorTypePassed::Nil,
                      ps.handsPlayed.toDouble::Nil
                    ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding).withColSpan(colspan)
                  } else {
                    val pre = s"${ps.player} in ${ct} as ${decl}"
                    val title = getTitle(pre, histogram, ps.handsPlayed, None, colorMap, madeColors.toList, downColors.toList, colorTypePassed)

                    DataPieChart(
                      if (ct == ContractTypeTotal) calcSizeTotal(ps.handsPlayed) else calcSizeCT(ps.handsPlayed),
                      cols,
                      vals
                    ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding ).
                        withColSpan( colspan )
                  }

                }
              case None =>
                Cell(zeroList).withColSpan(colspan)
            }
          }
        }
      }

      val rows = players.map { p =>
        val (declarerNoPass, defender) = stats.stats(p)

        val passedout = defender.find( ps => ps.contractType == ContractTypePassed.value).getOrElse( PlayerStat(p,false,ContractTypePassed.value) )

        val declarer = passedout.copy(declarer = true):: declarerNoPass

        logger.fine( s"""PlayerStats for ${p} = ${defender} ${declarer}""" )

        val data = genData(Some(declarer),Some(defender),None, calcSize(maxHandsPlayed), calcSize(maxHandsPlayedTotal) )

        Row( p, byType(declarer,"Declarer",passedout)::byType(defender,"Defender",passedout)::(data.drop(1)) )   // drop passed declarer
      }

      val totalsRow = Row( "Totals", dataTotals(contractStats) )

      val columns = Column(TagMod("Type by",<.br,"Declarer"))::Column(TagMod("Type by",<.br,"Defender"))::Column("Passed"):: order.filter(p => p!=ContractTypePassed).flatMap { ct =>
        val c = ct.toString()
        Column(TagMod(c,<.br,"Declarer"))::Column(TagMod(c,<.br,"Defender"))::Nil
      }

      PieChartTable(
        firstColumn = Column("Player"),
        columns = columns,
        rows = rows,
        header = None,
        footer = Some(
            <.tr(
              <.td(
                ^.colSpan := columns.length + 1,
                "The declarer column shows when the team that included the player got the contract,",
                " not whether the player was actually the declarer",
                <.br,
                "The color indicates the result of the contract.",
                <.br,
                "Red indicates a down, green a made contract.",
                <.br,
                "Dark green indicates a contract made with no overtricks",
                ColorBar.create(downColors.reverse, madeColors, None, Some(titleDown), Some(titleMade), None),
                "For the Type columns the colors are:",
                ColorBar.simple( ctColors, Some(typeOrder.map( ct => ct.toString() )) )
              )
            )
        ),
        totalRows = Some(List(totalsRow)),
        caption = Some( "Player Stats" )
      )
    }


    def dataTotals( stats: ContractStats ) = {

      val numberDown = Math.max( 0, -stats.min )
      val numberMade = Math.max( 0, stats.max+1 )

      val titleDown = if (numberDown > 0) {
        (-numberDown until 0).map( i => s"Down ${-i}" ).toList
      } else {
        List()
      }
      val titleMade = (0 until numberMade).map( i => if (i==0) "Made" else s"Made ${i}" ).toList

      val downColors: Seq[Color] = allDownColors.take(numberDown) // ColorBar.colors( 0, 25.0, numberDown, false )
      val madeColors: Seq[Color] = allMadeColors.take(numberMade) // ColorBar.colors( 120, 25.0, numberMade, false )

      def colorMap( i: Int ) = {
        // 10 tricks indicates passed out hand

        if (i < 0) downColors( -i-1 )
        else if (i == 10) colorTypePassed
        else madeColors( i )
      }

      val order: List[ContractType] = ContractTypePassed::ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypeTotal::Nil

      val columns = Column("Type")::order.map( ct => Column( ct.toString() ) )

      val (totalStats, maxHandsPlayed, maxHandsPlayedTotal) = {

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

        val extraStats = order.take(order.length-1).map( ct => ContractStat(ct.toString(), ct.value) )

        val almostAll = (extraStats:::stats.data).groupBy( ps => ps.contractType ).map { entry =>
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

        (almostAll ::: (t::Nil), max, t.handsPlayed )
      }

      val pieChartMaxSize = 100
      val pieChartMaxSizePlusPadding = 105

      val tooltipPieChartSize = 150

      def calcSize( handsPlayed: Int, max: Int ) = {
        (handsPlayed.toDouble/max*(pieChartMaxSize-5)).toInt + 5
      }

      def byType( list: List[ContractStat] ) = {
        val or: List[ContractType] = ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypePassed::Nil
        val dd = or.zipWithIndex.map { entry =>
          val (ct,i) = entry
          ( ct, list.find( ps => ps.contractType==ct.value ).map( ps => ps.handsPlayed ).getOrElse(0), colorsMapForType(ct) )
        }
        val sum = dd.foldLeft(0.0)((ac,v) => ac+v._2)
        val (cts,values,cols) = dd.unzip3
//        val title = s"Types of hands\nTotal $sum"+dd.map { entry =>
//          val (ct,value, col) = entry
//          f"${ct.toString()}: ${value} (${100*value/sum}%.2f%%)"
//        }.mkString("\n  ","\n  ","")
        val title = <.ul(
          <.li(s"Types of hands"),
          <.li(s"Total $sum"),
          dd.flatMap { entry =>
            val (ct,value, col) = entry
            if (value == 0) Nil
            else trickTitle( f"${ct.toString()}: ${value} (${100.0*value/sum}%.2f%%)", col )::Nil
          }.toTagMod
        )
        DataPieChart(
          calcSize( sum.toInt, maxHandsPlayedTotal ),
          cols,
          values.map( i => i.toDouble )
        ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding ).withColSpan(2)
      }

      val first = byType( totalStats.take(totalStats.length-1))

      val cells = first::
        totalStats.zip(order).map { entry =>
          val (ps,ct) = entry
          val colspan = if (ct == ContractTypePassed) 1 else 2
          if (ps.handsPlayed == 0) {
            Cell(zeroList).withColSpan(colspan)
          } else {
            val (cols,vals) = ps.histogram.map(cs => (colorMap(cs.tricks), cs.counter.toDouble)).unzip
            if (ps.contractType == ContractTypePassed.value) {
              val title = f"Passed: ${ps.handsPlayed} ${100.0*ps.handsPlayed/maxHandsPlayedTotal}%.2f%%"
              DataPieChart(
                calcSize(ps.handsPlayed, maxHandsPlayed),
                colorTypePassed::Nil,
                ps.handsPlayed.toDouble::Nil
              ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding ).withColSpan(colspan)
            } else {
              val title = getTitle( ct.toString(), ps.histogram, ps.handsPlayed, Some(maxHandsPlayedTotal), colorMap, madeColors.toList, downColors.toList, colorTypePassed )
//              val pre = f"${ct} ${100.0*ps.handsPlayed/maxHandsPlayedTotal}%.2f%%"
//              val (made,down,passed,smade,sdown) = ps.histogram.foldLeft((0,0,0,"","")) { (ac,v) =>
//                val percent: Double = 100.0 * v.counter / ps.handsPlayed
//                val s = if (v.tricks < 0)        f"\n  Down ${-v.tricks}: ${v.counter} (${percent}%.2f%%)"
//                        else if (v.tricks == 0)  f"\n  Made   : ${v.counter} (${percent}%.2f%%)"
//                        else if (v.tricks == 10) ""
//                        else                     f"\n  Made +${v.tricks}: ${v.counter} (${percent}%.2f%%)"
//                if (v.tricks == 10)    (ac._1,          ac._2,           ac._3+v.counter, ac._4,   ac._5 )
//                else if (v.tricks < 0) (ac._1,          ac._2+v.counter, ac._3,           ac._4,   s+ac._5 )
//                else                   (ac._1+v.counter,ac._2,           ac._3,           ac._4+s, ac._5 )
//              }
//              val pretitle = s"${pre}\nTotal: ${ps.handsPlayed}"
//              val tmade = if (made > 0) {
//                f"\nMade ${made} (${100.0*made/ps.handsPlayed}%.2f%%)"+smade
//              } else {
//                ""
//              }
//              val tdown = if (down > 0) {
//                f"\nDown ${down} (${100.0*down/ps.handsPlayed}%.2f%%)"+sdown
//              } else {
//                ""
//              }
//              val tpass = if (passed > 0) {
//                f"\nPassed ${passed} (${100.0*passed/ps.handsPlayed}%.2f%%)"
//              } else {
//                ""
//              }
//              val title = pretitle+tmade+tdown+tpass
              logger.fine(s"""Working with ${ct} histogram is ${ps.histogram}""")
              DataPieChart(
                calcSize(ps.handsPlayed, if (ct==ContractTypeTotal) maxHandsPlayedTotal else maxHandsPlayed),
                cols,
                vals
              ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding ).withColSpan(colspan)
            }

          }
        }

      cells
    }

    def displayTotals( stats: ContractStats ) = {

      val numberDown = Math.max( 0, -stats.min )
      val numberMade = Math.max( 0, stats.max+1 )

      val titleDown = if (numberDown > 0) {
        (-numberDown until 0).map( i => s"Down ${-i}" ).toList
      } else {
        List()
      }
      val titleMade = (0 until numberMade).map( i => if (i==0) "Made" else s"Made ${i}" ).toList

      val downColors: Seq[Color] = allDownColors.take(numberDown) // ColorBar.colors( 0, 25.0, numberDown, false )
      val madeColors: Seq[Color] = allMadeColors.take(numberMade) // ColorBar.colors( 120, 25.0, numberMade, false )

      def colorMap( i: Int ) = {
        // 10 tricks indicates passed out hand

        if (i < 0) downColors( -i-1 )
        else if (i == 10) colorTypePassed
        else madeColors( i )
      }

      val order: List[ContractType] = ContractTypePassed::ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypeTotal::Nil

      val columns = Column("Type")::order.map( ct => Column( ct.toString() ) )

      val (totalStats, maxHandsPlayed, maxHandsPlayedTotal) = {

        def fix2( h: ContractStat ) = {
          h.copy(ContractTypeTotal.toString, ContractTypeTotal.value, h.histogram, h.handsPlayed)
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

        val extraStats = order.take(order.length-1).map( ct => ContractStat(ct.toString(), ct.value) )

        val almostAll = (extraStats:::stats.data).groupBy( ps => ps.contractType ).map { entry =>
          val (ct, allStats) = entry

          val h = allStats.head
          add( fix(h), allStats.tail, fix _ ).normalize

        }.toList.sortWith { (l,r) =>
          val il = order.indexWhere( ct => ct.value == l.contractType)
          val ir = order.indexWhere( ct => ct.value == r.contractType)
          il < ir
        }.map { ps =>
          // need to divide all counters by 2 since there are two players on all teams.
          val handsPlayed = ps.handsPlayed
          val m = ps.histogram
          ps.copy(histogram = m, handsPlayed = handsPlayed).normalize
        }

        val max = almostAll.map( ps => ps.handsPlayed ).foldLeft(0)(Math.max _)

        val t = add( fix2(almostAll.head), almostAll.tail, fix2 _ ).normalize

        (almostAll ::: (t::Nil), max, t.handsPlayed )
      }

      val pieChartMaxSize = 100
      val pieChartMaxSizePlusPadding = 105

      val tooltipPieChartSize = 150

      def calcSize( handsPlayed: Int, max: Int ) = {
        (handsPlayed.toDouble/max*(pieChartMaxSize-5)).toInt + 5
      }

      def byType( list: List[ContractStat] ) = {
        val or: List[ContractType] = ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypePassed::Nil
        val dd = or.zipWithIndex.map { entry =>
          val (ct,i) = entry
          ( ct, list.find( ps => ps.contractType==ct.value ).map( ps => ps.handsPlayed ).getOrElse(0).toDouble, colorsMapForType(ct) )
        }
        val sum = dd.foldLeft(0.0)((ac,v) => ac+v._2)
        val (cts,values,cols) = dd.unzip3
        val title = s"Types of hands"+dd.map { entry =>
          val (ct,value, col) = entry
          f"${ct.toString()}: ${value} (${100*value/sum}%.2f%%)"
        }.mkString("\n  ","\n  ","")
        DataPieChart(
          calcSize( sum.toInt, maxHandsPlayedTotal ),
          cols,
          values
        ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding )
      }

      val first = byType( totalStats.take(totalStats.length-1))

      val data = first::
        totalStats.zip(order).map { entry =>
          val (s,ct) = entry
          if (s.handsPlayed == 0) {
            Cell(zeroList)
          } else {
            val (cols,vals) = s.histogram.map(cs => (colorMap(cs.tricks), cs.counter.toDouble)).unzip
            if (s.contractType == ContractTypePassed.value) {
              val title = f"Passed: ${s.handsPlayed} ${100.0*s.handsPlayed/maxHandsPlayedTotal}%.2f%%"
              DataPieChart(
                calcSize(s.handsPlayed, maxHandsPlayed),
                colorTypePassed::Nil,
                s.handsPlayed.toDouble::Nil
              ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding )
            } else {
              val pre = f"${ct} ${100.0*s.handsPlayed/maxHandsPlayedTotal}%.2f%%"
              val (made,down,passed,smade,sdown) = s.histogram.foldLeft((0,0,0,"","")) { (ac,v) =>
                val percent: Double = 100.0 * v.counter / s.handsPlayed
                val ss = if (v.tricks < 0)        f"\n  Down ${-v.tricks}: ${v.counter} (${percent}%.2f%%)"
                         else if (v.tricks == 0)  f"\n  Made   : ${v.counter} (${percent}%.2f%%)"
                         else if (v.tricks == 10) ""
                         else                     f"\n  Made +${v.tricks}: ${v.counter} (${percent}%.2f%%)"
                if (v.tricks == 10)    (ac._1,          ac._2,           ac._3+v.counter, ac._4,   ac._5 )
                else if (v.tricks < 0) (ac._1,          ac._2+v.counter, ac._3,           ac._4,   ss+ac._5 )
                else                   (ac._1+v.counter,ac._2,           ac._3,           ac._4+ss, ac._5 )
              }
              val pretitle = s"${pre}\nTotal: ${s.handsPlayed}"
              val tmade = if (made > 0) {
                f"\nMade ${made} (${100.0*made/s.handsPlayed}%.2f%%)"+smade
              } else {
                ""
              }
              val tdown = if (down > 0) {
                f"\nDown ${down} (${100.0*down/s.handsPlayed}%.2f%%)"+sdown
              } else {
                ""
              }
              val tpass = if (passed > 0) {
                f"\nPassed ${passed} (${100.0*passed/s.handsPlayed}%.2f%%)"
              } else {
                ""
              }
              val title = pretitle+tmade+tdown+tpass

              DataPieChart(
                calcSize(s.handsPlayed, if (ct==ContractTypeTotal) maxHandsPlayedTotal else maxHandsPlayed),
                cols,
                vals
              ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding )
            }

          }
        }

      val row = Row( "Total", data )

      PieChartTable(
        firstColumn = Column("All Hands"),
        columns = columns,
        rows = List(row),
        header = None,
        footer = Some(
            <.tr(
              <.td(
                ^.colSpan := columns.length + 1,
                "Blue indicates a passed out hand",
                <.br,
                "The color indicates the result of the contract.",
                <.br,
                "Red indicates a down, green a made contract.",
                <.br,
                "Dark green indicates a contract made with no overtricks",
                ColorBar.create(downColors, madeColors, None, Some(titleDown), Some(titleMade), None),
                "For the Type columns the colors are:",
                ColorBar.simple( ctColors, Some(typeOrder.map( ct => ct.toString() )) )
              )
            )
        ),
        totalRows = None,
        caption = Some( "All Hands Statistics" )
      )
    }



    def displayContract( stats: ContractStats, aggregateDouble: Boolean ) = {

      val numberDown = Math.max( 0, -stats.min )
      val numberMade = Math.max( 0, stats.max+1 )

      val titleDown = if (numberDown > 0) {
        (-numberDown until 0).map( i => s"Down ${-i}" ).toList
      } else {
        List()
      }
      val titleMade = (0 until numberMade).map( i => if (i==0) "Made" else s"Made ${i}" ).toList

      val downColors: Seq[Color] = allDownColors.take(numberDown) // ColorBar.colors( 0, 25.0, numberDown, false )
      val madeColors: Seq[Color] = allMadeColors.take(numberMade) // ColorBar.colors( 120, 25.0, numberMade, false )

      /* *
       * @param i - the number of overtricks, +, or undertricks, -, or 0 if exactly made
       *            10 if passed out
       * @return the color
       */
      def colorMap( i: Int ) = {
        if (i == 10) colorTypePassed
        else if (i < 0) downColors( -i-1 )
        else madeColors( i )
      }

      val statsDataAll = stats.data

      val statsDataDoubled = stats.data.filter( cs => cs.contract.contains("*") || cs.contract == "PassedOut" )

      val ( totalHandsPlayed, maxHandsPlayed) = statsDataAll.map( ps => ps.handsPlayed ).foldLeft((0,0))( (ac,v) => ( ac._1+v, Math.max(ac._2,v)) )
      val ( totalDoubledHandsPlayed, maxDoubledHandsPlayed) = statsDataDoubled.map( ps => ps.handsPlayed ).foldLeft((0,0))( (ac,v) => ( ac._1+v, Math.max(ac._2,v)) )

      val pieChartMaxSize = 100
      val pieChartMaxSizePlusPadding = 105

      val tooltipPieChartSize = 150

      def calcSizeAll( handsPlayed: Int ) = {
        if (handsPlayed == 0) -5
        else (handsPlayed.toDouble/maxHandsPlayed*(pieChartMaxSize-5)).toInt + 5
      }

      def calcSizeDoubled( handsPlayed: Int ) = {
        if (handsPlayed == 0) -5
        else (handsPlayed.toDouble/maxDoubledHandsPlayed*(pieChartMaxSize-5)).toInt + 5
      }

      def getTitle( contract: String, cs: ContractStat, totalHands: Int ) = {
        PageStatsInternal.getTitle(contract, cs.histogram, cs.handsPlayed, Some(totalHands), colorMap, madeColors.toList, downColors.toList, colorTypePassed)
      }

      /* *
       * @param data
       * @return a List[List[List[ContractStat]]].  The outermost list is suit, the middle is contract tricks, inner is doubled.
       * Passed shows up as a suit.
       */
      def sortContractStat( data: List[ContractStat], calcSize: Int => Int ) = {
        val bySuit = data.groupBy(cs=> cs.parseContract.suit)
        List("P","Z","S","H","D","C").map { suit =>
          val dataBySuit = bySuit.get(suit).getOrElse(List())
          if (suit == "P") {
            // passed out
            // dataBySuit should only have one entry
            val dd = if (dataBySuit.isEmpty) {
              emptyCell
            } else {
              val pd = dataBySuit.head
              val title = f"Passed Out: ${pd.handsPlayed} (${100.0*pd.handsPlayed/totalHandsPlayed}%.2f%%)"
              DataPieChart(
                calcSize( pd.handsPlayed),
                colorTypePassed::Nil,
                1.0::Nil
              ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding )
            }
            ( suit, List( dd, emptyCell, emptyCell, emptyCell, emptyCell, emptyCell, emptyCell ) )
          } else {
            val trickdata = (1 to 7).map { ntricks =>
              val tricks = ntricks.toString()

              val tcss = dataBySuit.filter( cs => cs.parseContract.tricks == tricks )

              if (tcss.isEmpty) emptyCell
              else {
                if (aggregateDouble) {
                  @tailrec
                  def add( cs: ContractStat, other: List[ContractStat] ): ContractStat = {
                    if (other.isEmpty) cs
                    else {
                      add( cs.add(other.head.copy(contract=cs.contract)), other.tail )
                    }
                  }
                  val s = add( tcss.head, tcss.tail )
                  // s.doubled is not valid after this.
                  val (cols,vals) = s.histogram.map(cs => (colorMap(cs.tricks), cs.counter.toDouble)).unzip
                  val con = s.parseContract
                  val suit = if (con.suit == "Z") "N" else con.suit
                  val title = getTitle(s"${con.tricks}${suit}", s, totalHandsPlayed)
                  DataPieChart(
                    calcSize(s.handsPlayed),
                    cols,
                    vals
                  ).toCellWithOneChartAndTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding )
                } else {
                  val celllist =
                    List( "", "*", "**" ).map { doubled =>
                      tcss.find( cs => cs.parseContract.doubled == doubled ).map { s =>
                        val con = s.parseContract
                        val suit = if (con.suit == "Z") "N" else con.suit
                        val (cols,vals) = s.histogram.map(cs => (colorMap(cs.tricks), cs.counter.toDouble)).unzip
                        val title = getTitle(s"${con.tricks}${suit}${con.doubled}", s, totalHandsPlayed)
                        DataPieChart(
                            calcSize(s.handsPlayed),
                            cols,
                            vals
                        ).chartWithTitle(title, tooltipPieChartSize, pieChartMaxSizePlusPadding )
                      }.getOrElse( zeroData )
                    }
                  Cell( celllist )
                }
              }
            }
            ( suit, trickdata.toList )
          }
        }.map { entry =>
          val (suit, data) = entry
          val s = suit match {
            case "P" => "Passed out"
            case "N" | "Z" => "No Trump"
            case "S" => "Spades"
            case "H" => "Hearts"
            case "D" => "Diamonds"
            case "C" => "Clubs"
            case _ => "Oops "+suit
          }
          Row( s, data )
        }
      }

      val rowsAll = sortContractStat( statsDataAll, calcSizeAll )
      val rowsDoubled = sortContractStat( statsDataDoubled, calcSizeDoubled )

      val rows = rowsAll.zip(rowsDoubled).map { entry =>
        val (rall, rdoubled) = entry
        val m = Cell( List( DataTagMod( rdoubled.name ) ) )
        Row( rall.name, rall.data:::(m::rdoubled.data) )
      }

      val columnTricks = (1 to 7).map( t => Column( t.toString ) ).toList

      val columns = columnTricks:::( Column("Doubled")::columnTricks )

      val atitle =
        if (aggregateDouble) {
          TagMod(
            "For each contract shows a piechart which aggregates stats for",
            <.br,
            "undoubled, doubled, and redoubled contracts."
          )
        } else {
          TagMod(
            "For each contract there are up to three piecharts for",
            <.br,
            "undoubled, doubled, and redoubled contracts.",
            <.br,
            "A black square indicates no hands with that doubling were played."
          )
        }

      PieChartTable(
        firstColumn = Column("Trump Suit"),
        columns = columns,
        rows = rows,
        header = Some(
          <.tr(
            <.th(
              ^.colSpan := 8,
              "All Contracts"
            ),
            <.th(
              ^.colSpan := 8,
              "Doubled Contracts"
            )
          )
        ),
        footer = Some(
            <.tr(
              <.td(
                ^.colSpan := columns.length + 1,
                atitle,
                <.br,
                "The color indicates the result of the contract.",
                <.br,
                "Blue indicates passed out",
                <.br,
                "Red indicates a down, green a made contract.",
                <.br,
                "Dark green indicates a contract made with no overtricks",
                ColorBar.create(downColors.reverse, madeColors, None, Some(titleDown), Some(titleMade), None)
              )
            )
        ),
        totalRows = None,
        caption = Some( TagMod(
            "Contract Stats",
            AppButton(
                "Aggregate",
                "Aggregate Double",
                ^.onClick --> toggleAggregateDouble,
                BaseStyles.highlight( selected = aggregateDouble )
            )
        ))
      )
    }

    val toggleAggregateDouble = scope.modState( s => s.copy( aggregateDouble = !s.aggregateDouble ) )

    def render( props: Props, state: State ) = {
      <.div(
        dupStyles.divPageStats,
        PopupOkCancel( state.msg, None, Some(cancel) ),

        state.stats match {
          case Some(stats) =>
            TagMod(
              <.div( displayPlayer( stats.playerStats, stats.contractStats) ),
//              <.div( displayTotals( stats.contractStats) ),
              <.div( displayContract( stats.contractStats, state.aggregateDouble ) )
            )
          case None =>
            <.div("Working")
        },
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "Home2", "Home", props.routerCtl.home )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "Summary2", "Summary", props.routerCtl.setOnClick(SummaryView) )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "BoardSets2", "BoardSets", props.routerCtl.setOnClick(BoardSetSummaryView) ),
            " ",
            AppButton( "Movements2", "Movements", props.routerCtl.setOnClick(MovementSummaryView) )
          )
        )
      )
    }

    private var mounted: Boolean = false

    val didMount = Callback {
      mounted = true;
      logger.info("PageSummary.didMount")
      GraphQLMethods.duplicateStats().map { result =>
        scope.withEffectsImpure.modState { s =>
          result match {
            case Right(stats) =>
              s.copy(stats = Some(stats.duplicatestats))
            case Left(error) =>
              s.copy(msg = Some(TagMod("Error getting stats")))
          }
        }
      }
    }

    val willUnmount = Callback {
      mounted = false;
      logger.finer("PageSummary.willUnmount")
    }
  }

  val component = ScalaComponent.builder[Props]("PageStats")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount(scope => scope.backend.willUnmount)
                            .build
}

