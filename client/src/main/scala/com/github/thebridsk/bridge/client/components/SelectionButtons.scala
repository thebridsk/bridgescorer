package com.github.thebridsk.bridge.client.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.Button
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.rootStyles2
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.baseStyles2
import com.github.thebridsk.bridge.client.pages.Pixels
import com.github.thebridsk.utilities.logging.Logger

/**
  * Component to select a position.
  *
  * Properties:
  *   buttons - a list of ButtonData objects
  *   selected  - the currently selected button
  *   onChange - callback when a change occurs, argument is the selected button
  *
  */
object SelectionButtons {

  trait ButtonData {
    val id: String
    val label: String
    val disabled: Boolean
  }

  case class Props[T <: ButtonData](
    onChange: T => Callback,
    selected: Option[T],
    buttons: List[T]
  )

  def apply[T <: ButtonData](
    onChange: T => Callback,
    selected: Option[T],
    buttons: List[T]
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val on: ButtonData => Callback = b => onChange(b.asInstanceOf[T])
    Internal.component(Props(on, selected, buttons))
  }

  protected object Internal {

    val log = Logger("bridge.SelectionButtons")

    case class State()

    class Backend[T <: ButtonData](scope: BackendScope[Props[T], State]) {
      def render(props: Props[T], state: State) = { // scalafix:ok ExplicitResultTypes; React
        val width = Properties.maxLength(props.buttons.map(_.label):_*)
        <.div(
          baseStyles2.clsSelectPosition,
          <.div(
            props.buttons.map { button =>
              <.div(
                Button(
                  rootStyles2.clsSelectionButton,
                  button.id,
                  button.label,
                  ^.onClick --> props.onChange(button),
                  ^.width := width.px,
                  ^.disabled := button.disabled,
                  BaseStyles.highlight(
                    selected = props.selected.map(s => button==s).getOrElse(false),
                    required = props.selected.isEmpty
                  )
                )
              )
            }.toTagMod
          )
        )
      }

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true

      }

      val willUnmount: Callback = Callback {
        mounted = false

      }
    }

    val component = ScalaComponent
      .builder[Props[ButtonData]]("SelectionButtons")
      .initialStateFromProps { props => State() }
      .backend(new Backend[ButtonData](_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

  object Properties {
    import Internal._

    lazy val defaultHandButtonFont: String = Pixels.getFont("BaseDefaultButton")
    lazy val extraLength: Int = {
      val (elem, computed) = Pixels.getElementAndComputedProperties("BaseDefaultButton")
      val name = elem.innerHTML
      val defaultWidth = Pixels.getPixels("width", computed.width, "BaseDefaultButton", -1)
      val r = if (defaultWidth < 0) {
        10
      } else {
        defaultWidth - Pixels.maxLengthWithFont(defaultHandButtonFont, name)
      }
      log.fine(s"extraLength is ${r}, defaultWidth is ${defaultWidth}")
      r
    }

    def maxLength( names: String* ): Int = {
      val l = Pixels.maxLengthWithFont(defaultHandButtonFont, names:_*)
      val r = l + extraLength
      log.fine(s"Length is ${r} of names: ${names.mkString}")
      r
    }
  }
}
