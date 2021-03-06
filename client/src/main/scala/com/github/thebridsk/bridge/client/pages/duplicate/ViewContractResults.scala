package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.duplicate.stats.ContractStats
import com.github.thebridsk.bridge.data.duplicate.stats.CounterStat
import scala.annotation.tailrec
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.Table.Column
import com.github.thebridsk.bridge.clientcommon.react.Table
import com.github.thebridsk.bridge.data.duplicate.stats.ContractStat
import com.github.thebridsk.bridge.clientcommon.react.PieChartOrSquareForZero
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import DuplicateStyles._

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * ViewContractResults( ViewContractResults.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object ViewContractResults {
  import ViewContractResultsInternal._

  case class Props(contractStats: ContractStats)

  def apply(
      contractStats: ContractStats
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(contractStats))

}

object ViewContractResultsInternal {
  import ViewContractResults._

  val logger: Logger = Logger("bridge.ViewContractResults")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(aggregateDouble: Boolean = false)

  val pieChartMaxSize = 100
  val tooltipPieChartSize = 150

  def calcSize(max: Int)(handsPlayed: Int): Int = {
    (handsPlayed.toDouble / max * (pieChartMaxSize - 5)).toInt + 5
  }

  def getSuit(suit: String): TagMod = {
    suit match {
      case "P"       => "Passed out"
      case "N" | "Z" => "No Trump"
      case "S"       => "Spades"
      case "H"       => "Hearts"
      case "D"       => "Diamonds"
      case "C"       => "Clubs"
      case _         => "Oops " + suit
    }
  }

  /* *
   * @param data
   * @return a List[List[TagMod]].  The outermost list is suit, the inner is contract tricks
   * Passed shows up as a suit.
   */
  def sortContractStat(
      data: List[ContractStat],
      calculateSize: Int => Int,
      totalHands: Int,
      aggregateDouble: Boolean,
      passPercent: Boolean,
      onlyDoubledContracts: Boolean
  ): List[List[TagMod]] = {
    val bySuit = data.groupBy(cs => cs.parseContract.suit)
    val r =
      List("P", "Z", "S", "H", "D", "C").map { suit =>
        val dataBySuit = bySuit.get(suit).getOrElse(List())
        val rr: List[TagMod] =
          if (suit == "P") {
            // passed out
            // dataBySuit should only have one entry
            val dd: TagMod = if (dataBySuit.isEmpty) {
              TagMod.empty
            } else {
              val pd = dataBySuit.head
              val ltitle = if (passPercent) {
                Right(
                  TagMod(
                    f"""Total: ${pd.handsPlayed} (${100.0 * pd.handsPlayed / totalHands}%.2f%%)"""
                  )
                )
              } else {
                Left(false)
              }
              TrickPieChart(
                histogram = CounterStat(10, pd.handsPlayed) :: Nil,
                title = Some("Passed Out"),
                legendtitle = ltitle,
                size = calculateSize(pd.handsPlayed),
                sizeInLegend = tooltipPieChartSize,
                minSize = pieChartMaxSize
              )
            }
            val row = List(
              getSuit(suit),
              dd,
              TagMod.empty,
              TagMod.empty,
              TagMod.empty,
              TagMod.empty,
              TagMod.empty,
              TagMod.empty
            )
            row
          } else {
            val trickdata = (1 to 7).map { ntricks =>
              val tricks = ntricks.toString()

              val tcss =
                dataBySuit.filter(cs => cs.parseContract.tricks == tricks)

              if (tcss.isEmpty) TagMod.empty
              else {
                if (aggregateDouble) {
                  @tailrec
                  def add(
                      cs: ContractStat,
                      other: List[ContractStat]
                  ): ContractStat = {
                    if (other.isEmpty) cs
                    else {
                      add(
                        cs.add(other.head.copy(contract = cs.contract)),
                        other.tail
                      )
                    }
                  }
                  val s = add(tcss.head, tcss.tail)
                  // s.doubled is not valid after this.
                  val con = s.parseContract
                  val suit = if (con.suit == "Z") "N" else con.suit
                  val title = s"${con.tricks}${suit}"
                  val r: TagMod =
                    TrickPieChart(
                      histogram = s.histogram,
                      title = Some(title),
                      legendtitle = Right(
                        f"""Total: ${s.handsPlayed} (${100.0 * s.handsPlayed / totalHands}%.2f%%)"""
                      ),
                      size = calculateSize(s.handsPlayed),
                      sizeInLegend = tooltipPieChartSize,
                      minSize = pieChartMaxSize
                    )
                  r
                } else {
                  val celllist =
                    (if (onlyDoubledContracts) List("*", "**")
                     else List("", "*", "**")).map { doubled =>
                      tcss
                        .find(cs => cs.parseContract.doubled == doubled)
                        .map { s =>
                          val con = s.parseContract
                          val suit = if (con.suit == "Z") "N" else con.suit
                          val title = s"${con.tricks}${suit}${con.doubled}"
                          TrickPieChart(
                            histogram = s.histogram,
                            title = Some(title),
                            legendtitle = Right(
                              f"""Total: ${s.handsPlayed} (${100.0 * s.handsPlayed / totalHands}%.2f%%)"""
                            ),
                            size = calculateSize(s.handsPlayed),
                            sizeInLegend = tooltipPieChartSize,
                            minSize = pieChartMaxSize
                          )
                        }
                        .getOrElse(
                          PieChartOrSquareForZero(
                            size = -5,
                            slices = List(),
                            colors = List(),
                            chartTitle = None
                          )
                        )
                    }
                  celllist.toTagMod
                }
              }
            }
            getSuit(suit) :: trickdata.map(t => <.div(t)).toList
          }
        rr
      }
    r
  }

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    val toggleAggregateDouble: Callback =
      scope.modState(s => s.copy(aggregateDouble = !s.aggregateDouble))

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

      val maxDown = Math.min(0, props.contractStats.min)
      val maxMade = Math.max(0, props.contractStats.max)

      val statsDataAll = props.contractStats.data

      val statsDataDoubled = statsDataAll.filter(cs =>
        cs.contract.contains("*") || cs.contract == "PassedOut"
      )

      val passedout = statsDataAll.find(cs => cs.contract == "PassedOut")

      val (totalHandsPlayed, maxHandsPlayed) = statsDataAll
        .map(ps => ps.handsPlayed)
        .foldLeft((0, 0))((ac, v) => (ac._1 + v, Math.max(ac._2, v)))
      val (totalDoubledHandsPlayedWithPassed, maxDoubledHandsPlayed) =
        statsDataDoubled
          .map(ps => ps.handsPlayed)
          .foldLeft((0, 0))((ac, v) => (ac._1 + v, Math.max(ac._2, v)))

      val totalDoubledHandsPlayed =
        totalDoubledHandsPlayedWithPassed - passedout
          .map(ps => ps.handsPlayed)
          .getOrElse(0)

      val rowsAll = sortContractStat(
        statsDataAll,
        calcSize(maxHandsPlayed),
        totalHandsPlayed,
        state.aggregateDouble,
        true,
        false
      )
      val rowsDoubled = sortContractStat(
        statsDataDoubled,
        calcSize(maxDoubledHandsPlayed),
        totalDoubledHandsPlayed,
        state.aggregateDouble,
        false,
        true
      )

      val rows = rowsAll.zip(rowsDoubled).map { entry =>
        val (rall, rdoubled) = entry
        rall ::: rdoubled
      }

      val columnTricks = (1 to 7).map(t => Column(t.toString)).toList

      val columns =
        Column("Suit") :: columnTricks ::: (Column("Doubled") :: columnTricks)

      val atitle =
        if (state.aggregateDouble) {
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

      <.div(
        dupStyles.viewContractResults,
        Table(
          columns = columns,
          rows = rows,
          initialSort = None,
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
                TrickPieChart.description(maxDown, maxMade, false)
              )
            )
          ),
          additionalRows = None,
          totalRows = None,
          caption = Some(
            TagMod(
              "Contract Stats",
              AppButton(
                "Aggregate",
                "Aggregate Double",
                ^.onClick --> toggleAggregateDouble,
                BaseStyles.highlight(selected = state.aggregateDouble)
              )
            )
          )
        )
      )
    }

  }

  private[duplicate] val component = ScalaComponent
    .builder[Props]("ViewContractResults")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .build
}
