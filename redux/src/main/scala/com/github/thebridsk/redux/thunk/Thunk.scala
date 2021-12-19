package com.github.thebridsk.redux

import scala.scalajs.js
import com.github.thebridsk.redux.Middleware
import com.github.thebridsk.redux.State
import com.github.thebridsk.redux.DispatchBasic

package object thunk {

  type Function[S <: State] = js.Function3[
    /* dispatch */ DispatchBasic,
    /* getState */ js.Function0[S],
    /* extraArgs */ js.Object,
    /* return */ Unit
  ]


  @js.native
  @js.annotation.JSImport("redux-thunk", js.annotation.JSImport.Namespace)
  object Thunk extends js.Object {

    @js.annotation.JSName("default")
    def thunk[S <: State]: Middleware[S] = js.native

  }

  object ThunkWithArgs {

    def withExtraArgument[S <: State](extraArgs: js.UndefOr[js.Dictionary[js.Any]]): Middleware[S] = {
      val t = Thunk.thunk.asInstanceOf[js.Dynamic].withExtraArgument.asInstanceOf[js.Function1[js.UndefOr[js.Dictionary[js.Any]], Middleware[S]]]
      t(extraArgs)
    }

  }
}
