package com.github.thebridsk.bridge.server.util

import com.github.thebridsk.utilities.logging.Logger
import java.net.URL
import java.io.File
import play.api.libs.json._
import java.util.Date
import java.text.ParseException
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import scala.util.Using
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant
import java.time.ZonedDateTime
import scala.util.matching.Regex

/**
  * @constructor
  * @param project the GitHub project, syntax: org/repo
  */
class GitHub(
    val project: String,
    val extSha: String = ".sha256",
    val shaAlgorithm: String = "SHA-256"
) {
  import GitHub._

  val host = "https://github.com"

//  https://api.github.com/repos/thebridsk/bridgescorer/releases/latest

  val apiHost = "https://api.github.com"

  def getLatestReleaseObject(): Either[String, Release] = {
    val url = s"${apiHost}/repos/${project}/releases/latest"
    val latest = HttpUtils.getHttpAsString(new URL(url), followRedirects = true)
    if (latest.status == 200) {
      latest.data match {
        case Some(d) =>
          log.fine(s"Response from ${url} is ${d}")
          val jsvalue: JsValue = Json.parse(d)
          Json.fromJson[Release](jsvalue) match {
            case JsSuccess(value, path) =>
              Right(value)
            case e: JsError =>
              Left(
                s"Error converting response from ${url}: ${JsError.toJson(e)}"
              )
          }
        case None =>
          Left(s"Did not get any data from ${url}")
      }
    } else {
      Left(s"Got bad status, ${latest.status}, code from ${url}")
    }
  }

  /**
    * Returns the HTML page for the latest release.
    * @return an Either.  Left( error ) indicates an error, where error is a message.
    * Right(sha) returns the HTML page for the latest release
    */
  def getLatestRelease(): Either[String, String] = {
    val url = s"${host}/${project}/releases/latest"

    val latest = HttpUtils.getHttpAsString(new URL(url), followRedirects = true)
    if (latest.status == 200) {
      latest.data match {
        case Some(d) =>
          Right(d)
        case None =>
          Left(s"Did not get any data from ${url}")
      }
    } else {
      Left(s"Got bad status, ${latest.status}, code from ${url}")
    }
  }

//  https://github.com/thebridsk/bridgescorer/releases/tag/v1.0.1

  /**
    * Returns the latest release version
    */
  def getLatestVersion(): Either[String, Version] = {
    val url = s"${host}/${project}/releases/latest"
    val latest =
      HttpUtils.getHttpAsString(new URL(url), followRedirects = false)
    if (latest.status == 302) {
      latest.location match {
        case Some(l) =>
          val lastslash = l.lastIndexOf("/")
          if (lastslash < 0) {
            Left(s"Location header not valid on GET ${url}: ${latest}")
          } else {
            val v = l.substring(lastslash + 1)
            Right(Version(if (v.startsWith("v")) v.substring(1) else v))
          }
        case None =>
          Left(s"Did not get Location header on GET ${url}: ${latest}")
      }
    } else {
      Left(s"Did not get 302 on GET ${url}: ${latest}")
    }

  }

  def findAssetJarLink(page: String, asset: String): Either[String, URL] = {
    val jarURIPattern =
      s"""<a href="(/${project}/releases/download/[^/]+/${asset}-.*?.jar)" """.r
    val jarURIP = jarURIPattern.unanchored

    page match {
      case jarURIP(jarURI) =>
        Right(new URL(host + jarURI))
      case _ =>
        Left(
          s"""Did not find link for jar in ${asset} in latest release for ${project}"""
        )
    }
  }

  /**
    * Return the SHA of one of the files.
    * @param filename the URL of the file to download
    * @return an Either.  Left( error ) indicates an error, where error is a message.
    * Right(sha) returns the tuple2 (sha, shafilecontent)
    */
  def getShaForFile(url: URL): Either[String, (String, String)] = {

    val name = new File(url.getPath).getName
    getShaFile(url) match {
      case Right(shas) =>
        val r =
          shaPattern
            .findAllMatchIn(shas)
            .flatMap { m =>
              if (m.group(3) == name) m.group(1) :: Nil
              else Nil
            }
            .toList

        if (r.length == 1) Right((r(0), shas))
        else Left(s"Did not find the SHA for file ${url}")
      case Left(error) =>
        Left(error)
    }

  }

  /**
    * Return the SHA of one of the files.
    * @param shas the SHAs of files in the format of sha256sum
    * @param filename the name of the file to get the sha for.
    * @return an Either.  Left( error ) indicates an error, where error is a message.
    * Right(sha) returns the tuple2 (sha, shafilecontent)
    */
  def getShaForFile(
      shas: String,
      filename: String
  ): Either[String, (String, String)] = {

    val r =
      shaPattern
        .findAllMatchIn(shas)
        .flatMap { m =>
          log.fine(s"""Found sha: ${m.group(3)} ${m.group(1)}""")
          if (m.group(3) == filename) m.group(1) :: Nil
          else Nil
        }
        .toList

    if (r.length == 1) Right((r(0), shas))
    else Left(s"Did not find the SHA for file ${filename}")

  }

  /**
    * Return the file that contains the SHA for a file.  The name of the file
    * is the specified filename with ".sha256" appended.
    * @param filename the URL of the file to download
    * @return an Either.  Left( error ) indicates an error, where error is a message.
    * Right(sha) returns the contents of the sha file
    */
  def getShaFile(url: URL): Either[String, String] = {
    val f = url.toString() + extSha
    val r = HttpUtils.getHttpAsString(new URL(f), followRedirects = true)
    log.fine(s"""Get ${f}: ${r}""")
    if (r.status == 200) {
      r.data match {
        case Some(d) =>
          Right(d)
        case None =>
          Left(s"""No data in file from ${f}""")
      }
    } else {
      Left(s"""Did not get sha256 file: ${r.status} from ${f}""")
    }

  }

  private def generateFilename(dir: String, url: URL) = {
    new File(dir, new File(url.getPath).getName)
  }

  /**
    * @param url the URL of the file to download
    * @param the directory of where to save it
    * @return an Either.  Left( error ) indicates an error, where error is a message.
    * Right(sha) returns the filename where it was stored and the sha of the contents
    */
  def downloadFile(
      url: URL,
      directory: String
  ): Either[String, (File, String)] = {
    val dir = new File(directory)
    if (!dir.isDirectory()) {
      val rc = dir.mkdirs()
      log.fine(s"""Return from mkdirs ${dir}, rc=${rc}""")
    }
    if (dir.isDirectory()) {
      val fname = generateFilename(directory, url)

      val result = HttpUtils.copyFromWebToFile(url, fname, shaAlgorithm, true)
      log.fine(s"""Get ${url} -> fname: ${result}""")
      if (result.status == 200) {
        result.hash
          .map(h => Right(fname, h))
          .getOrElse(Left(s"""Did not get a sha value from GET ${url}"""))
      } else {
        Left(s"Http status is ${result.status} from GET ${url}")
      }
    } else {
      log.fine(s"Target directory, ${directory}, is not a directory")
      Left(s"Target directory, ${directory}, is not a directory")
    }
  }

  /**
    * Download the file from the URL, and check the SHA of the file from file <url>.sha256
    * @param url the URL of the file to download
    * @param the directory of where to save it
    * @return an Either.  Left( error ) indicates an error, where error is a message.
    * Right(sha) returns the filename where it was stored and the sha of the contents
    */
  def downloadFileAndCheckSHA(
      url: URL,
      directory: String
  ): Either[String, (File, String)] = {
    getShaForFile(url).flatMap { value =>
      val (sha, shafilecontent) = value
      log.fine(s"""SHA is ${sha} of ${url}""")
      downloadFile(url, directory).flatMap { e =>
        val (f, dsha) = e
        if (dsha == sha) {
          val shafile = new File(f.toString() + extSha)
          Using.resource(
              new OutputStreamWriter(new FileOutputStream(shafile), "UTF8")
          ) { shaf =>
            shaf.write(shafilecontent)
            shaf.flush()
          }
          Right((f, dsha))
        } else {
          log.fine(
            s"""Sha of downloaded file doesn't match: ${dsha} expecting ${sha}, deleting ${f}"""
          )
          f.delete()
          Left(
            s"""Sha of downloaded file doesn't match: ${dsha} expecting ${sha}"""
          )
        }
      }
    }
  }

  /**
    * @param url the URL of the file to download
    * @param the directory of where to save it
    * @return an Either.  Left( error ) indicates an error, where error is a message.
    * Right(sha) returns the sha of the contents
    */
  def downloadLatestAsset(
      directory: String,
      asset: String
  ): Either[String, (File, String)] = {
    getLatestRelease()
      .flatMap { page =>
        findAssetJarLink(page, asset)
      }
      .flatMap { jarurl =>
        getShaForFile(jarurl).flatMap(sha => Right((jarurl, sha._1)))
      }
      .flatMap { e =>
        val (jarurl, sha) = e
        log.fine(s"""SHA is ${sha} of ${jarurl}""")
        downloadFile(jarurl, directory).flatMap { e =>
          val (f, dsha) = e
          if (dsha == sha) {
            Right((f, dsha))
          } else {
            log.fine(
              s"""Sha of downloaded file doesn't match: ${dsha} expecting ${sha}, deleting ${f}"""
            )
            f.delete()
            Left(
              s"""Sha of downloaded file doesn't match: ${dsha} expecting ${sha}"""
            )
          }
        }
      }
  }
}

