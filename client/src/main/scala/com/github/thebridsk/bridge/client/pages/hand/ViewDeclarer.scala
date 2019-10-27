package com.github.thebridsk.bridge.client.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.client.pages.hand.PageHandInternal.PageHandNextInput
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.InputMethod
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.maneuvers.TableManeuvers
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.pages.Pixels

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
object ViewDeclarer {
  import ViewDeclarerInternal._

  type CallbackPlayer = (PlayerPosition)=>Callback

  case class Props( current: Option[PlayerPosition], north: String, south: String, east: String, west: String,
                    callback: CallbackPlayer, teamNS: Option[Id.Team], teamEW: Option[Id.Team],
                    nextInput: PageHandNextInput.Value,
                    visible: Boolean,
                    nsVul: Boolean,
                    ewVul: Boolean )

  def apply( current: Option[PlayerPosition], north: String, south: String, east: String, west: String,
             callback: CallbackPlayer, teamNS: Option[Id.Team], teamEW: Option[Id.Team],
             nextInput: PageHandNextInput.Value,
             visible: Boolean,
             nsVul: Boolean,
             ewVul: Boolean ) =
    component(Props(current,north,south,east,west,callback,teamNS,teamEW,nextInput,visible,nsVul,ewVul))

}

object ViewDeclarerInternal {
  import ViewDeclarer._

  val logger = Logger("bridge.ViewDeclarer")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      import HandStyles._
      val playingDuplicate = props.teamEW.isDefined || props.teamNS.isDefined
      val (missingRequired,missingNotNext) = {
        val mr = props.current.isEmpty
        if (mr) {
          if (ComponentInputStyleButton.inputMethod == InputMethod.Guide) {
            val ni = props.nextInput == PageHandNextInput.InputContractBy
            (ni,!ni)
          } else {
            (true,false)
          }
        } else {
          (false,false)
        }
      }

      def isSelected( d: PlayerPosition ) = props.current.isDefined && props.current.get == d
      def getTeam( team: Option[Id.Team] ) = {
        team match {
          case Some(t) => <.span( " (", Id.teamIdToTeamNumber(t), ")")
          case None => EmptyVdom
        }
      }
      def getButtonText(text: String, pos: PlayerPosition, name: String, isvul: Boolean, teamId: Option[Id.Team] ) = {
        List(
          name,
          teamId.map( t => s" (${Id.teamIdToTeamNumber(t)})" ).getOrElse("") ,
          " ",
          if (isvul) "Vul"
          else "vul",
        )::: ( if (playingDuplicate) List(" "+pos.name) else List() )
      }
      val maxPlayerLen = {
        val texts =
          getButtonText("North", North, props.north, props.nsVul, props.teamNS)::
          getButtonText("South", South, props.south, props.nsVul, props.teamNS)::
          getButtonText("East", East, props.east, props.ewVul, props.teamEW)::
          getButtonText("West", West, props.west, props.ewVul, props.teamEW)::
          Nil
        val borderRadius = Properties.defaultHandButtonBorderRadius
        val vulpadding = Properties.defaultHandVulPaddingBorder
        Math.max( 5, Pixels.maxLength( texts.map(_.mkString("")): _*) ) + vulpadding + borderRadius + borderRadius
      }
      val bwidth = {
        s"${maxPlayerLen}px"
      }
      def getButtonPos(text: String, pos: PlayerPosition, name: String, isvul: Boolean, teamId: Option[Id.Team] ) =
        <.button(
                  ^.disabled := !props.visible,
                  ^.`type` := "button",
                  ^.width := bwidth,
                  ^.onClick --> props.callback(pos),
                  HandStyles.highlight(
                      selected = isSelected(pos),
                      required = missingRequired,
                      requiredNotNext = missingNotNext
                  ),
                  ^.id:="Dec"+pos.pos,
                  <.span(
                    ^.id:=text,
                    name,
                    getTeam(teamId),
                    " ",
                    if (isvul) <.span( handStyles.vulnerable, "Vul" )
                    else <.span( handStyles.notVulnerable, "vul" ),
                    playingDuplicate ?= <.span( " ", pos.name )
                  )
                )

      val header = "Declarer"
      val maxLen = Math.max(Pixels.maxLength(header), maxPlayerLen/2)
      val twidth: TagMod = ^.width := (maxLen).toString()+"px"
      val t2width: TagMod = ^.width := (maxLen*2).toString()+"px"
      val tablewidth: TagMod = ^.width := (maxLen*4).toString()+"px"

      def getButton( loc: PlayerPosition ) = {
        loc match {
          case North => getButtonPos("North", North, props.north, props.nsVul, props.teamNS)
          case South => getButtonPos("South", South, props.south, props.nsVul, props.teamNS)
          case East => getButtonPos("East", East, props.east, props.ewVul, props.teamEW)
          case West => getButtonPos("West", West, props.west, props.ewVul, props.teamEW)
          case PositionUndefined => TagMod()
        }
      }

      val maneuvers = TableManeuvers( props.north, props.south, props.east, props.west )
      val scorekeeper = PageHand.scorekeeper

      <.div(
        handStyles.viewDeclarer, // !props.visible ?= handStyles.notVisible,
        <.div(
          // row 1
          <.div( twidth, <.b( playingDuplicate ?= handStyles.titleDeclarerDup, "Declarer" ) ),
          <.div( t2width, getButton( maneuvers.partnerOfPosition(scorekeeper) ) ),
          <.div( twidth )
        ),
        <.div(
          // row 2
          <.div( t2width, getButton( maneuvers.leftOfPosition(scorekeeper) ) ),
          <.div( t2width, getButton( maneuvers.rightOfPosition(scorekeeper) ) )
        ),
        <.div(
          // row 3
          <.div( twidth ),
          <.div( t2width, getButton( scorekeeper ) ),
          <.div( twidth )
        )
      )

    }
  }

  val component = ScalaComponent.builder[Props]("ViewDeclarer")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}
