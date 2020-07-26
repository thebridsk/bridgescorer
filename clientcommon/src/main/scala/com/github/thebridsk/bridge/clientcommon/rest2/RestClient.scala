package com.github.thebridsk.bridge.clientcommon.rest2

import com.github.thebridsk.bridge.data.RestMessage
import org.scalajs.dom.ext.Ajax.InputData
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import org.scalactic.source.Position
import play.api.libs.json._
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import scala.concurrent.ExecutionContext

/**
 * @tparam R the resource class
 *
 * @constructor
 * @param resourceURIfragment the URI for the resource.  It must not end in "/".
 *                            if it is a root resource, it should start with "/".
 *                            if it is a nested resource, it should NOT start with "/"
 * @param parentClient the RestClient for the parent resource, None if root resource
 * @param parentInstance the id of the parent resource, None if root resource.
 *                       Must not be None if parentClient is not None.
 */
class RestClient[R,I]( val resourceURIfragment: String,
                       parentClient: Option[RestClient[_,_]] = None,
                       parentInstance: Option[String]=None
                     )( implicit
                          reader: Reads[R],
                          writer: Writes[R],
                          keywriter: KeyWrites[I],
                          classtag: ClassTag[R],
                          executor: ExecutionContext
                     ) {

  def getURI( id: I ): String = {
    s"${resourceURI}/${keywriter.writeKey(id)}"
  }

  def getURIfromString( id: String ): String = {
    s"${resourceURI}/${id}"
  }

  def resourceURI: String = parentClient match {
    case Some(parent) => s"${parent.getURIfromString(parentInstance.get)}/$resourceURIfragment"
    case None => resourceURIfragment
  }

  import scala.language.implicitConversions
  @inline
  implicit def RToInputData( r: R ): InputData = writeJson(r)

  @inline
  implicit def StringToR( data: String ): R = readJson[R](data)

  implicit def ajaxToRestResult( ajaxResult: AjaxResult[WrapperXMLHttpRequest] ): RestResult[R] = {
    RestResult.ajaxToRestResult(ajaxResult)
  }

  implicit def ajaxToRestResultUnit( ajaxResult: AjaxResult[WrapperXMLHttpRequest] ): RestResult[Unit] = {
    RestResult.ajaxToRestResult(ajaxResult)
  }

  implicit def ajaxToRestResultArray( ajaxResult: AjaxResult[WrapperXMLHttpRequest] ): RestResultArray[R] = {
    RestResult.ajaxToRestResultArray(ajaxResult)
  }

  val headersForPost: Map[String,String]=Map("Content-Type" -> "application/json; charset=UTF-8",
                         "Accept" -> "application/json")

  val headersForGet: Map[String,String]=Map( "Accept" -> "application/json")

  def getQueryString( query: Map[String,String] ): String = {
    if (query.isEmpty) {
      ""
    } else {
      query.map(pair => pair._1+"="+pair._2).mkString("?","&","")
    }
  }

  def getURL( id: I, query: Map[String,String] = Map() ): String = {
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

  def get( id: I,
           query: Map[String, String] = Map.empty,
           headers: Map[String, String] = headersForGet,
           timeout: Duration = AjaxResult.defaultTimeout
         )( implicit pos: Position): RestResult[R] = {
    AjaxResult.get(getURL(id,query), timeout=timeout, headers=headers)
  }



  def update( id: I,
              obj: R,
              query: Map[String, String] = Map.empty,
              headers: Map[String, String] = headersForPost,
              timeout: Duration = AjaxResult.defaultTimeout
            )( implicit pos: Position): RestResult[Unit] = {
    AjaxResult.put(getURL(id,query), data=obj, timeout=timeout, headers=headers)
  }

  def delete( id: I,
              query: Map[String, String] = Map.empty,
              headers: Map[String, String] = headersForGet,
              timeout: Duration = AjaxResult.defaultTimeout
            )( implicit pos: Position): RestResult[Unit] = {
    AjaxResult.delete(getURL(id,query), timeout=timeout, headers=headers)
  }

}

object RestClient {
  implicit class RestMessages( private val req: WrapperXMLHttpRequest ) extends AnyVal {
    def toRestMessage: String = {
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
