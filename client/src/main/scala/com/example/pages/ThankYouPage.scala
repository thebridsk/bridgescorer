package com.example.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object ThankYouPage {
  import ThankYouPageInternal._

  case class Props()

  def apply(  ) = component(Props())

}

object ThankYouPageInternal {
  import ThankYouPage._
  import BaseStyles._

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
