package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import java.net.URL
import java.io.File
import com.github.thebridsk.bridge.server.util.HttpUtils
import com.github.thebridsk.bridge.server.util.HttpUtils.NullOutputStream
import com.github.thebridsk.bridge.server.util.GitHub

object Download extends Main {

  val defaultAlgo = "SHA-256"

  val cmdname = {
    val x = Download.getClass.getName
    "scala "+x.substring(0, x.length()-1)
  }

  banner(s"""
Download a file from the web, and calculate

Syntax:
  ${cmdname} options
Options:""")

  val optionProject = opt[String](
                    "project",
                    short='p',
                    descr="The GitHub project that has the file in a release.",
                    argName="project",
                    default=None)

  val optionFile = opt[String](
                    "file",
                    short='f',
                    descr="The file in the GitHub release to download.",
                    argName="file",
                    default=None)

  val optionAlgo = opt[String](
                    "algorithm",
                    short='a',
                    descr=s"The hash algorithm to use, default is ${defaultAlgo}.",
                    argName="algo",
                    default=Some(defaultAlgo))


  def execute() = {
    val project = optionProject()
    val fileToDownload = optionFile()
    val algo = optionAlgo()

    val github = new GitHub( project )

    github.downloadLatestAsset(".", fileToDownload ) match {
      case Right((file,sha)) =>
        println( s"""Downloaded ${fileToDownload} to ${file}, sha is ${sha}""" )
      case Left(error) =>
        println( s"""Error getting asset: ${error}""" )
    }

    0
  }

//  https://github.com/thebridsk/utilities/releases/latest
//
//  <a href="/thebridsk/utilities/releases/download/v1.0.5/utilities-jvm_2.12-1.0.5-ef28b25b1ca26d6f52dbda1f1d676927ba95203b.jar" rel="nofollow">
//                <small class="text-gray float-right">163 KB</small>
//                <svg aria-hidden="true" class="octicon octicon-package text-gray d-inline-block" height="16" version="1.1" viewBox="0 0 16 16" width="16"><path fill-rule="evenodd" d="M1 4.27v7.47c0 .45.3.84.75.97l6.5 1.73c.16.05.34.05.5 0l6.5-1.73c.45-.13.75-.52.75-.97V4.27c0-.45-.3-.84-.75-.97l-6.5-1.74a1.4 1.4 0 0 0-.5 0L1.75 3.3c-.45.13-.75.52-.75.97zm7 9.09l-6-1.59V5l6 1.61v6.75zM2 4l2.5-.67L11 5.06l-2.5.67L2 4zm13 7.77l-6 1.59V6.61l2-.55V8.5l2-.53V5.53L15 5v6.77zm-2-7.24L6.5 2.8l2-.53L15 4l-2 .53z"></path></svg>
//                <strong class="pl-1">utilities-jvm_2.12-1.0.5-ef28b25b1ca26d6f52dbda1f1d676927ba95203b.jar</strong>
//              </a>

  def oldExecute() = {
    val project = optionProject()
    val fileToDownload = optionFile()
    val algo = optionAlgo()

    val host = "https://github.com"
    val url = s"${host}/${project}/releases/latest"

    val jarURIPattern = s"""<a href="(/${project}/releases/download/[^/]+/${fileToDownload}-.*?.jar)" """.r
    val jarURIP = jarURIPattern.unanchored

    val latest = HttpUtils.getHttpAsString( new URL(url), followRedirects=true )
    if (latest.status == 200) {
      latest.data match {
        case Some(d) =>
          d match {
            case jarURIP(jarURI) =>
              val jarURL = s"${host}${jarURI}"

              getSha256File(jarURL) match {
                case Some(sha) =>
                  println( s"Expecting ${algo} of ${sha}" )
                  val filename = new File( jarURI ).getName
                  val result = HttpUtils.copyFromWebToFile(new URL(jarURL), new File(filename), algo, true )
                  println( s"Http status is ${result.status}, ${algo} is ${result.hash.get}" )
                  if (sha == result.hash.get) {
                    println( s"Hash is good, downloaded file ${filename}" )
                  } else {
                    println( s"Hash does not match, do not trust downloaded file ${filename}" )
                  }
                case None =>
                  // error message already printed out
              }

            case _ =>
              println( s"""Did not find link for jar in ${url}""" )
          }

        case None =>
          println( s"""Did not get any data for ${url}""" )
      }
    } else {
      println( s"""Did not get expected response for ${url}: ${latest.status}""" )
    }

    0
  }

  val shaPattern = """([0-9a-zA-Z]+) ([* ])(.*)\s*""".r
  def getSha256File( filename: String ): Option[String] = {
    val f = filename+".sha256"
    val r = HttpUtils.getHttpAsString(new URL(f), followRedirects=true)
    if (r.status == 200) {
      r.data match {
        case Some(d) =>
          d match {
            case shaPattern(sha,binary,file) =>
              Some(sha)
            case _ =>
              println( s"""Did not find hash in file from ${f}: ${d}""" )
              None
          }
        case None =>
          println( s"""No data in file from ${f}""" )
          None
      }
    } else {
      println( s"""Did not get sha256 file: ${r.status} from ${f}""" )
      None
    }

  }

}
