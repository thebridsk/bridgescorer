package com.github.thebridsk.bridge.client.pages.individual.components

import com.github.thebridsk.utilities.logging.Logger

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseScoreboardViewWithPerspective
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective._
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles._

/**
  * Shows scoreboard help information
  *
  * To use, just code the following:
  *
  * {{{
  * val page: BaseScoreboardViewWithPerspective = ...
  * val score: IndividualDuplicateScore = ...
  *
  * ViewScoreboardHelp(
  *   page = page,
  *   score = score
  * )
  * }}}
  *
  * @author werewolf
  */
object ViewScoreboardHelp {
  case class Props(
      page: BaseScoreboardViewWithPerspective,
      score: IndividualDuplicateScore
  )

  /**
    * Instantiate the react component
    *
    * @param page the page that identifies the perspective
    * @param score the score object for the current match
    *
    * @return the unmounted react component
    */
  def apply(
    page: BaseScoreboardViewWithPerspective,
    score: IndividualDuplicateScore
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    Internal.component(
      Props(page, score)
    )

  protected object Internal {

    val logger: Logger = Logger("bridge.ViewScoreboardHelp")

    case class State()

    class Backend(scope: BackendScope[Props, State]) {

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        def showBoardAndMovement() = {
          <.p(
            "Boards " + props.score.duplicate.boardset.id + ", Movement " + props.score.duplicate.movement.id
          )
        }
        <.div(
          dupStyles.viewScoreboardHelp,
          props.page.getPerspective match {
            case PerspectiveTable(currentTable, currentRound) =>
              Seq(
                <.h1(
                  s"Table ${currentTable.toNumber} Scoreboard, Round ${currentRound}"
                ),
                showBoardAndMovement(),
                <.ul(
                  <.li(<.b("To score a board, hit that board number above.")),
                  <.li(
                    <.b(
                      s"""At the end of the round, hit the "Table ${currentTable.toNumber}" button to continue at the same table."""
                    )
                  ),
                  <.li(
                    <.b(
                      s"""or hit the "Completed Games Scoreboard" button to go to a different table."""
                    )
                  )
                )
              ).toTagMod
            case PerspectiveDirector =>
              Seq(
                <.h1("Director's Scoreboard"),
                showBoardAndMovement()
              ).toTagMod
            case PerspectiveComplete =>
              val tableIds = props.score.duplicate.getTableIds()
              val numberTables = tableIds.length
              Seq(
                <.h1("Completed Games Scoreboard"),
                showBoardAndMovement(),
                <.p(
                  <.b(
                    "To enter scores while playing at your table hit ",
                    if (numberTables < 4) {
                      tableIds
                        .map { id => "\"Table " + id.toNumber + "\"" }
                        .mkString(" or ") + " below."
                    } else {
                      "one of the table buttons below."
                    }
                  )
                ),
                <.p(
                  <.b(
                    "Incomplete boards will show a check mark for the teams that have played it."
                  )
                )
              ).toTagMod
          }
        )
      }

    }

    val component = ScalaComponent
      .builder[Props]("ViewScoreboardHelp")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
