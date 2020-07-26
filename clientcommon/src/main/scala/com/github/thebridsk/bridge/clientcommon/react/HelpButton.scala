package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod


object HelpButton {
  import HelpButtonInternal._

  case class Props( href: String, id: String, style: Option[TagMod] )

  def apply( href: String, id: String = "Help", style: Option[TagMod] = None ) = component(Props(href,id,style))  // scalafix:ok ExplicitResultTypes; ReactComponent
}

object HelpButtonInternal {
  import HelpButton._

  private[react]
  val component = ScalaComponent.builder[Props]("HelpButton")
                            .stateless
                            .noBackend
                            .render_P { props =>
                              AppButtonLinkNewWindow( props.id, "Help", props.href, true, props.style )
                            }
                            .build
}

