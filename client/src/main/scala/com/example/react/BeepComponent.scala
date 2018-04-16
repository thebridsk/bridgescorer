package com.example.skeleton.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.Bridge
import org.scalajs.dom.raw.HTMLAudioElement
import japgolly.scalajs.react.vdom.TagMod
import com.example.react.Utils._
import com.example.react.AppButton
import utils.logging.Logger
import com.example.pages.BaseStyles.baseStyles
import com.example.pages.BaseStyles

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

  def isPlayEnabled = playEnabled

  def beep() = {
    if (playEnabled) {
      try {
        val audio = Bridge.getElement("audioPlayer").asInstanceOf[HTMLAudioElement]
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

