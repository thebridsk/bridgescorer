package com.example.pages.chicagos

import com.example.data.chicago.ChicagoScoring
import utils.logging.Logger
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.pages.chicagos.ChicagoRouter.SummaryView
import com.example.react.AppButton

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

  case class Props( chicagoScoring: ChicagoScoring, page: SummaryView, routerCtl: RouterCtl[ChicagoPage] )

  def apply( chicagoScoring: ChicagoScoring, page: SummaryView, routerCtl: RouterCtl[ChicagoPage] ) =
    component( Props( chicagoScoring, page, routerCtl ) )

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
          <.tbody(
              (0 until byRounds.length).map { i =>
                val key="Round"+i
                val v = byRounds(i)
                summaryRoundRow.withKey(key)((props,i,v,order))
              }.toTagMod,
              <.tr( <.td( ^.colSpan := (scoring.players.size+1), " ")),
              <.tr(
                <.td( "Totals"),
                (0 until players.length).map { j =>
                  val i = order(j)
                  val player="Total"+(i+1)
                  val p = totals(i)
                  playerScore.withKey(player)((p,player))
                }.toTagMod
              )
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

