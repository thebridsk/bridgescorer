package com.github.thebridsk.bridge.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.routes.BridgeRouter
import com.github.thebridsk.bridge.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.controller.Controller
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.bridge.react.Utils._
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsData
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsDataSummary
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByWonPct
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairData
import scala.annotation.tailrec
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByPlayed
import com.github.thebridsk.bridge.pages.BaseStyles
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationType
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationAsPlayed
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationAsPlayed
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationMP
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationIMP
import com.github.thebridsk.bridge.data.DuplicateSummaryDetails
import com.github.thebridsk.bridge.react.Table
import com.github.thebridsk.bridge.react.Table.Sorter
import com.github.thebridsk.bridge.react.Table.SortableColumn
import com.github.thebridsk.bridge.react.Table.MultiColumnSort
import com.github.thebridsk.bridge.react.Table.Row

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
 * ViewPairsMadeDownTable( routerCtl: BridgeRouter[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPairsMadeDownTable {
  import ViewPairsMadeDownTableInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showPairs: Boolean = false, showNoDataMsg: Boolean = false)

  def apply( filter: ViewPlayerFilter.Filter, showPairs: Boolean = false, showNoDataMsg: Boolean = false ) =
    component(Props(filter,showPairs,showNoDataMsg))

}

object ViewPairsMadeDownTableInternal {
  import ViewPairsMadeDownTable._
  import DuplicateStyles._
  import Table.Sorter._

  val logger = Logger("bridge.ViewPairsMadeDownTable")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( ignoreResultsOnly: Boolean = false )

  abstract class StatColumn[T](
      id: String,
      name: String,
      formatter: T=>TagMod
    )(
      implicit
      sorter: Sorter[T]
    ) extends SortableColumn( id, name, formatter )(sorter) {

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
      name: String
    )(
      implicit
      sorter: Sorter[Double]
    ) extends StatColumn( id, name, (v: Double) => f"$v%.2f" )(sorter) {

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
      new PercentColumn( "DeclarerPct", "% Declarer" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentDeclared ).getOrElse(0.0)
      },
      new PercentColumn( "DefendedPct", "% Defended" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentDefended ).getOrElse(0.0)
      },
      new PercentColumn( "PassedPct", "% Passed" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentPassed ).getOrElse(0.0)
      },
      new PercentColumn( "MadePct", "% Made" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentMade ).getOrElse(0.0)
      },
      new PercentColumn( "DownPct", "% Down" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentDown ).getOrElse(0.0)
      },
      new PercentColumn( "AllowedMadePct", "% Allowed Made" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentAllowedMade ).getOrElse(0.0)
      },
      new PercentColumn( "TookDownPct", "% Took Down" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentTookDown ).getOrElse(0.0)
      },
      new PercentColumn( "GoodResult", "% Good" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.percentTookDown*d.percentDefended/100+d.percentMade*d.percentDeclared/100 ).getOrElse(0.0)
      },
      new IntColumn( "Total", "Total" ) {
        def getValue( pd: PairData ) = pd.details.map( d => d.total ).getOrElse(0)
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

    def render( props: Props, state: State ) = {
      props.filter.pairsData match {
        case Some(pd) =>
          val cols = (if (props.showPairs) pairColumns else peopleColumns):::columns

          val pds = if (props.showPairs) {
            val filter = props.filter.selectedPlayers
            pd.data.values.filter( pd => filter.contains(pd.player1) && filter.contains(pd.player2) ).toList
          } else {
            val summary = new PairsDataSummary(pd, ColorByWonPct, props.filter.selected, props.filter.filterDisplayOnly, ColorByPlayed)
            summary.playerTotals.values.toList
          }.filter( pd => pd.details.isDefined )

          val totals = if (props.showPairs) {
            None
          } else {
            val tpd = pds.map( pd => pd.details ).foldLeft( DuplicateSummaryDetails.zero("Totals")) { (ac,v) =>
              v.map( vv => ac.add(vv) ).getOrElse(ac)
            }
            val totalpd = pds.head.copy( player1="Totals", player2="", details = Some(tpd) )
            Some(List(totalpd).map( pd => getRow(pd,cols) ) )
          }

          val rows = pds.map( pd => getRow(pd,cols) )

          def additionalRows() = pds.map(pd => getRow(pd.swapNames,cols))

          <.div(
            if (props.showPairs) dupStyles.viewPairsDetailsTable else dupStyles.viewPeopleDetailsTable,
            Table(
                cols,
                rows,
                Some("GoodResult"),
                additionalRows = Some(additionalRows _),
                totalRows = totals,
                caption = Some( TagMod(if (props.showPairs) "Pairs Hand Results" else "Player Hand Results") )
            )
          )
        case None =>
          <.div(
            if (props.showPairs) dupStyles.viewPairsDetailsTable else dupStyles.viewPeopleDetailsTable,
            props.showNoDataMsg ?= "Waiting for data"
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPairsMadeDownTable")
                            .initialStateFromProps { props =>
                              props.filter.pairsData match {
                                case Some(pd) =>
                                  val anyMP = pd.pastgames.find( ds => ds.isMP ).isDefined
                                  val anyIMP = pd.pastgames.find( ds => ds.isIMP ).isDefined
                                  val calc: CalculationType =
                                    if (anyMP == anyIMP) CalculationAsPlayed
                                    else if (anyMP) CalculationMP
                                    else CalculationIMP
                                  State( )
                                case None =>
                                  State()
                              }
                            }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

