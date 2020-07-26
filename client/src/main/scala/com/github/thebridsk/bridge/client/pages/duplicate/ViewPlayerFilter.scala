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

    def toggleSelected( p: String ): Filter = {
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

    def setFilterDisplayOnly( b: Boolean ): Filter = {
      copy(filterDisplayOnly=b)
    }

    def isPlayerSelected( p: String ): Boolean = {
      selected match {
        case Some(list) => list.find(f => f==p).isDefined
        case None => false
      }
    }

    def isPlayerShown( p: String ): Boolean = {
      selected match {
        case Some(list) => list.find(f => f==p).isDefined
        case None => true
      }
    }

    def selectedPlayers: List[String] = selected.getOrElse( pairsData.map( psd => psd.players ).getOrElse( List()))

    def clearSelected: Filter = copy(selected = None)

    def selectPlayers( players: List[String] ): Filter = {
      val ns = selected.map { sel =>
        (sel:::players).distinct
      }.getOrElse(players)
      copy( selected = if (ns.isEmpty) None else Some(ns) )
    }

    def showFilter( b: Boolean ): Filter = copy( showFilter = b )

    def getNames: List[String] = pairsData.map( p => p.players ).getOrElse(List())
  }

  case class Props( defaultFilter: Filter, onChange: OnChange) {
    def filter = defaultFilter
  }

  def apply( defaultFilter: Filter, onChange: OnChange ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props( defaultFilter,onChange))

}

object ViewPlayerFilterInternal {
  import ViewPlayerFilter._
  import DuplicateStyles._

  val logger: Logger = Logger("bridge.ViewPlayerFilter")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, Unit]) {

    def togglePlayer( p: String ): Callback = scope.props >>= { props =>
      props.onChange( props.filter.toggleSelected(p) )
    }

    def showFilter( b: Boolean ): Callback = scope.props >>= { props =>
      props.onChange( props.filter.showFilter(b) )
    }

    def setFilterDisplayOnly( b: Boolean ): Callback = scope.props >>= { props =>
      props.onChange( props.filter.setFilterDisplayOnly(b) )
    }

    val clearFilter: Callback = scope.props >>= { props =>
      props.onChange( props.filter.clearSelected )
    }

    def selectAllFilter( players: List[String] ): Callback = scope.props >>= { props =>
      props.onChange( props.filter.selectPlayers(players) )
    }

    def render( props: Props ) = { // scalafix:ok ExplicitResultTypes; React

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

  private[duplicate]
  val component = ScalaComponent.builder[Props]("ViewPlayerFilter")
                            .stateless
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

