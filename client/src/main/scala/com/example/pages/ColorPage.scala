package com.example.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.react.ColorBar
import com.example.react.Utils._
import utils.logging.Logger

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ColorPage( ColorPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ColorPage {
  import ColorPageInternal._

  case class Props()

  def apply() = component(Props())

}

object ColorPageInternal {
  import ColorPage._

  val log = Logger("bridge.ColorPage")

  /*
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  /**
   * @param n the number of boxes
   * @param minLightness the minimum l from hsl, 0 to 1
   * @param maxLightness the maximum l from hsl, 0 to 1
   */
  case class State( n: String = "10", minLightness: String = "0.0", maxLightness: String = "100.0", huestep: String = "30") {

    def withN( v: String ) = copy(n=v)
    def withHueStep( v: String ) = copy(huestep=v)
    def withMinLightness( v: String ) = copy(minLightness=v)
    def withMaxLightness( v: String ) = copy(maxLightness=v)
  }

  def parseInt( s: String, default: Int ) = {
    try {
      s.toInt
    } catch {
      case x: Exception =>
        default
    }
  }

  def parseDouble( s: String, default: Double ) = {
    try {
      s.toDouble
    } catch {
      case x: Exception =>
        default
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setHueStep( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withHueStep(s) }
    }

    def setN( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withN(s) }
    }

    def setMinLightness( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withMinLightness(s) }
    }

    def setMaxLightness( e: ReactEventFromInput ) = e.inputText { s =>
      scope.modState { state => state.withMaxLightness(s) }
    }

    def render( props: Props, state: State ) = {
      <.div(
          <.ul(
            <.li(
              <.label(
                "Hue step",
                <.input( ^.`type`:="number",
                         ^.name:="Hue",
                         ^.onChange ==> setHueStep,
                         ^.value := state.huestep
                )
              )
            ),
            <.li(
              <.label(
                "N",
                <.input( ^.`type`:="number",
                         ^.name:="N",
                         ^.onChange ==> setN,
                         ^.value := state.n
                )
              )
            ),
            <.li(
              <.label(
                "Min Lightness",
                <.input( ^.`type`:="number",
                         ^.name:="minLightness",
                         ^.onChange ==> setMinLightness,
                         ^.value := state.minLightness
                )
              )
            ),
            <.li(
              <.label(
                "Max Lightness",
                <.input( ^.`type`:="number",
                         ^.name:="minLightness",
                         ^.onChange ==> setMaxLightness,
                         ^.value := state.maxLightness
                )
              )
            )
          ),
          <.div(
            <.table(
              <.thead(
                <.tr(
                  <.th("Hue"),
                  <.th("Color bar")
                )
              ),
              <.tbody(
                (0 to 360 by parseInt(state.huestep,30)).map { hue =>
                  val colors = ColorBar.colorsInclusive(
                                                          hue,
                                                          parseDouble(state.minLightness,0.0),
                                                          parseInt(state.n,10),
                                                          false,
                                                          parseDouble(state.maxLightness,100.0)
                                                      )

                  <.tr(
                    <.td(hue.toString()),
                    <.td(
                      ColorBar.simple(
                          colors,
                          Some( colors.map( c => s"${c}" ).toList )
                      )
                    )
                  )
                }.toTagMod
              )
            )
          )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ColorPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

