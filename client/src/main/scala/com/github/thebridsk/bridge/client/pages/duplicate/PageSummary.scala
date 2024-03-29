package com.github.thebridsk.bridge.client.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.controller.Controller
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.NewDuplicateView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.FinishedScoreboardsView
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
import com.github.thebridsk.bridge.clientcommon.rest2.RequestCancelled
import com.github.thebridsk.bridge.clientcommon.rest2.ResultHolder
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.bridge.store.DuplicateSummaryStore
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SuggestionView
import com.github.thebridsk.bridge.clientcommon.react.RadioButton
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.StatsView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.ImportSummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryViewBase
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLResponse
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.materialui.icons
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import play.api.libs.json.Reads

/**
  * Shows a summary page of all duplicate matches from the database.
  * Each match has a button that that shows that match, by going to the ScoreboardView(id) page.
  * There is also a button to create a new match, by going to the NewScoreboardView page.
  *
  * The data is obtained from the DuplicateStore object.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * PageSummary( routerCtl: BridgeRouter[DuplicatePage] )
  * </code></pre>
  *
  * @author werewolf
  */
object PageSummary {
  import Internal._

  case class Props(
      routerCtl: BridgeRouter[DuplicatePage],
      page: SummaryViewBase,
      defaultRows: Int = 10
  )

