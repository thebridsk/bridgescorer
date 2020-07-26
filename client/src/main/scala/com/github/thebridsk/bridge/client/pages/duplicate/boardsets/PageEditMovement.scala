package com.github.thebridsk.bridge.client.pages.duplicate.boardsets

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.bridge.store.BoardSetStore
import com.github.thebridsk.bridge.client.controller.BoardSetController
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.BridgeRouter
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
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementSummaryView
import com.github.thebridsk.bridge.client.pages.HomePage
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.MovementEditView
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.HandInTable
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.clientcommon.react.DropdownList

/**
 * Shows all the boards of a boardset.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageEditMovement( ... )
 * </code></pre>
 *
 * To obtain a reference to the PageEditMovement:
 *
 * <pre><code>
 * val ref = PageEditMovement.getRef()
 *
 * def render() = {
 *   PageEditMovement.withRef(ref)( ... )
 * }
 *
 * </code></pre>
 *
 * @author werewolf
 */
object PageEditMovement {
  import PageEditMovementInternal._

  type RefType = Ref.WithScalaComponent[Props,State,Backend,CtorType.Props]

  def getRef(): RefType = Ref.toScalaComponent(component)

  case class Props( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage )

  def apply( routerCtl: BridgeRouter[DuplicatePage], page: DuplicatePage ) = component(Props(routerCtl,page))

}

object PageEditMovementInternal {
  import PageEditMovement._

