package com.example.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.routes.BridgeRouter
import com.example.pages.duplicate.DuplicateRouter.DuplicateResultView
import utils.logging.Logger
import com.example.bridge.store.DuplicateResultStore
import com.example.controller.Controller
import com.example.react.AppButton
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.DuplicateResultEditView
import com.example.data.Id
import com.example.data.DuplicateSummaryEntry
import com.example.data.SystemTime
import com.example.bridge.store.NamesStore
import com.example.data.MatchDuplicateResult
import com.example.data.BoardResults
import com.example.rest2.RestClientDuplicateResult
import com.example.bridge.action.BridgeDispatcher
import com.example.react.ComboboxOrInput
import com.example.data.Team
import DuplicateStyles._
import com.example.react.Utils._
import com.example.react.DateUtils
import com.example.react.DateTimePicker
import scala.scalajs.js.Date
import com.example.react.reactwidgets.globalize.Moment
import com.example.react.reactwidgets.globalize.ReactWidgetsMoment
import com.example.react.CheckBox
import com.example.pages.BaseStyles
import com.example.data.MatchDuplicate
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import japgolly.scalajs.react.vdom.TagMod

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageDuplicateResultEdit( PageDuplicateResultEdit.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageDuplicateResultEdit {
  import PageDuplicateResultEditInternal._

  type Callback = ()=>Unit

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: DuplicateResultEditView )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: DuplicateResultEditView ) = component(Props(routerCtl,page))

}

object PageDuplicateResultEditInternal {
  import PageDuplicateResultEdit._

  val logger = Logger("bridge.PageDuplicateResultEdit")

  case class DSE( team: Team, result: String ) {

    def isValid() = {
      try {
        result.toDouble
        team.player1 != null && team.player1 != "" &&
          team.player2 != null && team.player2 != ""
      } catch {
        case _: Exception => false
      }
    }

    def toDuplicateSummaryEntry(useIMP: Boolean) = {
      if (useIMP) {
        DuplicateSummaryEntry( team, None, None, None, Some(result.toDouble), Some(0) )
      } else {
        DuplicateSummaryEntry( team, Some(result.toDouble), Some(0) )
      }
    }
  }

  def toDSE( dupS: DuplicateSummaryEntry, imp: Boolean ) = {
    val v = if (imp) dupS.resultImp.getOrElse(0.0).toString()
            else dupS.result.getOrElse(0.0).toString()
    DSE(dupS.team, v )
  }

