package com.github.thebridsk.bridge.client.pages.duplicate.boardsets

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.bridge.store.BoardSetStore
import com.github.thebridsk.bridge.client.controller.BoardSetController
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.data.BoardInSet
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateStyles
import japgolly.scalajs.react.CtorType
import com.github.thebridsk.bridge.clientcommon.react.RadioButton
import com.github.thebridsk.bridge.clientcommon.react.CheckBox
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicatePage
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicatePageBridgeAppBar
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetSummaryView
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.BoardSetEditView
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Shows all the boards of a boardset.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageEditBoardSet( ... )
 * </code></pre>
 *
 * To obtain a reference to the PageEditBoardSet:
 *
 * <pre><code>
 * val ref = PageEditBoardSet.getRef()
 *
 * def render() = {
 *   PageEditBoardSet.withRef(ref)( ... )
 * }
 *
 * </code></pre>
 *
 * @author werewolf
 */
object PageEditBoardSet {
  import PageEditBoardSetInternal._

  type RefType = Ref.WithScalaComponent[Props,State,Backend,CtorType.Props]

  def getRef(): RefType = Ref.toScalaComponent(component)

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage ) = component(Props(routerCtl,page))  // scalafix:ok ExplicitResultTypes; ReactComponent

}

object PageEditBoardSetInternal {
  import PageEditBoardSet._

  val logger: Logger = Logger("bridge.PageEditBoardSet")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   * @param boardSetId the boardset being edited, None if new boardset
   * @param boardset the edited boardset
   * @param nboards the number of boards in the set
   * @param msg a popup message
   */
  case class State( boardSetId: Option[BoardSet.Id] = None, boardset: Option[BoardSet] = None, nboards: Int = 0, msg: Option[TagMod] = None ) {

    def isNew = boardSetId.isEmpty
    def hasBoardSet = boardset.isDefined

    def getBoardSet: BoardSet = {
      boardset.getOrElse(
        BoardSet(
          boardSetId.getOrElse(BoardSet.idNul),
          "",
          "",
          if (nboards > 0) (1 to nboards).map( i => BoardInSet(i, false, false, "")).toList
          else List(),
          deletable = Some(true)
        )
      )
    }
    def setName( name: BoardSet.Id ): State = copy( boardset = Some(getBoardSet.copy(name = name)))
    def setShort( description: String ): State = copy( boardset = Some(getBoardSet.copy(short=description)))
    def setDescription( description: String ): State = copy( boardset = Some(getBoardSet.copy(description=description)))

    def setNBoards( n: Int ): State = {
      if ( n < nboards ) {
        val curbs = getBoardSet
        val nb = curbs.copy( boards = curbs.boards.take(n))
        copy(boardset = Some(nb), nboards = n)
      } else if (n > nboards) {
        val curbs = getBoardSet
        val nb = (nboards+1 to n).map( i => BoardInSet(i, false, false, "")).toList
        copy(boardset = Some(curbs.copy(boards = curbs.boards:::nb)), nboards = n)
      } else {
        this
      }
    }

    private def set( id: Int, f: BoardInSet=>BoardInSet) = {
      val curbs = getBoardSet

      val (before, curAndAfter) = curbs.boards.splitAt(id - 1)

      if (curAndAfter.isEmpty) {
        this
      } else {
        val cur = curAndAfter.head
        val after = curAndAfter.tail
        val nb = before ::: ( f(cur) :: after)
        val nbs = curbs.copy( boards = nb )
        copy( boardset = Some(nbs) )
      }
    }

    def setDealer( id: Int, dealer: String ): State = {
      set(id, bs => bs.copy(dealer = dealer))
    }
    def setNSVul( id: Int, nsVul: Boolean ): State = {
      set(id, bs => bs.copy(nsVul = nsVul))
    }
    def setEWVul( id: Int, ewVul: Boolean ): State = {
      set(id, bs => bs.copy(ewVul = ewVul))
    }
    def toggleNSVul( id: Int ): State = {
      set(id, bs => bs.copy(nsVul = !bs.nsVul))
    }
    def toggleEWVul( id: Int ): State = {
      set(id, bs => bs.copy(ewVul = !bs.ewVul))
    }

    def isValid(): Boolean = {
      boardset.flatMap { bs =>
        if (bs.name != null && bs.name != "") {
          if (bs.short != null && bs.short != "") {
            val noDealer = bs.boards.find { b =>
              try {
                val pos = PlayerPosition(b.dealer)
                false
              } catch {
                case x: IllegalArgumentException =>
                  true
              }
            }
            noDealer.toLeft(1).toOption
          } else {
            None
          }
        } else {
          None
        }
      }.isDefined
    }

    def setMsg( msg: String ): State = copy( msg = Some(msg))
    def setMsg( msg: TagMod ): State = copy( msg = Some(msg))
    def clearMsg(): State = copy( msg = None )
  }


