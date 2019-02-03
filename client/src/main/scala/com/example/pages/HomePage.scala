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
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.BridgeRouter
import com.example.react.AppButtonLinkNewWindow
import com.example.react.HelpButton
import com.example.Bridge
import com.example.materialui.MuiButton
import com.example.materialui.ColorVariant
import com.example.materialui.MuiMenu
import com.example.materialui.MuiMenuItem
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Element
import com.example.materialui.Variant
import com.example.materialui.Style
import com.example.materialui.Position
import com.example.materialui.MuiAppBar
import com.example.materialui.MuiToolbar
import com.example.materialui.MuiIconButton
import com.example.materialui.ColorVariant
import com.example.materialui.icons.MuiMenuIcon
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import com.example.materialui.icons.MuiHelpIcon
import com.example.materialui.Style
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react.vdom.HtmlStyles
import com.example.materialui.MuiMenuList
import com.example.materialui.component.MyMenu
import com.example.materialui.PopperPlacement
import com.example.routes.AppRouter
import com.example.materialui.icons.MuiCheckIcon
import com.example.materialui.icons.SvgColor
import com.example.materialui.icons.MuiChevronRightIcon

/**
 * @author werewolf
 */
object HomePage {

  var debugging = false

  case class Props( routeCtl: BridgeRouter[AppPage])

  case class State(
      debugging: Boolean,
      serverUrl: ServerURL,
      working: Option[String],
      fastclickTest: Boolean,
      userSelect: Boolean = false,
      anchorMainEl: js.UndefOr[Element] = js.undefined,
      anchorMainTestHandEl: js.UndefOr[Element] = js.undefined,
      anchorHelpEl: js.UndefOr[Element] = js.undefined
  ) {

    def openHelpMenu( n: Node ) = copy( anchorHelpEl = n.asInstanceOf[Element] )
    def closeHelpMenu() = copy( anchorHelpEl = js.undefined )

    def openMainMenu( n: Node ) = copy( anchorMainEl = n.asInstanceOf[Element] )
    def closeMainMenu() = copy( anchorMainEl = js.undefined, anchorMainTestHandEl = js.undefined )

    def openMainTestHandMenu( n: Node ) = copy( anchorMainTestHandEl = n.asInstanceOf[Element] )
    def closeMainTestHandMenu() = copy( anchorMainTestHandEl = js.undefined )

  }

  var fastclick: Option[FastClick] = None

  startFastClick()

//  // This will be the props object used from JS-land
//  @js.native
//  trait JsProps extends js.Object {
//    val classes: js.Dictionary[String]
//  }
//
//  val renderWithStyle = ScalaComponent.builder[js.Object]("Title")
//                            .stateless
//                            .noBackend
//                            .render_P { props =>
//                              val p = props.asInstanceOf[JsProps]
//                              logger.info(s"""renderWithStyle called with props $props""")
//                              val clsGrow = p.classes("grow")
//                              MuiTypography(
//                                  variant = TextVariant.h6,
//                                  color = TextColor.inherit,
//                                  className = clsGrow
//                              )(
//                                  "Bridge ScoreKeeper"
//                              )
//                            }
//                            .build
//
//  def renderWithStyleFn( props: js.Object ) = {
//                              logger.info(s"""renderWithStyleFn with props $props""")
//                              renderWithStyle(props)
//                            }

  val fastclickToggle = Callback {
    fastclick match {
      case Some(fc) => stopFastClick()
      case None => startFastClick()

    }
  }

  def startFastClick() {
    fastclick = Some(fastclick.map(f=>f).getOrElse( FastClick() ))
  }

  def stopFastClick() {
    fastclick.foreach(_.destroy())
    fastclick = None
  }

  def isFastclickOn = fastclick.isDefined

  class Backend( scope: BackendScope[Props, State]) {

    val toggleFastclickTest = scope.modState( s => s.copy( fastclickTest = !s.fastclickTest) )

