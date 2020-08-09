package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.raw.FileList

object Utils {

  implicit class ExtendReactEventFromInput[E <: ReactEventFromInput](
      private val e: E
  ) extends AnyVal {
    def preventDefaultAction: E = { e.preventDefault(); e }
    def inputText[A](f: String => A): A = e.extract(_.target.value)(f)
    def inputFiles[A](f: FileList => A): A = e.extract(_.target.files)(f)
  }

  implicit class OptionalTag(private val flag: Boolean) extends AnyVal {
    @inline
    def ?=(tag: => TagMod) = if (flag) tag else EmptyVdom
  }

  implicit class BackendScopeWrapper[P, S](
      private val scope: BackendScope[P, S]
  ) extends AnyVal {

    def stateProps[X](cb: (S, P) => CallbackTo[X]): CallbackTo[X] = {
      scope.state >>= { state =>
        scope.props >>= { props =>
          cb(state, props)
        }
      }
    }
  }

}
