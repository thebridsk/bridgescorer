package com.github.thebridsk.bridge.clientapi.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._


object ThankYouPage {
  import ThankYouPageInternal._

  case class Props()

  def apply(  ) = component(Props())  // scalafix:ok ExplicitResultTypes; ReactComponent

}

object ThankYouPageInternal {
  import ThankYouPage._

  private[pages]
  val component = ScalaComponent.builder[Props]("ThankYouPage")
                            .stateless
                            .noBackend
                            .render_P( props =>
                              <.div(
                                rootStyles.thankYouDiv,
                                <.h1("Thank you for using the Bridge Scorer"),
                                <.p("You can now close this window")
                              )
                            )
                            .build
}
