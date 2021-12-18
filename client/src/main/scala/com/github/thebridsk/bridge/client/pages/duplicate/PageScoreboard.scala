package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BaseScoreboardViewWithPerspective
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.FinishedScoreboardsView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.DirectorScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.NamesView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.TableRoundScoreboardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

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
  * val game = DirectorScoreboardView(id)
  * val game = CompleteScoreboardView(id)
  * val game = TableRoundScoreboardView(id, tableid, round)
  *
  * PageScoreboard(
  *   routerCtl = ...,
  *   game = game
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageScoreboard {
  import Internal._

  case class Props(
      routerCtl: BridgeRouter[DuplicatePage],
      game: BaseScoreboardViewWithPerspective
  )

  /**
    * Instantiate the component
    *
    * @param routerCtl the react router
    * @param game a [[BaseScoreboardViewWithPerspective]] object that identifies the match to display
    *             and the perspective to display
    * @return the unmounted react component
    *
    * @see See [[PageScoreboard]] for usage.
    */
  def apply(
      routerCtl: BridgeRouter[DuplicatePage],
      game: BaseScoreboardViewWithPerspective
  ) =
    component(
      Props(routerCtl, game)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def scoringMethodButton(
      useIMP: Option[Boolean],
      default: Option[Boolean],
      unknown: Boolean,
      cb: Callback
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    AppButton(
      "ScoreStyle",
      useIMP match {
        case None =>
          val sm = (default match {
            case Some(true)  => Some("IMP")
            case Some(false) => Some("MP")
            case None        => if (unknown) Some("Unknown") else None
          }).map(s => TagMod(s"Played Scoring Method: $s"))
            .getOrElse(TagMod("Played Scoring Method"))
          sm
        case Some(true)  => TagMod("International Match Points")
        case Some(false) => TagMod("Match Points")
      },
      ^.onClick --> cb
    )
  }

  protected object Internal {

    val logger: Logger = Logger("bridge.PageScoreboard")

    case class State(
        deletePopup: Boolean = false,
        showdetails: Boolean = false,
        useIMP: Option[Boolean] = None
    ) {

      /**
        * whether the current display should show MP scoring
        */
      def isMP(md: MatchDuplicate): Boolean = !useIMP.getOrElse(md.isIMP)

      /**
        * whether the current display should show IMP scoring
        */
      def isIMP(md: MatchDuplicate): Boolean = useIMP.getOrElse(md.isIMP)

      def toggleIMP(md: MatchDuplicate): State = {
        copy(useIMP = Some(!isIMP(md)))
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

    class Backend(scope: BackendScope[Props, State]) {
      import DuplicateStyles._

      def setScoringMethod(currentInMatchIsIMP: Boolean): Callback =
        scope.state >>= { state =>
          Callback {
            state.useIMP match {
              case Some(next) if currentInMatchIsIMP != next =>
                // switch to next
                DuplicateStore.getMatch() match {
                  case Some(md) =>
                    val nmd = md.copy(scoringmethod =
                      Some(
                        if (next) MatchDuplicate.InternationalMatchPoints
                        else MatchDuplicate.MatchPoints
                      )
                    )
                    Controller.updateMatch(nmd)
                  case None =>
                }
              case _ =>
              // nothing to do, already scoring with the wanted scoring method
            }
          }
        }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        def callbackPage(page: DuplicatePage)(e: ReactEvent) =
          props.routerCtl.set(page).runNow()

        def isSetScoringMethodEnabled(currentInMatchIsIMP: Boolean) = {
          val rc = state.useIMP match {
            case Some(next) =>
              // switch to next
              currentInMatchIsIMP != next
            case _ =>
              // nothing to do, already scoring with the wanted scoring method
              false
          }
          logger.fine(
            s"""isSetScoringMethodEnabled rc=$rc, useIMP=${state.useIMP}, currentInMatchIsIMP=$currentInMatchIsIMP"""
          )
          rc
        }

        DuplicateStore.getView(props.game.getPerspective) match {
          case Some(score) if score.id == props.game.dupid =>
            val winnersets = score.getWinnerSets

            def getScoringMethodButton() =
              scoringMethodButton(
                state.useIMP,
                Some(score.isIMP),
                false,
                nextIMPs
              )

            val (title, helpurl, pagemenu) = props.game match {
              case _: CompleteScoreboardView =>
                (
                  "Complete Scoreboard",
                  "../help/duplicate/scoreboardcomplete.html",
                  List[CtorType.ChildArg](
                    MuiMenuItem(
                      id = "Director",
                      onClick =
                        callbackPage(DirectorScoreboardView(props.game.sdupid)) _
                    )(
                      "Director's Scoreboard"
                    ),
                    MuiMenuItem(
                      id = "ForPrint",
                      onClick =
                        callbackPage(FinishedScoreboardsView(props.game.sdupid)) _
                    )(
                      "For Print"
                    )
                  )
                )
              case _: DirectorScoreboardView =>
                (
                  "Director's Scoreboard",
                  "../help/duplicate/scoreboardcomplete.html",
                  List[CtorType.ChildArg]()
                )
              case TableRoundScoreboardView(dupid, tableid, round) =>
                (
                  s"Table $tableid Round $round Scoreboard",
                  "../help/duplicate/scoreboardfromtable.html",
                  List[CtorType.ChildArg]()
                )
            }

            val sortedTables = score.tables.keys.toList.sorted

            logger.fine("WinnerSets: " + winnersets)
            <.div(
              PopupOkCancel(
                if (state.deletePopup) {
                  Some(
                    <.span(
                      s"Are you sure you want to delete duplicate match ${score.id.id}"
                    )
                  )
                } else {
                  None
                },
                Some(actionDeleteOk),
                Some(actionDeleteCancel)
              ),
              DuplicatePageBridgeAppBar(
                id = Some(props.game.dupid),
                tableIds = sortedTables,
                title = Seq[CtorType.ChildArg](
                  MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit
                  )(
                    <.span(
                      title
                    )
                  )
                ),
                helpurl = helpurl,
                routeCtl = props.routerCtl
              )(
                pagemenu: _*
              ),
              <.div(
                dupStyles.divScoreboardPage,
                ViewScoreboard(
                  props.routerCtl,
                  props.game,
                  score,
                  state.isIMP(score.duplicate)
                ),
                winnersets
                  .map(ws =>
                    ViewPlayerMatchResult(
                      (if (state.isIMP(score.duplicate))
                        score.placeImpByWinnerSet(ws)
                      else score.placeByWinnerSet(ws)),
                      state.isIMP(score.duplicate)
                    )
                  )
                  .toTagMod,
                if (state.showdetails) {
                  ViewScoreboardDetails(props.game, score)
                } else {
                  ViewScoreboardHelp(props.game, score)
                },
                <.div(baseStyles.divFlexBreak),
                <.div(
                  baseStyles.divFooter,
                  props.game.getPerspective match {
                    case PerspectiveComplete =>
                      TagMod(
                        <.div(
                          baseStyles.divFooterLeft,
                          sortedTables.map { table =>
                            val clickToTableView =
                              TableView(props.game.sdupid, table.id)
                            List[TagMod](
                              AppButton(
                                "Table_" + table.id,
                                "Table " + table.id,
                                baseStyles.requiredNotNext,
                                props.routerCtl.setOnClick(clickToTableView)
                              ),
                              <.span(" ")
                            ).toTagMod
                          }.toTagMod
                        ),
                        <.div(
                          baseStyles.divFooterCenter,
                          AppButton(
                            "AllBoards",
                            "All Boards",
                            props.routerCtl.setOnClick(props.game.toAllBoardsView)
                          ),
                          " ",
                          getScoringMethodButton(),
                          AppButton(
                            "SetScoringMethod",
                            "Set Scoring Method",
                            ^.onClick --> setScoringMethod(score.duplicate.isIMP),
                            ^.disabled := !isSetScoringMethodEnabled(
                              score.duplicate.isIMP
                            )
                          ).when(!score.isStarted),
                          if (score.alldone) {
                            TagMod(
                              " ",
                              AppButton(
                                "Details",
                                "Details",
                                ^.onClick --> toggleShowDetails,
                                BaseStyles.highlight(selected = state.showdetails)
                              )
                            )
                          } else {
                            TagMod()
                          }
                        )
                      )
                    case PerspectiveDirector =>
                      Seq(
                        <.div(
                          baseStyles.divFooterLeft,
                          AppButton(
                            "Game",
                            "Completed Games Scoreboard",
                            props.routerCtl.setOnClick(
                              CompleteScoreboardView(props.game.sdupid)
                            )
                          ),
                          " ",
                          AppButton(
                            "AllBoards",
                            "All Boards",
                            props.routerCtl.setOnClick(props.game.toAllBoardsView)
                          )
                        ),
                        <.div(
                          baseStyles.divFooterCenter,
                          getScoringMethodButton(),
                          " ",
                          AppButton(
                            "SetScoringMethod",
                            "Set Scoring Method",
                            ^.onClick --> setScoringMethod(score.duplicate.isIMP),
                            ^.disabled := !isSetScoringMethodEnabled(
                              score.duplicate.isIMP
                            )
                          )
                        ),
                        <.div(
                          baseStyles.divFooterRight,
                          AppButton(
                            "Delete",
                            "Delete",
                            ^.onClick --> actionDelete
                          ),
                          " ",
                          AppButton(
                            "EditNames",
                            "Edit Names",
                            props.routerCtl.setOnClick(
                              NamesView(props.game.sdupid)
                            )
                          )
                        )
                      ).toTagMod
                    case PerspectiveTable(team1, team2) =>
                      props.game match {
                        case trgv: TableRoundScoreboardView =>
                          val tablenumber = trgv.tableid.toNumber
                          val allplayedInRound =
                            score.getRound(trgv.tableid, trgv.round) match {
                              case Some(r) => r.complete
                              case _       => false
                            }
                          Seq(
                            <.div(
                              baseStyles.divFooterLeft,
                              AppButton(
                                "Table",
                                "Table " + tablenumber,
                                allplayedInRound ?= baseStyles.requiredNotNext,
                                props.routerCtl.setOnClick(trgv.toTableView)
                              )
                            ),
                            <.div(
                              baseStyles.divFooterCenter,
                              AppButton(
                                "Game",
                                "Completed Games Scoreboard",
                                allplayedInRound ?= baseStyles.requiredNotNext,
                                props.routerCtl.setOnClick(
                                  CompleteScoreboardView(props.game.sdupid)
                                )
                              ),
                              " ",
                              getScoringMethodButton()
                            ),
                            <.div(
                              baseStyles.divFooterRight,
                              AppButton(
                                "AllBoards",
                                "All Boards",
                                props.routerCtl.setOnClick(
                                  props.game.toAllBoardsView
                                )
                              )
                            )
                          ).toTagMod
                        case _ =>
                          <.div(
                            baseStyles.divFooterLeft,
                            AppButton(
                              "Game",
                              "Completed Games Scoreboard",
                              props.routerCtl.setOnClick(
                                CompleteScoreboardView(props.game.sdupid)
                              )
                            )
                          )
                      }
                  }
                ),
                <.div(
                  baseStyles.divTextFooter,
                  <.p(
                    "Game " + score.id.id + " created " + DateUtils.formatDate(
                      score.created
                    ) + " last updated " + DateUtils.formatDate(score.updated)
                  )
                )
              )
            )
          case _ =>
            <.div(
              DuplicatePageBridgeAppBar(
                id = Some(props.game.dupid),
                tableIds = List(),
                title = Seq[CtorType.ChildArg](
                  MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit
                  )(
                    <.span(
                      "Complete Scoreboard"
                    )
                  )
                ),
                helpurl = "../help/duplicate/scoreboardcomplete.html",
                routeCtl = props.routerCtl
              )(
              ),
              HomePage.loading
            )

        }
      }

      import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global

      val actionDelete: Callback = scope.modState(s => s.copy(deletePopup = true))

      val actionDeleteOk: Callback = scope.props >>= { props =>
        Callback {
          Controller
            .deleteMatchDuplicate(props.game.dupid)
            .foreach(msg => {
              logger.info("Deleted duplicate match, going to summary view")
              props.routerCtl.set(SummaryView).runNow()
            })
        }
      }

      val actionDeleteCancel: Callback =
        scope.modState(s => s.copy(deletePopup = false))

      val toggleShowDetails: Callback =
        scope.modState(s => s.copy(showdetails = !s.showdetails))

      val nextIMPs: Callback = scope.modState { s => s.nextIMPs }

      val storeCallback: Callback = scope.modStateOption { s =>
        DuplicateStore.getMatch().map(md => s.copy(useIMP = Some(md.isIMP)))
      }

      val didMount: Callback = scope.props >>= { (p) =>
        Callback {
          logger.info("PageScoreboard.didMount")
          DuplicateStore.addChangeListener(storeCallback)

          Controller.monitor(p.game.dupid)
        }
      }

      val willUnmount: Callback = CallbackTo {
        logger.info("PageScoreboard.willUnmount")
        DuplicateStore.removeChangeListener(storeCallback)
        Controller.delayStop()
      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps.game != props.game) {
          Controller.monitor(props.game.dupid)
          cdu.setState(State())
        }
      }

    val component = ScalaComponent
      .builder[Props]("PageScoreboard")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
