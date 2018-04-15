package com.example.pages.duplicate.boardsets

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.bridge.store.BoardSetStore
import com.example.controller.BoardSetController
import com.example.data.BoardSet
import utils.logging.Logger
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.BridgeRouter
import com.example.data.BoardInSet
import com.example.data.Movement
import com.example.data.HandInTable
import com.example.pages.duplicate.DuplicatePage
import com.example.react.AppButton
import com.example.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.example.pages.duplicate.DuplicateRouter.MovementView
import com.example.pages.duplicate.DuplicateStyles
import com.example.pages.BaseStyles

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
  import com.example.react.Utils._
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

    def okCallback = scope.forceUpdate >> scope.props >>= { props => props.routerCtl.set(props.backpage) }

    def toggleBoardSet( name: String ) = scope.props >>= { props =>
      props.initialDisplay match {
        case Some(s) if s == name => props.routerCtl.set(MovementSummaryView)
        case _ => props.routerCtl.set(MovementView(name))
      }
    }

    def render( props: Props, state: State ) = {
      logger.info("PageMovements.Backend.render: display "+props.initialDisplay)
      <.div(
        dupStyles.divMovementsPage,
        <.div(
          <.h1("Movements"),
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
          AppButton( "OK", "OK", ^.onClick-->okCallback )
        ),
        <.div(
          props.initialDisplay match {
            case Some(name) =>
              state.movements.get(name) match {
                case Some(htp) =>
                  <.div(
                    <.h1("Showing ", htp.short ),
                    <.p( htp.description ),
                    <.div(
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

    def didMount() = CallbackTo {
      logger.info("PageMovements.didMount")
      BoardSetStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => CallbackTo(
      BoardSetController.getMovement()
    )}

    def willUnmount() = CallbackTo {
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
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

