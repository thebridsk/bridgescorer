package com.example.pages

import com.example.routes.AppRouter._
import org.scalajs.dom.document
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import utils.logging.Logger
import com.example.data.ServerURL
import com.example.rest2.RestClientServerURL
import com.example.data.ServerVersion
import com.example.controller.Controller
import com.example.controller.RubberController
import com.example.version.VersionClient
import com.example.version.VersionShared
import scala.util.Success
import scala.util.Failure
import org.scalajs.dom.ext.AjaxException
import com.example.react.AppButton
import com.example.routes.AppRouter.About
import com.example.pages.chicagos.ChicagoModule.PlayChicago2
import com.example.pages.chicagos.ChicagoRouter.ListView
import com.example.pages.duplicate.DuplicateModule.PlayDuplicate
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.NewDuplicateView
import com.example.pages.rubber.RubberModule.PlayRubber
import com.example.pages.rubber.RubberRouter.{ ListView => RubberListView}
import com.example.pages.chicagos.ChicagoRouter.NamesView
import com.example.pages.rubber.RubberRouter.RubberMatchNamesView
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.controller.ChicagoController
import com.example.react.Utils._
import com.example.react.Popup
import com.example.rest2.RestResult
import com.example.data.MatchChicago
import com.example.rest2.Result
import com.example.rest2.ResultHolder
import com.example.rest2.RequestCancelled
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.rest2.RestClient
import com.example.data.RestMessage
import com.example.rest2.AjaxResult
import com.example.rest2.AjaxFailure
import com.example.rest2.WrapperXMLHttpRequest
import com.example.react.AppButtonLink
import com.example.fastclick.FastClick
import com.example.react.PopupOkCancel
import com.example.pages.duplicate.DuplicateRouter.SelectMatchView

/**
 * @author werewolf
 */
object HomePage {

  var debugging = false

  case class Props( callback: (AppPage)=>Callback)

  case class State( debugging: Boolean, serverUrl: ServerURL, working: Option[String], fastclickTest: Boolean )

  var fastclick: Option[FastClick] = None

  class Backend( scope: BackendScope[Props, State]) {

    def toggleFastclickTest() = scope.modState( s => s.copy( fastclickTest = !s.fastclickTest) )

    def isFastclickOn = fastclick.isDefined

    def toggleFastclick() = Callback {
      fastclick match {
        case Some(fc) =>
          fc.destroy()
          fastclick = None
        case None =>
          fastclick = Option( FastClick() )
      }
    } >> scope.forceUpdate

