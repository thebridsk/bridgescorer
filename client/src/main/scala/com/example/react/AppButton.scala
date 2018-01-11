package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.example.pages.BaseStyles

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * AppButton( AppButton.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object AppButton {
  import AppButtonInternal._

  case class Props( id: String, text: String, style: Option[TagMod], attrs: TagMod* )

  def apply( id: String, text: String, attrs: TagMod* ) = component(Props(id,text,None,attrs:_*))

  def withKey( key: String )( id: String, text: String, attrs: TagMod* ) = component.withKey(key)(Props(id,text,None,attrs:_*))
}

object AppButtonLink {
  import AppButtonInternal._
  import AppButton._

  val location = document.defaultView.location

  def apply( id: String, text: String, target: String, attrs: TagMod* ) = {
    val at = ^.onClick --> Callback {
      location.href = target
    }::attrs.toList
    component(Props(id,text,None,at:_*))
  }

  def withKey( key: String )( id: String, text: String, target: String, attrs: TagMod* ) = {
    val at = ^.onClick --> Callback {
      location.href = target
    }::attrs.toList
    val x = component.withKey(key) // (Props(id,text,None,at:_*))
    component.withKey(key)(Props(id,text,None,at:_*))
  }
}

object Button {
  import AppButtonInternal._
  import AppButton._

  def apply( style: TagMod, id: String, text: String, attrs: TagMod* ) = component(Props(id,text,Some(style),attrs:_*))

  def withKey( key: String )( style: TagMod, id: String, text: String, attrs: TagMod* ) = component.withKey(key)(Props(id,text,Some(style),attrs:_*))
}

object AppButtonInternal {
  import AppButton._
  import BaseStyles._

  val component = ScalaComponent.builder[Props]("AppButton")
                            .stateless
                            .noBackend
                            .render_P { props =>
                              val s = props.style.getOrElse(baseStyles.appButton)
                              <.button( ^.`type` := "button",
                                        props.attrs.toTagMod,
                                        s,
                                        ^.id:=props.id,
                                        props.text

                                      )
                            }
                            .build
}

