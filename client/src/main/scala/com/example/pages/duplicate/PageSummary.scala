package com.example.pages.duplicate


import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.rest2.RestClientDuplicateSummary
import com.example.controller.Controller
import com.example.data.SystemTime
import com.example.routes.BridgeRouter
import com.example.react.AppButton
import com.example.react.DateUtils
import com.example.pages.duplicate.DuplicateRouter.NewDuplicateView
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.pages.duplicate.DuplicateRouter.FinishedScoreboardsView
import com.example.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.example.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.example.react.Utils._
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.rest2.RequestCancelled
import com.example.react.Popup
import com.example.rest2.ResultHolder
import com.example.data.MatchDuplicate
import com.example.react.PopupOkCancel
import com.example.logger.Alerter
import com.example.bridge.store.DuplicateSummaryStore
import com.example.pages.duplicate.DuplicateRouter.DuplicateResultView
import com.example.pages.duplicate.DuplicateRouter.SuggestionView
import com.example.react.RadioButton
import com.example.pages.duplicate.DuplicateRouter.PairsView

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
 * PageSummary( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object PageSummary {
  import PageSummaryInternal._

  case class Props( routerCtl: BridgeRouter[DuplicatePage], defaultRows: Int = 10 )

  def apply( routerCtl: BridgeRouter[DuplicatePage], defaultRows: Int = 10 ) = component(Props(routerCtl, defaultRows))

}

object PageSummaryInternal {
  import PageSummary._
  import DuplicateStyles._

  val logger = Logger("bridge.PageSummary")

  val SummaryHeader = ScalaComponent.builder[(SummaryPeople,Props,State,Backend)]("SummaryRow")
                        .render_P( props => {
                          val (tp,pr,state,backend) = props
                          <.thead(
                            <.tr(
                              <.th(
                                ^.colSpan:=tp.allPlayers.length+4,
                                RadioButton("ShowBoth", "Show All", state.showEntries==ShowBoth, backend.show(ShowBoth) ),
                                RadioButton("ShowMD", "Show Matches", state.showEntries==ShowMD, backend.show(ShowMD) ),
                                RadioButton("ShowMDR", "Show Results Only", state.showEntries==ShowMDR, backend.show(ShowMDR) )
                              )
                            ),
                            <.tr(
                              <.th( "Id"),
                              state.forPrint ?= <.th( "Print" ),
                              <.th( "Finished"),
                              <.th( "Created", <.br(), "Last Updated"),
                              tp.allPlayers.length>0?=(<.th( ^.colSpan:=tp.allPlayers.length, "Results")),
                              <.th( "Totals", ^.rowSpan:=2 )
                            ),
                            <.tr(
                              <.th( ""),
                              state.forPrint ?= <.th( "" ),
                              <.th( ""),
                              <.th( AppButton( "DuplicateCreate", "New",
                                               pr.routerCtl.setOnClick(NewDuplicateView)
//                                              ^.onClick --> backend.newDuplicate()
                                    )
                              ),
                              tp.allPlayers.filter(p => p!="").map { p =>
                                <.th(
                                  <.span(p)
//                                  tp.firstPlaces.get(p) match {
//                                    case Some(fp) => <.span(<.br,fp.toString())
//                                    case None => <.span()
//                                  }, (tp.playerScores.get(p) match {
//                                    case Some(fp) => <.span(<.br,fp.toString())
//                                    case None => <.span()
//                                  })
                                )
                              }.toTagMod
                            )
                          )
                        }).build

  val SummaryRow = ScalaComponent.builder[(SummaryPeople,DuplicateSummary,Props,State,Backend)]("SummaryRow")
                      .render_P( props => {
                        val (tp,ds,pr,st,back) = props
                          <.tr(
                            <.td( AppButton( (if (ds.onlyresult) "Result_" else "Duplicate_")+ds.id, ds.id,
                                             baseStyles.appButton100,
                                             if (ds.onlyresult) {
                                               pr.routerCtl.setOnClick(DuplicateResultView(ds.idAsDuplicateResultId) )
                                             } else {
                                               pr.routerCtl.setOnClick(CompleteScoreboardView(ds.id) )
                                             }
                                            )),
                            st.forPrint ?= <.td(
                                                 <.input.checkbox(
                                                   ^.checked := st.selected.contains(ds.id),
                                                   ^.onClick --> back.toggleSelect(ds.id)
                                                 )
                                               ),
                            <.td( (if (ds.finished) "done"; else "")),
                            <.td( DateUtils.formatDate(ds.created), <.br(), DateUtils.formatDate(ds.updated)),
                            tp.allPlayers.filter(p => p!="").map { p =>
                              <.td(
                                ds.playerPlaces().get(p) match {
                                  case Some(place) => <.span(place.toString)
                                  case None => <.span()
                                },
                                ds.playerScores().get(p) match {
                                  case Some(place) => <.span(<.br,Utils.toPointsString(place))
                                  case None => <.span()
                                }
                              )
                            }.toTagMod,
                            <.td(
                              Utils.toPointsString(
                                tp.allPlayers.filter(p => p!="").flatMap { p =>
                                  ds.playerScores().get(p) match {
                                    case Some(place) => place::Nil
                                    case None => Nil
                                  }
                                }.foldLeft(0.0)((ac,v)=>ac+v)
                              )
                            )
                          )
                      }).build

  sealed trait ShowEntries
  object ShowMD extends ShowEntries
  object ShowMDR extends ShowEntries
  object ShowBoth extends ShowEntries

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( workingOnNew: Option[String], forPrint: Boolean, selected: List[Id.MatchDuplicate],
                    showRows: Option[Int], alwaysShowAll: Boolean,
                    showEntries: ShowEntries = ShowBoth )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val resultDuplicate = ResultHolder[MatchDuplicate]()

    def show( what: ShowEntries ) = scope.modState( s => s.copy( showEntries = what ) )

    def cancel() = Callback {
      resultDuplicate.cancel()
    } >> scope.modState( s => s.copy(workingOnNew=None))

    def newDuplicate( fortest: Boolean = false ) =
      scope.modState( s => s.copy(workingOnNew=Some("Working on creating a new duplicate match")), Callback {
        val result = Controller.createMatchDuplicate(test=fortest).recordFailure()
        result.foreach { created=>
          logger.info("Got new duplicate match ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) scope.withEffectsImpure.props.routerCtl.set(CompleteScoreboardView(created.id)).runNow()
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              scope.withEffectsImpure.modState( s => s.copy(workingOnNew=Some("Failed to create a new duplicate match")))
          }
        })
      })

    def toggleSelect( dupid: Id.MatchDuplicate ) = scope.modState(s => {
      val sel = if (s.selected.contains(dupid)) s.selected.filter( s => s!=dupid )
                else dupid::s.selected
      s.copy(selected = sel)
    })

    def forPrint(flagForPrint: Boolean) = scope.modState(s => s.copy(forPrint = flagForPrint))

    def forPrintOk() = CallbackTo {
      val s = scope.withEffectsImpure.state
      val mds = s.selected.reverse.map{ id => id.toString() }.mkString(",")
      forPrint(false).runNow()
      (scope.withEffectsImpure.props,mds)
    } >>= { case (p,mds) =>
      p.routerCtl.set(FinishedScoreboardsView(mds))
    }

    def forPrintCancel() = forPrint(false)

    def toggleRows() = scope.modState{ s =>
      val n = s.showRows match {
        case Some(r) => None
        case None => Some( scope.withEffectsImpure.props.defaultRows )
      }
      s.copy( showRows=n)
    }

    def render( props: Props, state: State ) = {
      val summaries = DuplicateSummaryStore.getDuplicateSummary()
      val tp = SummaryPeople(summaries)
      val takerows = if (state.alwaysShowAll)
      {
        tp.summaries.size
      } else {
        state.showRows match {
          case Some(r) => r
          case None => tp.summaries.size
        }
      }

      /**
       * Show the matches
       */
      def showMatches() = {
        <.table(
            dupStyles.tableSummary,
            SummaryHeader((tp,props,state,this)),
            <.tbody(
                summaries.get.sortWith((one,two)=>one.created>two.created).
                              filter { ds =>
                                state.showEntries match {
                                  case ShowMD =>
                                    !ds.onlyresult
                                  case ShowMDR =>
                                    ds.onlyresult
                                  case ShowBoth =>
                                    true
                                }
                              }.
                              take(takerows).
                              map { ds =>
                                    SummaryRow.withKey( ds.id )((tp,ds,props,state,this))
                                  }.toTagMod,
                (!state.alwaysShowAll && state.showRows.isDefined) ?=
                  <.tr(
                    <.td(
                          ^.colSpan:=3,
                          AppButton( "ShowRows2",
                                     state.showRows.map( n => "Show All" ).getOrElse(s"Show ${props.defaultRows}"),
                                     ^.onClick --> toggleRows()
                                   )
                    ),
                    <.td( ^.colSpan:=tp.allPlayers.length+1 )
                  )
            )
        )
      }

      /**
       * Display the still working to fill table
       */
      def showWorkingMatches() = {
        <.table(
            dupStyles.tableSummary,
            SummaryHeader((tp,props,state,this)),
            <.tbody(
              <.tr(
                <.td( "Working" ),
                state.forPrint ?= <.td( "Working" ),
                <.td( ""),
                <.td( ""),
                tp.allPlayers.filter(p => p!="").map { p =>
                  <.td( "")
                }.toTagMod
              )
            )
        )
      }

      <.div(
        dupStyles.divSummary,
        PopupOkCancel( state.workingOnNew.map( s=>s), None, Some(cancel()) ),
        <.span(
          dupStyles.spanTopButtons,
          !state.alwaysShowAll ?= AppButton( "ShowRows", state.showRows.map( n => "Show All" ).getOrElse(s"Show ${props.defaultRows}"), ^.onClick --> toggleRows() ),
          " ",
          AppButton( "Suggest", "Suggest Pairs", props.routerCtl.setOnClick(SuggestionView) ),
          " ",
          AppButton( "Home2", "Home", props.routerCtl.home ),
          " ",
          AppButton( "BoardSets2", "BoardSets", props.routerCtl.setOnClick(BoardSetSummaryView) ),
          " ",
          AppButton( "Movements2", "Movements", props.routerCtl.setOnClick(MovementSummaryView) )
        ),
        <.h1("Summary"),
//        <.div(
            if (tp.isData) showMatches()
            else showWorkingMatches(),
            <.p,
            <.div(
              baseStyles.divFooter,
              <.div(
                baseStyles.divFooterLeft,
                AppButton( "Home", "Home", props.routerCtl.home ),
                " ",
                if (!state.forPrint) AppButton("ForPrint", "Select For Print", ^.onClick --> forPrint(true))
                else Seq[TagMod](
                       AppButton("Cancel Print", "Cancel Print", ^.onClick --> forPrintCancel),
                       " ",
                       AppButton("Print", "Print", ^.onClick --> forPrintOk)
                       ).toTagMod
              ),
              <.div(
                baseStyles.divFooterCenter,
                AppButton( "BoardSets", "BoardSets", props.routerCtl.setOnClick(BoardSetSummaryView) ),
                " ",
                AppButton( "Movements", "Movements", props.routerCtl.setOnClick(MovementSummaryView) )
              ),
              <.div(
                baseStyles.divFooterRight,
                AppButton( "DuplicateCreateTest", "Test", ^.onClick --> newDuplicate(true) ),
                " ",
                AppButton("Pairs", "Pairs", props.routerCtl.setOnClick(PairsView))
              )
            )
//        )
      )
    }

    private var mounted = false

    val storeCallback = Callback { scope.withEffectsImpure.forceUpdate }

    def didMount() = Callback {
      logger.info("PageSummary.didMount")
      DuplicateSummaryStore.addChangeListener(storeCallback)
    } >> scope.props >>= { (p) => Callback(
      Controller.getSummary()
    )}

    def willUnmount() = Callback {
      logger.finer("PageSummary.willUnmount")
      DuplicateSummaryStore.removeChangeListener(storeCallback)
    }

  }

  val component = ScalaComponent.builder[Props]("PageSummary")
                            .initialStateFromProps { props => State( None, false, Nil,
                                                                    if (props.defaultRows==0) None else Some(props.defaultRows),
                                                                    false ) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount(scope => scope.backend.willUnmount())
                            .build
}

