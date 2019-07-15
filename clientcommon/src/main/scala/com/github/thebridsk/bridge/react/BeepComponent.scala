package com.github.thebridsk.bridge.skeleton.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import org.scalajs.dom.raw.HTMLAudioElement
import japgolly.scalajs.react.vdom.TagMod
import com.github.thebridsk.bridge.react.Utils._
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.pages.BaseStyles.baseStyles
import com.github.thebridsk.bridge.pages.BaseStyles
import com.github.thebridsk.materialui.icons.MuiIcons
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.materialui.icons.SvgColor
import com.github.thebridsk.bridge.logger.Info

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * BeepComponent( BeepComponent.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object BeepComponent {
  import BeepComponentInternal._

  case class Props( alwaysShow: ()=>Boolean )

  private var playEnabled: Boolean = false

  def apply( alwaysShow: ()=>Boolean ) = component(new Props(alwaysShow))

  private[react] def enableBeep() = {
    playEnabled = true
    log.info("Beep enabled")
    beep()
  }

  private[react] def disableBeep() = {
    playEnabled = false
    log.info("Beep disabled")
  }

  def toggleBeep() = {
    playEnabled = !playEnabled
    log.info(s"Beep playEnabled=$playEnabled")
    if (playEnabled) beep()
  }

  def isPlayEnabled = playEnabled

  def beep() = {
    if (playEnabled) {
      try {
        val audio = Info.getElement("audioPlayer").asInstanceOf[HTMLAudioElement]
        audio.play()
        log.info("beep")
      } catch {
        case x: IllegalStateException =>
          log.severe(s"Oops, unable to beep, ${x}")
      }
    } else {
      log.fine("Beep is not enabled")
    }
  }

  def toggle(cb: ()=>Unit)( e: ReactEvent ): Unit = {
    toggleBeep()
    cb()
  }

  def getMenuItem(cb: ()=>Unit): VdomNode = {
    MuiMenuItem(
        id = "Beep",
        onClick = toggle(cb) _,
        classes = js.Dictionary("root" -> "mainMenuItem")
    )(
        "Beep",
        {
//          val color = if (playEnabled) SvgColor.inherit else SvgColor.disabled
//          MuiIcons.Check(
//              color=color,
//              classes = js.Dictionary("root" -> "mainMenuItemIcon")
//          )
          if (playEnabled) {
            MuiIcons.CheckBox()
          } else {
            MuiIcons.CheckBoxOutlineBlank()
          }
        }
    )
  }
}

object BeepComponentInternal {
  import BeepComponent._

  val log = Logger("bridge.BeepComponent")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( displayButtons: Boolean = true )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val enablePlay = scope.modState(s => {
      enableBeep()
      s.copy(displayButtons = false)
    })

    val hideButtons = scope.modState(s => {
      disableBeep()
      s.copy(displayButtons = false)
    })

    def render( props: Props, state: State ) = {
      <.div(
        (props.alwaysShow()||state.displayButtons) ?= <.span(
          AppButton("enableBeep","Enable Beeps", ^.onClick --> enablePlay, BaseStyles.highlight(selected = isPlayEnabled ) ),
          AppButton("disableBeep","Disable Beeps", ^.onClick --> hideButtons)
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("BeepComponent")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build

}

