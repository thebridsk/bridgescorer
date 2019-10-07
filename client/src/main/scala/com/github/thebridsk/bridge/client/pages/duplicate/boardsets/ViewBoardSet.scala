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
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateStyles
import japgolly.scalajs.react.CtorType

/**
 * Shows all the boards of a boardset.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewBoardSet( ... )
 * </code></pre>
 *
 * To obtain a reference to the ViewBoardSet:
 *
 * <pre><code>
 * val ref = ViewBoardSet.getRef()
 *
 * def render() = {
 *   ViewBoardSet.withRef(ref)( ... )
 * }
 *
 * </code></pre>
 *
 * @author werewolf
 */
object ViewBoardSet {
  import ViewBoardSetInternal._

  type RefType = Ref.WithScalaComponent[Props,State,Backend,CtorType.Props]

  def getRef(): RefType = Ref.toScalaComponent(component)

  case class Props( boardset: BoardSet, columns: Int )

  def apply( boardset: BoardSet, columns: Int = 1 ) = component(Props(boardset,columns))

  def withRef(
      ref: RefType
  )(
      boardset: BoardSet,
      columns: Int = 1
  ) = component.withRef(ref)(Props(boardset,columns))

}

object ViewBoardSetInternal {
  import ViewBoardSet._

  val logger = Logger("bridge.ViewBoardSet")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   * @param boardSets all the boardsets
   * @param display the one to display
   */
  case class State()

  val BoardHeader = ScalaComponent.builder[Props]("ViewBoardSet.BoardHeader")
                    .render_P( props => {
                      <.tr(
                        <.th( "Board" ),
                        <.th( "Dealer" ),
                        <.th( "Vulnerability" )
                      )
                    }).build

  def showVul( b: BoardInSet ) = {
    if (b.nsVul) {
      if (b.ewVul) {
        "Both Vul"
      } else {
        "NS Vul"
      }
    } else {
      if (b.ewVul) {
        "EW Vul"
      } else {
        "Neither Vul"
      }
    }
  }

  val BoardRow = ScalaComponent.builder[(Props,BoardInSet)]("ViewBoardSet.BoardRow")
                    .render_P( args => {
                      val (props,board) = args
                      <.tr(
                        <.td( board.id),
                        <.td( board.dealer),
                        <.td( showVul(board))
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

    def render( props: Props, state: State ) = {
      import DuplicateStyles._
      val columns = props.columns
      val entriespercolumn = (props.boardset.boards.length+columns-1)/columns
      val listofboardsets = props.boardset.boards.sortWith( (t1,t2)=>t1.id<t2.id ).grouped(entriespercolumn)
      <.div(
        dupStyles.divBoardSetView,
        listofboardsets.map { boardsets =>
          <.div(
            <.table(
              <.thead(
                BoardHeader(props)
              ),
              <.tbody(
                boardsets.map { b =>
                  BoardRow.withKey( b.id )((props,b))
                }.toTagMod
              )
            )
          )
        }.toTagMod
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ViewBoardSet")
                            .initialStateFromProps { props => {
                              State()
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

