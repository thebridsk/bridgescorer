package com.github.thebridsk.bridge.client.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.clientcommon.pages.TitleSuits

object ThankYouPage {
  import ThankYouPageInternal._

  case class Props()

  def apply(  ) = component(Props())

}

object ThankYouPageInternal {
  import ThankYouPage._

  def exitFullscreen() = Callback {
    import org.scalajs.dom.document
    import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._

    if (document.isFullscreen) document.exitFullscreen()
  }

  val component = ScalaComponent.builder[Props]("ThankYouPage")
                            .stateless
                            .noBackend
                            .render_P( props =>
                              <.div(
                                rootStyles.thankYouDiv,
                                <.h1("Thank you for using the Bridge Scorer"),
                                <.p("You can now close this window"),
                                <.p( TitleSuits.suitspan)
                              )
                            )
                            .componentDidMount( scope => exitFullscreen() )
                            .componentDidUpdate( scope => exitFullscreen() )
                            .build
}
