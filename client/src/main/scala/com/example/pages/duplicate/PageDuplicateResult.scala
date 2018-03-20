package com.example.pages.duplicate

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.routes.BridgeRouter
import com.example.pages.duplicate.DuplicateRouter.DuplicateResultView
import utils.logging.Logger
import com.example.bridge.store.DuplicateResultStore
import com.example.controller.Controller
import com.example.react.AppButton
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.DuplicateResultEditView
import com.example.react.DateUtils
import com.example.react.DateTimePicker
import scala.scalajs.js.Date
import com.example.react.reactwidgets.globalize.Moment
import com.example.react.reactwidgets.globalize.ReactWidgetsMoment
import com.example.rest2.RestClientDuplicateResult
import com.example.react.PopupOkCancel

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageDuplicateResult( PageDuplicateResult.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageDuplicateResult {
  import PageDuplicateResultInternal._

  type Callback = ()=>Unit

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: DuplicateResultView )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: DuplicateResultView ) = component(Props(routerCtl,page))

}

object PageDuplicateResultInternal {
  import PageDuplicateResult._

  val logger = Logger("bridge.PageDuplicateResult")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( currentDate: Date = new Date(), deletePopup: Boolean = false )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    // ignore changes to DateTimePicker
    def setPlayedIgnore( value: Date ) = Callback {}

    def delete = scope.modState( s => s.copy(deletePopup=true) )

    def actionDeleteCancel = scope.modState( s => s.copy(deletePopup=false) )

    def actionDeleteOk = scope.props >>= { props => Callback {
      import scala.concurrent.ExecutionContext.Implicits.global
      RestClientDuplicateResult.delete( props.page.dupid ).recordFailure().foreach { f =>
        logger.info(s"Deleted Match duplicate result ${props.page.dupid}")
      }
    } >> props.routerCtl.set( SummaryView ) }

    def render( props: Props, state: State ) = {
      import DuplicateStyles._

      Moment.locale("en")
      ReactWidgetsMoment()

      DuplicateResultStore.getDuplicateResult() match {
        case Some(dre) if dre.id == props.page.dupid =>
          val bfinished = !dre.notfinished.getOrElse(false)
          val finished = if (bfinished) "Match complete" else "Match not complete"
          val comment = dre.comment.getOrElse("")
          val wss = dre.getWinnerSets
          <.div(
            PopupOkCancel(
              if (state.deletePopup) {
                Some( <.span( s"Are you sure you want to delete duplicate result ${props.page.dupid}" ) )
              } else {
                None
              },
              Some(actionDeleteOk),
              Some(actionDeleteCancel)
            ),
            <.div( dupStyles.divDuplicateResultPage,
//              <.div(
//                <.p( "Played: " ),
//                DateTimePicker("played",
//                               defaultValue=if (dre.played==0) None else Some(new Date(dre.played)),
//                               defaultCurrentDate = Some( state.currentDate ),
//  //                             onChange = Some(setPlayedIgnore),
//  //                             onCurrentDateChange = Some(setPlayedIgnore),
//                               readOnly = true
//                              ),
//                <.p( finished ),
//                <.p( comment )
//              ),
              wss.zipWithIndex.map { arg =>
                val (ws,i) = arg
                val pbws = if (dre.isIMP) dre.placeByWinnerSetIMP(ws) else dre.placeByWinnerSet(ws)
                ViewPlayerMatchResult( pbws, dre, i+1, wss.length, dre.isIMP )
              }.toTagMod,
              <.p( "Created: ", DateUtils.formatDate(dre.created), ", updated ", DateUtils.formatDate(dre.updated) )
            ),
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton( "Summary", "Summary",
                           props.routerCtl.setOnClick( SummaryView )
                )
              ),
              <.div(
                baseStyles.divFooterCenter,
                AppButton( "Edit", "Edit",
                           props.routerCtl.setOnClick( DuplicateResultEditView(props.page.dupid) )
                )
              ),
              <.div(
                baseStyles.divFooterRight,
                AppButton( "Delete", "Delete",
                           ^.onClick --> delete
                )
              )
            )
          )
        case _ =>
          <.div( s"Working" )
      }
    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    def didMount() = Callback {
      logger.info("PageDuplicateResult.didMount")
      DuplicateResultStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      Controller.monitorDuplicateResult(p.page.dupid))
    }

    def willUnmount() = Callback {
      logger.info("PageDuplicateResult.willUnmount")
      DuplicateResultStore.removeChangeListener(storeCallback)
      Controller.stopMonitoringDuplicateResult()
    }

  }

  val component = ScalaComponent.builder[Props]("PageDuplicateResult")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

