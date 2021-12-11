package com.github.thebridsk.bridge.client.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.baseStyles2
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import scala.scalajs.js
import japgolly.scalajs.react.facade.SyntheticEvent
import org.scalajs.dom.Node
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.javascript.ObjectToString
import com.github.thebridsk.materialui.MuiTextField
import com.github.thebridsk.materialui.MuiAutocomplete

/**
  * Component to enter a name.  A combobox is displayed.
  * The dropdown is populated with all the known names.
  *
  * Usage:
  *
  * {{{
  * case class State(name: String)
  * def onChange(name: String): Callback = {
  *   scope.modState(_.copy(name=name))
  * }
  *
  * EnterName(
  *   id = ...,
  *   name = state.name,
  *   tabIndex = -1,
  *   onChange = onChange _
  * )
  * }}}
  */
object EnterName {

  case class Props(
    id: String,
    name: String,
    tabIndex: Int,
    onChange: String => Callback
  )

  /**
    * Instantiate the component
    *
    * @param id the id attribute on the input field
    * @param name the value for the input field
    * @param tabIndex the tabIndex attribute on the input field
    * @param onChange callback when the input field value changes
    *
    * @return the unmounted react component
    */
  def apply(
    id: String,
    name: String,
    tabIndex: Int,
    onChange: String => Callback
  ) =
    Internal.component(Props(id,name,tabIndex,onChange)) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val log: Logger = Logger("bridge.EnterName")

    case class State()

    private def noNull(s: String) = Option(s).getOrElse("")

    import scala.language.implicitConversions
    implicit def toUndefOrAny(s: String): js.UndefOr[js.Any] = {
      val x: js.Any = s
      val y: js.UndefOr[js.Any] = x
      y
    }
    implicit def toArrayAny(a: js.Array[String]): js.Array[js.Any] = {
      val x = js.Array[js.Any](a.map(t=>t))
      x
    }

    class Backend(scope: BackendScope[Props, State]) {

      def renderInput(props: Props, state: State)(params: js.Object): facade.React.Node = {
        log.info(s"EnterNames params=${ObjectToString.objToString(params)}")

        // params are:
        //   id: Combo_South
        //   disabled: false
        //   fullWidth: true
        //   size: <undefined>
        //   InputLabelProps: object
        //     id: Combo_South-label
        //     htmlFor: Combo_South
        //   InputProps: object
        //     ref: <function>
        //     className: MuiAutocomplete-inputRoot
        //     startAdornment: <undefined>
        //     endAdornment: object
        //       $$typeof: Symbol(react.element)
        //       type: object
        //       key: <undefined>
        //       ref: <undefined>
        //       props: object
        //       _owner: object
        //       _store: object
        //   inputProps: object
        //     className: MuiAutocomplete-input MuiAutocomplete-inputFocused
        //     disabled: false
        //     id: Combo_South
        //     value:
        //     onBlur: <function>
        //     onFocus: <function>
        //     onChange: <function>
        //     onMouseDown: <function>
        //     aria-activedescendant: <undefined>
        //     aria-autocomplete: list
        //     aria-controls: <undefined>
        //     autoComplete: off
        //     ref: object
        //       current: <undefined>
        //     autoCapitalize: none
        //     spellCheck: false

        MuiTextField(params.asInstanceOf[js.Dictionary[js.Any]])().rawElement
      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val n = noNull(props.name)
        val names = NamesStore.getNames
        val isBusy = NamesStore.isBusy
        log.info(s"EnterNames props=${props}, isBusy=${isBusy} names=${names}")
        <.div(
          baseStyles2.clsEnterNames,
          BaseStyles.highlight(required = n.isEmpty),
          MuiAutocomplete(
            // autoComplete = true,
            value = n,
            id = props.id,
            options = names,
            freeSolo = true,
            loading = isBusy,
            onInputChange = onInputChange(props),
            disablePortal = true,
            renderInput = renderInput(props, state)
          )
        )
      }

      def onInputChange(
        props: Props
      ): js.Function3[SyntheticEvent[Node], String, js.UndefOr[String], Unit] =
        (
          event: SyntheticEvent[Node],
          value: String,
          reason: js.UndefOr[String]
        ) => props.onChange(value).runNow()

      private val namesCallback = scope.forceUpdate

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true
        NamesStore.addChangeListener(namesCallback)
        NamesStore.ensureNamesAreCached()
      }

      val willUnmount: Callback = Callback {
        mounted = false
        NamesStore.removeChangeListener(namesCallback)
      }
    }

    val component = ScalaComponent
      .builder[Props]("EnterName")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
