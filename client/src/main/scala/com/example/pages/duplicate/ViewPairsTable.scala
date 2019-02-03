package com.example.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.routes.BridgeRouter
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.data.SystemTime
import com.example.react.AppButton
import com.example.react.Utils._
import com.example.data.duplicate.suggestion.PairsData
import com.example.data.duplicate.suggestion.PairsDataSummary
import com.example.data.duplicate.suggestion.ColorByWonPct
import com.example.data.duplicate.suggestion.PairData
import scala.annotation.tailrec
import com.example.data.duplicate.suggestion.ColorByPlayed
import com.example.pages.BaseStyles
import com.example.data.duplicate.suggestion.CalculationType
import com.example.data.duplicate.suggestion.CalculationAsPlayed
import com.example.data.duplicate.suggestion.CalculationAsPlayed
import com.example.data.duplicate.suggestion.CalculationMP
import com.example.data.duplicate.suggestion.CalculationIMP
import com.example.data.duplicate.suggestion.ColorBy
import com.example.data.duplicate.suggestion.Stat
import com.example.data.duplicate.suggestion.ColorByPointsPct
import com.example.data.duplicate.suggestion.ColorByIMP
import com.example.react.Table
import com.example.react.Table.Sorter
import com.example.react.Table.SortableColumn
import com.example.react.Table.MultiColumnSort
import com.example.react.Table.Row

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
 * ViewPairsTable( routerCtl: BridgeRouter[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPairsTable {
  import ViewPairsTableInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showPairs: Boolean = false, showNoDataMsg: Boolean = false)

  def apply( filter: ViewPlayerFilter.Filter, showPairs: Boolean = false, showNoDataMsg: Boolean = false ) =
    component(Props(filter,showPairs,showNoDataMsg))

}

object ViewPairsTableInternal {
  import ViewPairsTable._
  import DuplicateStyles._
  import Table.Sorter._

  val logger = Logger("bridge.ViewPairsTable")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
      ignoreResultsOnly: Boolean = false,
      calc: CalculationType = CalculationAsPlayed,
      initialCalc: CalculationType = CalculationAsPlayed,
      showHidden: Boolean = false )

  abstract class StatColumn[T](
      id: String,
      name: String,
      formatter: T=>TagMod,
      hidden: Boolean = false
    )(
      implicit
      sorter: Sorter[T]
    ) extends SortableColumn( id, name, formatter, hidden = hidden )(sorter) {

    val showIn: List[CalculationType] = CalculationAsPlayed::CalculationMP::CalculationIMP::Nil

    def isUsed( c: CalculationType ) = showIn.contains(c)

    def getValue( pd: PairData ): T
  }

  abstract class StringColumn(
      id: String,
      name: String
    )(
      implicit
      sorter: Sorter[String]
    ) extends StatColumn( id, name, (v: String) => v )(sorter) {

    override
    val initialSortOnSelectAscending = true
  }

  abstract class IntColumn(
      id: String,
      name: String
    )(
      implicit
      sorter: Sorter[Int]
    ) extends StatColumn( id, name, (v: Int) => v.toString() )(sorter) {

  }

  abstract class PercentColumn(
      id: String,
      name: String
    )(
      implicit
      sorter: Sorter[Double]
    ) extends StatColumn( id, name, (v: Double) => f"$v%.2f%%" )(sorter) {

  }

  abstract class Float2Column(
      id: String,
      name: String,
      hidden: Boolean = false
    )(
      implicit
      sorter: Sorter[Double]
    ) extends StatColumn( id, name, (v: Double) => f"$v%.2f", hidden=hidden )(sorter) {

  }

  abstract class IMPColumn(
      id: String,
      name: String
    )(
      implicit
      sorter: Sorter[Double]
    ) extends StatColumn( id, name, (v: Double) => f"$v%.1f" )(sorter) {

  }

  abstract class MPColumn(
      id: String,
      name: String
    )(
      implicit
      sorter: Sorter[Double]
    ) extends StatColumn( id, name, (v: Double) => Utils.toPointsString(v) )(sorter) {

  }

  import scala.language.implicitConversions
  implicit def convertToStatsColumn[T]( sc: StatColumn[T] ): StatColumn[Any] = {
    sc.asInstanceOf[StatColumn[Any]]
  }

  val ostring = Ordering[String]

  class PlayerSorter( cols: String* ) extends MultiColumnSort( cols.map(c=>(c,None,false)): _* )(ostring)

  val pairColumns = List[StatColumn[Any]](
    new StringColumn( "Player1", "Player 1" )(new PlayerSorter("Player1","Player2")) {
      def getValue( pd: PairData ) = pd.player1
      override
      val useAdditionalDataWhenSorting = true
    },
    new StringColumn( "Player2", "Player 2" )(new PlayerSorter("Player2","Player1")) {
      def getValue( pd: PairData ) = pd.player2
      override
      val useAdditionalDataWhenSorting = true
    }
  )

  val peopleColumns = List[StatColumn[Any]](
    new StringColumn( "Player", "Player" ) { def getValue( pd: PairData ) = pd.player1 }
  )

  val columns = List[StatColumn[Any]](
      new PercentColumn( "WonPct", "% Won" )(MultiColumnSort.create2(("WonPct",false),("Hidden1",false),("WonPts",false),("Player",true),("Player1",true),("Player2",true))) { def getValue( pd: PairData ) = pd.winPercent },
      new PercentColumn( "WonPts", "% WonPoints" )(MultiColumnSort.create2(("WonPts",false),("Hidden1",false),("WonPct",false),("Player",true),("Player1",true),("Player2",true))) { def getValue( pd: PairData ) = pd.winPtsPercent },
      new PercentColumn( "ScorePct", "% MP" )(MultiColumnSort.create("ScorePct","WonPct")) {
        def getValue( pd: PairData ) = pd.pointsPercent
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationMP::Nil
      },
      // This entry will get replaced in the render method.
      new StatColumn( "NormalizedIMP", "Normalized IMP", (v: Double) => f"$v%.2f" ) {
        def getValue( pd: PairData ) = pd.avgIMP
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationIMP::Nil
      },
      new IMPColumn( "IMP", "IMP" )(MultiColumnSort.create("IMP","WonPct")) {
        def getValue( pd: PairData ) = pd.avgIMP
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationIMP::Nil
      },
      new IntColumn( "Won", "WonMP" ) {
        def getValue( pd: PairData ) = pd.won
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationMP::Nil
      },
      new Float2Column( "WonMPPoints", "WonMPPoints" ) {
        def getValue( pd: PairData ) = pd.wonPts
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationMP::Nil
      },
      new IntColumn( "WonImp", "WonIMP" ) {
        def getValue( pd: PairData ) = pd.wonImp
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationIMP::Nil
      },
      new Float2Column( "WonPtsImp", "WonIMPPoints" ) {
        def getValue( pd: PairData ) = pd.wonImpPts
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationIMP::Nil
      },
      new IntColumn( "Played", "Played" ) {
        def getValue( pd: PairData ) = pd.played
      },
      new IntColumn( "PlayedMP", "PlayedMP" ) {
        def getValue( pd: PairData ) = pd.playedMP
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::Nil
      },
      new IntColumn( "PlayedIMP", "PlayedIMP" ) {
        def getValue( pd: PairData ) = pd.playedIMP
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::Nil
      },
      new IntColumn( "Incomplete", "Incomplete" ) {
        def getValue( pd: PairData ) = pd.incompleteGames
      },
      new PercentColumn( "MaxMPPer" , "Max Points %") {
        def getValue( pd: PairData ) = pd.maxMPPercent
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationMP::Nil
      },
      new MPColumn( "Points", "MP" ) {
        def getValue( pd: PairData ) = pd.points
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationMP::Nil
      },
      new MPColumn( "Total", "TotalMP" ) {
        def getValue( pd: PairData ) = pd.totalPoints
        override
        val showIn: List[CalculationType] = CalculationAsPlayed::CalculationMP::Nil
      }
  )

  def getRow( pd: PairData, cols: List[StatColumn[Any]] ): Row = {
    cols.map { col =>
      col.getValue(pd)
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

    val toggleShowHidden = scope.modState { s => s.copy( showHidden = !s.showHidden ) }

    def setCalc( calc: CalculationType ) = scope.modState { s => s.copy( calc = calc ) }

    def render( props: Props, state: State ) = {
      props.filter.pairsData match {
        case Some(fpd) =>

          val pd =
            if (fpd.calc == state.calc) fpd
            else PairsData( fpd.pastgames, state.calc )

          val pds = if (props.showPairs) {
            val filter = props.filter.selectedPlayers
            pd.data.values.filter( pd => filter.contains(pd.player1) && filter.contains(pd.player2) ).toList
          } else {
            val summary = new PairsDataSummary(pd, ColorByWonPct, props.filter.selected, props.filter.filterDisplayOnly, ColorByPlayed)
            summary.playerTotals.values.toList
          }

          val statPts = new Stat(ColorByPointsPct)
          val statIMP = new Stat(ColorByIMP)

          Stat.addPairs(pds, None, statPts, statIMP)

          val minMP = statPts.min
          val maxMP = statPts.max
          val minIMP = statIMP.min
          val maxIMP = statIMP.max

          val diffMP = {
            if (statPts.n == 0) 0.0
            else maxMP - minMP
          }
          val diffIMP = {
            if (statIMP.n == 0) 0.0
            else maxIMP - minIMP
          }

          val hiddenColumn = new StatColumn( "Hidden1", "Normalize(Point%,IMP)", (v: Double) => f"$v%.2f", !state.showHidden ) {
            def getValue( pd: PairData ) = {
              val mpV = if (diffMP == 0.0) 0.0 else (pd.pointsPercent-minMP)/diffMP
              val mpW = pd.playedMP
              val impV = if (diffIMP == 0.0) 0.0 else (pd.avgIMP-minIMP)/diffIMP
              val impW = pd.playedIMP
              if (mpW+impW == 0) 0.0 else (mpV*mpW+impV*impW)/(mpW+impW)
            }
          }

          val normalizedIMPColumn: StatColumn[Any] = new StatColumn( "NormalizedIMP", "Normalized IMP", (v: Double) => f"$v%.2f" ) {
            def getValue( pd: PairData ) = {
              val r =
              if (diffIMP == 0.0) 0.0
              else 100.0*(pd.avgIMP-minIMP)/diffIMP

//              logger.fine( f"NormalizedIMPColumn ${pd.player1} ${r}%.2f, min=${minIMP}%.2f, max=${maxIMP}%.2f, avgIMP=${pd.avgIMP}%.2f" )
              r
            }
            override
            val showIn: List[CalculationType] = CalculationAsPlayed::CalculationIMP::Nil
          }

          val iCols = columns.map( c => if (c.id == normalizedIMPColumn.id) normalizedIMPColumn else c )

          val hiddenColumns: List[StatColumn[Any]] = hiddenColumn::Nil

          val allColumns: List[StatColumn[Any]] = (if (props.showPairs) pairColumns else peopleColumns):::
                                                  iCols :::
                                                  hiddenColumns

          val cols = allColumns.filter( c => c.isUsed(state.calc) )

          val rows = pds.map( pd => getRow(pd,cols) )

          def additionalRows() = pds.map(pd => getRow(pd.swapNames,cols))

          <.div(
            if (props.showPairs) dupStyles.viewPairsTable else dupStyles.viewPeopleTable,
            Table(
                cols,
                rows,
                initialSort = Some("WonPct"),
//                header = Some(       // additional header above column header row
//                  <.tr( <.td(
//                    ^.colSpan := cols.length,
//                  ))
//                ),
                additionalRows = Some(additionalRows _),
                caption = Some(
                  TagMod(
                    if (props.showPairs) "Pairs Results" else "People Results",
                    " ",
                    AppButton(
                        "CalcPlayed",
                        "As Played",
                        BaseStyles.highlight(selected = state.calc == CalculationAsPlayed),
                        ^.onClick --> setCalc( CalculationAsPlayed )
                    ),
                    AppButton(
                        "CalcMP",
                        "By MP",
                        BaseStyles.highlight(selected = state.calc == CalculationMP),
                        ^.onClick --> setCalc( CalculationMP )
                    ),
                    AppButton(
                        "CalcIMP",
                        "By IMP",
                        BaseStyles.highlight(selected = state.calc == CalculationIMP),
                        ^.onClick --> setCalc( CalculationIMP )
                    ),
                    AppButton(
                        "Hidden",
                        "Hidden",
                        BaseStyles.highlight(selected = state.showHidden),
                        ^.onClick --> toggleShowHidden
                    )
                  )
                )
            )
          )
        case None =>
          <.div(
            if (props.showPairs) dupStyles.viewPairsTable else dupStyles.viewPeopleTable,
            props.showNoDataMsg ?= "Waiting for data"
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPairsTable")
                            .initialStateFromProps { props =>
                              props.filter.pairsData match {
                                case Some(pd) =>
                                  val anyMP = pd.pastgames.find( ds => ds.isMP ).isDefined
                                  val anyIMP = pd.pastgames.find( ds => ds.isIMP ).isDefined
                                  val calc: CalculationType =
                                    if (anyMP == anyIMP) CalculationAsPlayed
                                    else if (anyMP) CalculationMP
                                    else CalculationIMP
                                  State( calc = calc, initialCalc = calc )
                                case None =>
                                  State()
                              }
                            }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

