package com.github.thebridsk.bridge.test.manual

import com.github.thebridsk.utilities.main.Main
import java.util.zip.ZipFile
import java.io.File

object InspectJars extends Main {

  class EnumerationIterator[A](e : java.util.Enumeration[A])
      extends Iterator[A] {
    def hasNext = e.hasMoreElements
    def next() = e.nextElement()
  }

  def execute(): Int = {
    val cp = sys.env.getOrElse("CLASSPATH","")
    println(s"Classpath is $cp")

    val cpp = sys.props.getOrElse("java.class.path","")
    println(s"java.class.path is $cp")

    cpp.split(';').filter(_.endsWith(".jar")).foreach{ fname =>
      val filename = new File(fname)
      val name = filename.getName()
      val zfile = new ZipFile(filename)
      val en = new EnumerationIterator(zfile.entries())
      en.filter( zef => zef.getName().endsWith("/module-info.class") || zef.getName().endsWith("/9")).foreach { zef =>
        val isdir = zef.isDirectory()
        println(s"$name: ${zef.getName()} isdir=$isdir")
      }
    }


    0
  }
}
