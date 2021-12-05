
import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._
import java.nio.charset.StandardCharsets

object BldMaterialUI {

  val icons = Seq(
    "Camera",
    "Check",
    "CheckBox",
    "CheckBoxOutlineBlank",
    "ChevronRight",
    "DeleteForever",
    "Fullscreen",
    "FullscreenExit",
    "Help",
    "Home",
    "Menu",
    "MoreVert",
    "Photo",
    "Place",
    "RadioButtonChecked",
    "RadioButtonUnchecked",
  )

  lazy val materialui = project.in(file("materialui")).
    configure( commonSettings ).
    enablePlugins(ScalaJSPlugin).
    settings(
      libraryDependencies ++= materialUiDeps.value,
      sourceGenerators in Compile += Def.task {
        val dir = (Compile / sourceManaged).value
        val pack = "com.github.thebridsk.materialui.icons"
        generateMuiIcons(dir,pack,icons)
      }
    )

  def generateMuiIcons( dir: File, pack: String, names: Seq[String] ) = {
    names.map( icon => generateMuiIcon(dir, pack, icon))
  }

  def generateMuiIcon( dir: File, pack: String, name: String ) = {
    val icon = s"""
      |package ${pack}
      |
      |import scala.scalajs.js
      |import scala.scalajs.js.annotation.JSImport
      |import japgolly.scalajs.react.JsComponent
      |import japgolly.scalajs.react.Children
      |import com.github.thebridsk.materialui.icons.SvgIconBase
      |import com.github.thebridsk.materialui.icons.SvgIconProps
      |
      |object ${name} extends SvgIconBase {
      |  @js.native @JSImport("@mui/icons-material/${name}", JSImport.Default)
      |  private object icon extends js.Any
      |  protected val f = JsComponent[SvgIconProps, Children.Varargs, Null](icon)
      |}
      |""".stripMargin

    val outf = dir / pack.replace(".","/") / s"${name}.scala"
    IO.write( outf, icon, StandardCharsets.UTF_8 )
    outf
  }
}
