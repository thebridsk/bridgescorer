package com.example.pages.rubber

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import utils.logging.Logger
import com.example.bridge.store.RubberStore
import com.example.controller.RubberController
import japgolly.scalajs.react._
import com.example.bridge.store.RubberStore
import com.example.bridge.store.RubberStore
import com.example.data.rubber.RubberScoring
import com.example.data.rubber.GameScoring
import com.example.pages.hand.ComponentInputStyleButton
import com.example.routes.BridgeRouter
import com.example.pages.rubber.RubberRouter.RubberMatchViewBase
import com.example.pages.rubber.RubberRouter.RubberMatchNamesView
import com.example.pages.rubber.RubberRouter.ListView
import com.example.react.AppButton
import com.example.react.Utils._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageRubberMatch( PageRubberMatch.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageRubberMatch {
  import PageRubberMatchInternal._

  case class Props( page: RubberMatchViewBase, routerCtl: BridgeRouter[RubberPage] )

  def apply( page: RubberMatchViewBase, routerCtl: BridgeRouter[RubberPage] ) =
    component( Props( page, routerCtl ) )

}

object PageRubberMatchInternal {
  import PageRubberMatch._

  val logger = Logger("bridge.PageRubberMatch")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Compares two lists, and returns the difference in length of the shorter list.
   * Returns a tuple of three Ints
   *     the length of the longer list
   *     the diff of the length of the first list from the longer list length
   *     the diff of the length of the second list from the longer list length
   */
  def getSizeDiff[A]( l1: List[A], l2: List[A] ) = {
    val s1 = l1.size
    val s2 = l2.size
    if (s1 == s2) (s1,0,0)
    else if (s1<s2) (s2,s2-s1,0)
    else (s1,0,s1-s2)
  }

  def getFromList[A]( l: List[A], i: Int, diff: Int ) = {
    val index = i-diff
    if (index < 0) None
    else Some(l(index))
  }

  def toTagMod[A]( l: List[A] ) = {
    if (l.isEmpty) EmptyVdom
    else {
      TagMod( l.flatMap{ i =>
        TagMod(<.br)::TagMod(i.toString())::Nil
      }.tail :_*)
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
    def render( props: Props, state: State ) = {
      import RubberStyles._
      RubberStore.getRubber match {
        case Some(rub) if (rub.id == props.page.rid && rub.gotAllPlayers()) =>
          val score = RubberScoring(rub)
          val (nsAbove,nsBelow,ewAbove,ewBelow) = score.totals

          def showRow( label: String, ns: String, ew: String ) = {
            <.tr(
                <.td( label ),
                <.td( ns, rubStyles.tableNumber ),
                <.td( ew, rubStyles.tableNumber )
                )
          }

          def showScoresAbove( label: String, nsl: List[Int], ewl: List[Int] ): TagMod = {

            val len = if (nsl.length<ewl.length) ewl.length else nsl.length
            if (len == 0) {
              showRow(label,"","")
            } else {
              <.tr(
                <.td( label ),
                <.td( toTagMod(nsl), rubStyles.tableNumber, rubStyles.aboveLine ),
                <.td( toTagMod(ewl), rubStyles.tableNumber, rubStyles.aboveLine ),
              )
            }

          }

          def showScoresBelow( label: String, nsl: List[Int], ewl: List[Int] ) = {

            val len = if (nsl.length<ewl.length) ewl.length else nsl.length
            if (len == 0) {
              showRow(label,"","")
            } else {
              <.tr(
                <.td( label ),
                <.td( toTagMod(nsl), rubStyles.tableNumber, rubStyles.belowLine ),
                <.td( toTagMod(ewl), rubStyles.tableNumber, rubStyles.belowLine ),
              )
            }

          }

          def aboveTheLine() = showScoresAbove("Above", nsAbove, ewAbove)
          def showGame( gameNumber: Int, game: GameScoring ) = {
            val (nsA,nsB,ewA,ewB) = game.scores
            showScoresBelow( "Game "+gameNumber, nsB,ewB )
          }
          def showGames() = {
            val n = score.games.length
            (0 until n).map { i =>
              showGame( i+1, score.games(i) )
            }.toTagMod
          }

          <.div( rubStyles.divRubberMatch,
              <.div( rubStyles.divRubberMatchView,
                  <.table(
                      <.thead(
                          <.tr(
                              <.th( score.rubber.id ),
                              <.th( rub.north, " ", rub.south ),
                              <.th( rub.east, " ", rub.west )
                              )
                          ),
                      <.tfoot(
                          showRow("Bonus", score.nsBonus.toString(), score.ewBonus.toString()),
                          showRow("Total", score.nsTotal.toString(), score.ewTotal.toString())
                      ),
                      <.tbody(
                          aboveTheLine(),
                          showGames(),
                          )
                      )
                  ),
              PageRubberMatchDetails(props.page.toDetails(), props.routerCtl, true ),
              <.div( baseStyles.divFooter,
                  <.div(
                      baseStyles.divFooterLeft,
                      !score.done ?= AppButton( "NextHand", "Next Hand", baseStyles.requiredNotNext, ^.onClick-->nextHand() )
                      ),
                  <.div(
                      baseStyles.divFooterLeft,
                      AppButton( "EditNames", "Edit Names", ^.onClick-->tonames() )
//                      false ?= AppButton( "Details", "Details", ^.onClick-->toDetails() )
                      ),
                  <.div(
                      baseStyles.divFooterRight,
                      ComponentInputStyleButton( CallbackTo{} ),
                      AppButton( "Quit", "Quit", score.done ?= baseStyles.requiredNotNext, ^.onClick-->quit() )
                      )
                  )
              )
        case Some(rub) if (rub.id == props.page.rid && !rub.gotAllPlayers()) =>
          <.div(
            PageRubberNames( RubberMatchNamesView( props.page.rid ), props.routerCtl )
          )
        case _ =>
          <.div(<.h1("Waiting to load data"))
      }
    }

    def toDetails() = scope.props >>= { props => props.routerCtl.set(props.page.toDetails()) }

    def tonames() = scope.props >>= { props => props.routerCtl.set(props.page.toNames()) }

    def quit() = scope.props >>= { props => props.routerCtl.set(ListView) }

    def nextHand() = {
      RubberStore.getRubber match {
        case Some(rub) =>
          val handid = "new"
          val props = scope.withEffectsImpure.props
          props.routerCtl.set(props.page.toHand(handid))
        case _ => CallbackTo {}
      }
    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    def didMount() = Callback {
      logger.info("PageRubberNames.didMount")
      RubberStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      RubberController.ensureMatch(p.page.rid)
    )}

    def willUnmount() = Callback {
      logger.info("PageRubberNames.willUnmount")
      RubberStore.removeChangeListener(storeCallback)
    }
  }

  val component = ScalaComponent.builder[Props]("PageRubberMatch")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

