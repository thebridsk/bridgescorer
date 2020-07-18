package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.client.bridge.store.ChicagoStore
import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.RoundView
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.SummaryView
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.ListView
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.data.MatchChicago

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
object PageSummary {
  import PageSummaryInternal._

  case class Props(
      page: Either[SummaryView, RoundView],
      routerCtl: BridgeRouter[ChicagoPage]
  )

  def apply(page: SummaryView, routerCtl: BridgeRouter[ChicagoPage]) =
    component(Props(Left(page), routerCtl))

  def apply(page: RoundView, routerCtl: BridgeRouter[ChicagoPage]) =
    component(Props(Right(page), routerCtl))

}

object PageSummaryInternal {
  import PageSummary._

  val logger = Logger("bridge.PageSummary")

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

    def toSummaryView(props: Props) =
      props.page match {
        case Left(summary)    => summary
        case Right(roundview) => roundview.toSummaryView
      }

    def toEditNamesView(props: Props) =
      props.page match {
        case Left(summary)    => summary.toEditNamesView
        case Right(roundview) => roundview.toEditNamesView
      }

    def toRoundView(round: Int, props: Props) =
      props.page match {
        case Left(summary)    => summary.toRoundView(round)
        case Right(roundview) => ChicagoRouter.RoundView(roundview.chiid.id, round)
      }

    def toNamesView(round: Int, props: Props) =
      props.page match {
        case Left(summary)    => summary.toNamesView(round)
        case Right(roundview) => ChicagoRouter.NamesView(roundview.chiid.id, round)
      }

    def toHandView(round: Int, hand: Int, props: Props) =
      props.page match {
        case Left(summary) => summary.toHandView(round, hand)
        case Right(roundview) =>
          ChicagoRouter.HandView(roundview.chiid.id, round, hand)
      }

