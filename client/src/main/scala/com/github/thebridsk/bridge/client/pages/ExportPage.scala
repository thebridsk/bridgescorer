package com.github.thebridsk.bridge.client.pages

import scala.scalajs.js
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerURL
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerVersion
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.data.ServerVersion
import com.github.thebridsk.bridge.client.version.VersionClient
import com.github.thebridsk.bridge.data.version.VersionShared
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.AppRouter.Home
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsArray
import com.github.thebridsk.bridge.clientcommon.react.AppButtonLink
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import play.api.libs.json.JsValue
import play.api.libs.json.JsString
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.clientcommon.pages.GotoPage
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ImportPage( ImportPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ExportPage {
  import ExportPageInternal._

  case class Props( router: BridgeRouter[AppPage] )

  def apply( router: BridgeRouter[AppPage] ) = component( Props(router))

}

object ExportPageInternal {
  import ExportPage._

  val logger = Logger("bridge.ExportPage")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( dupids: Option[List[String]] = None,
                    duprids: Option[List[String]] = None,
                    chiids: Option[List[String]] = None,
                    rubids: Option[List[String]] = None,
                    selectedIds: Option[List[String]] = None
                  ) {
    def isExportingAll() = {
      selectedIds match {
        case Some(selected) =>
          dupids.map( l => l.forall( id => selected contains id )).getOrElse(true) &&
          duprids.map( l => l.forall( id => selected contains id )).getOrElse(true) &&
          chiids.map( l => l.forall( id => selected contains id )).getOrElse(true) &&
          rubids.map( l => l.forall( id => selected contains id )).getOrElse(true)
        case None =>
          dupids.isDefined && duprids.isDefined && chiids.isDefined && rubids.isDefined
      }
    }
  }

  def jsValueToString( jsV: JsValue ) = {
    jsV match {
      case JsString(s) => s
      case _ => jsV.toString()
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val didMount = Callback {
      GraphQLClient.request("""{ duplicateIds, duplicateResultIds, chicagoIds, rubberIds }""").foreach { resp =>
        scope.withEffectsImpure.modState { s0 =>
          resp.data match {
            case Some(json) =>
              val s1 = json \ "duplicateIds" match {
                case JsDefined( JsArray( array ) ) =>
                  val list = array.map( s => jsValueToString(s) ).toList
                  val slist = list.sortWith( (l,r) => Id.idComparer(l, r)<0)
                  s0.copy(dupids = Some(slist))
                case JsDefined( _ ) =>
                  s0.copy(dupids = Some(List()))
                case x: JsUndefined =>
                  s0.copy(dupids = Some(List()))
              }
              val s2 = json \ "duplicateResultIds" match {
                case JsDefined( JsArray( array ) ) =>
                  val list = array.map( s => jsValueToString(s) ).toList
                  val slist = list.sortWith( (l,r) => Id.idComparer(l, r)<0)
                  s1.copy(duprids = Some(slist))
                case JsDefined( _ ) =>
                  s1.copy(duprids = Some(List()))
                case x: JsUndefined =>
                  s1.copy(duprids = Some(List()))
              }
              val s3 = json \ "chicagoIds" match {
                case JsDefined( JsArray( array ) ) =>
                  val list = array.map( s => jsValueToString(s) ).toList
                  val slist = list.sortWith( (l,r) => Id.idComparer(l, r)<0)
                  s2.copy(chiids = Some(slist))
                case JsDefined( _ ) =>
                  s2.copy(chiids = Some(List()))
                case x: JsUndefined =>
                  s2.copy(chiids = Some(List()))
              }
              val s4 = json \ "rubberIds" match {
                case JsDefined( JsArray( array ) ) =>
                  val list = array.map( s => jsValueToString(s) ).toList
                  val slist = list.sortWith( (l,r) => Id.idComparer(l, r)<0)
                  s3.copy(rubids = Some(slist))
                case JsDefined( _ ) =>
                  s3.copy(rubids = Some(List()))
                case x: JsUndefined =>
                  s3.copy(rubids = Some(List()))
              }
              s4
            case None =>
              s0.copy(dupids = Some(List()), chiids = Some(List()), rubids = Some(List()))
          }
        }
      }
    }

    def toggleSelectedId( id: String ) = scope.modState { s =>
      val current = s.selectedIds.getOrElse(List())
      val trydelete = current.filter( i => i!=id )
      val add = if (current.size == trydelete.size) id::trydelete
                else trydelete
      val next = if (add.isEmpty) None else Some(add)
      s.copy(selectedIds=next)
    }

    val selectAll = scope.modState { s =>
      val sel = s.dupids.getOrElse(List()):::s.chiids.getOrElse(List()):::s.rubids.getOrElse(List())
      s.copy(selectedIds = Some(sel))
    }

    val selectClearAll = scope.modState { s =>
      s.copy(selectedIds = None)
    }

    def selectAll( list: Option[List[String]] ) = scope.modState { s =>
      list.map { l =>
        l.foldLeft(s){(ac,v) =>
          ac.selectedIds match {
            case Some(selected) =>
              if (selected.contains(v)) ac
              else s.copy( selectedIds = Some(v::selected))
            case None =>
              ac.copy( selectedIds = Some(v::Nil))
          }
        }
      }.getOrElse(s)
    }

    def selectClearAll( list: Option[List[String]] ) = scope.modState { s =>
      list.map { l =>
        s.selectedIds match {
          case Some(selected) =>
            val newsel = selected.filter( id => !l.contains(id))
            s.copy( selectedIds = Some(newsel))
          case None =>
            s
        }
      }.getOrElse(s)
    }

    val indent = <.span( ^.dangerouslySetInnerHtml := "&nbsp;&nbsp;&nbsp;" )

    def render( props: Props, state: State ) = {

      def showList( tid: String, header: String, list: Option[List[String]] ) = {
        <.div(
          <.h2(
            header,
            list.filter( l => l.size > 2 ).map { l =>
              TagMod(
                indent,
                AppButton( tid+"SelectAll", "Select All", ^.onClick --> selectAll(list)),
                AppButton( tid+"ClearAll", "Clear All", ^.onClick --> selectClearAll(list))
              )
            }.whenDefined
          ),
          list match {
            case Some(l) =>
              if (l.isEmpty) {
                <.p("None found")
              } else {
                <.ul(
                  l.map { id =>
                    val selected = state.selectedIds.map( ids => ids.contains(id) ).getOrElse(false)
                    <.li(
                      CheckBox( tid+id, id, selected, toggleSelectedId(id) )
                    )
                  }.toTagMod
                )
              }
            case None =>
              <.p( "Working" )
          }
        )
      }

      val dupids = if (state.dupids.isEmpty || state.duprids.isEmpty) {
        None
      } else {
        Some(state.dupids.getOrElse(List()):::state.duprids.getOrElse(List()))
      }

      <.div(
        RootBridgeAppBar(
            Seq(MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Export Bridge Store",
                    )
                )
            ),
            None,
            props.router
        )(),
        <.div(
          rootStyles.exportPageDiv,
          <.div(
            showList( "Duplicate", "Duplicate", dupids ),
            showList( "Chicago", "Chicago", state.chiids ),
            showList( "Rubber", "Rubber", state.rubids ),
            <.p( if (state.isExportingAll()) "Exporting all" else "Exporting some" )
          ),

          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton( "Home", "Home",
                         props.router.setOnClick(Home)),
              {
                val filter = state.selectedIds.map(list => list.mkString("?filter=", ",", "")).getOrElse("")
                val path = GotoPage.getURL( s"""/v1/export${filter}""" )
                AppButtonLink( "Export", "Export", path,
                               baseStyles.appButton
                )
              }
            ),
            <.div(
              baseStyles.divFooterCenter,
              AppButton( "SelectAll", "Select All", ^.onClick --> selectAll),
              AppButton( "ClearAll", "Clear All", ^.onClick --> selectClearAll)
            )
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ExportPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .build
}

