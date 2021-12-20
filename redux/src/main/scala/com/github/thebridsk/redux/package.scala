package com.github.thebridsk

import scala.scalajs.js
import scala.scalajs.js.|

/**
 * Inspired by https://github.com/vegansk/scalajs-redux
 */
package object redux {

  type State = js.Object

  type Reducer[S] = js.Function2[js.UndefOr[S], NativeAction[Action], S]

  type Unsubscribe = js.Function0[Unit]
  type Listener = js.Function0[Unit]

  type StoreCreator[S <: State] = js.Function3[
    /* reducer */        Reducer[S],
    /* preloadedState */ S,
    /* enhancer */       js.Function, // Enhancer[S],
    /* return */         Store[S]
  ]

  type Enhancer[S <: State] = js.Function1[StoreCreator[S], StoreCreator[S]]

  type DispatchBasic = js.Function1[NativeAction[Action], NativeAction[Action]]
  type Dispatch[S <: State] = js.Function1[NativeAction[Action]|thunk.Function[S], NativeAction[Action]]

  type Middleware[S <: State] = js.Function1[
    /* api */ MiddlewareAPI[S],
    js.Function1[
      /* next */ Dispatch[S],
      js.Function1[
        /* action */ js.Any,
        js.Any
      ]
    ]
  ]
}
