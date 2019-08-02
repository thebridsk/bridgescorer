package com.github.thebridsk.bridge.client.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.BoardSetStore
import com.github.thebridsk.bridge.client.controller.BoardSetController
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Popup
import com.github.thebridsk.bridge.clientcommon.rest2.ResultHolder
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.clientcommon.rest2.RequestCancelled
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule.PlayDuplicate
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicateResult
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.DuplicateResultEditView
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import scala.annotation.tailrec
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.Bridge
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.client.bridge.store.DuplicateSummaryStore
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo

/**
 * PageNewDuplicate.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageNewDuplicate( PageNewDuplicate.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageNewDuplicate {
  import PageNewDuplicateInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage ) = component(Props(routerCtl,page))

}

object PageNewDuplicateInternal {
  import PageNewDuplicate._
  import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancelImplicits._

  val logger = Logger("bridge.PageNewDuplicate")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( boardsets: Map[String, BoardSet],
                    movements: Map[String, Movement],
                    workingOnNew: Option[String],
                    resultsOnly: Boolean = false
                  ) {
    def boardsetNames() = boardsets.keySet.toList.sorted
    def movementNames() = movements.keySet.toList.sorted
  }

  def canPlay( movement: Movement, boardset: BoardSet ) = {
    val movboards = movement.getBoards
    val boards = boardset.boards.map(b => b.id)
    movboards.find( b => !boards.contains(b) ).isEmpty
  }

  def missingBoards( movement: Movement, boardset: BoardSet ) = {
    val movboards = movement.getBoards
    val boards = boardset.boards.map(b => b.id)
    movboards.filter( b => !boards.contains(b) )
  }

  def intToString( list: List[Int] ) = {

    def appendNext( s: String, n: Int ) = {
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
    def next( s: String, openRange: Boolean, last: Int, l: List[Int] ): String = {
      if (l.isEmpty) {
        if (openRange) {
          s"${s}-${last}"
        } else {
          appendNext(s,last)
        }
      } else {
        val n = l.head
        if (openRange) {
          if (last+1 == n) {
            next(s,true,n,l.tail)
          } else {
            next(s"${s}-${last}",false,n,l.tail)
          }
        } else {
          if (last+1 == n) {
            next(appendNext(s,last),true,n,l.tail)
          } else {
            next(appendNext(s,last),false,n,l.tail)
          }
        }
      }
    }

    if (list.isEmpty) ""
    else next( "",false,list.head,list.tail)
  }

  val Header = ScalaComponent.builder[(Props,State,Backend)]("PageNewDuplicate.Header")
                        .render_P( args => {
                          val (props,state,backend) = args
                          val n = if (state.boardsets.isEmpty) 1 else state.boardsets.size
                          <.thead(
                            <.tr(
                              <.th( "Movement", ^.rowSpan := 1 ),
                              <.th( "Boards", ^.colSpan := n )
                            )
//                            <.th(
//                              if (state.boardsets.isEmpty) {
//                                <.td("?")
//                              } else {
//                                state.boardsetNames().map { bsname => {
//                                  <.td( bsname )
//                                }}
//                              }
//                            )
                          )
                        }).build

  val Row = ScalaComponent.builder[(String,Props,State,Backend)]("PageNewDuplicate.Row")
                        .render_P( args => {
                          val (movementid,props,state,backend) = args
                          val movement = state.movements(movementid)
                          <.tr(
                            <.td( movement.short ),
                            state.boardsetNames().map { bsname => {
                              val boardset = state.boardsets(bsname)
                              val missing = missingBoards(movement, boardset)
                              <.td(
                                if (missing.isEmpty) {
                                  AppButton( "New_"+movement.name+"_"+bsname, boardset.short,
                                            ^.onClick --> backend.newDuplicate( boards=Some(bsname),
                                                                                movement=Some(movementid))
                                  )
                                } else {
                                  TagMod(
                                    "Missing boards",
                                    <.br,
                                    intToString(missing.sorted)
                                  )
                                }
                              )
                            }}.toTagMod
                          )
                        }).build

  var demoId = 0

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    private var mounted = false

    val resultDuplicate = ResultHolder[MatchDuplicate]()

    val cancel = Callback {
      resultDuplicate.cancel()
    } >> scope.modState( s => s.copy(workingOnNew=None))

    import scala.concurrent.ExecutionContext.Implicits.global
    def newDuplicate(
                      default: Boolean = true,
                      boards: Option[String] = None,
                      movement: Option[String] = None,
                      fortest: Boolean = false ) =
        if (BridgeDemo.isDemo) {
          Callback {
            val s = scope.withEffectsImpure.state
            val p = scope.withEffectsImpure.props
            demoId = demoId + 1
            val id = s"M${demoId}"
            val mdraw = MatchDuplicate.create(id)
            val bs = s.boardsets(boards.getOrElse("StandardBoards"))
            val mov = s.movements( movement.getOrElse("Howell04T2B18"))
            val md = mdraw.fillBoards(bs, mov)
            Controller.monitor(md.id)
            val sum = DuplicateSummaryStore.getDuplicateSummary()
            val nsum = List(DuplicateSummary.create(md))
            val newsum = sum.map( l => l:::nsum).getOrElse(nsum)
            scalajs.js.timers.setTimeout(5) {
              BridgeDispatcher.updateDuplicateMatch(md)
              BridgeDispatcher.updateDuplicateSummary(None,newsum)
            }
            p.routerCtl.set(CompleteScoreboardView(md.id)).runNow()
          }
        } else {
          scope.modState { (s,p) =>
            Alerter.tryitWithUnit {
              if (s.resultsOnly) {
                val result = RestClientDuplicateResult.createDuplicateResult( MatchDuplicateResult.create(), boards=boards,movement=movement,test=fortest,default=default).recordFailure()
                result.foreach { created=>
                  logger.info(s"Got new duplicate result ${created.id}.  PageNewDuplicate.mounted=${mounted}")
                  if (mounted) scope.withEffectsImpure.props.routerCtl.set(DuplicateResultEditView(created.id)).runNow()
                }
                result.failed.foreach( t => {
                  t match {
                    case x: RequestCancelled =>
                    case _ =>
                      scope.withEffectsImpure.modState( s => s.copy(workingOnNew=Some("Failed to create a new duplicate result")))
                  }
                })
              } else {
                val result = Controller.createMatchDuplicate(boards=boards,movement=movement,test=fortest,default=default).recordFailure()
                result.foreach { created=>
                  logger.info(s"Got new duplicate match ${created.id}.  PageNewDuplicate.mounted=${mounted}")
                  if (mounted) scope.withEffectsImpure.props.routerCtl.set(CompleteScoreboardView(created.id)).runNow()
                }
                result.failed.foreach( t => {
                  t match {
                    case x: RequestCancelled =>
                    case _ =>
                      scope.withEffectsImpure.modState( s => s.copy(workingOnNew=Some("Failed to create a new duplicate match")))
                  }
                })
              }
            }
            s.copy(workingOnNew=Some("Working on creating a new duplicate match"))
          }
        }

    val resultsOnlyToggle = scope.modState( s => s.copy( resultsOnly = !s.resultsOnly ) )

    def render( props: Props, state: State ) = {
      import DuplicateStyles._
      val movementNames = state.movementNames()
      val boardsetNames = state.boardsetNames()
      <.div(
        PopupOkCancel( state.workingOnNew, None, Some(cancel) ),
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "New Duplicate Match",
                    )
                )),
          helpurl = "../help/duplicate/new.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          dupStyles.divNewDuplicate,
          CheckBox("resultsOnly", "Create Results Only", state.resultsOnly, resultsOnlyToggle ),
          <.table(
            dupStyles.tableNewDuplicate,
            Header((props,state,this)),
            <.tbody(
              state.movementNames().map { movname => {
                Row.withKey(movname)((movname,props,state,this))
              }}.toTagMod
            )
          ),
          <.div(
            <.h2("Show movements"),
            if (movementNames.isEmpty) {
              <.p("No movements were found")
            } else {
              movementNames.map { movname => {
                val movement = state.movements(movname)
                val clickToMovement = MovementView(movname)
                AppButton( "ShowM_"+movname, movement.short,
                           props.routerCtl.setOnClick( clickToMovement )
                )
              }}.toTagMod
            }
          ),
          <.div(
            <.h2("Show Boardsets"),
            if (boardsetNames.isEmpty) {
              <.p("No boardsets were found")
            } else {
              boardsetNames.map { bsname => {
                val boardset = state.boardsets(bsname)
                val clickToBoardset = BoardSetView(bsname)
                AppButton( "ShowB_"+bsname, boardset.short,
                           props.routerCtl.setOnClick( clickToBoardset )
                )
              }}.toTagMod
            }
          )
        )
      )
    }

    val storeCallback = Callback {
      val boardsets = BoardSetStore.getBoardSets()
      val movements = BoardSetStore.getMovement()
      logger.info(s"Got boardsets=${boardsets.size} movements=${movements.size}" )
      scope.withEffectsImpure.modState(s => s.copy( boardsets=boardsets, movements=movements))
    }

    val didMount = Callback {
      mounted = true
      logger.info("PageNewDuplicate.didMount")
      BoardSetStore.addChangeListener(storeCallback)

//      BoardSetController.getBoardSets()
//      BoardSetController.getMovement()
      BoardSetController.getBoardsetsAndMovements()
    }

    val willUnmount = Callback {
      mounted = false
      logger.info("PageNewDuplicate.willUnmount")
      BoardSetStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageNewDuplicate")
                            .initialStateFromProps { props => State(Map(),Map(), None) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

