package com.github.thebridsk.bridge.client.pages

import com.github.thebridsk.bridge.client.routes.AppRouter._
import org.scalajs.dom.document
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.data.ServerVersion
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.controller.RubberController
import com.github.thebridsk.bridge.client.version.VersionClient
import com.github.thebridsk.bridge.data.version.VersionShared
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.AppRouter.About
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoModule.PlayChicago2
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.ListView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule.PlayDuplicate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.NewDuplicateView
import com.github.thebridsk.bridge.client.pages.rubber.RubberModule.PlayRubber
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.{ ListView => RubberListView}
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.NamesView
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.RubberMatchNamesView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.Popup
import com.github.thebridsk.bridge.clientcommon.rest2.RestResult
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.clientcommon.rest2.Result
import com.github.thebridsk.bridge.clientcommon.rest2.ResultHolder
import com.github.thebridsk.bridge.clientcommon.rest2.RequestCancelled
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.clientcommon.rest2.RestClient
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxFailure
import com.github.thebridsk.bridge.clientcommon.rest2.WrapperXMLHttpRequest
import com.github.thebridsk.bridge.clientcommon.react.AppButtonLink
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SelectMatchView
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.react.AppButtonLinkNewWindow
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.bridge.client.Bridge
import com.github.thebridsk.materialui.MuiButton
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.materialui.MuiMenu
import com.github.thebridsk.materialui.MuiMenuItem
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Element
import com.github.thebridsk.materialui.Variant
import com.github.thebridsk.materialui.Style
import com.github.thebridsk.materialui.Position
import com.github.thebridsk.materialui.MuiAppBar
import com.github.thebridsk.materialui.MuiToolbar
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.Style
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react.vdom.HtmlStyles
import com.github.thebridsk.materialui.MuiMenuList
import com.github.thebridsk.materialui.component.MyMenu
import com.github.thebridsk.materialui.PopperPlacement
import com.github.thebridsk.bridge.client.routes.AppRouter
import com.github.thebridsk.bridge.clientcommon.pages.GotoPage
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.client.bridge.store.ServerURLStore
import _root_.com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicateSummary
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.client.pages.info.InfoPage

/**
 * @author werewolf
 */
object HomePage {

  val loading = <.h1("Loading...")

  var debugging = false

  case class Props( routeCtl: BridgeRouter[AppPage])

  case class State(
      debugging: Boolean,
      working: Option[String],
      fastclickTest: Boolean,
      userSelect: Boolean = false,
      anchorMainEl: js.UndefOr[Element] = js.undefined,
      anchorMainTestHandEl: js.UndefOr[Element] = js.undefined,
      anchorHelpEl: js.UndefOr[Element] = js.undefined,
      gotoDuplicateList: Boolean = false,
      serverCert: Boolean = false
  ) {

    def openHelpMenu( n: Node ) = copy( anchorHelpEl = n.asInstanceOf[Element] )
    def closeHelpMenu() = copy( anchorHelpEl = js.undefined )

    def openMainMenu( n: Node ) = copy( anchorMainEl = n.asInstanceOf[Element] )
    def closeMainMenu() = copy( anchorMainEl = js.undefined, anchorMainTestHandEl = js.undefined )

    def openMainTestHandMenu( n: Node ) = copy( anchorMainTestHandEl = n.asInstanceOf[Element] )
    def closeMainTestHandMenu() = copy( anchorMainTestHandEl = js.undefined )

  }

  class Backend( scope: BackendScope[Props, State]) {

    val urlStoreListener = scope.forceUpdate

    val toggleDebug = scope.modState { s =>
      val newstate = s.copy(debugging = !s.debugging)
      debugging = newstate.debugging

      newstate.copy(debugging = false, working=Some("Debugging not enabled"))
    }

    val toggleUserSelect = { (event: ReactEvent) => scope.withEffectsImpure.modState { s =>
      val newstate = s.copy( userSelect = !s.userSelect )
      val style = Bridge.getElement("allowSelect")
      if (newstate.userSelect) {
        style.innerHTML = """
           |* {
           |  user-select: text;
           |}
           |""".stripMargin
      } else {
        style.innerHTML = ""
      }
      newstate
    }}

