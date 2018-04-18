package com.example.react

import org.scalajs.dom
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import com.example.color.Color
import japgolly.scalajs.react.vdom.Attr
import japgolly.scalajs.react.vdom.Attr.ValueType

object Utils {

  implicit class ExtendReactEventFromInput[E <: ReactEventFromInput](val e: E) extends AnyVal {
    def inputText[A]( f: String => A ): A = e.extract(_.target.value)(f)
  }

  implicit class OptionalTag( val flag: Boolean ) extends AnyVal {
    @inline
    def ?=( tag: =>TagMod ) = if (flag) tag else EmptyVdom
  }

  implicit class BackendScopeWrapper[P, S]( val scope: BackendScope[P, S] ) extends AnyVal {

    def stateProps[X]( cb: (S,P)=>CallbackTo[X] ) = {
      scope.state >>= { state =>
        scope.props >>= { props =>
          cb(state,props)
        }
      }
    }
  }

}
