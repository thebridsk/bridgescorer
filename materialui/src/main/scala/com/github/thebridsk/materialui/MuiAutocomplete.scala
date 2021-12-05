package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import japgolly.scalajs.react.facade.SyntheticEvent
import org.scalajs.dom.Node

@js.native
trait AutocompleteProps extends AdditionalProps with StandardProps {
  val options: js.UndefOr[js.Array[js.Any]] = js.undefined
  val autoComplete: js.UndefOr[Boolean] = js.undefined
  val autoHighlight: js.UndefOr[Boolean] = js.undefined
  val autoSelect: js.UndefOr[Boolean] = js.undefined
  val blurOnSelect: js.UndefOr[js.Any] = js.undefined
  val clearIcon: js.UndefOr[js.Object /* Node */] = js.undefined
  val clearOnBlur: js.UndefOr[Boolean] = js.undefined
  val clearOnEscape: js.UndefOr[Boolean] = js.undefined
  val clearText: js.UndefOr[String] = js.undefined
  val closeText: js.UndefOr[String] = js.undefined
  val defaultValue: js.UndefOr[js.Any] = js.undefined
  val disableClearable: js.UndefOr[Boolean] = js.undefined
  val disableCloseOnSelect: js.UndefOr[Boolean] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val disabledItemsFocusable: js.UndefOr[Boolean] = js.undefined
  val disableListWrap: js.UndefOr[Boolean] = js.undefined
  val disablePortal: js.UndefOr[Boolean] = js.undefined
  val filterOptions: js.UndefOr[js.Function2[js.Array[js.Any], js.Object, js.Array[js.Any]]] = js.undefined
  val filterSelectedOptions: js.UndefOr[Boolean] = js.undefined
  val forcePopupIcon: js.UndefOr[js.Any] = js.undefined
  val freeSolo: js.UndefOr[Boolean] = js.undefined
  val fullWidth: js.UndefOr[Boolean] = js.undefined
  val getLimitTagsText: js.UndefOr[js.Function1[js.Any /* number */, js.Object /* ReactNode */]] = js.undefined
  val getOptionDisabled: js.UndefOr[js.Function1[js.Any, Boolean]] = js.undefined
  val getOptionLabel: js.UndefOr[js.Function1[js.Any, String]] = js.undefined
  val groupBy: js.UndefOr[js.Function1[js.Any, String]] = js.undefined
  val handleHomeEndKeys: js.UndefOr[Boolean] = js.undefined
  val id: js.UndefOr[String] = js.undefined
  val includeInputInList: js.UndefOr[Boolean] = js.undefined
  val inputValue: js.UndefOr[String] = js.undefined
  val isOptionEqualToValue: js.UndefOr[Boolean] = js.undefined
  val limitTags: js.UndefOr[js.Any /* number */] = js.undefined
  val ListboxComponent: js.UndefOr[String] = js.undefined
  val ListboxProps: js.UndefOr[js.Object] = js.undefined
  val loading: js.UndefOr[Boolean] = js.undefined
  val loadingText: js.UndefOr[js.Object /* Node */] = js.undefined
  val multiple: js.UndefOr[Boolean] = js.undefined
  val noOptionsText: js.UndefOr[js.Object /* Node */] = js.undefined
  val onChange: js.UndefOr[js.Function4[SyntheticEvent[Node], js.Any, String, String, Unit]] = js.undefined
  val onClose: js.UndefOr[js.Function2[SyntheticEvent[Node], String, Unit]] = js.undefined
  val onHighlightChange: js.UndefOr[js.Function3[SyntheticEvent[Node], js.Any, String, Unit]] = js.undefined
  val onInputChange: js.UndefOr[js.Function3[SyntheticEvent[Node], String, String, Unit]] = js.undefined
  val onOpen: js.UndefOr[js.Function1[SyntheticEvent[Node], Unit]] = js.undefined
  val open: js.UndefOr[Boolean] = js.undefined
  val openOnFocus: js.UndefOr[Boolean] = js.undefined
  val openText: js.UndefOr[String] = js.undefined
  val paperComponent: js.UndefOr[js.Object /* elementType */] = js.undefined
  val popperComponent: js.UndefOr[js.Object /* elementType */] = js.undefined
  val popupIcon: js.UndefOr[js.Object /* Node */] = js.undefined
  val renderGroup: js.UndefOr[js.Function1[js.Object /* AutocompleteRenderGroupParams */, js.Object /* ReactNode */]] = js.undefined
  val renderOption: js.UndefOr[js.Function3[js.Object, js.Any, js.Object, js.Object /* ReactNode */]] = js.undefined
  val renderTags: js.UndefOr[js.Function2[js.Array[js.Any], js.Any, js.Object /* ReactNode */]] = js.undefined
  val selectOnFocus: js.UndefOr[Boolean] = js.undefined
  val size: js.UndefOr[String] = js.undefined
  val value: js.UndefOr[js.Any] = js.undefined

