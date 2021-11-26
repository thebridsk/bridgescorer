package com.github.thebridsk.bridge.client.pages.individual.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.BoardSetStore
import com.github.thebridsk.bridge.client.controller.BoardSetController
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.{CompleteScoreboardView => DCompleteScoreboardView}
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.{MovementView => DMovementView}
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.rest2.ResultHolder
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.clientcommon.rest2.RequestCancelled
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicateResult
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.DuplicateResultEditView
import scala.annotation.tailrec
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.client.bridge.store.DuplicateSummaryStore
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles
import com.github.thebridsk.bridge.client.pages.individual.components.DuplicateBridgeAppBar
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule.PlayDuplicate
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.client.controller.IndividualController
import com.github.thebridsk.bridge.data.MovementBase
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateModule.PlayIndividualDuplicate
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.MovementView
import com.github.thebridsk.bridge.clientcommon.react.Utils._

/**
  * Component page to select the movement and boardsets used for a duplicate match.
  *
  * This component displays a table, the rows are the movements that can be used,
  * the columns are the boardsets.
  *
  * Clicking on a button will create a duplicated match with the movement and boardset.
  *
  * To use, just code the following:
  *
  * {{{
  * PageNewDuplicate(
  *   routerCtl = ...,
  *   page = NewDuplicateView
  * )
  * }}}
  *
  * @author werewolf
  */
object PageNewDuplicate {
  import Internal._

  case class Props(routerCtl: BridgeRouter[IndividualDuplicatePage], page: IndividualDuplicatePage)

