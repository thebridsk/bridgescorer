package com.github.thebridsk.bridge.client.pages.rubber

import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.utilities.logging.Logger

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.controller.RubberController
import com.github.thebridsk.bridge.data.rubber.RubberScoring
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.RubberMatchNamesView
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.RubberMatchView
import com.github.thebridsk.utilities.logging.Level
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.ListViewBase
import com.github.thebridsk.bridge.client.bridge.store.RubberListStore
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.ListView
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.ImportListView
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLResponse
import com.github.thebridsk.bridge.clientcommon.rest2.ResultHolder
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * PageRubberList( PageRubberList.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object PageRubberList {
  import PageRubberListInternal._

  case class Props(page: ListViewBase, routerCtl: BridgeRouter[RubberPage])

  def apply(
      page: ListViewBase,
      routerCtl: BridgeRouter[RubberPage]
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(page, routerCtl))

}

object PageRubberListInternal {
  import PageRubberList._
  import RubberStyles._

  val logger: Logger = Logger("bridge.PageRubberList")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    *
    * @constructor
    * @param askingToDelete The Id of rubber match being deleted.  None if not deleting.
    * @param popupMsg show message in popup if not None.
    */
  case class State(
      askingToDelete: Option[MatchRubber.Id] = None,
      popupMsg: Option[String] = None,
      info: Boolean = false
  )

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global

    def delete(id: MatchRubber.Id): Callback =
      scope.modState(s => s.copy(askingToDelete = Some(id)))

    val deleteOK: Callback = scope.modState { s =>
      s.askingToDelete
        .map { id =>
          RubberController.deleteRubber(id)
          val ns = s.copy(askingToDelete = None)
          ns
        }
        .getOrElse(s)
    }

    val deleteCancel: Callback =
      scope.modState(s => s.copy(askingToDelete = None))

    val resultRubber: ResultHolder[MatchRubber] = ResultHolder[MatchRubber]()
    val resultGraphQL: ResultHolder[GraphQLResponse] =
      ResultHolder[GraphQLResponse]()

    val cancel: Callback = Callback {
      resultRubber.cancel()
      resultGraphQL.cancel()
    } >> scope.modState(s => s.copy(popupMsg = None))

    val newRubber: Callback =
      scope.modState(
        s => s.copy(popupMsg = Some("Creating new rubber match")),
        Callback {
          val rescre = RubberController.createMatch()
          resultRubber.set(rescre)
          rescre.foreach(created => {
            logger.info("Got new rubber match " + created.id)
            scope
              .modState(
                s => s.copy(popupMsg = None),
                scope.props >>= { p =>
                  p.routerCtl.set(RubberMatchNamesView(created.id.id))
                }
              )
              .runNow()
          })
        }
      )

    def showRubber(chi: MatchRubber): Callback =
      Callback {
        RubberController.showMatch(chi)
      } >> {
        scope.withEffectsImpure.props.routerCtl.set(RubberMatchView(chi.id.id))
      }

    def setMessage(msg: String, info: Boolean = false): Unit =
      scope.withEffectsImpure.modState(s =>
        s.copy(popupMsg = Some(msg), info = info)
      )

    def importRubber(importId: String, rubid: MatchRubber.Id): Callback =
      scope.modState(
        s =>
          s.copy(
            popupMsg = Some(
              s"Importing Rubber Match ${rubid.id} from import ${importId}"
            )
          ),
        Callback {
          val query =
            """mutation importRubber( $importId: ImportId!, $rubId: RubberId! ) {
              |  import( id: $importId ) {
              |    importrubber( id: $rubId ) {
              |      id
              |    }
              |  }
              |}
              |""".stripMargin
          val vars = JsObject(
            Seq("importId" -> JsString(importId), "rubId" -> JsString(rubid.id))
          )
          val op = Some("importRubber")
          val result = GraphQLClient.request(query, Some(vars), op)
          resultGraphQL.set(result)
          result
            .map { gr =>
              gr.data match {
                case Some(data) =>
                  data \ "import" \ "importrubber" \ "id" match {
                    case JsDefined(JsString(newid)) =>
                      setMessage(
                        s"import rubber ${rubid.id} from ${importId}, new ID ${newid}",
                        true
                      )
                      initializeNewSummary(scope.withEffectsImpure.props)
                    case JsDefined(x) =>
                      setMessage(
                        s"expecting string on import rubber ${rubid.id} from ${importId}, got ${x}"
                      )
                    case _: JsUndefined =>
                      setMessage(
                        s"error import rubber ${rubid.id} from ${importId}, did not find import/importrubber/id field"
                      )
                  }
                case None =>
                  setMessage(
                    s"error import rubber ${rubid.id} from ${importId}, ${gr.getError()}"
                  )
              }
            }
            .recover {
              case x: Exception =>
                logger.warning(
                  s"exception import rubber ${rubid.id} from ${importId}",
                  x
                )
                setMessage(
                  s"exception import rubber ${rubid.id} from ${importId}"
                )
            }
            .foreach { x => }
        }
      )

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      val importId = props.page match {
        case ilv: ImportListView => Some(ilv.getDecodedId)
        case _                   => None
      }

      val (bok, bcancel) = if (state.info) {
        (Some(cancel), None)
      } else {
        (None, Some(cancel))
      }

      val (msg, funOk, funCancel) = state.popupMsg
        .map(msg => (Some(msg), bok, bcancel))
        .getOrElse(
          (
            state.askingToDelete
              .map(id => s"Are you sure you want to delete Rubber match ${id}"),
            Some(deleteOK),
            Some(deleteCancel)
          )
        )
      <.div(
        PopupOkCancel(msg.map(s => s), funOk, funCancel),
        RubberPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span("List")
            )
          ),
          helpurl = "../help/rubber/list.html",
          routeCtl = props.routerCtl
        )(),
        <.div(
          rubStyles.listPage,
          if (importId == RubberListStore.getImportId) {
            RubberListStore.getRubberSummary() match {
              case Some(rubberlist) =>
                val rubbers = rubberlist.sortWith { (l, r) =>
                  if (l.created == r.created) l.id > r.id
                  else l.created > r.created
                }
                <.table(
                  <.thead(
                    <.tr(
                      <.th("Id"),
                      importId.map { id =>
                        TagMod(
                          <.th("Import from"),
                          <.th("Best Match")
                        )
                      }.whenDefined,
                      <.th("Created", <.br(), "Updated"),
                      <.th("Complete"),
                      <.th("North", <.br(), "South"),
                      <.th("NS Score"),
                      <.th("East", <.br(), "West"),
                      <.th("EW Score"),
                      <.th("")
                    )
                  ),
                  <.tbody(
                    <.tr(
                      <.td(""),
                      importId
                        .map { id =>
                          TagMod(
                            <.th(id),
                            <.th(""),
                            <.th("")
                          )
                        }
                        .getOrElse(
                          <.td(
                            AppButton("New", "New", ^.onClick --> newRubber)
                          )
                        ),
                      <.td(""),
                      <.td(^.colSpan := 4, ""),
                      <.td("")
                    ),
                    (0 until rubbers.length).map { i =>
                      val key = "Game" + i
                      val r = RubberScoring(rubbers(i))
                      RubberRow(this, props, state, i, r, importId)
                    }.toTagMod
                  )
                )
              case None =>
                HomePage.loading
            }
          } else {
            HomePage.loading
          }
        )
      )

    }

    def RubberRow(
        backend: Backend,
        props: Props,
        state: State,
        game: Int,
        rubber: RubberScoring,
        importId: Option[String]
    ) = { // scalafix:ok ExplicitResultTypes; React
      val id = rubber.rubber.id
      val date = id
      val created = DateUtils.formatDate(rubber.rubber.created)
      val updated = DateUtils.formatDate(rubber.rubber.updated)

      <.tr(
        <.td(
          AppButton(
            "Rubber" + id.id,
            id.id,
            baseStyles.appButton100,
            ^.onClick --> backend.showRubber(rubber.rubber),
            importId.map { id =>
              ^.disabled := true
            }.whenDefined
          )
        ),
        importId.map { iid =>
          TagMod(
            <.td(
              AppButton(
                "ImportRubber_" + id.id,
                "Import",
                baseStyles.appButton100,
                ^.onClick --> backend.importRubber(iid, id)
              )
            ),
            <.td(
              rubber.rubber.bestMatch.map { bm =>
                if (bm.id.isDefined && bm.sameness > 90) {
                  val title = bm.htmlTitle
                  TagMod(
                    Tooltip(
                      f"""${bm.id.get.id} ${bm.sameness}%.2f%%""",
                      <.div(title)
                    )
                  )
                } else {
                  TagMod()
                }
              }.whenDefined
            )
          )
        }.whenDefined,
        <.td(created, <.br(), updated),
        <.td(if (rubber.done) "done" else ""),
        <.td(rubber.rubber.north, <.br(), rubber.rubber.south),
        <.td(rubber.nsTotal.toString()),
        <.td(rubber.rubber.east, <.br(), rubber.rubber.west),
        <.td(rubber.ewTotal.toString()),
        <.td(
          importId.isEmpty ?= AppButton(
            s"Delete_${id.id}",
            "Delete",
            ^.onClick --> backend.delete(id)
          )
        )
      )
    }

    val storeCallback: Callback = scope.props >>= { (p) =>
      Callback {
        logger.fine(s"Got rubberlist update, importid=${p.page}")
      } >> scope.forceUpdate
    }

    def summaryError(): Unit =
      scope.withEffectsImpure.modState(s =>
        s.copy(popupMsg = Some("Error getting duplicate summary"))
      )

    val didMount: Callback = scope.props >>= { (p) =>
      Callback {
        // make AJAX rest call here
        logger.finer("PageRubberList: Sending rubber list request to server")
        RubberListStore.addChangeListener(storeCallback)
        initializeNewSummary(p)
      }
    }

    val willUnmount: Callback = Callback {
      // TODO: release RubberListStore memory
    }

    def initializeNewSummary(props: Props): Unit = {
      props.page match {
        case isv: ImportListView =>
          val importId = isv.getDecodedId
          RubberController.getImportSummary(importId, summaryError _)
        case ListView =>
          RubberController.getSummary(summaryError _)
      }
    }
  }

  implicit val loggerForReactComponents: Logger = Logger(
    "bridge.PageChicagoList"
  )
  implicit val defaultTraceLevelForReactComponents: Level = Level.FINER

  def didUpdate(
      cdu: ComponentDidUpdate[Props, State, Backend, Unit]
  ): Callback =
    Callback {
      val props = cdu.currentProps
      val prevProps = cdu.prevProps
      if (prevProps.page != props.page) {
        cdu.backend.initializeNewSummary(props)
      }
    }

  private[rubber] val component = ScalaComponent
    .builder[Props]("PageRubberList")
    .initialStateFromProps { props =>
      State()
    }
    .backend(new Backend(_))
    .renderBackend
//                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .componentDidUpdate(didUpdate)
    .build
}
