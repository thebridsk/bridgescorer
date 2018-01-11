package com.example.pages.hand

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.data.bridge.ContractTricks
import com.example.data.bridge._
import japgolly.scalajs.react.extra.LogLifecycle
import com.example.logging.LogLifecycleToServer
import utils.logging.Logger
import utils.logging.Level
import com.example.pages.hand.PageHandInternal.PageHandNextInput

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
object SectionScore {

  implicit val loggerForReactComponents = Logger("bridge.SectionScore")
  implicit val defaultTraceLevelForReactComponents = Level.FINER

  case class Props( contract: Contract, nextInput: PageHandNextInput.Value )

  def apply( contract: Contract, nextInput: PageHandNextInput.Value ) = component(Props(contract,nextInput))

  private val component = ScalaComponent.builder[Props]("SectionScore")
                            .render_P( props => {
                                import HandStyles._
                                <.div(
                                    handStyles.sectionScore,
                                    <.table( ^.width:="100%",
                                        <.tbody(
                                            props.contract.scorer match {
                                              case Some(Left(score)) /* Rubber */ =>
                                                val c = props.contract
                                                <.tr(
                                                  <.td(^.textAlign := "left", "Score: "+score.totalScore(c.north,c.south,c.east,c.west)),
                                                  <.td(^.textAlign := "center", score.contractAndResultAsString ),
                                                  <.td(^.textAlign := "right", score.explain())
                                                )
                                              case Some(Right(score)) /* Duplicate */ =>
                                                val c = props.contract
                                                val ts = c.scoringSystem match {
                                                  case Chicago => score.totalScoreNoPos(c.north,c.south,c.east,c.west)
                                                  case _ => score.totalScore(c.north,c.south,c.east,c.west)
                                                }
                                                <.tr(
                                                  <.td(^.textAlign := "left", "Score: "+ts ),
                                                  <.td(^.textAlign := "center", score.contractAndResultAsString ),
                                                  <.td(^.textAlign := "right", score.explain() )
                                                )
                                              case None =>
                                                import PageHandNextInput._
                                                val msg = (props.nextInput match {
                                                  case InputContractTricks => Some("contract tricks")
                                                  case InputContractSuit => Some("contract suit")
                                                  case InputContractDoubled => Some("contract doubled")
                                                  case InputContractBy => Some("declarer")
                                                  case InputHonors => Some("honors")
                                                  case InputHonorsPlayer => Some("honors player")
                                                  case InputResultMadeOrDown => Some("made/down")
                                                  case InputResultTricks => Some("tricks")
                                                  case InputAll => None
                                                }) match {
                                                  case Some(s) => s"Enter $s"
                                                  case None => "Unknown missing information"
                                                }
                                                <.tr(
                                                  <.td(^.textAlign := "left", handStyles.required, "Missing required information" ),
                                                  <.td(^.textAlign := "center" ),
                                                  <.td(^.textAlign := "right", handStyles.required, msg )
                                                )
                                            }
                                        )
                                    )
                                )
                            })
                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
                            .build
}
