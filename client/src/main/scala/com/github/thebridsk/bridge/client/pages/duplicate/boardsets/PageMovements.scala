package com.github.thebridsk.bridge.client.pages.duplicate.boardsets

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.bridge.store.BoardSetStore
import com.github.thebridsk.bridge.client.controller.BoardSetController
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.data.BoardInSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.HandInTable
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicatePage
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateStyles
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicatePageBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementEditView
import _root_.com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementNewView
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientMovement
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageMovements( PageMovements.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageMovements {
  import PageMovementsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[Movement.Id] )

  def apply( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[Movement.Id] ) = component(Props(routerCtl,backpage,initialDisplay))

}

object PageMovementsInternal {
  import PageMovements._
  import com.github.thebridsk.bridge.clientcommon.react.Utils._
  import DuplicateStyles._

  val logger = Logger("bridge.PageMovements")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   * @param boardSets all the boardsets
   */
  case class State( movements: Map[Movement.Id,Movement] = Map(), msg: Option[TagMod] = None ) {

    def setMsg( msg: String ) = copy( msg = Some(msg))
    def setMsg( msg: TagMod ) = copy( msg = Some(msg))
    def clearMsg() = copy( msg = None )

  }

  val SummaryHeader = ScalaComponent.builder[State]("PageMovements.SummaryHeader")
                    .render_P( state => {
                      <.tr(
                        <.th( "Name" ),
                        <.th( "Description" ),
                        <.th( "Action" )
                      )
                    }).build

  val SummaryRow = ScalaComponent.builder[(Backend,Props,State,Movement.Id,Callback,Option[Movement.Id])]("PageMovements.SummaryRow")
                    .render_P( args => {
                      val (backend,props,state,current,toggle,selected) = args
                      val mov = state.movements(current)
                      val sel = selected.map( s => s==current ).getOrElse(false)
                      val disabled = mov.isDisabled
                      <.tr(
                        <.td(
                          AppButton( mov.name.id, mov.short, BaseStyles.highlight(selected = sel), ^.onClick-->toggle )
                        ),
                        <.td(
                          disabled ?= "Disabled, ",
                          mov.description
                        ),
                        <.td(
                          AppButton( s"${mov.name}_edit", "Edit", props.routerCtl.setOnClick(MovementEditView(mov.name.id)) ),
                          mov.isDeletable ?= AppButton( s"${mov.name}_delete", "Delete", ^.onClick --> backend.deleteCB(mov.id) ),
                          mov.isResetToDefault ?= AppButton( s"${mov.name}_reset", "Reset", ^.onClick --> backend.resetCB(mov.id) ),
                        )
                      )
                    }).build

  val BoardHeader = ScalaComponent.builder[(State,Int,Boolean)]("PageMovements.BoardHeader")
                    .render_P( props => {
                      val (state,table,relay) = props
                      <.thead(
                        <.tr(
                          <.th( ^.colSpan:=(if (relay) 5 else 4), "Table "+table )
                        ),
                        <.tr(
                          <.th( "Round" ),
                          <.th( "NS" ),
                          <.th( "EW" ),
                          <.th( "Boards" ),
                          if (relay) <.th( "Relay" )
                          else TagMod()
                        )
                      )
                    }).build

  val BoardRow = ScalaComponent.builder[(State,Movement,HandInTable,Boolean)]("PageMovements.BoardRow")
                    .render_P( props => {
                      val (state,movement,hit,relay) = props
                      <.tr(
                        <.td( hit.round),
                        <.td( hit.ns),
                        <.td( hit.ew),
                        <.td( hit.boards.mkString(", ")),
                        if (relay) {
                          val relaytables = movement.tableRoundRelay(hit.table, hit.round)
                          <.td( relaytables.mkString(", ") )
                        } else {
                          TagMod()
                        }
                      )
                    }).build

