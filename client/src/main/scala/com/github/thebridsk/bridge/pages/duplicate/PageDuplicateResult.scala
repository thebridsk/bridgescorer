package com.github.thebridsk.bridge.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.routes.BridgeRouter
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.DuplicateResultView
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.bridge.store.DuplicateResultStore
import com.github.thebridsk.bridge.controller.Controller
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.pages.duplicate.DuplicateRouter.DuplicateResultEditView
import com.github.thebridsk.bridge.react.DateUtils
import com.github.thebridsk.bridge.react.DateTimePicker
import scala.scalajs.js.Date
import com.github.thebridsk.bridge.react.reactwidgets.globalize.Moment
import com.github.thebridsk.bridge.react.reactwidgets.globalize.ReactWidgetsMoment
import com.github.thebridsk.bridge.rest2.RestClientDuplicateResult
import com.github.thebridsk.bridge.react.PopupOkCancel
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

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

    val delete = scope.modState( s => s.copy(deletePopup=true) )

    val actionDeleteCancel = scope.modState( s => s.copy(deletePopup=false) )

    val actionDeleteOk = scope.props >>= { props => Callback {
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
            DuplicatePageBridgeAppBar(
              id = None,
              tableIds = List(),
              title = Seq[CtorType.ChildArg](
                    MuiTypography(
                        variant = TextVariant.h6,
                        color = TextColor.inherit,
                    )(
                        <.span(
                          "Duplicate Results",
                        )
                    )),
              helpurl = "../help/duplicate/summary.html",
              routeCtl = props.routerCtl
            )(

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
              <.p( "Created: ", DateUtils.formatDate(dre.created), ", updated ", DateUtils.formatDate(dre.updated) ),
              <.div( baseStyles.divFlexBreak ),
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
            ),
          )
        case _ =>
          <.div( s"Working" )
      }
    }

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { (p) => Callback {
      logger.info("PageDuplicateResult.didMount")
      DuplicateResultStore.addChangeListener(storeCallback)

      Controller.monitorDuplicateResult(p.page.dupid)
    }}

    val willUnmount = Callback {
      logger.info("PageDuplicateResult.willUnmount")
      DuplicateResultStore.removeChangeListener(storeCallback)
      Controller.stopMonitoringDuplicateResult()
    }

  }

  val component = ScalaComponent.builder[Props]("PageDuplicateResult")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

