package com.github.thebridsk.bridge.client.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.clientcommon.logging.LogLifecycleToServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Level
import com.github.thebridsk.bridge.client.pages.hand.PageHandInternal.PageHandNextInput

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
                                    props.contract.scorer match {
                                      case Some(Left(score)) /* Rubber */ =>
                                        val c = props.contract
                                        TagMod(
                                          <.div("Score: "+score.totalScore(c.north,c.south,c.east,c.west)),
                                          <.div(score.contractAndResultAsString, handStyles.contractAndResult ),
                                          <.div(score.explain)
                                        )
                                      case Some(Right(score)) /* Duplicate */ =>
                                        val c = props.contract
                                        val ts = c.scoringSystem match {
                                          case _ : Chicago => score.totalScoreNoPos(c.north,c.south,c.east,c.west)
                                          case _ => score.totalScore(c.north,c.south,c.east,c.west)
                                        }
                                        TagMod(
                                          <.div("Score: "+ts ),
//                                          <.div(score.contractAndResultAsString, handStyles.contractAndResult ),
                                          <.div(score.explain )
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
                                        TagMod(
                                          <.div(handStyles.required, "Missing required information" ),
                                          <.div(handStyles.required, msg )
                                        )
                                    }
                                )
                            })
//                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
                            .build
}