  val logger = Logger("bridge.PageEditMovement")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   * @param movementId the movement being edited, None is new movement being created
   * @param movement the edited movement
   * @param ntables the number of tables in the movement
   * @param nrounds the number of rounds
   * @param msg a popup message
   */
  case class State(
    movementId: Option[Movement.Id] = None,
    movement: Option[Movement] = None,
    /**
     * Map[table,round] =: boards
     */
    boards: Map[(Int,Int),String] = Map(),
    nteams: Int = 0,
    ntables: Int = 0,
    nrounds: Int = 0,
    nboards: Int = 0,
    msg: Option[TagMod] = None,
    gotBoardsets: Boolean = false,
    boardset: Option[BoardSet] = None
  ) {

    def isNew = movementId.isEmpty
    def hasMovement = movement.isDefined

    def getMovement: Movement = {
      movement.getOrElse(
        Movement(
          movementId.getOrElse(Movement.idNul),
          "",
          "",
          nteams,
          if (ntables > 0 && nrounds > 0) {
            (for {
              table <- 1 to ntables
              round <- 1 to nrounds
            } yield {
              HandInTable(table,round,0,0,List())
            }).toList
          }
          else List(),
          deletable = Some(true)
        )
      )
    }
    def setName( name: Movement.Id ) = copy( movement = Some(getMovement.copy(name = name)))
    def setShort( description: String ) = copy( movement = Some(getMovement.copy(short=description)))
    def setDescription( description: String ) = copy( movement = Some(getMovement.copy(description=description)))

    def logState( msg: String, nstate: State ) = {
      logger.fine(s"$msg: $nstate")
      nstate
    }

    private def adjustHands( newTables: Int, newRounds: Int ) = {
      val mov = getMovement
      logger.fine(s"Entry: newTables=$newTables/$ntables, newRounds=$newRounds/$nrounds, mov=$mov")
      if (newTables <= 0 || newRounds <= 0) {
        logger.fine(s"Tables or rounds is zero: newTables=$newTables/$ntables, newRounds=$newRounds/$nrounds, mov=$mov")
        copy(ntables=newTables,nrounds=newRounds, movement = Some( getMovement.copy(hands=List())))
      } else {
        val step1 = if (newTables < ntables || newRounds < nrounds) {
          logger.fine(s"Removing tables or rounds: newTables=$newTables, newRounds=$newRounds, mov=$mov")
          mov.hands.filter { hit => hit.table <= newTables && hit.round <= newRounds }
        } else {
          mov.hands
        }
        val step2 = if (newTables > ntables || newRounds > nrounds) {
          val step2a = if (newTables > ntables) {
            logger.fine(s"Adding tables: newTables=$newTables, newRounds=$newRounds, mov=$step1")
            step1:::(for {
              t <- (ntables+1 to newTables)
              r <- (1 to newRounds)
            } yield {
              HandInTable(t,r,0,0,List())
            }).toList
          } else {
            step1
          }
          if (newRounds > nrounds) {
            logger.fine(s"Adding rounds: newTables=$newTables, newRounds=$newRounds, mov=$step2a")
            step2a:::( for {
              t <- (1 to ntables)
              r <- (nrounds+1 to newRounds)
            } yield {
              HandInTable(t,r,0,0,List())
            } ).toList
          } else {
            step2a
          }
        } else {
          step1
        }
        val step3 = step2.sortWith { (l,r) =>
          if (l.table == r.table) {
            l.round < r.round
          } else {
            l.table < r.table
          }
        }
        val newmov = mov.copy(hands=step3)
        logger.fine(s"Old mov=$mov, new mov=$newmov")
        copy(ntables=newTables, nrounds=newRounds, movement=Some(newmov))
      }
    }

    def setNTeams( n: Int ) = {
      val mov = getMovement
      val newmov = mov.copy(numberTeams = n)
      logState("setNTeams", copy(nteams = n, movement = Some(newmov)) )
    }

    def setNBoards( n: Int ) = {
      logState("setNBoards", copy(nboards = n) )
    }

    def setNTables( n: Int ) = {
      logState("setNTables", adjustHands(n,nrounds) )
    }

    def setNRounds( n: Int ) = {
      logState("setNRounds", adjustHands(ntables,n) )
    }

    private def set( table: Int, round: Int, f: HandInTable=>HandInTable) = {
      val curmov = getMovement
      val nl = curmov.hands.map { hit =>
        if (hit.table == table && hit.round == round) f(hit)
        else hit
      }
      copy(movement = Some(curmov.copy(hands=nl)))
    }

    def setNSTeam( table: Int, round: Int, team: Int ) = {
      set(table,round, hit => hit.copy(ns = team))
    }
    def setEWTeam( table: Int, round: Int, team: Int ) = {
      set(table,round, hit => hit.copy(ew = team))
    }
    def setBoards( table: Int, round: Int, boards: String ) = {
      logger.fine(s"setBoards table=$table, round=$round, boards=$boards")
      val ns = try {
        val b = boards.split("[, ]+").map( sb => sb.trim.toInt).toList
        logger.fine(s"setBoards table=$table, round=$round, boards=$boards, b=${b.mkString(",")}")
        set(table,round, hit => hit.copy(boards = b)).copy( boards = this.boards+((table,round)->boards))
      } catch {
        case x: NumberFormatException =>
          setMsg("Must be a comma separated list of board numbers")
      }
      logState("setBoards", ns)
    }

    def isValid() = {
      movement.flatMap { mov =>
        if (mov.name != null && mov.name != "") {
          if (mov.short != null && mov.short != "") {
            mov.hands.find { hit =>
              hit.ns <= 0 || hit.ns > nteams || hit.ew <= 0 || hit.ew > nteams ||
                hit.boards.isEmpty || hit.boards.find( b => b<=0 || b>nboards).isDefined
            }.toLeft(1).toOption
          } else {
            None
          }
        } else {
          None
        }
      }.isDefined
    }

    def setMsg( msg: String ) = copy( msg = Some(msg))
    def setMsg( msg: TagMod ) = copy( msg = Some(msg))
    def clearMsg() = copy( msg = None )
  }

  case class VulStat( me: Int, opp: Int, bothVul: Int = 0, meVul: Int = 0, oppVul: Int = 0, neitherVul: Int = 0) {
    def key = (me,opp)
    def incBothVul = copy(bothVul = bothVul+1)
    def incMeVul = copy(meVul = meVul+1)
    def incOppVul = copy(oppVul = oppVul+1)
    def incNeitherVul = copy(neitherVul = neitherVul+1)

    def add( v: VulStat ) = copy( bothVul=bothVul+v.bothVul, meVul=meVul+v.meVul, oppVul=oppVul+v.oppVul, neitherVul=neitherVul+v.neitherVul)

    def swapTeams = copy(me=opp,opp=me,meVul=oppVul,oppVul=meVul)

    def normalize = if (me < opp) this else swapTeams
  }

