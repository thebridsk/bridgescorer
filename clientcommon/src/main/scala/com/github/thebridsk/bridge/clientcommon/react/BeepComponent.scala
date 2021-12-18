package com.github.thebridsk.bridge.clientcommon.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import org.scalajs.dom.HTMLAudioElement
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui.icons
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.bridge.clientcommon.logger.Info

/**
  * A component that manages beeps by the application.
  * This component can provide a MuiMenuItem that toggles the beep.
  * This component has 2 buttons, to enable and disable the beep.
  * Either or both these can be used.
  *
  * When running on an iPad, the first beep must be enabled by a user event.
  * Any beeps that are requested would not sound until one was initiated by
  * a user event.
  *
  * To use the 2 buttons:
  *
  * {{{
  * def showButtons(): Boolean = { ... }
  *
  * BeepComponent(showButtons _)
  * }}}
  *
  * The buttons will be in a div element.
  * See the [[apply]] method for a description of the arguments.
  *
  * The component also has a menu item that allows the beep to be turned on and off.
  *
  * {{{
  * def beepChanged(): Unit = { scope.withEffectsImpure.forceUpdate }
  *
  * BeepComponent.getMenuItem(beepChanged _)
  * }}}
  *
  * See the [[getMenuItem]] method for a description of the arguments.
  *
  * To play a beep:
  *
  * {{{
  * BeepComponent.beep()
  * }}}
  *
  * @author werewolf
  */
object BeepComponent {
  import Internal._

  case class Props(alwaysShow: () => Boolean)

  private var playEnabled: Boolean = false

  /**
    * Create a component with 2 buttons, one to enable and the other to disable the beep.
    *
    * @param alwaysShow
    * @return the react component
    */
  def apply(alwaysShow: () => Boolean) =
    component(
      new Props(alwaysShow)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

    /**
      * Enable beep.  This will cause a beep.
      */
    def enableBeep() = {
    playEnabled = true
    log.info("Beep enabled")
    beep()
  }

  def disableBeep() = {
    playEnabled = false
    log.info("Beep disabled")
  }

  /**
    * toggle the beep.  If the beep is enabled, then a beep will play.
    */
  def toggleBeep(): Unit = {
    playEnabled = !playEnabled
    log.info(s"Beep playEnabled=$playEnabled")
    if (playEnabled) beep()
  }

  def isPlayEnabled = playEnabled

  /**
    * Play the sound for the beep.
    */
  def beep(): Unit = {
    if (playEnabled) {
      try {
        val audio =
          Info.getElement("audioPlayer").asInstanceOf[HTMLAudioElement]
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

  private def toggle(cb: () => Unit)(e: ReactEvent): Unit = {
    toggleBeep()
    cb()
  }

  /**
    * Get a MUI MenuItem component that toggles the beep
    *
    * @param cb a callback that is called when the menu item is clicked.
    * @return the react component, MuiMenuItem
    */
  def getMenuItem(cb: () => Unit): VdomNode = {
    MuiMenuItem(
      id = "Beep",
      onClick = toggle(cb) _,
      classes = js.Dictionary("root" -> "mainMenuItem")
    )(
      "Beep", {
//          val color = if (playEnabled) SvgColor.inherit else SvgColor.disabled
//          icons.Check(
//              color=color,
//              classes = js.Dictionary("root" -> "mainMenuItemIcon")
//          )
        if (playEnabled) {
          icons.CheckBox()
        } else {
          icons.CheckBoxOutlineBlank()
        }
      }
    )
  }

  protected object Internal {

    val log: Logger = Logger("bridge.BeepComponent")

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(displayButtons: Boolean = true)

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      val enablePlay: Callback = scope.modState(s => {
        enableBeep()
        s.copy(displayButtons = false)
      })

      val hideButtons: Callback = scope.modState(s => {
        disableBeep()
        s.copy(displayButtons = false)
      })

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          (props.alwaysShow() || state.displayButtons) ?= <.span(
            AppButton(
              "enableBeep",
              "Enable Beeps",
              ^.onClick --> enablePlay,
              BaseStyles.highlight(selected = isPlayEnabled)
            ),
            AppButton("disableBeep", "Disable Beeps", ^.onClick --> hideButtons)
          )
        )
      }
    }

    private[react] val component = ScalaComponent
      .builder[Props]("BeepComponent")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build

  }

}
