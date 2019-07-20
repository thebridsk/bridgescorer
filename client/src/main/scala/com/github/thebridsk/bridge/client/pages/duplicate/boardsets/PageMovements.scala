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

  case class Props( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[String] )

  def apply( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[String] ) = component(Props(routerCtl,backpage,initialDisplay))

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
  case class State( movements: Map[String,Movement] )

  val SummaryHeader = ScalaComponent.builder[State]("PageMovements.SummaryHeader")
                    .render_P( state => {
                      <.tr(
                        <.th( "Name" ),
                        <.th( "Description" )
                      )
                    }).build

  val SummaryRow = ScalaComponent.builder[(State,String,Callback,Option[String])]("PageMovements.SummaryRow")
                    .render_P( props => {
                      val (state,current,toggle,selected) = props
                      val bs = state.movements(current)
                      val sel = selected.map( s => s==current ).getOrElse(false)
                      <.tr(
                        <.td(
                          AppButton( bs.name, bs.short, BaseStyles.highlight(selected = sel), ^.onClick-->toggle )
                        ),
                        <.td( bs.description)
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
                                  BoardRow.withKey( h.table+"-"+h.round )((state,htp,h,relay))
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
  class Backend(scope: BackendScope[Props, State]) {

    val okCallback = scope.forceUpdate >> scope.props >>= { props => props.routerCtl.set(props.backpage) }

    def toggleBoardSet( name: String ) = scope.props >>= { props =>
      props.initialDisplay match {
        case Some(s) if s == name => props.routerCtl.set(MovementSummaryView)
        case _ => props.routerCtl.set(MovementView(name))
      }
    }

    def render( props: Props, state: State ) = {
      logger.info("PageMovements.Backend.render: display "+props.initialDisplay)
      <.div(
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
                SummaryRow.withKey( name )((state,name,toggleBoardSet(name),props.initialDisplay))
              }.toTagMod
            )
          ),
          AppButton( "OK", "OK", ^.onClick-->okCallback ),
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
                        MovementTable((state,htp,table))
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

    val storeCallback = scope.modState { s =>
      val boardsets = BoardSetStore.getMovement()
      logger.info("Got all boardsets, n="+boardsets.size )
      s.copy( movements=boardsets)
    }

    val didMount = CallbackTo {
      logger.info("PageMovements.didMount")
      BoardSetStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => CallbackTo(
      BoardSetController.getMovement()
    )}

    val willUnmount = CallbackTo {
      logger.info("PageMovements.willUnmount")
      BoardSetStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageMovements")
                            .initialStateFromProps { props => {
                              logger.info("PageMovements.component.initialState: initial display "+props.initialDisplay)
                              State(Map())
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

