package com.github.thebridsk.bridge.server.util

import java.io.InputStream
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import java.io.InputStreamReader
import com.github.thebridsk.utilities.logging.Logger
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import play.api.libs.json.Reads
import java.io.OutputStreamWriter
import play.api.libs.json.Format
import java.io.OutputStream
import java.security.MessageDigest
import java.security.DigestOutputStream
import java.io.FileOutputStream
import java.io.File
import java.io.StringWriter
import scala.util.Using

object HttpUtilsInternal {

  private[util] val logger = Logger[HttpUtils]()

  private[util] val bufsize = 1024 * 1024
}

trait HttpUtils {
  import HttpUtilsInternal._
  import HttpUtils._

  /**
    * @param contentEncoding
    * @param is the input stream.  The contents are in <i>contentEncoding</i>.
    * @param out where to save the decoded contents
    * @throws IOException if there was an error
    */
  private def readAllBytesAndCloseInputStream(
      contentEncoding: Option[String],
      is: InputStream,
      out: OutputStream
  ): Unit = {
    try {
      var len = 0
      val buf = new Array[Byte](bufsize)

      var rlen = 0

      val in = contentEncoding match {
        case Some(ce) if ce == "gzip" =>
          new GZIPInputStream(is)
        case _ =>
          is
      }

      while ({ rlen = in.read(buf); rlen > 0 }) {
        out.write(buf, 0, rlen)
      }
      out.flush()

    } finally {
      is.close()
    }
  }

  /**
    * @param url where to get the data from
    * @param out where to save the decoded contents
    * @throws IOException if there was an error
    */
  def getHttpAllBytes(
      url: URL,
      out: OutputStream,
      followRedirects: Boolean = false
  ): ResponseFromHttp = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setInstanceFollowRedirects(followRedirects)
      conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
      val status = conn.getResponseCode
      logger.fine("Get " + url + ": status code is " + status)
      val loc = Option(conn.getHeaderField("Location"))
      val ce = Option(conn.getHeaderField("Content-Encoding"))
      readAllBytesAndCloseInputStream(ce, conn.getInputStream, out)
      ResponseFromHttp(status, loc, ce)
    } catch {
      case x: IOException =>
        logger.info("Exception trying to get data from " + url, x)
        throw x
    } finally {
      conn.disconnect()
    }

  }

  /**
    * get the data, up to 100000 characters.
    * @param url where to get the data from
    * @param charset the charset to decode the bytes.  Default is UTF-8.
    * @throws IOException if there was an error
    */
  def getHttpAsString(
      url: URL,
      charset: String = "UTF8",
      followRedirects: Boolean = false
  ): ResponseFromHttp = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setInstanceFollowRedirects(followRedirects)
      conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
      val status = conn.getResponseCode
      logger.fine("Get " + url + ": status code is " + status)
      val loc = Option(conn.getHeaderField("Location"))
      val ce = Option(conn.getHeaderField("Content-Encoding"))
      val baos = new ByteArrayOutputStream
      readAllBytesAndCloseInputStream(ce, conn.getInputStream, baos)
      val b = new InputStreamReader(
        new ByteArrayInputStream(baos.toByteArray()),
        charset
      )
      val cbuf = new Array[Char](100000)
      val l = b.read(cbuf)
      val data = new String(cbuf, 0, l)
      ResponseFromHttp(status, loc, ce, data = Some(data))
    } catch {
      case x: IOException =>
        logger.info("Exception trying to get data from " + url, x)
        throw x
    } finally {
      conn.disconnect()
    }

  }

  /**
    * Download a file from the web and save to an output stream, and returning a hash of the contents.
    * @param url
    * @param out
    * @param hashalgo the hash algorithm to use.  default is "SHA-256"
    * @return a tuple2, the first is the status code from the HTTP GET, the second is the <i>hash</i> hash of the file contents.
    */
  def copyFromWeb(
      url: URL,
      out: OutputStream,
      hashalgo: String = "SHA-256",
      followRedirects: Boolean = false
  ): ResponseFromHttp = {

    val md = MessageDigest.getInstance(hashalgo)
    val dos = new DigestOutputStream(out, md)

    val httpResp = HttpUtils.getHttpAllBytes(url, dos, followRedirects)

    httpResp.copy(hash = Some(toHexString(md.digest())))
  }

  def toHexString(hash: Array[Byte]) = {
    hash.map(b => f"${b}%02x").mkString
  }

  /**
    * Download a file from the web and save to a file, and returning a hash of the contents.
    * @param url
    * @param file
    * @param hashalgo the hash algorithm to use.  default is "SHA-256"
    * @return a tuple2, the first is the status code from the HTTP GET, the second is the <i>hash</i> hash of the file contents.
    */
  def copyFromWebToFile(
      url: URL,
      outfile: File,
      hashalgo: String = "SHA-256",
      followRedirects: Boolean = false
  ): ResponseFromHttp = {

    Using.resource(new FileOutputStream(outfile)) { out =>
      copyFromWeb(url, out, hashalgo, followRedirects)
    }

  }

}

object HttpUtils extends HttpUtils {

  case class ResponseFromHttp(
      val status: Int,
      location: Option[String],
      contentencoding: Option[String],
      data: Option[String] = None,
      hash: Option[String] = None
  )

  object NullOutputStream extends OutputStream {

    def write(i: Int) = {}
    override def write(b: Array[Byte]) = {}
    override def write(b: Array[Byte], off: Int, len: Int) = {}

    override def close() = {}
    override def flush() = {}

  }
}
