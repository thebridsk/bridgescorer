package com.example.pages.duplicate

import scala.scalajs.js
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
import com.example.routes.BridgeRouter
import com.example.react.Utils.ExtendReactEventFromInput
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor

/**
 * PageSelectMatch.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageSelectMatch( PageSelectMatch.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageSelectMatch {
  import PageSelectMatchInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage ) = component(Props(routerCtl,page))

}

object PageSelectMatchInternal {
  import PageSelectMatch._
  import com.example.react.PopupOkCancelImplicits._

  val logger = Logger("bridge.PageSelectMatch")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( selection: Option[String] = None, error: Option[String] = None ) {

    def clear = copy( selection = None )

    def setSelection( s: String ) = copy( selection=if (s=="") None else Some(s) )

    def isValid = selection.filter( s => s!="" ).map( s => true ).getOrElse(false)
  }

  val patternValidInput = """ ?(\d+) ?""".r

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    private var mounted = false

    val ok = scope.modStateOption { ( state, props) =>
      state.selection match {
        case Some(s) =>
          val id = s"M${s}"
          props.routerCtl.set( CompleteScoreboardView(id) )
        case None =>
          logger.severe("No selection for PageSelectMatch")
          Callback {}
      }
      None
    }

    def inputCB( data: ReactEventFromInput): Callback = data.inputText { text =>
      scope.modState { s =>
        text match {
          case patternValidInput(i) =>
            s.setSelection(i)
          case _ =>
            s.copy(error = Some(s"""Expecting only numbers in the input, string "${text}" is not valid"""))
        }
      }
    }

    val popupOk = scope.modState { s => s.copy(error = None) }

    def render( props: Props, state: State ) = {
      import DuplicateStyles._

      <.div(
        dupStyles.divSelectMatch,
        PopupOkCancel( state.error.map( s => <.p(s) ), Some(popupOk), None ),
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
            MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit,
            )(
                <.span(
                  "Select a Match"
                )
            )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          <.label(
            "Please enter just the number for the match: ",
            <.input.text(
                        ^.name:="DuplicateId",
                        ^.onChange ==> inputCB _,
                        ^.value := state.selection.getOrElse(""))
            ),
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton( "OK", "OK", ^.onClick-->ok )
              ),
              <.div(
                baseStyles.divFooterCenter,
                AppButton( "Cancel", "Cancel", props.routerCtl.home ),
              ),
              <.div(
                baseStyles.divFooterRight
              )
            )
        ),
      )
    }

    val didMount = Callback {
      mounted = true
      logger.info("PageSelectMatch.didMount")
    }

    val willUnmount = Callback {
      mounted = false
      logger.info("PageSelectMatch.willUnmount")
    }
  }

  val component = ScalaComponent.builder[Props]("PageSelectMatch")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

