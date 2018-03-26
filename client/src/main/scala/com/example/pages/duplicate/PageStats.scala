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
import org.scalajs.dom.ext.Color
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
import com.example.react.HSLColor
import com.example.react.FixedColorBar

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

  val zeroData = Data[Int](-5,Nil,Nil,None)
  val zeroDataList = List(zeroData)
  val zeroData3List = List(zeroData,zeroData,zeroData)
  val zeroList = List[Data[Int]]()

  val suitSortOrder = "PNZSHDC"

  val yellow = HSLColor( 60, 1, .5 )
  val orange = HSLColor( 30, 1, .5 )
  val purple = HSLColor( 300, 1, .5 )
  val cyan = Color.Cyan

  val ctColors = yellow::orange::purple::cyan::Color.Blue::Nil
  val typeOrder: List[ContractType] = ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypePassed::Nil

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend( scope: BackendScope[Props, State]) {

    def cancel = scope.modState( s => s.copy(msg = None) )

    def displayPlayer( stats: PlayerStats ) = {

      val numberDown = Math.max( 0, -stats.min )
      val numberMade = Math.max( 0, stats.max+1 )

      val titleDown = if (numberDown > 0) {
        (-numberDown until 0).map( i => s"Down ${-i}" ).toList
      } else {
        List()
      }
      val titleMade = (0 until numberMade).map( i => if (i==0) "Made" else s"Made ${i}" ).toList

      val downColors = ColorBar.colors( 0, 0.25, numberDown ).reverse
      val madeColors = ColorBar.colors( 120, 0.25, numberMade )

      def colorMap( i: Int ) = {
        // 10 tricks indicates passed out hand

        if (i < 0) downColors( -i-1 )
        else if (i == 10) Color.Blue
        else if (i > 10) {
          ctColors(i-11)
        } else {
          madeColors( i )
        }
      }

      val maxHandsPlayed = (stats.declarer.map( ps => ps.handsPlayed ):::stats.defender.map( ps => ps.handsPlayed )).foldLeft(0)(Math.max _)

      def calcSize( handsPlayed: Int, max: Int ) = {
        (handsPlayed.toDouble/max*75).toInt + 5
      }

      val players = (stats.declarer.map( ps => ps.player ):::stats.defender.map( ps => ps.player )).distinct.sorted

      def getCT( ct: ContractType, list: List[PlayerStat] ) = {
        if (list.isEmpty) None
        else if (ct == ContractTypeTotal) {

          // ignore passed hands in total
          def fix( h: PlayerStat ) = {
            val (hp,hist) = if (h.contractType == ContractTypePassed) {
              (0,List())
            } else {
              (h.handsPlayed,h.histogram)
            }
            h.copy(h.player, h.declarer, ContractTypeTotal.value, hist, hp)
          }

          @tailrec
          def add( sum: PlayerStat, l: List[PlayerStat] ): PlayerStat = {
            if (l.isEmpty) sum
            else {
              val h = l.head
              add( sum.add(fix(h)), l.tail )
            }
          }

          val h = list.head
          val cc = add( fix(h), list.tail ).normalize

          Some(cc)

        } else {
          list.find( ps => ps.contractType == ct.value ).map { ct =>
            if (ct.contractType == ContractTypePassed) {
              // 10 tricks indicates passed out hand
              ct.copy( histogram=List(CounterStat(10,ct.handsPlayed)), handsPlayed=ct.handsPlayed)
            } else {
              ct
            }
          }
        }
      }

      val maxHandsPlayedTotal = players.flatMap { p =>
        val (declarer, defender) = stats.stats(p)
        getCT(ContractTypeTotal,declarer).toList:::getCT(ContractTypeTotal,defender).toList
      }.map( ps => ps.handsPlayed ).foldLeft(0)(Math.max _)

      val order: List[ContractType] = ContractTypePassed::ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypeTotal::Nil

      val rows = players.map { p =>
        val (declarer, defender) = stats.stats(p)

        val data =
          order.flatMap { ct =>
            ((getCT(ct,declarer),true)::(getCT(ct,defender),false)::Nil).map { entry =>
              val (stat, decl) = entry
              stat match {
                case Some(s) =>
                  if (s.handsPlayed == 0) {
                  zeroList
                  } else {
                    val (cols,vals) = s.histogram.map(cs => (cs.tricks, cs.counter.toDouble)).unzip
                    if (ct == ContractTypePassed) {
                      val title = s"${s.player} in ${ct}\nPassed: ${s.handsPlayed}"
                      List(Data[Int]( calcSize(s.handsPlayed, maxHandsPlayed) , 10::Nil, s.handsPlayed.toDouble::Nil, Some(title) ))
                    } else {
                      val pre = s"${s.player} in ${ct} as ${if (s.declarer) "Declarer" else "Defender"}"
                      val title = s"${pre}\nTotal: ${s.handsPlayed}"+s.histogram.sortBy(cs=>cs.tricks).map { cs =>
                        val percent = 100.0*cs.counter/s.handsPlayed
                        if (cs.tricks < 0) f"  Down ${-cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
                        else if (cs.tricks == 0) f"  Made   : ${cs.counter} (${percent}%.2f%%)"
                        else if (cs.tricks == 10) f"  Passed : ${cs.counter} (${percent}%.2f%%)"
                        else f"  Made +${cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
                      }.mkString("\n","\n","")
                      List(Data[Int]( calcSize(s.handsPlayed, if (ct==ContractTypeTotal) maxHandsPlayedTotal else maxHandsPlayed) , cols, vals, Some(title) ))
                    }

                  }
                case None =>
                  zeroList
              }
            }
          }

        val passedout = defender.find( ps => ps.contractType == ContractTypePassed.value).getOrElse( PlayerStat("Passed",false,ContractTypePassed.value) )

        def byType( list: List[PlayerStat], playedAs: String ) = {
          val or: List[ContractType] = ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::Nil
          val dd = or.zipWithIndex.map { entry =>
            val (ct,i) = entry
            ( ct, list.find( ps => ps.contractType==ct.value ).map( ps => ps.handsPlayed ).getOrElse(0).toDouble, i+11 )
          }
          val sum = dd.foldLeft(0.0)((ac,v) => ac+v._2) + passedout.handsPlayed
          val (cts,values,cols) = dd.unzip3
          val title = s"Types of hands as ${playedAs}"+dd.map { entry =>
            val (ct,value, col) = entry
            f"${ct.toString()}: ${value} (${100.0*value/sum}%.2f%%)"
          }.mkString("\n  ","\n  ","\n  ")+f"${ContractTypePassed.toString()}: ${passedout.handsPlayed} (${100.0*passedout.handsPlayed/sum}%.2f%%)"
          List(Data[Int]( calcSize( sum.toInt, maxHandsPlayedTotal ), cols:::(15::Nil), values:::(passedout.handsPlayed.toDouble::Nil), Some(title) ))
        }

        Row( p, byType(declarer,"Declarer")::byType(defender,"Defender")::(data.drop(1)) )   // drop passed declarer
      }

      val columns = Column(TagMod("Type by",<.br,"Declarer"))::Column(TagMod("Type by",<.br,"Defender"))::Column("Passed"):: order.filter(p => p!=ContractTypePassed).flatMap { ct =>
        val c = ct.toString()
        Column(TagMod(c,<.br,"Declarer"))::Column(TagMod(c,<.br,"Defender"))::Nil
      }

      PieChartTable(
        firstColumn = Column("Player"),
        columns = columns,
        rows = rows,
        colorMap = colorMap,
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
                ColorBar( 0, 0.25, numberDown, true, 120, 0.25, numberMade, true, false, Some(titleDown), Some(titleMade), None ),
                "For the Type columns the colors are:",
                FixedColorBar( ctColors, Some(typeOrder.map( ct => ct.toString() )) )
              )
            )
        ),
        totalRows = None,
        caption = Some( "Player Stats" )
      )
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

      val downColors = ColorBar.colors( 0, 0.25, numberDown ).reverse
      val madeColors = ColorBar.colors( 120, 0.25, numberMade )

      def colorMap( i: Int ) = {
        // 10 tricks indicates passed out hand

        if (i < 0) downColors( -i-1 )
        else if (i == 10) Color.Blue
        else if (i > 10) {
          ctColors(i-11)
        } else {
          madeColors( i )
        }
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


      def calcSize( handsPlayed: Int, max: Int ) = {
        (handsPlayed.toDouble/max*75).toInt + 5
      }

      def byType( list: List[ContractStat] ) = {
        val or: List[ContractType] = ContractTypePartial::ContractTypeGame::ContractTypeSlam::ContractTypeGrandSlam::ContractTypePassed::Nil
        val dd = or.zipWithIndex.map { entry =>
          val (ct,i) = entry
          ( ct, list.find( ps => ps.contractType==ct.value ).map( ps => ps.handsPlayed ).getOrElse(0).toDouble, i+11 )
        }
        val sum = dd.foldLeft(0.0)((ac,v) => ac+v._2)
        val (cts,values,cols) = dd.unzip3
        val title = s"Types of hands"+dd.map { entry =>
          val (ct,value, col) = entry
          f"${ct.toString()}: ${value} (${100*value/sum}%.2f%%)"
        }.mkString("\n  ","\n  ","")
        List(Data[Int]( calcSize( sum.toInt, maxHandsPlayedTotal ), cols, values, Some(title) ))
      }

      val first = byType( totalStats.take(totalStats.length-1))

      val data = first::
        totalStats.zip(order).map { entry =>
          val (s,ct) = entry
          if (s.handsPlayed == 0) {
            zeroList
          } else {
            val (cols,vals) = s.histogram.map(cs => (cs.tricks, cs.counter.toDouble)).unzip
            if (s.contractType == ContractTypePassed.value) {
              val title = f"Passed: ${s.handsPlayed} ${100.0*s.handsPlayed/maxHandsPlayedTotal}%.2f%%"
              List(Data[Int]( calcSize(s.handsPlayed, maxHandsPlayed) , 10::Nil, s.handsPlayed.toDouble::Nil, Some(title) ))
            } else {
              val pre = f"${ct} ${100.0*s.handsPlayed/maxHandsPlayedTotal}%.2f%%"
              val title = s"${pre}\nTotal: ${s.handsPlayed}"+s.histogram.sortBy(cs=>cs.tricks).map { cs =>
                val percent = 100.0*cs.counter/s.handsPlayed
                if (cs.tricks < 0) f"  Down ${-cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
                else if (cs.tricks == 0) f"  Made   : ${cs.counter} (${percent}%.2f%%)"
                else if (cs.tricks == 10) f"  Passed : ${cs.counter} (${percent}%.2f%%)"
                else f"  Made +${cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
              }.mkString("\n","\n","")
              List(Data[Int]( calcSize(s.handsPlayed, if (ct==ContractTypeTotal) maxHandsPlayedTotal else maxHandsPlayed) , cols, vals, Some(title) ))
            }

          }
        }

      val row = Row( "Total", data )

      PieChartTable(
        firstColumn = Column("All Hands"),
        columns = columns,
        rows = List(row),
        colorMap = colorMap,
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
                ColorBar( 0, 0.25, numberDown, true, 120, 0.25, numberMade, true, false, Some(titleDown), Some(titleMade), None ),
                "For the Type columns the colors are:",
                FixedColorBar( ctColors, Some(typeOrder.map( ct => ct.toString() )) )
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

      val downColors = ColorBar.colors( 0, 0.25, numberDown ).reverse
      val madeColors = ColorBar.colors( 120, 0.25, numberMade )

      /* *
       * @param i - the number of overtricks, +, or undertricks, -, or 0 if exactly made
       *            10 if passed out
       * @return the color
       */
      def colorMap( i: Int ) = {
        if (i == 10) Color.Blue
        else if (i < 0) downColors( -i-1 )
        else madeColors( i )
      }

      val ( totalHandsPlayed, maxHandsPlayed) = stats.data.map( ps => ps.handsPlayed ).foldLeft((0,0))( (ac,v) => ( ac._1+v, Math.max(ac._2,v)) )

      def calcSize( handsPlayed: Int ) = {
        if (handsPlayed == 0) -5
        else (handsPlayed.toDouble/maxHandsPlayed*75).toInt + 5
      }

      /* *
       * @param data
       * @return a List[List[List[ContractStat]]].  The outermost list is suit, the middle is contract tricks, inner is doubled.
       * Passed shows up as a suit.
       */
      def sortContractStat( data: List[ContractStat] ) = {
        data.groupBy(cs=> cs.parseContract.suit).map { entryBySuit =>
          val (suit, dataBySuit) = entryBySuit
          if (suit == "P") {
            // passed out
            // dataBySuit should only have one entry
            val pd = dataBySuit.head
            val title = f"Passed Out: ${pd.handsPlayed} (${100.0*pd.handsPlayed/totalHandsPlayed}%.2f%%)"
            val dd = Data[Int]( calcSize( pd.handsPlayed), 10::Nil, 1.0::Nil, Some(title) )
            ( suit, List( List(dd), zeroList, zeroList, zeroList, zeroList, zeroList, zeroList ) )
          } else {
            val trickdata = (1 to 7).map { ntricks =>
              val tricks = ntricks.toString()

              val tcss = dataBySuit.filter( cs => cs.parseContract.tricks == tricks )

              if (tcss.isEmpty) zeroList
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
                  val (cols,vals) = s.histogram.map(cs => (cs.tricks, cs.counter.toDouble)).unzip
                  val con = s.parseContract
                  val suit = if (con.suit == "Z") "N" else con.suit
                  val title = f"${con.tricks}${suit} (${100.0*s.handsPlayed/totalHandsPlayed}%.2f%%)\nTotal: ${s.handsPlayed}"+s.histogram.sortBy(cs=>cs.tricks).map { cs =>
                    val percent = 100.0*cs.counter/s.handsPlayed
                    if (cs.tricks < 0) f"  Down ${-cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
                    else if (cs.tricks == 0) f"  Made   : ${cs.counter} (${percent}%.2f%%)"
                    else f"  Made +${cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
                  }.mkString("\n","\n","")
                  List( Data[Int]( calcSize(s.handsPlayed) , cols, vals, Some(title) ) )
                } else {
                  List( "", "*", "**" ).map { doubled =>
                    tcss.find( cs => cs.parseContract.doubled == doubled ).map { s =>
                      val con = s.parseContract
                      val suit = if (con.suit == "Z") "N" else con.suit
                      val (cols,vals) = s.histogram.map(cs => (cs.tricks, cs.counter.toDouble)).unzip
                      val title = f"${con.tricks}${suit}${con.doubled} (${100.0*s.handsPlayed/totalHandsPlayed}%.2f%%)\nTotal: ${s.handsPlayed}"+s.histogram.sortBy(cs=>cs.tricks).map { cs =>
                        val percent = 100.0*cs.counter/s.handsPlayed
                        if (cs.tricks < 0) f"  Down ${-cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
                        else if (cs.tricks == 0) f"  Made   : ${cs.counter} (${percent}%.2f%%)"
                        else f"  Made +${cs.tricks}: ${cs.counter} (${percent}%.2f%%)"
                      }.mkString("\n","\n","")
                      Data[Int]( calcSize(s.handsPlayed) , cols, vals, Some(title) )
                    }.getOrElse( zeroData )
                  }
                }
              }
            }
            ( suit, trickdata.toList )
          }
        }.toList.sortWith{ (l,r) =>
          val il = suitSortOrder.indexOf(l._1)
          val ir = suitSortOrder.indexOf(r._1)
          il < ir
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

      val rows = sortContractStat( stats.data )

      val columns = (1 to 7).map( t => Column( t.toString ) ).toList

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
        firstColumn = Column("Player"),
        columns = columns,
        rows = rows,
        colorMap = colorMap,
        header = None,
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
                ColorBar( 0, 0.25, numberDown, true, 120, 0.25, numberMade, true, false, Some(titleDown), Some(titleMade), None )
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

    def toggleAggregateDouble = scope.modState( s => s.copy( aggregateDouble = !s.aggregateDouble ) )

    def render( props: Props, state: State ) = {
      <.div(
        dupStyles.divPageStats,
        PopupOkCancel( state.msg, None, Some(cancel) ),

        state.stats match {
          case Some(stats) =>
            TagMod(
              <.div( displayPlayer( stats.playerStats) ),
              <.div( displayTotals( stats.contractStats) ),
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

    def didMount() = Callback {
      mounted = true;
      logger.info("PageSummary.didMount")
      GraphQLMethods.duplicateStats().map { result =>
        result match {
          case Right(stats) =>
            scope.withEffectsImpure.modState { s => s.copy(stats = Some(stats.duplicatestats)) }
          case Left(error) =>
            scope.withEffectsImpure.modState { s => s.copy(msg = Some(TagMod("Error getting stats"))) }
        }
      }
    }

    def willUnmount() = Callback {
      mounted = false;
      logger.finer("PageSummary.willUnmount")
    }
  }

  val component = ScalaComponent.builder[Props]("PageStats")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount(scope => scope.backend.willUnmount())
                            .build
}