    def handleMainClick( event: ReactEvent ) = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openMainMenu(currentTarget)).runNow() )
    def handleMainCloseClick( event: ReactEvent ) = scope.modState(s => s.closeMainMenu()).runNow()
    def handleMainClose( /* event: js.Object, reason: String */ ) = {
      logger.fine(s"""Closing main menu""")
      scope.modStateOption { s =>
        if (s.anchorMainTestHandEl.isDefined) {
          None
        } else {
          Some(s.closeMainMenu())
        }
      }.runNow()
    }

    def handleHelpClick( event: ReactEvent ) = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openHelpMenu(currentTarget)).runNow() )
    def handleHelpClose( /* event: js.Object, reason: String */ ) = {
      logger.fine("HelpClose called")
      scope.modState(s => s.closeHelpMenu()).runNow()
    }

    def gotoPage( uri: String ) = {
      GotoPage.inNewWindow(uri)
    }

    def gotoView( page: AppRouter.AppPage ) = { (event: ReactEvent) =>
      logger.fine(s"""GotoView $page""")
      scope.withEffectsImpure.modState { (s,p) =>
        s.closeMainMenu()
      }
      scope.withEffectsImpure.props.routeCtl.set(page).runNow()
    }

    def handleMainGotoPageClick(uri: String)( event: ReactEvent ) = {
      logger.info(s"""Going to page ${uri}""")
      handleMainClose()

      gotoPage(uri)
    }

    def handleHelpGotoPageClick(uri: String)( event: ReactEvent ) = {
      logger.info(s"""Going to page ${uri}""")
      handleHelpClose()

      gotoPage(uri)
    }

    def handleHelpGotoPageClickSwaggerAPI( event: ReactEvent ) = {
      val uri = "/public/apidocs.html"
      logger.info(s"""Going to page ${uri}""")
      handleHelpClose()

      gotoPage(uri)
    }

    def handleTestHandClick( event: ReactEvent ) = event.extract(
                                                       _.currentTarget
                                                   )(
                                                       currentTarget => scope.modState( s =>
                                                         s.openMainTestHandMenu(currentTarget)).runNow()
                                                   )
    def handleMainTestHandClose( /* event: js.Object, reason: String */ ) = scope.modState(s => s.closeMainTestHandMenu()).runNow()
    def handleMainTestHandCloseClick( event: ReactEvent ) = scope.modState(s => s.closeMainTestHandMenu()).runNow()

    def render( props: Props, state: State ) = {

      def callbackPage(page: AppPage) = props.routeCtl.set(page)

      val toCancel = if (state.gotoDuplicateList) scope.modState( s => s.copy(working=None, gotoDuplicateList = false), callbackPage(PlayDuplicate(SummaryView)) )
                     else cancel

      val doingWork = state.working.getOrElse("")
      val isWorking = state.working.isDefined

      import japgolly.scalajs.react.vdom.VdomNode
      <.div(
        PopupOkCancel( if (isWorking) Some(doingWork) else None, None, Some( toCancel) ),
        RootBridgeAppBar(
            title = Seq(),
            helpurl = Some("../help/introduction.html"),
            routeCtl = props.routeCtl,
            showAPI = true
        )(),
        <.div(
          rootStyles.homeDiv,
          <.div(
            rootStyles.serverDiv,
            ^.id:="url",
            <.h1("Server"),
            <.ul(
              ServerURLStore.getURLItems
            ),
            state.serverCert ?= <.a(
              ^.href := "/servercert",
              if (InfoPage.isIpad()) "Install server certificate" else "Download server certificate"
            )
          ),
          <.div(
            rootStyles.gameDiv,
            <.h1("Play"),
            <.table(
              <.tbody(
                <.tr(
                  <.td( ^.width:="25%",
                    AppButton( "ChicagoList2", "Chicago List", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayChicago2(ListView)))
                  ),
                  <.td( ^.width:="25%",
                    AppButton( "Chicago2", "New Chicago", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick -->newChicago )
                  ),
                  <.td( ^.width:="25%"
                  ),
                  <.td( ^.width:="25%"
                  )
                ),
                <.tr(
                  <.td(
                    AppButton( "Duplicate", "Duplicate List", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayDuplicate(SummaryView)))
                  ),
                  <.td(
                    AppButton( "NewDuplicate", "New Duplicate", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayDuplicate(NewDuplicateView)))
                  ),
                  // <.td(
                  //   AppButton( "SelectDuplicate", "Select Match", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayDuplicate(SelectMatchView)))
                  // ),
                  <.td(
                    AppButton( "LatestNewMatch", "Latest New Match", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> latestNewMatch)
                  )
                ),
                <.tr(
                  <.td(
                    AppButton( "Rubber", "Rubber Bridge List", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(PlayRubber(RubberListView)))
                  ),
                  <.td(
                    AppButton( "NewRubber", "New Rubber Bridge", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> newRubber)
                  )
                ),
                <.tr(
                  <.td(
                    AppButton( "Import", "Import", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(ImportsList))
                  ),
                  <.td(
                    AppButton( "Export", "Export", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick --> callbackPage(Export))
                  ),
                  <.td(
                    {
                      val path = GotoPage.getURL( s"""/v1/diagnostics""" )
                      AppButtonLink( "Diagnostics", "Diagnostics", path,
                                     rootStyles.playButton,
                                     ^.disabled:=isWorking
                      )
                    }
                  )
                )
              )
            )
          ),
          isPageFromLocalHost() ?= <.div(
            rootStyles.miscDiv,
            <.h1("Miscellaneous"),
            <.table(
              <.tbody(
                <.tr(
                  <.td( ^.width:="25%",
                    isPageFromLocalHost() ?= AppButton(
                      "Shutdown", "Shutdown Server",
                      rootStyles.playButton,
                      ^.disabled:=isWorking,
                      ^.onClick --> doShutdown
                    )
                  ),
                  <.td( ^.width:="25%"
                  ),
                  <.td( ^.width:="25%"
                  ),
                  <.td( ^.width:="25%"
                  ),
                )
              )
            )
          )
        )
      )
    }

    val resultDuplicateSummary = ResultHolder[Array[DuplicateSummary]]()

    val latestNewMatch = setPopupTextCB( "Determining latest match", Callback {
      val list = RestClientDuplicateSummary.list().recordFailure()
      resultDuplicateSummary.set(list)
      list.onComplete( _ match {
        case Success(req) =>
          val fourHourAgo = SystemTime.currentTimeMillis() - 4*60*60*1000   // one hour ago
          val sorted = req.filter( md => md.created > fourHourAgo ).sortWith { (l,r) =>
            if (l.finished == r.finished) {
              if (l.onlyresult == r.onlyresult) {
                Id.idComparer(l.id,r.id) > 0
              } else {
                if (l.onlyresult) false
                else true
              }
            } else {
              if (l.finished) false
              else true
            }
          }
          sorted.headOption match {
            case Some(md) if !md.onlyresult && !md.finished =>
              gotoPage( PlayDuplicate(CompleteScoreboardView(md.id)) )
            case _ =>
              logger.severe("Did not find an unfinished duplicate match")
              setPopupText("Did not find an unfinished duplicate match", gotoDuplicateList = true )
          }

        case Failure(f) =>
          f match {
            case x: RequestCancelled =>
              // ignore this
            case _ =>
              logger.severe("Error trying to obtain list of duplicate matches: ",f)
              setPopupText(s"Error trying to obtain list of duplicate matches: ${f}", gotoDuplicateList = true )
          }
      })
    })

    def gotoPage( page: AppPage ) = scope.withEffectsImpure.props.routeCtl.set(page).runNow()

    /**
     * Sets the text in the working field.
     * Only call when not doing another modState or from a callback from a non GUI item.
     * @param text string to show as an error
     */
    def setPopupText( text: String, cb: Callback = Callback.empty, gotoDuplicateList: Boolean = false ) =
      scope.withEffectsImpure.modState( s => s.copy( working = Some(text), gotoDuplicateList = gotoDuplicateList ), cb )

    /**
     * Sets the text in the working field.
     * @param text string to show as an error
     */
    def setPopupTextCB( text: String, cb: Callback = Callback.empty ) = scope.modState( s => s.copy( working = Some(text) ), cb )

    val doShutdown = scope.modState( s => s.copy(working = Some("Sending shutdown command to server")), Callback {

      import com.github.thebridsk.bridge.clientcommon.rest2.RestClient._

      val url = "/v1/shutdown?doit=yes"

      val res = AjaxResult.post(url).recordFailure()
      resultShutdown.set(res)
      res.onComplete( _ match {
        case Success(req) =>
          if (req.status == 204) {
            gotoPage(ThankYou)
          } else {
            val resp = req.toRestMessage
            logger.severe(s"Error from server on shutdown action: ${resp}")
            setPopupText(s"Error from server: ${resp}")
          }
        case Failure(f) =>
          f match {
            case x: RequestCancelled =>
              // ignore this
            case _ =>
              logger.severe("Error trying to shutdown server: ",f)
              setPopupText(s"Error sending shutdown to server ${f}")
          }
      })
    })

    val resultChicago = ResultHolder[MatchChicago]()
    val resultShutdown = ResultHolder[WrapperXMLHttpRequest]()

    val cancel = Callback {
      resultChicago.cancel()
      resultShutdown.cancel()
      resultDuplicateSummary.cancel()
    } >> scope.modState( s => s.copy(working=None))

    import scala.concurrent.ExecutionContext.Implicits.global

    val newChicago = {
      setPopupTextCB("Working on creating a new Chicago match", Callback {
        val result = ChicagoController.createMatch()
        resultChicago.set(result)
        result.foreach { created =>
          logger.info(s"Got new chicago ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) gotoPage(PlayChicago2(NamesView(created.id,0)))
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              setPopupText("Failed to create a new Chicago match")
          }
        })
      })
    }

    val newRubber =
      setPopupTextCB("Working on creating a new rubber match", Callback {
        val result = RubberController.createMatch()
        result.foreach { created =>
          logger.info(s"Got new rubber ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) gotoPage(PlayRubber(RubberMatchNamesView(created.id)))
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              setPopupText("Failed to create a new Chicago match")
          }
        })
      })

    val newDuplicate =
      setPopupTextCB("Working on creating a new duplicate match", Callback {
        val result = Controller.createMatchDuplicate().recordFailure()
        result.foreach { created=>
          logger.info("Got new duplicate match ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) gotoPage(PlayDuplicate(CompleteScoreboardView(created.id)))
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              setPopupText("Failed to create a new duplicate match")
          }
        })
      })

    private var mounted = false

    val didMount = Callback {
      mounted = true
      // make AJAX rest call here
      logger.info("HomePage.didMount: Sending serverurl request to server")
      ServerURLStore.addChangeListener(urlStoreListener)
      ServerURLStore.updateURLs()
      AjaxResult.head("/servercert").onComplete { twxhr =>
        twxhr match {
          case Success(wxhr) =>
            if (wxhr.status == 200) {
              scope.withEffectsImpure.modState( s => s.copy(serverCert = true))
            } else {
              logger.info(s"""Error getting HEAD /servercert, status=${wxhr.status}""")
            }
          case Failure(exception) =>
            logger.info(s"""Error getting HEAD /servercert, exception: ${exception}""")
        }
      }
    }

    val willUnmount = Callback {
      mounted = false
      logger.finer("HomePage.willUnmount")
      ServerURLStore.removeChangeListener(urlStoreListener)
    }

  }

  def isPageFromLocalHost() = {
    import org.scalajs.dom.document

    val hostname = GotoPage.hostname
    hostname == "localhost" || hostname == "loopback" || hostname == "127.0.0.1"

  }

  val logger = Logger("bridge.HomePage")

  private val component = ScalaComponent.builder[Props]("HomePage")
        .initialStateFromProps { props =>
          logger.info("HomePage.initialStateFromProps")
          State(debugging, None, false )
        }
        .backend( backendScope => new Backend(backendScope))
        .renderBackend
        .componentDidMount( scope => scope.backend.didMount)
        .componentWillUnmount( scope => scope.backend.willUnmount )
        .build

  def apply( routeCtl: BridgeRouter[AppPage] ) = component(Props(routeCtl))

}
