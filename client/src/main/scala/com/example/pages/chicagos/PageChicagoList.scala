package com.example.pages.chicagos

import scala.scalajs.js

import com.example.controller.ChicagoController
import com.example.data.MatchChicago
import com.example.data.SystemTime
import com.example.data.chicago.ChicagoScoring
import utils.logging.Logger
import com.example.logging.LogLifecycleToServer
import com.example.rest2.RestClientChicago
import com.example.routes.BridgeRouter
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.example.react.AppButton
import com.example.react.DateUtils
import com.example.pages.chicagos.ChicagoRouter.NamesView
import com.example.pages.chicagos.ChicagoRouter.SummaryView
import utils.logging.Level
import com.example.rest2.ResultHolder
import com.example.rest2.RequestCancelled
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.data.Id
import com.example.react.PopupOkCancel
import com.example.logger.Alerter
import com.example.react.HelpButton

/**
 * @author werewolf
 */
object PageChicagoList {
  import PageChicagoListInternal._

  type CallbackDone = Callback
  type ShowCallback = (/* id: */ String)=>Callback

  case class Props( routerCtl: BridgeRouter[ChicagoPage] )

  def apply( routerCtl: BridgeRouter[ChicagoPage] ) = component(Props(routerCtl))

}

object PageChicagoListInternal {
  import PageChicagoList._
  import ChicagoStyles._

  implicit val logger = Logger("bridge.PageChicagoList")
  implicit val defaultTraceLevelForReactComponents = Level.FINER

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( chicagos: Array[MatchChicago], workingOnNew: Option[String], askingToDelete: Option[String] )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def delete( id: String ) = scope.modState(s => s.copy( askingToDelete = Some(id)))

    val deleteOK = scope.modState{ s =>
        s.askingToDelete.map{ id =>
          val ns = s.copy(chicagos= s.chicagos.filter(c=>c.id!=id), askingToDelete = None)
          RestClientChicago.delete(id).recordFailure()
          ns
        }.getOrElse(s)
      }

    val deleteCancel = scope.modState(s => s.copy( askingToDelete = None))

    val resultChicago = ResultHolder[MatchChicago]()

    val cancel = Callback {
      resultChicago.cancel()
    } >> scope.modState( s => s.copy(workingOnNew=None))

    val newChicago = {
      import scala.concurrent.ExecutionContext.Implicits.global
      scope.modState( s => s.copy(workingOnNew=Some("Creating a new Chicago match...")), Callback {
        logger.info(s"Creating new chicago.  HomePage.mounted=${mounted}")
        val result = ChicagoController.createMatch()
        resultChicago.set(result)
        result.foreach { created =>
          logger.info(s"Got new chicago ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) {
            scope.withEffectsImpure.props.routerCtl.set(NamesView(created.id,0)).runNow()
          }
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              scope.withEffectsImpure.modState( s => s.copy(workingOnNew=Some("Failed to create a new Chicago match")))
          }
        })
      })

    }
//      scope.modState( s => s.copy( workingOnNew = Some("Creating new...") )) >> ChicagoController.createMatch(
//        created=> {
//          logger.info("Got new chicago "+created.id)
//          scope.props.runNow.routerCtl.set(NamesView(created.id,0)).runNow()
//        }
//      )

    def showChicago( chi: MatchChicago ) = Callback {
      ChicagoController.showMatch( chi )
    } >> scope.props >>= { props => props.routerCtl.set(SummaryView(chi.id)) }


    def render(props: Props, state:State) = {
      val chicagos = state.chicagos.sortWith((l,r) => Id.idComparer(l.id, r.id) > 0)
      val maxplayers = chicagos.map( mc => mc.players.length ).foldLeft(4){case (m, i) => math.max(m,i)}
      <.div( chiStyles.chicagoListPage,
          PopupOkCancel(
            state.askingToDelete.map(id => s"Are you sure you want to delete Chicago match ${id}"),
            Some(deleteOK),
            Some(deleteCancel)
          ),
          <.table(
              <.thead(
                <.tr(
                  <.th( "Id"),
                  <.th( "Created", <.br(), "Updated"),
                  <.th( "Players - Scores", ^.colSpan:=maxplayers),
                  <.th( "")
              )),
              <.tbody(
                  ChicagoRowFirst.withKey("New")((this,props,state,maxplayers)),
                  (0 until chicagos.length).map { i =>
                    val key="Game"+i
                    val chicago = ChicagoScoring(chicagos(i))
                    ChicagoRow.withKey(key)((this,props,state,i,maxplayers,chicago))
                  }.toTagMod
              )
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "Home", "Home", props.routerCtl.home )
            ),
            <.div(
              baseStyles.divFooterLeft,
              HelpButton("/help/chicago/list.html")
            )
          )
      )
    }

    private var mounted = false

    val didMount = Callback {
      mounted = true

      // make AJAX rest call here
      logger.finer("PageChicagoList: Sending chicagos list request to server")
      RestClientChicago.list().recordFailure().foreach( list => Alerter.tryitWithUnit {
        logger.finer(s"PageChicagoList: got ${list.size} entries, mounted=${mounted}")
        scope.withEffectsImpure.modState( s => s.copy(chicagos=list))
      })

    }

    val willUnmount = Callback {
      mounted = false
    }

  }

  val ChicagoRowFirst = ScalaComponent.builder[(Backend, Props, State, Int)]("ChicagoRowFirst")
    .stateless
    .render_P { args =>
      val (backend, props, state, maxplayers) = args
      <.tr(
          <.td( "" ),
          <.td(
            state.workingOnNew match {
              case Some(msg) =>
                <.span(
                  msg,
                  " ",
                  AppButton(
                    "PopupCancel", "Cancel",
                    ^.onClick --> backend.cancel
                  )
                )
              case None =>
                AppButton( "New", "New", ^.onClick --> backend.newChicago)
            }
          ),
          <.td( ^.colSpan:=maxplayers,"" ),
          <.td( "")
          )
    }.build

  val ChicagoRow = ScalaComponent.builder[(Backend, Props, State, Int, Int, ChicagoScoring)]("ChicagoRow")
    .stateless
    .render_P( args => {
      val (backend, props, state, game, maxplayers, chicago) = args
      val id = chicago.chicago.id
      val date = id
      val created = DateUtils.showDate(chicago.chicago.created)
      val updated = DateUtils.showDate(chicago.chicago.updated)

      val (players,scores) = chicago.sortedResults()

      <.tr(
          <.td(
                AppButton( "Chicago"+id, id,
                           baseStyles.appButton100,
                           ^.onClick --> backend.showChicago(chicago.chicago)
                         )
              ),
          <.td( created,<.br(),updated),
          (0 until players.length).map { i =>
            <.td( players(i)+" - "+scores(i).toString)
          }.toTagMod,
          (players.length until maxplayers ).map { i =>
            <.td( )
          }.toTagMod,
          <.td( AppButton( "Delete", "Delete", ^.onClick --> backend.delete(id) ))
          )
    }).build

  val component = ScalaComponent.builder[Props]("PageChicagoList")
                            .initialStateFromProps { props => State(Array(),None, None) }
                            .backend(new Backend(_))
                            .renderBackend
                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

