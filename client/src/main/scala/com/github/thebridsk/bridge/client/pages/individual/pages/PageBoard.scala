package com.github.thebridsk.bridge.client.pages.individual

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.IndividualController
import com.github.thebridsk.bridge.client.bridge.store.IndividualDuplicateStore
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles._
import com.github.thebridsk.bridge.client.pages.individual.components.DuplicateBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.ForPageBoard
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.DirectorAllBoardView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.CompleteAllBoardView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.TableRoundAllBoardView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.DirectorBoardView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.CompleteBoardView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.TableRoundBoardView
import com.github.thebridsk.bridge.client.pages.individual.components.ViewBoard
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseBoardView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseAllBoardsView
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.bridge.individual.IndividualBoardScore

/**
  * A component page that shows final scoreboard view.
  *
  * The ScoreboardView object will identify which MatchDuplicate to look at
  * and the perspective used to show the scoreboard.
  *
  * Perspectives are:
  * - director - director's perspective always shows everything
  * - complete - only shows results of completed boards.  Incomplete boards only
  *              shows which teams played the board.
  * - table - only shows results of boards that both teams at the table have played.
  *
  * Has a scoring method button that allows changing the scoring method.  The matches
  * scoring method may only be change prior to any hands being played.
  *
  * To use, just code the following:
  *
  * {{{
  * // one of:
  * val game = DirectorAllBoardView(id)
  * val game = CompleteAllBoardView(id)
  * val game = TableRoundAllBoardView(id, tableid, round)
  * val game = DirectorBoardView(id, boardid)
  * val game = CompleteBoardView(id, boardid)
  * val game = TableRoundBoardView(id, tableid, round, boardid)
  *
  * PageBoard(
  *   router = ...,
  *   game = game
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageBoard {
  import Internal._

  case class Props(
      router: BridgeRouter[IndividualDuplicatePage],
      page: ForPageBoard
  )

  /**
    * Instantiate the component
    *
    * @param router the react router
    * @param page a [[ForPageBoard]] object that identifies the match or matches to display
    *             and the perspective to display
    * @return the unmounted react component
    *
    * @see See [[PageBoard]] for usage.
    */
  def apply(
      router: BridgeRouter[IndividualDuplicatePage],
      page: ForPageBoard
  ) =
    component(
      Props(router, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.PageBoardIndividual")

    case class State(
      useIMP: Option[Boolean] = None
    ) {

      def isMP: Boolean = !useIMP.getOrElse(false)
      def isIMP: Boolean = useIMP.getOrElse(false)

      def toggleIMP: State = {
        copy(useIMP = Some(!isIMP))
      }

      def nextIMPs: State = {
        val n = useIMP match {
          case None        => Some(false)
          case Some(false) => Some(true)
          case Some(true)  => None
        }
        copy(useIMP = n)
      }
    }

    private val BoardsRow = ScalaComponent
      .builder[(List[IndividualBoardScore], Props, IndividualDuplicateScore)](
        "PageBoard.BoardsRow"
      )
      .render_P(args => {
        val (row, props, mds) = args
        <.tr(
          row.map { bs =>
            BoardCell.withKey("KeyBoard_" + bs.board.id.id)((bs.board.id, props, bs))
          }.toTagMod
        )
      })
      .build

    private val BoardCell = ScalaComponent
      .builder[(IndividualBoard.Id, Props, IndividualBoardScore)]("PageBoard.BoardCell")
      .render_P(args => {
        val (id, props, bs) = args
        val me = props.page.boardid
        val clickToBoard = props.page.toScoreboardView.toBoardView(id)
        logger.fine(s"Target for setOnClick is ${clickToBoard}")
        <.td(
          AppButton(
            s"Board_${id.id}",
            "Board " + id.toNumber,
            BaseStyles.highlight(
              selected = me == id,
              required = me != id && bs.allplayed,
              requiredNotNext = me != id && bs.anyplayed
            ),
            baseStyles.appButton100,
            props.router.setOnClick(clickToBoard)
          )
        )
      })
      .build

    class Backend(scope: BackendScope[Props, State]) {

      val nextIMPs: Callback = scope.modState { s => s.nextIMPs }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        val (titleprefix, tableperspective, currenttable, baseBoardView) = props.page match {
          case v: DirectorAllBoardView =>
            ("Director's", None, None, None)
          case v: CompleteAllBoardView =>
            ("Complete", None, None, None)
          case v: TableRoundAllBoardView =>
            (s"Table ${v.tableid.toNumber} Round ${v.round}", None, Some(v.tableid), None)
          case v: DirectorBoardView =>
            ("Director's", None, None, Some(v))
          case v: CompleteBoardView =>
            ("Complete", None, None, Some(v))
          case v: TableRoundBoardView =>
            (s"Table ${v.tableid.toNumber} Round ${v.round}", Some(v), Some(v.tableid), Some(v))
        }

        def buttons(
            label: String,
            hands: List[IndividualDuplicateHand],
            ns: IndividualDuplicateHand.Id,
            played: Boolean
        ) = {
          <.span(
            !hands.isEmpty ?= <.b(label),
            !hands.isEmpty ?= hands.map { hand =>
              val boardid = hand.board
              val selected = boardid == props.page.boardid
              val clickPage = if (played) {
                props.page.toBoardView(boardid)
              } else {
                props.page.toBoardView(boardid).toHandView(ns)
              }
              Seq[TagMod](
                AppButton(
                  s"Board_${boardid.id}",
                  "Board " + boardid.toNumber,
                  BaseStyles.highlight(
                    selected = selected,
                    requiredNotNext = !played && !selected
                  ),
                  props.router.setOnClick(clickPage)
                )
              ).toTagMod
            }.toTagMod
          )
        }

        def boardsFromTable(mds: IndividualDuplicateScore) = {
          props.page match {
            case tbpage: TableRoundBoardView =>
              val hands = mds.getBoardsInRound(tbpage.round, tbpage.tableid)
                .flatMap { ibs =>
                  ibs.board.hands.find(h => h.round == tbpage.round && h.table == tbpage.tableid)
                }.groupBy(_.played.isDefined)
              val played = hands.get(true).getOrElse(List.empty)
              val unplayed = hands.get(false).getOrElse(List.empty)
              val north = (played ::: unplayed).headOption
                .map(h => IndividualDuplicateHand.id(h.north))
                .getOrElse(IndividualDuplicateHand.idNul)
              <.span(
                buttons("Played: ", played, north, true),
                <.span(^.dangerouslySetInnerHtml := "&nbsp;&nbsp;"),
                buttons("Unplayed: ", unplayed, north, false)
              )
            case _ =>
              None
              <.span()
          }
        }

        def boards(mds: IndividualDuplicateScore) = {
          var counter = 0
          def getKey() = {
            counter = counter + 1
            "KeyBoardsRow_" + counter
          }
          <.div(
            <.table(
              <.tbody(
                mds.boardScores
                  .sortBy(_.board.id)
                  .grouped(6)
                  .map { row =>
                    BoardsRow.withKey(getKey())((row, props, mds))
                  }
                  .toTagMod
              )
            )
          )
        }

        <.div(
          DuplicateBridgeAppBar(
            id = Some(props.page.dupid),
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span({
                  val s = props.page.getBoardId
                    .map(id => s"${titleprefix} Board ${id.toNumber}")
                    .getOrElse(s"${titleprefix} All Boards")
                  s
                })
              )
            ),
            helpurl = "../help/duplicate/scoreboardcomplete.html",
            routeCtl = props.router
          )(
          ),
          <.div(
            dupStyles.pageBoard,
            IndividualDuplicateStore.getView(props.page.getPerspective) match {
              case Some(score) if score.duplicate.id == props.page.dupid =>
                props.page match {
                  case pp: BaseAllBoardsView =>
                    TagMod(
                      <.div(
                        AppButton(
                          "Game",
                          "Scoreboard",
                          props.router.setOnClick(props.page.toScoreboardView)
                        )
                      ),
                      score.boardScores
                        .map(bs => bs.board.id)
                        .sorted
                        .map { boardid =>
                          ViewBoard(
                            props.router,
                            pp.toBoardView(boardid),
                            score,
                            boardid,
                            state.useIMP.getOrElse(score.duplicate.isIMP),
                            /* pictures */
                          ).toTagMod
                        }.toTagMod,
                      <.div(
                        AppButton(
                          "Game2",
                          "Scoreboard",
                          props.router.setOnClick(props.page.toScoreboardView)
                        ),
                        PageScoreboard.scoringMethodButton(
                          state.useIMP,
                          Some(score.duplicate.isIMP),
                          false,
                          nextIMPs
                        )
                      )

                    )
                  case pp: BaseBoardView =>
                    val allplayedInRound = tableperspective.map { tp =>
                      score.duplicate.getHandsInRound(tp.tableid, tp.round)
                        .find(h => h.played.isEmpty)  // find a hand that was not played
                        .isEmpty  // all hands have been played
                    }.getOrElse(false)
                    TagMod(
                      ViewBoard(
                        props.router,
                        pp,
                        score,
                        pp.boardid,
                        state.useIMP.getOrElse(score.duplicate.isIMP),
                        /* pictures */
                      ),
                      <.div(
                        if (tableperspective.isDefined) {
                          TagMod(
                            boardsFromTable(score),
                            <.p
                          )
                        } else {
                          TagMod.empty
                        },
                        AppButton(
                          "Game",
                          "Scoreboard",
                          allplayedInRound ?= baseStyles.requiredNotNext,
                          props.router.setOnClick(props.page.toScoreboardView)
                        ),
                        " ",
                        tableperspective.map { tp =>
                          AppButton(
                            "Table",
                            "Table " + tp.tableid.toInt,
                            allplayedInRound ?= baseStyles.requiredNotNext,
                            props.router.setOnClick(tp.toTableView)
                          )
                        },
                        " ",
                        AppButton(
                          "AllBoards",
                          "All Boards",
                          props.router.setOnClick(baseBoardView.get.toAllBoardsView)
                        ),
                        " ",
                        PageScoreboard.scoringMethodButton(
                          state.useIMP,
                          Some(score.duplicate.isIMP),
                          false,
                          nextIMPs
                        ),
                        if (tableperspective.isEmpty) boards(score)
                        else TagMod()
                      )
                    )
                }
              case _ =>
                HomePage.loading
            }
          )
        )
      }

      val storeCallback: Callback = scope.modStateOption { s =>
        IndividualDuplicateStore.getMatch().map(md => s.copy(useIMP = Some(md.isIMP)))
      }

      val didMount: Callback = scope.props >>= { (p) =>
        Callback {
          logger.info("PageBoard.didMount")
          IndividualDuplicateStore.addChangeListener(storeCallback)

          IndividualController.monitor(p.page.dupid)
        }
      }

      val willUnmount: Callback = CallbackTo {
        logger.info("PageBoard.willUnmount")
        IndividualDuplicateStore.removeChangeListener(storeCallback)
        IndividualController.delayStop()
      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps.page != props.page) {
          IndividualController.monitor(props.page.dupid)
          cdu.setState(State()).runNow()
        }
      }

    val component = ScalaComponent
      .builder[Props]("PageBoard")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