    def render( props: Props, state: State ) = {
      import BaseStyles._
      def callbackPage(page: AppPage) = props.callback(page)
      val doingWork = state.working.getOrElse("")
      val isWorking = state.working.isDefined
      <.div(
        rootStyles.homeDiv,
        PopupOkCancel( if (isWorking) Some(doingWork) else None, None, Some(cancel()) ),
        <.div(
          rootStyles.serverDiv,
          ^.id:="url",
          <.h1("Server"),
          <.ul(
            if (state.serverUrl.serverUrl.isEmpty) {
              <.li("No network interfaces found")
            } else {
              state.serverUrl.serverUrl.map{ url => <.li(url) }.toTagMod
            }
          )
        ),
        <.div(
          rootStyles.gameDiv,
          <.h1("Play"),
          <.table(
            <.tbody(
              <.tr(
                <.td( ^.width:="33%",
                  AppButton( "ChicagoList2", "Chicago List", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayChicago2(ListView)))
                ),
                <.td( ^.width:="33%",
                  AppButton( "Chicago2", "New Chicago", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick -->newChicago() )
                ),
                <.td( ^.width:="33%"
                )
              ),
              <.tr(
                <.td(
                  AppButton( "Duplicate", "Duplicate List", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayDuplicate(SummaryView)))
                ),
                <.td(
                  AppButton( "NewDuplicate", "New Duplicate", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayDuplicate(NewDuplicateView)))
                ),
                <.td(
                  AppButton( "SelectDuplicate", "Select Match", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayDuplicate(SelectMatchView)))
                )
              ),
              <.tr(
                <.td(
                  AppButton( "Rubber", "Rubber Bridge List", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayRubber(RubberListView)))
                ),
                <.td(
                  AppButton( "NewRubber", "New Rubber Bridge", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> newRubber())
                )
              )
            )
          )
        ),
        <.div(
          rootStyles.testHandsDiv,
          <.h1("Test Hands"),
          <.table(
            <.tbody(
              <.tr(
                <.td( ^.width:="33%",
                  AppButton( "TestDuplicateHand",  "Duplicate",
                             rootStyles.playButton,
                             ^.disabled:=isWorking,
                             ^.onClick --> callbackPage(ShowDuplicateHand))
                ),
                <.td( ^.width:="33%",
                  AppButton( "TestChicagoHand", "Chicago",
                             rootStyles.playButton,
                             ^.disabled:=isWorking,
                             ^.onClick --> callbackPage(ShowChicagoHand))
                ),
                <.td( ^.width:="33%",
                  AppButton( "TestRubberHand", "Rubber",
                             rootStyles.playButton,
                             ^.disabled:=isWorking,
                             ^.onClick --> callbackPage(ShowRubberHand))
                )
              )
            )
          )
        ),
        <.div(
          rootStyles.miscDiv,
          <.h1("Miscellaneous"),
          <.table(
            <.tbody(
              <.tr(
                <.td( ^.width:="25%",
                  {
                    val location = document.defaultView.location
                    val origin = location.origin.get
                    val path = location.pathname
                    val (newp,name) = if (path.indexOf("indexNoScale") >= 0) {
                      (s"""${origin}/public/index.html""", "Scaling")
                    } else {
                      (s"""${origin}/public/indexNoScale.html""", "No Scaling")
                    }
                    val newpath = if (path.endsWith(".gz")) {
                      s"""${newp}.gz"""
                    } else {
                      newp
                    }
                    AppButtonLink( "NoScaling", name, newpath,
                                   rootStyles.playButton,
                                   ^.disabled:=isWorking
                    )
                  }
                ),
                <.td( ^.width:="25%",
                  {
                    AppButton(
                      "FastclickTest", "Fast Click Test",
                      rootStyles.playButton,
                      ^.disabled:=isWorking,
                      state.fastclickTest ?= baseStyles.buttonSelected,
                      ^.onClick --> toggleFastclickTest()
                    )
                  }
                ),
                <.td( ^.width:="25%",
                  AppButton(
                    "ToggleFastclick", "Fast Click",
                    rootStyles.playButton,
                    isFastclickOn ?= baseStyles.buttonSelected,
                    ^.disabled:=isWorking,
                    ^.onClick --> toggleFastclick()
                  )
                ),
                <.td( ^.width:="25%",
                  isPageFromLocalHost() ?= AppButton(
                    "Shutdown", "Shutdown Server",
                    rootStyles.playButton,
                    ^.disabled:=isWorking,
                    ^.onClick --> doShutdown()
                  )
                )
              ),
              <.tr(
                <.td( ^.width:="25%",
                  AppButton( "About", "About",
                             rootStyles.playButton,
                             ^.disabled:=isWorking,
                             ^.onClick --> callbackPage(About))
                ),
                <.td( ^.width:="25%",
                  AppButton( "Info", "Info",
                             rootStyles.playButton,
                             ^.disabled:=isWorking,
                             ^.onClick --> callbackPage(Info))
                ),
                <.td( ^.width:="25%",
                  AppButton( "Debug", "Debug",
                             rootStyles.playButton,
                             ^.disabled:=isWorking,
//                               ^.onClick --> debugCallback,
                             debugging ?= baseStyles.buttonSelected)
                ),
                <.td( ^.width:="25%",
                  AppButton( "TestPage", "Test Page",
                             rootStyles.playButton,
                             ^.disabled:=isWorking,
                             ^.onClick --> callbackPage(PageTest))
                )
              ),
              <.tr(
                <.td(" ")
              ),
              <.tr(
                <.td( ^.width:="25%",
                  {
                    val location = document.defaultView.location
                    val origin = location.origin.get
                    val path = s"""${origin}/v1/docs"""
                    AppButtonLink( "SwaggerDocs", "Swagger Docs", path,
                                   rootStyles.playButton,
                                   ^.disabled:=isWorking
                    )
                  }
                ),
                <.td( ^.width:="25%",
                  {
                    val location = document.defaultView.location
                    val origin = location.origin.get
                    val path = s"""${origin}/public/apidocs.html"""
                    AppButtonLink( "SwaggerDocs2", "Swagger API Docs", path,
                                   rootStyles.playButton,
                                   ^.disabled:=isWorking
                    )
                  }
                )
              )
            )
          )
        )
      )
    }

    def doShutdown() = scope.modState( s => s.copy(working = Some("Sending shutdown command to server")), Callback {

      import com.example.rest2.RestClient._

      val url = "/v1/shutdown?doit=yes"

      val res = AjaxResult.post(url).recordFailure()
      resultShutdown.set(res)
      res.onComplete( _ match {
        case Success(req) =>
          if (req.status == 204) {
            scope.withEffectsImpure.props.callback(ThankYou).runNow()
          } else {
            val resp = req.toRestMessage
            logger.severe(s"Error from server on shutdown action: ${resp}")
            scope.withEffectsImpure.modState( s => s.copy(working = Some(s"Error from server: ${resp}")) )
          }
        case Failure(f) =>
          f match {
            case x: RequestCancelled =>
              // ignore this
            case _ =>
              logger.severe("Error trying to shutdown server: ",f)
              scope.withEffectsImpure.modState( s => s.copy(working = Some(s"Error sending shutdown to server ${f}")) )
          }
      })
    })

    def errorCB( result: RestResult[MatchChicago] ): Unit = {
      scope.modState( s => s.copy(working=Some("Unable to contact server")))
    }

    val resultChicago = ResultHolder[MatchChicago]()
    val resultShutdown = ResultHolder[WrapperXMLHttpRequest]()