  class Stats( state: State ) {

    /**
     * @return Map[(t1,t2),v].  t1 is team 1, t2 is team 2,
     *         v is number of boards played between the two.  t1 < t2
     */
    def boardsPlayed = {
      val played = scala.collection.mutable.Map[(Int,Int),Int]()

      def add( team1: Int, team2: Int, n: Int ) = {
        val key = if (team1<team2) (team1,team2) else (team2,team1)
        val v = played.getOrElse(key,0)
        played += key->(v+n)
      }

      state.getMovement.hands.foreach { hit =>
        add(hit.ns,hit.ew,hit.boards.length)
      }

      played.toMap
    }

    def getPairingVul(): Option[Map[(Int,Int),VulStat]] = {
      state.boardset.map { bs =>
        val bmap = bs.boards.map( b => b.id -> b ).toMap
        state.getMovement.hands.map { hit =>
          val ns = hit.ns
          val ew = hit.ew
          hit.boards.foldLeft( VulStat(ns,ew) ) { (ac,boardn) =>
            val board = bmap.get(boardn)
            board.map { b=>
              if (b.nsVul) {
                if (b.ewVul) ac.incBothVul else ac.incMeVul
              } else {
                if (b.ewVul) ac.incOppVul else ac.incNeitherVul
              }
            }.getOrElse(ac)
          }.normalize
        }.groupBy( _.key ).map { e =>
          val (k,v) = e
          v.tail.foldLeft( v.head ) { (ac,v) =>
            ac.add(v)
          }
        }.map { v =>
          v.key -> v
        }.toMap
      }
    }
  }

  class Checker( state: State ) {
    private var errors = scala.collection.mutable.Buffer[String]()

    /**
     * first index is team-1
     * second index is board-1
     * value is number of times played
     */
    val played: Array[Array[Int]] = {
      Array( (for {
        i <- 0 until state.nteams
      } yield {
        new Array[Int](state.nboards)
      }): _* )
    }

    state.getMovement.hands.foreach { hit =>
      if (hit.boards.isEmpty) {
        errors+=s"Table ${hit.table} round ${hit.round} does not play any boards"
      }
      hit.boards.foreach { b =>
        if (b <= 0 || b > state.nboards) errors+=s"Table ${hit.table} round ${hit.round} board is out of range: $b"
        else {
          if (hit.ns > state.nteams) errors+=s"Table ${hit.table} round ${hit.round} north-south team number is out of range: ${hit.ns}"
          else if (hit.ns > 0) played(hit.ns-1)(b-1) += 1
          else errors+=s"North-south team not defined on table ${hit.table} round ${hit.round}"

          if (hit.ew > state.nteams) errors+=s"Table ${hit.table} round ${hit.round} east-west team number is out of range: ${hit.ew}"
          else if (hit.ew > 0) played(hit.ew-1)(b-1) += 1
          else errors+=s"East-west team not defined on table ${hit.table} round ${hit.round}"
        }
      }
    }

    /**
     * returns the errors in the current definition.
     * if the returned list is empty, the definition is good.
     */
    def getErrors: List[String] = {

      if (state.nteams > 0) {
        for {
          t <- 0 until state.nteams
          b <- 0 until state.nboards
        } {
          if (played(t)(b) > 1) {
            errors += s"Team ${t+1} played board ${b+1} ${played(t)(b)} times"
          }
        }
      } else {
        errors += "No teams defined"
      }

      errors.toList
    }

    def stats = new Stats(state)
  }

  val TableCaption = ScalaComponent.builder[(Props,State,Int)]("PageEditMovement.TableCaption")
                    .render_P { args =>
                      val (props,state,table) = args
                      <.caption(
                        s"Table $table"
                      )
                    }.build