  private[boardsets]
  val BoardHeader = ScalaComponent.builder[Props]("PageEditBoardSet.BoardHeader")
                    .render_P( props => {
                      <.thead(
                        <.tr(
                          <.th( ^.rowSpan:=2, "Board" ),
                          <.th( ^.colSpan:=4, "Dealer" ),
                          <.th( ^.colSpan:=2, "Vulnerability" )
                        ),
                        <.tr(
                          <.th("North"),
                          <.th("East"),
                          <.th("South"),
                          <.th("West"),
                          <.th("North South"),
                          <.th("East West")
                        )
                      )
                    }).build

  //
  // prop._3 is setDealer( boardId: String, dealerPos: String ) => Callback
  // prop._4 is toggleNSVul( boardId: String ) => Callback
  // prop._5 is toggleEWVul( boardId: String ) => Callback
  private[boardsets]
  val BoardRow = ScalaComponent.builder[(Props,BoardInSet,(Int,String)=>Callback,Int=>Callback,Int=>Callback)]("PageEditBoardSet.BoardRow")
                    .render_P( args => {
                      val (props,board,setDealerCB,toggleNSVul,toggleEWVul) = args

                      def radioDealer( pos: String ) = {
                        val isDealer = board.dealer == pos

                        RadioButton(id = s"${board.id}_D_$pos", text = "", value = isDealer, toggle = setDealerCB(board.id,pos) )
                      }

                      <.tr(
                        <.th( board.id),
                        <.td( radioDealer("N")),
                        <.td( radioDealer("E")),
                        <.td( radioDealer("S")),
                        <.td( radioDealer("W")),
                        <.td( CheckBox( id = "Vul_NS", text = "", value = board.nsVul, toggle = toggleNSVul(board.id)) ),
                        <.td( CheckBox( id = "Vul_EW", text = "", value = board.ewVul, toggle = toggleEWVul(board.id)) ),
                      )
                    }).build

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def inputCB( data: ReactEventFromInput): Callback = data.inputText( text => scope.modState( _.setName(BoardSet.id(text))) )
    def shortCB( data: ReactEventFromInput): Callback = data.inputText( text => scope.modState( _.setShort(text)) )
    def descCB( data: ReactEventFromInput): Callback = data.inputText( text => scope.modState( _.setDescription(text)) )
    def setNboardsCB( data: ReactEventFromInput): Callback = data.inputText { text =>
      scope.modState { s =>
        try {
          val n = text.trim.toInt
          if (n <= 0 || n >= 100) {
            s.setMsg( "Must enter a valid number between 1 and 99" )
          } else {
            s.setNBoards(text.toInt)
          }
        } catch {
          case x: NumberFormatException =>
            s.setMsg( "Must enter a valid number" )
        }
      }
    }

    def setDealer( boardId: Int, dealerPos: String ): Callback = scope.modState( s => s.setDealer(boardId, dealerPos))

    def toggleNSVul( boardId: Int ): Callback = scope.modState( s => s.toggleNSVul(boardId))
    def toggleEWVul( boardId: Int ): Callback = scope.modState( s => s.toggleEWVul(boardId))

    val clickOk: Callback = scope.modState(
      { s =>
        s.setMsg( s.boardSetId.map(i=>s"Updating boardset $i").getOrElse("Creating new boardset"))
      },
      Callback {
        val s = scope.withEffectsImpure.state
        s.boardSetId match {
          case Some(id) =>
            BoardSetController.updateBoardSet(s.getBoardSet).onComplete { tr =>
              logger.fine(s"update boardset completed: $tr")
              tr match {
                case _: Success[_] =>
                  scope.withEffectsImpure.props.routerCtl.set( BoardSetSummaryView ).runNow()
                case Failure(ex) =>
                  scope.withEffectsImpure.modState { ss =>
                    ss.setMsg(s"Error updating boardset $id")
                  }
              }
            }
          case None =>
            BoardSetController.createBoardSet(s.getBoardSet).onComplete { tr =>
              logger.fine(s"create boardset completed: $tr")
              tr match {
                case _: Success[_] =>
                  scope.withEffectsImpure.props.routerCtl.set( BoardSetSummaryView ).runNow()
                case Failure(ex) =>
                  scope.withEffectsImpure.modState { ss =>
                    ss.setMsg(s"Error creating new boardset")
                  }
              }
            }
        }
      }
    )

