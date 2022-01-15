package com.github.thebridsk.bridge.clientcommon.javascript

import scala.scalajs.js

object ObjectToString {

  def objToString(obj: js.Object, indent: String = "", depth: Int = 3): String = {
    dynToString(obj.asInstanceOf[js.Dynamic], indent)
  }

  def dynToString(obj: js.Dynamic, indent: String = "", depth: Int = 3): String = {
    if (depth <= 0) {
      "<too deep>"
    } else if (js.isUndefined(obj)) {
      "<undefined>"
    } else if (obj == null) {
      "<null>"
    } else {
      val i = s"\n${indent}"
      val keys = js.Object.keys(obj.asInstanceOf[js.Object])
      if (keys.isEmpty) ""
      else {
        keys.map { key =>
          val v = obj.selectDynamic(key)
          val sv = anyToString(v, indent+"  ", depth-1)
          s"  ${key}: ${sv}"
        }.mkString(s"[${i}",s"${i},",s"${i}]")
      }
    }
  }

  def arrayToString[T <: js.Any](array: js.Array[T], indent: String = "", depth: Int = 3): String = {
    if (depth <= 0) {
      "<too deep>"
    } else {
      val i = s"\n${indent}"
      array.map { e =>
        anyToString(e, indent+"  ", depth-1)
      }.mkString(s"[${i}",s"${i},",s"${i}]")
    }
  }

  def anyToString(any: js.Any, indent: String = "", depth: Int = 3): String = {
    if (js.isUndefined(any)) {
      "<undefined>"
    } else if (any == null) {
      "<null>"
    } else {
      js.typeOf(any) match {
        case "object"   =>
          if (js.Array.isArray(any)) {
            s"array ${arrayToString(any.asInstanceOf[js.Array[js.Any]], indent+"  ", depth-1)}"
          } else {
            s"object ${dynToString(any.asInstanceOf[js.Dynamic], indent+"  ", depth-1)}"
          }
        case "function" =>
          s"<function> ${any}"
        case _          =>
          any.toString()
      }
    }
  }
}
