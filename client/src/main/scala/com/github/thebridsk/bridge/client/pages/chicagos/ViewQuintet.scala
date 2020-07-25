package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.SummaryView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.client.routes.BridgeRouter

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
object ViewQuintet {
  import ViewQuintetInternal._

  case class Props( chicagoScoring: ChicagoScoring, page: SummaryView, routerCtl: BridgeRouter[ChicagoPage] )

  def apply( chicagoScoring: ChicagoScoring, page: SummaryView, routerCtl: BridgeRouter[ChicagoPage] ) =
    component( Props( chicagoScoring, page, routerCtl ) )

}

object ViewQuintetInternal {
  import ViewQuintet._

  val logger = Logger("bridge.ViewQuintet")

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
      val r = props.chicagoScoring.rounds(round)

      def getPlayer(pos: PlayerPosition) = pos match {
        case North => r.round.north
        case South => r.round.south
        case East => r.round.east
        case West => r.round.west
        case _ =>
          logger.severe("Unknown position: "+pos)
          r.round.north
      }

      val roundTarget = {
        if (r.hands.isEmpty && round!=0) props.page.toNamesView(round)
        else props.page.toHandView(round, 0)
      }

      if (r.hands.isEmpty) {
        <.tr(
            <.td(
              AppButton("Round"+(round+1).toString(),
                        (round+1).toString(),
                        props.routerCtl.setOnClick(roundTarget))
            ),
            <.td( getPlayer(r.dealerForHand(1)) ),
            <.td( "" ),
            <.td( "" ),
            <.td( "" ),
            <.td( "" ),
            (0 until scores.length).map { j =>
              val i = order(j)
              val player="Round"+round+"Player"+(i+1)
              val p = scores(i)
              playerScore.withKey(player)((p,player))
            }.toTagMod
        )
      } else {
        val h = r.hands(0)

        val (made,down) = {
          h.madeOrDown match {
            case Made => ( h.tricks.toString(), "" )
            case Down => ( "", h.tricks.toString() )
          }
        }

        <.tr(
          <.td(
            AppButton("Round"+(round+1).toString(),
                      (round+1).toString(),
                      props.routerCtl.setOnClick(props.page.toHandView(round, 0)))
          ),
          <.td( getPlayer(r.dealerForHand(1)) ),
          <.td( h.contractAsStringNoVul ),
          <.td( getPlayer(h.declarer) ),
          <.td( made ),
          <.td( down ),
          (0 until scores.length).map { j =>
            val i = order(j)
            val player="Round"+round+"Player"+(i+1)
            val p = scores(i)
            playerScore.withKey(player)((p,player))
          }.toTagMod
        )
      }
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
                <.th( scoring.chicago.id.id ),
                <.th( ^.colSpan := 5 ),
                <.th( ^.colSpan:=scoring.players.size, "Totals")
                ),
            <.tr(
              <.th( "Hand"),
              <.th( "Dealer"),
              <.th( "Contract"),
              <.th( "By"),
              <.th( "Made"),
              <.th( "Down"),
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
                <.td( ^.colSpan := 5 ),
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

  val component = ScalaComponent.builder[Props]("ViewQuintet")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

