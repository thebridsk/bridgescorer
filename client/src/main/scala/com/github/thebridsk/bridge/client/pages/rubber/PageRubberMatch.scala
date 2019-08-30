package com.github.thebridsk.bridge.client.pages.rubber

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.RubberStore
import com.github.thebridsk.bridge.client.controller.RubberController
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.bridge.store.RubberStore
import com.github.thebridsk.bridge.client.bridge.store.RubberStore
import com.github.thebridsk.bridge.data.rubber.RubberScoring
import com.github.thebridsk.bridge.data.rubber.GameScoring
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.RubberMatchViewBase
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.RubberMatchNamesView
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.ListView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

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

          <.div(
              RubberPageBridgeAppBar(
                title = Seq[CtorType.ChildArg](
                  MuiTypography(
                      variant = TextVariant.h6,
                      color = TextColor.inherit,
                  )(
                      <.span( "Match" )
                  )),
                helpurl = "../help/rubber/summary.html",
                routeCtl = props.routerCtl
              )(),
              <.div(
                rubStyles.divRubberMatch,
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
                ViewRubberMatchDetails(props.page.toDetails(), props.routerCtl, true ),
                <.div( baseStyles.divFlexBreak ),
                <.div( baseStyles.divFooter,
                    <.div(
                        baseStyles.divFooterLeft,
                        !score.done ?= AppButton( "NextHand", "Next Hand", baseStyles.requiredNotNext, ^.onClick-->nextHand )
                      ),
                    <.div(
                        baseStyles.divFooterLeft,
                        AppButton( "EditNames", "Edit Names", ^.onClick-->tonames )
  //                      false ?= AppButton( "Details", "Details", ^.onClick-->toDetails() )
                      ),
                    <.div(
                        baseStyles.divFooterRight,
                        ComponentInputStyleButton( CallbackTo{} ),
                        AppButton( "Quit", "Quit", score.done ?= baseStyles.requiredNotNext, ^.onClick-->quit ),
  //                      HelpButton("../help/rubber/summary.html")
                    )
                  )
                )
              )
        case Some(rub) if (rub.id == props.page.rid && !rub.gotAllPlayers()) =>
          <.div(
            PageRubberNames( RubberMatchNamesView( props.page.rid ), props.routerCtl )
          )
        case _ =>
          <.div(
              RubberPageBridgeAppBar(
                title = Seq[CtorType.ChildArg](
                  MuiTypography(
                      variant = TextVariant.h6,
                      color = TextColor.inherit,
                  )(
                      <.span( "Match" )
                  )),
                helpurl = "../help/rubber/summary.html",
                routeCtl = props.routerCtl
              )(),
              <.div(
                rubStyles.divRubberMatch,
                <.h1("Waiting to load data")
              )
          )
      }
    }

    val toDetails = scope.props >>= { props => props.routerCtl.set(props.page.toDetails()) }

    val tonames = scope.props >>= { props => props.routerCtl.set(props.page.toNames()) }

    val quit = scope.props >>= { props => props.routerCtl.set(ListView) }

    val nextHand = {
      RubberStore.getRubber match {
        case Some(rub) =>
          val handid = "new"
          val props = scope.withEffectsImpure.props
          props.routerCtl.set(props.page.toHand(handid))
        case _ => CallbackTo {}
      }
    }

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    val didMount = Callback {
      logger.info("PageRubberMatch.didMount")
      RubberStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback {
      RubberController.ensureMatch(p.page.rid)
      RubberController.monitor(p.page.rid)
    }}

    val willUnmount = Callback {
      logger.info("PageRubberMatch.willUnmount")
      RubberStore.removeChangeListener(storeCallback)
      RubberController.delayStop()
    }
  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = Callback {
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (prevProps.page != props.page) {
      RubberController.monitor(props.page.rid)
    }
  }

  val component = ScalaComponent.builder[Props]("PageRubberMatch")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .componentDidUpdate( didUpdate )
                            .build
}