  val TableHeader = ScalaComponent.builder[(Props,State)]("PageEditMovement.TableHeader")
                    .render_P { args =>
                      val (props,state) = args
                      <.thead(
                        <.tr(
                          <.th( "Round" ),
                          <.th( "NS" ),
                          <.th( "EW" ),
                          <.th( "Boards" )
                        )
                      )
                    }.build

  //
  // prop._3 is setNSTeam( nsTeam: ReactEventFromInput ) => Callback
  // prop._4 is setEWTeam( ewTeam: ReactEventFromInput ) => Callback
  // prop._5 is setBoards( boards: ReactEventFromInput ) => Callback
  val TableRow = ScalaComponent.builder[(Props,HandInTable,String,(ReactEventFromInput)=>Callback,(ReactEventFromInput)=>Callback,(ReactEventFromInput)=>Callback)]("PageEditMovement.TableRow")
                    .render_P { args =>
                      val (props,movement,boards,setNSTeam,setEWTeam,setBoards) = args

                      <.tr(
                        <.th(
                          movement.round.toString
                        ),
                        <.td(
                          <.input(
                            ^.`type`:="number",
                            ^.name := s"T${movement.table}_${movement.round}_NSTeam",
                            ^.onChange ==> setNSTeam,
                            ^.value := (if (movement.ns==0) "" else movement.ns.toString)
                          )
                        ),
                        <.td(
                          <.input(
                            ^.`type`:="number",
                            ^.name := s"T${movement.table}_${movement.round}_EWTeam",
                            ^.onChange ==> setEWTeam,
                            ^.value := (if (movement.ew==0) "" else movement.ew.toString)
                          )
                        ),
                        <.td(
                          <.input(
                            ^.name := s"T${movement.table}_${movement.round}_NBoards",
                            ^.onChange ==> setBoards,
                            ^.value := boards
                          )
                        ),
                      )
                    }.build

  def boardsetToDropdownListValue( bs: BoardSet ) = {
    js.Dictionary( "short"->bs.short, "obj"->bs).asInstanceOf[js.Object]
  }