  def toTeams( teams: List[List[DSE]] ) = {

  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
                    original: Option[MatchDuplicateResult] = None,    // if None, not initialized
                    teams: List[List[DSE]] = List(),
                    boardresults: Option[List[BoardResults]] = None,
                    played: SystemTime.Timestamp = SystemTime.currentTimeMillis(),
                    comment: Option[String] = None,
                    notfinished: Boolean = false,
                    nameSuggestions: Option[List[String]] = None,         // known names from server
                    useIMP: Boolean = false
                  ) {
    import scala.scalajs.js.JSConverters._
    def getSuggestions = nameSuggestions.getOrElse(List()).toJSArray
    def gettingNames = nameSuggestions.isEmpty

    def updateNames( list: List[String] ) = copy( nameSuggestions = Some(list) )

    def getMDR(): MatchDuplicateResult = {

      val time = SystemTime.currentTimeMillis()
      val t = teams.map(l => l.map( e => e.toDuplicateSummaryEntry(useIMP) ))
      val c = comment match {
        case Some(c) if (c.length()>0) => comment
        case _ => None
      }
      val nf = if (notfinished) Some(true) else None
      val sm = if (useIMP) MatchDuplicate.InternationalMatchPoints else MatchDuplicate.MatchPoints
      original.get.copy( results=t, boardresults=boardresults, comment=c, notfinished=nf, played=played, updated=time, scoringmethod=sm ).fixup()
    }

    def setPlayer( iwinnerset: Int, teamid: Id.Team, iplayer: Int )( name: String ) = {
      copy( teams=
        teams.zipWithIndex.map { e =>
          val (ws,i) = e
          if (i == iwinnerset) {
            ws.map { dse =>
              if (dse.team.id == teamid) {
                val time = SystemTime.currentTimeMillis()
                val nt = if (iplayer == 1) {
                  dse.team.copy( player1=name, updated=time)
                } else {
                  dse.team.copy( player2=name, updated=time)
                }
                dse.copy(team=nt)
              } else {
                dse
              }
            }
          } else {
            ws
          }
        }
      )
    }

    def setPoints( iwinnerset: Int, teamid: Id.Team )( points: String ) = {
      copy( teams=
        teams.zipWithIndex.map { e =>
          val (ws,i) = e
          if (i == iwinnerset) {
            ws.map { dse =>
              if (dse.team.id == teamid) {
                val time = SystemTime.currentTimeMillis()
                dse.copy( result=points)
              } else {
                dse
              }
            }
          } else {
            ws
          }
        }
      )
    }

    def updateOriginal( mdr: Option[MatchDuplicateResult] ) = {
      val imp = mdr.map( m => m.isIMP ).getOrElse(false)
      val t = mdr.map( m => m.results.map( l => l.map( e => toDSE(e,imp)) ) ).getOrElse(List())
      val b = mdr.map( m => m.boardresults ).getOrElse(None)
      val p = mdr.map( m => m.played ).filter( x => x!=0 ).getOrElse(SystemTime.doubleToTimestamp(0))
      val c = mdr.map( m => m.comment ).getOrElse(None)
      val f = mdr.map( m => m.notfinished ).getOrElse(Some(false)).getOrElse(false)
      copy( original=mdr, teams=t, boardresults=b, comment=c, notfinished=f, played=p, useIMP=imp )
    }

    def isValid() = {
      played > 0 &&
      teams.flatten.find( t => !t.isValid() ).isEmpty
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

    val ok = scope.state >>= { state => Callback {
      val props = scope.withEffectsImpure.props
      state.original match {
        case Some(mdr) =>
          val newmdr = state.getMDR()
          logger.fine(s"""Updating, state.played=${state.played} MDR: ${newmdr}""")
          BridgeDispatcher.updateDuplicateResult(newmdr)
          import scala.concurrent.ExecutionContext.Implicits.global
          RestClientDuplicateResult.update(newmdr.id, newmdr).recordFailure().foreach { e =>
            logger.info("Updated "+newmdr)
          }
        case None =>
      }

      if (mounted) props.routerCtl.set(DuplicateResultView(props.page.dupid)).runNow()

    }}

    val cancel = scope.props >>= { props => Callback {

      if (mounted) props.routerCtl.set(DuplicateResultView(props.page.dupid)).runNow()

    }}

    def setPlayer( iwinnerset: Int, teamid: Id.Team, iplayer: Int )( name: String ) = scope.modState( s =>
      s.setPlayer(iwinnerset, teamid, iplayer)(name.trim())
    )

    def setPoints( iwinnerset: Int, teamid: Id.Team )( e: ReactEventFromInput ) = e.inputText( points =>
      scope.modState( s =>
        s.setPoints(iwinnerset, teamid)(points)
      )
    )

    def setPlayed( value: Date ) = {
      logger.fine(s"""Setting date to ${value}: ${value.getTime()}""")
      scope.modState { s =>
        val t = if (value == null) 0 else value.getTime()
        val ns = s.copy( played=t)
        logger.fine(s"""New date in state is ${ns.played}""")
        ns
      }.runNow()
    }

    val toggleComplete = scope.modState( s=>s.copy( notfinished = !s.notfinished ))

    val toggleIMP = scope.modState( s=>s.copy( useIMP = !s.useIMP ))

    def setComment( e: ReactEventFromInput ) =  e.inputText { comment =>
      scope.modState( s=>s.copy( comment = if (comment == null || comment == "") None else Some(comment) ))
    }

    def render( props: Props, state: State ) = {

      def getWinnerSet( iws: Int, ws: List[DSE], tabstart: Int ) = {
        <.table(
          Header(props),
          <.tbody(
            ws.zipWithIndex.map{ entry =>
              val (dse,i) = entry
              val t = tabstart + i*3
              TeamRow.withKey(dse.team.id)((iws,dse.team.id, dse.team.player1, dse.team.player2, dse.result, dse.isValid, this, props, state,t))
            }.toTagMod,
            TotalRow((ws,this,props,state))
          )
        )
      }

      Moment.locale("en")
      ReactWidgetsMoment()

      <.div(
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Edit Duplicate Result",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        DuplicateResultStore.getDuplicateResult() match {
          case Some(dre) if dre.id == props.page.dupid =>
            val finished = !state.notfinished
            val comment = state.comment.getOrElse("")
            TagMod(
              <.div( dupStyles.divDuplicateResultEditPage,
                if (state.original.isEmpty) {
                  <.h1( "Working" )
                } else {
                  TagMod(
                    <.div(
                      <.p( "Played: " ),
                      DateTimePicker("played",
                                     defaultValue=if (state.played==0) Some( new Date() ) else Some(new Date(state.played)),
                                     defaultCurrentDate = Some( new Date() ),
                                     onChange = Some(setPlayed),
                                     disabled = false
                                    ),
                      CheckBox("Complete","Match complete",finished,toggleComplete),
                      CheckBox("IMP","Use IMP",state.useIMP,toggleIMP),
                      <.br,
                      <.label(
                        "Comment: ",
                        <.input(
                          ^.`type` := "text",
                          ^.name := "Comment",
                          ^.value := comment,
                          ^.onChange ==> setComment
                        )
                      )
                    ),
                    state.teams.zipWithIndex.map{ e =>
                      val (ws,i) = e
                      val t = i*ws.length*3
                      getWinnerSet(i,ws,t)
                    }.toTagMod,
                    !state.isValid() ?= <.p("Data not valid"),
                    <.p( "Created: ", DateUtils.formatDate(dre.created), ", updated ", DateUtils.formatDate(dre.updated) )
                  )
                },
                <.div( baseStyles.divFlexBreak ),
                <.div(
                  baseStyles.divFooter,
                  <.div(
                    baseStyles.divFooterLeft,
                    AppButton( "OK", "OK",
                               ^.disabled := !state.isValid(),
                               ^.onClick --> ok
                    )
                  ),
                  <.div(
                    baseStyles.divFooterCenter,
                    AppButton( "Cancel", "Cancel",
                               ^.onClick --> cancel )
                  )
                )
              ),
            )
          case _ =>
            <.div( s"Working" )
        }
      )
    }

    var mounted = false
    val storeCallback = scope.modState { s =>
      val mdr = DuplicateResultStore.getDuplicateResult()
      s.updateOriginal(mdr)
    }

    val namesCallback = scope.modState(s => {
      val sug = NamesStore.getNames
      s.copy( nameSuggestions=Some(sug))
    })

    val didMount = scope.props >>= { (p) => Callback {
      mounted = true
      logger.info("PageDuplicateResultEdit.didMount")
      NamesStore.ensureNamesAreCached(Some(namesCallback))
      DuplicateResultStore.addChangeListener(storeCallback)

      Controller.monitorDuplicateResult(p.page.dupid)
    }}

    val willUnmount = Callback {
      mounted = false
      logger.info("PageDuplicateResultEdit.willUnmount")
      DuplicateResultStore.removeChangeListener(storeCallback)
      Controller.stopMonitoringDuplicateResult()
    }

  }

