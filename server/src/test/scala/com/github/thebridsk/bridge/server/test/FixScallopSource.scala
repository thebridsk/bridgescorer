package com.github.thebridsk.bridge.server.test

import com.github.thebridsk.utilities.main.MainNoArgs
import java.io.File
import com.github.thebridsk.bridge.server.util.Version
import java.nio.file.Files
import java.util.jar.JarOutputStream
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import java.io.FileOutputStream
import java.io.FileInputStream
import scala.util.matching.Regex

object FixScallopSource extends MainNoArgs {
  // C:\Users\werewolf\.ivy2\cache\org.rogach\scallop_2.12\srcs

//  scallop_2.12-3.1.1-sources.original.jar
//  scallop_2.12-3.1.1-sources.jar

  val patternScallop: Regex =
    """scallop_(\d+\.\d+-\d+\.\d+\.\d+)-sources(\.original)?.jar""".r

  case class ScallopVersion(scalaVersion: Version, scallopVersion: Version)
      extends Ordered[ScallopVersion] {

    def compare(other: ScallopVersion): Int = {
      if (scalaVersion < other.scalaVersion) return -1
      if (scalaVersion != other.scalaVersion) return 1
      else {
        if (scallopVersion < other.scallopVersion) return -1
        if (scallopVersion != other.scallopVersion) return 1
        return 0
      }

    }

    override def equals(other: Any): Boolean = {
      other match {
        case sv: ScallopVersion => compare(sv) == 0
        case _                  => false
      }
    }

    override def hashCode(): Int = {
      scalaVersion.hashCode() + scallopVersion.hashCode()
    }
  }

  object ScallopVersion {
    val patternVersion: Regex = """(\d+\.\d+)-(\d+\.\d+\.\d+)""".r

    def apply(ver: String): ScallopVersion = {
      ver match {
        case patternVersion(scalav, scallopv) =>
          new ScallopVersion(Version(scalav), Version(scallopv))
      }
    }
  }

  def execute(): Int = {

    val homedir = sys.props
      .get("user.home")
      .getOrElse(
        throw new IllegalStateException("Unable to determine home directory")
      )

    val srcdir =
      new File(homedir, """.ivy2\cache\org.rogach\scallop_2.12\srcs""")

    val files = collection.mutable.Map[Version, (File, File)]()

    srcdir.listFiles.foreach { f =>
      f.getName match {
        case patternScallop(ver, original) =>
          logger.info(s"""Found version ${ver} ${original}""")
          val version = Version(ver)
          files.get(version) match {
            case Some((jar, originaljar)) =>
              files += (version -> (if (original == null) (f, originaljar)
                                    else (jar, f)))
            case None =>
              files += (version -> (if (original == null) (f, null)
                                    else (null, f)))
          }
        case _ =>
          logger.fine(s"""ignoring ${f}""")
      }
    }
    logger.info(s"""There are ${files.size} files found: ${files.mkString(
      "\n",
      "\n",
      ""
    )}""")
    files
      .filter(p => p._2._2 == null)
      .toList
      .sortBy(e => e._1)
      .lastOption match {
      case Some(lastversion) =>
        val version = lastversion._1
        val file = lastversion._2._1
        logger.info(s"""Converting version ${version} from file ${file}""")
        val lastdot = file.toString().lastIndexOf(".")
        val originalfilename = new File(
          file.toString().substring(0, lastdot) + ".original.jar"
        )
        logger.info(s"""Saving file in file ${originalfilename}""")
        Files.copy(file.toPath(), originalfilename.toPath)
        fixJar(originalfilename, file)
      case None =>
        logger.info("Did not find any raw source files that were not converted")
    }
    0
  }

  def copyZE(e: ZipEntry): ZipEntry = {
    val i = e.getName.lastIndexOf(".")
    val fixedname =
      if (i < 0) e.getName
      else {
        if (e.getName.startsWith("org.")) {
          val b = e.getName.substring(0, i)
          val a = e.getName.substring(i)
          b.replace(".", "/") + a
        } else {
          e.getName
        }
      }

    val n = new ZipEntry(fixedname)

    if (e.getComment != null) n.setComment(e.getComment)
//    n.setCompressedSize( e.getCompressedSize )
//    n.setCrc( e.getCrc )
    if (e.getCreationTime != null) n.setCreationTime(e.getCreationTime)
    if (e.getExtra != null) n.setExtra(e.getExtra)
    if (e.getLastAccessTime != null) n.setLastAccessTime(e.getLastAccessTime)
    if (e.getLastModifiedTime != null)
      n.setLastModifiedTime(e.getLastModifiedTime)
//    n.setMethod( e.getMethod )
//    n.setSize( e.getSize )
    n.setTime(e.getTime)

    n
  }

  def fixJar(input: File, output: File): Unit = {
    val out = new JarOutputStream(new FileOutputStream(output))
    val in = new JarInputStream(new FileInputStream(input))

    var zipEntry: ZipEntry = null
    while ({ zipEntry = in.getNextEntry; zipEntry } != null) {
      val outze = copyZE(zipEntry)
      logger.info(s"""Copying ${zipEntry.getName} to ${outze.getName}""")
      out.putNextEntry(outze)
      val buf = new Array[Byte](10240)
      var rlen = 0
      while ({ rlen = in.read(buf); rlen } > 0) {
        out.write(buf, 0, rlen)
      }
    }
    out.finish()
    out.close()
    in.close()
  }
}