  val nullboardsetToDropdownListValue = {
    js.Dictionary( "short"->"none").asInstanceOf[js.Object]
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def inputCB( data: ReactEventFromInput): Callback = data.inputText( text => scope.modState( _.setName(Movement.id(text))) )
    def shortCB( data: ReactEventFromInput): Callback = data.inputText( text => scope.modState( _.setShort(text)) )
    def descCB( data: ReactEventFromInput): Callback = data.inputText( text => scope.modState( _.setDescription(text)) )
    def setNTeamsCB( data: ReactEventFromInput): Callback = data.inputText { text =>
      scope.modState { s =>
        try {
          val n = stringToInt(text)
          if (n <= 0 || n >= 100) {
            s.setMsg( "Must enter a valid number between 1 and 99" )
          } else {
            s.setNTeams(n)
          }
        } catch {
          case x: NumberFormatException =>
            s.setMsg( "Must enter a valid number" )
        }
      }
    }
    def setNBoardsCB( data: ReactEventFromInput): Callback = data.inputText { text =>
      scope.modState { s =>
        try {
          val n = stringToInt(text)
          if (n <= 0 || n >= 100) {
            s.setMsg( "Must enter a valid number between 1 and 99" )
          } else {
            s.setNBoards(n)
          }
        } catch {
          case x: NumberFormatException =>
            s.setMsg( "Must enter a valid number" )
        }
      }
    }
    def setNTablesCB( data: ReactEventFromInput): Callback = data.inputText { text =>
      scope.modState { s =>
        try {
          val n = stringToInt(text)
          if (n <= 0 || n >= 100) {
            s.setMsg( "Must enter a valid number between 1 and 99" )
          } else {
            s.setNTables(n)
          }
        } catch {
          case x: NumberFormatException =>
            s.setMsg( "Must enter a valid number" )
        }
      }
    }
    def setNRoundsCB( data: ReactEventFromInput): Callback = data.inputText { text =>
      scope.modState { s =>
        try {
          val n = stringToInt(text)
          if (n <= 0 || n >= 100) {
            s.setMsg( "Must enter a valid number between 1 and 99" )
          } else {
            s.setNRounds(n)
          }
        } catch {
          case x: NumberFormatException =>
            s.setMsg( "Must enter a valid number" )
        }
      }
    }

    def stringToInt( text: String ) = {
      val t = text.trim
      if (t=="") 0 else t.toInt
    }

    def setNSTeam( table: Int, round: Int )( data: ReactEventFromInput) = data.inputText { text =>
      val nsTeam = stringToInt(text)
      scope.modState( s => s.setNSTeam(table,round,nsTeam))
    }
    def setEWTeam( table: Int, round: Int )( data: ReactEventFromInput) = data.inputText { text =>
      val ewTeam = stringToInt(text)
      scope.modState( s => s.setEWTeam(table,round,ewTeam))
    }
    def setBoards( table: Int, round: Int )( data: ReactEventFromInput) = data.inputText { boards =>
      scope.modState( s => s.setBoards(table,round,boards))
    }

    val toggleDisabled = scope.modState { s =>
      val mov = s.getMovement
      val m = mov.copy( disabled = Some(!mov.isDisabled))
      s.copy(movement = Some(m))
    }

    val clickOk = scope.modState(
      { s =>
        s.setMsg( s.movementId.map(i=>s"Updating movement $i").getOrElse("Creating new movement"))
      },
      Callback {
        val s = scope.withEffectsImpure.state
        s.movementId match {
          case Some(id) =>
            BoardSetController.updateMovement(s.getMovement).onComplete { tr =>
              logger.fine(s"update movement completed: $tr")
              tr match {
                case _: Success[_] =>
                  scope.withEffectsImpure.props.routerCtl.set( MovementSummaryView ).runNow()
                case Failure(ex) =>
                  scope.withEffectsImpure.modState { ss =>
                    ss.setMsg(s"Error updating movement $id")
                  }
              }
            }
          case None =>
            BoardSetController.createMovement(s.getMovement).onComplete { tr =>
              logger.fine(s"create movement completed: $tr")
              tr match {
                case _: Success[_] =>
                  scope.withEffectsImpure.props.routerCtl.set( MovementSummaryView ).runNow()
                case Failure(ex) =>
                  scope.withEffectsImpure.modState { ss =>
                    ss.setMsg(s"Error creating new movement")
                  }
              }
            }
        }
      }
    )

    val popupCancel = Callback {

    } >> scope.modState( s => s.clearMsg())

    def selectBoardset( v: js.Any ): Unit = {
      val bs = v.asInstanceOf[js.Dictionary[BoardSet]].get("obj")
      scope.withEffectsImpure.modState(_.copy(boardset = bs))
    }

    def render( props: Props, state: State ) = {
      import DuplicateStyles._
      val movement = state.getMovement
      val vlistoftables = movement.getTables
      val listoftables = if (vlistoftables.isEmpty || vlistoftables.head.isEmpty) List() else vlistoftables
      val isValid = state.isValid()
      val checker = new Checker(state)
      val errors = checker.getErrors
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
                      "Movements",
                    )
                )),
          helpurl = "../help/duplicate/summary.html",
          routeCtl = props.routerCtl
        )(

        ),
        <.div(
          dupStyles.divEditMovement,
          if (state.isNew || state.hasMovement) {
            TagMod(
              <.div(
                <.label(
                  "Name: ",
                  if (state.isNew) {
                    <.input(
                      ^.name := "Name",
                      ^.onChange ==> inputCB _,
                      ^.value := state.getMovement.name.id
                    )
                  } else {
                    state.getMovement.name.id
                  }
                ),
                <.label(
                  "Short: ",
                  <.input(
                    ^.name := "Short",
                    ^.onChange ==> shortCB _,
                    ^.value := state.getMovement.short
                  )
                ),
                <.label(
                  "Description: ",
                  <.input(
                    ^.name := "Description",
                    ^.onChange ==> descCB _,
                    ^.value := state.getMovement.description
                  )
                ),
                <.label(
                  "Number of teams: ",
                  <.input(
                    ^.`type`:="number",
                    ^.name := "NTeams",
                    ^.onChange ==> setNTeamsCB _,
                    ^.value := state.nteams.toString
                  )
                ),
                <.label(
                  "Number of boards: ",
                  <.input(
                    ^.`type`:="number",
                    ^.name := "NBoards",
                    ^.onChange ==> setNBoardsCB _,
                    ^.value := state.nboards.toString
                  )
                ),
                <.label(
                  "Number of tables: ",
                  <.input(
                    ^.`type`:="number",
                    ^.name := "NTables",
                    ^.onChange ==> setNTablesCB _,
                    ^.value := state.ntables.toString
                  )
                ),
                <.label(
                  "Number of rounds: ",
                  <.input(
                    ^.`type`:="number",
                    ^.name := "NRounds",
                    ^.onChange ==> setNRoundsCB _,
                    ^.value := state.nrounds.toString
                  )
                ),
                CheckBox( id = "Disabled", text = "Disabled", value = state.getMovement.isDisabled, toggle = toggleDisabled ),
                <.label(
                  "Boardset for stats: ",
                  DropdownList(
                    value = state.boardset.map( boardsetToDropdownListValue(_)),
                    textField = Some("short"),
                    placeholder = Some("Select a boardset"),
                    onChange = Some(selectBoardset),
                    data = {
                      logger.fine(s"For DropdownList, state=$state")
                      if (state.gotBoardsets) {
                        val ddl = js.Array( BoardSetStore.getBoardSets().map { e =>
                          val (k,v) = e
                          boardsetToDropdownListValue(v)
                        }.toList: _*).toList
                        Some( js.Array( (nullboardsetToDropdownListValue::ddl): _* ) )
                      } else {
                        None
                      }
                    },
                    containerClassName = Some(BaseStyles.baseStyles.comboboxLightDarkClass)
                  )

                )
              ),
              <.div(
                listoftables.map { rounds =>
                  val table = rounds.head.table
                  <.div(
                    <.table(
                      TableCaption(props,state,table),
                      TableHeader(props,state),
                      <.tbody(
                        rounds.map { round =>
                          val rid = round.round
                          TableRow.withKey( s"T${round.table}_R${round.round}" )((props,round, state.boards.getOrElse((table,rid),""), setNSTeam(table,rid), setEWTeam(table,rid), setBoards(table,rid)))
                        }.toTagMod
                      )
                    )
                  )
                }.toTagMod
              ),
              // if (!errors.isEmpty)
              {
                val played = checker.played
                TagMod(
                  <.div(
                    <.table(
                      <.caption( "Boards played by teams"),
                      <.thead(
                        <.tr(
                          <.th( ^.rowSpan:=2, "Team"),
                          <.th( ^.colSpan:=state.nboards, "Boards"),
                        ),
                        <.tr(
                          ( (1 to state.nboards).map { i =>
                            <.th(i)
                          }).toTagMod
                        )
                      ),
                      <.tbody(
                        ( (1 to state.nteams).map { t =>
                          <.tr(
                            <.th(t),
                            ( (1 to state.nboards).map { i =>
                              val v = played(t-1)(i-1)
                              <.td(v)
                            }).toTagMod
                          )
                        }).toTagMod
                      )
                    )
                  ),
                  <.div(
                    <.pre(
                      errors.mkString("\n")
                    )
                  )
                )
              // } else {
              //   TagMod()
              },
              {
                val stats = checker.stats
                val played = stats.boardsPlayed
                val pair = stats.getPairingVul()
                TagMod(
                  <.div(
                    <.table(
                      <.caption( "Boards played" ),
                      <.thead(
                        <.tr(
                          <.th("Team"),
                          ( (1 to state.nteams).map { i =>
                              <.th(i)
                            }
                          ).toTagMod,
                          <.th("Totals")
                        )
                      ),
                      <.tbody(
                        ((1 to state.nteams).map { r =>
                          <.tr(
                            <.th(r),
                            (
                              (1 to state.nteams).map { c =>
                                type Swapper = VulStat => VulStat
                                val (key,swap: Swapper ) = if (r<c) ((r,c), (vv: VulStat)=>vv) else ((c,r),(vv: VulStat)=>vv.swapTeams)
                                val v = played.getOrElse(key,0)
                                val p = pair.map { p =>
                                  p.get(key).map { vv =>
                                    val v = swap(vv)
                                    s" (${v.bothVul} ${v.meVul} ${v.oppVul} ${v.neitherVul})"
                                  }.getOrElse(" (0 0 0 0)")
                                }.getOrElse("")
                                <.td(
                                  if (v == 0) "" else s"${v}${p}"
                                )
                              }
                            ).toTagMod,
                            <.td(
                              {
                                val x =
                                pair.map { p =>
                                  val v =
                                  p.flatMap { v=>
                                    val (k,vul) = v
                                    if (vul.me == r) vul::Nil
                                    else if (vul.opp == r) vul.swapTeams::Nil
                                    else Nil
                                  }.foldLeft(VulStat(r,r)) { (ac,v) =>
                                    ac.add(v)
                                  }
                                  s" (${v.bothVul} ${v.meVul} ${v.oppVul} ${v.neitherVul})"
                                }.getOrElse("")
                                x
                              }
                            )
                          )
                        }).toTagMod
                      ),
                      <.tfoot(
                        <.tr(
                          <.td(
                            ^.colSpan := 2+state.nteams,
                            "Syntax for an entry: # (b r c n)",
                            " each cell shows how many times the row team plays the column team.",
                            <.br,
                            "Where: # - the number of boards played against each other by the two teams, ",
                            "b - both teams vulnerable, r - row team vulnerable, c - column team vulnerable, n - neither team vulnerable.",
                            <.br,
                            "To see the vulnerabilities, a boardset must be selected."
                          )
                        )
                      )
                    )
                  )
                )
              },
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
                    props.routerCtl.setOnClick(MovementSummaryView)
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

    val storeCallback = scope.modStateOption { (state,props) =>
      props.page match {
        case mev: MovementEditView =>
          val display = mev.display
          BoardSetStore.getMovement(display) match {
            case Some(mov) =>
              val mov1 = if (mov.isDeletable) mov else mov.copy(resetToDefault = Some(true))
              Some(state.copy(
                      movement = Some(mov1),
                      nteams = mov1.numberTeams,
                      nboards = mov1.getBoards.length,
                      nrounds = mov1.allRounds.length,
                      ntables = mov1.getTables.length,
                      boards = mov1.hands.map { hit =>
                        val key = (hit.table,hit.round)
                        key -> hit.boards.mkString(" ")
                      }.toMap
              ))
            case None =>
              None
          }
        case _ =>
          None
      }
    }

    val didMount = scope.stateProps { (state,props) => Callback {
      logger.info("PageEditMovement.didMount")
      BoardSetStore.addChangeListener(storeCallback)
      BoardSetController.getBoardSets().onComplete { tr =>
        logger.fine(s"didMound got getBoardSets result: $tr")
        tr match {
          case Success(v) =>
            scope.withEffectsImpure.modState( _.copy(gotBoardsets=true))
          case Failure(ex) =>
            scope.withEffectsImpure.modState( _.setMsg("Unable to get list of boardsets") )
        }
      }
      props.page match {
        case mev: MovementEditView =>
          val display = mev.display
          BoardSetController.getMovement(display)
        case _ =>
      }
    }}

    val willUnmount = Callback {
      logger.info("PageEditMovement.willUnmount")
      BoardSetStore.removeChangeListener(storeCallback)
    }

  }

  def didUpdate( cdu: ComponentDidUpdate[Props,State,Backend,Unit] ) = cdu.modStateOption { state =>
    val props = cdu.currentProps
    val prevProps = cdu.prevProps
    if (props.page != prevProps.page) {
      props.page match {
        case mev: MovementEditView =>
          val display = mev.display
          BoardSetController.getMovement(display)
          Some( State( movementId = Some(display)) )
        case _ =>
          Some( State() )
      }
    } else {
      None
    }
  }

  val component = ScalaComponent.builder[Props]("PageEditMovement")
                            .initialStateFromProps { props =>
                              props.page match {
                                case mev: MovementEditView =>
                                  val display = mev.display
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

