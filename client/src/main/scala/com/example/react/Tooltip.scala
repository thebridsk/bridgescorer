package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.pages.BaseStyles._
import org.scalajs.dom.raw.Event

/**
 * Creates a tooltip that will show up when a mouse enters the element,
 * or when the element is touched on a touch screen.
 *
 * To use, just code the following to initialize for touch screens:
 *
 * {{{
 * Tooltip.init     // only needs to be called once
 * }}}
 *
 * Then to create a tooltip:
 *
 * {{{
 * val data: TagMod
 * val tooltip: TagMod
 * Tooltip( data, tooltip )
 * }}}
 *
 * This will generate the following html:
 *
 * {{{
 * <div class="withTooltipBox">
 *   data
 *   <div class="tooltipContent">
 *     tooltip
 *   </div>
 * </div>
 * }}}
 *
 * If the data TagMod contains attributes, they will be added to the withTooltipBox div.
 * If the tooltip TagMod contains attributes, they will be added to the tooltipContent div.
 *
 * The CSS on the element with class withTooltipBox should not adjust the following styles,
 * these are needed to control the displaying of the tooltip:
 *   position
 *   user-select
 *
 * The CSS on the element with claass tooltipContent should not adjust the following styles:
 *   visibility
 *   position
 *   z-index
 *
 * The following CSS will put up a tooltip with rounded corners, and
 * set the background of the data element to orange:
 *
 * {{{
 * .withTooltipBox {
 *    /* Don't change this */
 *    position: relative;
 * }
 * .withTooltipBox > .tooltipContent {
 *    /* Don't change this */
 *     visibility: hidden;
 *     position: absolute;
 *     z-index: 1;
 *    /* the following can be changed */
 *     top: 90%;
 *     left: 10%;
 *     color: black;
 *     background-color: white;
 *     padding: 5px;
 *     border-radius: 6px;
 *     border: 1px solid black;
 * }
 * /* Show the tooltip text when you mouse over the tooltip container */
 * .withTooltipBox:hover > .tooltipContent {
 *    /* Don't change this */
 *     visibility: visible;
 * }
 * body.touched .withTooltipBox:hover > .tooltipContent {
 *    /* Don't change this */
 *     visibility: visible;
 * }
 *
 * .withTooltipBox:hover {
 *    /* Don't change this */
 *     user-select: auto;
 *    /* the following can be changed */
 *     background-color: orange;
 * }
 *
 * body.touched .withTooltipBox:hover {
 *    /* Don't change this */
 *     user-select: auto;
 *    /* the following can be changed */
 *     background-color: orange;
 * }
 *
 * }}}
 *
 * Based on https://stackoverflow.com/questions/12539006/tooltips-for-mobile-browsers?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
 *
 * @author werewolf
 */
object Tooltip {
  import TooltipInternal._

  case class Props(
      data: TagMod,
      tooltip: TagMod
  )

  /**
   * @param data the data to display.
   * @param tooltip the tooltip for the data when the mouse flies over.
   */
  def apply(
      data: TagMod,
      tooltip: TagMod
  ) = component( Props(data,tooltip) )

  private var initialized: Boolean = false

  def init() = {
    import org.scalajs.dom.document

    if (!initialized) {
      document.body.addEventListener(
          "touchstart",
          listener = (e: Event)=>document.body.classList.add("touched"),
          useCapture = false)
      initialized = true
    }
  }
}

object TooltipInternal {
  import Tooltip._

  val component = ScalaComponent.builder[Props]("Tooltip")
      .stateless
      .noBackend
      .render_P { props =>
        <.div(
          baseStyles.withTooltipBox,
          props.data,
          <.div(
            baseStyles.tooltipContent,
            props.tooltip
          )
        )
      }.build
}