  /**
    * Instantiate the component
    *
    * @param routerCtl
    * @param page
    * @param defaultRows the number of matches to display, default is 10
    * @return the unmounted react component
    */
  def apply(
      routerCtl: BridgeRouter[DuplicatePage],
      page: SummaryViewBase,
      defaultRows: Int = 10
  ) =
    component(
      Props(routerCtl, page, defaultRows)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  object Internal {
    import DuplicateStyles._

    val logger: Logger = Logger("bridge.PageSummary")

    private val SummaryHeader = ScalaComponent
      .builder[(SummaryPeople, Props, State, Backend, Option[String])](
        "SummaryRow"
      )
      .render_P(props => {
        val (tp, pr, state, backend, importId) = props
        val isImportStore = pr.page.isInstanceOf[ImportSummaryView]

        val result = state.useIMP
          .map(useIMP =>
            if (useIMP) " (International Match Points)" else " (Match Points)"
          )
          .getOrElse("")

        val allplayers = tp.allPlayers.filter(n => n != "")

        <.thead(
          <.tr(
            <.th("Id"),
            importId.map { id =>
              TagMod(
                <.th(s"Import from $id"),
                <.th("Best Match")
              )
            }.whenDefined,
            state.forPrint ?= <.th(
              importId.map(id => "Import").getOrElse("Print").toString
            ),
            <.th("Finished"),
            <.th("Created", <.br(), "Last Updated"),
            <.th("Scoring", <.br, "Method"),
            allplayers.map { p =>
              <.th(
                <.span(p)
              )
            }.toTagMod,
            <.th("Totals")
          )
        )
      })
      .build

    private val SummaryRow = ScalaComponent
      .builder[
        (SummaryPeople, DuplicateSummary, Props, State, Backend, Option[String])
      ]("SummaryRow")
      .render_P(props => {
        val (tp, ds, pr, st, back, importId) = props
        <.tr(
          <.td(
            DuplicateSummary.useId(
              ds.id,
              { id =>
                val t: TagMod =
                  AppButton(
                    s"Duplicate_${id.id}",
                    ds.id.id,
                    baseStyles.appButton100,
                    pr.routerCtl.setOnClick(pr.page.getScoreboardPage(id)),
                    importId.map { id => ^.disabled := true }.whenDefined
                  )
                t
              },
              { id =>
                val t: TagMod =
                  AppButton(
                    s"Result_${id.id}",
                    ds.id.id,
                    baseStyles.appButton100,
                    pr.routerCtl.setOnClick(pr.page.getDuplicateResultPage(id)),
                    importId.map { id => ^.disabled := true }.whenDefined
                  )
                t
              },
              TagMod()
            )
          ),
          importId.map { iid =>
            TagMod(
              <.td(
                ds.idAsDuplicateId
                  .map { id =>
                    val t: TagMod =
                      AppButton(
                        s"ImportDuplicate_${id.id}",
                        "Import",
                        baseStyles.appButton100,
                        ^.onClick --> back.importDuplicateMatch(iid, id)
                      )
                    t
                  }
                  .getOrElse {
                    ds.idAsDuplicateResultId
                      .map { id =>
                        val t: TagMod =
                          AppButton(
                            s"ImportResult_${id.id}",
                            "Import",
                            baseStyles.appButton100,
                            ^.onClick --> back.importDuplicateResult(iid, id)
                          )
                        t
                      }
                      .getOrElse {
                        TagMod()
                      }
                  }
              ),
              <.td(
                ds.bestMatch.map { bm =>
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
          st.forPrint ?= <.td(
            <.input.checkbox(
              ^.checked := st.selected.contains(ds.id),
              ^.onClick --> back.toggleSelect(ds.id)
            )
          ),
          <.td((if (ds.finished) "done"; else "")),
          <.td(
            DateUtils.formatDate(ds.created),
            <.br(),
            DateUtils.formatDate(ds.updated)
          ),
          <.td(ds.scoringmethod.getOrElse("MP").toString()),
          tp.allPlayers
            .filter(p => p != "")
            .map { p =>
              if (st.useIMP.getOrElse(ds.isIMP)) {
                if (ds.hasImpScores) {
                  <.td(
                    ds.playerPlacesImp().get(p) match {
                      case Some(place) => <.span(place.toString)
                      case None        => <.span()
                    },
                    ds.playerScoresImp().get(p) match {
                      case Some(place) => <.span(<.br, f"${place}%.1f")
                      case None        => <.span()
                    }
                  )
                } else {
                  <.td("NA")
                }
              } else {
                if (ds.hasMpScores) {
                  <.td(
                    ds.playerPlaces().get(p) match {
                      case Some(place) => <.span(place.toString)
                      case None        => <.span()
                    },
                    ds.playerScores().get(p) match {
                      case Some(place) =>
                        <.span(<.br, Utils.toPointsString(place))
                      case None => <.span()
                    }
                  )
                } else {
                  <.td("NA")
                }
              }
            }
            .toTagMod,
          if (st.useIMP.getOrElse(ds.isIMP)) {
            <.td(
              Utils.toPointsString(
                tp.allPlayers
                  .filter(p => p != "")
                  .flatMap { p =>
                    ds.playerScoresImp().get(p) match {
                      case Some(place) => place :: Nil
                      case None        => Nil
                    }
                  }
                  .foldLeft(0.0)((ac, v) => ac + v)
              )
            )
          } else {
            <.td(
              Utils.toPointsString(
                tp.allPlayers
                  .filter(p => p != "")
                  .flatMap { p =>
                    ds.playerScores().get(p) match {
                      case Some(place) => place :: Nil
                      case None        => Nil
                    }
                  }
                  .foldLeft(0.0)((ac, v) => ac + v)
              )
            )
          }
        )
      })
      .build

    sealed trait ShowEntries
    object ShowMD extends ShowEntries
    object ShowMDR extends ShowEntries
    object ShowBoth extends ShowEntries

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(
        workingOnNew: Option[String],
        forPrint: Boolean,
        selected: List[DuplicateSummary.Id],
        showRows: Option[Int],
        alwaysShowAll: Boolean,
        showEntries: ShowEntries = ShowBoth,
        useIMP: Option[Boolean] = None,
        info: Boolean = false
  //                    anchorMainEl: js.UndefOr[Element] = js.undefined
    ) {

  //    def openMainMenu( n: Node ) = copy( anchorMainEl = n.asInstanceOf[Element] )
  //    def closeMainMenu() = copy( anchorMainEl = js.undefined )

      def withError(err: String, info: Boolean): State =
        copy(workingOnNew = Some(err), info = info)
      def clearError(): State = copy(workingOnNew = None)

      def isMP: Boolean = useIMP.getOrElse(true)
      def isIMP: Boolean = useIMP.getOrElse(false)

      def toggleIMP: State = {
        copy(useIMP = Some(!isIMP))
      }

      def nextIMPs: State = {
        val n = useIMP match {
          case None        => Some(false)
          case Some(false) => Some(true)
          case Some(true)  => None
        }
        copy(useIMP = n)
      }

    }

    case class ImportReturn(id: String)

    implicit val importReturnReader: Reads[ImportReturn] =
      Json.reads[ImportReturn]

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

  //    def handleMainClick( event: ReactEvent ) = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openMainMenu(currentTarget)).runNow() )
  //    def handleMainCloseClick( event: ReactEvent ) = scope.modState(s => s.closeMainMenu()).runNow()
  //    def handleMainClose( /* event: js.Object, reason: String */ ) = {
  //      logger.fine("HelpClose called")
  //      scope.modState(s => s.closeMainMenu()).runNow()
  //    }

      val resultDuplicate: ResultHolder[MatchDuplicate] =
        ResultHolder[MatchDuplicate]()
      val resultGraphQL: ResultHolder[GraphQLResponse] =
        ResultHolder[GraphQLResponse]()

      def show(what: ShowEntries): Callback =
        scope.modState(s => s.copy(showEntries = what))

      val cancel: Callback = Callback {
        resultDuplicate.cancel()
        resultGraphQL.cancel()
      } >> scope.modState(s => s.clearError())

      def setMessage(msg: String, info: Boolean = false): Unit =
        scope.withEffectsImpure.modState(s => s.withError(msg, info))

      def setMessageCB(msg: String): Callback =
        scope.modState(s => s.withError(msg, false))

      def newDuplicateTest(e: ReactEvent): Unit =
        scope
          .modState(
            s =>
              s.copy(workingOnNew =
                Some("Working on creating a new duplicate match")
              ),
            Callback {
              val result =
                Controller.createMatchDuplicate(test = true).recordFailure()
              resultDuplicate.set(result)
              result.foreach { created =>
                logger.info(
                  s"Got new duplicate match ${created.id}.  HomePage.mounted=${mounted}"
                )
                if (mounted)
                  scope.withEffectsImpure.props.routerCtl
                    .set(CompleteScoreboardView(created.id.id))
                    .runNow()
              }
              result.failed.foreach(t => {
                t match {
                  case x: RequestCancelled =>
                  case _ =>
                    scope.withEffectsImpure.modState(s =>
                      s.copy(workingOnNew =
                        Some("Failed to create a new duplicate match")
                      )
                    )
                }
              })
            }
          )
          .runNow()

      def toggleSelect(dupid: DuplicateSummary.Id): Callback =
        scope.modState(s => {
          val sel =
            if (s.selected.contains(dupid)) s.selected.filter(s => s != dupid)
            else dupid :: s.selected
          s.copy(selected = sel)
        })

      val clearAllSelected: Callback =
        scope.modState(s => s.copy(selected = List()))

      val selectedAll: Callback = scope.modState { (s, props) =>
        val (importid, summaries) = getDuplicateSummaries(props)
        val allIds = summaries
          .map { list =>
            list.map(sum => sum.id)
          }
          .getOrElse(List())
        s.copy(selected = allIds)
      }

      def forPrint(flagForPrint: Boolean): Callback =
        scope.modState(s => s.copy(forPrint = flagForPrint))

      def clickForPrint(flagForPrint: Boolean)(event: ReactEvent): Unit =
        forPrint(flagForPrint).runNow()

      val forPrintOk: Callback = CallbackTo {
        val s = scope.withEffectsImpure.state
        val mds = s.selected.reverse.map { id => id.id }.mkString(",")
        forPrint(false).runNow()
        (scope.withEffectsImpure.props, mds)
      } >>= {
        case (p, mds) =>
          p.routerCtl.set(FinishedScoreboardsView(mds))
      }

      val forPrintCancel: Callback = forPrint(false)

      def toggleRows(e: ReactEvent): Unit =
        scope.withEffectsImpure.modState { (s, props) =>
          val n = s.showRows match {
            case Some(r) => None
            case None    => Some(props.defaultRows)
          }
          s.copy(showRows = n)
        }

      def importDuplicateResult(
          importId: String,
          id: MatchDuplicateResult.Id
      ): Callback =
        scope.modState(
          s =>
            s.copy(workingOnNew =
              Some(s"Importing Duplicate Result ${id.id} from import ${importId}")
            ),
          Callback {
            val query =
              """mutation importDuplicate( $importId: ImportId!, $dupId: DuplicateResultId! ) {
                |  import( id: $importId ) {
                |    importduplicateresult( id: $dupId ) {
                |      id
                |    }
                |  }
                |}
                |""".stripMargin
            val vars = JsObject(
              Seq("importId" -> JsString(importId), "dupId" -> JsString(id.id))
            )
            val op = Some("importDuplicate")
            val result = GraphQLClient.request(query, Some(vars), op)
            resultGraphQL.set(result)
            result
              .map { gr =>
                gr.data match {
                  case Some(data) =>
                    data \ "import" \ "importduplicateresult" \ "id" match {
                      case JsDefined(JsString(newid)) =>
                        setMessage(
                          s"import duplicate result ${id.id} from ${importId}, new ID ${newid}",
                          true
                        )
                        initializeNewSummary(scope.withEffectsImpure.props)
                      case JsDefined(x) =>
                        setMessage(
                          s"expecting string on import duplicate result ${id.id} from ${importId}, got ${x}"
                        )
                      case _: JsUndefined =>
                        setMessage(
                          s"error import duplicate result ${id.id} from ${importId}, did not find import/importduplicateresult/id field"
                        )
                    }
                  case None =>
                    setMessage(
                      s"error import duplicate result ${id.id} from ${importId}, ${gr.getError()}"
                    )
                }
              }
              .recover {
                case x: Exception =>
                  logger.warning(
                    s"exception import duplicate result ${id.id} from ${importId}",
                    x
                  )
                  setMessage(
                    s"exception import duplicate result ${id.id} from ${importId}"
                  )
              }
              .foreach { x => }
          }
        )

      def importDuplicateMatch(
          importId: String,
          id: MatchDuplicate.Id
      ): Callback =
        scope.modState(
          s =>
            s.copy(workingOnNew =
              Some(s"Importing Duplicate Match ${id.id} from import ${importId}")
            ),
          Callback {
            val query =
              """mutation importDuplicate( $importId: ImportId!, $dupId: DuplicateId! ) {
                |  import( id: $importId ) {
                |    importduplicate( id: $dupId ) {
                |      id
                |    }
                |  }
                |}
                |""".stripMargin
            val vars = JsObject(
              Seq("importId" -> JsString(importId), "dupId" -> JsString(id.id))
            )
            val op = Some("importDuplicate")
            val result = GraphQLClient.request(query, Some(vars), op)
            resultGraphQL.set(result)
            result
              .map { gr =>
                gr.data match {
                  case Some(data) =>
                    data \ "import" \ "importduplicate" \ "id" match {
                      case JsDefined(JsString(newid)) =>
                        setMessage(
                          s"import duplicate ${id.id} from ${importId}, new ID ${newid}",
                          true
                        )
                        initializeNewSummary(scope.withEffectsImpure.props)
                      case JsDefined(x) =>
                        setMessage(
                          s"expecting string on import duplicate ${id.id} from ${importId}, got ${x}"
                        )
                      case _: JsUndefined =>
                        setMessage(
                          s"error import duplicate ${id.id} from ${importId}, did not find import/importduplicate/id field"
                        )
                    }
                  case None =>
                    setMessage(
                      s"error import duplicate ${id.id} from ${importId}, ${gr.getError()}"
                    )
                }
              }
              .recover {
                case x: Exception =>
                  logger.warning(
                    s"exception import duplicate ${id.id} from ${importId}",
                    x
                  )
                  setMessage(
                    s"exception import duplicate ${id.id} from ${importId}"
                  )
              }
              .foreach { x => }
          }
        )

      def importSelected(importId: String): Callback =
        scope.modState(
          { s =>
            val ids = s.selected.map(id => id.id)
            s.copy(workingOnNew =
              Some(
                s"Importing Duplicate Match ${ids.mkString(", ")} from import ${importId}"
              )
            )
          },
          scope.stateProps { (s, props) =>
            Callback {
              val (importid, summaries) = getDuplicateSummaries(props)

  //        val sortByDate = if (s.selected.isEmpty) {
  //          s.selected
  //        } else {
  //          summaries.map { summs =>
  //            val sorted = summs.sortWith((l,r) => l.created < r.created).map( sum => sum.id )
  //            sorted.filter(id=> s.selected.contains(id))
  //          }.getOrElse( s.selected )
  //        }
              val sortByDate = s.selected

              val fragment = sortByDate.map { id =>
                DuplicateSummary.useId(
                  id,
                  mdid =>
                    s"""${mdid.id}: importduplicate( id: "${mdid.id}") { id }""",
                  mdrid =>
                    s"""${mdrid.id}: importduplicateresult( id: "${mdrid.id}") { id }""",
                  ""
                )
              }
              val query = s"""mutation importDuplicate( $$importId: ImportId! ) {
                            |  import( id: $$importId ) {
                            |    ${fragment.mkString("\n    ", "\n    ", "")}
                            |  }
                            |}
                            |""".stripMargin
              val vars = JsObject(Seq("importId" -> JsString(importId)))
              val op = Some("importDuplicate")
              val result = GraphQLClient.request(query, Some(vars), op)
              resultGraphQL.set(result)
              result
                .map { gr =>
                  gr.data match {
                    case Some(data) =>
                      data \ "import" match {
                        case JsDefined(map: JsObject) =>
                          Json.fromJson[Map[String, ImportReturn]](map) match {
                            case JsSuccess(m, path) =>
                              val v = m.map(e => s"${e._1}->${e._2.id}")
                              setMessage(
                                s"import selected from ${importId}, old IDs -> new IDs: ${v
                                  .mkString(", ")}",
                                true
                              )
                              initializeNewSummary(scope.withEffectsImpure.props)
                            case JsError(err) =>
                              setMessage(
                                s"expecting map on import selected from ${importId}, got error ${err}"
                              )
                          }

                        case JsDefined(x) =>
                          setMessage(
                            s"expecting string on import selected from ${importId}, got ${x}"
                          )
                        case _: JsUndefined =>
                          setMessage(
                            s"error import selected from ${importId}, did not find import/importduplicate/id field"
                          )
                      }
                    case None =>
                      setMessage(
                        s"error import selected from ${importId}, ${gr.getError()}"
                      )
                  }
                }
                .recover {
                  case x: Exception =>
                    logger
                      .warning(s"exception import selected from ${importId}", x)
                    setMessage(s"exception import selected from ${importId}")
                }
                .foreach { x => }
            }
          }
        )

      def getDuplicateSummaries(
          props: Props
      ): (Option[String], Option[List[DuplicateSummary]]) = {
        logger.fine("PageSummary.getDuplicateSummaries")
        val importId = DuplicateSummaryStore.getImportId
        val wanted = props.page.getImportId
        if (wanted == importId)
          (
            DuplicateSummaryStore.getImportId,
            DuplicateSummaryStore.getDuplicateSummary
          )
        else (wanted, None)
      }

      val nextIMPs: Callback = scope.modState { s => s.nextIMPs }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val (importId, summaries) = getDuplicateSummaries(props)

        logger.finer(
          s"PageSummary.render called, importId=${importId}, summaries=${summaries}"
        )

        val tp = SummaryPeople(summaries)
        val takerows = if (state.alwaysShowAll) {
          tp.summaries.size
        } else {
          state.showRows match {
            case Some(r) => r
            case None    => tp.summaries.size
          }
        }

        def footer() = {
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              PageScoreboard
                .scoringMethodButton(state.useIMP, None, false, nextIMPs),
              whenUndefined(importId)(
                TagMod(
                  " ",
                  if (!state.forPrint)
                    TagMod() // AppButton("ForPrint", "Select For Print", ^.onClick --> forPrint(true))
                  else
                    Seq[TagMod](
                      AppButton(
                        "Cancel Print",
                        "Cancel Print",
                        ^.onClick --> forPrintCancel
                      ),
                      " ",
                      AppButton("Print", "Print", ^.onClick --> forPrintOk),
                      " ",
                      AppButton(
                        "PrintSelectAll",
                        "Select All",
                        ^.onClick --> selectedAll
                      ),
                      " ",
                      AppButton(
                        "PrintClearAll",
                        "Clear Selected",
                        ^.onClick --> clearAllSelected
                      )
                    ).toTagMod
                )
              ),
              importId.whenDefined { importid =>
                TagMod(
                  " ",
                  if (!state.forPrint)
                    AppButton(
                      "ForImport",
                      "Select For Import",
                      ^.onClick --> forPrint(true)
                    )
                  else
                    Seq[TagMod](
                      AppButton(
                        "Cancel Import",
                        "Cancel Import",
                        ^.onClick --> forPrintCancel
                      ),
                      " ",
                      AppButton(
                        "Import",
                        "Import Selected",
                        ^.onClick --> importSelected(importid),
                        ^.disabled := state.selected.isEmpty
                      ),
                      " ",
                      AppButton(
                        "ImportSelectAll",
                        "Select All",
                        ^.onClick --> selectedAll
                      ),
                      " ",
                      AppButton(
                        "ImportClearAll",
                        "Clear Selected",
                        ^.onClick --> clearAllSelected
                      )
                    ).toTagMod
                )
              }
            )
          )
        }

        def footerColspan = {
          5 + // the number columns on normal summary (no names)
            tp.allPlayers.size +
            importId.map(i => 2).getOrElse(0) +
            (if (state.forPrint) 1 else 0)
        }

        /**
          * Show the matches
          */
        def showMatches() = {
          val isImportStore = props.page.isInstanceOf[ImportSummaryView]
          <.table(
            <.caption(
              if (isImportStore) {
                TagMod()
              } else {
                AppButton(
                  "DuplicateCreate",
                  "New",
                  props.routerCtl.setOnClick(NewDuplicateView)
                )
              },
              <.span(
                RadioButton(
                  "ShowBoth",
                  "Show All",
                  state.showEntries == ShowBoth,
                  show(ShowBoth)
                ),
                RadioButton(
                  "ShowMD",
                  "Show Matches",
                  state.showEntries == ShowMD,
                  show(ShowMD)
                ),
                RadioButton(
                  "ShowMDR",
                  "Show Results Only",
                  state.showEntries == ShowMDR,
                  show(ShowMDR)
                )
              )
            ),
            SummaryHeader((tp, props, state, this, importId)),
            <.tfoot(
              <.tr(
                <.td(^.colSpan := footerColspan, footer())
              )
            ),
            <.tbody(
              if (tp.isData) {
                summaries.get
                  .sortWith { (one, two) =>
                    if (one.created == two.created) one.id > two.id
                    else one.created > two.created
                  }
                  .filter { ds =>
                    state.showEntries match {
                      case ShowMD =>
                        !ds.onlyresult
                      case ShowMDR =>
                        ds.onlyresult
                      case ShowBoth =>
                        true
                    }
                  }
                  .take(takerows)
                  .map { ds =>
                    SummaryRow
                      .withKey(ds.id.id)((tp, ds, props, state, this, importId))
                  }
                  .toTagMod
              } else {
                <.tr(
                  <.td("Working"),
                  importId.map { id =>
                    TagMod(
                      <.th(""),
                      <.th("")
                    )
                  }.whenDefined,
                  state.forPrint ?= <.td("Working"),
                  <.td(""),
                  <.td(""),
                  tp.allPlayers
                    .filter(p => p != "")
                    .map { p =>
                      <.td("")
                    }
                    .toTagMod,
                  <.td(""),
                  <.td("")
                )
              }
            )
          )
        }

        def whenUndefined(op: Option[_])(f: => TagMod) = {
          op.fold(f)(a => TagMod())
        }

        def callbackPage(page: DuplicatePage)(e: ReactEvent) =
          props.routerCtl.set(page).runNow()

        val (bok, bcancel) = if (state.info) {
          (Some(cancel), None)
        } else {
          (None, Some(cancel))
        }

        <.div(
          PopupOkCancel(state.workingOnNew.map(s => s), bok, bcancel),
          DuplicatePageBridgeAppBar(
            id = None,
            tableIds = List(),
            title = Seq(
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  " Summary"
                )
              )
            ),
            helpurl = "../help/duplicate/summary.html",
            routeCtl = props.routerCtl
          )(
            // main menu additions for page
            (if (importId.isDefined) {
              List[VdomNode](
                MuiMenuItem(
                  id = "ShowRows",
                  onClick = toggleRows _,
                  classes = js.Dictionary("root" -> "mainMenuItem")
                )(
                  "Show All", {
                    if (state.alwaysShowAll || state.showRows.isEmpty) {
                      icons.CheckBox()
                    } else {
                      icons.CheckBoxOutlineBlank()
                    }
                  }
                )
              )
            } else {
              val x: List[VdomNode] =
                List(
                  MuiMenuItem(
                    id = "Suggest",
                    onClick = callbackPage(SuggestionView) _
                  )(
                    "Suggest Pairs"
                  ),
                  MuiMenuItem(
                    id = "ShowRows",
                    onClick = toggleRows _,
                    classes = js.Dictionary("root" -> "mainMenuItem")
                  )(
                    "Show All", {
                      if (state.alwaysShowAll || state.showRows.isEmpty) {
                        icons.CheckBox()
                      } else {
                        icons.CheckBoxOutlineBlank()
                      }
                    }
                  ),
                  MuiMenuItem(
                    id = "Statistics",
                    onClick = callbackPage(StatsView) _
                  )(
                    "Statistics"
                  ),
                  MuiMenuItem(
                    id = "DuplicateCreateTest",
                    onClick = newDuplicateTest _
                  )(
                    "Test"
                  ),
                  MuiMenuItem(
                    id = "ForPrint",
                    onClick = clickForPrint(true) _
                  )(
                    "Select For Print"
                  )
                )
              x
            }): _*
          ),
          <.div(
            dupStyles.divSummary,
            showMatches()
          )
        )
      }