    val toggleFastclick = fastclickToggle >> scope.forceUpdate

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
      import BaseStyles._

      def callbackPage(page: AppPage) = props.routeCtl.set(page)

      val doingWork = state.working.getOrElse("")
      val isWorking = state.working.isDefined

      import japgolly.scalajs.react.vdom.VdomNode
      <.div(
        rootStyles.homeDiv,
        PopupOkCancel( if (isWorking) Some(doingWork) else None, None, Some(cancel) ),
        <.div(
          rootStyles.serverDiv,
          ^.id:="url",
          <.div(
            RootBridgeAppBar(
                title = Seq(),
                helpurl = Some("../help/introduction.html"),
                routeCtl = props.routeCtl
            )()
          ),
          <.h1("Server"),
          <.ul(
            if (Bridge.isDemo) {
              <.li("Demo mode, all data entered will be lost on page refresh or closing page")
            } else {
              if (state.serverUrl.serverUrl.isEmpty) {
                <.li("No network interfaces found")
              } else {
                state.serverUrl.serverUrl.map{ url => <.li(url) }.toTagMod
              }
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
                  AppButton( "Chicago2", "New Chicago", rootStyles.playButton, ^.disabled:=isWorking, ^.onClick -->newChicago )
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
        <.div(
          rootStyles.testHandsDiv,
        ),
        <.div(
          rootStyles.miscDiv,
          <.h1("Miscellaneous"),
          <.table(
            <.tbody(
              <.tr(
                <.td( ^.width:="25%",
                  {
                    AppButton(
                      "FastclickTest", "Fast Click Test",
                      rootStyles.playButton,
                      ^.disabled:=isWorking,
                      BaseStyles.highlight(selected = state.fastclickTest),
                      ^.onClick --> toggleFastclickTest
                    )
                  }
                ),
                <.td( ^.width:="25%",
                  AppButton(
                    "ToggleFastclick", "Fast Click",
                    rootStyles.playButton,
                    BaseStyles.highlight( selected = isFastclickOn ),
                    ^.disabled:=isWorking,
                    ^.onClick --> toggleFastclick
                  )
                ),
                <.td( ^.width:="25%",
                  isPageFromLocalHost() ?= AppButton(
                    "Shutdown", "Shutdown Server",
                    rootStyles.playButton,
                    ^.disabled:=isWorking,
                    ^.onClick --> doShutdown
                  )
                )
              ),
              <.tr(
                <.td(" ")
              ),
            )
          )
        )
      )
    }

    def gotoPage( page: AppPage ) = scope.withEffectsImpure.props.routeCtl.set(page).runNow()

    /**
     * Sets the text in the working field.
     * Only call when not doing another modState or from a callback from a non GUI item.
     * @param text string to show as an error
     */
    def setPopupText( text: String, cb: Callback = Callback.empty ) = scope.withEffectsImpure.modState( s => s.copy( working = Some(text) ), cb )

    /**
     * Sets the text in the working field.
     * @param text string to show as an error
     */
    def setPopupTextCB( text: String, cb: Callback = Callback.empty ) = scope.modState( s => s.copy( working = Some(text) ), cb )

    val doShutdown = scope.modState( s => s.copy(working = Some("Sending shutdown command to server")), Callback {

      import com.example.rest2.RestClient._

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
      RestClientServerURL.list().recordFailure().foreach( serverUrl => {
        if (mounted) {
          scope.withEffectsImpure.modState( s => s.copy(serverUrl=serverUrl(0)))
        }
      })
    }

    val willUnmount = Callback {
      mounted = false
      logger.finer("HomePage.willUnmount")
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
          State(debugging,ServerURL(Nil), None, false )
        }
        .backend( backendScope => new Backend(backendScope))
        .renderBackend
        .componentDidMount( scope => scope.backend.didMount)
        .componentWillUnmount( scope => scope.backend.willUnmount )
        .build

  def apply( routeCtl: BridgeRouter[AppPage] ) = component(Props(routeCtl))

}
