package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object PopupOkCancelImplicits {
  import scala.language.implicitConversions
  implicit def optionStringToTagMod(os: Option[String]): Option[TagMod] =
    os.map(s => s)
}

/**
  * A Popup react component, with optional OK and Cancel buttons.
  *
  * Displays a popup.  The entire page is dimmed, while the content of the popup is centered on the display.
  *
  * Usage:
  *
  * {{{
  * val ok: Callback = ...
  * val cancel: Callback = ...
  *
  * PopupOkCancel(
  *   content = Some(<.div("Hello World!")),
  *   ok = Some(ok),
  *   cancel = Some(cancel)
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the parameters.
  *
  * @author werewolf
  */
object PopupOkCancel {
  import Internal._

  case class Props(
      content: Option[TagMod],
      ok: Option[Callback],
      cancel: Option[Callback],
      id: Option[String]
  )

  /**
    * Instantiate the react component.
    *
    * @param content - the optional content.  If specified as None, then the popup is not displayed.
    * @param ok - the optional callback for the ok button.  If specified as None, then the ok button is not displayed.
    * @param cancel - the optional callback for the cancel button.  If specified as None, then the cancel button is not displayed.
    * @param id - the optional id of the root element of the popup.  Default is none.
    *
    * @return the unmounted react component.
    */
  def apply(
      content: Option[TagMod],
      ok: Option[Callback],
      cancel: Option[Callback] = None,
      id: Option[String] = None
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(content, ok, cancel, id))

  protected object Internal {

    case class State()
    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; ReactComponent
        import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
        Popup(
          props.content.isDefined,
          <.div(
            baseStyles.divPopupOKCancelDiv,
            <.div(
              baseStyles.divPopupOKCancelBody,
              props.content.getOrElse(<.span)
            ),
            <.div(
              <.div(
                baseStyles.divFooterRight,
                props.ok.map { ok =>
                  AppButton("PopUpOk", "OK", ^.width := "6em", ^.onClick --> ok)
                }.whenDefined,
                props.cancel
                  .map(cancel =>
                    AppButton(
                      "PopUpCancel",
                      "Cancel",
                      ^.width := "6em",
                      ^.onClick --> cancel
                    )
                  )
                  .whenDefined
              )
            )
          ),
          props.id
        )
      }
    }

    private[react] val component = ScalaComponent
      .builder[Props]("PopupComponent")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