  val classes: js.UndefOr[js.Dictionary[String]] = js.undefined

}

object AutocompleteProps extends PropsFactory[AutocompleteProps] {

  /**
    * @param p the object that will become the properties object
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param options Array of options.  These are the values for the autocomplete.
    * @param autoComplete If true, the portion of the selected suggestion that has not been typed by the user,
    *                     known as the completion string, appears inline after the input cursor in the textbox.
    *                     The inline completion string is visually highlighted and has a selected state.
    *               Default: false
    * @param autoHighlight If true, the first option is automatically highlighted.
    *               Default: false
    * @param autoSelect If true, the selected option becomes the value of the input when the Autocomplete loses
    *                   focus unless the user chooses a different option or changes the character string in the input.
    *               Default: false
    * @param blurOnSelect Control if the input should be blurred when an option is selected:
    *                     - false the input is not blurred.  Default
    *                     - true the input is always blurred.
    *                     - 'touch' the input is blurred after a touch event.
    *                     - 'mouse' the input is blurred after a mouse event.
    * @param clearIcon  The icon to display in place of the default clear icon.  A Node.
    *                   Default: <ClearIcon fontSize="small" />
    * @param clearOnBlur  If true, the input's text is cleared on blur if no value is selected.
    *                     Set to true if you want to help the user enter a new value. Set to false if you want to help the user resume his search.
    *                     Default: !freeSolo
    * @param clearOnEscape If true, clear all values when the user presses escape and the popup is closed.
    *                      Default: false
    * @param clearText  Override the default text for the clear icon button.
    *                   For localization purposes, you can use the provided translations.
    *                   Default: 'Clear'
    * @param closeText  Override the default text for the close popup icon button.
    *                   For localization purposes, you can use the provided translations.
    *                   Default: 'Close'
    * @param defaultValue The default value. Use when the component is not controlled.
    * @param disableClearable If true, the input can't be cleared.  Default false.
    * @param disableCloseOnSelect If true, the popup won't close when a value is selected.  Default false.
    * @param disabled If true, the component is disabled.  Default false.
    * @param disabledItemsFocusable If true, will allow focus on disabled items.  Default false.
    * @param disableListWrap If true, the list box in the popup will not wrap focus.  Default false.
    * @param disablePortal If true, the Popper content will be under the DOM hierarchy of the parent component.  Default false.
    * @param filterOptions A filter function that determines the options that are eligible.
    *                      Signature:
    *                        function(options: Array<T>, state: object) => Array<T>
    *                      options: The options to render.
    *                      state: The state of the component.
    * @param filterSelectedOptions If true, hide the selected options from the list box.  Default false
    * @param forcePopupIcon Force the visibility display of the popup icon.
    *                       Values: 'auto'|bool
    *                       Default: 'auto'
    * @param freeSolo If true, the Autocomplete is free solo, meaning that the user input is not bound to provided options.  Default false
    * @param fullWidth If true, the input will take up the full width of its container.  Default false
    * @param getLimitTagsText The label to display when the tags are truncated (limitTags).
    *                         Signature:
    *                           function(more: number) => ReactNode
    *                             more: The number of truncated tags.
    *                         Default: (more) => `+${more}`
    * @param getOptionDisabled Used to determine the disabled state for a given option.
    *                          Signature:
    *                            function(option: T) => boolean
    *                            option: The option to test.
    * @param getOptionLabel Used to determine the string value for a given option.
    *                       It's used to fill the input (and the list box options if renderOption is not provided).
    *                       Signature:
    *                         function(option: T) => string
    *                       Default: (option) => option.label ?? option
    * @param groupBy If provided, the options will be grouped under the returned string.
    *                The groupBy value is also used as the text for group headings when renderGroup is not provided.
    *                Signature:
    *                  function(options: T) => string
    *                  options: The options to group.
    * @param handleHomeEndKeys If true, the component handles the "Home" and "End" keys when the popup is open.
    *                          It should move focus to the first option and last option, respectively.
    *                          Default: !freeSolo
    * @param id This prop is used to help implement the accessibility logic.
    *           If you don't provide an id it will fall back to a randomly generated one.
    * @param includeInputInList If true, the highlight can move to the input.
    *                           Default: false
    * @param inputValue The input value.
    * @param isOptionEqualToValue Used to determine if the option represents the given value.
    *                             Uses strict equality by default. Both arguments need to be handled,
    *                             an option can only match with one value.
    *                             Signature:
    *                             function(option: T, value: T) => boolean
    *                               option: The option to test.
    *                               value: The value to test against.
    * @param limitTags The maximum number of tags that will be visible when not focused. Set -1 to disable the limit.
    *                  Default: -1
    * @param ListboxComponent The component used to render the listbox.
    *                         Default: 'ul'
    * @param ListboxProps Props applied to the Listbox element.
    * @param loading If true, the component is in a loading state. This shows the loadingText in place of
    *                suggestions (only if there are no suggestions to show, e.g. options are empty).
    *                Default: false
    * @param loadingText Text to display when in a loading state.
    *                    For localization purposes, you can use the provided translations.
    *                    Default: 'Loading…'
    * @param multiple If true, value must be an array and the menu will support multiple selections.
    *                 Default: false
    * @param noOptionsText Text to display when there are no options.
    *                      For localization purposes, you can use the provided translations.
    *                      Default: 'No options'
    * @param onChange Callback fired when the value changes.
    *                 Signature:
    *                   function(event: React.SyntheticEvent, value: T | Array<T>, reason: string, details?: string) => void
    *                     event: The event source of the callback.
    *                     value: The new value of the component.
    *                     reason: One of "createOption", "selectOption", "removeOption", "blur" or "clear".
    * @param onClose Callback fired when the popup requests to be closed. Use in controlled mode (see open).
    *                Signature:
    *                  function(event: React.SyntheticEvent, reason: string) => void
    *                    event: The event source of the callback.
    *                    reason: Can be: "toggleInput", "escape", "selectOption", "removeOption", "blur".
    * @param onHighlightChange Callback fired when the highlight option changes.
    *                          Signature:
    *                          function(event: React.SyntheticEvent, option: T, reason: string) => void
    *                            event: The event source of the callback.
    *                            option: The highlighted option.
    *                            reason: Can be: "keyboard", "auto", "mouse".
    * @param onInputChange Callback fired when the input value changes.
    *                      Signature:
    *                        function(event: React.SyntheticEvent, value: string, reason: string) => void
    *                          event: The event source of the callback.
    *                          value: The new value of the text input.
    *                          reason: Can be: "input" (user input), "reset" (programmatic change), "clear".
    * @param onOpen Callback fired when the popup requests to be opened. Use in controlled mode (see open).
    *               Signature:
    *                 function(event: React.SyntheticEvent) => void
    *                   event: The event source of the callback.
    * @param open If true, the component is shown.
    *             Default: false
    * @param openOnFocus If true, the popup will open on input focus.
    *                    Default: false
    * @param openText Override the default text for the open popup icon button.
    *                 For localization purposes, you can use the provided translations.
    *                 Default: 'Open'
    * @param PaperComponent The component used to render the body of the popup.
    *                       Default: Paper
    * @param PopperComponent The component used to position the popup.
    *                        Default: Popper
    * @param popupIcon The icon to display in place of the default popup icon.
    *                  Default: <ArrowDropDownIcon />
    * @param renderGroup Render the group.
    *                    Signature:
    *                      function(params: AutocompleteRenderGroupParams) => ReactNode
    *                        params: The group to render.
    * @param renderOption Render the option, use getOptionLabel by default.
    *                     Signature:
    *                       function(props: object, option: T, state: object) => ReactNode
    *                         props: The props to apply on the li element.
    *                         option: The option to render.
    *                         state: The state of the component.
    * @param renderTags Render the selected value.
    *                   Signature:
    *                     function(value: Array<T>, getTagProps: function) => ReactNode
    *                       value: The value provided to the component.
    *                       getTagProps: A tag props getter.
    * @param selectOnFocus If true, the input's text is selected on focus. It helps the user clear the selected value.
    *                      Default: !freeSolo
    * @param size The size of the component.
    *             Default: 'small'
    * @param value The value of the autocomplete.
    *              The value must have reference equality with the option in order to be selected.
    *              You can customize the equality behavior with the isOptionEqualToValue prop.
    *
    *
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: AutocompleteProps](
      props: js.UndefOr[P] = js.undefined,
      options: js.UndefOr[js.Array[js.Any]] = js.undefined,
      autoComplete: js.UndefOr[Boolean] = js.undefined,
      autoHighlight: js.UndefOr[Boolean] = js.undefined,
      autoSelect: js.UndefOr[Boolean] = js.undefined,
      blurOnSelect: js.UndefOr[js.Any] = js.undefined,
      clearIcon: js.UndefOr[js.Object /* Node */] = js.undefined,
      clearOnBlur: js.UndefOr[Boolean] = js.undefined,
      clearOnEscape: js.UndefOr[Boolean] = js.undefined,
      clearText: js.UndefOr[String] = js.undefined,
      closeText: js.UndefOr[String] = js.undefined,
      defaultValue: js.UndefOr[js.Any] = js.undefined,
      disableClearable: js.UndefOr[Boolean] = js.undefined,
      disableCloseOnSelect: js.UndefOr[Boolean] = js.undefined,
      disabled: js.UndefOr[Boolean] = js.undefined,
      disabledItemsFocusable: js.UndefOr[Boolean] = js.undefined,
      disableListWrap: js.UndefOr[Boolean] = js.undefined,
      disablePortal: js.UndefOr[Boolean] = js.undefined,
      filterOptions: js.UndefOr[js.Function2[js.Array[js.Any], js.Object, js.Array[js.Any]]] = js.undefined,
      filterSelectedOptions: js.UndefOr[Boolean] = js.undefined,
      forcePopupIcon: js.UndefOr[js.Any] = js.undefined,
      freeSolo: js.UndefOr[Boolean] = js.undefined,
      fullWidth: js.UndefOr[Boolean] = js.undefined,
      getLimitTagsText: js.UndefOr[js.Function1[js.Any /* number */, js.Object /* ReactNode */]] = js.undefined,
      getOptionDisabled: js.UndefOr[js.Function1[js.Any, Boolean]] = js.undefined,
      getOptionLabel: js.UndefOr[js.Function1[js.Any, String]] = js.undefined,
      groupBy: js.UndefOr[js.Function1[js.Any, String]] = js.undefined,
      handleHomeEndKeys: js.UndefOr[Boolean] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      includeInputInList: js.UndefOr[Boolean] = js.undefined,
      inputValue: js.UndefOr[String] = js.undefined,
      isOptionEqualToValue: js.UndefOr[Boolean] = js.undefined,
      limitTags: js.UndefOr[js.Any /* number */] = js.undefined,
      listboxComponent: js.UndefOr[String] = js.undefined,
      listboxProps: js.UndefOr[js.Object] = js.undefined,
      loading: js.UndefOr[Boolean] = js.undefined,
      loadingText: js.UndefOr[js.Object /* Node */] = js.undefined,
      multiple: js.UndefOr[Boolean] = js.undefined,
      noOptionsText: js.UndefOr[js.Object /* Node */] = js.undefined,
      onChange: js.UndefOr[js.Function4[SyntheticEvent[Node], js.Any, String, String, Unit]] = js.undefined,
      onClose: js.UndefOr[js.Function2[SyntheticEvent[Node], String, Unit]] = js.undefined,
      onHighlightChange: js.UndefOr[js.Function3[SyntheticEvent[Node], js.Any, String, Unit]] = js.undefined,
      onInputChange: js.UndefOr[js.Function3[SyntheticEvent[Node], String, String, Unit]] = js.undefined,
      onOpen: js.UndefOr[js.Function1[SyntheticEvent[Node], Unit]] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      openOnFocus: js.UndefOr[Boolean] = js.undefined,
      openText: js.UndefOr[String] = js.undefined,
      paperComponent: js.UndefOr[js.Object /* elementType */] = js.undefined,
      popperComponent: js.UndefOr[js.Object /* elementType */] = js.undefined,
      popupIcon: js.UndefOr[js.Object /* Node */] = js.undefined,
      renderGroup: js.UndefOr[js.Function1[js.Object /* AutocompleteRenderGroupParams */, js.Object /* ReactNode */]] = js.undefined,
      renderOption: js.UndefOr[js.Function3[js.Object, js.Any, js.Object, js.Object /* ReactNode */]] = js.undefined,
      renderTags: js.UndefOr[js.Function2[js.Array[js.Any], js.Any, js.Object /* ReactNode */]] = js.undefined,
      selectOnFocus: js.UndefOr[Boolean] = js.undefined,
      size: js.UndefOr[String] = js.undefined,
      value: js.UndefOr[js.Any] = js.undefined,


      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    classes.foreach(p.updateDynamic("classes")(_))
    className.foreach(p.updateDynamic("className")(_))

    props.foreach(p.updateDynamic("props")(_))
    options.foreach(p.updateDynamic("options")(_))
    autoComplete.foreach(p.updateDynamic("autoComplete")(_))
    autoHighlight.foreach(p.updateDynamic("autoHighlight")(_))
    autoSelect.foreach(p.updateDynamic("autoSelect")(_))
    blurOnSelect.foreach(p.updateDynamic("blurOnSelect")(_))
    clearIcon.foreach(p.updateDynamic("clearIcon")(_))
    clearOnBlur.foreach(p.updateDynamic("clearOnBlur")(_))
    clearOnEscape.foreach(p.updateDynamic("clearOnEscape")(_))
    clearText.foreach(p.updateDynamic("clearText")(_))
    closeText.foreach(p.updateDynamic("closeText")(_))
    defaultValue.foreach(p.updateDynamic("defaultValue")(_))
    disableClearable.foreach(p.updateDynamic("disableClearable")(_))
    disableCloseOnSelect.foreach(p.updateDynamic("disableCloseOnSelect")(_))
    disabled.foreach(p.updateDynamic("disabled")(_))
    disabledItemsFocusable.foreach(p.updateDynamic("disabledItemsFocusable")(_))
    disableListWrap.foreach(p.updateDynamic("disableListWrap")(_))
    disablePortal.foreach(p.updateDynamic("disablePortal")(_))
    filterOptions.foreach(p.updateDynamic("filterOptions")(_))
    filterSelectedOptions.foreach(p.updateDynamic("filterSelectedOptions")(_))
    forcePopupIcon.foreach(p.updateDynamic("forcePopupIcon")(_))
    freeSolo.foreach(p.updateDynamic("freeSolo")(_))
    fullWidth.foreach(p.updateDynamic("fullWidth")(_))
    getLimitTagsText.foreach(p.updateDynamic("getLimitTagsText")(_))
    getOptionDisabled.foreach(p.updateDynamic("getOptionDisabled")(_))
    getOptionLabel.foreach(p.updateDynamic("getOptionLabel")(_))
    groupBy.foreach(p.updateDynamic("groupBy")(_))
    handleHomeEndKeys.foreach(p.updateDynamic("handleHomeEndKeys")(_))
    id.foreach(p.updateDynamic("id")(_))
    includeInputInList.foreach(p.updateDynamic("includeInputInList")(_))
    inputValue.foreach(p.updateDynamic("inputValue")(_))
    isOptionEqualToValue.foreach(p.updateDynamic("isOptionEqualToValue")(_))
    limitTags.foreach(p.updateDynamic("limitTags")(_))
    listboxComponent.foreach(p.updateDynamic("ListboxComponent")(_))
    listboxProps.foreach(p.updateDynamic("ListboxProps")(_))
    loading.foreach(p.updateDynamic("loading")(_))
    loadingText.foreach(p.updateDynamic("loadingText")(_))
    multiple.foreach(p.updateDynamic("multiple")(_))
    noOptionsText.foreach(p.updateDynamic("noOptionsText")(_))
    onChange.foreach(p.updateDynamic("onChange")(_))
    onClose.foreach(p.updateDynamic("onClose")(_))
    onHighlightChange.foreach(p.updateDynamic("onHighlightChange")(_))
    onInputChange.foreach(p.updateDynamic("onInputChange")(_))
    onOpen.foreach(p.updateDynamic("onOpen")(_))
    open.foreach(p.updateDynamic("open")(_))
    openOnFocus.foreach(p.updateDynamic("openOnFocus")(_))
    openText.foreach(p.updateDynamic("openText")(_))
    paperComponent.foreach(p.updateDynamic("PaperComponent")(_))
    popperComponent.foreach(p.updateDynamic("PopperComponent")(_))
    popupIcon.foreach(p.updateDynamic("popupIcon")(_))
    renderGroup.foreach(p.updateDynamic("renderGroup")(_))
    renderOption.foreach(p.updateDynamic("renderOption")(_))
    renderTags.foreach(p.updateDynamic("renderTags")(_))
    selectOnFocus.foreach(p.updateDynamic("selectOnFocus")(_))
    size.foreach(p.updateDynamic("size")(_))
    value.foreach(p.updateDynamic("value")(_))

    p
  }

}

