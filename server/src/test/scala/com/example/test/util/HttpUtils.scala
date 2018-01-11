package com.example.test.util

import java.io.InputStream
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import java.io.InputStreamReader
import utils.logging.Logger
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import play.api.libs.json.Reads
import java.io.OutputStreamWriter
import play.api.libs.json.Format

object HttpUtilsInternal {

  private[util] val logger = Logger[HttpUtils]

}

trait HttpUtils {
  import HttpUtilsInternal._

  /**
   * Returns the first 500 bytes as a string
   */
  private def readAndCloseInputStream( contentEncoding: Option[String], is: InputStream ) = {
    try {
      var len = 0

      val in = contentEncoding match {
        case Some(ce) if ce=="gzip" =>
          new GZIPInputStream(is)
        case _ =>
          is
      }
      val firstbuf = new Array[Byte](1024)
      val rlen=in.read(firstbuf)
      val buf = new Array[Byte](1024)
      if (rlen > 0) while ( { len=is.read(buf) ; len > 0 } ) len = 0
      val b = new InputStreamReader( new ByteArrayInputStream(firstbuf,0,rlen), "UTF8" )
      val cbuf = new Array[Char]( 500 )
      val l = b.read(cbuf)
      new String( cbuf, 0, l )
    } finally {
      is.close()
    }
  }

  case class ResponseFromHttp[T]( val status: Int, location: Option[String], contentencoding: Option[String], data: T )

  /**
   * Get the first 500 bytes of the response data
   * @return (status,locationheader,contentencodingheader,body)
   */
  def getHttp( url: URL ): ResponseFromHttp[String] = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setInstanceFollowRedirects(false)
      conn.setRequestProperty("Accept-Encoding","gzip, deflate")
      val status = conn.getResponseCode
      if (status < 200 || status >= 300) {
        logger.warning("Error getting "+url+", status code is "+status)
      }
      val loc = Option(conn.getHeaderField("Location"))
      val ce = Option(conn.getHeaderField("Content-Encoding"))
      ResponseFromHttp(status,loc,ce,readAndCloseInputStream(ce,conn.getInputStream))
    } catch {
      case x: IOException =>
        logger.info("Exception trying to get data from "+url, x)
        throw x
    } finally {
      conn.disconnect()
    }

  }

  private def readAllBytesAndCloseInputStream( contentEncoding: Option[String], is: InputStream ) = {
    try {
      var len = 0
      val buf = new Array[Byte](1024)
      val bytesout = new ByteArrayOutputStream()

      var rlen = 0

      val in = contentEncoding match {
        case Some(ce) if ce=="gzip" =>
          new GZIPInputStream(is)
        case _ =>
          is
      }

      while ( { rlen = in.read(buf); rlen > 0 }) {
        bytesout.write(buf, 0, rlen)
      }
      bytesout.toByteArray()

    } finally {
      is.close()
    }
  }

  private def readAllAndCloseInputStream( contentEncoding: Option[String], is: InputStream ) = {
    try {
      var len = 0
      val buf = new Array[Byte](1024)
      val bytesout = new ByteArrayOutputStream()

      var rlen = 0

      val in = contentEncoding match {
        case Some(ce) if ce=="gzip" =>
          new GZIPInputStream(is)
        case _ =>
          is
      }

      while ( { rlen = in.read(buf); rlen > 0 }) {
        bytesout.write(buf, 0, rlen)
      }
      new String(bytesout.toByteArray(), "UTF8" )

    } finally {
      is.close()
    }
  }

  /**
   * @return
   */
  def getHttpAll( url: URL ): ResponseFromHttp[String] = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setInstanceFollowRedirects(false)
      conn.setRequestProperty("Accept-Encoding","gzip, deflate")
      val status = conn.getResponseCode
      if (status < 200 || status >= 300) {
        logger.warning("Error getting "+url+", status code is "+status)
      }
      val loc = Option(conn.getHeaderField("Location"))
      val ce = Option(conn.getHeaderField("Content-Encoding"))
      ResponseFromHttp(status,loc,ce,readAllAndCloseInputStream(ce,conn.getInputStream))
    } catch {
      case x: IOException =>
        logger.info("Exception trying to get data from "+url, x)
        throw x
    } finally {
      conn.disconnect()
    }

  }

  def getHttpAllBytes( url: URL ): ResponseFromHttp[Array[Byte]] = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setInstanceFollowRedirects(false)
      conn.setRequestProperty("Accept-Encoding","gzip, deflate")
      val status = conn.getResponseCode
      if (status < 200 || status >= 300) {
        logger.warning("Error getting "+url+", status code is "+status)
      }
      val loc = Option(conn.getHeaderField("Location"))
      val ce = Option(conn.getHeaderField("Content-Encoding"))
      ResponseFromHttp(status,loc,ce,readAllBytesAndCloseInputStream(ce,conn.getInputStream))
    } catch {
      case x: IOException =>
        logger.info("Exception trying to get data from "+url, x)
        throw x
    } finally {
      conn.disconnect()
    }

  }

  def getHttpObject[T :Reads]( url: URL ): ResponseFromHttp[Option[T]] = {
    import com.example.rest.UtilsPlayJson._

    val ResponseFromHttp(status,loc,ce,resp) = getHttpAll(url)
    if (status >=200 && status <300) {
      ResponseFromHttp(status,loc,ce,Some( readJson[T](resp)))
    } else {
      ResponseFromHttp(status,loc,ce,None)
    }
  }

  def postHttp( url: URL, data: String, contentEncoding: String ) = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setDoOutput( true )
      conn.setRequestMethod("POST")
      conn.setInstanceFollowRedirects(false)
      conn.setRequestProperty("Accept-Encoding","gzip, deflate")
      conn.setRequestProperty("content-type", "application/json")
      val w = new OutputStreamWriter( conn.getOutputStream, contentEncoding )
      w.write(data)
      w.flush()
      val status = conn.getResponseCode
      if (status < 200 || status >= 300) {
        logger.warning("Error getting "+url+", status code is "+status)
      }
      val loc = Option(conn.getHeaderField("Location"))
      val ce = Option(conn.getHeaderField("Content-Encoding"))
      ResponseFromHttp(status,loc,ce,readAllAndCloseInputStream(ce,conn.getInputStream))
    } catch {
      case x: IOException =>
        logger.info("Exception trying to get data from "+url, x)
        throw x
    } finally {
      conn.disconnect()
    }
  }

  def postHttpObject[T :Format]( url: URL, data: T ): ResponseFromHttp[Option[T]] = {
    import com.example.rest.UtilsPlayJson._

    val ResponseFromHttp(status,loc,ce,resp) = postHttp(url, writeJson(data), "UTF8")
    if (status >=200 && status <300) {
      ResponseFromHttp(status,loc,ce,Some( readJson[T](resp)))
    } else {
      ResponseFromHttp(status,loc,ce,None)
    }
  }

}

object HttpUtils extends HttpUtils