object GitHub {

  val log: Logger = Logger[GitHub]()

  val shaPattern: Regex = """([0-9a-zA-Z]+) ([* ])([^\n\r]*)""".r

  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz").withZone( ZoneId.systemDefault() )
  def formatDate(date: Date): String = {

    dateFormat.format( Instant.ofEpochMilli(date.getTime()))
  }

  case class Asset(
      name: String,
      created_at: Date,
      updated_at: Date,
      browser_download_url: URL
  ) {
    def forTrace(): String = {
      s"""$name updated ${formatDate(updated_at)} ${browser_download_url}"""
    }
  }

  case class Person(
      login: String,
      url: URL
  ) {
    def forTrace(): String = {
      s"""$login ${url}"""
    }
  }

  case class Release(
      tag_name: String,
      created_at: Date,
      published_at: Date,
      author: Person,
      assets: List[Asset],
      zipball_url: URL,
      tarball_url: URL,
      body: Option[String]
  ) {
    def getVersion(): Version = {
      Version(if (tag_name.startsWith("v")) tag_name.substring(1) else tag_name)
    }
    def forTrace(): String = {
      s"""Release ${tag_name} published ${formatDate(published_at)} by ${author
        .forTrace()}""" +
        body.map(b => s"\n${b}").getOrElse("") +
        assets.zipWithIndex
          .map { e =>
            s"  ${e._2}: ${e._1.forTrace()}"
          }
          .mkString("\n  ", "\n  ", "")
    }
  }

