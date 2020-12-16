package com.github.thebridsk.bridge.clientcommon.materialui.component

import japgolly.scalajs.react._
import com.github.thebridsk.materialui.MuiIconButton
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js
import org.scalajs.dom.document
import com.github.thebridsk.materialui.icons

/**
  * A button to click to enter and exit fullscreen mode.
  *
  * The DOM fullscreen mode is slightly different from the browser fullscreen mode.
  * The browser fullscreen controls will not terminate the DOM fullscreen mode.
  *
  * The button will show the appropriate icon depending on the state.
  *
  * Clicking the button either enters or exits fullscreen mode.
  *
  * To use, just code the following:
  *
  * {{{
  * val buttonStyle = js.Dictionary("root" -> "toolbarIcon")
  * FullscreenButton(
  *   classes = buttonStyle
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object FullscreenButton {
  import Internal._

  case class Props(
      classes: js.UndefOr[js.Dictionary[String]],
  )

  /**
    * Intantiate the component.
    *
    * @param classes - a material-ui defined object.
    *                  Should have one field, "root" with a value that
    *                  identifies the class name to apply to the root element.
    * @return the unmounted react component
    *
    * @see [[FullscreenButton]] for usage.
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
  ) =
    component(Props(classes)) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @return true if DOM fullscreen is active.
    */
  def isFullscreen: Boolean = {
    import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._
    val doc = document
    logger.info(s"browser fullscreenEnabled: ${doc.myFullscreenEnabled}")
    if (isFullscreenEnabledI) {
      val fe = doc.myFullscreenElement
      val r = !js.isUndefined(fe) && fe != null
      logger.fine(s"browser isfullscreen: $r")
      if (r) {
        val elem = doc.myFullscreenElement
        logger.info(s"browser fullscreen element is ${elem.nodeName}")
      }
      r
    } else {
      false
    }
  }

  protected object Internal {

    val logger: Logger = Logger("bridge.FullscreenButton")

    case class State()

    def isFullscreenEnabledI: Boolean = {
      import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._
      val doc = document
      logger.info(s"browser fullscreenEnabled: ${doc.myFullscreenEnabled}")
      val e = doc.myFullscreenEnabled
      if (!e) {
        logger.info("fullscreenEnabled = false")
      }
      e
    }

    class Backend(scope: BackendScope[Props, State]) {

      def toggleFullscreen(event: ReactEvent): Unit = {
        import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._
        val body = document.documentElement
        val doc = document
        if (isFullscreenEnabled) {
          val isfullscreen = isFullscreen
          if (isfullscreen) {
            logger.info(s"browser exiting fullscreen")
            doc.myExitFullscreen()
          } else {
            logger.info(s"browser requesting fullscreen on body")
            body.requestFullscreen()
          }
          scalajs.js.timers.setTimeout(500) {
            scope.withEffectsImpure.forceUpdate
          }
        } else {
          logger.info(s"fullscreen is disabled")
        }
      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        MuiIconButton(
          id = "Fullscreen",
          onClick = toggleFullscreen _,
          title = if (isFullscreen) "Exit fullscreen" else "Go to fullscreen",
          color = ColorVariant.inherit,
          classes = props.classes
        )(
          if (isFullscreen) {
            icons.FullscreenExit()
          } else {
            icons.Fullscreen()
          }
        )
      }
    }

    val component = ScalaComponent
      .builder[Props]("FullscreenButton")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
