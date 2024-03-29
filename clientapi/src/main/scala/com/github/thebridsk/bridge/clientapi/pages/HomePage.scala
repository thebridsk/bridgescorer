package com.github.thebridsk.bridge.clientapi.pages

import com.github.thebridsk.bridge.clientapi.routes.AppRouter._
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerURL
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.rest2.ResultHolder
import com.github.thebridsk.bridge.clientcommon.rest2.RequestCancelled
import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.rest2.WrapperXMLHttpRequest
//import com.github.thebridsk.bridge.clientapi.fastclick.FastClick
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.logger.Info
import org.scalajs.dom.Node
import org.scalajs.dom.Element
import com.github.thebridsk.bridge.clientapi.routes.AppRouter
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._

/**
  * @author werewolf
  */
object HomePage {

  var debugging = false

  case class Props(routeCtl: BridgeRouter[AppPage])

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

    def openHelpMenu(n: Node): State =
      copy(anchorHelpEl = n.asInstanceOf[Element])
    def closeHelpMenu(): State = copy(anchorHelpEl = js.undefined)

    def openMainMenu(n: Node): State =
      copy(anchorMainEl = n.asInstanceOf[Element])
    def closeMainMenu(): State =
      copy(anchorMainEl = js.undefined, anchorMainTestHandEl = js.undefined)