  import play.api.libs.json.Reads._
  import play.api.libs.functional.syntax._

  implicit object dateReads extends Reads[Date] {

    // "2018-01-17T00:47:37Z"
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

    def reads(json: JsValue): JsResult[Date] = {
      json match {
        case JsString(date) =>
          try {
            JsSuccess( new Date( ZonedDateTime.parse( date, dateFormat ).toInstant().toEpochMilli() ))
          } catch {
            case x: ParseException =>
              JsError(
                s"Expecting date value to be in 2018-01-17T00:47:37Z format, got ${json}"
              )
          }
        case _ =>
          JsError(
            s"Unexpected value, expecting a Date in a JSON string, got ${json}"
          )
      }
    }
  }

  implicit object urlReads extends Reads[URL] {
    def reads(json: JsValue): JsResult[URL] = {
      json match {
        case JsString(url) => JsSuccess(new URL(url))
        case _ =>
          JsError(
            s"Unexpected value, expecting a URL in a JSON string, got ${json}"
          )
      }
    }
  }

  implicit val assetReads: Reads[Asset] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "created_at").read[Date] and
      (JsPath \ "updated_at").read[Date] and
      (JsPath \ "browser_download_url").read[URL]
  )(Asset.apply _)

  implicit val personReads: Reads[Person] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "url").read[URL]
  )(Person.apply _)

  implicit val releaseReads: Reads[Release] = (
    (JsPath \ "tag_name").read[String] and
      (JsPath \ "created_at").read[Date] and
      (JsPath \ "published_at").read[Date] and
      (JsPath \ "author").read[Person] and
      (JsPath \ "assets").read[List[Asset]] and
      (JsPath \ "zipball_url").read[URL] and
      (JsPath \ "tarball_url").read[URL] and
      (JsPath \ "body").readNullable[String]
  )(Release.apply _)
