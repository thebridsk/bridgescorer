package com.github.thebridsk.bridge.client.pages.individual.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.BoardSetStore
import com.github.thebridsk.bridge.client.controller.BoardSetController
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import scala.annotation.tailrec
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles
import com.github.thebridsk.bridge.client.pages.individual.components.DuplicateBridgeAppBar
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.MovementBase
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.MovementView
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.MovementSummaryView
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.MovementNewView
import com.github.thebridsk.bridge.client.pages.duplicate.boardsets.PageMovementsInternal
import org.scalajs.dom.html
import com.github.thebridsk.bridge.client.pages.individual.components.ViewTable
import com.github.thebridsk.bridge.data.Table
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

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
  * PageMovements(
  *   routerCtl = ...,
  *   page = NewDuplicateView
  * )
  * }}}
  *
  * @author werewolf
  */
object PageMovements {
  import Internal._

  case class Props(
    routerCtl: BridgeRouter[IndividualDuplicatePage],
    backpage: IndividualDuplicatePage,
    initialDisplay: Option[MovementBase.Id]
  )

  /**
    * Instantiate the component
    *
    * @param routerCtl
    * @param backpage the page to return to when the OK button is hit.
    * @param initialDisplay
    * @return the unmounted react component
    */
  def apply(
    routerCtl: BridgeRouter[IndividualDuplicatePage],
    backpage: IndividualDuplicatePage,
    initialDisplay: Option[MovementBase.Id]
  ) =
    component(
      Props(routerCtl, backpage, initialDisplay)
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

    val logger: Logger = Logger("bridge.PageMovements")

    case class State(
        initialDisplay: Option[MovementBase.Id],
        tmovements: List[Movement] = List.empty,
        imovements: List[IndividualMovement] = List.empty,
        movements: List[MovementBase] = List.empty,
        msg: Option[String] = None,
        playIndividual: Boolean = false
    ) {
      def getTeamMovements: List[MovementBase] = tmovements
      def getIndividualMovements: List[MovementBase] = imovements

      def getPlayingMovements: List[MovementBase] = if (playIndividual) imovements else tmovements

      def withPlayingIndividual(f: Boolean) = {
        copy(playIndividual = f, initialDisplay = if (f == playIndividual) initialDisplay else None)
      }

      def withMovements(l: List[Movement]) = {
        val a = (l:::imovements).sorted
        copy(tmovements = l.sorted, movements = a).updateInitialDisplay()
      }
      def withIndividaulMovements(l: List[IndividualMovement]) = {
        val a = (l:::tmovements).sorted
        copy(imovements = l.sorted, movements = a).updateInitialDisplay()
      }

      def withMovements(
        mov: List[Movement],
        imov: List[IndividualMovement]
      ) = {
        val a = (mov:::imov).sorted
        copy(tmovements = mov.sorted, imovements = imov.sorted, movements = a).updateInitialDisplay()
      }

      def setInitialDisplay(display: Option[MovementBase.Id]) = {
        copy(initialDisplay = display).updateInitialDisplay()
      }

      def updateInitialDisplay() = {
        initialDisplay
          .flatMap { in =>
            movements.find { m =>
              m.nameAsString == in.id
            }.map( m => copy(initialDisplay = Some(m.getId)))
          }.getOrElse(this)
      }

      def withMsg(m: String) = copy(msg = Some(m))
      def clearMsg() = copy(msg = None)

      def getMovement(id: Movement.Id) = tmovements.find(_.id == id)
      def getIndividualMovement(id: IndividualMovement.Id) = imovements.find(_.id == id)
    }

    private val Header = ScalaComponent
      .builder[(Props, State, Backend)]("PageMovements.Header")
      .render_P(args => {
        val (props, state, backend) = args
        <.thead(
          <.tr(
            <.th("Name"),
            <.th("Description"),
            <.th("Actions")
          )
        )
      })
      .build

    private val Row = ScalaComponent
      .builder[(MovementBase, Props, State, Backend, Callback, Option[MovementBase.Id])]("PageMovements.Row")
      .render_P(args => {
        val (mov, props, state, backend, toggle, selected) = args
        val sel = selected.map(s => s == mov.getId).getOrElse(false)
        val disabled = mov.isDisabled
        <.tr(
          <.td(
            AppButton(
              mov.nameAsString,
              mov.short,
              BaseStyles.highlight(selected = sel),
              ^.onClick --> toggle
            )
          ),
          <.td(
            disabled ?= "Disabled, ",
            mov.description
          ),
          <.td(
            AppButton(
              s"${mov.nameAsString}_edit",
              "Edit",
              // props.routerCtl.setOnClick(MovementEditView(mov.name.id))
              ^.onClick --> backend.editCB(mov.getId)
            ),
            mov.isDeletable ?= AppButton(
              s"${mov.nameAsString}_delete",
              "Delete",
              ^.onClick --> backend.deleteCB(mov.getId)
            ),
            mov.isResetToDefault ?= AppButton(
              s"${mov.nameAsString}_reset",
              "Reset",
              ^.onClick --> backend.resetCB(mov.getId)
            )
          )
        )
      })
      .build

    var demoId = 0

    class Backend(scope: BackendScope[Props, State]) {

      private var mounted = false

      private val movementTableRef = Ref[html.Element]

      def setPlayingIndividual(f: Boolean): Callback = scope.modState(s => s.withPlayingIndividual(f))

      val cancel: Callback = Callback {
        // resultDuplicate.cancel()
      } >> scope.modState(s => s.clearMsg())

      val okCallback: Callback = scope.forceUpdate >> scope.props >>= { props =>
        props.routerCtl.set(props.backpage)
      }

      def toggle(name: MovementBase.Id): Callback =
        scope.stateProps { (state,props) =>
          state.initialDisplay match {
            case Some(s) if s == name => props.routerCtl.set(MovementSummaryView)
            case _                    => props.routerCtl.set(MovementView(name.id))
          }
        }

      def editCB(id: MovementBase.Id): Callback = Callback {

      }

      def deleteCB(id: MovementBase.Id): Callback = Callback {

      }

      def resetCB(id: MovementBase.Id): Callback = Callback {

      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        import IndividualStyles._

        logger.fine(s"PageMovements props.initialDisplay=${props.initialDisplay}, state=${state}")

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
                  "Movements"
                )
              )
            ),
            helpurl = "../help/duplicate/summary.html",
            routeCtl = props.routerCtl
          )(
          ),
          <.div(
            dupStyles.pageMovements,
            <.div(
              AppButton(
                "PlayIndividual",
                "Individual",
                state.playIndividual ?= baseStyles.buttonSelected,
                ^.onClick --> setPlayingIndividual(true)
              ),
              AppButton(
                "PlayTeams",
                "Teams",
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
                    Row.withKey(mov.nameAsString)(
                      (mov, props, state, this, toggle(mov.getId), state.initialDisplay)
                    )
                  }
                  .toTagMod
              )
            ),
            AppButton("OK", "OK", ^.onClick --> okCallback),
            AppButton("New", "New", props.routerCtl.setOnClick(MovementNewView)),
            state.initialDisplay match {
              case Some(name) =>
                state.movements.find(_.getId == name) match {
                  case Some(movb) =>
                    TagMod(
                      <.h1("Showing ", movb.short),
                      <.p(movb.description),
                      <.div.withRef(movementTableRef)(
                        MovementBase.use(
                          movb,
                          { mov =>
                            mov.hands
                              .map(h => h.table)
                              .toList
                              .distinct
                              .sorted
                              .map { table =>
                                PageMovementsInternal.MovementTable(
                                  (mov, table)
                                )
                              }
                              .toTagMod
                          },
                          { imov =>
                            imov.hands
                              .map(h => h.table)
                              .toList
                              .distinct
                              .sorted
                              .map { table =>
                                ViewTable(
                                  props.routerCtl,
                                  Table.id(table),
                                  imov
                                )
                              }
                              .toTagMod
                          },
                          <.span("Unknown movement type")
                        )
                      )
                    )
                  case None =>
                    <.span(s"Movement $name not found")
                }
              case None =>
                <.span()
            }

          )
        )
      }

      val storeCallback: Callback = Callback {
        val movements = BoardSetStore.getMovement().values.toList
        val imovements = BoardSetStore.getIndividualMovement().values.toList
        logger.info(
          s"Got imovements=${imovements.size} movements=${movements.size}"
        )
        scope.withEffectsImpure.modState(s =>
          s.withMovements(
            movements,
            imovements
          )
        )
      }

      val scrollToCB: Callback = movementTableRef.get
        .map( re => re.scrollIntoView(false))
        .asCallback
        .void

      val didMount: Callback = Callback {
        mounted = true
        logger.info("PageMovements.didMount")
        BoardSetStore.addChangeListener(storeCallback)
        BoardSetController.getBoardsetsAndMovements()
      } >> scrollToCB

      val willUnmount: Callback = Callback {
        mounted = false
        logger.info("PageMovements.willUnmount")
        BoardSetStore.removeChangeListener(storeCallback)
      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (props.initialDisplay != prevProps.initialDisplay) {
          cdu.modState{ s =>
            s.setInitialDisplay(props.initialDisplay)
          }.runNow()
          cdu.backend.scrollToCB.runNow()
        }
      }

    val component = ScalaComponent
      .builder[Props]("PageMovements")
      .initialStateFromProps { props => State(props.initialDisplay) }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentDidUpdate(didUpdate)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
