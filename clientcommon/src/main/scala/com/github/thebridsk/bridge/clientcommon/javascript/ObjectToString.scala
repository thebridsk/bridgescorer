package com.github.thebridsk.bridge.clientcommon.javascript

import scala.scalajs.js

object ObjectToString {

  def objToString(obj: js.Object, indent: String = "", depth: Int = 3): String = {
    dynToString(obj.asInstanceOf[js.Dynamic], indent)
  }

  def dynToString(obj: js.Dynamic, indent: String = "", depth: Int = 3): String = {
    if (depth == 0) {
      ""
    } else if (js.isUndefined(obj)) {
      "<undefined>"
    } else if (obj == null) {
      "<null>"
    } else {
      val i = s"\n${indent}"
      js.Object.keys(obj.asInstanceOf[js.Object]).map { key =>
        val v = obj.selectDynamic(key)
        val sv = if (js.isUndefined(v) || v == null) {
          "<undefined>"
        } else {
          js.typeOf(v) match {
            case "object"   => s"object${dynToString(v, indent+"  ", depth-1)}"
            case "function" => "<function>"
            case _          => v.toString()
          }
        }
        s"${key}: ${sv}"
      }.mkString(i, i, "")
    }
  }
}
