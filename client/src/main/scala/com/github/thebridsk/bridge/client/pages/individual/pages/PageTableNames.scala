package com.github.thebridsk.bridge.client.pages.individual.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.TableNamesView
import com.github.thebridsk.bridge.client.pages.individual.components.DuplicateBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles.dupStyles
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles.baseStyles
import com.github.thebridsk.bridge.rotation.{ Table => RTable }
import com.github.thebridsk.bridge.client.bridge.store.IndividualDuplicateStore
import com.github.thebridsk.bridge.client.pages.HomePage
import com.github.thebridsk.bridge.client.controller.IndividualController
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.components.EnterName
import com.github.thebridsk.bridge.clientcommon.react.Button
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.West
import com.github.thebridsk.bridge.client.pages.hand.PageHand
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseBoardView
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import scala.util.Failure
import scala.util.Success
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.utilities.logging.Logger

/**
  * A component that shows the players and their position at the table.
  *
  * If a name has not been entered yet, an input field is displayed to enter the name.
  *
  * Selecting the scorekeeper is also possible.
  *
  * To use, just code the following:
  *
  * {{{
  * PageTableNames(
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageTableNames {
  import Internal._

  case class Props(
    router: BridgeRouter[IndividualDuplicatePage],
    page: TableNamesView
  )

  /**
    *
    *
    * @return the unmounted react component
    *
    * @see [[PageTableNames$]] for usage.
    */
  def apply(
    router: BridgeRouter[IndividualDuplicatePage],
    page: TableNamesView
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(router,page))

  protected object Internal {

    val log = Logger("bridge.PageTableNames")

    /**
      *
      *
      * @param scorekeeper
      * @param originalTable
      * @param table
      * @param north
      * @param players the players, the order is north, south, east, west
      */
    case class State(
      scorekeeper: Option[RTable.Location] = None,
      originalTable: Option[RTable] = None,
      table: Option[RTable] = None,
      north: Option[IndividualDuplicateHand.Id] = None,
      players: List[Int] = List(),
      msg: Option[String] = None
    ) {

      def getScorekeeper = scorekeeper.getOrElse(RTable.North)

      def props(props: Props) = {
        this
      }

      def clearMsg() = copy(msg = None)

      def setMsg(msg: String) = copy(msg = Some(msg))
    }

    /**
      * @param props
      * @param md
      * @return a rotation table, north, and all the player ids
      */
    def getPlayers(
      props: Props,
      md: IndividualDuplicate
    ): (
      Option[RTable],
      Option[IndividualDuplicateHand.Id],
      List[Int]
    ) = {
      val table = props.page.tableid
      val round = props.page.round

      md.getHandsInRound(table,round).headOption.map { hand =>
        (
          Some(
            RTable(
              md.getPlayer(hand.north),
              md.getPlayer(hand.south),
              md.getPlayer(hand.east),
              md.getPlayer(hand.west),
              ""
            )
          ),
          Some(IndividualDuplicateHand.id(hand.north)),
          List(hand.north, hand.south, hand.east, hand.west)
        )
      }.getOrElse((Some(RTable("","","","","")),None,List()))
    }

    private val locationMap = Map(
      RTable.North -> North,
      RTable.South -> South,
      RTable.East -> East,
      RTable.West -> West,
    )

    class Backend(scope: BackendScope[Props, State]) {

      def setScorekeeper(loc: RTable.Location) = scope.modState(_.copy(scorekeeper = Some(loc)))

      def setName(loc: RTable.Location)(name: String) = scope.modState { s =>
        s.copy(table = s.table.map(_.setPlayer(loc,name)))
      }

      val okCB = scope.modState { (state, props) =>
        PageHand.scorekeeper = state.scorekeeper.flatMap(locationMap.get(_)).getOrElse(North)

        if (state.table != state.originalTable) {
          IndividualDuplicateStore.getMatch() match {
            case Some(md) if md.id == props.page.dupid =>
              state.table.foreach { table =>
                import scala.concurrent.ExecutionContext.Implicits.global
                IndividualController.updatePlayers(
                  md,
                  state.players.zip(table.north::table.south::table.east::table.west::Nil)
                      .map { e =>
                        val (ip, name) = e
                        ip -> name
                      }.toMap
                ).recordFailure()
                .onComplete { tryresult =>
                  tryresult match {
                    case Success(v) =>
                      log.fine(s"Updated player names to ${v}, mounted=${mounted}")
                      if (mounted) {
                        props.router.toPage(props.page.toNextView match {
                          case p: BaseBoardView =>
                            state.north.map(n => p.toHandView(n)).getOrElse(p)
                          case p => p
                        })
                      }
                    case Failure(ex) =>
                      scope.withEffectsImpure.modState(_.setMsg(ex.getMessage()))
                  }
                }
              }
            case _ =>
          }
          state.setMsg("Updating names")
        } else {
          props.router.toPage(props.page.toNextView match {
            case p: BaseBoardView =>
              state.north.map(n => p.toHandView(n)).getOrElse(p)
            case p => p
          })
          state
        }
      }

      val resetCB = scope.modState{ s =>
        s.copy(scorekeeper = None, table = s.originalTable)
      }

      val cancelMsgCB = scope.modState(_.clearMsg())

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          DuplicateBridgeAppBar(
            id = Some(props.page.dupid),
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  s"Table ${props.page.tableid.toNumber}"
                )
              )
            ),
            helpurl = "../help/duplicate/table.html",
            routeCtl = props.router
          )(
          ),
          PopupOkCancel(
            content = state.msg.map(m => m),
            cancel = Some(cancelMsgCB),
            ok = None
          ),
          IndividualDuplicateStore.getMatch() match {
            case Some(dup) =>
              state.table match {
                case Some(order) =>
                  val valid = order.arePlayersValid() && state.scorekeeper.isDefined
                  def player(loc: RTable.Location) = {
                    val useInput = state.originalTable.map(_.find(loc).isEmpty()).getOrElse(true)
                    <.div(
                      <.div(loc.toString()),
                      <.div(
                        if (useInput) {
                          EnterName(
                            loc.toString(),
                            state.table.map(_.find(loc)).getOrElse(""),
                            -1,
                            setName(loc) _
                          )
                        } else {
                          <.span(
                            ^.id := s"${loc.toString()}_name",
                            state.table.map(_.find(loc))
                          )
                        }
                      ),
                      AppButton(
                        s"SK_${loc}",
                        "Set ScoreKeeper",
                        ^.onClick --> setScorekeeper(loc),
                        BaseStyles.highlight(
                          selected = state.scorekeeper.map(_ == loc).getOrElse(false),
                          required = state.scorekeeper.isEmpty
                        )
                      )
                    )
                  }
                  <.div(
                    dupStyles.pageTableNames,
                    <.div(
                      order.getPlayerOrder(state.getScorekeeper).map { loc =>
                        player(loc)
                      }.toTagMod
                    ),
                    <.div(
                      baseStyles.divFooter,
                      <.div(
                        baseStyles.divFooterLeft,
                        Button(
                          baseStyles.footerButton,
                          "OK",
                          "OK",
                          ^.onClick --> okCB,
                          ^.disabled := !valid,
                          BaseStyles.highlight(required = valid)
                        )
                      ),
                      <.div(
                        baseStyles.divFooterCenter,
                        Button(
                          baseStyles.footerButton,
                          "Reset",
                          "Reset",
                          ^.onClick --> resetCB
                        )
                      ),
                      <.div(
                        baseStyles.divFooterRight,
                        Button(
                          baseStyles.footerButton,
                          "Cancel",
                          "Cancel",
                          props.router.setOnClick(props.page.toTableView)
                        )
                        //                    helppage.whenDefined( p => HelpButton(p) )
                      )
                    )
                  )
                case None =>
                  HomePage.loading
              }
            case None =>
              HomePage.loading
          }
        )
      }

      private var mounted = false

      val storeCallback = scope.modStateOption { (state, props) =>
        IndividualDuplicateStore.getId() match {
          case Some(storeid) if storeid == props.page.dupid =>
            IndividualDuplicateStore.getMatch() match {
              case Some(md) =>
                if (state.table == None) {
                  val (p, n, allplayers) = getPlayers(props, md)
                  if (state.originalTable == p) {
                    Some(state)
                  } else {
                    Some(state.copy(table = p, originalTable = p, north = n, players = allplayers))
                  }
                } else {
                  Some(state)
                }
              case None =>
                None
            }
          case _ =>
            None
        }
      }

      val didMount: Callback = scope.props >>= { p =>
        Callback {
          mounted = true
          IndividualDuplicateStore.addChangeListener(storeCallback)
          IndividualController.monitor(p.page.dupid)
        }
      }

      val willUnmount: Callback = Callback {
        mounted = false
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
          // props have change, reinitialize state
          IndividualController.monitor(props.page.dupid)
          cdu.mountedImpure.setState(State())
        }
      }

    val component = ScalaComponent
      .builder[Props]("PageTableNames")
      .initialStateFromProps { props =>
        IndividualDuplicateStore.getMatch() match {
          case Some(md) =>
            val (p, north, allplayers) = getPlayers(props, md)
            State(None, p, p, north, allplayers)
          case None =>
            State()
        }
      }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate _)
      .build
  }

}
