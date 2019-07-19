package com.github.thebridsk.bridge.clientcommon.logging

import scala.scalajs.js
import japgolly.scalajs.react._
import scala.util.Success
import scala.util.Failure
import scala.scalajs.js.Date
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Level
// import japgolly.scalajs.react.extra.LogLifecycle

/**
 * @author werewolf
 */
object LogLifecycleToServer {

  private[this] def header(name: String): String => String =
    h => s"[$name] $h"

  private[this] def fmt(m: String, a: Any) =
    Seq[js.Any](s"\n  $m: $a")

  private[this] def log(m: js.Any, ps: js.Any*)(implicit level: Level, logger: Logger) = {
    val msg: String = ""+m+ (if (ps.length > 0) "  args="+ps.map { x => s"$x" }.mkString(", "); else "")
    CallbackTo { logger.log(level, msg) }
  }

  private[this] def logc(m: js.Any, c: js.Any, ps: js.Any*)(implicit level: Level, logger: Logger) =
    log(m + "\n ", /* c +: */ ps: _*)

  private[this] def log1(m: String)(implicit level: Level, logger: Logger) = (c: js.Any) =>
    logc(m, c)

//  private[this] def logp(m: String)(implicit level: Level, logger: Logger) = (c: js.Any, p: Any) =>
//    logc(m, c, fmt("Props", p): _*)

  private[this] def logps(m: String)(implicit level: Level, logger: Logger) = (c: js.Any, p: Any, s: Any) =>
    logc(m, c, fmt("Props", p) ++ fmt("State", s): _*)

  private[this] def logP[P <: Product](m: String, c: P => js.Any, extra: P => Seq[js.Any])(implicit level: Level, logger: Logger) = (p: P) =>
    logc(m, c(p), extra(p): _*)

//  def short[P, C <: Children, S, B]: ScalaComponent.Config[P, C, S, B] =
//    LogLifecycle.custom(componentName => lc =>
//      Callback.log(s"[$componentName] ${lc.toString.replaceFirst("\\(.+", "")}"))
//
//  def default[P, C <: Children, S, B]: ScalaComponent.Config[P, C, S, B] =
//    LogLifecycle.custom(componentName => lc =>
//      Callback.log(s"[$componentName] $lc"))
//
//  def verbose[P, C <: Children, S, B]: ScalaComponent.Config[P, C, S, B] =
//    LogLifecycle.custom(componentName => lc =>
//      Callback.log(s"[$componentName] $lc", lc.raw))

}
