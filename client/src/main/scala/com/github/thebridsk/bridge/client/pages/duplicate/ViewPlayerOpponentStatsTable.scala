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
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsData
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsDataSummary
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByWonPct
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairData
import scala.annotation.tailrec
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByPlayed
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationType
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationAsPlayed
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationAsPlayed
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationMP
import com.github.thebridsk.bridge.data.duplicate.suggestion.CalculationIMP
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorBy
import com.github.thebridsk.bridge.data.duplicate.suggestion.Stat
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByPointsPct
import com.github.thebridsk.bridge.data.duplicate.suggestion.ColorByIMP
import com.github.thebridsk.bridge.clientcommon.react.Table
import com.github.thebridsk.bridge.clientcommon.react.Table.Sorter
import com.github.thebridsk.bridge.clientcommon.react.Table.SortableColumn
import com.github.thebridsk.bridge.clientcommon.react.Table.MultiColumnSort
import com.github.thebridsk.bridge.clientcommon.react.Table.Row
import com.github.thebridsk.bridge.data.duplicate.stats.PlayersOpponentsStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerOpponentStat
import com.github.thebridsk.bridge.client.pages.HomePage

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
 * ViewPlayerOpponentStatsTable( routerCtl: BridgeRouter[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerOpponentStatsTable {
  import ViewPlayerOpponentStatsTableInternal._

  case class Props( stats: Option[PlayersOpponentsStats], showPairs: Boolean = false, showNoDataMsg: Boolean = false)

  def apply( stats: Option[PlayersOpponentsStats], showPairs: Boolean = false, showNoDataMsg: Boolean = false ) =
    component(Props(stats,showPairs,showNoDataMsg))

}

object ViewPlayerOpponentStatsTableInternal {
  import ViewPlayerOpponentStatsTable._
  import DuplicateStyles._
  import Table.Sorter._

  val logger = Logger("bridge.ViewPlayerOpponentStatsTable")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
      showHidden: Boolean = false
  )

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

    def getValue( pd: PlayerOpponentStat ): T
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
    new StringColumn( "Player", "Player" )(new PlayerSorter("Player","Opponent")) {
      def getValue( pd: PlayerOpponentStat ) = pd.player
      override
      val useAdditionalDataWhenSorting = true
    },
    new StringColumn( "Opponent", "Opponent" )(new PlayerSorter("Opponent","Player")) {
      def getValue( pd: PlayerOpponentStat ) = pd.opponent
      override
      val useAdditionalDataWhenSorting = true
    }
  )

  val peopleColumns = List[StatColumn[Any]](
    new StringColumn( "Player", "Player" ) { def getValue( pd: PlayerOpponentStat ) = pd.player }
  )

  val columns = List[StatColumn[Any]](
      new PercentColumn( "MPPct", "% MP" ) { def getValue( pd: PlayerOpponentStat ) = pd.wonMP*100.0/pd.totalMP },
      new PercentColumn( "BeatPts", "% Beat" ) { def getValue( pd: PlayerOpponentStat ) = pd.matchesBeat*100.0/pd.matchesPlayed },
      new PercentColumn( "TiedPts", "% Tied" ) { def getValue( pd: PlayerOpponentStat ) = pd.matchesTied*100.0/pd.matchesPlayed },
      new PercentColumn( "LostPts", "% Lost" ) { def getValue( pd: PlayerOpponentStat ) = (pd.matchesPlayed-pd.matchesBeat-pd.matchesTied)*100.0/pd.matchesPlayed },

      new IntColumn( "Won", "WonMP" ) {
        def getValue( pd: PlayerOpponentStat ) = pd.wonMP
      },

      new IntColumn( "TotalMP", "TotalMP" ) {
        def getValue( pd: PlayerOpponentStat ) = pd.totalMP
      },

      new IntColumn( "Played", "Played" ) {
        def getValue( pd: PlayerOpponentStat ) = pd.matchesPlayed
      },

      new IntColumn( "Beat", "Beat" ) {
        def getValue( pd: PlayerOpponentStat ) = pd.matchesBeat
      },

      new IntColumn( "Tied", "Tied" ) {
        def getValue( pd: PlayerOpponentStat ) = pd.matchesTied
      },

      new IntColumn( "Lost", "Lost" ) {
        def getValue( pd: PlayerOpponentStat ) = pd.matchesPlayed-pd.matchesBeat-pd.matchesTied
      },
  )

  def getRow( pd: PlayerOpponentStat, cols: List[StatColumn[Any]] ): Row = {
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

    def render( props: Props, state: State ) = {
      props.stats match {
        case Some(pd) =>

          val sortedpd = pd.sort()

          val allColumns: List[StatColumn[Any]] = (if (props.showPairs) pairColumns else peopleColumns):::
                                                  columns

          val rows =
            if (props.showPairs) {
              sortedpd.players.flatMap( pos => pos.opponents.map( ps => getRow(ps,allColumns) ) )
            } else {
              sortedpd.players.map( pos => getRow( pos.playerTotal(), allColumns))
            }

          <.div(
            if (props.showPairs) dupStyles.viewPairsTable else dupStyles.viewPeopleTable,
            Table(
                allColumns,
                rows,
                initialSort = Some("MPPct"),
//                header = Some(       // additional header above column header row
//                  <.tr( <.td(
//                    ^.colSpan := cols.length,
//                  ))
//                ),
                additionalRows = None,
                caption = Some(
                  TagMod(
                    if (props.showPairs) "Opponents Results" else "Opponents Total Results",
                  )
                )
            )
          )
        case None =>
          <.div(
            if (props.showPairs) dupStyles.viewPairsTable else dupStyles.viewPeopleTable,
            props.showNoDataMsg ?= HomePage.loading
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPlayerOpponentStatsTable")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

