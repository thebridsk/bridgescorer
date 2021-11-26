package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.react.Utils.ExtendReactEventFromInput
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

import scala.util.matching.Regex

/**
  * Component page to select a duplicate match.
  *
  * This component shows an input field where the id number is entered.
  *
  * To use, just code the following:
  *
  * {{{
  * PageSelectMatch(
  *   routerCtl = ...,
  *   page = SelectMatchView
  * )
  * }}}
  *
  * @author werewolf
  */
object PageSelectMatch {
  import Internal._

  case class Props(routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage)

  /**
    * Instantiate the component
    *
    * @param routerCtl
    * @param page
    * @return the unmounted react component
    */
  def apply(routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage) =
    component(
      Props(routerCtl, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.PageSelectMatch")

    case class State(
        selection: Option[String] = None,
        error: Option[String] = None
    ) {

      def clear: State = copy(selection = None)

      def setSelection(s: String): State =
        copy(selection = if (s == "") None else Some(s))

      def isValid: Boolean =
        selection.filter(s => s != "").map(s => true).getOrElse(false)
    }

    val patternValidInput: Regex = """ ?(\d+) ?""".r

    class Backend(scope: BackendScope[Props, State]) {

      private var mounted = false

      val ok: Callback = scope.modStateOption { (state, props) =>
        state.selection match {
          case Some(s) =>
            val id = s"M${s}"
            props.routerCtl.set(CompleteScoreboardView(id))
          case None =>
            logger.severe("No selection for PageSelectMatch")
            Callback {}
        }
        None
      }

      def inputCB(data: ReactEventFromInput): Callback =
        data.inputText { text =>
          scope.modState { s =>
            text match {
              case patternValidInput(i) =>
                s.setSelection(i)
              case _ =>
                s.copy(error =
                  Some(
                    s"""Expecting only numbers in the input, string "${text}" is not valid"""
                  )
                )
            }
          }
        }

      val popupOk: Callback = scope.modState { s => s.copy(error = None) }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        import DuplicateStyles._

        <.div(
          PopupOkCancel(state.error.map(s => <.p(s)), Some(popupOk), None),
          DuplicatePageBridgeAppBar(
            id = None,
            tableIds = List(),
            title = Seq[CtorType.ChildArg](
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  "Select a Match"
                )
              )
            ),
            helpurl = "../help/duplicate/summary.html",
            routeCtl = props.routerCtl
          )(
          ),
          <.div(
            dupStyles.divSelectMatch,
            <.label(
              "Please enter just the number for the match: ",
              <.input.text(
                ^.name := "DuplicateId",
                ^.onChange ==> inputCB _,
                ^.value := state.selection.getOrElse("")
              )
            ),
            <.div(baseStyles.divFlexBreak),
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton("OK", "OK", ^.onClick --> ok)
              ),
              <.div(
                baseStyles.divFooterCenter,
                AppButton("Cancel", "Cancel", props.routerCtl.home)
              ),
              <.div(
                baseStyles.divFooterRight
              )
            )
          )
        )
      }

      val didMount: Callback = Callback {
        mounted = true
        logger.info("PageSelectMatch.didMount")
      }

      val willUnmount: Callback = Callback {
        mounted = false
        logger.info("PageSelectMatch.willUnmount")
      }
    }

    val component = ScalaComponent
      .builder[Props]("PageSelectMatch")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
