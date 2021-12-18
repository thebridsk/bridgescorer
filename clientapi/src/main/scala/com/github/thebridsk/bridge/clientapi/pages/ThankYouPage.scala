package com.github.thebridsk.bridge.clientapi.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.clientcommon.pages.TitleSuits
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.AppPage

object ThankYouPage {
  import ThankYouPageInternal._

  case class Props(
      routeCtl: BridgeRouter[AppPage]
  )

  def apply(
      routeCtl: BridgeRouter[AppPage]
  ) =
    component(
      Props(routeCtl)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object ThankYouPageInternal {
  import ThankYouPage._

  def exitFullscreen(): Callback =
    Callback {
      import org.scalajs.dom.document
      import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._

      if (document.myIsFullscreen) document.myExitFullscreen()
    }

  private[pages] val component = ScalaComponent
    .builder[Props]("ThankYouPage")
    .stateless
    .noBackend
    .render_P(props =>
      <.div(
        RootBridgeAppBar(
          title = Seq(),
          helpurl = Some("../help/introduction.html"),
          routeCtl = props.routeCtl
        )(),
        <.div(
          rootStyles.thankYouDiv,
          <.h1("Thank you for using the Bridge ScoreKeeper"),
          <.p("You can now close this window"),
          <.p(TitleSuits.suitspan)
        )
      )
    )
    .componentDidMount(scope => exitFullscreen())
    .componentDidUpdate(scope => exitFullscreen())
    .build
}
