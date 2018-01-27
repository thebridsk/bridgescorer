package com.example.rest2

import com.example.data.RestMessage
import org.scalajs.dom.ext.Ajax.InputData
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import java.net.URI
import org.scalactic.source.Position
import play.api.libs.json._
import com.example.data.rest.JsonSupport._
import scala.concurrent.ExecutionContext

/**
 * @tparam R the resource class
 *
 * @constructor
 * @param resourceURIfragment the URI for the resource.  It must not end in "/".
 *                            if it is a root resource, it should start with "/".
 *                            if it is a nested resource, it should NOT start with "/"
 * @param parentClient the RestClient for the parent resource, None if root resource
 * @param parentInstance the id of the parent resource, None if root resource
 */
class RestClient[R]( val resourceURIfragment: String,
                     parentClient: Option[RestClient[_]] = None,
                     parentInstance: Option[String]=None
                   )( implicit reader: Reads[R],
                               writer: Writes[R],
                               classtag: ClassTag[R],
                               executor: ExecutionContext
                   ) {

  def getURI( id: String ): String = {
    resourceURI+"/"+id
  }

  def resourceURI: String = parentClient match {
    case Some(parent) => parent.getURI(parentInstance.get)+"/"+resourceURIfragment
    case None => resourceURIfragment
  }

  import scala.language.implicitConversions
  @inline
  implicit def RToInputData( r: R ): InputData = writeJson(r)

  @inline
  implicit def StringToR( data: String ) = readJson[R](data)

  implicit def ajaxToRestResult( ajaxResult: AjaxResult ) = {
    RestResult.ajaxToRestResult(ajaxResult)
  }

  implicit def ajaxToRestResultUnit( ajaxResult: AjaxResult ): RestResult[Unit] = {
    RestResult.ajaxToRestResult(ajaxResult)
  }

  implicit def ajaxToRestResultArray( ajaxResult: AjaxResult ) = {
    RestResult.ajaxToRestResultArray(ajaxResult)
  }

  val headersForPost=Map("Content-Type" -> "application/json; charset=UTF-8",
                         "Accept" -> "application/json")

  val headersForGet=Map( "Accept" -> "application/json")

  def getQueryString( query: Map[String,String] ): String = {
    if (query.isEmpty) {
      ""
    } else {
      query.map(pair => pair._1+"="+pair._2).mkString("?","&","")
    }
  }

  def getURL( id: String, query: Map[String,String] = Map() ): String = {
    getURI(id)+getQueryString(query)
  }

  def getURL( query: Map[String,String] ): String = {
    resourceURI+getQueryString(query)
  }

  def getURL(): String = {
    resourceURI+getQueryString(Map())
  }

  def create( obj: R,
              query: Map[String, String] = Map.empty,
              headers: Map[String, String] = headersForPost,
              timeout: Duration = AjaxResult.defaultTimeout
            )( implicit xpos: Position): RestResult[R] = {
    AjaxResult.post(getURL(query), obj, timeout, headers)
  }

  def list( query: Map[String, String] = Map.empty,
            headers: Map[String, String] = headersForGet,
            timeout: Duration = AjaxResult.defaultTimeout
         )( implicit pos: Position): RestResultArray[R] = {
    val r = AjaxResult.get(getURL(query), timeout=timeout, headers=headers)
    r
  }

  def get( id: String,
           query: Map[String, String] = Map.empty,
           headers: Map[String, String] = headersForGet,
           timeout: Duration = AjaxResult.defaultTimeout
         )( implicit pos: Position): RestResult[R] = {
    AjaxResult.get(getURL(id,query), timeout=timeout, headers=headers)
  }



  def update( id: String,
              obj: R,
              query: Map[String, String] = Map.empty,
              headers: Map[String, String] = headersForPost,
              timeout: Duration = AjaxResult.defaultTimeout
            )( implicit pos: Position): RestResult[Unit] = {
    AjaxResult.put(getURL(id,query), data=obj, timeout=timeout, headers=headers)
  }

  def delete( id: String,
              query: Map[String, String] = Map.empty,
              headers: Map[String, String] = headersForGet,
              timeout: Duration = AjaxResult.defaultTimeout
            )( implicit pos: Position): RestResult[Unit] = {
    AjaxResult.delete(getURL(id,query), timeout=timeout, headers=headers)
  }

}

object RestClient {
  implicit class RestMessages( val req: WrapperXMLHttpRequest ) extends AnyVal {
    def toRestMessage = {
      (try {
        Some(req.responseText)
      } catch {
        case x: Throwable =>
          None
      }) match {
        case Some(r) =>
          try {
            readJson[RestMessage]( r ).msg
          } catch {
            case x: Throwable =>
              r
          }
        case None =>
          "Unknown error"
      }
    }
  }
}
