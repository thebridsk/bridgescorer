package com.example.pages.rubber

import com.example.data.MatchRubber
import utils.logging.Logger
import com.example.logging.LogLifecycleToServer
import com.example.rest2.RestClientRubber

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.controller.RubberController
import com.example.controller.RubberController
import com.example.data.rubber.RubberScoring
import com.example.data.SystemTime
import scala.scalajs.js
import com.example.routes.BridgeRouter
import com.example.react.DateUtils
import com.example.react.AppButton
import com.example.pages.rubber.RubberRouter.RubberMatchNamesView
import com.example.pages.rubber.RubberRouter.RubberMatchView
import utils.logging.Level
import com.example.data.Id
import com.example.react.PopupOkCancel
import com.example.react.HelpButton

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageRubberList( PageRubberList.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageRubberList {
  import PageRubberListInternal._

  case class Props( page: RubberPage, routerCtl: BridgeRouter[RubberPage] )

  def apply( page: RubberPage, routerCtl: BridgeRouter[RubberPage] ) =
    component( Props( page, routerCtl ) )

}

object PageRubberListInternal {
  import PageRubberList._
  import RubberStyles._

  val logger = Logger("bridge.PageRubberList")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( rubbers: Array[MatchRubber], workingOnNew: Boolean, askingToDelete: Option[String] )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    import scala.concurrent.ExecutionContext.Implicits.global

    def delete( id: String ) = scope.modState(s => s.copy( askingToDelete = Some(id)))

    val deleteOK = scope.modState{ s =>
        s.askingToDelete.map{ id =>
          val ns = s.copy(rubbers= s.rubbers.filter(c=>c.id!=id), askingToDelete = None)
          RestClientRubber.delete(id).recordFailure()
          ns
        }.getOrElse(s)
      }

    val deleteCancel = scope.modState(s => s.copy( askingToDelete = None))

    val newRubber =
      scope.modState( s => s.copy(s.rubbers, true), Callback {
        RubberController.createMatch().foreach( created => {
          logger.info("Got new rubber match "+created.id)
          scope.withEffectsImpure.props.routerCtl.set(RubberMatchNamesView(created.id)).runNow()
        })
      })

    def showRubber( chi: MatchRubber ) = Callback {
      RubberController.showMatch( chi )
    } >> {
      scope.withEffectsImpure.props.routerCtl.set(RubberMatchView(chi.id))
    }

    def render( props: Props, state: State ) = {
      val rubbers = state.rubbers.sortWith((l,r) => Id.idComparer( l.id, r.id) > 0)
      <.div(
          rubStyles.listPage,
          PopupOkCancel(
            state.askingToDelete.map(id => s"Are you sure you want to delete Rubber match ${id}"),
            Some(deleteOK),
            Some(deleteCancel)
          ),
          <.table(
              <.thead(
                <.tr(
                  <.th( "Id"),
                  <.th( "Created", <.br(), "Updated"),
                  <.th( "Complete"),
                  <.th( "North", <.br(), "South"),
                  <.th( "NS Score"),
                  <.th( "East", <.br(), "West"),
                  <.th( "EW Score"),
                  <.th( "")
              )),
              <.tbody(
                  <.tr(
                      <.td( "" ),
                      <.td(
                        if (state.workingOnNew) {
                          <.span("Creating new...")
                        } else {
                          AppButton( "New", "New", ^.onClick --> newRubber)
                        }
                      ),
                      <.td( ""),
                      <.td( ^.colSpan:=4,"" ),
                      <.td( "")
                      ),
                  (0 until rubbers.length).map { i =>
                    val key="Game"+i
                    val r = RubberScoring(rubbers(i))
                    RubberRow(this,props,state,i,r)
                  }.toTagMod
              )
          ),
          <.div( baseStyles.divFooter,
            <.div( baseStyles.divFooterLeft,
              AppButton( "Home", "Home", props.routerCtl.home, ^.disabled:=state.workingOnNew )
            ),
            <.div(
              baseStyles.divFooterLeft,
              HelpButton("/help/rubber/list.html")
            )
          )
      )
    }

    def RubberRow(backend: Backend, props: Props, state: State, game: Int, rubber: RubberScoring) = {
      val id = rubber.rubber.id
      val date = id
      val created = DateUtils.formatDate(rubber.rubber.created)
      val updated = DateUtils.formatDate(rubber.rubber.updated)

      <.tr(
          <.td(
            AppButton( "Rubber"+id, id,
                       baseStyles.appButton100,
                       ^.disabled:=state.workingOnNew,
                       ^.onClick --> backend.showRubber(rubber.rubber) )
          ),
          <.td( created,<.br(),updated),
          <.td( if (rubber.done) "done" else ""),
          <.td( rubber.rubber.north,<.br(),rubber.rubber.south),
          <.td( rubber.nsTotal.toString()),
          <.td( rubber.rubber.east,<.br(),rubber.rubber.west),
          <.td( rubber.ewTotal.toString()),
          <.td( AppButton( "Delete", "Delete", ^.disabled:=state.workingOnNew, ^.onClick --> backend.delete(id) ))
          )
    }

    val didMount = Callback {
      // make AJAX rest call here
      logger.finer("PageChicagoList: Sending chicagos list request to server")
      RestClientRubber.list().recordFailure().foreach( list => {
        scope.withEffectsImpure.modState( s => s.copy(rubbers=list))
      })
    }
  }

  implicit val loggerForReactComponents = Logger("bridge.PageChicagoList")
  implicit val defaultTraceLevelForReactComponents = Level.FINER

  val component = ScalaComponent.builder[Props]("PageRubberList")
                            .initialStateFromProps { props => State( Array(), false, None ) }
                            .backend(new Backend(_))
                            .renderBackend
//                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
                            .componentDidMount( scope => scope.backend.didMount)
                            .build
}

