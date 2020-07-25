package com.github.thebridsk.bridge.client.pages.duplicate


import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.duplicate.suggestion.PairsData
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import com.github.thebridsk.bridge.client.pages.HomePage

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
 * ViewPlayerFilter( routerCtl: BridgeRouter[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerFilter {
  import ViewPlayerFilterInternal._

  type OnChange = Filter => Callback

  case class Filter(
                    pairsData: Option[PairsData] = None,
                    selected: Option[List[String]] = None,
                    showFilter: Boolean = false,
                    filterDisplayOnly: Boolean = true
                  ) {

    def toggleSelected( p: String ) = {
      val ns = selected match {
        case Some(list) =>
          val rlist = list.filter(r => r!=p)
          if (rlist.length==0) None
          else if (rlist.length == list.length) Some(p::list)
          else Some(rlist)
        case None =>
          Some(List(p))
      }
      copy(selected=ns)
    }

    def setFilterDisplayOnly( b: Boolean ) = {
      copy(filterDisplayOnly=b)
    }

    def isPlayerSelected( p: String ) = {
      selected match {
        case Some(list) => list.find(f => f==p).isDefined
        case None => false
      }
    }

    def isPlayerShown( p: String ) = {
      selected match {
        case Some(list) => list.find(f => f==p).isDefined
        case None => true
      }
    }

    def selectedPlayers = selected.getOrElse( pairsData.map( psd => psd.players ).getOrElse( List()))

    def clearSelected = copy(selected = None)

    def selectPlayers( players: List[String] ) = {
      val ns = selected.map { sel =>
        (sel:::players).distinct
      }.getOrElse(players)
      copy( selected = if (ns.isEmpty) None else Some(ns) )
    }

    def showFilter( b: Boolean ) = copy( showFilter = b )

    def getNames = pairsData.map( p => p.players ).getOrElse(List())
  }

  case class Props( defaultFilter: Filter, onChange: OnChange) {
    def filter = defaultFilter
  }

  def apply( defaultFilter: Filter, onChange: OnChange ) =
    component(Props( defaultFilter,onChange))

}

object ViewPlayerFilterInternal {
  import ViewPlayerFilter._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewPlayerFilter")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, Unit]) {

    def togglePlayer( p: String ) = scope.props >>= { props =>
      props.onChange( props.filter.toggleSelected(p) )
    }

    def showFilter( b: Boolean ) = scope.props >>= { props =>
      props.onChange( props.filter.showFilter(b) )
    }

    def setFilterDisplayOnly( b: Boolean ) = scope.props >>= { props =>
      props.onChange( props.filter.setFilterDisplayOnly(b) )
    }

    val clearFilter = scope.props >>= { props =>
      props.onChange( props.filter.clearSelected )
    }

    def selectAllFilter( players: List[String] ) = scope.props >>= { props =>
      props.onChange( props.filter.selectPlayers(players) )
    }

    def render( props: Props ) = {

      props.filter.pairsData match {
        case Some(pds) if !pds.players.isEmpty =>
          val allPlayers = pds.players.sorted

          <.div(
            dupStyles.divPlayerFilter,
            <.ul(
              !props.filter.showFilter ?= baseStyles.alwaysHide,
              allPlayers.zipWithIndex.map { entry =>
                val (p,i) = entry
                <.li(
                  CheckBox( s"KP${i}", p, props.filter.isPlayerSelected(p), togglePlayer(p) )
                )
              }.toTagMod
            ),
            <.div(
              if (props.filter.showFilter) {
                TagMod(
                  AppButton( "HideFilter", "Hide Filter", ^.onClick-->showFilter(false) ),
                  AppButton( "ClearFilter", "Clear Filter", ^.onClick-->clearFilter ),
                  AppButton( "SelectAllFilter", "Select All", ^.onClick-->selectAllFilter(allPlayers) ),
                  CheckBox( "DisplayOnly", "Filter Display Only", props.filter.filterDisplayOnly, setFilterDisplayOnly(!props.filter.filterDisplayOnly) )
                )
              } else {
                AppButton( "ShowFilter", "Show Filter", ^.onClick-->showFilter(true) )
              }
            )
          )
        case Some(pds) =>
          <.div(
            dupStyles.divPlayerFilter,
            "No past duplicate matches were found"
          )
        case None =>
          HomePage.loading
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPlayerFilter")
                            .stateless
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

