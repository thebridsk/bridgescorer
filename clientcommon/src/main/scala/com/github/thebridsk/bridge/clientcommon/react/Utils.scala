package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.raw.FileList
import japgolly.scalajs.react.component.Js.UnmountedWithRoot

/**
  * A bunch of implicit classes
  */
object Utils {

  /** utility methods that are added to event types to make handling the
    * event value easier.
    *
    * @param E the event type.  Must be a subclass of ReactEventFromInput.
    *
    * @param e the event
    */
  implicit class ExtendReactEventFromInput[E <: ReactEventFromInput](
      private val e: E
  ) extends AnyVal {
    /**
      * Stops the default action of an element from happening. For example: Prevent a submit button from submitting a form Prevent a link from following the URL
      *
      * @return the event
      */
    def preventDefaultAction: E = { e.preventDefault(); e }

    /**
      * Extracts the target value from the event, and calls the specified function.
      * If a Callback is handling the event, then the target object will not be
      * available when the Callback is executed.
      *
      * @param A the return type from the function.
      *
      * @param f function to call with the target value.
      *
      * @return the return from the specified function, f.
      */
    def inputText[A](f: String => A): A = e.extract(_.target.value)(f)

    /**
      * Extracts the target value, which is a list of filename, from the event,
      * and calls the specified function.
      * If a Callback is handling the event, then the target object will not be
      * available when the Callback is executed.
      *
      * @param A the return type from the function.
      *
      * @param f function to call with the target value.
      *
      * @return the return from the specified function, f.
      */
      def inputFiles[A](f: FileList => A): A = e.extract(_.target.files)(f)
  }

  /**
    * Wrapper to a boolean to help create optional TagMods.
    *
    * @param flag
    */
  implicit class OptionalTag(private val flag: Boolean) extends AnyVal {
    /**
      * Returns the specified TagMod, tag, when the boolean is true,
      * otherwise returns an empty VDom element.
      */
    @inline
    def ?=(tag: => TagMod) = if (flag) tag else EmptyVdom
  }

  /**
    * Wrapper to a BackendScope to add helper methods.
    *
    * @param scope
    */
  implicit class BackendScopeWrapper[P, S](
      private val scope: BackendScope[P, S]
  ) extends AnyVal {

    /**
      * Get the state and properties from the scope.
      *
      * @param X the return type of the CallbackTo
      *
      * @param cb callback that takes a state and properties
      *
      * @return a callback to X
      */
    def stateProps[X](cb: (S, P) => CallbackTo[X]): CallbackTo[X] = {
      scope.state >>= { state =>
        scope.props >>= { props =>
          cb(state, props)
        }
      }
    }
  }

  implicit class wrapUnmountedWithRoot(
    val v: UnmountedWithRoot[_,_,_,_]
  ) extends AnyVal {
    def toTagMod: TagMod = v
  }


}
