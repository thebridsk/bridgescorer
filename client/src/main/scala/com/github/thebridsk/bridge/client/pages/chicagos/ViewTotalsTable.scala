package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.SummaryView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.BaseStyles

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageChicagoSkeleton( PageChicagoSkeleton.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewTotalsTable {
  import ViewTotalsTableInternal._

  case class Props( chicagoScoring: ChicagoScoring, showRound: Option[Int], page: SummaryView, routerCtl: BridgeRouter[ChicagoPage] )

  def apply( chicagoScoring: ChicagoScoring, showRound: Option[Int], page: SummaryView, routerCtl: BridgeRouter[ChicagoPage] ) =
    component( Props( chicagoScoring, showRound, page, routerCtl ) )

}

object ViewTotalsTableInternal {
  import ViewTotalsTable._

  val logger = Logger("bridge.ViewTotalsTable")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  val playerName = ScalaComponent.builder[(String,String)]("PlayerName")
    .render_P( props => {
      val (name,key) = props
      <.th( ^.id:=key, ^.textAlign := "center", name )
    }).build

  val playerScore = ScalaComponent.builder[(Int,String)]("PlayerScore")
    .render_P( props => {
      val (score,key) = props
      val s = if (score < 0) "X" else score.toString()
      <.td( ^.id:=key, ^.textAlign := "right", s )
    }).build

  val summaryRoundRow = ScalaComponent.builder[(Props,Int,List[Int],List[Int])]("SummaryRoundRow")
    .render_P( rowprops => {
      val (props,round,scores,order) = rowprops
      <.tr(
          <.td(
            AppButton("Round"+(round+1).toString(),
                      (round+1).toString(),
                      BaseStyles.highlight(selected=props.showRound.map(_ == round).getOrElse(false)),
                      props.routerCtl.setOnClick(props.page.toRoundView(round)))
          ),
          (0 until scores.length).map { j =>
            val i = order(j)
            val player="Round"+round+"Player"+(i+1)
            val p = scores(i)
            playerScore.withKey(player)((p,player))
          }.toTagMod
      )
    }).build

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      val scoring = props.chicagoScoring
      logger.finer("ViewRounds.totalsTable "+scoring)
      val players = scoring.players
      logger.finer( "Players: "+players.mkString(", "))
      val byRounds = scoring.byRounds
      val totals = scoring.totals
      val order = players.zipWithIndex.sortWith( (l,r) => l._1 < r._1).map(_._2)
      <.table(
          <.thead(
            <.tr(
                <.th( scoring.chicago.id ),
                <.th( ^.colSpan:=scoring.players.size, "Totals")
                ),
            <.tr(
              <.th( "Round"),
              (0 until players.length).map { j =>
                val i = order(j)
                val player="Player"+(i+1)
                val p = players(i)
                playerName.withKey(player)((p,player))
              }.toTagMod
            )
          ),
          <.tfoot(
              <.tr(
                <.td( "Totals"),
                (0 until players.length).map { j =>
                  val i = order(j)
                  val player="Total"+(i+1)
                  val p = totals(i)
                  playerScore.withKey(player)((p,player))
                }.toTagMod
              )
          ),
          <.tbody(
              (0 until byRounds.length).map { i =>
                val key="Round"+i
                val v = byRounds(i)
                summaryRoundRow.withKey(key)((props,i,v,order))
              }.toTagMod,
          )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ViewTotalsTable")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

