package com.github.thebridsk.bridge.client.pages.individual

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.individual.support.SummaryPeople
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.data.bridge.individual.DuplicateSummary
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles._
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.SummaryViewBase
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.client.pages.individual.support.Utils
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.IndividualDuplicateSummaryStore
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.ImportSummaryView
import com.github.thebridsk.bridge.client.controller.IndividualSummaryController
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.SummaryView
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.pages.individual.components.DuplicateBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.NewDuplicateView

/**
  * A page that shows all the duplicate matches in a table.
  *
  * To use, just code the following:
  *
  * {{{
  * val router: BridgeRouter[IndividualDuplicatePage] = ...
  * val page: SummaryViewBase = ...
  * PageSummary(
  *   routerCtl = router,
  *   page = page
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object PageSummary {
  import Internal._

  val logger: Logger = Logger("bridge.PageSummary")

  case class Props(
    routerCtl: BridgeRouter[IndividualDuplicatePage],
    page: SummaryViewBase,
    defaultRows: Option[Int] = None
  )

  /**
    * Instantiate the react component.
    *
    * @param routerCtl
    * @param page
    * @param defaultRows the number of rows to display at start.
    *                    <=0 means all rows
    *
    * @return the unmounted react component
    *
    * @see [[PageSummary$]] for usage.
    */
  def apply(
    routerCtl: BridgeRouter[IndividualDuplicatePage],
    page: SummaryViewBase,
    defaultRows: Int = 10
  ) =
    component(Props(
      routerCtl,
      page,
      Some(defaultRows).filter(_ > 0)
    )) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State(
      useIMP: Option[Boolean] = None,
      forPrint: Boolean = false,
      forPrintSelected: List[DuplicateSummary.Id] = List.empty,
      popupMsg: Option[String] = None,
      popupInfo: Boolean = false,
      takeRows: Option[Int] = None
    ) {

      def withInfo(info: String): State =
        copy(popupMsg = Some(info), popupInfo = true)
      def withError(err: String): State =
        copy(popupMsg = Some(err), popupInfo = false)
      def clearError(): State = copy(popupMsg = None)

    }

    private val SummaryHeader = ScalaComponent
      .builder[(SummaryPeople, Props, State, Backend, Option[String])](
        "SummaryHeader"
      )
      .render_P(props => {
        val (tp, pr, state, backend, importId) = props
        val isImportStore = importId.isDefined

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

        def idButton() = {
          DuplicateSummary.useId(
            ds.id,
            { id =>
              val t: TagMod =
                AppButton(
                  s"Duplicate_${id.id}",
                  ds.id.id,
                  baseStyles.appButton100,
                  ^.onClick --> Callback { pr.routerCtl.toRootPage(pr.page.getScoreboardPage(id)) },
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
            { id =>
              val t: TagMod =
                AppButton(
                  s"Individual_${id.id}",
                  ds.id.id,
                  baseStyles.appButton100,
                  pr.routerCtl.setOnClick(pr.page.getIndividualScoreboardPage(id)),
                  importId.map { id => ^.disabled := true }.whenDefined
                )
              t
            },
            TagMod()
          )
        }

        def importColumns() = {
          importId.map { iid =>
            TagMod(
              <.td(
                DuplicateSummary.useId(
                  ds.id,
                  { id =>
                    val t: TagMod =
                      AppButton(
                        s"ImportDuplicate_${id.id}",
                        "Import",
                        baseStyles.appButton100,
                        ^.onClick --> back.importDuplicateMatch(iid, id)
                      )
                    t
                  },
                  { id =>
                    val t: TagMod =
                      AppButton(
                        s"ImportResult_${id.id}",
                        "Import",
                        baseStyles.appButton100,
                        ^.onClick --> back.importDuplicateResult(iid, id)
                      )
                    t
                  },
                  { id =>
                    val t: TagMod =
                      AppButton(
                        s"ImportIndividual_${id.id}",
                        "Import",
                        baseStyles.appButton100,
                        ^.onClick --> back.importIndividualDuplicate(iid, id)
                      )
                    t
                  },
                  TagMod()
                )
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
          }.whenDefined

        }

        <.tr(
          <.td(
            idButton()
          ),
          importColumns(),
          st.forPrint ?= <.td(
            <.input.checkbox(
              ^.checked := st.forPrintSelected.contains(ds.id),
              ^.onClick --> back.toggleForPrintSelect(ds.id)
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

    class Backend(scope: BackendScope[Props, State]) {

      def importIndividualDuplicate(importId: String, id: IndividualDuplicate.Id) = Callback {}
      def importDuplicateMatch(importId: String, id: MatchDuplicate.Id) = Callback {}
      def importDuplicateResult(importId: String, id: MatchDuplicateResult.Id) = Callback {}

      def toggleForPrintSelect(dsid: DuplicateSummary.Id): Callback =
        scope.modState(s => {
          val sel =
            if (s.forPrintSelected.contains(dsid)) s.forPrintSelected.filter(s => s != dsid)
            else dsid :: s.forPrintSelected
          s.copy(forPrintSelected = sel)
        })

      def getDuplicateSummaries(
          props: Props
      ): (Option[String], Option[List[DuplicateSummary]]) = {
        logger.fine("PageSummary.getDuplicateSummaries")
        val importId = IndividualDuplicateSummaryStore.getImportId
        val wanted = props.page.getImportId
        if (wanted == importId)
          (
            importId,
            IndividualDuplicateSummaryStore.getDuplicateSummary
          )
        else (wanted, None)
      }

      val popupCancel = scope.modState(_.clearError())

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        val isImportStore = props.page.isInstanceOf[ImportSummaryView]
        val (importId, summaries) = getDuplicateSummaries(props)
        val summaryPeople = SummaryPeople(summaries)
        val takerows = state.takeRows.getOrElse(summaryPeople.summaries.size)

        val (bok, bcancel) = if (state.popupInfo) {
          (Some(popupCancel), None)
        } else {
          (None, Some(popupCancel))
        }

        def footerColspan = {
          5 + // the number columns on normal summary (no names)
            summaryPeople.allPlayers.size +
            importId.map(i => 2).getOrElse(0) +
            (if (state.forPrint) 1 else 0)
        }
        def footer() = {
          TagMod.empty
        }

        def matchTable() = {
          <.table(
            <.caption(
              !isImportStore ?= AppButton(
                  "DuplicateCreate",
                  "New",
                  props.routerCtl.setOnClick(NewDuplicateView)
              )
            ),
            SummaryHeader((summaryPeople,props,state,this,importId)),
            <.tbody(
              if (summaryPeople.isData) {
                summaries.get
                  .sortWith { (one, two) =>
                    if (one.created == two.created) one.id > two.id
                    else one.created > two.created
                  }
                  .take(takerows)
                  .map { ds =>
                    SummaryRow.withKey(ds.id.id)(
                      (summaryPeople,ds,props,state,this,importId)
                    )
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
                  summaryPeople.allPlayers
                    .filter(p => p != "")
                    .map { p =>
                      <.td("")
                    }
                    .toTagMod,
                  <.td(""),
                  <.td("")
                )
              }
            ),
            <.tfoot(
              <.tr(
                <.td(^.colSpan := footerColspan, footer())
              )
            )
          )
        }

        <.div(
          PopupOkCancel(state.popupMsg.map(s => s), bok, bcancel),
          DuplicateBridgeAppBar(
            id = None,
            tableIds = List.empty,
            title = Seq[CtorType.ChildArg](
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
            // pagemenu: _*
          ),
          <.div(
            dupStyles.pageSummary,
            matchTable()
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
          s.withError("Error getting duplicate summary")
        )

      def initializeNewSummary(p: Props): Unit = {
        logger.fine("PageSummary.initializeNewSummary")
        p.page match {
          case isv: ImportSummaryView =>
            val importId = isv.getDecodedId
            IndividualSummaryController.getImportSummary(importId, summaryError _)
          case SummaryView =>
            IndividualSummaryController.getSummary(summaryError _)
        }
      }

      val didMount: Callback = scope.props >>= { (p) =>
        Callback {
          logger.fine("PageSummary.didMount")
          mounted = true
          IndividualDuplicateSummaryStore.addChangeListener(storeCallback)
          initializeNewSummary(p)
        }
      }

      val willUnmount: Callback = Callback {
        logger.finer("PageSummary.willUnmount")
        mounted = false
        IndividualDuplicateSummaryStore.removeChangeListener(storeCallback)
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
          takeRows = props.defaultRows
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