      private var mounted = false

      val storeCallback: Callback = Callback {
        logger.fine("PageSummary.Backend.storeCallback called")
      } >>
        scope.forceUpdate

      def summaryError(): Unit =
        scope.withEffectsImpure.modState(s =>
          s.copy(workingOnNew = Some("Error getting duplicate summary"))
        )

      def initializeNewSummary(p: Props): Unit = {
        logger.fine("PageSummary.initializeNewSummary")
        p.page match {
          case isv: ImportSummaryView =>
            val importId = isv.getDecodedId
            Controller.getImportSummary(importId, summaryError _)
          case SummaryView =>
            Controller.getSummary(summaryError _)
        }
      }

      val didMount: Callback = scope.props >>= { (p) =>
        Callback {
          logger.fine("PageSummary.didMount")
          mounted = true
          DuplicateSummaryStore.addChangeListener(storeCallback)
          initializeNewSummary(p)
        }
      }

      val willUnmount: Callback = Callback {
        logger.finer("PageSummary.willUnmount")
        mounted = false
        DuplicateSummaryStore.removeChangeListener(storeCallback)
      }

    }

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

    val component = ScalaComponent
      .builder[Props]("PageSummary")
      .initialStateFromProps { props =>
        State(
          None,
          false,
          Nil,
          if (props.defaultRows == 0) None else Some(props.defaultRows),
          false
        )
      }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