  val Header = ScalaComponent.builder[Props]("PageDuplicateResultEdit.Header")
                      .render_P( props => {
                        <.thead(
                          <.tr(
                            <.th( "Team"),
                            <.th( "Player1"),
                            <.th( "Player2"),
                            <.th( "Points")
                          )
                        )
                      }).build

  private def noNull( s: String ) = if (s == null) ""; else s

  val TeamRow = ScalaComponent.builder[(Int,Id.Team,String,String,String,Boolean,Backend,Props,State,Int)]("PageDuplicateResultEdit.TeamRow")
                      .render_P( args => {
                        val (iws,id,player1, player2, points, valid, backend, props, state, tabstart) = args
                        val busy = state.gettingNames
                        val names = state.getSuggestions
                        <.tr(
                          <.td( Id.teamIdToTeamNumber(id) ),
                          <.td(
                            <.div(
                              ComboboxOrInput( backend.setPlayer(iws, id, 1), noNull(player1), names, "startsWith", -1, s"P${iws}T${id}P1",
                                               msgEmptyList="No suggested names", msgEmptyFilter="No names matched", id=s"P${iws}T${id}P1")
                            )
                          ),
                          <.td(
                            <.div(
                              ComboboxOrInput( backend.setPlayer(iws, id, 2), noNull(player2), names, "startsWith", -1, s"P${iws}T${id}P2",
                                               msgEmptyList="No suggested names", msgEmptyFilter="No names matched", id=s"P${iws}T${id}P2")
                            )
                          ),
                          <.td(
                            <.input( ^.`type`:="number",
                                     ^.name:=s"P${iws}T${id}PP",
                                     ^.onChange ==> backend.setPoints(iws,id),
                                     ^.value := points.toString()
                            )
                          ),
                          BaseStyles.highlight(required = !valid)
                        )
                      }).build

  val TotalRow = ScalaComponent.builder[(List[DSE],Backend,Props,State)]("PageDuplicateResultEdit.TotalRow")
                      .render_P( args => {
                        val (ws, backend, props, state) = args
                        val total = ws.map { dse =>
                          try {
                            dse.result.toDouble
                          } catch {
                            case _ : Exception => 0
                          }
                        }.foldLeft(0.0)((ac,v)=>ac+v)

                        <.tr(
                          <.td(),
                          <.td(),
                          <.td("Total"),
                          <.td(total)
                        )

                      }).build


  val component = ScalaComponent.builder[Props]("PageDuplicateResultEdit")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

