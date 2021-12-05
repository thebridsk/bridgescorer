package com.github.thebridsk.bridge.client.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.bridge.store.NamesStore
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.baseStyles2
import com.github.thebridsk.bridge.clientcommon.react.Combobox
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.materialui.MuiAutocomplete
import scala.scalajs.js
import japgolly.scalajs.react.facade.SyntheticEvent
import org.scalajs.dom.Node

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

    import scala.language.implicitConversions
    implicit def toAny(s: String): js.Any = s
    implicit def toUndefOrAny(s: String): js.UndefOr[js.Any] = {
      val x: js.Any = s
      val y: js.UndefOr[js.Any] = x
      y
    }
    implicit def toUndefOrArrayAny(a: js.Array[String]): js.UndefOr[js.Array[js.Any]] = {
      val x = js.Array[js.Any](a.map(t=>t))
      val y: js.UndefOr[js.Array[js.Any]] = x
      y
    }

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val n = noNull(props.name)
        <.div(
          baseStyles2.clsEnterNames,
          BaseStyles.highlight(required = n.isEmpty),
          // MuiAutocomplete(
          //   autoComplete = true,
          //   value = n,
          //   id = props.id,
          //   options = NamesStore.getNames,
          //   freeSolo = true,
          //   loading = NamesStore.isBusy,
          //   onInputChange = onInputChange(props)
          // )
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
          )
        )
      }

      def onInputChange(
        props: Props
      ): js.Function3[SyntheticEvent[Node], String, String, Unit] =
        (
          event: SyntheticEvent[Node],
          value: String,
          reason: String
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
