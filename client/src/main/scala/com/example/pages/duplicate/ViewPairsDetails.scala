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
import com.example.react.AppButton
import com.example.react.Utils._
import com.example.data.duplicate.suggestion.PairsData
import com.example.data.duplicate.suggestion.PairData
import com.example.data.duplicate.suggestion.PairsDataSummary
import com.example.pages.BaseStyles

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
 * ViewPairsDetails( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPairsDetails {
  import ViewPairsDetailsInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false)

  def apply( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false ) =
    component(Props(filter,showNoDataMsg))

}

object PairsDetailsSorting {

  def compareName1( l: PairData, r: PairData ): Int = {
    l.player1.compareTo(r.player1)
  }

  def compareName2( l: PairData, r: PairData ): Int = {
    l.player2.compareTo(r.player2)
  }

  def compareDeclarer( l: PairData, r: PairData ) = {
    r.details match {
      case Some(rd) =>
        l.details.map(ld=> rd.percentDeclared.compareTo(ld.percentDeclared)).getOrElse(1)
      case None =>
        l.details.map(ld => -1).getOrElse(0)
    }
  }

  def compareDefended( l: PairData, r: PairData ) = {
    r.details match {
      case Some(rd) =>
        l.details.map(ld=> rd.percentDefended.compareTo(ld.percentDefended)).getOrElse(1)
      case None =>
        l.details.map(ld => -1).getOrElse(0)
    }
  }

  def comparePassed( l: PairData, r: PairData ) = {
    r.details match {
      case Some(rd) =>
        l.details.map(ld=> rd.percentPassed.compareTo(ld.percentPassed)).getOrElse(1)
      case None =>
        l.details.map(ld => -1).getOrElse(0)
    }
  }

  def compareMade( l: PairData, r: PairData ) = {
    r.details match {
      case Some(rd) =>
        l.details.map(ld=> rd.percentMade.compareTo(ld.percentMade)).getOrElse(1)
      case None =>
        l.details.map(ld => -1).getOrElse(0)
    }
  }

  def compareDown( l: PairData, r: PairData ) = {
    r.details match {
      case Some(rd) =>
        l.details.map(ld=> rd.percentDown.compareTo(ld.percentDown)).getOrElse(1)
      case None =>
        l.details.map(ld=> -1).getOrElse(0)
    }
  }

  def compareAllowedMade( l: PairData, r: PairData ) = {
    r.details match {
      case Some(rd) =>
        l.details.map(ld=> rd.percentAllowedMade.compareTo(ld.percentAllowedMade)).getOrElse(1)
      case None =>
        l.details.map(ld=> -1).getOrElse(0)
    }
  }

  def compareTookDown( l: PairData, r: PairData ) = {
    r.details match {
      case Some(rd) =>
        l.details.map(ld=> rd.percentTookDown.compareTo(ld.percentTookDown)).getOrElse(1)
      case None =>
        l.details.map(ld=> -1).getOrElse(0)
    }
  }

  trait SortBy {
    val compfun: List[(PairData,PairData)=>Int]
    def getPairData( lpd: List[PairData] ) = lpd.sortWith( sort _ )
    def sort( l: PairData, r: PairData ): Boolean = {
      for (c <- compfun) {
        val rc = c(l,r)
        if (rc != 0) return rc<0
      }
      false
    }
  }
  object SortByDeclarer extends SortBy {
    val compfun = List(compareDeclarer, compareName1, compareName2)
  }
  object SortByDefended extends SortBy {
    val compfun = List(compareDefended, compareName1, compareName2)
  }
  object SortByPassed extends SortBy {
    val compfun = List(comparePassed, compareName1, compareName2)
  }
  object SortByMade extends SortBy {
    val compfun = List(compareMade, compareName1, compareName2)
  }
  object SortByDown extends SortBy {
    val compfun = List(compareDown, compareName1, compareName2)
  }
  object SortByAllowedMade extends SortBy {
    val compfun = List(compareAllowedMade, compareName1, compareName2)
  }
  object SortByTookDown extends SortBy {
    val compfun = List(compareTookDown, compareName1, compareName2)
  }
  object SortByName extends SortBy {
    val compfun = List(compareName1, compareName2)

    override
    def getPairData( lpd: List[PairData] ) = {
      val data = lpd:::lpd.map(pd => pd.swapNames).toList
      data.sortWith( sort _ )
    }
  }
}

object ViewPairsDetailsInternal {
  import ViewPairsDetails._
  import DuplicateStyles._
  import PairsDetailsSorting._

  val logger = Logger("bridge.ViewPairsDetails")

  val SummaryHeader = ScalaComponent.builder[(Props,State,Backend)]("PairsHeader")
                        .render_P( args => {
                          val (props,state,backend) = args
                          val sortBy = state.sortBy
                          def button( id: String, name: String, sort: SortBy ) = {
                            AppButton( id, name,
                                       ^.onClick --> backend.setSortBy(sort),
                                       BaseStyles.highlight(selected = sortBy==sort)
                                     )
                          }
                          <.thead(
                            <.tr(
                              <.th( button( "Player1", "Player 1", SortByName) ),
                              <.th( button( "Player2", "Player 2", SortByName) ),
                              <.th( button( "DeclarerPct", "% Declarer", SortByDeclarer) ),
                              <.th( button( "DefendedPct", "% Defended", SortByDefended) ),
                              <.th( button( "PassedPct", "% Passed", SortByPassed) ),
                              <.th( button( "MadePct", "% Made", SortByMade) ),
                              <.th( button( "DownPct", "% Down", SortByDown) ),
                              <.th( button( "AllowedMadePct", "% Allowed Made", SortByAllowedMade) ),
                              <.th( button( "TookDownPct", "% Took Down", SortByTookDown) ),
                              <.th( "Total" )
                            )
                          )
                        }).build


  val SummaryRow = ScalaComponent.builder[(Props,PairData)]("PairsRow")
                      .render_P( props => {
                        val (pr,pd) = props
                        <.tr(
                          <.td( pd.player1 ),
                          <.td( pd.player2 ),
                          pd.details match {
                            case Some(det) =>
                              TagMod(
                                <.td( f"${det.percentDeclared}%.2f" ),
                                <.td( f"${det.percentDefended}%.2f" ),
                                <.td( f"${det.percentPassed}%.2f" ),
                                <.td( f"${det.percentMade}%.2f" ),
                                <.td( f"${det.percentDown}%.2f" ),
                                <.td( f"${det.percentAllowedMade}%.2f" ),
                                <.td( f"${det.percentTookDown}%.2f" ),
                                <.td( s"${det.total}" )
                              )
                            case None =>
                              TagMod(
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td()
                              )
                          }
                        )
                      }).build

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( sortBy: SortBy = SortByMade)

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setSortBy( sortBy: SortBy ) = scope.modState { s => s.copy(sortBy=sortBy) }

    def render( props: Props, state: State ) = {

      props.filter.pairsData match {
        case Some(psd) =>
          val filter = props.filter.selectedPlayers
          val lpd = psd.data.values.filter( pd => filter.contains(pd.player1) && filter.contains(pd.player2) ).toList
          val sorted = state.sortBy.getPairData(lpd)
          <.table(
            ^.id:="Pairs",
            dupStyles.tablePairsDetailsSummary,
            SummaryHeader((props,state,this)),
            <.tbody(
              sorted.filter( pd => pd.details.isDefined ).zipWithIndex.map { e =>
                val (pd,i) = e
                SummaryRow.withKey( s"PD${i}" )((props,pd))
              }.toTagMod
            )
          )
        case None =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= "Waiting for data"
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPairsDetails")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

