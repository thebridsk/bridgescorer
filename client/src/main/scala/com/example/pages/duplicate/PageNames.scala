package com.example.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.routes.BridgeRouter
import com.example.routes.AppRouter.AppPage
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.bridge.store.DuplicateStore
import com.example.data.Team
import com.example.pages.duplicate.DuplicateRouter.NamesView
import com.example.react.AppButton
import com.example.react.ComboboxOrInput
import com.example.bridge.store.NamesStore
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageNames( routerCtl: BridgeRouter[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageNames {
  import PageNamesInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: NamesView, returnPage: DuplicatePage )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: NamesView, returnPage: DuplicatePage ) = component(Props(routerCtl,page,returnPage))

}

object PageNamesInternal {
  import PageNames._

  val logger = Logger("bridge.PageNames")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( teams: Map[Id.Team, Team]=Map(), nameSuggestions: Option[List[String]] = None ) {
    import scala.scalajs.js.JSConverters._

    def reset() =
      DuplicateStore.getMatch() match {
        case Some(md) => copy(teams=md.teams.map(t=> t.id->t).toMap)
        case None => copy(teams=Map())
      }

    def getSuggestions = nameSuggestions.getOrElse(List()).toJSArray
    def gettingNames = nameSuggestions.isEmpty

  }

  val Header = ScalaComponent.builder[Props]("PageNames.Header")
                      .render_P( props => {
                        <.thead(
                          <.tr(
                            <.th( "Team"),
                            <.th( "Player1"),
                            <.th( "Player2")
                          )
                        )

                      }).build

  private def noNull( s: String ) = if (s == null) ""; else s
  private def playerValid( s: String ) = s!=null && s.length!=0

  val TeamRow = ScalaComponent.builder[(Team,Backend,State,Props)]("PageNames.TeamRow")
                      .render_P( args => {
                        val (team, backend, st, p) = args
                        val busy = st.gettingNames
                        val names = st.getSuggestions
                        logger.fine( s"""busy=${busy}, names=${names}""")
                        <.tr(
                          <.td( Id.teamIdToTeamNumber(team.id)),
                          <.td(
                              ComboboxOrInput( p => backend.setPlayer(team.id, 1)(p), noNull(team.player1), names, "startsWith", -1, "I_"+team.id+"_1",
                                               msgEmptyList="No suggested names", msgEmptyFilter="No names matched", id="I_"+team.id+"_1",
                                               busy=busy )
                          ),
                          <.td(
                              ComboboxOrInput( p => backend.setPlayer(team.id, 2)(p), noNull(team.player2), names, "startsWith", -1, "I_"+team.id+"_2",
                                               msgEmptyList="No suggested names", msgEmptyFilter="No names matched", id="I_"+team.id+"_2",
                                               busy=busy )
                          )
                        )
                      }).build


  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def render( props: Props, state: State ) = {
      import DuplicateStyles._
      logger.info("Rendering "+props.page+" suggestions="+state.nameSuggestions)
      <.div(
        dupStyles.divNamesPage,
        DuplicatePageBridgeAppBar(
          id = Some(props.page.dupid),
          tableIds = List(),
          pageMenuItems = List[CtorType.ChildArg](),
          title = Seq[CtorType.ChildArg](MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Edit Names",
                    )
                )),
          helpurl = "/help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        DuplicateStore.getMatch() match {
          case Some(md) =>
            <.div(
//              <.h1("Edit Names"),
              <.h2("Only change the spelling of a players name"),
              <.h2("or replace a player."),
              <.h2("Do NOT swap players."),
              <.table(
                Header(props),
                <.tbody(
                  state.teams.values.toList.sortWith( (t1,t2)=>Id.idComparer(t1.id,t2.id)<0).map { team =>
                    TeamRow.withKey( team.id )((team,this,state,props))
                  }.toTagMod
                )
              ),
              AppButton( "OK", "OK", ^.onClick-->okCallback ),
              " ",
              AppButton( "Reset", "Reset", ^.onClick-->resetCallback ),
              " ",
              AppButton( "Cancel", "Cancel", props.routerCtl.setOnClick(props.returnPage) )
            )
          case None =>
            <.p( "Waiting" )
        }
      )
    }

    import com.example.react.Utils._
    def setPlayer(teamid: Id.Team, player: Int)( name: String ) =
      scope.modState( ps => {
        ps.teams.get(teamid) match {
          case Some(team) =>
            val newteam = team.setPlayers( if (player==1) name else team.player1,
                                           if (player==2) name else team.player2
                                         )
            val s = ps.copy( teams=ps.teams+(team.id->newteam) )
            logger.fine( s"""Updating name ${teamid} ${player} to ${name}: ${s}""" )
            s
          case None =>
            logger.fine( s"""Did not find team ${teamid}""" )
            ps
        }
      })

    val doUpdate = scope.state >>= { state => Callback {
      DuplicateStore.getMatch() match {
        case Some(md) =>
          state.teams.values.foreach { team => {
            val t = team.copy( player1 = team.player1.trim, player2 = team.player2.trim )
            val changed = md.getTeam(t.id) match {
              case Some(original) =>
                val changed = !t.equalsIgnoreModifyTime(original)
                if (changed) {
                  logger.fine( s"""Updating team ${t}: was ${original}""" )
                  Controller.updateTeam(md, t)
                } else {
                  logger.fine( s"""Updating team ${t}: was ${original}""" )
                }
              case None =>
                logger.fine( s"""Updating team ${t}: did not find old team""" )
            }
          }}
        case None =>
      }
    }}

    def okCallback = doUpdate >> scope.props >>= { props => props.routerCtl.set(props.returnPage) }

    val resetCallback = scope.props >>= { props =>
      scope.modState(s => s.reset())
    }

    val storeCallback = scope.modState { s => s.reset() }

    val namesCallback = scope.modState { s =>
      val sug = NamesStore.getNames
      logger.fine( s"""Got names ${sug}""" )
      s.copy( nameSuggestions=Some(sug))
    }

    val didMount =scope.props >>= { props =>  Callback {
      logger.info("PageNames.didMount")
      NamesStore.ensureNamesAreCached(Some(namesCallback))
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitorMatchDuplicate(props.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageNames.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageNames")
                            .initialStateFromProps { props => State().reset }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