    def render(props: Props, state: State) = {
      import ChicagoStyles._
      val smc = ChicagoStore.getChicago
      logger.fine(s"Render with $smc")
      <.div(
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span("Summary")
            )
          ),
          helpurl =
            if (smc.map(mc => mc.isQuintet()).getOrElse(false))
              "../help/chicago/summaryquintet.html"
            else "../help/chicago/summary.html",
          routeCtl = props.routerCtl
        )(),
        smc match {
          case Some(mc) =>
            val scoring = ChicagoScoring(mc)
            val numberRounds = scoring.rounds.length
            if (numberRounds == 0) {
              <.div(
                chiStyles.chicagoSummaryPage,
                <.p("The Chicago Match has not been started yet."),
                <.p(),
                AppButton("Start", "Start", ^.onClick --> nextRound),
                " ",
                AppButton(
                  "Quit",
                  "Quit",
                  ^.onClick --> props.routerCtl.set(ListView)
                )
              )
            } else {
              val lastRound = scoring.rounds(numberRounds - 1)
              val lastRoundHands = lastRound.hands
              // The buttons: (description is round and hand one based)
              //   next hand in round ( handsPerRound==0 or hand < handsPerRound) and not 6 8 hand round buttons
              //   new round (show in round 1 hand 4, or when #hands == handPerRound != 0)
              //   6 hand round (show if in round 1 hand 5)
              //   8 hand round (show if in round 1 hand 5)
              //
              val show68HandRound = scoring.gamesPerRound == 0 && numberRounds == 1 && lastRoundHands.length == 5
              val showNextHand = scoring.gamesPerRound == 1 || ((scoring.gamesPerRound == 0 || lastRoundHands.length < scoring.gamesPerRound) && !show68HandRound)
              val showNewRound = ((numberRounds == 1 && lastRoundHands.length == 4) ||
                                  (scoring.gamesPerRound != 0 && lastRoundHands.length == scoring.gamesPerRound)) &&
                                 scoring.gamesPerRound != 1
              val showSet68HandRound = numberRounds == 1 && lastRoundHands.length == 4 && scoring.gamesPerRound == 0
              val (start, end, showRound) = props.page match {
                case Left(_) => (0, scoring.rounds.length, None)
                case Right(RoundView(chiid, round)) =>
                  (round, round + 1, Some(round))
              }

              def displayRound() =
                props.page match {
                  case Left(_)                        => 0
                  case Right(RoundView(chiid, round)) => round
                }

              <.div(
                if (scoring.chicago.gamesPerRound == 1)
                  chiStyles.chicagoFastSummaryPage
                else chiStyles.chicagoSummaryPage,
                if (scoring.chicago.isQuintet()) {
                  <.div(
                    ViewQuintet(scoring, toSummaryView(props), props.routerCtl)
                  )
                } else {
                  Seq[TagMod](
                    <.div(
                      ViewTotalsTable(
                        scoring,
                        showRound,
                        toSummaryView(props),
                        props.routerCtl
                      )
                    ),
                    (start until end).map { i =>
                      <.div(
                        ViewRoundTable.withKey("ShowRound" + i)(
                          scoring,
                          i,
                          toRoundView(i, props),
                          props.routerCtl
                        )
                      )
                    }.toTagMod
                  ).toTagMod
                },
                <.div(baseStyles.divFlexBreak),
                <.div(
                  baseStyles.divFooter,
                  <.div(
                    baseStyles.divFooterLeft,
                    (props.page.isLeft || displayRound() == scoring.rounds.length - 1) ?=
                      <.span(
                        showNewRound ?= AppButton(
                          "NewRound",
                          "New Round",
                          baseStyles.requiredNotNext,
                          ^.onClick --> nextRound
                        ),
                        <.span(" "),
                        show68HandRound || showSet68HandRound ?= AppButton(
                          "6HandRound",
                          "6 Hand Round",
                          baseStyles.requiredNotNext,
                          ^.onClick --> do68Callback(6)
                        ),
                        <.span(" "),
                        show68HandRound || showSet68HandRound ?= AppButton(
                          "8HandRound",
                          "8 Hand Round",
                          baseStyles.requiredNotNext,
                          ^.onClick --> do68Callback(8)
                        ),
                        <.span(" "),
                        showNextHand ?= AppButton(
                          "NextHand",
                          "Next Hand",
                          baseStyles.requiredNotNext,
                          ^.onClick --> (if (scoring.gamesPerRound ==1) nextRound else nextHand)
                        ),
                        <.span(" ")
                      )
                  ),
                  <.div(
                    baseStyles.divFooterCenter,
                    AppButton(
                      "Quit",
                      "Quit",
                      ^.onClick --> props.routerCtl.set(ListView)
                    ),
                    " ",
                    props.page.isRight ?= AppButton(
                      "Summary",
                      "All Rounds",
                      ^.onClick --> props.routerCtl.set(toSummaryView(props))
                    )
                  ),
                  <.div(
                    baseStyles.divFooterRight,
                    ComponentInputStyleButton(scope.forceUpdate),
                    AppButton(
                      "EditNames",
                      "Edit Names",
                      ^.onClick --> props.routerCtl.set(toEditNamesView(props))
                    )
                    )
                )
              )
            }
          case _ =>
            HomePage.loading
        }
      )
    }

    def do68Callback(gamesInRound: Int) = scope.props >>= { props =>
      Callback {
        ChicagoStore.getChicago match {
          case Some(mc) =>
            if (mc.rounds.length == 1 && mc.gamesPerRound == 0) {
              val newmc = mc.setGamesPerRound(gamesInRound)
              ChicagoController.updateMatch(newmc)
              props.routerCtl
                .set(toHandView(0, mc.rounds(0).hands.length, props))
                .runNow()
            } else {
              logger.warning(
                "PageSummary: 68 not in first round, or already set"
              )
            }
          case _ =>
            logger.warning("PageSummary: 68 MatchChicago not found")
        }
      }
    }

    val nextRound = scope.props >>= { props =>
      Callback {
        ChicagoStore.getChicago match {
          case Some(mc) =>
            val n = {
              val nn = mc.rounds.length
              if (nn > 0 && mc.rounds.last.hands.isEmpty) nn-1
              else nn
            }
            if (n == 1 && mc.gamesPerRound == 0) {
              val nhands = mc.rounds(0).hands.length
              val newmc = mc.setGamesPerRound(nhands)
              logger.fine(
                "Setting games per round, rounds=" + n + ", mc.gamesPerRound=" + mc.gamesPerRound + ", nhands=" + nhands + ", newGamesPerRound=" + newmc.gamesPerRound
              )
              ChicagoController.updateMatch(newmc)
//            val newgpr = ChicagoStore.getChicago.get.gamesPerRound
//            logger.warning("new games per round is "+newgpr)
            } else {
              logger.fine(
                "Not setting games per round, rounds=" + n + ", mc.gamesPerRound=" + mc.gamesPerRound
              )
            }
            props.routerCtl.set(toNamesView(n, props)).runNow()
          case None =>
            logger.warning("PageSummary: no chicago match found")
        }
      }
    }

    val nextHand = scope.props >>= { props =>
      {
        ChicagoStore.getChicago match {
          case Some(mc) =>
            val nr = mc.rounds.size
            val nh = mc.rounds(nr - 1).hands.size
            if (mc.gamesPerRound == 1 && nh == 0 && nr != 1)
              props.routerCtl.set(toNamesView(nr - 1, props))
            else props.routerCtl.set(toHandView(nr - 1, nh, props))
          case None =>
            Callback {
              logger.warning("PageSummary: no chicago match found")
            }
        }
      }
    }

    val storeCallback = scope.forceUpdate

    val forceUpdate = scope.forceUpdate

    val didMount = Callback {
      ChicagoStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) =>
      Callback {
        val chiid = MatchChicago.id(p.page match {
          case Left(SummaryView(chiid))       => chiid
          case Right(RoundView(chiid, round)) => chiid
        })
        logger.info(s"PageSummary.didMount on $chiid")
        import scala.concurrent.ExecutionContext.Implicits.global
//      ChicagoController.ensureMatch(chiid).foreach( m => scope.withEffectsImpure.forceUpdate )
        ChicagoController.monitor(chiid)
      }
    }

    val willUnmount = Callback {
      logger.info("PageSummary.willUnmount")
      ChicagoStore.removeChangeListener(storeCallback)
      ChicagoController.delayStop()
    }

  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = Callback {
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (prevProps.page != props.page) {
      val chiid = MatchChicago.id(props.page match {
        case Left(SummaryView(chiid))       => chiid
        case Right(RoundView(chiid, round)) => chiid
      })
      ChicagoController.monitor(chiid)
    }
  }

  val component = ScalaComponent
    .builder[Props]("PageSummary")
    .initialStateFromProps { props =>
      State()
    }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .componentDidUpdate( didUpdate )
    .build
}
