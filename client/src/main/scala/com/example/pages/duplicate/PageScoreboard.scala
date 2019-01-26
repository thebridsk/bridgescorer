package com.example.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.routes.BridgeRouter
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.data.bridge.DuplicateViewPerspective
import com.example.bridge.store.DuplicateStore
import com.example.data.bridge.MatchDuplicateScore
import com.example.react.Utils._
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.PerspectiveTable
import com.example.data.bridge.PerspectiveComplete
import com.example.react.DateUtils
import com.example.pages.duplicate.DuplicateRouter.BaseScoreboardViewWithPerspective
import com.example.pages.duplicate.DuplicateRouter.TableView
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.FinishedScoreboardsView
import com.example.pages.duplicate.DuplicateRouter.DirectorScoreboardView
import com.example.pages.duplicate.DuplicateRouter.NamesView
import com.example.pages.duplicate.DuplicateRouter.AllTableView
import com.example.pages.duplicate.DuplicateRouter.DuplicateBoardSetView
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.pages.duplicate.DuplicateRouter.TableRoundScoreboardView
import com.example.react.AppButton
import com.example.react.PopupOkCancel
import com.example.pages.BaseStyles
import com.example.react.HelpButton
import com.example.routes.BridgeRouter
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import com.example.materialui.MuiMenuItem

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * The ScoreboardView object will identify which MatchDuplicate to look at.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageScoreboard( routerCtl: BridgeRouter[DuplicatePage], game: BaseScoreboardViewWithPerspective )
 * </code></pre>
 *
 * @author werewolf
 */
object PageScoreboard {
  import PageScoreboardInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], game: BaseScoreboardViewWithPerspective )

  def apply( routerCtl: BridgeRouter[DuplicatePage], game: BaseScoreboardViewWithPerspective ) = component(Props(routerCtl,game))

}

object PageScoreboardInternal {
  import PageScoreboard._

  val logger = Logger("bridge.PageScoreboard")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( deletePopup: Boolean = false, showdetails: Boolean = false, useIMP: Option[Boolean] = None ) {

    def isMP = useIMP.getOrElse(true)
    def isIMP = useIMP.getOrElse(false)

    def toggleIMP = {
      copy( useIMP = Some(!isIMP) )
    }

    def nextIMPs = {
      val n = useIMP match {
        case None => Some(false)
        case Some(false) => Some(true)
        case Some(true) => None
      }
      copy(useIMP=n)
    }

  }

  def scoringMethodButton( useIMP: Option[Boolean], default: Option[Boolean], unknown: Boolean, cb: Callback ) = {
    AppButton(
      "ScoreStyle",
      useIMP match {
        case None =>
          val sm = (default match {
            case Some(true) => Some("IMP")
            case Some(false) => Some("MP")
            case None => if (unknown) Some("Unknown") else None
          }).map( s => TagMod( s"Played Scoring Method: $s" ) ).getOrElse(TagMod("Played Scoring Method"))
          sm
        case Some(true) => TagMod("International Match Points")
        case Some(false) => TagMod("Match Points")
      },
      ^.onClick --> cb )
  }

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

      def callbackPage(page: DuplicatePage)(e: ReactEvent) = props.routerCtl.set(page).runNow()

