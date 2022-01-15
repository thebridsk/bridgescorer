package com.github.thebridsk.bridge.clienttest.store

import com.github.thebridsk.redux.NativeAction
import com.github.thebridsk.redux.Action
import scala.scalajs.js
import com.github.thebridsk.utilities.logging.Logger

case class BaseState(mode: String)

object BaseState {
  private val log: Logger = Logger("bridge.BaseState")

  val actionDarkMode = NativeAction.simple("darkMode")
  val actionLightMode = NativeAction.simple("lightMode")
  val actionToggleMode = NativeAction.simple("toggleMode")

  def reducer(state: js.UndefOr[BaseState], action: NativeAction[Action]): BaseState = {
    val s = state.getOrElse(BaseState(actionDarkMode.actiontype))

    val ns = action.actiontype match {
      case actionDarkMode.actiontype =>
        log.info("Setting dark mode")
        s.copy( mode = actionDarkMode.actiontype)
      case actionLightMode.actiontype =>
        log.info("Setting light mode")
        s.copy( mode = actionLightMode.actiontype)
      case actionToggleMode.actiontype =>
        val mode =
          if (s.mode == actionDarkMode.actiontype) actionLightMode.actiontype
          else actionDarkMode.actiontype
        log.info(s"toggle mode to ${mode}")
        s.copy( mode = mode)
      case _ =>
        s
    }
    ns
  }

}