  val MovementTable = ScalaComponent.builder[(State,Movement,Int)]("PageMovements.MovementTable")
                        .render_P( props => {
                          val (state,htp,table) = props
                          val relay = htp.matchHasRelay
                          <.div(
                            <.table(
                              BoardHeader((state,table,relay)),
                              <.tbody(
                                htp.hands.filter( h => h.table == table).sortWith( (h1,h2)=> {
                                    val t1 = h1.table
                                    val t2 = h2.table
                                    if (t1 == t2) {
                                      h1.round<h2.round
                                    } else {
                                      t1<t2
                                    }
                                  } ).map { h =>
                                  BoardRow.withKey( s"${h.table}-${h.round}" )((state,htp,h,relay))
                                }.toTagMod
                              )
                            )
                          )
                        }).build

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend( scope: BackendScope[Props, State]) {

    private val movementTableRef = Ref.toScalaComponent(MovementTable)

    val popupCancel = Callback {

    } >> scope.modState( s => s.clearMsg())

    val okCallback = scope.forceUpdate >> scope.props >>= { props => props.routerCtl.set(props.backpage) }

    def toggleBoardSet( name: Movement.Id ) = scope.props >>= { props =>
      props.initialDisplay match {
        case Some(s) if s == name => props.routerCtl.set(MovementSummaryView)
        case _ => props.routerCtl.set(MovementView(name.id))
      }
    }

    def deleteCB( id: Movement.Id ) = scope.modState(
      { s =>
        s.setMsg(s"Deleting movement $id")
      },
      Callback {
        BoardSetController.deleteMovement(id).recordFailure().onComplete { tr =>
          logger.fine(s"movement $id deleted: $tr")
          tr match {
            case Success(x) =>
              scope.withEffectsImpure.modState( _.setMsg(s"Deleted movement $id"))
            case Failure(ex) =>
              scope.withEffectsImpure.modState( _.setMsg(s"Error deleting movement $id"))
          }
        }
      }
    )

    def resetCB( id: Movement.Id ) = scope.modState(
      { s =>
        s.setMsg(s"Resetting movement $id to default")
      },
      Callback {
        BoardSetController.deleteMovement(id).recordFailure().onComplete { tr =>
          logger.fine(s"movement $id reset: $tr")
          tr match {
            case Success(x) =>
              BoardSetController.getMovement()
              scope.withEffectsImpure.modState( _.setMsg(s"Movement $id was reset to default"))
            case Failure(ex) =>
              scope.withEffectsImpure.modState( _.setMsg(s"Error resetting movement $id to default"))
          }
        }
      }
    )

    def render( props: Props, state: State ) = {
      logger.info("PageMovements.Backend.render: display "+props.initialDisplay)
      <.div(
        PopupOkCancel( state.msg, None, Some(popupCancel) ),
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Movements",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          dupStyles.divMovementsPage,
          <.table(
            <.thead(
              SummaryHeader(state)
            ),
            <.tbody(
              state.movements.keySet.toList.sortWith( (t1,t2)=>t1<t2 ).map { name =>
                SummaryRow.withKey( name.id )((this,props,state,name,toggleBoardSet(name),props.initialDisplay))
              }.toTagMod
            )
          ),
          AppButton( "OK", "OK", ^.onClick-->okCallback ),
          AppButton( "New", "New", props.routerCtl.setOnClick(MovementNewView) ),
          props.initialDisplay match {
            case Some(name) =>
              state.movements.get(name) match {
                case Some(htp) =>
                  TagMod(
                    <.h1("Showing ", htp.short ),
                    <.p( htp.description ),
                    <.div(
                      dupStyles.divMovementView,
                      htp.hands.map( h => h.table ).toList.distinct.sorted.map { table =>
                        MovementTable.withRef(movementTableRef)((state,htp,table))
                      }.toTagMod
                    )
                  )
                case None =>
                  <.span(s"BoardSet $name not found")
              }
            case None =>
              <.span()
          }
        )
      )
    }

    def forceUpdate = scope.withEffectsImpure.forceUpdate

    val storeCallback = scope.modState { s =>
      val movements = BoardSetStore.getMovement()
      logger.info("Got all boardsets, n="+movements.size )
      s.copy( movements=movements)
    }

    val scrollToCB = movementTableRef.get.map { re =>
      re.getDOMNode.toElement.map { el =>
        el.scrollIntoView(false)
      }
    }.asCallback.void

    val didMount = CallbackTo {
      logger.info("PageMovements.didMount")
      BoardSetStore.addChangeListener(storeCallback)
    } >> Callback {
      BoardSetController.getMovement()
    } >> scrollToCB

    val willUnmount = CallbackTo {
      logger.info("PageMovements.willUnmount")
      BoardSetStore.removeChangeListener(storeCallback)
    }
  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = Callback {
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (props.initialDisplay != prevProps.initialDisplay) {
      cdu.backend.scrollToCB.runNow()
      cdu.backend.forceUpdate
    }
  }

  val component = ScalaComponent.builder[Props]("PageMovements")
                            .initialStateFromProps { props => {
                              logger.info("PageMovements.component.initialState: initial display "+props.initialDisplay)
                              State()
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentDidUpdate( didUpdate )
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

