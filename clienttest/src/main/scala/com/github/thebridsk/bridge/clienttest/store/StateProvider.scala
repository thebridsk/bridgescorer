package com.github.thebridsk.bridge.clienttest.routes

import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import scala.scalajs.js.|

import com.github.thebridsk.redux.State
import com.github.thebridsk.redux.Store
import com.github.thebridsk.redux.Dispatch
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.facade.React.{
  ComponentType => fComponentType
}
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.javascript.ObjectToString

object StateProvider {
  private val logger: Logger = Logger("base.StateProvider")

  @js.native @JSImport(
    "react-redux",
    "Provider"
  ) private object Provider extends js.Any

  private val component =
    JsComponent[js.Object, Children.Varargs, Null](
      Provider
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def provide[S <: State](
    store: Store[S]
  )(
    children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p = js.Dictionary(
      "store" -> store
    ).asInstanceOf[js.Object]
    component(p)(children:_*)
  }

  type MapStateToProps[S <: State, P <: js.Object] = js.Function2[S, P, js.Object] | js.Function1[S, js.Object]
  type MapDispatchToProps[S <: State] = js.Object | js.Function2[Dispatch[S], js.Object, js.Object]
  type MergeProps = js.Function3[js.Object, js.Object, js.Object, js.Object]

  // See https://react-redux.js.org/api/connect
  @js.native @JSImport(
    "react-redux",
    "connect"
  )
  private def connectRaw[
    S <: State,
    P <: js.Object
  ](
    mapStateToProps: js.UndefOr[MapStateToProps[S, P]],
    mapDispatchToProps: js.UndefOr[MapDispatchToProps[S]],
    mergeProps: js.UndefOr[MergeProps],
    options: js.UndefOr[js.Object]
  )(
    childComponent: P => js.Any // CtorType.ChildArg
  ): js.Any = js.native

  def connect[
    S <: State,
    P <: js.Object
  ](
    mapStateToProps: js.UndefOr[MapStateToProps[S, P]] = js.undefined,
    mapDispatchToProps: js.UndefOr[MapDispatchToProps[S]] = js.undefined,
    mergeProps: js.UndefOr[MergeProps] = js.undefined,
    options: js.UndefOr[js.Object] = js.undefined
  )(
    childComponent: P => js.Any
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    logger.info(s"StateProvider.connect, child=${ObjectToString.anyToString(childComponent)}")

    val c = connectRaw(mapStateToProps,mapDispatchToProps,mergeProps,options)(childComponent)
    // c
    // c.asInstanceOf[VdomNode]
    JsComponent[P, Children.None, S](c)
  }

  def connectWithChildren[
    S <: State,
    P <: js.Object
  ](
    mapStateToProps: js.UndefOr[MapStateToProps[S, P]] = js.undefined,
    mapDispatchToProps: js.UndefOr[MapDispatchToProps[S]] = js.undefined,
    mergeProps: js.UndefOr[MergeProps] = js.undefined,
    options: js.UndefOr[js.Object] = js.undefined
  )(
    childComponentWithChildren: P => js.Any
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    logger.info(s"StateProvider.connectWithChildren, child=${ObjectToString.anyToString(childComponentWithChildren)}")

    val c = connectRaw(mapStateToProps,mapDispatchToProps,mergeProps,options)(childComponentWithChildren)
    // c
    // c.asInstanceOf[VdomNode]
    JsComponent[P, Children.Varargs, S](c)
  }

}
