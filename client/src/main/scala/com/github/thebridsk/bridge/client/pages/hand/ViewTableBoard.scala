package com.github.thebridsk.bridge.client.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * Component( Component.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewTableBoard {

  case class Props( table: Int, board: Int, dealer: PlayerPosition, dealerName: String )

  def apply( table: Int, board: Int, dealer: PlayerPosition, dealerName: String ) =
    component(Props(table,board,dealer,dealerName))

  private val component = ScalaComponent.builder[Props]("ViewTableBoard")
                            .render_P(props => {
                                import HandStyles._
                                <.div( handStyles.viewTableBoard,
                                    <.table(
                                        <.tbody(
                                          <.tr(
                                              <.td( ^.colSpan :=1, "Table "+props.table )
                                          ),
                                          <.tr(
                                            <.td( ^.colSpan :=1, "Board "+props.board )
                                          ),
                                          <.tr(
                                            <.td( ^.colSpan :=1,
                                                  "Dealer "+props.dealer.forDisplay,
                                                  <.br,
                                                  <.span( ^.dangerouslySetInnerHtml:="&nbsp;&nbsp;"),
                                                  <.span( ^.id:="Dealer", props.dealerName ) )
                                          )
                                        )
                                     )
                                )

                            })
                            .build
}
