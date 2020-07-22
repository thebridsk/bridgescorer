package com.github.thebridsk.bridge.client.pages.duplicate

import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.utilities.logging.Logger

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseScoreboardViewWithPerspective
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableRoundScoreboardView
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.Table

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewScoreboardHelp( routerCtl: BridgeRouter[DuplicatePage], score: MatchDuplicateScore )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewScoreboardHelp {
 case class Props( page: BaseScoreboardViewWithPerspective, md: MatchDuplicateScore )

  def apply( page: BaseScoreboardViewWithPerspective, md: MatchDuplicateScore  ) = ViewScoreboardHelpInternal.component(Props(page,md))

}

object ViewScoreboardHelpInternal {
  import ViewScoreboardHelp._

  val logger = Logger("bridge.ViewScoreboardHelp")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    import DuplicateStyles._
    def render( props: Props, state: State ) = {
      def teamColumn( teams: List[Team] ) = {
        var count = 0
        teams.sortWith((t1,t2)=> t1.id < t2.id).map { team =>
          count += 1
          <.span( count!=1 ?= <.br(), team.id.toNumber+" "+team.player1+" "+team.player2 )
        }.toTagMod
      }

      def showBoardAndMovement() = {
        <.p("Boards "+props.md.getBoardSet.id+", Movement "+props.md.getMovement.id)
      }
      <.div(
        dupStyles.divScoreboardHelp,
        props.page.getPerspective match {
          case PerspectiveTable(t1, t2) =>
            val (team1,team2) = if (t1<t2) (t1,t2) else (t2,t1)
            val (currentRound,currentTable) = props.page match {
              case v: TableRoundScoreboardView => (v.round,v.tableid)
              case _ => (-1,Table.idNul)
            }
            Seq(
              <.h1(s"Table ${currentTable.toNumber} Scoreboard, Round ${currentRound}, Teams ${team1.toNumber} and ${team2.toNumber}"),
              showBoardAndMovement(),
              <.ul(
                <.li(<.b("To score a board, hit that board number above.")),
                <.li(<.b(s"""At the end of the round, hit the "Table ${currentTable.toNumber}" button to continue at the same table.""" )),
                <.li(<.b(s"""or hit the "Completed Games Scoreboard" button to go to a different table.""" ))
              )
            ).toTagMod
          case PerspectiveDirector =>
            Seq(
              <.h1("Director's Scoreboard"),
              showBoardAndMovement()
            ).toTagMod
          case PerspectiveComplete =>
            val tableIds = props.md.getTableIds
            val numberTables = tableIds.length
            Seq(
              <.h1("Completed Games Scoreboard"),
              showBoardAndMovement(),
              <.p(<.b("To enter scores while playing at your table hit ",
                  if (numberTables<4) {
                    tableIds.map { id => "\"Table "+id.toNumber+"\"" }.mkString(" or ")+" below."
                  } else {
                    "one of the table buttons below."
                  }
              )),
              <.p(<.b("Incomplete boards will show a check mark for the teams that have played it."))
            ).toTagMod
        }
      )
    }

  }

  val component = ScalaComponent.builder[Props]("ViewScoreboardHelp")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

