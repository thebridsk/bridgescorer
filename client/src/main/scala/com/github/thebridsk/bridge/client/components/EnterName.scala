package com.github.thebridsk.bridge.client.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.baseStyles2
import com.github.thebridsk.bridge.clientcommon.react.Combobox
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles

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

    case class State()

    private def noNull(s: String) = Option(s).getOrElse("")

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val n = noNull(props.name)
        <.div(
          baseStyles2.clsEnterNames,
          Combobox.create(
            props.onChange,
            n,
            NamesStore.getNames,
            "startsWith",
            props.tabIndex,
            props.id,
            msgEmptyList = "No suggested names",
            msgEmptyFilter = "No names matched",
            busy = NamesStore.isBusy,
            id = props.id
          ),
          BaseStyles.highlight(required = n.isEmpty)

        )
      }

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
