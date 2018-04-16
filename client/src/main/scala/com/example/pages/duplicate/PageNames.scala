package com.example.pages.duplicate


import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.bridge.store.DuplicateStore
import com.example.data.Team
import com.example.pages.duplicate.DuplicateRouter.NamesView
import com.example.react.AppButton

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageNames( routerCtl: RouterCtl[DuplicatePage], page: BaseBoardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageNames {
  import PageNamesInternal._

  case class Props( routerCtl: RouterCtl[DuplicatePage], page: NamesView, returnPage: DuplicatePage )

  def apply( routerCtl: RouterCtl[DuplicatePage], page: NamesView, returnPage: DuplicatePage ) = component(Props(routerCtl,page,returnPage))

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
  case class State( teams: Map[Id.Team, Team] )

  object State {
    def create( props: Props ) =
      DuplicateStore.getMatch() match {
        case Some(md) => State(md.teams.map(t=> t.id->t).toMap)
        case None => State(Map())
      }
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

  val TeamRow = ScalaComponent.builder[(Team,Backend,Props)]("PageNames.TeamRow")
                      .render_P( props => {
                        val (team, backend, p) = props
                        <.tr(
                          <.td( Id.teamIdToTeamNumber(team.id)),
                          <.td(
                              <.input( ^.`type`:="text", ^.name:="I_"+team.id+"_1", ^.onChange ==> backend.setPlayer(team.id, 1), ^.value := noNull(team.player1))
                          ),
                          <.td(
                              <.input( ^.`type`:="text", ^.name:="I_"+team.id+"_2", ^.onChange ==> backend.setPlayer(team.id, 2), ^.value := noNull(team.player2))
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
      logger.info("Rendering board "+props.page)
      <.div(
        dupStyles.divNamesPage,
        DuplicateStore.getMatch() match {
          case Some(md) =>
            <.div(
              <.h1("Edit Names"),
              <.h2("Only change the spelling of a players name"),
              <.h2("or replace a player."),
              <.h2("Do NOT swap players."),
              <.table(
                Header(props),
                <.tbody(
                  state.teams.values.toList.sortWith( (t1,t2)=>Id.idComparer(t1.id,t2.id)<0).map { team =>
                    TeamRow.withKey( team.id )((team,this,props))
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
    def setPlayer(teamid: Id.Team, player: Int)( e: ReactEventFromInput ) = e.inputText( name =>
      scope.modState( ps => {
        ps.teams.get(teamid) match {
          case Some(team) =>
            val newteam = team.setPlayers( if (player==1) name else team.player1,
                                           if (player==2) name else team.player2
                                         )
            ps.copy( teams=ps.teams+(team.id->newteam) )
          case None =>
            ps
        }
      }))

    val doUpdate = scope.modStateOption { state =>
      DuplicateStore.getMatch() match {
        case Some(md) =>
          state.teams.values.foreach { team => {
            val t = team.copy( player1 = team.player1.trim, player2 = team.player2.trim )
            md.getTeam(t.id) match {
              case Some(original) => !t.equalsIgnoreModifyTime(original)
              case None => true
            }
            Controller.updateTeam(md, t)
          }}
        case None =>
      }
      None
    }

    def okCallback = doUpdate >> scope.props >>= { props => props.routerCtl.set(props.returnPage) }

    val resetCallback = scope.props >>= { props =>
      scope.modState(s => State.create(props))
    }

    val storeCallback = scope.modState { (s,p) =>
      State.create(p)
    }

    val didMount =scope.props >>= { props =>  Callback {
      logger.info("PageNames.didMount")
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitorMatchDuplicate(props.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageNames.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageNames")
                            .initialStateFromProps { props => State.create( props) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