    val popupCancel: Callback = Callback {

    } >> scope.modState( s => s.clearMsg())

    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React
      import DuplicateStyles._
      val columns = if (state.nboards <= 10) 1 else if (state.nboards <= 20) 2 else 3
      val boardset = state.getBoardSet
      val entriespercolumn = (boardset.boards.length+columns-1)/columns
      val listofboardsets = if (entriespercolumn == 0) List()
                            else boardset.boards.sortWith( (t1,t2)=>t1.id<t2.id ).grouped(entriespercolumn).toList
      val isValid = state.isValid()
      <.div(
        PopupOkCancel( state.msg, None, Some(popupCancel) ),
        DuplicatePageBridgeAppBar(
          id = None,
          tableIds = List(),
          title = Seq[CtorType.ChildArg](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "BoardSets",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          dupStyles.divEditBoardSet,
          if (state.isNew || state.hasBoardSet) {
            TagMod(
              <.div(
                <.label(
                  "Name: ",
                  if (state.isNew) {
                    <.input(
                      ^.name := "Name",
                      ^.onChange ==> inputCB _,
                      ^.value := state.getBoardSet.name.id
                    )
                  } else {
                    state.getBoardSet.name.id
                  }
                ),
                <.label(
                  "Short: ",
                  <.input(
                    ^.name := "Short",
                    ^.onChange ==> shortCB _,
                    ^.value := state.getBoardSet.short
                  )
                ),
                <.label(
                  "Description: ",
                  <.input(
                    ^.name := "Description",
                    ^.onChange ==> descCB _,
                    ^.value := state.getBoardSet.description
                  )
                ),
                <.label(
                  "Number of boards: ",
                  <.input(
                    ^.`type`:="number",
                    ^.name := "NBoards",
                    ^.onChange ==> setNboardsCB _,
                    ^.value := state.nboards.toString
                  )
                ),
              ),
              <.div(
                listofboardsets.map { boardsets =>
                  <.div(
                    <.table(
                      BoardHeader(props),
                      <.tbody(
                        boardsets.map { b =>
                          BoardRow.withKey( b.id )((props,b, setDealer, toggleNSVul, toggleEWVul))
                        }.toTagMod
                      )
                    )
                  )
                }.toTagMod
              ),
              <.div(
                baseStyles.divFooter,
                <.div(
                  baseStyles.divFooterLeft,
                  AppButton(
                    "OK","OK",
                    ^.onClick --> clickOk,
                    BaseStyles.highlight(
                      requiredNotNext = isValid
                    ),
                    ^.disabled := !isValid
                  )
                ),
                <.div(
                  baseStyles.divFooterLeft,
                  AppButton(
                    "Cancel","Cancel",
                    props.routerCtl.setOnClick(BoardSetSummaryView)
                  )
                )
              )
            )
          } else {
            HomePage.loading
          }
        )
      )
    }

    def forceUpdate = scope.withEffectsImpure.forceUpdate

    val storeCallback: Callback = scope.modStateOption { (state,props) =>
      props.page match {
        case bsev: BoardSetEditView =>
          val display = bsev.display
          BoardSetStore.getBoardSet(display) match {
            case Some(bs) =>
              val bs1 = if (bs.isDeletable) bs else bs.copy(resetToDefault = Some(true))
              Some(state.copy(boardset = Some(bs1), nboards = bs1.boards.length))
            case None =>
              None
          }
        case _ =>
          None
      }
    }

    val didMount: Callback = scope.modStateOption { (state,props) =>
      logger.info("PageEditBoardSet.didMount")
      BoardSetStore.addChangeListener(storeCallback)
      props.page match {
        case bsev: BoardSetEditView =>
          val display = bsev.display
          BoardSetController.getBoardSet(display)
        case _ =>
      }
      None
    }

    val willUnmount: Callback = Callback {
      logger.info("PageEditBoardSet.willUnmount")
      BoardSetStore.removeChangeListener(storeCallback)
    }

  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ): Callback = cdu.modStateOption { state =>
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (props.page != prevProps.page) {
      props.page match {
        case bsev: BoardSetEditView =>
          val display = bsev.display
          BoardSetController.getBoardSet(display)
          Some( State( boardSetId = Some(display)) )
        case _ =>
          Some( State() )
      }
    } else {
      None
    }
  }

  private[boardsets]
  val component = ScalaComponent.builder[Props]("PageEditBoardSet")
                            .initialStateFromProps { props =>
                              props.page match {
                                case bsev: BoardSetEditView =>
                                  val display = bsev.display
                                  State( Some(display) )
                                case _ =>
                                  State()
                              }
                            }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentDidUpdate( didUpdate )
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

