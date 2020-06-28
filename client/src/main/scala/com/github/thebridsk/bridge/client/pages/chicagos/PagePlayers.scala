package com.github.thebridsk.bridge.client.pages.chicagos

import com.github.thebridsk.bridge.client.bridge.store.ChicagoStore
import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.data._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.routes.BridgeRouter

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.NamesView
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage

/**
  * @author werewolf
  */
object PagePlayers {
  import PagePlayersInternal._

  val logger = Logger("bridge.PagePlayers")

  case class PlayerState(
      north: String,
      south: String,
      east: String,
      west: String,
      dealer: Option[PlayerPosition],
      gotNames: Boolean = false,
      names: List[String] = Nil,
      chicago5: Boolean = false,
      quintet: Boolean = false,
      simpleRotation: Boolean = false,
      extra: Option[String] = None
  ) {
    def isDealerValid() = dealer.isDefined
    def areAllPlayersValid() =
      playerValid(north) && playerValid(south) && playerValid(east) && playerValid(
        west
      ) &&
        (if (chicago5 || quintet) {
           extra.map(p => playerValid(p)).getOrElse(false)
         } else {
           true
         })

    def areAllPlayersUnique() = {
      val p =
        north.trim ::
        south.trim ::
        east.trim ::
        west.trim ::
        (if (chicago5 || quintet)
           extra.map(_.trim).toList
         else Nil)
      val before = p.length
      val after = p.distinct.length
      before == after
    }

    def isValid() =
      areAllPlayersValid() && isDealerValid() && areAllPlayersUnique()

    def isDealer(p: PlayerPosition): Boolean =
      dealer.map(d => d == p).getOrElse(false)

    def isDealer(p: String): Boolean =
      p match {
        case `north` => isDealer(North)
        case `south` => isDealer(South)
        case `east`  => isDealer(East)
        case `west`  => isDealer(West)
        case _       => false
      }

    def getDealer = dealer.map(d => d.pos.toString).getOrElse("")

    def getDealerName() =
      dealer
        .map(
          d =>
            d match {
              case North => north
              case South => south
              case East  => east
              case West  => west
            }
        )
        .getOrElse("")
  }

  case class State()

  type CallbackOk = (PlayerState) => Callback
  type CallbackCancel = Callback

  def apply(page: NamesView, router: BridgeRouter[ChicagoPage]) =
    component(MyProps(page, router))

  case class Props(
      page: NamesView,
      chicago: MatchChicago,
      router: BridgeRouter[ChicagoPage]
  )

  case class MyProps(page: NamesView, router: BridgeRouter[ChicagoPage]) {
    def getProps(chicago: MatchChicago) = Props(page, chicago, router)
  }
}

object PagePlayersInternal {
  import PagePlayers._

  def playerValid(s: String) = s.length != 0

  class Backend(scope: BackendScope[MyProps, State]) {
    def render(props: MyProps, state: State): VdomElement = {
      ChicagoStore.getChicago match {
        case Some(chi) if (chi.id == props.page.chiid) =>
          val rounds = chi.rounds
//          <.div(
          if (rounds.length == 0
              || rounds.length == 1 && rounds.head.hands.isEmpty
          ) {
            ViewPlayersVeryFirstRound(props.getProps(chi))
          } else {
            if (chi.players.size != 4) {
              if (chi.isQuintet()) {
                ViewPlayersQuintet(props.getProps(chi))
              } else {
                ViewPlayersFive(props.getProps(chi))
              }
            } else {
              rounds.length % 3 match {
                case 0 => ViewPlayersFourthRound(props.getProps(chi))
                case 1 => ViewPlayersSecondRound(props.getProps(chi))
                case 2 => ViewPlayersThirdRound(props.getProps(chi))
                case _ =>
                  throw new IllegalArgumentException(
                    "Internal error, can't happen"
                  )
              }
            }
          }
//          )
        case _ =>
          <.div(
            ChicagoPageBridgeAppBar(
              title = Seq[CtorType.ChildArg](
                MuiTypography(
                  variant = TextVariant.h6,
                  color = TextColor.inherit
                )(
                  <.span("Enter players and identify first dealer")
                )
              ),
              helpurl = "../help/chicago/four/names4.html",
              routeCtl = props.router
            )(),
            HomePage.loading
          )
      }

    }

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { props =>
      Callback {
        logger.info("PagePlayers.didMount")
        ChicagoStore.addChangeListener(storeCallback)

        import scala.concurrent.ExecutionContext.Implicits.global
        ChicagoController
          .ensureMatch(props.page.chiid)
//          .foreach(m => scope.withEffectsImpure.forceUpdate)
      }
    }

    val willUnmount = Callback {
      logger.info("PagePlayers.willUnmount")
      ChicagoStore.removeChangeListener(storeCallback)
    }

  }

  val component = ScalaComponent
    .builder[MyProps]("PagePlayers")
    .initialStateFromProps { props =>
      State()
    }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build
}
