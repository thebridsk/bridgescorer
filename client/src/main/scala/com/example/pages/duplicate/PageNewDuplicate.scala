package com.example.pages.duplicate

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import utils.logging.Logger
import com.example.bridge.store.BoardSetStore
import com.example.controller.BoardSetController
import com.example.data.BoardSet
import com.example.data.Movement
import com.example.controller.Controller
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.pages.duplicate.DuplicateRouter.MovementView
import com.example.pages.duplicate.DuplicateRouter.BoardSetView
import com.example.react.AppButton
import com.example.react.Popup
import com.example.rest2.ResultHolder
import com.example.data.MatchDuplicate
import com.example.rest2.RequestCancelled
import com.example.pages.duplicate.DuplicateModule.PlayDuplicate
import com.example.react.PopupOkCancel
import com.example.logger.Alerter
import com.example.rest2.RestClientDuplicateResult
import com.example.data.MatchDuplicateResult
import com.example.pages.duplicate.DuplicateRouter.DuplicateResultEditView
import com.example.react.CheckBox

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

  case class Props( routerCtl: RouterCtl[DuplicatePage], page: DuplicatePage )

  def apply( routerCtl: RouterCtl[DuplicatePage], page: DuplicatePage ) = component(Props(routerCtl,page))

}

object PageNewDuplicateInternal {
  import PageNewDuplicate._
  import com.example.react.PopupOkCancelImplicits._

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
                              <.td(
                                AppButton( "New_"+movement.name+"_"+bsname, boardset.short,
                                          ^.onClick --> backend.newDuplicate( boards=Some(bsname),
                                                                              movement=Some(movementid))
                                )
                              )
                            }}.toTagMod
                          )
                        }).build

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
      scope.modState { s =>
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

    val resultsOnlyToggle = scope.modState( s => s.copy( resultsOnly = !s.resultsOnly ) )

    def render( props: Props, state: State ) = {
      import DuplicateStyles._
      val movementNames = state.movementNames()
      val boardsetNames = state.boardsetNames()
      <.div(
        dupStyles.divNewDuplicate,
        PopupOkCancel( state.workingOnNew, None, Some(cancel) ),
        <.h1("New Duplicate Match"),
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

