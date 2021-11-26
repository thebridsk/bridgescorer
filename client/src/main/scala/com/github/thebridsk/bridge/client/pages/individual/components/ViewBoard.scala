package com.github.thebridsk.bridge.client.pages.individual.components

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicatePage
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseBoardView
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.data.IndividualDuplicatePicture
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.bridge.individual.IndividualBoardScore
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveDirector
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.materialui.icons.Photo
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.pages.hand.Picture
import com.github.thebridsk.bridge.client.pages.hand.HandStyles.handStyles
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles.dupStyles
import com.github.thebridsk.bridge.data.util.Strings

/**
  * A component to show the results of playing a board.
  *
  * To use, just code the following:
  *
  * {{{
  * ViewBoard(
  *   ViewBoard.Props( ... )
  * )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object ViewBoard {
  import Internal._

  case class Props(
    router: BridgeRouter[IndividualDuplicatePage],
    page: BaseBoardView,
    score: IndividualDuplicateScore,
    board: IndividualBoard.Id,
    useIMPs: Boolean = false,
    pictures: List[IndividualDuplicatePicture] = List.empty
  )

  /**
    *
    * @param router
    * @param page
    * @param score
    * @param board
    * @param useIMPs
    * @param pictures
    *
    * @return the unmounted react component
    *
    * @see [[ViewBoard$]] for usage.
    */
  def apply(
    router: BridgeRouter[IndividualDuplicatePage],
    page: BaseBoardView,
    score: IndividualDuplicateScore,
    board: IndividualBoard.Id,
    useIMPs: Boolean = false,
    pictures: List[IndividualDuplicatePicture] = List.empty
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(router,page,score,board,useIMPs,pictures))

  protected object Internal {

    val logger: Logger = Logger("bridge.ViewBoard")

    case class State(
      val showPicture: Option[String] = None
    )

    private val Header = ScalaComponent
      .builder[Props]("ViewBoard.Header")
      .render_P { props =>
        <.thead(
          <.tr(
            <.th(^.rowSpan := 2, "N"),
            <.th(^.rowSpan := 2, "S"),
            <.th(^.rowSpan := 2, "E"),
            <.th(^.rowSpan := 2, "W"),
            <.th(^.rowSpan := 2, "Contract"),
            <.th(^.rowSpan := 2, "By"),
            <.th(^.rowSpan := 2, "Made"),
            <.th(^.rowSpan := 2, "Down"),
            <.th(^.rowSpan := 1, ^.colSpan := 2, "Score"),
            <.th(^.rowSpan := 1, ^.colSpan := 2, (if (props.useIMPs) "IMPs" else "Match Points")),
            <.th(^.rowSpan := 2, "Picture")
          ),
          <.tr(
            <.th(^.rowSpan := 1, "NS"),
            <.th(^.rowSpan := 1, "EW"),
            <.th(^.rowSpan := 1, "NS"),
            <.th(^.rowSpan := 1, "EW")
          )
        )
      }
      .build

    private val Row = ScalaComponent
      .builder[(IndividualBoardScore.Result, IndividualBoardScore, Props, Backend)]("ViewBoard.TeamRow")
      .render_P { cprops =>
        val (hand, boardscore, props, backend) = cprops
        logger.fine(
          s"ViewBoard hand ${hand} boardscore ${boardscore} page ${props.page}"
        )

        def northButton: TagMod = {
          val northplayer = hand.hand.north
          logger.fine(s"ViewBoard.teamButton northplayer ${northplayer}, hand ${hand}")
          if (hand.hide) {
            northplayer
          } else {
            val enabled =
              if (boardscore.allplayed) {
                true
              } else {
                props.score.perspective match {
                  case PerspectiveTable(tableid, round) =>
                    hand.hand.table == tableid && hand.hand.round == round
                  case PerspectiveComplete => false
                  case PerspectiveDirector => true
                }
              }
            if (enabled) {
              val clickPage = props.page.toHandView(IndividualDuplicateHand.id(northplayer))
              AppButton(
                s"Hand_${northplayer}",
                northplayer,
                props.router.setOnClick(clickPage)
              )
            } else {
              northplayer
            }
          }
        }

        val md = props.score

        <.tr(
          <.td(northButton),
          <.td(hand.hand.south),
          <.td(hand.hand.east),
          <.td(hand.hand.west),
          <.td(hand.showContract),
          <.td(hand.showDeclarer),
          <.td(hand.showMade),
          <.td(hand.showDown),
          <.td(hand.showNSScore),
          <.td(hand.showEWScore),
          <.td(if (props.useIMPs) hand.showNSIMP else hand.showNSMP),
          <.td(if (props.useIMPs) hand.showEWIMP else hand.showEWMP),
          <.td(
            if (hand.scorehand.isEmpty) ""
            else if (hand.hide) Strings.checkmark
            else {
              props.pictures
                .find(dp => dp.boardId == hand.hand.board && dp.handId == hand.hand.id)
                .whenDefined { dp =>
                  <.button(
                    ^.`type` := "button",
                    handStyles.footerButton,
                    ^.onClick --> backend.doShowPicture(dp.url),
                    ^.id := "ShowPicture_" + hand.hand.id.id,
                    Photo()
                  )
                }
            }
          )
        )

      }
      .build

    class Backend(scope: BackendScope[Props, State]) {

      val popupOk: Callback = scope.modState(s => s.copy(showPicture = None))

      def doShowPicture(url: String): Callback =
        scope.modState(s => s.copy(showPicture = Some(url)))

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val board = props.score.getBoardScore(props.board.toInt)
        <.div(
          dupStyles.viewBoard,
          PopupOkCancel(
            content = state.showPicture.map(p => Picture(None, Some(p))),
            ok = Some(popupOk)
          ),
          <.table(
            <.caption(
              <.span(
                ^.float := "left",
                "Board " + props.board.toNumber
              ),
              <.span(
                ^.float := "right",
                board.whenDefined(_.showVul)
              )
            ),
            Header(props),
            <.tbody(
              board.whenDefined { b =>
                b.all.map { h =>
                  // props (IndividualBoardScore.Result, IndividualBoardScore, Props, Backend)
                  Row.withKey(h.hand.id.id)((h, b, props, this))
                }.toTagMod
              }
            )
          )
        )
      }

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true

      }

      val willUnmount: Callback = Callback {
        mounted = false

      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps != props) {
          // props have change, reinitialize state
          cdu.setState(State())
        }
      }

    val component = ScalaComponent
      .builder[Props]("ViewBoard")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
