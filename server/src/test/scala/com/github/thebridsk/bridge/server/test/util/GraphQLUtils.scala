package com.github.thebridsk.bridge.server.test.util

import play.api.libs.json.JsObject
import play.api.libs.json.JsString

object GraphQLUtils {

  def queryToJson( query: String, variables: Option[JsObject]= None, operation: Option[String] = None ): String = {
    JsObject(
        operation.map( v => "operationName" -> JsString(v) ).toList :::
        variables.map( v => "variables" -> v ).toList :::
        "query" -> JsString(query) :: Nil
    ).toString()

  }

}