  /**
    * Instantiate the component
    *
    * @param routerCtl
    * @param page
    * @return the unmounted react component
    */
  def apply(routerCtl: BridgeRouter[IndividualDuplicatePage], page: IndividualDuplicatePage) =
    component(
      Props(routerCtl, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def intToString(list: List[Int]): String = {

    def appendNext(s: String, n: Int) = {
      if (s.isEmpty()) s"$n"
      else s"${s}, ${n}"
    }

    /* *
    * @param s the string collected so far
    * @param openRange true if the last item in s is the start of the range (no "-"), last could close the range
    * @param last the last number, it is not in s
    * @param l the remaining numbers
    */
    @tailrec
    def next(s: String, openRange: Boolean, last: Int, l: List[Int]): String = {
      if (l.isEmpty) {
        if (openRange) {
          s"${s}-${last}"
        } else {
          appendNext(s, last)
        }
      } else {
        val n = l.head
        if (openRange) {
          if (last + 1 == n) {
            next(s, true, n, l.tail)
          } else {
            next(s"${s}-${last}", false, n, l.tail)
          }
        } else {
          if (last + 1 == n) {
            next(appendNext(s, last), true, n, l.tail)
          } else {
            next(appendNext(s, last), false, n, l.tail)
          }
        }
      }
    }

    if (list.isEmpty) ""
    else next("", false, list.head, list.tail)
  }

  protected object Internal {
    import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancelImplicits._

    val logger: Logger = Logger("bridge.PageNewDuplicate")

    case class State(
        boardsets: List[BoardSet] = List.empty,
        tmovements: List[Movement] = List.empty,
        imovements: List[IndividualMovement] = List.empty,
        movements: List[MovementBase] = List.empty,
        msg: Option[String] = None,
        playIndividual: Boolean = false
    ) {
      def getTeamMovements: List[MovementBase] = tmovements
      def getIndividualMovements: List[MovementBase] = imovements

      def getPlayingMovements: List[MovementBase] = if (playIndividual) imovements else tmovements

      def withPlayingIndividual(f: Boolean) = copy(playIndividual = f)

      def withBoardsets(l: List[BoardSet]) = copy(boardsets = l.sortWith((l,r) => l.short < r.short))

      def withMovements(l: List[Movement]) = {
        val a = (l:::imovements).sorted
        copy(tmovements = l.sorted, movements = a)
      }
      def withIndividaulMovements(l: List[IndividualMovement]) = {
        val a = (l:::tmovements).sorted
        copy(imovements = l.sorted, movements = a)
      }

      def withBoardsetsAndMovements(
        bs: List[BoardSet],
        mov: List[Movement],
        imov: List[IndividualMovement]
      ) = {
        val a = (mov:::imov).sorted
        copy(boardsets = bs, tmovements = mov.sorted, imovements = imov.sorted, movements = a)
      }

      def withMsg(m: String) = copy(msg = Some(m))
      def clearMsg() = copy(msg = None)

      def getBoardset(id: BoardSet.Id) = boardsets.find(_.id == id)
      def getMovement(id: Movement.Id) = tmovements.find(_.id == id)
      def getIndividualMovement(id: IndividualMovement.Id) = imovements.find(_.id == id)
    }

    def canPlay(movement: MovementBase, boardset: BoardSet): Boolean = {
      val movboards = movement.getBoards
      val boards = boardset.boards.map(b => b.id)
      movboards.find(b => !boards.contains(b)).isEmpty
    }

    def missingBoards(movement: MovementBase, boardset: BoardSet): List[Int] = {
      val movboards = movement.getBoards
      val boards = boardset.boards.map(b => b.id)
      movboards.filter(b => !boards.contains(b))
    }

    private val Header = ScalaComponent
      .builder[(Props, State, Backend)]("PageNewDuplicate.Header")
      .render_P(args => {
        val (props, state, backend) = args
        val n = if (state.boardsets.isEmpty) 1 else state.boardsets.size
        <.thead(
          <.tr(
            <.th("Movement"),
            <.th("Boards", ^.colSpan := n),
            <.th("Results Only")
          )
        )
      })
      .build

    private def getMovType(movement: MovementBase) = if (movement.isIndividual) "_I_" else "_T_"

    private val Row = ScalaComponent
      .builder[(MovementBase, Props, State, Backend)]("PageNewDuplicate.Row")
      .render_P(args => {
        val (movement, props, state, backend) = args
        val movType = getMovType(movement)
        <.tr(
          <.td(movement.short),
          state
            .boardsets
            .map { boardset =>
              {
                val missing = missingBoards(movement, boardset)
                <.td(
                  if (missing.isEmpty) {
                    AppButton(
                      s"New${movType}${ movement.nameAsString }_${ boardset.id.id }",
                      boardset.short,
                      ^.onClick --> backend.newDuplicate(
                        boards = Some(boardset),
                        movement = movement
                      )
                    )
                  } else {
                    TagMod(
                      "Missing boards",
                      <.br,
                      intToString(missing.sorted)
                    )
                  }
                )
              }
            }
            .toTagMod,
          <.td(
            movement.getMovementId.whenDefined { id =>
              AppButton(
                s"New${movType}${ movement.nameAsString }_Result",
                "Results Only",
                ^.onClick --> backend.newDuplicate(
                  boards = None,
                  movement = movement,
                  resultsOnly = true
                )
              )
            }
          )
        )
      })
      .build

    var demoId = 0

    class Backend(scope: BackendScope[Props, State]) {

      private var mounted = false

      val resultDuplicate: ResultHolder[MatchDuplicate] =
        ResultHolder[MatchDuplicate]()

      val cancel: Callback = Callback {
        resultDuplicate.cancel()
      } >> scope.modState(s => s.clearMsg())

      def setPlayingIndividual(f: Boolean): Callback = scope.modState(s => s.withPlayingIndividual(f))

      import scala.concurrent.ExecutionContext.Implicits.global
      def newDuplicate(
          movement: MovementBase,
          boards: Option[BoardSet] = None,
          default: Boolean = true,
          resultsOnly: Boolean = false,
          fortest: Boolean = false
      ): Callback =
        if (BridgeDemo.isDemo) {
          val s = scope.withEffectsImpure.state
          val p = scope.withEffectsImpure.props
          movement match {
            case mov: Movement =>
              Callback {
                demoId = demoId + 1
                val id = MatchDuplicate.id(demoId)
                val mdraw = MatchDuplicate.create(id)
                val bs = boards.orElse(s.getBoardset(BoardSet.standard))
                val md = if (bs.isDefined)
                  mdraw.fillBoards(bs.get, mov)
                else
                  mdraw
                Controller.monitor(md.id)
                val sum = DuplicateSummaryStore.getDuplicateSummary
                val nsum = List(DuplicateSummary.create(md))
                val newsum = sum.map(l => l ::: nsum).getOrElse(nsum)
                scalajs.js.timers.setTimeout(5) {
                  BridgeDispatcher.updateDuplicateMatch(md)
                  BridgeDispatcher.updateDuplicateSummary(None, newsum)
                }
                p.routerCtl.toRootPage(PlayDuplicate(DCompleteScoreboardView(md.id.id)))
              }
            case mov: IndividualMovement =>
              scope.modState(_.withMsg("Not supported"))
          }
        } else {
          scope.modState { (s, p) =>
            Alerter.tryit {
              if (resultsOnly) {
                movement match {
                  case mov: Movement =>
                    val result = RestClientDuplicateResult
                      .createDuplicateResult(
                        MatchDuplicateResult.create(),
                        boards = boards.map(_.id),
                        movement = Some(mov.id),
                        test = fortest,
                        default = default
                      )
                      .recordFailure()
                    result.foreach { created =>
                      logger.info(
                        s"Got new duplicate result ${created.id}.  PageNewDuplicate.mounted=${mounted}"
                      )
                      if (mounted)
                        scope.withEffectsImpure.props.routerCtl
                          .toRootPage(PlayDuplicate(DuplicateResultEditView(created.id.id)))
                    }
                    result.failed.foreach(t => {
                      t match {
                        case x: RequestCancelled =>
                        case _ =>
                          scope.withEffectsImpure.modState(s =>
                            s.withMsg("Failed to create a new duplicate result")
                          )
                      }
                    })
                    s.withMsg("Working on creating a new duplicate match")
                  case mov: IndividualMovement =>
                    s.withMsg("Not supported")
                  case _ =>
                    s.withMsg("Internal error")
                }
              } else {
                movement match {
                  case mov: Movement =>
                    val result = Controller
                      .createMatchDuplicate(
                        boards = boards.map(_.id),
                        movement = Some(mov.id),
                        test = fortest,
                        default = default
                      )
                      .recordFailure()
                    result.foreach { created =>
                      logger.info(
                        s"Got new duplicate match ${created.id}.  PageNewDuplicate.mounted=${mounted}"
                      )
                      if (mounted)
                        scope.withEffectsImpure.props.routerCtl
                          .toRootPage(PlayDuplicate(DCompleteScoreboardView(created.id.id)))
                    }
                    result.failed.foreach(t => {
                      t match {
                        case x: RequestCancelled =>
                        case _ =>
                          scope.withEffectsImpure.modState(s =>
                            s.withMsg("Failed to create a new duplicate match")
                          )
                      }
                    })
                    s.withMsg("Working on creating a new duplicate match")
                  case mov: IndividualMovement =>
                    val result = IndividualController
                      .createIndividualDuplicate(
                        boards = boards.map(_.id),
                        movement = Some(mov.id),
                        default = default
                      )
                      .recordFailure()
                    result.foreach { created =>
                      logger.info(
                        s"Got new individual duplicate match ${created.id}.  PageNewDuplicate.mounted=${mounted}"
                      )
                      if (mounted) {
                        scope.withEffectsImpure.props.routerCtl
                          .set(CompleteScoreboardView(created.id.id))
                          .runNow()
                      }
                    }
                    result.failed.foreach(t => {
                      t match {
                        case x: RequestCancelled =>
                        case _ =>
                          scope.withEffectsImpure.modState(s =>
                            s.withMsg("Failed to create a new individual duplicate match")
                          )
                      }
                    })
                    s.withMsg("Working on creating a new individual duplicate match")
                  case _ =>
                    s.withMsg("Internal error")
                }
              }
            }
          }
        }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        import IndividualStyles._

        <.div(
          PopupOkCancel(state.msg, None, Some(cancel)),
          DuplicateBridgeAppBar(
            id = None,
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  "New Duplicate Match"
                )
              )
            ),
            helpurl = "../help/duplicate/new.html",
            routeCtl = props.routerCtl
          )(
          ),
          <.div(
            dupStyles.pageNewDuplicate,
            <.div(
              AppButton(
                "PlayIndividual",
                "Play Individual",
                state.playIndividual ?= baseStyles.buttonSelected,
                ^.onClick --> setPlayingIndividual(true)
              ),
              AppButton(
                "PlayTeams",
                "Play Teams",
                !state.playIndividual ?= baseStyles.buttonSelected,
                ^.onClick --> setPlayingIndividual(false)
              )
            ),
            <.table(
              Header((props, state, this)),
              <.tbody(
                state
                  .getPlayingMovements
                  .filter { mov => !mov.isDisabled }
                  .map { mov =>
                    Row.withKey(mov.nameAsString)((mov, props, state, this))
                  }
                  .toTagMod
              )
            ),
            <.div(
              <.h2("Show movements"),
              if (state.getPlayingMovements.isEmpty) {
                <.p("No movements were found")
              } else {
                state.getPlayingMovements
                  .filter { m => !m.isDisabled }
                  .map { movement =>
                    movement match {
                      case mov: Movement =>
                        val b: TagMod =
                          AppButton(
                            "ShowM_T_" + movement.nameAsString,
                            movement.short,
                            props.routerCtl.onClickToRootPage(PlayDuplicate(DMovementView(mov.id.id)))
                          )
                        b
                      case mov: IndividualMovement =>
                        val b: TagMod =
                          AppButton(
                            "ShowM_I_" + movement.nameAsString,
                            movement.short,
                            props.routerCtl.onClickToRootPage(PlayIndividualDuplicate(MovementView(mov.id.id)))
                          )
                        b
                      case _ =>
                        val b: TagMod = <.span()
                        b
                    }
                  }
                  .toTagMod
              }
            ),
            <.div(
              <.h2("Show Boardsets"),
              if (state.boardsets.isEmpty) {
                <.p("No boardsets were found")
              } else {
                state.boardsets.map { boardset =>
                  val clickToBoardset = PlayDuplicate(BoardSetView(boardset.id.id))
                  AppButton(
                    "ShowB_" + boardset.id.id,
                    boardset.short,
                    props.routerCtl.onClickToRootPage(clickToBoardset)
                  )
                }.toTagMod
              }
            )
          )
        )
      }

      val storeCallback: Callback = Callback {
        val boardsets = BoardSetStore.getBoardSets().values.toList
        val movements = BoardSetStore.getMovement().values.toList
        val imovements = BoardSetStore.getIndividualMovement().values.toList
        logger.info(
          s"Got boardsets=${boardsets.size} movements=${movements.size}"
        )
        scope.withEffectsImpure.modState(s =>
          s.withBoardsetsAndMovements(
            boardsets,
            movements,
            imovements
          )
        )
      }

      val didMount: Callback = Callback {
        mounted = true
        logger.info("PageNewDuplicate.didMount")
        BoardSetStore.addChangeListener(storeCallback)

  //      BoardSetController.getBoardSets()
  //      BoardSetController.getMovement()
        BoardSetController.getBoardsetsAndMovements()
      }

      val willUnmount: Callback = Callback {
        mounted = false
        logger.info("PageNewDuplicate.willUnmount")
        BoardSetStore.removeChangeListener(storeCallback)
      }
    }

    val component = ScalaComponent
      .builder[Props]("PageNewDuplicate")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