object MuiAutocomplete extends ComponentNoChildrenFactory[AutocompleteProps] {
  @js.native @JSImport(
    "@mui/material/Autocomplete",
    JSImport.Default
  ) private object MAutocomplete extends js.Any

  protected val f =
    JsComponent[AutocompleteProps, Children.None, Null](
      MAutocomplete
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param options Array of options.  These are the values for the autocomplete.
    * @param autoComplete If true, the portion of the selected suggestion that has not been typed by the user,
    *                     known as the completion string, appears inline after the input cursor in the textbox.
    *                     The inline completion string is visually highlighted and has a selected state.
    *               Default: false
    * @param autoHighlight If true, the first option is automatically highlighted.
    *               Default: false
    * @param autoSelect If true, the selected option becomes the value of the input when the Autocomplete loses
    *                   focus unless the user chooses a different option or changes the character string in the input.
    *               Default: false
    * @param blurOnSelect Control if the input should be blurred when an option is selected:
    *                     - false the input is not blurred.  Default
    *                     - true the input is always blurred.
    *                     - 'touch' the input is blurred after a touch event.
    *                     - 'mouse' the input is blurred after a mouse event.
    * @param clearIcon  The icon to display in place of the default clear icon.  A Node.
    *                   Default: <ClearIcon fontSize="small" />
    * @param clearOnBlur  If true, the input's text is cleared on blur if no value is selected.
    *                     Set to true if you want to help the user enter a new value. Set to false if you want to help the user resume his search.
    *                     Default: !freeSolo
    * @param clearOnEscape If true, clear all values when the user presses escape and the popup is closed.
    *                      Default: false
    * @param clearText  Override the default text for the clear icon button.
    *                   For localization purposes, you can use the provided translations.
    *                   Default: 'Clear'
    * @param closeText  Override the default text for the close popup icon button.
    *                   For localization purposes, you can use the provided translations.
    *                   Default: 'Close'
    * @param defaultValue The default value. Use when the component is not controlled.
    * @param disableClearable If true, the input can't be cleared.  Default false.
    * @param disableCloseOnSelect If true, the popup won't close when a value is selected.  Default false.
    * @param disabled If true, the component is disabled.  Default false.
    * @param disabledItemsFocusable If true, will allow focus on disabled items.  Default false.
    * @param disableListWrap If true, the list box in the popup will not wrap focus.  Default false.
    * @param disablePortal If true, the Popper content will be under the DOM hierarchy of the parent component.  Default false.
    * @param filterOptions A filter function that determines the options that are eligible.
    *                      Signature:
    *                        function(options: Array<T>, state: object) => Array<T>
    *                      options: The options to render.
    *                      state: The state of the component.
    * @param filterSelectedOptions If true, hide the selected options from the list box.  Default false
    * @param forcePopupIcon Force the visibility display of the popup icon.
    *                       Values: 'auto'|bool
    *                       Default: 'auto'
    * @param freeSolo If true, the Autocomplete is free solo, meaning that the user input is not bound to provided options.  Default false
    * @param fullWidth If true, the input will take up the full width of its container.  Default false
    * @param getLimitTagsText The label to display when the tags are truncated (limitTags).
    *                         Signature:
    *                           function(more: number) => ReactNode
    *                             more: The number of truncated tags.
    *                         Default: (more) => `+${more}`
    * @param getOptionDisabled Used to determine the disabled state for a given option.
    *                          Signature:
    *                            function(option: T) => boolean
    *                            option: The option to test.
    * @param getOptionLabel Used to determine the string value for a given option.
    *                       It's used to fill the input (and the list box options if renderOption is not provided).
    *                       Signature:
    *                         function(option: T) => string
    *                       Default: (option) => option.label ?? option
    * @param groupBy If provided, the options will be grouped under the returned string.
    *                The groupBy value is also used as the text for group headings when renderGroup is not provided.
    *                Signature:
    *                  function(options: T) => string
    *                  options: The options to group.
    * @param handleHomeEndKeys If true, the component handles the "Home" and "End" keys when the popup is open.
    *                          It should move focus to the first option and last option, respectively.
    *                          Default: !freeSolo
    * @param id This prop is used to help implement the accessibility logic.
    *           If you don't provide an id it will fall back to a randomly generated one.
    * @param includeInputInList If true, the highlight can move to the input.
    *                           Default: false
    * @param inputValue The input value.
    * @param isOptionEqualToValue Used to determine if the option represents the given value.
    *                             Uses strict equality by default. Both arguments need to be handled,
    *                             an option can only match with one value.
    *                             Signature:
    *                             function(option: T, value: T) => boolean
    *                               option: The option to test.
    *                               value: The value to test against.
    * @param limitTags The maximum number of tags that will be visible when not focused. Set -1 to disable the limit.
    *                  Default: -1
    * @param ListboxComponent The component used to render the listbox.
    *                         Default: 'ul'
    * @param ListboxProps Props applied to the Listbox element.
    * @param loading If true, the component is in a loading state. This shows the loadingText in place of
    *                suggestions (only if there are no suggestions to show, e.g. options are empty).
    *                Default: false
    * @param loadingText Text to display when in a loading state.
    *                    For localization purposes, you can use the provided translations.
    *                    Default: 'Loading…'
    * @param multiple If true, value must be an array and the menu will support multiple selections.
    *                 Default: false
    * @param noOptionsText Text to display when there are no options.
    *                      For localization purposes, you can use the provided translations.
    *                      Default: 'No options'
    * @param onChange Callback fired when the value changes.
    *                 Signature:
    *                   function(event: React.SyntheticEvent, value: T | Array<T>, reason: string, details?: string) => void
    *                     event: The event source of the callback.
    *                     value: The new value of the component.
    *                     reason: One of "createOption", "selectOption", "removeOption", "blur" or "clear".
    * @param onClose Callback fired when the popup requests to be closed. Use in controlled mode (see open).
    *                Signature:
    *                  function(event: React.SyntheticEvent, reason: string) => void
    *                    event: The event source of the callback.
    *                    reason: Can be: "toggleInput", "escape", "selectOption", "removeOption", "blur".
    * @param onHighlightChange Callback fired when the highlight option changes.
    *                          Signature:
    *                          function(event: React.SyntheticEvent, option: T, reason: string) => void
    *                            event: The event source of the callback.
    *                            option: The highlighted option.
    *                            reason: Can be: "keyboard", "auto", "mouse".
    * @param onInputChange Callback fired when the input value changes.
    *                      Signature:
    *                        function(event: React.SyntheticEvent, value: string, reason: string) => void
    *                          event: The event source of the callback.
    *                          value: The new value of the text input.
    *                          reason: Can be: "input" (user input), "reset" (programmatic change), "clear".
    * @param onOpen Callback fired when the popup requests to be opened. Use in controlled mode (see open).
    *               Signature:
    *                 function(event: React.SyntheticEvent) => void
    *                   event: The event source of the callback.
    * @param open If true, the component is shown.
    *             Default: false
    * @param openOnFocus If true, the popup will open on input focus.
    *                    Default: false
    * @param openText Override the default text for the open popup icon button.
    *                 For localization purposes, you can use the provided translations.
    *                 Default: 'Open'
    * @param PaperComponent The component used to render the body of the popup.
    *                       Default: Paper
    * @param PopperComponent The component used to position the popup.
    *                        Default: Popper
    * @param popupIcon The icon to display in place of the default popup icon.
    *                  Default: <ArrowDropDownIcon />
    * @param renderGroup Render the group.
    *                    Signature:
    *                      function(params: AutocompleteRenderGroupParams) => ReactNode
    *                        params: The group to render.
    * @param renderOption Render the option, use getOptionLabel by default.
    *                     Signature:
    *                       function(props: object, option: T, state: object) => ReactNode
    *                         props: The props to apply on the li element.
    *                         option: The option to render.
    *                         state: The state of the component.
    * @param renderTags Render the selected value.
    *                   Signature:
    *                     function(value: Array<T>, getTagProps: function) => ReactNode
    *                       value: The value provided to the component.
    *                       getTagProps: A tag props getter.
    * @param selectOnFocus If true, the input's text is selected on focus. It helps the user clear the selected value.
    *                      Default: !freeSolo
    * @param size The size of the component.
    *             Default: 'small'
    * @param value The value of the autocomplete.
    *              The value must have reference equality with the option in order to be selected.
    *              You can customize the equality behavior with the isOptionEqualToValue prop.
    *
    *
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      options: js.UndefOr[js.Array[js.Any]] = js.undefined,
      autoComplete: js.UndefOr[Boolean] = js.undefined,
      autoHighlight: js.UndefOr[Boolean] = js.undefined,
      autoSelect: js.UndefOr[Boolean] = js.undefined,
      blurOnSelect: js.UndefOr[js.Any] = js.undefined,
      clearIcon: js.UndefOr[js.Object /* Node */] = js.undefined,
      clearOnBlur: js.UndefOr[Boolean] = js.undefined,
      clearOnEscape: js.UndefOr[Boolean] = js.undefined,
      clearText: js.UndefOr[String] = js.undefined,
      closeText: js.UndefOr[String] = js.undefined,
      defaultValue: js.UndefOr[js.Any] = js.undefined,
      disableClearable: js.UndefOr[Boolean] = js.undefined,
      disableCloseOnSelect: js.UndefOr[Boolean] = js.undefined,
      disabled: js.UndefOr[Boolean] = js.undefined,
      disabledItemsFocusable: js.UndefOr[Boolean] = js.undefined,
      disableListWrap: js.UndefOr[Boolean] = js.undefined,
      disablePortal: js.UndefOr[Boolean] = js.undefined,
      filterOptions: js.UndefOr[js.Function2[js.Array[js.Any], js.Object, js.Array[js.Any]]] = js.undefined,
      filterSelectedOptions: js.UndefOr[Boolean] = js.undefined,
      forcePopupIcon: js.UndefOr[js.Any] = js.undefined,
      freeSolo: js.UndefOr[Boolean] = js.undefined,
      fullWidth: js.UndefOr[Boolean] = js.undefined,
      getLimitTagsText: js.UndefOr[js.Function1[js.Any /* number */, js.Object /* ReactNode */]] = js.undefined,
      getOptionDisabled: js.UndefOr[js.Function1[js.Any, Boolean]] = js.undefined,
      getOptionLabel: js.UndefOr[js.Function1[js.Any, String]] = js.undefined,
      groupBy: js.UndefOr[js.Function1[js.Any, String]] = js.undefined,
      handleHomeEndKeys: js.UndefOr[Boolean] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      includeInputInList: js.UndefOr[Boolean] = js.undefined,
      inputValue: js.UndefOr[String] = js.undefined,
      isOptionEqualToValue: js.UndefOr[Boolean] = js.undefined,
      limitTags: js.UndefOr[js.Any /* number */] = js.undefined,
      listboxComponent: js.UndefOr[String] = js.undefined,
      listboxProps: js.UndefOr[js.Object] = js.undefined,
      loading: js.UndefOr[Boolean] = js.undefined,
      loadingText: js.UndefOr[js.Object /* Node */] = js.undefined,
      multiple: js.UndefOr[Boolean] = js.undefined,
      noOptionsText: js.UndefOr[js.Object /* Node */] = js.undefined,
      onChange: js.UndefOr[js.Function4[SyntheticEvent[Node], js.Any, String, String, Unit]] = js.undefined,
      onClose: js.UndefOr[js.Function2[SyntheticEvent[Node], String, Unit]] = js.undefined,
      onHighlightChange: js.UndefOr[js.Function3[SyntheticEvent[Node], js.Any, String, Unit]] = js.undefined,
      onInputChange: js.UndefOr[js.Function3[SyntheticEvent[Node], String, String, Unit]] = js.undefined,
      onOpen: js.UndefOr[js.Function1[SyntheticEvent[Node], Unit]] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      openOnFocus: js.UndefOr[Boolean] = js.undefined,
      openText: js.UndefOr[String] = js.undefined,
      paperComponent: js.UndefOr[js.Object /* elementType */] = js.undefined,
      popperComponent: js.UndefOr[js.Object /* elementType */] = js.undefined,
      popupIcon: js.UndefOr[js.Object /* Node */] = js.undefined,
      renderGroup: js.UndefOr[js.Function1[js.Object /* AutocompleteRenderGroupParams */, js.Object /* ReactNode */]] = js.undefined,
      renderOption: js.UndefOr[js.Function3[js.Object, js.Any, js.Object, js.Object /* ReactNode */]] = js.undefined,
      renderTags: js.UndefOr[js.Function2[js.Array[js.Any], js.Any, js.Object /* ReactNode */]] = js.undefined,
      selectOnFocus: js.UndefOr[Boolean] = js.undefined,
      size: js.UndefOr[String] = js.undefined,
      value: js.UndefOr[js.Any] = js.undefined,

      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: AutocompleteProps = AutocompleteProps(
      options = options,
      autoComplete = autoComplete,
      autoHighlight = autoHighlight,
      autoSelect = autoSelect,
      blurOnSelect = blurOnSelect,
      clearIcon = clearIcon,
      clearOnBlur = clearOnBlur,
      clearOnEscape = clearOnEscape,
      clearText = clearText,
      closeText = closeText,
      defaultValue = defaultValue,
      disableClearable = disableClearable,
      disableCloseOnSelect = disableCloseOnSelect,
      disabled = disabled,
      disabledItemsFocusable = disabledItemsFocusable,
      disableListWrap = disableListWrap,
      disablePortal = disablePortal,
      filterOptions = filterOptions,
      filterSelectedOptions = filterSelectedOptions,
      forcePopupIcon = forcePopupIcon,
      freeSolo = freeSolo,
      fullWidth = fullWidth,
      getLimitTagsText = getLimitTagsText,
      getOptionDisabled = getOptionDisabled,
      getOptionLabel = getOptionLabel,
      groupBy = groupBy,
      handleHomeEndKeys = handleHomeEndKeys,
      id = id,
      includeInputInList = includeInputInList,
      inputValue = inputValue,
      isOptionEqualToValue = isOptionEqualToValue,
      limitTags = limitTags,
      listboxComponent = listboxComponent,
      listboxProps = listboxProps,
      loading = loading,
      loadingText = loadingText,
      multiple = multiple,
      noOptionsText = noOptionsText,
      onChange = onChange,
      onClose = onClose,
      onHighlightChange = onHighlightChange,
      onInputChange = onInputChange,
      onOpen = onOpen,
      open = open,
      openOnFocus = openOnFocus,
      openText = openText,
      paperComponent = paperComponent,
      popperComponent = popperComponent,
      popupIcon = popupIcon,
      renderGroup = renderGroup,
      renderOption = renderOption,
      renderTags = renderTags,
      selectOnFocus = selectOnFocus,
      size = size,
      value = value,

      classes = classes,
      className = className,
      additionalProps = additionalProps,
    )
    super.create(p)
  }
}