      DuplicateStore.getView( props.game.getPerspective() ) match {
        case Some(score) =>
          val winnersets = score.getWinnerSets()

          def getScoringMethodButton() = scoringMethodButton( state.useIMP, Some( score.isIMP), false, nextIMPs )

          val (title,helpurl,pagemenu) = props.game match {
            case _: CompleteScoreboardView =>
              ("Complete Scoreboard",
               "/help/duplicate/scoreboardcomplete.html",
               List[CtorType.ChildArg](
                 MuiMenuItem(
                     id = "Director",
                     onClick = callbackPage(DirectorScoreboardView(props.game.dupid)) _
                 )(
                     "Director's Scoreboard"
                 ),
                 MuiMenuItem(
                     id = "ForPrint",
                     onClick = callbackPage(FinishedScoreboardsView(props.game.dupid)) _
                 )(
                     "For Print"
                 ),
               )
              )
            case _: DirectorScoreboardView =>
              ("Director's Scoreboard",
               "/help/duplicate/scoreboardcomplete.html",
               List[CtorType.ChildArg]()
              )
            case TableRoundScoreboardView(dupid, tableid, round) =>
              (s"Table $tableid Scoreboard, Round $round",
               "/help/duplicate/scoreboardfromtable.html",
               List[CtorType.ChildArg]()
              )
          }

          val sortedTables = score.tables.keys.toList.sortWith((t1,t2)=>t1<t2)

          logger.fine( "WinnerSets: "+winnersets )
          <.div(
            dupStyles.divScoreboardPage,
            DuplicatePageBridgeAppBar(
              id = Some(props.game.dupid),
              tableIds = sortedTables,
              pageMenuItems = pagemenu,
              title = Seq[CtorType.ChildArg](MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      title
                    )
                )),
              helpurl = helpurl,
              routeCtl = props.routerCtl
            )(

            ),
            PopupOkCancel(
              if (state.deletePopup) {
                Some( <.span( s"Are you sure you want to delete duplicate match ${score.id}" ) )
              } else {
                None
              },
              Some(actionDeleteOk),
              Some(actionDeleteCancel)
            ),
            ViewScoreboard( props.routerCtl, props.game, score, state.isIMP ),
            winnersets.map(ws => ViewPlayerMatchResult( (if (state.isIMP) score.placeImpByWinnerSet(ws) else score.placeByWinnerSet(ws)), state.isIMP )).toTagMod,
            if (state.showdetails) {
              ViewScoreboardDetails( props.game, score )
            } else {
              ViewScoreboardHelp( props.game, score )
            },
            <.div(
              baseStyles.divFooter,
              props.game.getPerspective() match {
                case PerspectiveComplete =>
                  TagMod(
                    <.div(
                      baseStyles.divFooterLeft,
                      sortedTables.map { table =>
                        val clickToTableView = TableView(props.game.dupid,table)
                        List[TagMod](
                          AppButton( "Table_"+table, "Table "+table,
                                     baseStyles.requiredNotNext,
                                     props.routerCtl.setOnClick(clickToTableView) ),
                          <.span(" ")
                          ).toTagMod
                      }.toTagMod
                    ),
                    <.div(
                      baseStyles.divFooterCenter,
                      AppButton( "AllBoards", "All Boards", props.routerCtl.setOnClick(props.game.toAllBoardsView()) ),
                      " ",
                      getScoringMethodButton(),
                      if (score.alldone) {
                        TagMod(
                          " ",
                          AppButton( "Details", "Details", ^.onClick --> toggleShowDetails, BaseStyles.highlight(selected = state.showdetails )  )
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
                      AppButton( "Game", "Completed Games Scoreboard", props.routerCtl.setOnClick(CompleteScoreboardView(props.game.dupid)) )
                    ),
                    <.div(
                      baseStyles.divFooterCenter,
                      AppButton( "AllBoards", "All Boards", props.routerCtl.setOnClick(props.game.toAllBoardsView()) ),
                      " ",
                      getScoringMethodButton(),
                    ),
                    <.div(
                      baseStyles.divFooterRight,
                      AppButton( "Delete", "Delete", ^.onClick-->actionDelete ),
                      " ",
                      AppButton( "EditNames", "Edit Names", props.routerCtl.setOnClick(NamesView(props.game.dupid)) )
                    )
                  ).toTagMod
                case PerspectiveTable(team1, team2) =>
                  props.game match {
                    case trgv: TableRoundScoreboardView =>
                      val tablenumber = Id.tableIdToTableNumber(trgv.tableid)
                      val allplayedInRound = score.getRound(trgv.tableid, trgv.round) match {
                        case Some(r) => r.complete
                        case _ => false
                      }
                      Seq(
                        <.div(
                          baseStyles.divFooterLeft,
                          AppButton( "Table", "Table "+tablenumber,
                                     allplayedInRound ?= baseStyles.requiredNotNext,
                                     props.routerCtl.setOnClick(trgv.toTableView()) )
                        ),
                        <.div(
                          baseStyles.divFooterCenter,
                          AppButton( "Game", "Completed Games Scoreboard",
                                     allplayedInRound ?= baseStyles.requiredNotNext,
                                     props.routerCtl.setOnClick(CompleteScoreboardView(props.game.dupid)) ),
                          " ",
                          getScoringMethodButton(),
                        ),
                        <.div(
                          baseStyles.divFooterRight,
                          AppButton( "AllBoards", "All Boards", props.routerCtl.setOnClick(props.game.toAllBoardsView())  ),
                        )
                      ).toTagMod
                    case _ =>
                      <.div(
                        baseStyles.divFooterLeft,
                        AppButton( "Game", "Completed Games Scoreboard", props.routerCtl.setOnClick(CompleteScoreboardView(props.game.dupid)) )
                      )
                  }
              }
            ),
            <.div(
              baseStyles.divTextFooter,
              <.p("Game "+score.id+" created "+DateUtils.formatDate(score.created)+" last updated "+DateUtils.formatDate(score.updated))
            )
          )
        case None =>
          <.h1( "Waiting" )
      }
    }

    import scala.concurrent.ExecutionContext.Implicits.global

    val actionDelete = scope.modState(s => s.copy(deletePopup=true) )

    val actionDeleteOk = scope.props >>= { props => Callback {
      Controller.deleteMatchDuplicate(props.game.dupid).foreach( msg => {
        logger.info("Deleted duplicate match, going to summary view")
        props.routerCtl.set(SummaryView).runNow()
      })
    }}

    val actionDeleteCancel = scope.modState(s => s.copy(deletePopup=false) )

    val toggleShowDetails = scope.modState( s => s.copy( showdetails = !s.showdetails) )

    val nextIMPs = scope.modState { s => s.nextIMPs }

    val storeCallback = scope.modStateOption { s =>
      DuplicateStore.getMatch().map( md => s.copy( useIMP = Some(md.isIMP) ) )
    }

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("PageScoreboard.didMount")
      DuplicateStore.addChangeListener(storeCallback)

      Controller.monitorMatchDuplicate(p.game.dupid)
    }}

    val willUnmount = CallbackTo {
      logger.info("PageScoreboard.willUnmount")
      DuplicateStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageScoreboard")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

