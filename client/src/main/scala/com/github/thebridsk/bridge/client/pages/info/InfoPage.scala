package com.github.thebridsk.bridge.client.pages.info

import org.scalajs.dom.document
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.AppRouter._
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.RootBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

/**
 * @author werewolf
 */
object InfoPage {
  import BaseStyles._

  private val log = Logger("bridge.InfoPage")

  case class Props( routeCtl: BridgeRouter[AppPage] )

  class Backend( scope: BackendScope[Props, Unit] ) {
    def render( props: Props ) = {
          val gotoPage = props.routeCtl.set _
          <.div(
            rootStyles.infoPageDiv,
            RootBridgeAppBar(
                Seq(MuiTypography(
                        variant = TextVariant.h6,
                        color = TextColor.inherit,
                    )(
                        <.span(
                          " Info",
                        )
                    )
                ),
                None,
                props.routeCtl
            )(),
            <.div(
              <.table( ^.width := "100%",
                  <.thead(
                    <.tr(
                      <.th( ^.textAlign:="left", "Property"), <.th(^.textAlign:="left", "Value")
                  )),
                  <.tbody(
                    info().map{ e => tablerow(e) }.toTagMod
                  )
              ),
              <.p(
  //              AppButton( "Cancel", "Cancel", ^.onClick --> gotoPage(Home)),
  //              " ",
                AppButton( "Refresh", "Refresh", ^.onClick --> scope.forceUpdate )
              )
            )
          )
        }

  }

  def info() = {
    val window = document.defaultView
    val nav = window.navigator
    val geoloc = nav.geolocation
    val screen = window.screen
    val styleMediaDefined = js.typeOf(window.styleMedia) != "undefined"
    val styleMedia = if (styleMediaDefined) window.styleMedia; else null
//    log.info( "InfoPage" )
    val i = List(
      ("Navigator.appName", nav.appName),
      ("Navigator.appVersion", nav.appVersion),
      ("Navigator.userAgent", nav.userAgent),
      ("Navigator.platform", nav.platform),
      ("Navigator.onLine", nav.onLine),
      ("Navigator.standalone", js.Dynamic.global.window.navigator.standalone),
      ("window.innerWidth", window.innerWidth),
      ("window.innerHeight", window.innerHeight),
      ("window.orientation", getOrientation()),
      ("isPortrait", isPortrait),
      ("Screen.width", screen.width),
      ("Screen.height", screen.height),
      ("Screen.availHeight", screen.availHeight),
      ("Screen.availWidth", screen.availWidth),
      ("Screen.colorDepth", screen.colorDepth),
      ("Screen.pixelDepth", screen.pixelDepth),
      ("typeOf(StyleMedia)", js.typeOf(window.styleMedia)),
      ("StyleMedia.type", (if (styleMediaDefined) styleMedia.`type`; else "???")),
      ("isTouchEnabled", isTouchEnabled),
      ("closed", window.asInstanceOf[js.Dynamic].closed)
//      ("", "")
    ).map{ case (key,value) =>
      val v=value.toString()
//      log.info(s"""  ${key} = ${v}""")
      (key,v)
    }
    log.info(s"InfoPage\n  ${i.map{ e => s"${e._1} = ${e._2}" }.mkString("\n  ")}")
    i
  }

  private val tablerow = ScalaComponent.builder[(String,String)]("InfoViewRow")
        .render_P{ props =>
          val (name,value) = props
//          log.info(s"""  ${name} = ${value}""")
          <.tr(
            <.td( name ),
            <.td( value )
          )
        }.build

  private val component = ScalaComponent.builder[Props]("InfoView")
        .stateless
        .renderBackend[Backend]
        .build

  def apply( routeCtl: BridgeRouter[AppPage] ) = component(Props(routeCtl))

  /**
   * window.orientation: (from http://www.williammalone.com/articles/html5-javascript-ios-orientation/)
   *   0 - portrait
   * 180 - portrait, upsidedown
   *  90 - landscape, counterclockwise
   * -90 - landscape, clockwise
   *
   */
  def getOrientation() = {
    js.Dynamic.global.window.orientation.toString match {
      case "undefined" => None
      case s => Some(s.toInt)
    }
  }

  def isPortrait = {
    val window = document.defaultView // js.Dynamic.global.window
    window.innerHeight / window.innerWidth > 1
  };

  def isLandscape = !isPortrait

  def isWindowsAsusTablet() = {
    // HACK Alert
    // screen is 1368x768, platform is Win32
    val s = js.Dynamic.global.window.screen
    val h = s.height.asInstanceOf[Int]
    val w = s.width.asInstanceOf[Int]
    val p = js.Dynamic.global.window.navigator.platform.asInstanceOf[String]
    (p=="Win32") && ((w==1368 && h==768)||(w==768 && h==1368))
  }

  def isTouchEnabled() = {
    val g = js.Dynamic.global.window
    !js.isUndefined(g.ontouchstart) || isWindowsAsusTablet()
  }

  val touchEnabled = isTouchEnabled()

  def showOnlyInLandscapeOnTouch() = {
    if (touchEnabled) baseStyles.hideInPortrait
    else baseStyles.alwaysHide
  }
}
