package com.github.thebridsk.bridge.clientcommon.react

import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import org.scalajs.dom.raw.Window
import scala.scalajs.js.Dynamic
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.pages.GotoPage

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

  case class Props(
      id: String,
      text: TagMod,
      style: Option[TagMod],
      attrs: TagMod*
  )

  def apply(id: String, text: TagMod, attrs: TagMod*) =
    component(
      Props(id, text, None, attrs: _*)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def withKeyTagMod(key: String)(id: String, text: TagMod, attrs: TagMod*) =
    component.withKey(key)(
      Props(id, text, None, attrs: _*)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def apply(id: String, text: String, attrs: TagMod*) =
    component(
      Props(id, text, None, attrs: _*)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def withKey(key: String)(id: String, text: String, attrs: TagMod*) =
    component.withKey(key)(
      Props(id, text, None, attrs: _*)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent
}

object AppButtonLink {
  import AppButtonInternal._
  import AppButton._

  def apply(id: String, text: String, target: String, attrs: TagMod*) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      GotoPage.inSameWindow(target)
    } :: attrs.toList
    component(Props(id, text, None, at: _*))
  }

  def withKey(key: String)(
      id: String,
      text: String,
      target: String,
      attrs: TagMod*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      GotoPage.inSameWindow(target)
    } :: attrs.toList
    val x = component.withKey(key) // (Props(id,text,None,at:_*))
    component.withKey(key)(Props(id, text, None, at: _*))
  }
}

object AppButtonLinkNewWindow {
  import AppButtonInternal._
  import AppButton._

  val window = document.defaultView

  private var helpWindow: Option[Window] = None

  def topage(page: String): Unit = {
    helpWindow match {
      case Some(w) =>
        val closed = w.asInstanceOf[Dynamic].closed
        val b = closed.asInstanceOf[Boolean]
        logger.fine(
          s"""helpWindow closed=${closed}, b=${b}, helpWindow=${helpWindow}"""
        )
        if (b) {
          helpWindow = Some(document.defaultView.open(page, "_blank"))
        } else {
          w.location.href = page
          w.focus()
        }
      case None =>
        helpWindow = Some(document.defaultView.open(page, "_blank"))
    }
    logger.fine(s"""helpWindow helpWindow=${helpWindow}""")
  }

  def apply(id: String, text: String, target: String, attrs: TagMod*) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      window.open(target, "_blank")
    } :: attrs.toList
    component(Props(id, text, None, at: _*))
  }

  def apply(
      id: String,
      text: String,
      target: String,
      style: TagMod,
      attrs: TagMod*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      window.open(target, "_blank")
    } :: attrs.toList
    component(Props(id, text, Some(style), at: _*))
  }

  def apply(
      id: String,
      text: String,
      target: String,
      style: Option[TagMod],
      attrs: TagMod*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      topage(target)
    } :: attrs.toList
    component(Props(id, text, style, at: _*))
  }

  def apply(
      id: String,
      text: String,
      target: String,
      samepage: Boolean,
      attrs: TagMod*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      topage(target)
    } :: attrs.toList
    component(Props(id, text, None, at: _*))
  }

  def apply(
      id: String,
      text: String,
      target: String,
      samepage: Boolean,
      style: TagMod,
      attrs: TagMod*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      topage(target)
    } :: attrs.toList
    component(Props(id, text, Some(style), at: _*))
  }

  def apply(
      id: String,
      text: String,
      target: String,
      samepage: Boolean,
      style: Option[TagMod],
      attrs: TagMod*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      topage(target)
    } :: attrs.toList
    component(Props(id, text, style, at: _*))
  }

  def withKey(key: String)(
      id: String,
      text: String,
      target: String,
      attrs: TagMod*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val at = ^.onClick --> Callback {
      window.open(target, "_blank")
    } :: attrs.toList
    val x = component.withKey(key) // (Props(id,text,None,at:_*))
    component.withKey(key)(Props(id, text, None, at: _*))
  }
}

object Button {
  import AppButtonInternal._
  import AppButton._

  def apply(style: TagMod, id: String, text: TagMod, attrs: TagMod*) =
    component(
      Props(id, text, Some(style), attrs: _*)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def withKey(
      key: String
  )(style: TagMod, id: String, text: TagMod, attrs: TagMod*) =
    component.withKey(key)(
      Props(id, text, Some(style), attrs: _*)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent
}

object AppButtonInternal {
  import AppButton._
  import BaseStyles._

  val logger: Logger = Logger("bridge.AppButton")

  private[react] val component = ScalaComponent
    .builder[Props]("AppButton")
    .stateless
    .noBackend
    .render_P { props =>
      val s = props.style.getOrElse(baseStyles.appButton)
      <.button(
        ^.`type` := "button",
        props.attrs.toTagMod,
        s,
        ^.id := props.id,
        props.text
      )
    }
    .build
}
