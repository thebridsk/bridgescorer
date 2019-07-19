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
import com.github.thebridsk.bridge.client.pages.BaseStyles
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
 * PageBoardSets( PageBoardSets.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageBoardSets {
  import PageBoardSetsInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[String] )

  def apply( routerCtl: BridgeRouter[DuplicatePage], backpage: DuplicatePage, initialDisplay: Option[String] ) = component(Props(routerCtl,backpage,initialDisplay))

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
  case class State( boardSets: Map[String,BoardSet] )

  val SummaryHeader = ScalaComponent.builder[State]("PageBoardSets.SummaryHeader")
                    .render_P( state => {
                      <.tr(
                        <.th( "Name" ),
                        <.th( "Description" )
                      )
                    }).build

  val SummaryRow = ScalaComponent.builder[(State,String,Callback,Option[String])]("PageBoardSets.SummaryRow")
                    .render_P( props => {
                      val (state,current,toggle,selected) = props
                      val bs = state.boardSets(current)
                      val sel = selected.map( s => s==current ).getOrElse(false)
                      <.tr(
                        <.td(
                          AppButton( bs.name, bs.short, BaseStyles.highlight(selected = sel), ^.onClick-->toggle )
                        ),
                        <.td( bs.description)
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
        case Some(s) if s == name => props.routerCtl.set(BoardSetSummaryView)
        case _ => props.routerCtl.set(BoardSetView(name))
      }
    }

    def render( props: Props, state: State ) = {
      logger.info("PageBoardSets.Backend.render: display "+props.initialDisplay)
      <.div(
        dupStyles.divBoardSetsPage,
//        <.div(
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
            <.div(
              <.table(
                <.thead(
                  SummaryHeader(state)
                ),
                <.tbody(
                  state.boardSets.keySet.toList.sortWith( (t1,t2)=>t1<t2 ).map { name =>
                    SummaryRow.withKey( name )((state,name,toggleBoardSet(name),props.initialDisplay))
                  }.toTagMod
                )
              ),
              AppButton( "OK", "OK", ^.onClick-->okCallback )
            ),
            <.div(
              props.initialDisplay match {
                case Some(name) =>
                  state.boardSets.get(name) match {
                    case Some(bs) =>
                      <.div(
                        <.h1("Showing ", bs.short ),
                        <.p(bs.description),
                        ViewBoardSet(bs,2)
                      )
                    case None =>
                      <.span(s"BoardSet $name not found")
                  }
                case None =>
                  <.span()
              }
            )
          )
        ),
//      )
    }

    val storeCallback = scope.modState { s =>
      val boardsets = BoardSetStore.getBoardSets()
      logger.info("Got all boardsets, n="+boardsets.size )
      s.copy( boardSets=boardsets)
    }

    val didMount = CallbackTo {
      logger.info("PageBoardSets.didMount")
      BoardSetStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => CallbackTo(
      BoardSetController.getBoardSets()
    )}

    val willUnmount = CallbackTo {
      logger.info("PageBoardSets.willUnmount")
      BoardSetStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageBoardSets")
                            .initialStateFromProps { props => {
                              logger.info("PageBoardSets.component.initialState: initial display "+props.initialDisplay)
                              State(Map())
                            }}
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

