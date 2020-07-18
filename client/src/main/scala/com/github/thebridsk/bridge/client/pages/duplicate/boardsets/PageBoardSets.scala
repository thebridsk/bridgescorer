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
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicatePage
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateStyles
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicatePageBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetEditView
import _root_.com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetNewView
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientBoardSet
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageBoardSets( PageBoardSets.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageBoardSets {
  import PageBoardSetsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[BoardSet.Id] )

  def apply( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[BoardSet.Id] ) = component(Props(routerCtl,backpage,initialDisplay))

}

object PageBoardSetsInternal {
  import PageBoardSets._
  import com.github.thebridsk.bridge.clientcommon.react.Utils._
  import DuplicateStyles._

  val logger = Logger("bridge.PageBoardSets")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   * @param boardSets all the boardsets
   */
  case class State( boardSets: Map[BoardSet.Id,BoardSet] = Map(), msg: Option[TagMod] = None ) {

    def setMsg( msg: String ) = copy( msg = Some(msg))
    def setMsg( msg: TagMod ) = copy( msg = Some(msg))
    def clearMsg() = copy( msg = None )

  }

  val SummaryHeader = ScalaComponent.builder[State]("PageBoardSets.SummaryHeader")
                    .render_P( state => {
                      <.tr(
                        <.th( "Name" ),
                        <.th( "Description" ),
                        <.th( "Action" )
                      )
                    }).build

  val SummaryRow = ScalaComponent.builder[(Backend,Props,State,BoardSet.Id,Callback,Option[BoardSet.Id])]("PageBoardSets.SummaryRow")
                    .render_P( args => {
                      val (backend,props,state,current,toggle,selected) = args
                      val bs = state.boardSets(current)
                      val sel = selected.map( s => s==current ).getOrElse(false)
                      <.tr(
                        <.td(
                          AppButton( bs.name.id, bs.short, BaseStyles.highlight(selected = sel), ^.onClick-->toggle )
                        ),
                        <.td( bs.description),
                        <.td(
                          AppButton( s"${bs.name}_edit", "Edit", props.routerCtl.setOnClick(BoardSetEditView(bs.name.id)) ),
                          bs.isDeletable ?= AppButton( s"${bs.name}_delete", "Delete", ^.onClick --> backend.deleteCB(bs.id) ),
                          bs.isResetToDefault ?= AppButton( s"${bs.name}_reset", "Reset", ^.onClick --> backend.resetCB(bs.id) ),
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
  class Backend(scope: BackendScope[Props, State]) {

    private val boardSetViewRef = ViewBoardSet.getRef()

    val popupCancel = Callback {

    } >> scope.modState( s => s.clearMsg())

    val okCallback = scope.forceUpdate >> scope.props >>= { props => props.routerCtl.set(props.backpage) }

    def toggleBoardSet( name: BoardSet.Id ) = scope.props >>= { props =>
      props.initialDisplay match {
        case Some(s) if s == name => props.routerCtl.set(BoardSetSummaryView)
        case _ => props.routerCtl.set(BoardSetView(name.id))
      }
    }

    def deleteCB( id: BoardSet.Id ) = scope.modState(
      { s =>
        s.setMsg(s"Deleting boardset $id")
      },
      Callback {
        BoardSetController.deleteBoardSet(id).recordFailure().onComplete { tr =>
          logger.fine(s"boardset $id deleted: $tr")
          tr match {
            case Success(x) =>
              scope.withEffectsImpure.modState( _.setMsg(s"Deleted boardset $id"))
            case Failure(ex) =>
              scope.withEffectsImpure.modState( _.setMsg(s"Error deleting boardset $id"))
          }
        }
      }
    )

    def resetCB( id: BoardSet.Id ) = scope.modState(
      { s =>
        s.setMsg(s"Resetting boardset $id to default")
      },
      Callback {
        BoardSetController.deleteBoardSet(id).recordFailure().onComplete { tr =>
          logger.fine(s"boardset $id reset: $tr")
          tr match {
            case Success(x) =>
              BoardSetController.getBoardSets()
              scope.withEffectsImpure.modState( _.setMsg(s"Boardset $id was reset to default"))
            case Failure(ex) =>
              scope.withEffectsImpure.modState( _.setMsg(s"Error resetting boardset $id to default"))
          }
        }
      }
    )

    def render( props: Props, state: State ) = {
      logger.info("PageBoardSets.Backend.render: display "+props.initialDisplay)
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
                      "BoardSets",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          dupStyles.divBoardSetsPage,
          <.div(
            <.table(
              <.thead(
                SummaryHeader(state)
              ),
              <.tbody(
                state.boardSets.keySet.toList.sortWith( (t1,t2)=>t1<t2 ).map { name =>
                  SummaryRow.withKey( name.id )((this,props,state,name,toggleBoardSet(name),props.initialDisplay))
                }.toTagMod
              )
            ),
            AppButton( "OK", "OK", ^.onClick-->okCallback ),
            AppButton( "New", "New", props.routerCtl.setOnClick(BoardSetNewView) )
            ),
          <.div(
            props.initialDisplay match {
              case Some(name) =>
                state.boardSets.get(name) match {
                  case Some(bs) =>
                    <.div(
                      <.h1("Showing ", bs.short ),
                      <.p(bs.description),
                      ViewBoardSet.withRef(boardSetViewRef)(bs,2)
                    )
                  case None =>
                    <.span(s"BoardSet $name not found")
                }
              case None =>
                <.span()
            }
          )
        )
      )
    }

    def forceUpdate = scope.withEffectsImpure.forceUpdate

    val storeCallback = scope.modState { s =>
      val boardsets = BoardSetStore.getBoardSets()
      logger.info("Got all boardsets, n="+boardsets.size )
      s.copy( boardSets=boardsets)
    }

    val scrollToCB = boardSetViewRef.get.map { re =>
      re.getDOMNode.toElement.map { el =>
        el.scrollIntoView(false)
      }
    }.asCallback.void

    val didMount = Callback {
      logger.info("PageBoardSets.didMount")
      BoardSetStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      BoardSetController.getBoardSets()
    )}

    val willUnmount = Callback {
      logger.info("PageBoardSets.willUnmount")
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

  val component = ScalaComponent.builder[Props]("PageBoardSets")
                            .initialStateFromProps { props => {
                              logger.info("PageBoardSets.component.initialState: initial display "+props.initialDisplay)
                              State()
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentDidUpdate( didUpdate _ )
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

