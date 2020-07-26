package com.github.thebridsk.bridge.client.pages.duplicate

import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.utilities.logging.Logger

import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.clientcommon.react.DateUtils


/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewPlayerMatchResult( routerCtl: BridgeRouter[DuplicatePage], score: MatchDuplicateScore )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerMatchResult {
  case class Props( score: List[MatchDuplicateScore.Place], mdr: Option[MatchDuplicateResult], iws: Int, nws: Int, useIMPs: Boolean = false )

  def apply( score: List[MatchDuplicateScore.Place] ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    ViewPlayerMatchResultInternal.component(Props(score,None,0,0,false))

  def apply( score: List[MatchDuplicateScore.Place], useIMPs: Boolean ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    ViewPlayerMatchResultInternal.component(Props(score,None,0,0,useIMPs))

  def apply( score: List[MatchDuplicateScore.Place], mdr: MatchDuplicateResult, iws: Int, nws: Int, useIMPs: Boolean = false ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    ViewPlayerMatchResultInternal.component(Props(score,Some(mdr),iws,nws,useIMPs))

}

object ViewPlayerMatchResultInternal {
  import ViewPlayerMatchResult._

  val logger: Logger = Logger("bridge.ViewPlayerMatchResult")

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
    import DuplicateStyles._
    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React
      val places = props.score.sortWith( (l,r) => l.place < r.place)
      def teamColumn( teams: List[Team] ) = {
        var count = 0
        teams.sortWith((t1,t2)=> t1.id < t2.id).map { team =>
          count += 1
          <.span( count!=1 ?= <.br(), team.id.toNumber+" "+team.player1+" "+team.player2 )
        }.toTagMod
      }
      <.div(
        dupStyles.divPlayerPosition,
        <.table( ^.id := "scoreboardplayers",
          dupStyles.tablePlayerPosition,
          <.thead(
            props.mdr.whenDefined { mdr =>
              TagMod(
                <.tr(
                  <.th( mdr.id.id),
                  <.th( DateUtils.formatDay(mdr.played), ^.colSpan:=2)
                ),
                mdr.comment.whenDefined { c =>
                  <.tr(
                    <.th( c, ^.colSpan:=3)
                  )
                },
                props.nws != 1 || mdr.notfinished.getOrElse(false) ?= <.tr(
                  <.th(
                    ^.colSpan:=3,
                    props.nws != 1?=s"${props.iws}/${props.nws} ",
                    s"${mdr.notfinished.filter(b=>b).map(b=>"incomplete").getOrElse("")}"
                  )
                )
              )
            },
            <.tr(
              <.th( "Place" ),
              <.th( if (props.useIMPs) "IMP" else "Point" ),
              <.th( "Players" )
            )
          ),
          <.tbody(
            places.map { place =>
              <.tr(
                <.td( place.place.toString ),
                <.td( (if (props.useIMPs) f"${place.score}%.1f" else Utils.toPointsString(place.score) ) ),
                <.td( teamColumn(place.teams) )
              )
            }.toTagMod
          )
        )
      )
    }

  }

  private[duplicate]
  val component = ScalaComponent.builder[Props]("ViewPlayerMatchResults")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