    def openMainTestHandMenu(n: Node): State =
      copy(anchorMainTestHandEl = n.asInstanceOf[Element])
    def closeMainTestHandMenu(): State =
      copy(anchorMainTestHandEl = js.undefined)

  }

  class Backend(scope: BackendScope[Props, State]) {

//    val toggleFastclickTest = scope.modState( s => s.copy( fastclickTest = !s.fastclickTest) )

//    val toggleFastclick = fastclickToggle >> scope.forceUpdate

    val toggleDebug: Callback = scope.modState { s =>
      val newstate = s.copy(debugging = !s.debugging)
      debugging = newstate.debugging

      newstate.copy(debugging = false, working = Some("Debugging not enabled"))
    }

    val toggleUserSelect: ReactEvent => js.Any = { (event: ReactEvent) =>
      scope.withEffectsImpure.modState { s =>
        val newstate = s.copy(userSelect = !s.userSelect)
        val style = Info.getElement("allowSelect")
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
      }
    }

    def handleMainClick(event: ReactEvent): Unit =
      event.extract(_.currentTarget)(currentTarget =>
        scope.modState(s => s.openMainMenu(currentTarget)).runNow()
      )
    def handleMainCloseClick(event: ReactEvent): Unit =
      scope.modState(s => s.closeMainMenu()).runNow()
    def handleMainClose( /* event: js.Object, reason: String */ ): Unit = {
      logger.fine(s"""Closing main menu""")
      scope
        .modStateOption { s =>
          if (s.anchorMainTestHandEl.isDefined) {
            None
          } else {
            Some(s.closeMainMenu())
          }
        }
        .runNow()
    }

    def handleHelpClick(event: ReactEvent): Unit =
      event.extract(_.currentTarget)(currentTarget =>
        scope.modState(s => s.openHelpMenu(currentTarget)).runNow()
      )
    def handleHelpClose( /* event: js.Object, reason: String */ ): Unit = {
      logger.fine("HelpClose called")
      scope.modState(s => s.closeHelpMenu()).runNow()
    }

    def gotoPage(uri: String): Unit = {
      GotoPage.inNewWindow(uri)
    }

    def gotoView(page: AppRouter.AppPage): ReactEvent => Unit = {
      (event: ReactEvent) =>
        logger.fine(s"""GotoView $page""")
        scope.withEffectsImpure.modState { (s, p) =>
          s.closeMainMenu()
        }
        scope.withEffectsImpure.props.routeCtl.set(page).runNow()
    }

    def handleMainGotoPageClick(uri: String)(event: ReactEvent): Unit = {
      logger.info(s"""Going to page ${uri}""")
      handleMainClose()

      gotoPage(uri)
    }

    def handleHelpGotoPageClick(uri: String)(event: ReactEvent): Unit = {
      logger.info(s"""Going to page ${uri}""")
      handleHelpClose()

      gotoPage(uri)
    }

    def handleHelpGotoPageClickSwaggerAPI(event: ReactEvent): Unit = {
      val uri = "/public/apidocs.html"
      logger.info(s"""Going to page ${uri}""")
      handleHelpClose()

      gotoPage(uri)
    }

    def handleTestHandClick(event: ReactEvent): Unit =
      event.extract(
        _.currentTarget
      )(currentTarget =>
        scope.modState(s => s.openMainTestHandMenu(currentTarget)).runNow()
      )
    def handleMainTestHandClose(
        /* event: js.Object, reason: String */
    ): Unit = scope.modState(s => s.closeMainTestHandMenu()).runNow()
    def handleMainTestHandCloseClick(event: ReactEvent): Unit =
      scope.modState(s => s.closeMainTestHandMenu()).runNow()

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

      def callbackPage(page: AppPage) = props.routeCtl.set(page)

      val doingWork = state.working.getOrElse("")
      val isWorking = state.working.isDefined

      <.div(
        PopupOkCancel(
          if (isWorking) Some(doingWork) else None,
          None,
          Some(cancel)
        ),
        RootBridgeAppBar(
          title = Seq(),
          helpurl = Some("../help/introduction.html"),
          routeCtl = props.routeCtl
        )(),
        <.div(
          rootStyles.homeDiv,
          <.div(
            rootStyles.serverDiv,
            ^.id := "url",
            <.h1("Server"),
            <.ul(
              if (BridgeDemo.isDemo) {
                <.li(
                  "Demo mode, all data entered will be lost on page refresh or closing page"
                )
              } else {
                if (state.serverUrl.serverUrl.isEmpty) {
                  <.li("No network interfaces found")
                } else {
                  state.serverUrl.serverUrl.map { url => <.li(url) }.toTagMod
                }
              }
            )
          ),
          <.div(
            rootStyles.gameDiv
            // <.h1("Play"),
          ),
          //        <.div(
          //          rootStyles.testHandsDiv,
          //        ),
          isPageFromLocalHost() ?= <.div(
            rootStyles.miscDiv,
            <.h1("Miscellaneous"),
            <.table(
              <.tbody(
                <.tr(
                  //                <.td( ^.width:="25%",
                  //                  {
                  //                    AppButton(
                  //                      "FastclickTest", "Fast Click Test",
                  //                      rootStyles.playButton,
                  //                      ^.disabled:=isWorking,
                  //                      BaseStyles.highlight(selected = state.fastclickTest),
                  //                      ^.onClick --> toggleFastclickTest
                  //                    )
                  //                  }
                  //                ),
                  //                <.td( ^.width:="25%",
                  //                  AppButton(
                  //                    "ToggleFastclick", "Fast Click",
                  //                    rootStyles.playButton,
                  //                    BaseStyles.highlight( selected = isFastclickOn ),
                  //                    ^.disabled:=isWorking,
                  //                    ^.onClick --> toggleFastclick
                  //                  )
                  //                ),
                  <.td(
                    ^.width := "25%",
                    isPageFromLocalHost() ?= AppButton(
                      "Shutdown",
                      "Shutdown Server",
                      rootStyles.playButton,
                      ^.disabled := isWorking,
                      ^.onClick --> doShutdown
                    )
                  ),
                  <.td(^.width := "25%"),
                  <.td(^.width := "25%"),
                  <.td(^.width := "25%")
                )
                //              <.tr(
                //                <.td(" ")
                //              ),
              )
            )
          )
        )
      )
    }

    def gotoPage(page: AppPage): Unit =
      scope.withEffectsImpure.props.routeCtl.set(page).runNow()

    /**
      * Sets the text in the working field.
      * Only call when not doing another modState or from a callback from a non GUI item.
      * @param text string to show as an error
      */
    def setPopupText(text: String, cb: Callback = Callback.empty): Unit =
      scope.withEffectsImpure.modState(s => s.copy(working = Some(text)), cb)

    /**
      * Sets the text in the working field.
      * @param text string to show as an error
      */
    def setPopupTextCB(text: String, cb: Callback = Callback.empty): Callback =
      scope.modState(s => s.copy(working = Some(text)), cb)

    val doShutdown: Callback = scope.modState(
      s => s.copy(working = Some("Sending shutdown command to server")),
      Callback {

        import com.github.thebridsk.bridge.clientcommon.rest2.RestClient._

        val url = "/v1/shutdown?doit=yes"

        val res = AjaxResult.post(url).recordFailure()
        resultShutdown.set(res)
        res.onComplete(_ match {
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
                logger.severe("Error trying to shutdown server: ", f)
                setPopupText(s"Error sending shutdown to server ${f}")
            }
        })
      }
    )

    val resultShutdown: ResultHolder[WrapperXMLHttpRequest] =
      ResultHolder[WrapperXMLHttpRequest]()

    val cancel: Callback = Callback {
      resultShutdown.cancel()
    } >> scope.modState(s => s.copy(working = None))

    import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global

    private var mounted = false

    val didMount: Callback = Callback {
      mounted = true
      // make AJAX rest call here
      logger.info("HomePage.didMount: Sending serverurl request to server")
      RestClientServerURL
        .list()
        .recordFailure()
        .foreach(serverUrl => {
          if (mounted) {
            scope.withEffectsImpure
              .modState(s => s.copy(serverUrl = serverUrl(0)))
          }
        })
    }

    val willUnmount: Callback = Callback {
      mounted = false
      logger.finer("HomePage.willUnmount")
    }

  }

  def isPageFromLocalHost(): Boolean = {

    val hostname = GotoPage.hostname
    hostname == "localhost" || hostname == "loopback" || hostname == "127.0.0.1"

  }

  val logger: Logger = Logger("bridge.HomePage")

  private val component = ScalaComponent
    .builder[Props]("HomePage")
    .initialStateFromProps { props =>
      logger.info("HomePage.initialStateFromProps")
      State(debugging, ServerURL(Nil), None, false)
    }
    .backend(backendScope => new Backend(backendScope))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build

  def apply(routeCtl: BridgeRouter[AppPage]) =
    component(
      Props(routeCtl)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}
