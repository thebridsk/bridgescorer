package com.github.thebridsk.bridge.client.pages.individual.router.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseScoreboardViewWithPerspective
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles._
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.FinishedScoreboardView
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective._
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.bridge.individual.IndividualBoardScore
import com.github.thebridsk.bridge.data.util.Strings
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.TableRoundScoreboardView

/**
  * A React component that shows the scoreboard for an individual duplicate match.
  *
  * The scoreboard is a table where the rows are the players, and the columns are
  * the boards.  There is also a totals column that sums up the board scores.
  *
  * There are three perspectives that can be used to display the scoreboard:
  * - DirectorScoreboardView - the scoreboard view for the director
  * - CompleteScoreboardView - scoreboard that only shows completed board results
  * - TableRoundScoreboardView - scoreboard view for the table and round.
  * - FinishedScoreboardView - view for printing
  *
  * To use, just code the following:
  *
  * {{{
  * val page: BaseScoreboardViewWithPerspective = ...
  * ViewScoreboard(
  *   page = page,
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object ViewScoreboard {
  import Internal._

  case class Props(
    page: BaseScoreboardViewWithPerspective,
    score: IndividualDuplicateScore,
    useMP: Boolean,
    router: BridgeRouter[IndividualDuplicatePage]
  )

  /**
    *
    * @param page identifies the perspective and the match to display
    * @param score the IndividualDuplicateScore object for the match
    * @param useMP true - use Match Points, false - use International Match Points
    * @param router
    *
    * @return the unmounted react component
    *
    * @see [[ViewScoreboard$]] for usage.
    */
  def apply(
    page: BaseScoreboardViewWithPerspective,
    score: IndividualDuplicateScore,
    useMP: Boolean,
    router: BridgeRouter[IndividualDuplicatePage]
  ) =
    component(Props(
      page,
      score,
      useMP,
      router
    )) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val log = Logger("bridge.ViewScoreboard")

    case class State()

    private val Caption = ScalaComponent
      .builder[Props]("ViewScoreboard.Caption")
      .render_P { props =>
        val showidbutton = props.page.isInstanceOf[FinishedScoreboardView]
        <.caption(
          props.page match {
            case FinishedScoreboardView(dupid) => "Scoreboard"
            case _ =>
              props.page.getPerspective match {
                case PerspectiveDirector => "Scoreboard from Director's view"
                case PerspectiveComplete =>
                  "Scoreboard with completed boards only"
                case PerspectiveTable(currentTable, currentRound) =>
                  s"Scoreboard from table ${currentTable.toNumber} round ${currentRound}"
              }
          },
          ", ",
          DateUtils.formatDay(props.score.duplicate.created),
          <.span(
            ^.float := "right",
            <.span(showidbutton ?= baseStyles.onlyInPrint, props.score.duplicate.id.id),
            showidbutton ?= <.span(
              baseStyles.hideInPrint,
              AppButton(
                "IDuplicate_" + props.score.duplicate.id.id,
                props.score.duplicate.id.id,
                props.router.setOnClick(
                  CompleteScoreboardView(props.score.duplicate.id.id)
                )
              )
            )
          ),
          <.span(
            ^.float := "left",
            <.span(showidbutton ?= baseStyles.onlyInPrint, props.score.duplicate.id.id),
            showidbutton ?= <.span(
              baseStyles.hideInPrint,
              AppButton(
                "IDuplicate2_" + props.score.duplicate.id.id,
                props.score.duplicate.id.id,
                props.router.setOnClick(
                  CompleteScoreboardView(props.score.duplicate.id.id)
                )
              )
            )
          )
        )
      }
      .build

    private implicit class WrapPerspective(val view: BaseScoreboardViewWithPerspective) extends AnyVal {

        def tablePerspective: Option[TableRoundScoreboardView] =
          view match {
            case p: TableRoundScoreboardView => Some(p)
            case _                   => None
          }

        /**
          * @param b
          * @return
          *   true if the perspective is complete or director
          *   true if table perspective and the board is in the table and round
          *   false if table perspective and the board is NOT in the table and round
          *   false if finished perspective
          */
        def isShowBoardButton(b: IndividualBoardScore): Boolean = {
          log.fine(s"isShowBoardButton: tablePerspective=${tablePerspective}, view=${view}")
          tablePerspective.map { tp =>
            b.board.hands.find { h =>
              h.table==tp.tableid &&
                h.round==tp.round
            }.isDefined
          }.getOrElse(!view.isInstanceOf[FinishedScoreboardView])
        }

        /**
          * @param b
          * @return
          *   Some(handid) if table perspective and the hand was not played
          *   None otherwise
          */
        def unplayedHand(b: IndividualBoardScore): Option[IndividualDuplicateHand.Id] = {
          tablePerspective.flatMap { tp =>
            b.board.hands.find { h =>
              h.table==tp.tableid &&
                h.round==tp.round
            }.filter { h =>
              h.played.isEmpty
            }.map { h =>
              h.id
            }
          }
        }

    }

    private val Header = ScalaComponent
      .builder[Props]("ViewScoreboard.Header")
      .render_P { props =>

        <.thead(
          <.tr(
            <.th(^.rowSpan := 2, "#"),
            <.th(^.rowSpan := 2, "Player"),
            <.th(^.rowSpan := 2, "Total"),
            <.th(^.colSpan := props.score.duplicate.boards.size, "Boards" )
          ),
          <.tr(
            props.score.boardScores.map { board =>
              <.th(
                if (props.page.isShowBoardButton(board)) {
                  val id = board.board.id
                  val unplayed = props.page.unplayedHand(board)
                  val clickPage = unplayed.map { handid =>
                    props.page.toBoardView(id).toHandView(handid)
                  }.getOrElse(props.page.toBoardView(id))
                  AppButton(
                    s"Board_${id.id}",
                    id.toNumber,
                    unplayed.whenDefined(_ => baseStyles.requiredNotNext),
                    props.router.setOnClick(clickPage)
                  )
                } else {
                  board.board.id.toNumber
                }
              )
            }.toTagMod
          )
        )
      }
      .build

    private implicit class WrapScore(val s: IndividualDuplicateScore.Score) extends AnyVal {
      def display(mp: Boolean): String = {
        if (mp) s.mp.toString
        else f"${s.imp}%.1f"
      }
    }

    private implicit class WrapResult(val s: IndividualBoardScore.Result) extends AnyVal {
      def display(mp: Boolean): String = {
        if (!s.played) ""
        else if (s.hide) Strings.checkmark
        else if (mp) {
          if (s.isNS) s.nsMP.toString()
          else s.ewMP.toString()
        } else {
          val p = if (s.isNS) s.nsIMP
                  else s.ewIMP
          f"${p}%.1f"
        }
      }
    }

    private val Row = ScalaComponent
      .builder[(Props, Int)]("ViewScoreboard.Row")
      .render_P { args =>
        val (props, nameIndex) = args
        val isMP = props.useMP
        <.tr(
          <.td(s"$nameIndex"),
          <.td(props.score.duplicate.getPlayer(nameIndex)),
          <.td(props.score.scores(nameIndex).display(isMP)),
          props.score.boardScores.map { board =>
            val p = board.getResult(nameIndex)
                      .map(_.display(isMP))
                      .getOrElse(Strings.xmark)
            <.td(p)
          }.toTagMod
        )
      }
      .build

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val ids = props.score
        <.div(
          dupStyles.viewScoreboard,
          <.table(
            Caption(props),
            Header(props),
            <.tbody(
              ids.duplicate.sortedPlayers
                .map { i =>
                  Row.withKey(i)((props, i))
                }
                .toTagMod
            )
          )
        )
      }

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true
      }

      val willUnmount: Callback = Callback {
        mounted = false
      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps.score.duplicate.id != props.score.duplicate.id
            || prevProps.score.duplicate.updated != props.score.duplicate.updated
        ) {
          // props have change, reinitialize state
          cdu.forceUpdate
        }
      }

    val component = ScalaComponent
      .builder[Props]("ViewScoreboard")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
