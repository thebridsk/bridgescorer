package com.example.util

import utils.logging.Logger
import java.net.URL
import java.io.File

/**
 * @constructor
 * @param project the GitHub project, syntax: org/repo
 */
class GitHub( val project: String,
              val extSha: String = ".sha256",
              val shaAlgorithm: String = "SHA-256" ) {
  import GitHub._

  val host = "https://github.com"

  /**
   * Returns the HTML page for the latest release.
   * @return an Either.  Left( error ) indicates an error, where error is a message.
   * Right(sha) returns the HTML page for the latest release
   */
  def getLatestRelease(): Either[String,String] = {
    val url = s"${host}/${project}/releases/latest"

    val latest = HttpUtils.getHttpAsString( new URL(url), followRedirects=true )
    if (latest.status == 200) {
      latest.data match {
        case Some(d) =>
          Right(d)
        case None =>
          Left( s"Did not get any data from ${url}" )
      }
    } else {
      Left( s"Got bad status, ${latest.status}, code from ${url}" )
    }
  }

//  https://github.com/thebridsk/bridgescorer/releases/tag/v1.0.1

  /**
   * Returns the latest release version
   */
  def getLatestVersion(): Either[String,Version] = {
    val url = s"${host}/${project}/releases/latest"
    val latest = HttpUtils.getHttpAsString( new URL(url), followRedirects=false )
    if (latest.status == 302) {
      latest.location match {
        case Some(l) =>
          val lastslash = l.lastIndexOf("/")
          if (lastslash <0) {
            Left( s"Location header not valid on GET ${url}: ${latest}" )
          } else {
            val v = l.substring(lastslash+1)
            Right( Version( if (v.startsWith("v")) v.substring(1) else v ))
          }
        case None =>
          Left( s"Did not get Location header on GET ${url}: ${latest}" )
      }
    } else {
      Left( s"Did not get 302 on GET ${url}: ${latest}" )
    }

  }

  def findAssetJarLink( page: String, asset: String ): Either[String,URL] = {
    val jarURIPattern = s"""<a href="(/${project}/releases/download/[^/]+/${asset}-.*?.jar)" """.r
    val jarURIP = jarURIPattern.unanchored

    page match {
      case jarURIP(jarURI) =>
        Right( new URL( host+jarURI ) )
      case _ =>
        Left( s"""Did not find link for jar in ${asset} in latest release for ${project}""" )
    }
  }

  /**
   * Return the SHA of one of the files.
   * @param filename the URL of the file to download
   * @return an Either.  Left( error ) indicates an error, where error is a message.
   * Right(sha) returns the sha
   */
  def getShaForFile( url: URL ): Either[String,String] = {

    val name = new File( url.getPath ).getName
    getShaFile(url) match {
      case Right(shas) =>
        val r =
        shaPattern.findAllMatchIn(shas).flatMap { m =>
          if (m.group(3)==name) m.group(1)::Nil
          else Nil
        }.toList

        if (r.length == 1) Right(r(0))
        else Left( s"Did not find the SHA for file ${url}" )
      case Left(error) =>
        Left(error)
    }

  }

  /**
   * Return the SHA of one of the files.
   * @param shas the SHAs of files in the format of sha256sum
   * @param filename the name of the file to get the sha for.
   * @return an Either.  Left( error ) indicates an error, where error is a message.
   * Right(sha) returns the sha
   */
  def getShaForFile( shas: String, filename: String ): Either[String,String] = {

    val r =
    shaPattern.findAllMatchIn(shas).flatMap { m =>
      log.fine( s"""Found sha: ${m.group(3)} ${m.group(1)}""" )
      if (m.group(3)==filename) m.group(1)::Nil
      else Nil
    }.toList

    if (r.length == 1) Right(r(0))
    else Left( s"Did not find the SHA for file ${filename}" )

  }

  /**
   * Return the file that contains the SHA for a file.  The name of the file
   * is the specified filename with ".sha256" appended.
   * @param filename the URL of the file to download
   * @return an Either.  Left( error ) indicates an error, where error is a message.
   * Right(sha) returns the contents of the sha file
   */
  def getShaFile( url: URL ): Either[String,String] = {
    val f = url.toString()+extSha
    val r = HttpUtils.getHttpAsString(new URL(f), followRedirects=true)
    log.fine(s"""Get ${f}: ${r}""")
    if (r.status == 200) {
      r.data match {
        case Some(d) =>
          Right(d)
        case None =>
          Left( s"""No data in file from ${f}""" )
      }
    } else {
      Left( s"""Did not get sha256 file: ${r.status} from ${f}""" )
    }

  }

  private def generateFilename( dir: String, url: URL ) = {
      new File( dir, new File(url.getPath).getName )
  }

  /**
   * @param url the URL of the file to download
   * @param the directory of where to save it
   * @return an Either.  Left( error ) indicates an error, where error is a message.
   * Right(sha) returns the filename where it was stored and the sha of the contents
   */
  def downloadFile( url: URL, directory: String ): Either[String,(File,String)] = {
    val dir = new File(directory)
    if (!dir.isDirectory()) {
      val rc = dir.mkdirs()
      log.fine( s"""Return from mkdirs ${dir}, rc=${rc}""" )
    }
    if (dir.isDirectory()) {
      val fname = generateFilename(directory, url)

      val result = HttpUtils.copyFromWebToFile(url, fname, shaAlgorithm, true )
      log.fine(s"""Get ${url} -> fname: ${result}""")
      if (result.status == 200) {
        result.hash.map( h => Right(fname,h) ).getOrElse(Left(s"""Did not get a sha value from GET ${url}"""))
      } else {
        Left( s"Http status is ${result.status} from GET ${url}" )
      }
    } else {
      log.fine( s"Target directory, ${directory}, is not a directory" )
      Left( s"Target directory, ${directory}, is not a directory" )
    }
  }


  /**
   * @param url the URL of the file to download
   * @param the directory of where to save it
   * @return an Either.  Left( error ) indicates an error, where error is a message.
   * Right(sha) returns the sha of the contents
   */
  def downloadLatestAsset( directory: String, asset: String ): Either[String,(File,String)] = {
    getLatestRelease().
       flatMap { page => findAssetJarLink(page, asset) }.
       flatMap { jarurl => getShaForFile(jarurl).flatMap(sha => Right((jarurl,sha)) ) }.
       flatMap { e =>
         val (jarurl, sha) = e
         log.fine( s"""SHA is ${sha} of ${jarurl}""" )
         downloadFile( jarurl, directory ).flatMap { e =>
           val (f,dsha) = e
           if (dsha == sha) {
             Right((f,dsha))
           } else {
             log.fine( s"""Sha of downloaded file doesn't match: ${dsha} expecting ${sha}, deleting ${f}""" )
             f.delete()
             Left( s"""Sha of downloaded file doesn't match: ${dsha} expecting ${sha}""" )
           }
         }
       }
  }
}

object GitHub {

  val log = Logger[GitHub]

  val shaPattern = """([0-9a-zA-Z]+) ([* ])([^\n\r]*)""".r
}