// https://api.github.com/repos/thebridsk/bridgescorer/releases/latest
//{
//  "url": "https://api.github.com/repos/thebridsk/bridgescorer/releases/9273387",
//  "assets_url": "https://api.github.com/repos/thebridsk/bridgescorer/releases/9273387/assets",
//  "upload_url": "https://uploads.github.com/repos/thebridsk/bridgescorer/releases/9273387/assets{?name,label}",
//  "html_url": "https://github.com/thebridsk/bridgescorer/releases/tag/v1.0.2",
//  "id": 9273387,
//  "tag_name": "v1.0.2",
//  "target_commitish": "master",
//  "name": null,
//  "draft": false,
//  "author": {
//    "login": "thebridsk",
//    "id": 35344976,
//    "avatar_url": "https://avatars0.githubusercontent.com/u/35344976?v=4",
//    "gravatar_id": "",
//    "url": "https://api.github.com/users/thebridsk",
//    "html_url": "https://github.com/thebridsk",
//    "followers_url": "https://api.github.com/users/thebridsk/followers",
//    "following_url": "https://api.github.com/users/thebridsk/following{/other_user}",
//    "gists_url": "https://api.github.com/users/thebridsk/gists{/gist_id}",
//    "starred_url": "https://api.github.com/users/thebridsk/starred{/owner}{/repo}",
//    "subscriptions_url": "https://api.github.com/users/thebridsk/subscriptions",
//    "organizations_url": "https://api.github.com/users/thebridsk/orgs",
//    "repos_url": "https://api.github.com/users/thebridsk/repos",
//    "events_url": "https://api.github.com/users/thebridsk/events{/privacy}",
//    "received_events_url": "https://api.github.com/users/thebridsk/received_events",
//    "type": "User",
//    "site_admin": false
//  },
//  "prerelease": false,
//  "created_at": "2018-01-17T00:47:37Z",
//  "published_at": "2018-01-17T16:58:06Z",
//  "assets": [
//    {
//      "url": "https://api.github.com/repos/thebridsk/bridgescorer/releases/assets/5902255",
//      "id": 5902255,
//      "name": "bridgescorer-server-assembly-1.0.2-dd1a645aa063474956561c7140fcfdf20f7d2b34.jar",
//      "label": "",
//      "uploader": {
//        "login": "thebridsk",
//        "id": 35344976,
//        "avatar_url": "https://avatars0.githubusercontent.com/u/35344976?v=4",
//        "gravatar_id": "",
//        "url": "https://api.github.com/users/thebridsk",
//        "html_url": "https://github.com/thebridsk",
//        "followers_url": "https://api.github.com/users/thebridsk/followers",
//        "following_url": "https://api.github.com/users/thebridsk/following{/other_user}",
//        "gists_url": "https://api.github.com/users/thebridsk/gists{/gist_id}",
//        "starred_url": "https://api.github.com/users/thebridsk/starred{/owner}{/repo}",
//        "subscriptions_url": "https://api.github.com/users/thebridsk/subscriptions",
//        "organizations_url": "https://api.github.com/users/thebridsk/orgs",
//        "repos_url": "https://api.github.com/users/thebridsk/repos",
//        "events_url": "https://api.github.com/users/thebridsk/events{/privacy}",
//        "received_events_url": "https://api.github.com/users/thebridsk/received_events",
//        "type": "User",
//        "site_admin": false
//      },
//      "content_type": "application/java-archive",
//      "state": "uploaded",
//      "size": 65791747,
//      "download_count": 2,
//      "created_at": "2018-01-17T16:57:37Z",
//      "updated_at": "2018-01-17T16:58:04Z",
//      "browser_download_url": "https://github.com/thebridsk/bridgescorer/releases/download/v1.0.2/bridgescorer-server-assembly-1.0.2-dd1a645aa063474956561c7140fcfdf20f7d2b34.jar"
//    },
//    {
//      "url": "https://api.github.com/repos/thebridsk/bridgescorer/releases/assets/5902256",
//      "id": 5902256,
//      "name": "bridgescorer-server-assembly-1.0.2-dd1a645aa063474956561c7140fcfdf20f7d2b34.jar.sha256",
//      "label": "",
//      "uploader": {
//        "login": "thebridsk",
//        "id": 35344976,
//        "avatar_url": "https://avatars0.githubusercontent.com/u/35344976?v=4",
//        "gravatar_id": "",
//        "url": "https://api.github.com/users/thebridsk",
//        "html_url": "https://github.com/thebridsk",
//        "followers_url": "https://api.github.com/users/thebridsk/followers",
//        "following_url": "https://api.github.com/users/thebridsk/following{/other_user}",
//        "gists_url": "https://api.github.com/users/thebridsk/gists{/gist_id}",
//        "starred_url": "https://api.github.com/users/thebridsk/starred{/owner}{/repo}",
//        "subscriptions_url": "https://api.github.com/users/thebridsk/subscriptions",
//        "organizations_url": "https://api.github.com/users/thebridsk/orgs",
//        "repos_url": "https://api.github.com/users/thebridsk/repos",
//        "events_url": "https://api.github.com/users/thebridsk/events{/privacy}",
//        "received_events_url": "https://api.github.com/users/thebridsk/received_events",
//        "type": "User",
//        "site_admin": false
//      },
//      "content_type": "application/octet-stream",
//      "state": "uploaded",
//      "size": 146,
//      "download_count": 1,
//      "created_at": "2018-01-17T16:58:05Z",
//      "updated_at": "2018-01-17T16:58:05Z",
//      "browser_download_url": "https://github.com/thebridsk/bridgescorer/releases/download/v1.0.2/bridgescorer-server-assembly-1.0.2-dd1a645aa063474956561c7140fcfdf20f7d2b34.jar.sha256"
//    }
//  ],
//  "tarball_url": "https://api.github.com/repos/thebridsk/bridgescorer/tarball/v1.0.2",
//  "zipball_url": "https://api.github.com/repos/thebridsk/bridgescorer/zipball/v1.0.2",
//  "body": null
//}

}
