package com.github.thebridsk.bridge.client.pages.hand

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.clientcommon.logging.LogLifecycleToServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.MyEnumeration
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.InputMethod
import com.github.thebridsk.utilities.logging.Level
import com.github.thebridsk.bridge.data.maneuvers.TableManeuvers
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.Button
import com.github.thebridsk.bridge.client.pages.Pixels
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.icons.Camera
import org.scalajs.dom.raw.FileList
import org.scalajs.dom.raw.FormData
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.materialui.icons.Photo
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import org.scalajs.dom.raw.FileReader
import org.scalajs.dom.raw.File

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * Component( Component.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object Picture {
  import PictureInternal._

  case class Props( imagefile: Option[File], imageUrl: Option[String] )

  /**
   * Display an image.
   * @param imageFile File object that contains the image to display
   * @param imageUrl URL of image to display, only displayed if imageFile is None.
   */
  def apply( imageFile: Option[File], imageUrl: Option[String] ) = component(Props(imageFile,imageUrl))

}

object PictureInternal {
  import Picture._

  val logger = Logger("bridge.Picture")

  case class State( fileURL: Option[String] = None )

  class Backend(scope: BackendScope[Props, State]) {

    import org.scalajs.dom.html
    private val canvasRef = Ref[html.Canvas]

    def render(props: Props,state: State) = {
      <.div(
        ^.id := "HandPicture",
        state.fileURL.orElse(props.imageUrl).whenDefined { f =>
          <.img( ^.src := f )
        }
      )
    }

    def readFile(p: Props) = {
      if (p.imagefile.isDefined) {
        val reader = new FileReader
        reader.onload = (event) => {
          val dataURL = reader.result.toString()
          scope.withEffectsImpure.modState( s => s.copy( fileURL = Some(dataURL) ))
        }
        reader.readAsDataURL(p.imagefile.get)
      }
    }

    val didMount = scope.stateProps { (s,p) => Callback {
      logger.fine(s"Picture.didMount")
      readFile(p)
    }}

    val willUnmount = Callback {
      logger.info("Picture.willUnmount")

    }

    def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = scope.stateProps { (state,props) => Callback {
      logger.fine(s"Picture.didUpdate")
      val changed = (cdu.prevProps.imagefile.isDefined != cdu.prevProps.imagefile.isDefined) ||
                    (cdu.prevProps.imagefile.isDefined && cdu.prevProps.imagefile.get.name != props.imagefile.get.name )
      if (changed) {
        scope.withEffectsImpure.setState( state.copy(fileURL = None))
        readFile(props)
      }

    }}

  }

  val component = ScalaComponent.builder[Props]("Picture")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount( scope => scope.backend.didMount)
    .componentWillUnmount( scope => scope.backend.willUnmount )
    .componentDidUpdate( cdu => cdu.backend.didUpdate(cdu) )
    .build

}
