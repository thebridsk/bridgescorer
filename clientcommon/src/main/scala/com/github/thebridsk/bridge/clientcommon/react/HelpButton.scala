package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod

/**
  * A help button react component.
  *
  * Creates a button which when clicked opens the specified help page.
  *
  * Usage:
  *
  * {{{
  * HelpButton(
  *   helpurl = "https://...",
  *   id = "..."
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the parameters.
  */
object HelpButton {
  import Internal._

  case class Props(helpurl: String, id: String, style: Option[TagMod])

  /**
    * Instantiate the react component.
    *
    * @param helpurl - The URL of the help page to open when clicked.
    * @param id - The id of the button.  Default is "Help"
    * @param style - Additional style to add to button.  Default is none.
    *
    * @return the unmounted react component.
    */
  def apply(helpurl: String, id: String = "Help", style: Option[TagMod] = None) =
    component(
      Props(helpurl, id, style)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val component = ScalaComponent
      .builder[Props]("HelpButton")
      .stateless
      .noBackend
      .render_P { props =>
        AppButtonLinkNewWindow(props.id, "Help", props.helpurl, true, props.style)
      }
      .build
  }

}

