package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object PopupOkCancelImplicits {
  import scala.language.implicitConversions
  implicit def optionStringToTagMod( os: Option[String] ): Option[TagMod] = os.map(s=>s)
}

object PopupOkCancel {
  import PopupOkCancelInternal._

  case class Props( content: Option[TagMod], ok: Option[Callback], cancel: Option[Callback], id: Option[String] )

  def apply( content: Option[TagMod], ok: Option[Callback], cancel: Option[Callback]=None, id: Option[String] = None ) =
    component( Props( content,ok,cancel,id ) )
}


object PopupOkCancelInternal {
  import PopupOkCancel._
  /**
   * Internal state for rendering the Popup.
   *
   * I'd like this class to be private, but the instantiation of Popup
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the Popup.
   *
   * I'd like this class to be private, but the instantiation of Popup
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
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
              props.ok.map{ ok => AppButton("PopUpOk", "OK", ^.width := "6em", ^.onClick --> ok ) }.whenDefined,
              props.cancel.map( cancel => AppButton("PopUpCancel", "Cancel", ^.width := "6em" , ^.onClick --> cancel ) ).whenDefined
            )
          )
        ),
        props.id
      )
    }
  }

  val component = ScalaComponent.builder[Props]("PopupComponent")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}