//  import org.scalajs.dom.document
//  document.defaultView.location.reload(true)
    def cancel() = Callback {
      resultChicago.cancel()
      resultShutdown.cancel()
    } >> scope.modState( s => s.copy(working=None))

    import scala.concurrent.ExecutionContext.Implicits.global

    def newChicago() = {
      scope.modState( s => s.copy(working=Some("Working on creating a new Chicago match")), Callback {
        val result = ChicagoController.createMatch()
        resultChicago.set(result)
        result.foreach { created =>
          logger.info(s"Got new chicago ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) scope.withEffectsImpure.props.callback(PlayChicago2(NamesView(created.id,0))).runNow()
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              scope.withEffectsImpure.modState( s => s.copy(working=Some("Failed to create a new Chicago match")))
          }
        })
      })
    }

    def newRubber() =
      scope.modState( s => s.copy(working=Some("Working on creating a new rubber match")), Callback {
        val result = RubberController.createMatch()
        result.foreach { created =>
          logger.info(s"Got new rubber ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) scope.withEffectsImpure.props.callback(PlayRubber(RubberMatchNamesView(created.id))).runNow()
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              scope.withEffectsImpure.modState( s => s.copy(working=Some("Failed to create a new Chicago match")))
          }
        })
      })

    def newDuplicate() =
      scope.modState( s => s.copy(working=Some("Working on creating a new duplicate match")), Callback {
        val result = Controller.createMatchDuplicate().recordFailure()
        result.foreach { created=>
          logger.info("Got new duplicate match ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) scope.withEffectsImpure.props.callback(PlayDuplicate(CompleteScoreboardView(created.id))).runNow()
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              scope.withEffectsImpure.modState( s => s.copy(working=Some("Failed to create a new duplicate match")))
          }
        })
      })

    private var mounted = false

    def didMount() = Callback {
      mounted = true
      // make AJAX rest call here
      logger.info("HomePage.didMount: Sending serverurl request to server")
      RestClientServerURL.list().recordFailure().foreach( serverUrl => {
        if (mounted) {
          scope.withEffectsImpure.modState( s => s.copy(serverUrl=serverUrl(0)))
        }
      })

    }

    def willUnmount() = Callback {
      mounted = false
      logger.finer("HomePage.willUnmount")
    }

  }

  def isPageFromLocalHost() = {
    import org.scalajs.dom.document

    val hostname = document.defaultView.location.hostname
    hostname == "localhost" || hostname == "loopback" || hostname == "127.0.0.1"

  }

  val logger = Logger("bridge.HomePage")

  private val component = ScalaComponent.builder[Props]("HomePage")
        .initialStateFromProps(props => State(debugging,ServerURL(Nil), None, false ))
        .backend( backendScope => new Backend(backendScope))
        .renderBackend
        .componentDidMount( scope => scope.backend.didMount())
        .componentWillUnmount( scope => scope.backend.willUnmount() )
        .build

  def apply( callbackPage: (AppPage)=>Callback ) = component(Props(callbackPage))

}
