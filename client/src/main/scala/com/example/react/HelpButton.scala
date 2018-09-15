package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.example.pages.BaseStyles

object HelpButton {
  import HelpButtonInternal._

  case class Props( href: String, id: String, style: Option[TagMod] )

  def apply( href: String, id: String = "Help", style: Option[TagMod] = None ) = component(Props(href,id,style))
}

object HelpButtonInternal {
  import HelpButton._
  import BaseStyles._

  val component = ScalaComponent.builder[Props]("HelpButton")
                            .stateless
                            .noBackend
                            .render_P { props =>
                              AppButtonLinkNewWindow( props.id, "Help", props.href, true, props.style )
                            }
                            .build
}

