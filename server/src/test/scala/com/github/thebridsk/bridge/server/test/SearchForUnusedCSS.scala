package com.github.thebridsk.bridge.server.test

import com.github.thebridsk.utilities.main.Main
import scala.reflect.io.Path
import scala.reflect.io.Directory
import scala.io.Source
import scala.reflect.io.File
import com.github.thebridsk.source.SourcePosition
import org.scalactic.source.Position

object SearchForUnusedCSS extends Main {

  import com.github.thebridsk.utilities.main.Converters._

  val cssDir = trailArg[Directory](
      name = "cssdir",
      descr = "Directory that contains the css files",
      required = true,
      default = Some( Directory("./src/main/public/css") ),
      hidden = false)

  val scalaDir = trailArg[Directory](
      name = "scaladir",
      descr = "Directory that contains the scala files",
      required = true,
      default = Some( Directory("../client/src/main/scala") ),
      hidden = false)

  case class Entry( cssClass: String, styleName: List[String] = List(), styleDef: List[SourcePosition] = List(), sourceUse: List[SourcePosition] = List() ) {
    def addStyleName( s: String ) = {
      if (styleName.contains(s)) this
      else copy(styleName = s::styleName )
    }
    def addStyleDef( f: SourcePosition) = {
      if (styleDef.contains(f)) this
      else copy( styleDef = f::styleDef )
    }
    def addSource( f: SourcePosition) = {
      if (sourceUse.contains(f)) this
      else copy( sourceUse = f::sourceUse )
    }
  }

  def execute() = {

    val classes = getCssClasses( cssDir() )
    logger.info( s"""Found the following CSS classes:${classes.mkString("\n  ", "\n  ", "")}""" )
    val style = searchForStyleDefinition(classes, scalaDir())

    logger.info( s"""The following locations were found:""" )

    style.foreach { e =>
      logger.info( s"""  ${e.cssClass} -> ${e.styleName} at""" )
      e.styleDef.foreach { loc =>
        logger.info( s"    ${loc.line}" )
      }
    }

    val use = searchForUse(style, scalaDir())

    logger.info( s"""The following uses were found:""" )

    use.foreach { e =>
      logger.info( s"""  ${e.cssClass} -> ${e.styleName} defs at""" )
      e.styleDef.foreach { loc =>
        logger.info( s"    ${loc.line}" )
      }
      logger.info( "  Uses" )
      e.sourceUse.foreach { loc =>
        logger.info( s"    ${loc.line}" )
      }
    }

    use.foreach { e =>
      if (!e.sourceUse.isEmpty) {
        logger.info( s"""CSS ClassName ${e.cssClass} Styles [${e.styleName.mkString(",")}]""" )
        logger.info( e.sourceUse.map(p=>p.line).mkString("  Used at:\n    ", "\n    ", "") )
      }
    }

    val user = use.map { e =>
      val uses = e.sourceUse.filter( sp => !sp.pos.fileName.contains("Styles") )
      e.copy(sourceUse=uses)
    }

    logger.info( "CSS class names that are not used" )
    user.foreach { e =>
      if (e.sourceUse.isEmpty) {
        logger.info( s"""CSS ClassName ${e.cssClass} Styles [${e.styleName.mkString(",")}] defs:""" )
        e.styleDef.foreach { loc =>
          logger.info( s"    ${loc.line}" )
        }
      }
    }
    0
  }

  val patternClassname = """\.([a-zA-Z][a-zA-Z0-9]+)""".r
  def getCssClasses( dir: Directory ) = {
    dir.files.flatMap { f =>
      Source.fromFile(f.jfile, "UTF-8").getLines().flatMap { line =>
        patternClassname.findAllIn(line).matchData.map { matcher => matcher.group(1) }
      }
    }.toList.distinct.sorted.map( n => Entry(n))
  }

  def searchForStyleDefinition( entries: List[Entry], dir: Directory ): List[Entry] = {
    dir.deepFiles.map { f =>
      logger.info(s"Found file ${f}")
      f.name == "RootStyles.scala"
      f
    }.foldLeft(entries) { (acentries,f) =>

      f.lines("UTF-8").zipWithIndex.foldLeft(acentries) { (ac,v) =>
        val (line, iminus1) = v
        val i = iminus1+1

        ac.map { e =>
          val pat = s"""val +([a-zA-Z0-9]+) *= *cls\\( *"[ a-zA-Z0-9]* *${e.cssClass}(?: +[a-zA-Z0-9]+)* *" *\\)""".r

          val r = pat.findAllIn(line).matchData
          if (r.isEmpty) e
          else {
            r.map { matcher =>
              matcher.group(1)
            }.foldLeft( e ) { (ac,v) =>
              ac.addStyleDef( SourcePosition(Position(f.name,f.toString(),i)) ).addStyleName(v)
            }
          }
        }
      }
    } //.filter(e=>e.cssClass=="rootThankYouDiv")
  }

  def searchForUse( entries: List[Entry], dir: Directory ): List[Entry] = {
    dir.deepFiles.map { f =>
      logger.info(s"Found file ${f}")
      f.name == "RootStyles.scala" || f.name == "ThankYouPage.scala"
      f
    }.foldLeft(entries) { (acentries,f) =>

      f.lines("UTF-8").zipWithIndex.foldLeft(acentries) { (ac,v) =>
        val (line, iminus1) = v
        val i = iminus1+1

        ac.map { e =>
          e.styleName.foldLeft(e) { (ace,dname) =>
            if (line.contains(dname)) ace.addSource( SourcePosition(Position(f.name,f.toString(),i)) )
            else ace
          }
        }
      }
    }
  }
}
