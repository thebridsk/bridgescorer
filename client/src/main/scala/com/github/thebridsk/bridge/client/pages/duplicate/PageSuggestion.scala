package com.github.thebridsk.bridge.client.pages.duplicate

import scala.scalajs.js.timers._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.DuplicateSummaryStore
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestionsCalculation
import com.github.thebridsk.bridge.data.duplicate.suggestion.Suggestion
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestions
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicateSuggestions
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.clientcommon.rest2.RequestCancelled
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.duplicate.suggestion.NeverPair
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor


/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageSuggestion( PageSuggestion.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageSuggestion {
  import PageSuggestionInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage] )

  def apply( routerCtl: BridgeRouter[DuplicatePage] ) = component(Props(routerCtl))  // scalafix:ok ExplicitResultTypes; ReactComponent

}

object PageSuggestionInternal {
  import PageSuggestion._
  import DuplicateStyles._

  val logger: Logger = Logger("bridge.PageSuggestion")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( numberPlayers: Option[Int] = Some(8),
                    knownPlayers: Option[List[String]] = None,
                    knownPlayersSelected: List[String] = List(),
                    newPlayers: List[String] = List(),
                    suggestion: Option[DuplicateSuggestions] = None,
                    error: Option[String] = None,
                    showNeverPair: Boolean = false,
                    neverPair: List[NeverPair] = List(),
                    showDetails: Boolean = false
                  ) {
    def isValid: Boolean = {
      val nkp = knownPlayersSelected.length
      val nnp = newPlayers.filter(p => p!="").length
      numberPlayers.getOrElse(-1) == nkp+nnp
    }

    def getSelected: List[String] = knownPlayersSelected:::newPlayers

    def trace( msg: String ): State = {
      logger.fine( s"""${msg}: ${this}""" )
      this
    }

    def neverPairKey( p1: String, p2: String ): NeverPair = {
      if (p1 < p2) NeverPair(p1,p2)
      else NeverPair(p2,p1)
    }

    def isNeverPair( p1: String, p2: String ): Boolean = {
      neverPair.contains(neverPairKey(p1,p2))
    }

    def removeNeverPair( p1: String, p2: String ): State = {
      val key = neverPairKey(p1,p2)
      if (!neverPair.contains(key)) this
      else copy( neverPair = neverPair.filter( p => p!=key) )
    }

    def addNeverPair( p1: String, p2: String ): State = {
      val key = neverPairKey(p1,p2)
      if (neverPair.contains(key)) this
      else copy( neverPair = key::neverPair )
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

    def toggleNeverPairPlayers( p1: String, p2: String ): Callback = scope.modState { s =>
      if ( s.isNeverPair(p1, p2) ) s.removeNeverPair(p1, p2)
      else s.addNeverPair(p1, p2)
    }

    val toggleNeverPair: Callback = scope.modState { s => s.copy( showNeverPair = !s.showNeverPair ) }

    val cancelNeverPair: Callback = scope.modState { s => s.copy( showNeverPair = false, neverPair=List() ) }

    val clearNeverPair: Callback = scope.modState { s => s.copy( neverPair=List() ) }

    val toggleDetails: Callback = scope.modState { s => s.copy( showDetails = !s.showDetails ) }

    def toggleKnownPlayer( p: String ): Callback = scope.modState { s =>
      val kps = if (s.knownPlayersSelected.contains(p)) s.knownPlayersSelected.filter(pp => pp!=p)
                else p::s.knownPlayersSelected
      val error = if (kps.length > s.numberPlayers.getOrElse(0)) Some("Too many players are selected")
                  else None
      val np = if (kps.length + s.newPlayers.length > s.numberPlayers.getOrElse(0)) {
        val keep = s.numberPlayers.getOrElse(0) - kps.length
        if (keep >= 0) s.newPlayers.take(keep) else Nil
      } else {
        s.newPlayers
      }
      s.copy( knownPlayersSelected = kps, newPlayers=np, error=error ).trace(s"""toggleKnownPlayer(${p})""")
    }

    import com.github.thebridsk.bridge.clientcommon.react.Utils._
    def setNumberPlayers( e: ReactEventFromInput ): Callback =  e.inputText { number =>
      scope.modState { s =>
        val error = if (s.knownPlayersSelected.length > s.numberPlayers.getOrElse(0)) Some("Too many players are selected")
                    else None
        s.copy(numberPlayers = Some(number.toInt), error = error ).trace(s"""setNumberPlayer(${number})""")
      }
    }

    def setNewPlayers(i: Int)( e: ReactEventFromInput ): Callback =  e.inputText { p =>
      scope.modState { s =>
        logger.fine(s"""Setting new player ${i} to ${p}""")
        def setListLength( list: List[String], n: Int ): List[String] = {
          if (list.length < n) (list:::List.fill(n)("")).take(n)
          else list
        }
        val list = setListLength( s.newPlayers, i+1 )
        val np = list.zipWithIndex.map { e =>
          val (newp,ii) = e
          if (i == ii) p else newp
        }
        s.copy(newPlayers = np ).trace(s"""setNewPlayer(${i},${p})""")
      }
    }

    val clear: Callback = scope.modState { s => s.copy( knownPlayersSelected=List(), newPlayers=List(), suggestion=None ) }

    val clearError: Callback = scope.modState( s => s.copy(error=None))

    val calculateLocal: Callback = scope.modState { s =>
      setTimeout(0) { Alerter.tryitWithUnit {
        val summary = DuplicateSummaryStore.getDuplicateSummary.getOrElse(List())

        val input = DuplicateSuggestions(s.knownPlayersSelected:::s.newPlayers, 10, neverPair = Some(s.neverPair) )
        val suggestion = DuplicateSuggestionsCalculation.calculate(input, summary)
        suggestion.suggestions match {
          case None =>
            logger.fine("Did not get any suggestions")
          case Some(list) =>
            logger.fine(s"""Suggestions:${list.map(su=>su.toString()).mkString("\n  ","\n  ","")}""")
        }
        if (mounted) {
          scope.withEffectsImpure.modState { ss =>
            ss.copy( suggestion=Some(suggestion), error=None )
          }
        }
      } }
      s.copy(error=Some(s"Calculating best pairing for ${s.getSelected.mkString(", ")}"))
    }

    val calculateServer: Callback = scope.modState { s =>
      val input = DuplicateSuggestions(s.knownPlayersSelected:::s.newPlayers, 10, neverPair = Some(s.neverPair) )
      val result = RestClientDuplicateSuggestions.create(input).recordFailure()
      result.foreach { suggestion =>
        logger.info(s"Got new duplicate suggestion ${suggestion}.  PageSuggestion.mounted=${mounted}")
        if (mounted) {
          scope.withEffectsImpure.modState { ss =>
            ss.copy( suggestion=Some(suggestion), error=None )
          }
        }
      }
      result.failed.foreach( t => {
        t match {
          case x: RequestCancelled =>
          case _ =>
            scope.withEffectsImpure.modState( s => s.copy(error=Some("Failed to calculate suggestion")))
        }
      })

      s.copy(error=Some(s"Calculating best pairing for ${s.getSelected.mkString(", ")}"))
    }

    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React

      val nplayer = state.numberPlayers.getOrElse(0)
      val history = state.suggestion.map { sug =>
                      sug.history.map{ h =>
                        s"Calculated with a history of ${h} matches"
                      }.getOrElse( s"Calculated with an unknown number of matches" )
                    }.getOrElse( s"Have a history of ${DuplicateSummaryStore.getDuplicateSummary.map(l => l.length).getOrElse(0)} matches" )

      val sortedPlayers = (state.knownPlayersSelected:::state.newPlayers).sorted

      val showDetails = state.suggestion.flatMap( s => s.suggestions ).isDefined

      <.div(
        PopupOkCancel( state.error.map(s => TagMod(s)), None, Some(clearError) ),
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Pairs Suggestion",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          dupStyles.divSuggestionPage,
          <.div(
            baseStyles.divText100,
            "Number of players: ",
            "8"
  //          <.input(
  //            ^.`type` := "number",
  //            ^.name := "Number",
  //            ^.value := state.numberPlayers.map( n => n.toString() ).getOrElse(""),
  //            ^.onChange ==> setNumberPlayers
  //          )
          ),
          <.div(
            <.ul(
              state.knownPlayers.whenDefined { list =>
                val x = list.zipWithIndex.map { entry =>
                  val (p,i) = entry
                  <.li(
                    CheckBox( s"KP${i}", p, state.knownPlayersSelected.contains(p), toggleKnownPlayer(p) )
                  )
                }.toTagMod
                x
              }
            )
          ),
          <.div(
            <.ul(
              (0 until state.numberPlayers.getOrElse(0)-state.knownPlayersSelected.length).map { i =>
                <.li(
                  <.input(
                    ^.`type` := "text",
                    ^.name := s"NP${i}",
                    ^.value := (if (i < state.newPlayers.length) state.newPlayers(i) else ""),
                    ^.onChange ==> setNewPlayers(i)
                  )
                )
              }.toTagMod
            )
          ),
          <.div(
            !state.showNeverPair ?= baseStyles.alwaysHide,
            <.table(
              NeverPairHeader(props,state,this,sortedPlayers),
              <.tbody(
                sortedPlayers.zipWithIndex.map { e =>
                  val (p,i) = e
                  NeverPairRow.withKey(s"Player${i}")(( props,state,this,p,sortedPlayers))
                }.toTagMod
              )
            )
          ),
          <.div(
            <.p( history ),
            state.suggestion match {
              case None => <.span()
              case Some(suggestion) =>
                suggestion.suggestions match {
                  case None => <.span()
                  case Some(sugs) =>
                    <.table(
                      SummaryHeader(props,state,this),
                      <.tfoot(
                        <.tr(
                          <.td(),
                          <.td( ^.colSpan:=sugs.head.players.length, "Note: (games since last played, games played together)" ),
                          if (state.showDetails) <.td( ^.colSpan:=8, ^.rowSpan:=2, "The higher the weight the better the pairing is" ) else TagMod()
                        ),
                        <.tr(
                          <.td( ^.colSpan:=sugs.head.players.length+1, suggestion.calcTimeMillis.whenDefined( calcTime => f"Calculation time: ${calcTime}%.0f milliseconds" ) )
                        )
                      ),
                      <.tbody(
                        sugs.zipWithIndex.map { e =>
                          val (su,i) = e
                          SummaryRow.withKey( s"Suggestion${i}" )((props,state,this,i,su))
                        }.toTagMod
                      )
                    )
                }
            }
          ),
          <.div( baseStyles.divFlexBreak ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "Calculate", "Calculate", ^.disabled := !state.isValid, ^.onClick --> calculateServer )
            ),
            <.div(
              baseStyles.divFooterCenter,
              AppButton( "CalculateLocal", "Calculate local", ^.disabled := !state.isValid, ^.onClick --> calculateLocal ),
              AppButton( "Clear", "Clear", state.showNeverPair ?= baseStyles.alwaysHide, ^.onClick --> clear ),
              AppButton( "NeverPair", "Never Pair", state.showNeverPair || !state.isValid ?= baseStyles.alwaysHide, ^.onClick --> toggleNeverPair ),
              AppButton( "ClearNeverPair", "Clear Never Pair", !state.showNeverPair ?= baseStyles.alwaysHide, ^.onClick --> clearNeverPair ),
              AppButton( "CancelNeverPair", "Cancel Never Pair", !state.showNeverPair ?= baseStyles.alwaysHide, ^.onClick --> cancelNeverPair ),
              AppButton(
                  "ToggleDetails",
                  if (state.showDetails) "Hide Details" else "Show Details",
                  !showDetails ?= baseStyles.alwaysHide,
                  ^.onClick --> toggleDetails
              ),
            ),
            <.div(
              baseStyles.divFooterRight,
              AppButton( "Cancel", "Cancel", props.routerCtl.setOnClick(SummaryView) )
            )
          )
        )
      )
    }

    private var mounted = false

    val namesCallback: Callback = scope.modState(s => {
      val sug = NamesStore.getNames
      s.copy( knownPlayers=Some(sug))
    })

    val storeCallback = scope.forceUpdate

    val didMount: Callback = Callback {
      mounted = true
      logger.info("PageSummary.didMount")
      DuplicateSummaryStore.addChangeListener(storeCallback)
      NamesStore.ensureNamesAreCached(Some(namesCallback))
      Controller.getSummary(
          () => scope.withEffectsImpure.modState( s => s.copy(error=Some("Error getting duplicate summary")))
      )
    }

    val willUnmount: Callback = Callback {
      mounted = false
      logger.finer("PageSummary.willUnmount")
      DuplicateSummaryStore.removeChangeListener(storeCallback)
    }

  }

  private[duplicate]
  val SummaryHeader = ScalaComponent.builder[(Props,State,Backend)]("SuggestionHeader")
                        .render_P( args => {
                          val (props,state,backend) = args
                          state.suggestion match {
                            case None =>
                              <.span()
                            case Some(suggestion) =>
                              suggestion.suggestions match {
                                case None =>
                                  <.span()
                                case Some(sug) =>
                                  val e1 = sug.head
                                  <.thead(
                                    <.tr(
                                      <.th( "Suggestion"),
                                      e1.players.zipWithIndex.map { e =>
                                        val (p,i) = e
                                        <.th( s"Pair ${i+1}")
                                      }.toTagMod,
                                      if (state.showDetails) {
                                        TagMod(
                                          <.th("Weight"),
                                          <.th("MinLastPlayed"),
                                          <.th("MaxLastPlayed"),
                                          <.th("TimesPlayed"),
                                          <.th("AveLastPlayed"),
                                          <.th("AveTimesPlayed"),
                                          <.th("LastAll"),
                                          <.th("MinLastAll")
                                        )
                                      } else {
                                        TagMod()
                                      }
                                    )
                                  )
                              }
                          }
                        }).build


  private[duplicate]
  val SummaryRow = ScalaComponent.builder[(Props,State,Backend,Int,Suggestion)]("SuggestionRow")
                      .render_P( args => {
                        // row is zero based
                        val (props,state,backend,row,sug) = args

                        <.tr(
                          <.td( s"${row+1}" ),
                          sug.players.sortWith { (l,r) =>
                            if (l.lastPlayed==r.lastPlayed) l.timesPlayed<r.timesPlayed
                            else l.lastPlayed<r.lastPlayed
                          }.map { p =>
                            <.td( s"${p.player1}-${p.player2} (${p.lastPlayed},${p.timesPlayed})")
                          }.toTagMod,
                          if (state.showDetails) {
                            TagMod(
                              <.td(f"${sug.weight}%6.4f"),
                              sug.weights.map( w => <.td(f"${w}%6.4f") ).toTagMod
                            )
                          } else {
                            TagMod()
                          }
                        )
                      }).build

  private[duplicate]
  val NeverPairHeader = ScalaComponent.builder[(Props,State,Backend,List[String])]("SuggestionHeader")
                        .render_P( args => {
                          val (props,state,backend,allplayers) = args

                          <.thead(
                            <.tr(
                              <.th( "Never Pair" ),
                              allplayers.map(p => <.th(p)).toTagMod
                            )
                          )
                        }).build


  private[duplicate]
  val NeverPairRow = ScalaComponent.builder[(Props,State,Backend,String,List[String])]("SuggestionRow")
                      .render_P( args => {
                        // row is zero based
                        val (props,state,backend,player,allplayers) = args

                        <.tr(
                          <.td( player ),
                          allplayers.map { p =>
                            <.td(
                              ^.onClick --> backend.toggleNeverPairPlayers( player, p ),
                              state.isNeverPair(player, p) ?= TagMod( baseStyles.buttonSelected, "X" )
                            )
                          }.toTagMod
                        )
                      }).build

  private[duplicate]
  val component = ScalaComponent.builder[Props]("PageSuggestion")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

